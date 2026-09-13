package com.paperpages.service;

import com.paperpages.dto.AdminCommentDto;
import com.paperpages.dto.CommentDto;
import com.paperpages.dto.CommentRequest;
import com.paperpages.dto.PageResult;
import com.paperpages.entity.Article;
import com.paperpages.entity.Comment;
import com.paperpages.exception.BusinessException;
import com.paperpages.repository.ArticleRepository;
import com.paperpages.repository.CommentRepository;
import com.paperpages.util.CommentRateLimiter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 评论服务。
 * - 前台：匿名即时发布（approved），支持一层回复楼；可选邮箱用于回复通知
 * - 反垃圾：CommentRateLimiter 对「同一 IP + 同一文章」30 秒限频 + honeypot 隐藏字段
 * - 后台：评论管理（分页检索 / 通过 / 驳回 / 删除 / 博主回复）
 */
@Service
public class CommentService {

    private static final Set<String> STATUSES = Set.of("pending", "approved", "rejected");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final int MAX_EMAIL = 100;
    private static final String AUTHOR_DISPLAY = "博主";

    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;
    private final CommentRateLimiter rateLimiter;
    private final MailService mailService;

    public CommentService(CommentRepository commentRepository,
                          ArticleRepository articleRepository,
                          CommentRateLimiter rateLimiter,
                          MailService mailService) {
        this.commentRepository = commentRepository;
        this.articleRepository = articleRepository;
        this.rateLimiter = rateLimiter;
        this.mailService = mailService;
    }

    /* ==================== 前台 ==================== */

    /** 某篇文章的已通过评论（按创建顺序，前端按其 parentId 组一层楼）。 */
    public List<CommentDto> listByArticle(String articleSlug) {
        Article a = articleRepository.findBySlugAndStatus(articleSlug, "published")
                .orElseThrow(() -> BusinessException.notFound("文章不存在"));
        return commentRepository.findByArticleAndStatusOrderByIdAsc(a, "approved")
                .stream().map(c -> toDto(c, a)).toList();
    }

    /** 访客发表评论（匿名即时展示；ip 用于限频；website 为空才放行 = honeypot 反垃圾）。 */
    @Transactional
    public CommentDto add(String articleSlug, CommentRequest req, String ip) {
        Article a = articleRepository.findBySlugAndStatus(articleSlug, "published")
                .orElseThrow(() -> BusinessException.notFound("文章不存在"));

        // honeypot：正常用户表单里隐藏字段为空；机器人往往会填
        if (req.website() != null && !req.website().isBlank()) {
            throw BusinessException.badRequest("评论提交失败，请稍后再试");
        }

        String email = normalizeEmail(req.email());

        String clientIp = (ip == null || ip.isBlank()) ? "unknown" : ip;
        if (!rateLimiter.allow(clientIp + "|" + articleSlug)) {
            throw BusinessException.badRequest("评论太频繁了，请稍后再试");
        }

        Comment replyTarget = resolveReplyTarget(a, req.parentId());
        Comment c = new Comment();
        c.setArticle(a);
        c.setAuthor(normalizeAuthor(req.author()));
        c.setContent(req.content().trim());
        c.setEmail(email);
        c.setParentId(childParentId(replyTarget));
        c.setStatus("approved");
        c.setAuthor(false);
        commentRepository.save(c);

        notifyIfNeeded(replyTarget, a, c);
        return toDto(c, a);
    }

    /* ==================== 后台 ==================== */

    /** 后台评论分页检索（可按状态 / 文章 / 关键词模糊过滤），新评论在前。 */
    public PageResult<AdminCommentDto> adminList(String status, Long articleId, String keyword, int page, int size) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 50);
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        String st = (status == null || status.isBlank() || "all".equals(status)) ? null : status;

        Pageable pageable = PageRequest.of(p - 1, s);
        Page<Comment> result = commentRepository.adminSearch(st, articleId, kw, pageable);

        // 一次取回父评论作者，避免逐条 N+1
        List<Long> parentIds = result.getContent().stream()
                .map(Comment::getParentId).filter(x -> x != null).collect(Collectors.toList());
        Map<Long, String> parentAuthors = parentIds.isEmpty() ? Map.of()
                : commentRepository.findAllById(parentIds).stream()
                        .collect(Collectors.toMap(Comment::getId, Comment::getAuthor));

        List<AdminCommentDto> list = result.getContent().stream()
                .map(c -> toAdminDto(c, parentAuthors)).toList();
        return new PageResult<>(list, p, s, result.getTotalElements(), result.getTotalPages());
    }

    /** 各状态评论数（后台入口角标用）。 */
    public Map<String, Long> statusCounts() {
        return Map.of(
                "total", commentRepository.count(),
                "pending", commentRepository.countByStatus("pending"),
                "approved", commentRepository.countByStatus("approved"),
                "rejected", commentRepository.countByStatus("rejected"));
    }

    /** 删除评论（连带删除其直接回复，避免孤儿回复）。 */
    @Transactional
    public void delete(Long id) {
        commentRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("评论不存在"));
        commentRepository.deleteByParentId(id);
        commentRepository.deleteById(id);
    }

    /** 通过 / 驳回。 */
    @Transactional
    public void setStatus(Long id, String status) {
        if (!STATUSES.contains(status)) {
            throw BusinessException.badRequest("无效的状态：" + status);
        }
        Comment c = commentRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("评论不存在"));
        c.setStatus(status);
        commentRepository.save(c);
    }

    /** 博主回复某条评论（标记博主徽标；若其留有邮箱则发回复通知）。 */
    @Transactional
    public CommentDto authorReply(Long parentId, String content) {
        Comment target = commentRepository.findById(parentId)
                .orElseThrow(() -> BusinessException.notFound("要回复的评论不存在"));
        Article a = target.getArticle();

        Comment c = new Comment();
        c.setArticle(a);
        c.setAuthor(AUTHOR_DISPLAY);
        c.setContent(content.trim());
        c.setParentId(childParentId(target));
        c.setStatus("approved");
        c.setAuthor(true);
        commentRepository.save(c);

        notifyIfNeeded(target, a, c);
        return toDto(c, a);
    }

    /* ==================== 内部 ==================== */

    /** 校验并返回真正被回复的评论（不存在 / 不属于本文则拒绝）。 */
    private Comment resolveReplyTarget(Article a, Long parentId) {
        if (parentId == null) return null;
        Comment target = commentRepository.findById(parentId)
                .orElseThrow(() -> BusinessException.badRequest("要回复的评论不存在"));
        if (!target.getArticle().getId().equals(a.getId())) {
            throw BusinessException.badRequest("只能回复这篇文章下的评论");
        }
        return target;
    }

    /** 存储到评论表的 parentId：一律挂到顶层评论（保证只有一层楼）。 */
    private Long childParentId(Comment target) {
        if (target == null) return null;
        return target.getParentId() != null ? target.getParentId() : target.getId();
    }

    /** 若目标评论留下邮箱，异步发送回复通知。 */
    private void notifyIfNeeded(Comment target, Article a, Comment newComment) {
        if (target == null || target.getEmail() == null || target.getEmail().isBlank()) return;
        mailService.sendReplyNotification(
                target.getEmail(), a.getTitle(), a.getSlug(),
                target.getAuthor(), newComment.getAuthor(), newComment.getContent());
    }

    /** 校验并规范化可选邮箱（允许空）；格式不对直接拒绝。 */
    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) return null;
        String e = email.trim();
        if (e.length() > MAX_EMAIL || !EMAIL_PATTERN.matcher(e).matches()) {
            throw BusinessException.badRequest("邮箱格式不正确");
        }
        return e;
    }

    private String normalizeAuthor(String author) {
        String a = (author == null || author.isBlank()) ? "匿名读者" : author.trim();
        return a.substring(0, Math.min(a.length(), 20));
    }

    private CommentDto toDto(Comment c, Article a) {
        return new CommentDto(
                c.getId(), a.getSlug(), a.getTitle(), c.getParentId(),
                c.getAuthor(), c.getContent(), c.getDate(), c.isAuthor());
    }

    private AdminCommentDto toAdminDto(Comment c, Map<Long, String> parentAuthors) {
        Long parentId = c.getParentId();
        return new AdminCommentDto(
                c.getId(), c.getArticle().getId(), c.getArticle().getSlug(), c.getArticle().getTitle(),
                parentId, parentId == null ? null : parentAuthors.get(parentId),
                c.getAuthor(), c.getEmail(), c.getContent(), c.getDate(), c.getStatus(), c.isAuthor());
    }
}

package com.paperpages.service;

import com.paperpages.dto.*;
import com.paperpages.entity.Article;
import com.paperpages.exception.BusinessException;
import com.paperpages.repository.ArticleLikeRepository;
import com.paperpages.repository.ArticleRepository;
import com.paperpages.repository.CommentRepository;
import com.paperpages.util.JsonUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ArticleService {

    private static final Set<String> TYPES = Set.of("reading", "movie");
    private static final Set<String> STATUSES = Set.of("published", "draft");

    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final ArticleLikeRepository likeRepository;
    private final JsonUtil jsonUtil;

    public ArticleService(ArticleRepository articleRepository,
                          CommentRepository commentRepository,
                          ArticleLikeRepository likeRepository,
                          JsonUtil jsonUtil) {
        this.articleRepository = articleRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
        this.jsonUtil = jsonUtil;
    }

    /* ==================== 前台 ==================== */

    /** 分页获取已发布文章（可按类型 / 标签过滤），按日期倒序。 */
    public PageResult<ArticleListItem> pagePublished(String type, String tag, int page, int size) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 50);
        boolean hasTag = tag != null && !tag.isBlank();

        if (!hasTag) {
            Pageable pageable = PageRequest.of(p - 1, s);
            Page<Article> result = isAll(type)
                    ? articleRepository.findByStatusOrderByDateDesc("published", pageable)
                    : articleRepository.findByStatusAndTypeOrderByDateDesc("published", type, pageable);
            List<ArticleListItem> list = result.getContent().stream().map(this::toListItem).toList();
            return new PageResult<>(list, p, s, result.getTotalElements(), result.getTotalPages());
        }

        // 标签过滤：取全量已发布列表 → 内存过滤 → 手动分页（个人博客规模足够）
        List<Article> base = isAll(type)
                ? articleRepository.findByStatusOrderByDateDesc("published")
                : articleRepository.findByStatusAndTypeOrderByDateDesc("published", type);
        List<Article> filtered = base.stream()
                .filter(a -> splitTags(a.getTags()).contains(tag))
                .toList();
        int total = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / s));
        int from = Math.min((p - 1) * s, total);
        int to = Math.min(from + s, total);
        List<ArticleListItem> list = filtered.subList(from, to).stream().map(this::toListItem).toList();
        return new PageResult<>(list, p, s, total, totalPages);
    }

    private boolean isAll(String type) {
        return type == null || type.isBlank() || "all".equals(type);
    }

    /** 全文检索（标题/摘要/标签/正文）。 */
    public List<ArticleListItem> search(String q) {
        String k = (q == null ? "" : q).trim();
        if (k.isEmpty()) return List.of();
        return articleRepository.searchPublished(k).stream().map(this::toListItem).toList();
    }

    /** 已发布文章详情。 */
    public ArticleDetail detail(String slug) {
        Article a = articleRepository.findBySlugAndStatus(slug, "published")
                .orElseThrow(() -> BusinessException.notFound("文章不存在"));
        return toDetail(a);
    }

    /** 随机一篇文章（随便翻翻）。 */
    public Map<String, String> random() {
        List<Article> all = articleRepository.findByStatusOrderByDateDesc("published");
        if (all.isEmpty()) return Map.of();
        Article a = all.get(ThreadLocalRandom.current().nextInt(all.size()));
        return Map.of("slug", a.getSlug(), "title", a.getTitle());
    }

    /** 相关推荐：与当前文章共享标签最多者优先，最多 limit 篇。 */
    public List<ArticleListItem> related(String slug, int limit) {
        Article cur = articleRepository.findBySlugAndStatus(slug, "published").orElse(null);
        if (cur == null) return List.of();
        List<String> curTags = splitTags(cur.getTags());
        int n = Math.min(Math.max(limit, 1), 5);

        return articleRepository.findByStatusOrderByDateDesc("published").stream()
                .filter(a -> !a.getSlug().equals(slug))
                .map(a -> new Scored(a, (int) splitTags(a.getTags()).stream().filter(curTags::contains).count()))
                .filter(s -> s.shared > 0)
                .sorted(Comparator.comparingInt((Scored s) -> s.shared).reversed()
                        .thenComparing(s -> s.article.getDate(), Comparator.reverseOrder()))
                .limit(n)
                .map(s -> toListItem(s.article))
                .toList();
    }

    /** 管理端详情（含草稿，编辑/发布后回显用）。 */
    public ArticleDetail detailAdmin(String slug) {
        Article a = articleRepository.findBySlug(slug)
                .orElseThrow(() -> BusinessException.notFound("文章不存在"));
        return toDetail(a);
    }

    /** 阅读计数 +1（仅已发布文章）。 */
    @Transactional
    public void incrementViews(String slug) {
        articleRepository.findBySlugAndStatus(slug, "published").ifPresent(a -> {
            a.setViews(a.getViews() + 1);
            articleRepository.save(a);
        });
    }

    /* ==================== 后台 ==================== */

    public List<ArticleListItem> adminList(String status) {
        List<Article> all;
        if (status == null || status.isBlank() || "all".equals(status)) {
            all = articleRepository.findAllByOrderByDateDesc();
        } else {
            all = articleRepository.findByStatusOrderByDateDesc(status);
        }
        return all.stream().map(this::toListItem).toList();
    }

    @Transactional
    public ArticleDetail create(ArticleRequest req) {
        validate(req);

        String slug = (req.slug() == null || req.slug().isBlank())
                ? "p" + System.currentTimeMillis()
                : normalizeSlug(req.slug());
        if (articleRepository.findBySlug(slug).isPresent()) {
            throw BusinessException.badRequest("slug 已被使用：" + slug);
        }

        Article a = new Article();
        a.setSlug(slug);
        apply(a, req);
        articleRepository.save(a);
        return toDetail(a);
    }

    @Transactional
    public ArticleDetail update(String slug, ArticleRequest req) {
        Article a = articleRepository.findBySlug(slug)
                .orElseThrow(() -> BusinessException.notFound("文章不存在"));

        if (req.slug() != null && !req.slug().isBlank() && !req.slug().equals(slug)) {
            String newSlug = normalizeSlug(req.slug());
            if (!newSlug.equals(slug) && articleRepository.findBySlug(newSlug).isPresent()) {
                throw BusinessException.badRequest("slug 已被使用：" + newSlug);
            }
            a.setSlug(newSlug);
        }
        apply(a, req);
        articleRepository.save(a);
        return toDetail(a);
    }

    @Transactional
    public void delete(String slug) {
        Article a = articleRepository.findBySlug(slug)
                .orElseThrow(() -> BusinessException.notFound("文章不存在"));
        commentRepository.deleteByArticleId(a.getId());
        articleRepository.delete(a);
    }

    @Transactional
    public void updateStatus(String slug, String status) {
        if (!STATUSES.contains(status)) {
            throw BusinessException.badRequest("无效的状态：" + status);
        }
        Article a = articleRepository.findBySlug(slug)
                .orElseThrow(() -> BusinessException.notFound("文章不存在"));
        a.setStatus(status);
        articleRepository.save(a);
    }

    /* ==================== 内部 ==================== */

    private void validate(ArticleRequest req) {
        if (req.title() == null || req.title().isBlank()) {
            throw BusinessException.badRequest("标题不能为空");
        }
        if (!TYPES.contains(req.type())) {
            throw BusinessException.badRequest("分类只能是 reading 或 movie");
        }
        if (req.date() == null || req.date().isBlank()) {
            throw BusinessException.badRequest("日期不能为空");
        }
        if (req.summary() == null || req.summary().isBlank()) {
            throw BusinessException.badRequest("摘要不能为空");
        }
        if (req.content() == null || req.content().isEmpty()) {
            throw BusinessException.badRequest("正文不能为空");
        }
        if (!STATUSES.contains(req.status())) {
            throw BusinessException.badRequest("状态只能是 published 或 draft");
        }
    }

    private void apply(Article a, ArticleRequest req) {
        a.setTitle(req.title().trim());
        a.setType(req.type());
        a.setCategory(req.category() != null && !req.category().isBlank()
                ? req.category().trim()
                : ("movie".equals(req.type()) ? "观影感受" : "读书笔记"));
        a.setDate(LocalDate.parse(req.date()));
        a.setTags(String.join(",", req.tags() == null ? List.of() : req.tags()));
        a.setSummary(req.summary().trim());
        a.setContent(jsonUtil.writeList(req.content()));
        a.setStatus(req.status());
        a.setUpdatedAt(LocalDate.now());
    }

    private String normalizeSlug(String slug) {
        String s = slug.trim().toLowerCase()
                .replaceAll("[^a-z0-9\\-]", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("(^-|-$)", "");
        return s.isEmpty() ? "p" + System.currentTimeMillis() : s;
    }

    /** 确定性伪随机阅读数：60 + hash(slug) % 420，刷新/重启不变。 */
    public static int defaultViews(String slug) {
        int h = 0;
        for (int i = 0; i < slug.length(); i++) {
            h = h * 31 + slug.charAt(i);
        }
        return 60 + Math.floorMod(h, 420);
    }

    public ArticleListItem toListItem(Article a) {
        return new ArticleListItem(
                a.getSlug(), a.getTitle(), a.getType(), a.getCategory(), a.getDate(),
                splitTags(a.getTags()), a.getSummary(), a.getViews(),
                commentRepository.countByArticle(a),
                likeRepository.countByArticle(a),
                a.getStatus());
    }

    private ArticleDetail toDetail(Article a) {
        return new ArticleDetail(
                a.getSlug(), a.getTitle(), a.getType(), a.getCategory(), a.getDate(),
                splitTags(a.getTags()), a.getSummary(),
                jsonUtil.readList(a.getContent()), a.getViews(),
                commentRepository.countByArticle(a),
                likeRepository.countByArticle(a),
                a.getStatus(), a.getUpdatedAt());
    }

    public List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) return List.of();
        return java.util.Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private record Scored(Article article, int shared) {
    }
}

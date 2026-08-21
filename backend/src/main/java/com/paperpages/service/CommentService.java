package com.paperpages.service;

import com.paperpages.dto.CommentDto;
import com.paperpages.dto.CommentRequest;
import com.paperpages.entity.Article;
import com.paperpages.entity.Comment;
import com.paperpages.exception.BusinessException;
import com.paperpages.repository.ArticleRepository;
import com.paperpages.repository.CommentRepository;
import com.paperpages.util.CommentRateLimiter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 评论服务：访客评论即时发布（无审核流），
 * 通过 CommentRateLimiter 对「同一 IP + 同一文章」做 30 秒限频。
 */
@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;
    private final CommentRateLimiter rateLimiter;

    public CommentService(CommentRepository commentRepository,
                          ArticleRepository articleRepository,
                          CommentRateLimiter rateLimiter) {
        this.commentRepository = commentRepository;
        this.articleRepository = articleRepository;
        this.rateLimiter = rateLimiter;
    }

    /** 某篇文章的评论（新的在前，全部可见）。 */
    public List<CommentDto> listByArticle(String articleSlug) {
        Article a = articleRepository.findBySlugAndStatus(articleSlug, "published")
                .orElseThrow(() -> BusinessException.notFound("文章不存在"));
        return commentRepository.findByArticleOrderByCreatedAtDesc(a)
                .stream().map(c -> toDto(c, a)).toList();
    }

    /** 访客发表评论（即时展示；ip 用于限频）。 */
    @Transactional
    public CommentDto add(String articleSlug, CommentRequest req, String ip) {
        Article a = articleRepository.findBySlugAndStatus(articleSlug, "published")
                .orElseThrow(() -> BusinessException.notFound("文章不存在"));

        String clientIp = (ip == null || ip.isBlank()) ? "unknown" : ip;
        if (!rateLimiter.allow(clientIp + "|" + articleSlug)) {
            throw BusinessException.badRequest("评论太频繁了，请稍后再试");
        }

        Comment c = new Comment();
        c.setArticle(a);
        String author = req.author() == null || req.author().isBlank() ? "匿名读者" : req.author().trim();
        c.setAuthor(author.substring(0, Math.min(author.length(), 20)));
        c.setContent(req.content().trim());
        c.setStatus("approved");   // 即时展示，不再需要审核
        commentRepository.save(c);
        return toDto(c, a);
    }

    private CommentDto toDto(Comment c, Article a) {
        return new CommentDto(
                c.getId(),
                a.getSlug(),
                a.getTitle(),
                c.getAuthor(),
                c.getContent(),
                c.getDate(),
                c.getStatus());
    }
}

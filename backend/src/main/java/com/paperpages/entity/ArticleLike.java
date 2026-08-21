package com.paperpages.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 文章点赞。
 * visitorKey 由后端下发的 HttpOnly Cookie（pp_visitor）标识访客，
 * (article_id, visitor_key) 唯一约束保证同一访客对同一文章只能点赞一次。
 */
@Entity
@Table(name = "article_likes",
    uniqueConstraints = @UniqueConstraint(name = "uk_like_article_visitor", columnNames = {"article_id", "visitor_key"}),
    indexes = @Index(name = "idx_likes_article", columnList = "article_id"))
public class ArticleLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(name = "visitor_key", nullable = false, length = 64)
    private String visitorKey;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Article getArticle() { return article; }
    public void setArticle(Article article) { this.article = article; }
    public String getVisitorKey() { return visitorKey; }
    public void setVisitorKey(String visitorKey) { this.visitorKey = visitorKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

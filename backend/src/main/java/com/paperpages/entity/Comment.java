package com.paperpages.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 评论实体（匿名即时发布 + 回复楼 + 可选邮箱通知）。
 * status: pending(待审核) | approved(已通过) | rejected(已驳回/前台隐藏)
 * 访客评论默认 approved（即时展示）；博主可在后台审核、驳回或删除。
 * 回复：parentId 指向父评论，仅允许一层（回复挂到顶层评论下）。
 */
@Entity
@Table(name = "comments",
    indexes = {
        @Index(name = "idx_comments_article", columnList = "article_id"),
        @Index(name = "idx_comments_status", columnList = "status")
    })
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(nullable = false, length = 50)
    private String author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 20)
    private String status = "pending";

    /** 父评论 id（回复楼）；null 表示顶层评论。 */
    @Column(name = "parent_id")
    private Long parentId;

    /** 回复通知邮箱（可选，仅在博主/他人回复时用于通知，不对外展示）。 */
    @Column(length = 100)
    private String email;

    /** 是否博主本人发布的回复（前台显示「博主」徽标）。 */
    @Column(name = "is_author", nullable = false)
    private boolean authorFlag = false;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (date == null) date = LocalDate.now();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Article getArticle() { return article; }
    public void setArticle(Article article) { this.article = article; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public boolean isAuthor() { return authorFlag; }
    public void setAuthor(boolean author) { this.authorFlag = author; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

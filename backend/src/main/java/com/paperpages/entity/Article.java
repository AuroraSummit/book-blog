package com.paperpages.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 文章实体。
 * type:   reading(读书笔记) | movie(观影感受)
 * status: published(已发布) | draft(草稿)
 * content 以 JSON 数组字符串存储（每项一段正文，支持 "## " 小标题与 "> " 引用前缀）。
 */
@Entity
@Table(name = "articles",
    indexes = {
        @Index(name = "idx_articles_date", columnList = "date"),
        @Index(name = "idx_articles_type", columnList = "type"),
        @Index(name = "idx_articles_status", columnList = "status")
    })
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 对外使用的可读标识，如 moon-and-sixpence */
    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(length = 50)
    private String category;

    @Column(nullable = false)
    private LocalDate date;

    /** 逗号分隔的标签 */
    @Column(length = 500)
    private String tags;

    @Column(columnDefinition = "TEXT")
    private String summary;

    /** 正文：JSON 数组字符串，如 ["段落一", "> 引用", "## 小标题"] */
    @Lob
    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;

    @Column(nullable = false)
    private int views = 0;

    @Column(nullable = false, length = 20)
    private String status = "published";

    @Column(name = "updated_at")
    private LocalDate updatedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = date;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getViews() { return views; }
    public void setViews(int views) { this.views = views; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDate updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

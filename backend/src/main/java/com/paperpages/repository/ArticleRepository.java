package com.paperpages.repository;

import com.paperpages.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    Optional<Article> findBySlug(String slug);

    Optional<Article> findBySlugAndStatus(String slug, String status);

    // ---------- 前台（仅已发布） ----------
    Page<Article> findByStatusOrderByDateDesc(String status, Pageable pageable);

    Page<Article> findByStatusAndTypeOrderByDateDesc(String status, String type, Pageable pageable);

    List<Article> findByStatusAndTypeOrderByDateDesc(String status, String type);

    // 说明：MySQL utf8mb4_unicode_ci 排序规则本身不区分大小写，无需 LOWER
    @Query("SELECT a FROM Article a WHERE a.status = 'published' AND (" +
            "a.title LIKE CONCAT('%', :q, '%') OR " +
            "a.summary LIKE CONCAT('%', :q, '%') OR " +
            "a.tags LIKE CONCAT('%', :q, '%') OR " +
            "a.content LIKE CONCAT('%', :q, '%')) " +
            "ORDER BY a.date DESC")
    List<Article> searchPublished(@Param("q") String q);

    // ---------- 后台（含草稿） ----------
    List<Article> findAllByOrderByDateDesc();

    List<Article> findByStatusOrderByDateDesc(String status);

    long countByType(String type);

    long countByStatus(String status);
}

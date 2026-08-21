package com.paperpages.repository;

import com.paperpages.entity.Article;
import com.paperpages.entity.ArticleLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ArticleLikeRepository extends JpaRepository<ArticleLike, Long> {

    boolean existsByArticleAndVisitorKey(Article article, String visitorKey);

    long countByArticle(Article article);

    /** 点赞榜：按点赞数倒序取前 N。返回 [slug, title, likes]。 */
    @Query("SELECT l.article.slug, l.article.title, COUNT(l) AS cnt " +
            "FROM ArticleLike l GROUP BY l.article.id, l.article.slug, l.article.title " +
            "ORDER BY cnt DESC")
    List<Object[]> topLiked();
}

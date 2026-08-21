package com.paperpages.repository;

import com.paperpages.entity.Article;
import com.paperpages.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 评论仓库：评论即时发布（无审核流），全部可见。 */
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByArticleOrderByCreatedAtDesc(Article article);

    long countByArticle(Article article);

    void deleteByArticleId(Long articleId);
}

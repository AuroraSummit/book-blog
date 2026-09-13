package com.paperpages.repository;

import com.paperpages.entity.Article;
import com.paperpages.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** 评论仓库。 */
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** 某篇文章的评论（仅 approved，按创建顺序，供前台组树）。 */
    List<Comment> findByArticleAndStatusOrderByIdAsc(Article article, String status);

    /** 后台分页检索：可按 状态 / 文章 / 关键词（作者或内容模糊）过滤，新评论在前。 */
    @Query("SELECT c FROM Comment c WHERE " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:articleId IS NULL OR c.article.id = :articleId) AND " +
            "(:kw IS NULL OR c.author LIKE CONCAT('%', :kw, '%') OR c.content LIKE CONCAT('%', :kw, '%')) " +
            "ORDER BY c.id DESC")
    Page<Comment> adminSearch(@Param("status") String status,
                              @Param("articleId") Long articleId,
                              @Param("kw") String kw,
                              Pageable pageable);

    /** 某篇文章的已通过评论数（前台展示）。 */
    long countByArticleAndStatus(Article article, String status);

    /** 某篇文章的全部评论数（后台统计用）。 */
    long countByArticle(Article article);

    long countByStatus(String status);

    /** 删除某条评论的直接回复（删除评论时级联清理）。 */
    void deleteByParentId(Long parentId);

    void deleteByArticleId(Long articleId);
}

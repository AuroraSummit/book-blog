package com.paperpages.service;

import com.paperpages.dto.LikeResponse;
import com.paperpages.entity.Article;
import com.paperpages.entity.ArticleLike;
import com.paperpages.exception.BusinessException;
import com.paperpages.repository.ArticleLikeRepository;
import com.paperpages.repository.ArticleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 文章点赞：同一访客（visitorKey）对同一文章只能点赞一次（幂等）。 */
@Service
public class LikeService {

    private final ArticleRepository articleRepository;
    private final ArticleLikeRepository likeRepository;

    public LikeService(ArticleRepository articleRepository, ArticleLikeRepository likeRepository) {
        this.articleRepository = articleRepository;
        this.likeRepository = likeRepository;
    }

    @Transactional
    public LikeResponse like(String slug, String visitorKey) {
        Article a = articleRepository.findBySlugAndStatus(slug, "published")
                .orElseThrow(() -> BusinessException.notFound("文章不存在"));
        if (visitorKey == null || visitorKey.isBlank()) visitorKey = "anon";
        if (!likeRepository.existsByArticleAndVisitorKey(a, visitorKey)) {
            ArticleLike like = new ArticleLike();
            like.setArticle(a);
            like.setVisitorKey(visitorKey);
            likeRepository.save(like);
        }
        return new LikeResponse(true, likeRepository.countByArticle(a));
    }

    /** 查询当前访客是否已点赞及最新点赞数。 */
    public LikeResponse liked(String slug, String visitorKey) {
        Article a = articleRepository.findBySlugAndStatus(slug, "published")
                .orElseThrow(() -> BusinessException.notFound("文章不存在"));
        boolean liked = visitorKey != null && !visitorKey.isBlank()
                && likeRepository.existsByArticleAndVisitorKey(a, visitorKey);
        return new LikeResponse(liked, likeRepository.countByArticle(a));
    }
}

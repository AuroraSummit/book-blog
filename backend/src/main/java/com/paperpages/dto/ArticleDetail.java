package com.paperpages.dto;

import java.time.LocalDate;
import java.util.List;

/** 文章详情（含正文、评论数与点赞数）。 */
public record ArticleDetail(
        String slug,
        String title,
        String type,
        String category,
        LocalDate date,
        List<String> tags,
        String summary,
        List<String> content,
        int views,
        long commentCount,
        long likeCount,
        String status,
        LocalDate updatedAt) {
}

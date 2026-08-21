package com.paperpages.dto;

import java.util.List;
import java.util.Map;

/** 后台仪表盘统计数据。 */
public record StatsDto(
        long totalArticles,
        long totalBooks,
        long totalMovies,
        long totalDrafts,
        long totalComments,
        long totalLikes,
        List<ArticleListItem> recentArticles,
        List<Map<String, Object>> visitors,
        List<Map<String, Object>> published,
        List<Map<String, Object>> mostViewed,
        List<Map<String, Object>> mostLiked,
        List<TagCount> tagDistribution) {
}

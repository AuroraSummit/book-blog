package com.paperpages.dto;

import java.time.LocalDate;
import java.util.List;

/** 列表条目（不含正文）。 */
public record ArticleListItem(
        String slug,
        String title,
        String type,
        String category,
        LocalDate date,
        List<String> tags,
        String summary,
        int views,
        long commentCount,
        long likeCount,
        String status) {
}

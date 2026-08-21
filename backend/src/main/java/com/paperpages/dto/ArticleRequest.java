package com.paperpages.dto;

import java.util.List;

/**
 * 创建/更新文章的请求体。
 * slug 可选：留空时后端自动生成；编辑时必须传原 slug（用于定位文章）。
 */
public record ArticleRequest(
        String slug,
        String title,
        String type,
        String category,
        String date,
        List<String> tags,
        String summary,
        List<String> content,
        String status) {

    public ArticleRequest {
        if (type == null || type.isBlank()) type = "reading";
        if (status == null || status.isBlank()) status = "published";
    }
}

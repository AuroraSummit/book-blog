package com.paperpages.dto;

import java.time.LocalDate;

/** 后台评论管理条目（含邮箱、审核状态等完整信息，仅登录后可访问）。 */
public record AdminCommentDto(
        Long id,
        Long articleId,
        String articleSlug,
        String articleTitle,
        Long parentId,
        String parentAuthor,
        String author,
        String email,
        String content,
        LocalDate date,
        String status,
        boolean isAuthor) {
}

package com.paperpages.dto;

import java.time.LocalDate;

/** 评论（前台只含 approved；后台含 status）。 */
public record CommentDto(
        Long id,
        String articleSlug,
        String articleTitle,
        String author,
        String content,
        LocalDate date,
        String status) {
}

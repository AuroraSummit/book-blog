package com.paperpages.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 博主回复请求：回复某条评论（文章取自已存在评论，无需传文章 slug）。 */
public record AdminReplyRequest(
        @NotBlank(message = "评论内容不能为空") @Size(max = 500, message = "评论最长 500 字") String content,
        Long parentId) {
}

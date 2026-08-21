package com.paperpages.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 访客发表评论请求。 */
public record CommentRequest(
        @Size(max = 20, message = "昵称最长 20 字") String author,
        @NotBlank(message = "评论内容不能为空") @Size(max = 500, message = "评论最长 500 字") String content) {
}

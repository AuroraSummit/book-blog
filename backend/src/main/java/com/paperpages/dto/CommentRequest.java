package com.paperpages.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 发表评论请求（匿名即时发布；email 可选用于回复通知；website 为 honeypot 反垃圾陷阱）。 */
public record CommentRequest(
        @Size(max = 20, message = "昵称最长 20 字") String author,
        @NotBlank(message = "评论内容不能为空") @Size(max = 500, message = "评论最长 500 字") String content,
        String email,
        Long parentId,
        /** honeypot：正常用户不填，机器人会填（隐藏字段）。 */
        String website) {
}

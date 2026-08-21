package com.paperpages.dto;

/** 点赞响应：本次点赞后状态与最新点赞数。 */
public record LikeResponse(boolean liked, long likeCount) {
}

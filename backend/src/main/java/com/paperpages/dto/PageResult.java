package com.paperpages.dto;

import java.util.List;

/** 分页结果。 */
public record PageResult<T>(List<T> list, int page, int size, long total, int totalPages) {
}

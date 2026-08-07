package com.whut.training.domain.dto;

/**
 * 练习题抽题请求。
 *
 * @param minRating 最小 rating。
 * @param maxRating 最大 rating。
 * @param tags      必须同时包含的 Codeforces 标签。
 */
public record PracticeDrawRequest(Integer minRating, Integer maxRating, java.util.List<String> tags) {
}

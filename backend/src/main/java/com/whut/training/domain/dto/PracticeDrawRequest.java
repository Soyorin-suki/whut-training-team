package com.whut.training.domain.dto;

/**
 * 练习题抽题请求。
 *
 * @param minRating 最小 rating。
 * @param maxRating 最大 rating。
 */
public record PracticeDrawRequest(Integer minRating, Integer maxRating) {
}

package com.whut.training.domain.dto;

/**
 * 独立的趣味签到记录，与每日一题完成状态互不影响。
 */
public record FunCheckInItem(
        Long id,
        Long userId,
        String date,
        String fortuneKey,
        String fortuneTitle,
        String fortuneMessage,
        String luckyTag,
        String luckyColor,
        int luckLevel,
        String checkedAt
) {
}

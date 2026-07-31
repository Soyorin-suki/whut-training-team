package com.whut.training.domain.dto;

/**
 * 近期比赛条目。
 *
 * @param platform        比赛平台。
 * @param contestId       平台内比赛 ID。
 * @param name            比赛名称。
 * @param type            比赛类型。
 * @param startTime       带时区的开始时间。
 * @param durationMinutes 比赛时长（分钟）。
 * @param ratedRange      Rated 范围。
 * @param url             官方比赛地址。
 */
public record UpcomingContestItem(
        String platform,
        String contestId,
        String name,
        String type,
        String startTime,
        int durationMinutes,
        String ratedRange,
        String url
) {
}

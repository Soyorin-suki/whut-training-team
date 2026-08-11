package com.whut.training.service;

import com.whut.training.domain.dto.UpcomingContestItem;

import java.util.List;

/** 近期比赛聚合用例，隔离控制器和各竞赛平台的具体抓取实现。 */
public interface ContestService {
    List<UpcomingContestItem> getUpcomingContests();

    /** 返回用于每周训练追踪的 AtCoder 比赛窗口，包括近期已结束比赛。 */
    List<UpcomingContestItem> getAtCoderContestWindow();
}

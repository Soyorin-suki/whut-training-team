package com.whut.training.service.impl;

import com.whut.training.domain.dto.HomeOverview;
import com.whut.training.domain.dto.LeaderboardItem;
import com.whut.training.domain.entity.PushPoolItem;
import com.whut.training.repository.UserRepository;
import com.whut.training.repository.DailyProblemRepository;
import com.whut.training.service.HomeService;
import com.whut.training.service.PushPoolService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class HomeServiceImpl implements HomeService {

    private final UserRepository userRepository;
    private final LeaderboardServiceImpl leaderboardService;
    private final DailyProblemRepository dailyProblemRepository;
    private final PushPoolService pushPoolService;

    public HomeServiceImpl(UserRepository userRepository, LeaderboardServiceImpl leaderboardService,
                           DailyProblemRepository dailyProblemRepository, PushPoolService pushPoolService) {
        this.userRepository = userRepository;
        this.leaderboardService = leaderboardService;
        this.dailyProblemRepository = dailyProblemRepository;
        this.pushPoolService = pushPoolService;
    }

    @Override
    public HomeOverview getOverview(int topLimit) {
        HomeOverview o = new HomeOverview();
        o.setTotalUsers(userRepository.countAll());

        List<LeaderboardItem> top = leaderboardService.getTop(topLimit, 0);
        o.setTopUsers(top);

        LocalDate today = LocalDate.now();
        var slots = dailyProblemRepository.findDailySlotsByDate(today);
        if (slots != null && !slots.isEmpty()) {
            List<com.whut.training.domain.dto.ProblemView> views = slots.stream().map(s -> new com.whut.training.domain.dto.ProblemView(
                    s.slot().toUpperCase(),
                    s.date().toString(),
                    s.problemKey(),
                    s.contestId(),
                    s.problemIndex(),
                    s.name(),
                    s.rating(),
                    s.tags(),
                    s.sourceUrl()
            )).toList();
            o.setTodayProblem(views);
        } else {
            o.setTodayProblem(null);
        }

        Optional<PushPoolItem> todayPush = pushPoolService.getTodayPush();
        o.setTodayPushProblem(todayPush.orElse(null));

        int todayCheckedIn = dailyProblemRepository.countCheckedInUsersByDate(today);
        int todaySubmissions = dailyProblemRepository.countSubmissionsByDate(today);
        o.setDailySubmissionSummary(new HomeOverview.DailySubmissionSummary(todaySubmissions, todayCheckedIn));

        return o;
    }
}

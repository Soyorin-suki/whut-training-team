package com.whut.training.service;

import com.whut.training.common.TimeProvider;
import com.whut.training.domain.dto.ActiveTeamTrainingDashboard;
import com.whut.training.domain.dto.CodeforcesOverview;
import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.MemberType;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.repository.CodeforcesProfileSnapshotRepository;
import com.whut.training.repository.TrainingDashboardRepository;
import com.whut.training.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrainingDashboardServiceTest {

    @Test
    void aggregatesActiveTeamTrainingWithoutRemoteRequests() {
        UserRepository userRepository = mock(UserRepository.class);
        TrainingDashboardRepository dashboardRepository = mock(TrainingDashboardRepository.class);
        CodeforcesProfileSnapshotRepository snapshotRepository = mock(CodeforcesProfileSnapshotRepository.class);
        TimeProvider timeProvider = mock(TimeProvider.class);
        LocalDate today = LocalDate.of(2026, 8, 1);

        User owl = member(1L, "owl", "猫头鹰", 2000, 1961);
        User cat = member(2L, "cat", "小猫", 1000, 1400);

        when(timeProvider.today()).thenReturn(today);
        when(userRepository.findByMemberType(MemberType.ACTIVE_TEAM)).thenReturn(List.of(owl, cat));
        when(dashboardRepository.findActiveTeamDailyActivity(today.minusDays(364), today)).thenReturn(List.of(
                activity(1L, today),
                activity(1L, today.minusDays(1)),
                activity(1L, today.minusDays(2)),
                activity(2L, today.minusDays(5))
        ));
        when(dashboardRepository.findActiveTeamPracticeSummary(today.minusDays(29), today)).thenReturn(List.of(
                new TrainingDashboardRepository.PracticeSummary(1L, 4, 3)
        ));

        CodeforcesOverview overview = new CodeforcesOverview(
                "Persona_owl", 1961, 2040, 1644, 1700, 1700, 62,
                null, false, Instant.parse("2026-08-01T00:00:00Z"), false,
                List.of(), List.of()
        );
        when(snapshotRepository.findAllForActiveTeam()).thenReturn(Map.of(
                1L, new CodeforcesProfileSnapshotRepository.Snapshot(
                        overview,
                        Instant.parse("2026-08-01T00:00:00Z")
                )
        ));

        ActiveTeamTrainingDashboard dashboard = new TrainingDashboardService(
                userRepository,
                dashboardRepository,
                snapshotRepository,
                timeProvider
        ).getDashboard();

        assertThat(dashboard.summary().activeMemberCount()).isEqualTo(2);
        assertThat(dashboard.summary().todayCompletedCount()).isEqualTo(1);
        assertThat(dashboard.summary().sevenDayActiveCount()).isEqualTo(2);
        assertThat(dashboard.summary().sevenDayCompletionRate()).isEqualTo(28.6);
        assertThat(dashboard.summary().totalPoints()).isEqualTo(3000);
        assertThat(dashboard.sevenDayTrend()).hasSize(7);
        assertThat(dashboard.members()).extracting(ActiveTeamTrainingDashboard.MemberTraining::username)
                .containsExactly("owl", "cat");

        ActiveTeamTrainingDashboard.MemberTraining owlRow = dashboard.members().get(0);
        assertThat(owlRow.todayCompleted()).isTrue();
        assertThat(owlRow.currentStreakDays()).isEqualTo(3);
        assertThat(owlRow.thirtyDayPracticeDraws()).isEqualTo(4);
        assertThat(owlRow.thirtyDayPracticeSolved()).isEqualTo(3);
        assertThat(owlRow.codeforcesSolvedCount()).isEqualTo(1644);
    }

    private static User member(Long id, String username, String displayName, int points, int rating) {
        User user = new User(id, username, username + "@whut.local", "password", UserRole.USER);
        user.setDisplayName(displayName);
        user.setMemberType(MemberType.ACTIVE_TEAM);
        user.setTotalPoints(points);
        user.setCodeforcesRating(rating);
        user.setCodeforcesHandle(username + "_cf");
        return user;
    }

    private static TrainingDashboardRepository.DailyActivity activity(Long userId, LocalDate date) {
        return new TrainingDashboardRepository.DailyActivity(userId, date, 1200);
    }
}

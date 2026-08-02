package com.whut.training.repository;

import com.whut.training.domain.dto.CodeforcesOverview;
import com.whut.training.domain.entity.CfProblem;
import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.MemberType;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.service.CodeforcesApiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 使用 H2 MySQL 模式执行项目中的 MySQL 建表和 upsert 方言。
 */
@SpringBootTest
@Transactional
class MySqlRepositoryCompatibilityTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DailyProblemRepository dailyProblemRepository;

    @Autowired
    private PushPoolRepository pushPoolRepository;

    @Autowired
    private CodeforcesProfileSnapshotRepository snapshotRepository;

    @Autowired
    private TrainingDashboardRepository trainingDashboardRepository;

    @Autowired
    private CodeforcesRatingHistoryRepository ratingHistoryRepository;

    @Autowired
    private ProblemListRepository problemListRepository;

    @Test
    void supportsRegistrationAndAllMySqlUpserts() {
        User user = new User(null, "mysql-user", null, "password", UserRole.USER);
        user.setDisplayName("MySQL User");
        userRepository.save(user);
        assertThat(user.getId()).isNotNull();
        userRepository.updateMemberType(user.getId(), MemberType.ACTIVE_TEAM);

        CfProblem first = new CfProblem(
                "100-A", 100, "A", "First", 1200, "math", false,
                null, 10, "https://codeforces.com/problemset/problem/100/A"
        );
        CfProblem updated = new CfProblem(
                "100-A", 100, "A", "Updated", 1300, "math,greedy", false,
                null, 20, "https://codeforces.com/problemset/problem/100/A"
        );
        dailyProblemRepository.upsertProblems(List.of(first));
        dailyProblemRepository.upsertProblems(List.of(updated));
        assertThat(dailyProblemRepository.countProblems()).isGreaterThanOrEqualTo(1);
        assertThat(dailyProblemRepository.findRandomProblem(1200, 1400)).isPresent();
        assertThat(dailyProblemRepository.findRandomProblem(1200, 1400, List.of("math", "greedy")))
                .get()
                .extracting(CfProblem::problemKey)
                .isEqualTo("100-A");
        assertThat(dailyProblemRepository.findRandomProblem(1200, 1400, List.of("graphs"))).isEmpty();
        assertThat(dailyProblemRepository.findAvailableProblemTags()).contains("greedy", "math");

        LocalDate date = LocalDate.of(2026, 8, 1);
        dailyProblemRepository.insertDailySlot(date, "easy", updated, "test");
        dailyProblemRepository.saveUserDailySlotStatus(
                user.getId(), date, "easy", "100-A", 1L, "OK", 1200
        );
        dailyProblemRepository.saveUserDailySlotStatus(
                user.getId(), date, "easy", "100-A", 2L, "OK", 1300
        );
        assertThat(dailyProblemRepository.findUserDailySlotStatus(user.getId(), date, "100-A"))
                .get().extracting(status -> status.score()).isEqualTo(1300);

        dailyProblemRepository.upsertUserDailyStatus(user.getId(), date, 1L, "OK", 1200);
        dailyProblemRepository.upsertUserDailyStatus(user.getId(), date, 2L, "OK", 1300);
        assertThat(dailyProblemRepository.findUserDailyStatus(user.getId(), date))
                .get().extracting(status -> status.score()).isEqualTo(1300);

        dailyProblemRepository.insertPracticeDraw(user.getId(), date, updated);
        assertThat(trainingDashboardRepository.findActiveTeamDailyActivity(date.minusDays(1), date))
                .extracting(TrainingDashboardRepository.DailyActivity::userId)
                .contains(user.getId());
        assertThat(trainingDashboardRepository.findActiveTeamPracticeSummary(date.minusDays(1), date))
                .singleElement()
                .satisfies(summary -> {
                    assertThat(summary.userId()).isEqualTo(user.getId());
                    assertThat(summary.drawCount()).isEqualTo(1);
                    assertThat(summary.solvedCount()).isZero();
                });
        assertThat(trainingDashboardRepository.findActiveTeamDailyExport(date.minusDays(1)))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.userId()).isEqualTo(user.getId());
                    assertThat(row.problemKey()).isEqualTo("100-A");
                    assertThat(row.completed()).isTrue();
                });
        ratingHistoryRepository.replaceForUser(user.getId(), List.of(
                new CodeforcesApiService.CodeforcesRatingChange(
                        2048L, "Codeforces Round", 321, 1450, 1500, 1_754_006_400L
                )
        ));
        assertThat(ratingHistoryRepository.findActiveTeamHistory(1_700_000_000L))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.userId()).isEqualTo(user.getId());
                    assertThat(row.ratingChange()).isEqualTo(50);
                    assertThat(row.contestId()).isEqualTo(2048L);
                });
        assertThat(ratingHistoryRepository.existsByUserId(user.getId())).isTrue();

        Long listId = problemListRepository.createList(user.getId(), "区间 DP", "专题训练", false);
        assertThat(listId).isNotNull();
        assertThat(problemListRepository.findVisible(user.getId()))
                .extracting(com.whut.training.domain.dto.ProblemListSummary::name)
                .contains("区间 DP");
        Long itemId = problemListRepository.addItem(
                listId, "Updated", "https://codeforces.com/problemset/problem/100/A",
                "重做", "100-A", 1300, "math,greedy"
        );
        assertThat(problemListRepository.findItems(listId))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.id()).isEqualTo(itemId);
                    assertThat(item.problemKey()).isEqualTo("100-A");
                    assertThat(item.rating()).isEqualTo(1300);
                });
        assertThat(problemListRepository.findProblemMetadata("100-A"))
                .get()
                .extracting(ProblemListRepository.ProblemMetadata::name)
                .isEqualTo("Updated");

        pushPoolRepository.setDailyPush(date, 10L);
        pushPoolRepository.setDailyPush(date, 11L);
        assertThat(pushPoolRepository.findDailyPushId(date)).contains(11L);

        CodeforcesOverview overview = new CodeforcesOverview(
                "mysql-handle", 1500, 1600, 3, 5, 4, 2,
                100L, false, Instant.parse("2026-08-01T00:00:00Z"), false,
                List.of(), List.of()
        );
        snapshotRepository.save(user.getId(), "mysql-handle", overview);
        snapshotRepository.save(user.getId(), "mysql-handle", overview);
        assertThat(snapshotRepository.find(user.getId(), "mysql-handle")).isPresent();
        assertThat(snapshotRepository.findAllForActiveTeam()).containsKey(user.getId());
    }
}

package com.whut.training.controller;

import com.whut.training.domain.entity.CfProblem;
import com.whut.training.domain.entity.User;
import com.whut.training.domain.entity.UserPracticeDraw;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.repository.AuthTokenSessionRepository;
import com.whut.training.repository.AuthTokenSessionRepository.AuthTokenSession;
import com.whut.training.repository.DailyProblemRepository;
import com.whut.training.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@AutoConfigureMockMvc
class AdminTrainingControllerIntegrationTest {

    private static final Path TEST_DB = Paths.get(
            System.getProperty("java.io.tmpdir"),
            "whut-training-admin-dashboard-test-" + System.nanoTime() + ".db"
    ).toAbsolutePath();

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + TEST_DB.toString().replace("\\", "/"));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DailyProblemRepository dailyProblemRepository;

    @Autowired
    private AuthTokenSessionRepository authTokenSessionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM auth_token_session");
        jdbcTemplate.update("DELETE FROM user_daily_status");
        jdbcTemplate.update("DELETE FROM user_practice_draw");
        jdbcTemplate.update("DELETE FROM daily_problem");
        jdbcTemplate.update("DELETE FROM cf_problem");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void returnsOverviewDailyRecordsAndUserTimelineForAdmin() throws Exception {
        User admin = createUser("admin", UserRole.ADMIN, 0);
        User alice = createUser("alice", UserRole.USER, 1600);
        User bob = createUser("bob", UserRole.USER, 1200);

        String[] adminTokens = issueTokens(admin);

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        CfProblem todayDailyProblem = createProblem("2000-A", 2000, "A", "Today Daily", 1500);
        CfProblem yesterdayDailyProblem = createProblem("1999-B", 1999, "B", "Yesterday Daily", 1400);
        CfProblem practiceProblem = createProblem("1888-C", 1888, "C", "Practice C", 1700);
        CfProblem practiceProblemTwo = createProblem("1777-D", 1777, "D", "Practice D", 1500);

        dailyProblemRepository.insertDailyProblem(today, todayDailyProblem, "admin");
        dailyProblemRepository.insertDailyProblem(yesterday, yesterdayDailyProblem, "scheduler");

        dailyProblemRepository.saveUserDailyStatus(alice.getId(), today, 10001L, "OK", 1);
        dailyProblemRepository.saveUserDailyStatus(alice.getId(), yesterday, 10002L, "OK", 1);
        userRepository.updateUserStreakStats(alice.getId(), 2, 4);
        userRepository.updateUserStreakStats(bob.getId(), 1, 3);

        UserPracticeDraw alicePractice = dailyProblemRepository.insertPracticeDraw(alice.getId(), today, practiceProblem);
        dailyProblemRepository.updatePracticeCheck(alicePractice.id(), alice.getId(), 20001L, "OK");

        UserPracticeDraw bobPractice = dailyProblemRepository.insertPracticeDraw(bob.getId(), today, practiceProblemTwo);
        dailyProblemRepository.updatePracticeCheck(bobPractice.id(), bob.getId(), 20002L, "WRONG_ANSWER");

        mockMvc.perform(get("/api/admin/training/overview")
                        .header("Authorization", "Bearer " + adminTokens[0])
                        .header("X-Refresh-Token", adminTokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.date").value(today.toString()))
                .andExpect(jsonPath("$.data.problem.name").value("Today Daily"))
                .andExpect(jsonPath("$.data.totalUsers").value(3))
                .andExpect(jsonPath("$.data.activeUsers").value(2))
                .andExpect(jsonPath("$.data.dailyCheckInCount").value(1))
                .andExpect(jsonPath("$.data.pendingDailyUserCount").value(2))
                .andExpect(jsonPath("$.data.practiceDrawCount").value(2))
                .andExpect(jsonPath("$.data.practiceCheckCount").value(2))
                .andExpect(jsonPath("$.data.todayStreakUserCount").value(1))
                .andExpect(jsonPath("$.data.maxCurrentStreakDays").value(2))
                .andExpect(jsonPath("$.data.maxLongestStreakDays").value(4))
                .andExpect(jsonPath("$.data.averageCurrentStreakDays").value(1.5d));

        mockMvc.perform(get("/api/admin/training/daily-records")
                        .param("startDate", yesterday.toString())
                        .param("endDate", today.toString())
                        .param("page", "1")
                        .param("pageSize", "10")
                        .header("Authorization", "Bearer " + adminTokens[0])
                        .header("X-Refresh-Token", adminTokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.entries[0].date").value(today.toString()))
                .andExpect(jsonPath("$.data.entries[0].problem.problemKey").value("2000-A"))
                .andExpect(jsonPath("$.data.entries[0].dailyCheckInCount").value(1))
                .andExpect(jsonPath("$.data.entries[0].practiceCheckCount").value(2))
                .andExpect(jsonPath("$.data.entries[1].date").value(yesterday.toString()))
                .andExpect(jsonPath("$.data.entries[1].dailyCheckInCount").value(1));

        mockMvc.perform(get("/api/admin/training/daily-records/" + today)
                        .header("Authorization", "Bearer " + adminTokens[0])
                        .header("X-Refresh-Token", adminTokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.problem.problemKey").value("2000-A"))
                .andExpect(jsonPath("$.data.checkIns.length()").value(1))
                .andExpect(jsonPath("$.data.checkIns[0].username").value("alice"))
                .andExpect(jsonPath("$.data.practiceChecks.length()").value(2));

        mockMvc.perform(get("/api/admin/training/users")
                        .param("keyword", "ali")
                        .param("page", "1")
                        .param("pageSize", "10")
                        .header("Authorization", "Bearer " + adminTokens[0])
                        .header("X-Refresh-Token", adminTokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.entries[0].username").value("alice"))
                .andExpect(jsonPath("$.data.entries[0].dailyCheckInCount").value(2))
                .andExpect(jsonPath("$.data.entries[0].practiceCheckCount").value(1))
                .andExpect(jsonPath("$.data.entries[0].currentStreakDays").value(2))
                .andExpect(jsonPath("$.data.entries[0].longestStreakDays").value(4));

        mockMvc.perform(get("/api/admin/training/users/" + alice.getId() + "/timeline")
                        .param("limit", "10")
                        .header("Authorization", "Bearer " + adminTokens[0])
                        .header("X-Refresh-Token", adminTokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(alice.getId()))
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.currentStreakDays").value(2))
                .andExpect(jsonPath("$.data.longestStreakDays").value(4))
                .andExpect(jsonPath("$.data.entries.length()").value(3))
                .andExpect(jsonPath("$.data.entries[0].activityType").value("PRACTICE"))
                .andExpect(jsonPath("$.data.entries[1].activityType").value("DAILY"));
    }

    @Test
    void rejectsNonAdminUsers() throws Exception {
        User user = createUser("alice", UserRole.USER, 1000);
        String[] tokens = issueTokens(user);

        mockMvc.perform(get("/api/admin/training/overview")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("admin role required"));
    }

    @Test
    void rejectsUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/admin/training/overview"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void returnsEmptyDailyRecordPagesWhenNoTrainingDataExists() throws Exception {
        User admin = createUser("admin", UserRole.ADMIN, 0);
        String[] tokens = issueTokens(admin);
        LocalDate today = LocalDate.now();

        mockMvc.perform(get("/api/admin/training/overview")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.problem").value(nullValue()))
                .andExpect(jsonPath("$.data.dailyCheckInCount").value(0))
                .andExpect(jsonPath("$.data.practiceCheckCount").value(0))
                .andExpect(jsonPath("$.data.todayStreakUserCount").value(0))
                .andExpect(jsonPath("$.data.maxCurrentStreakDays").value(0))
                .andExpect(jsonPath("$.data.maxLongestStreakDays").value(0))
                .andExpect(jsonPath("$.data.averageCurrentStreakDays").value(0.0d));

        mockMvc.perform(get("/api/admin/training/daily-records")
                        .param("startDate", today.minusDays(1).toString())
                        .param("endDate", today.toString())
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.entries.length()").value(0));
    }

    private User createUser(String username, UserRole role, int score) {
        User user = new User(
                null,
                username,
                username + "@example.com",
                "password123",
                role
        );
        userRepository.save(user);
        userRepository.updateUserScore(user.getId(), score);
        user.setScore(score);
        return user;
    }

    private String[] issueTokens(User user) {
        String accessToken = "access-" + user.getUsername();
        String refreshToken = "refresh-" + user.getUsername();
        authTokenSessionRepository.save(new AuthTokenSession(
                user.getId(),
                accessToken,
                refreshToken,
                Instant.now().plusSeconds(3600).getEpochSecond(),
                Instant.now().plusSeconds(7200).getEpochSecond()
        ));
        return new String[]{accessToken, refreshToken};
    }

    private CfProblem createProblem(String problemKey, int contestId, String problemIndex, String name, int rating) {
        return new CfProblem(
                problemKey,
                contestId,
                problemIndex,
                name,
                rating,
                "dp,graphs",
                false,
                null,
                100,
                "https://codeforces.com/problemset/problem/" + contestId + "/" + problemIndex
        );
    }
}

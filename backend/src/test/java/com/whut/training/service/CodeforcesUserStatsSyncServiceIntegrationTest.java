package com.whut.training.service;

import com.whut.training.domain.entity.CfProblem;
import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.repository.DailyProblemRepository;
import com.whut.training.repository.UserRepository;
import com.whut.training.service.impl.CodeforcesUserStatsSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
class CodeforcesUserStatsSyncServiceIntegrationTest {

    private static final String TEST_DB_NAME = "whut-training-codeforces-sync-test-" + System.nanoTime();

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:" + TEST_DB_NAME + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
    }

    @Autowired
    private CodeforcesUserStatsSyncService syncService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DailyProblemRepository dailyProblemRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private CodeforcesApiService codeforcesApiService;

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM auth_token_session");
        jdbcTemplate.update("DELETE FROM user_daily_status");
        jdbcTemplate.update("DELETE FROM user_practice_draw");
        jdbcTemplate.update("DELETE FROM daily_problem");
        jdbcTemplate.update("DELETE FROM cf_problem");
        jdbcTemplate.update("DELETE FROM users");
        reset(codeforcesApiService);
    }

    @Test
    void syncsSolvedAndHardSolvedProblemCounts() {
        User alice = createUser("alice");
        User admin = createUser("admin");

        dailyProblemRepository.upsertProblems(List.of(
                new CfProblem(
                        "1000-A",
                        1000,
                        "A",
                        "Very Hard Problem",
                        2301,
                        "",
                        false,
                        null,
                        1,
                        "https://codeforces.com/problemset/problem/1000/A"
                ),
                new CfProblem(
                        "1003-D",
                        1003,
                        "D",
                        "Beginner Problem",
                        1300,
                        "",
                        false,
                        null,
                        1,
                        "https://codeforces.com/problemset/problem/1003/D"
                )
        ));

        when(codeforcesApiService.getUserInfo("alice")).thenReturn(Optional.of(
                new CodeforcesApiService.CodeforcesUserProfile(1500, 1700, false, 0L, "avatar")
        ));
        when(codeforcesApiService.getUserInfo("admin")).thenReturn(Optional.empty());
        when(codeforcesApiService.fetchUserSubmissions(eq("alice"), eq(1), eq(1000))).thenReturn(Optional.of(List.of(
                new CodeforcesApiService.UserSubmission(1L, 1000, "A", "OK", null, Instant.now()),
                new CodeforcesApiService.UserSubmission(2L, 1000, "A", "OK", null, Instant.now()),
                new CodeforcesApiService.UserSubmission(3L, 1001, "B", "OK", 1500, Instant.now()),
                new CodeforcesApiService.UserSubmission(4L, 1003, "D", "OK", null, Instant.now()),
                new CodeforcesApiService.UserSubmission(5L, 1002, "C", "WRONG_ANSWER", 2300, Instant.now())
        )));

        syncService.syncAllUsersOnce();

        User syncedAlice = userRepository.findById(alice.getId()).orElseThrow();
        assertEquals(3, syncedAlice.getSolvedProblemCount());
        assertEquals(1, syncedAlice.getHardSolvedProblemCount());
        assertEquals(1, syncedAlice.getSolved800To1400Count());
        assertEquals(1, syncedAlice.getSolved1500To2200Count());
        assertEquals(1, syncedAlice.getSolvedAbove2200Count());

        User skippedAdmin = userRepository.findById(admin.getId()).orElseThrow();
        assertEquals(0, skippedAdmin.getSolvedProblemCount());
        assertEquals(0, skippedAdmin.getHardSolvedProblemCount());
        assertEquals(0, skippedAdmin.getSolved800To1400Count());
        assertEquals(0, skippedAdmin.getSolved1500To2200Count());
        assertEquals(0, skippedAdmin.getSolvedAbove2200Count());

        verify(codeforcesApiService, never()).fetchUserSubmissions(eq("admin"), eq(1), eq(1000));
    }

    @Test
    void seedsProblemPoolBeforeCalculatingHardSolvedCounts() {
        User alice = createUser("alice");

        when(codeforcesApiService.fetchProblemSet()).thenReturn(List.of(
                new CfProblem(
                        "1000-A",
                        1000,
                        "A",
                        "Seeded Hard Problem",
                        2301,
                        "",
                        false,
                        null,
                        1,
                        "https://codeforces.com/problemset/problem/1000/A"
                )
        ));
        when(codeforcesApiService.getUserInfo("alice")).thenReturn(Optional.of(
                new CodeforcesApiService.CodeforcesUserProfile(1500, 1700, false, 0L, "avatar")
        ));
        when(codeforcesApiService.fetchUserSubmissions(eq("alice"), eq(1), eq(1000))).thenReturn(Optional.of(List.of(
                new CodeforcesApiService.UserSubmission(1L, 1000, "A", "OK", null, Instant.now())
        )));

        syncService.syncAllUsersOnce();

        User syncedAlice = userRepository.findById(alice.getId()).orElseThrow();
        assertEquals(1, dailyProblemRepository.countProblems());
        assertEquals(1, syncedAlice.getSolvedProblemCount());
        assertEquals(1, syncedAlice.getHardSolvedProblemCount());
        assertEquals(0, syncedAlice.getSolved800To1400Count());
        assertEquals(0, syncedAlice.getSolved1500To2200Count());
        assertEquals(1, syncedAlice.getSolvedAbove2200Count());
    }

    private User createUser(String username) {
        User user = new User(
                null,
                username,
                username + "@example.com",
                "password123",
                UserRole.USER
        );
        userRepository.save(user);
        assertTrue(user.getId() != null);
        return user;
    }
}

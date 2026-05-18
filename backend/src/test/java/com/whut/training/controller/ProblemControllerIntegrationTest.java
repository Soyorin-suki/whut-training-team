package com.whut.training.controller;

import com.whut.training.domain.entity.CfProblem;
import com.whut.training.domain.entity.User;
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
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@AutoConfigureMockMvc
class ProblemControllerIntegrationTest {

    private static final Path TEST_DB = Paths.get(
            System.getProperty("java.io.tmpdir"),
            "whut-training-problem-controller-test-" + System.nanoTime() + ".db"
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
        jdbcTemplate.update("DELETE FROM problem_comment");
        jdbcTemplate.update("DELETE FROM daily_problem_comment");
        jdbcTemplate.update("DELETE FROM problem_favorite");
        jdbcTemplate.update("DELETE FROM problem_like");
        jdbcTemplate.update("DELETE FROM user_daily_status");
        jdbcTemplate.update("DELETE FROM user_practice_draw");
        jdbcTemplate.update("DELETE FROM daily_problem");
        jdbcTemplate.update("DELETE FROM cf_problem");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void publicRequestCanReadProblemDetail() throws Exception {
        seedProblem(new CfProblem(
                "2000-A", 2000, "A", "Problem A", 1500, "dp,graphs", false, null, 100,
                "https://codeforces.com/problemset/problem/2000/A"
        ));

        mockMvc.perform(get("/api/problems/2000-A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.problemKey").value("2000-A"))
                .andExpect(jsonPath("$.data.name").value("Problem A"))
                .andExpect(jsonPath("$.data.likeCount").value(0))
                .andExpect(jsonPath("$.data.likedByMe").value(false))
                .andExpect(jsonPath("$.data.favoritedByMe").value(false));
    }

    @Test
    void authenticatedRequestIncludesLikeAndFavoriteState() throws Exception {
        User alice = createUser("alice");
        String[] tokens = issueTokens(alice);
        seedProblem(new CfProblem(
                "2000-A", 2000, "A", "Problem A", 1500, "dp,graphs", false, null, 100,
                "https://codeforces.com/problemset/problem/2000/A"
        ));
        jdbcTemplate.update(
                "INSERT INTO problem_like (user_id, problem_key, created_at) VALUES (?, ?, ?)",
                alice.getId(), "2000-A", "2026-05-17T09:00:00+08:00"
        );
        jdbcTemplate.update(
                "INSERT INTO problem_favorite (user_id, problem_key, created_at) VALUES (?, ?, ?)",
                alice.getId(), "2000-A", "2026-05-17T09:05:00+08:00"
        );

        mockMvc.perform(get("/api/problems/2000-A")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.likeCount").value(1))
                .andExpect(jsonPath("$.data.likedByMe").value(true))
                .andExpect(jsonPath("$.data.favoritedByMe").value(true))
                .andExpect(jsonPath("$.data.favoritedAt").value("2026-05-17T09:05:00+08:00"));
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

    private void seedProblem(CfProblem problem) {
        dailyProblemRepository.upsertProblems(List.of(problem));
        dailyProblemRepository.insertDailyProblem(LocalDate.now(), problem, "admin");
    }
}

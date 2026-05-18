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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@AutoConfigureMockMvc
class ProblemFavoriteControllerIntegrationTest {

    private static final Path TEST_DB = Paths.get(
            System.getProperty("java.io.tmpdir"),
            "whut-training-problem-favorite-controller-test-" + System.nanoTime() + ".db"
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
        jdbcTemplate.update("DELETE FROM problem_favorite");
        jdbcTemplate.update("DELETE FROM problem_like");
        jdbcTemplate.update("DELETE FROM user_daily_status");
        jdbcTemplate.update("DELETE FROM user_practice_draw");
        jdbcTemplate.update("DELETE FROM daily_problem");
        jdbcTemplate.update("DELETE FROM cf_problem");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void favoriteFlowsThroughDailyPracticeHistoryAndMine() throws Exception {
        User user = createUser("alice");
        String[] tokens = issueTokens(user);
        LocalDate today = LocalDate.now();

        CfProblem sharedProblem = createProblem("2000-A", 2000, "A", "Shared Problem", 1500);
        dailyProblemRepository.upsertProblems(List.of(sharedProblem));
        dailyProblemRepository.insertDailyProblem(today, sharedProblem, "admin");
        UserPracticeDraw draw = dailyProblemRepository.insertPracticeDraw(user.getId(), today, sharedProblem);
        dailyProblemRepository.updatePracticeCheck(draw.id(), user.getId(), 30001L, "OK");

        mockMvc.perform(post("/api/problem-favorite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"problemKey\":\"2000-A\"}")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.problemKey").value("2000-A"))
                .andExpect(jsonPath("$.data.favoritedByMe").value(true))
                .andExpect(jsonPath("$.data.favoritedAt").isNotEmpty());

        mockMvc.perform(post("/api/problem-favorite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"problemKey\":\"2000-A\"}")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.favoritedByMe").value(true))
                .andExpect(jsonPath("$.data.favoritedAt").isNotEmpty());

        mockMvc.perform(get("/api/daily-problem/today")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.problem.problemKey").value("2000-A"))
                .andExpect(jsonPath("$.data.problem.favoritedByMe").value(true))
                .andExpect(jsonPath("$.data.problem.favoritedAt").isNotEmpty());

        mockMvc.perform(get("/api/daily-problem/history")
                        .param("limit", "1")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].problemKey").value("2000-A"))
                .andExpect(jsonPath("$.data[0].favoritedByMe").value(true))
                .andExpect(jsonPath("$.data[0].favoritedAt").isNotEmpty());

        mockMvc.perform(post("/api/practice/draw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.problem.problemKey").value("2000-A"))
                .andExpect(jsonPath("$.data.problem.favoritedByMe").value(true))
                .andExpect(jsonPath("$.data.problem.favoritedAt").isNotEmpty());

        mockMvc.perform(get("/api/practice/history")
                        .param("limit", "1")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].problemKey").value("2000-A"))
                .andExpect(jsonPath("$.data[0].favoritedByMe").value(true))
                .andExpect(jsonPath("$.data[0].favoritedAt").isNotEmpty());

        mockMvc.perform(get("/api/problem-favorite/mine")
                        .param("page", "1")
                        .param("limit", "50")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.limit").value(50))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].problemKey").value("2000-A"))
                .andExpect(jsonPath("$.data.items[0].sourceType").value("DAILY"))
                .andExpect(jsonPath("$.data.items[0].favoritedAt").isNotEmpty());

        mockMvc.perform(delete("/api/problem-favorite/2000-A")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.problemKey").value("2000-A"))
                .andExpect(jsonPath("$.data.favoritedByMe").value(false))
                .andExpect(jsonPath("$.data.favoritedAt").isEmpty());

        mockMvc.perform(delete("/api/problem-favorite/2000-A")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.favoritedByMe").value(false))
                .andExpect(jsonPath("$.data.favoritedAt").isEmpty());

        mockMvc.perform(get("/api/problem-favorite/mine")
                        .param("page", "1")
                        .param("limit", "50")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void rejectsUnknownProblemKey() throws Exception {
        User user = createUser("alice");
        String[] tokens = issueTokens(user);

        mockMvc.perform(post("/api/problem-favorite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"problemKey\":\"2999-Z\"}")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("problem not found"));
    }

    @Test
    void rejectsUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/problem-favorite/mine"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
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

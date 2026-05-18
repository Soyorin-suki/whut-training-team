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
class ProblemLikeControllerIntegrationTest {

    private static final String TEST_DB_NAME = "whut-training-problem-like-test-" + System.nanoTime();

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:" + TEST_DB_NAME + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
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
    void likeAndUnlikeAreSharedAcrossDailyPracticeAndHistory() throws Exception {
        User user = createUser("alice");
        String[] tokens = issueTokens(user);
        LocalDate today = LocalDate.now();

        CfProblem sharedProblem = createProblem("2000-A", 2000, "A", "Shared Problem", 1500);
        dailyProblemRepository.upsertProblems(List.of(sharedProblem));
        dailyProblemRepository.insertDailyProblem(today, sharedProblem, "admin");
        UserPracticeDraw draw = dailyProblemRepository.insertPracticeDraw(user.getId(), today, sharedProblem);
        dailyProblemRepository.updatePracticeCheck(draw.id(), user.getId(), 30001L, "OK");

        mockMvc.perform(post("/api/problem-like")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"problemKey\":\"2000-A\"}")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.problemKey").value("2000-A"))
                .andExpect(jsonPath("$.data.likeCount").value(1))
                .andExpect(jsonPath("$.data.likedByMe").value(true));

        mockMvc.perform(post("/api/problem-like")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"problemKey\":\"2000-A\"}")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.likeCount").value(1))
                .andExpect(jsonPath("$.data.likedByMe").value(true));

        mockMvc.perform(get("/api/daily-problem/today")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.problem.problemKey").value("2000-A"))
                .andExpect(jsonPath("$.data.problem.likeCount").value(1))
                .andExpect(jsonPath("$.data.problem.likedByMe").value(true));

        mockMvc.perform(get("/api/daily-problem/history")
                        .param("limit", "1")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].problemKey").value("2000-A"))
                .andExpect(jsonPath("$.data[0].likeCount").value(1))
                .andExpect(jsonPath("$.data[0].likedByMe").value(true));

        mockMvc.perform(post("/api/practice/draw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.problem.problemKey").value("2000-A"))
                .andExpect(jsonPath("$.data.problem.likeCount").value(1))
                .andExpect(jsonPath("$.data.problem.likedByMe").value(true));

        mockMvc.perform(get("/api/practice/history")
                        .param("limit", "1")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].problemKey").value("2000-A"))
                .andExpect(jsonPath("$.data[0].likeCount").value(1))
                .andExpect(jsonPath("$.data[0].likedByMe").value(true));

        mockMvc.perform(delete("/api/problem-like/2000-A")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.likeCount").value(0))
                .andExpect(jsonPath("$.data.likedByMe").value(false));

        mockMvc.perform(delete("/api/problem-like/2000-A")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.likeCount").value(0))
                .andExpect(jsonPath("$.data.likedByMe").value(false));

        mockMvc.perform(get("/api/daily-problem/today")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.problem.likeCount").value(0))
                .andExpect(jsonPath("$.data.problem.likedByMe").value(false));
    }

    @Test
    void rejectsUnknownProblemKey() throws Exception {
        User user = createUser("alice");
        String[] tokens = issueTokens(user);

        mockMvc.perform(post("/api/problem-like")
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
        mockMvc.perform(post("/api/problem-like")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"problemKey\":\"2000-A\"}"))
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

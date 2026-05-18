package com.whut.training.controller;

import com.whut.training.domain.entity.CfProblem;
import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.repository.AuthTokenSessionRepository;
import com.whut.training.repository.AuthTokenSessionRepository.AuthTokenSession;
import com.whut.training.repository.DailyProblemRepository;
import com.whut.training.repository.UserRepository;
import com.whut.training.service.ProblemCommentMigrationService;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@AutoConfigureMockMvc
class ProblemCommentControllerIntegrationTest {

    private static final Path TEST_DB = Paths.get(
            System.getProperty("java.io.tmpdir"),
            "whut-training-problem-comment-controller-test-" + System.nanoTime() + ".db"
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
    private ProblemCommentMigrationService problemCommentMigrationService;

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
    void publicGetReturnsMigratedCommentsForSameProblemAcrossLegacyDailyInstances() throws Exception {
        User alice = createUser("alice");
        User bob = createUser("bob");
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        seedProblem(today, new CfProblem(
                "2000-A", 2000, "A", "Problem A", 1500, "dp", false, null, 100,
                "https://codeforces.com/problemset/problem/2000/A"
        ));

        jdbcTemplate.update(
                "INSERT INTO daily_problem_comment (id, daily_problem_date, problem_key, user_id, reply_comment_id, content, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                1L, yesterday.toString(), "2000-A", alice.getId(), null, "Yesterday root", "2026-05-16T09:00:00+08:00"
        );
        jdbcTemplate.update(
                "INSERT INTO daily_problem_comment (id, daily_problem_date, problem_key, user_id, reply_comment_id, content, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                2L, today.toString(), "2000-A", bob.getId(), null, "Today root", "2026-05-17T09:00:00+08:00"
        );

        problemCommentMigrationService.migrateLegacyComments();

        mockMvc.perform(get("/api/problem-comments/2000-A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].content").value("Today root"))
                .andExpect(jsonPath("$.data[1].content").value("Yesterday root"));
    }

    @Test
    void loggedInUserCanCreateRootCommentAndReply() throws Exception {
        User alice = createUser("alice");
        User bob = createUser("bob");
        String[] aliceTokens = issueTokens(alice);
        String[] bobTokens = issueTokens(bob);
        seedProblem(LocalDate.now(), new CfProblem(
                "2000-A", 2000, "A", "Problem A", 1500, "dp", false, null, 100,
                "https://codeforces.com/problemset/problem/2000/A"
        ));

        mockMvc.perform(post("/api/problem-comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "problemKey": "2000-A",
                                  "content": "First root",
                                  "replyCommentId": null
                                }
                                """)
                        .header("Authorization", "Bearer " + aliceTokens[0])
                        .header("X-Refresh-Token", aliceTokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").value("First root"))
                .andExpect(jsonPath("$.data.replyCommentId").value(nullValue()))
                .andExpect(jsonPath("$.data.author.username").value("alice"));

        Long rootId = jdbcTemplate.queryForObject(
                "SELECT id FROM problem_comment WHERE content = 'First root'",
                Long.class
        );

        mockMvc.perform(post("/api/problem-comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "problemKey": "2000-A",
                                  "content": "Reply once",
                                  "replyCommentId": %d
                                }
                                """.formatted(rootId))
                        .header("Authorization", "Bearer " + bobTokens[0])
                        .header("X-Refresh-Token", bobTokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.replyCommentId").value(rootId))
                .andExpect(jsonPath("$.data.replyToUsername").value("alice"))
                .andExpect(jsonPath("$.data.author.username").value("bob"));

        mockMvc.perform(get("/api/problem-comments/2000-A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].content").value("First root"))
                .andExpect(jsonPath("$.data[0].replies", hasSize(1)))
                .andExpect(jsonPath("$.data[0].replies[0].content").value("Reply once"))
                .andExpect(jsonPath("$.data[0].replies[0].replyToUsername").value("alice"));
    }

    @Test
    void rejectsReplyToCommentFromAnotherProblem() throws Exception {
        User alice = createUser("alice");
        String[] tokens = issueTokens(alice);
        seedProblem(LocalDate.now(), new CfProblem(
                "2000-A", 2000, "A", "Problem A", 1500, "dp", false, null, 100,
                "https://codeforces.com/problemset/problem/2000/A"
        ));
        seedProblem(LocalDate.now().plusDays(1), new CfProblem(
                "2000-B", 2000, "B", "Problem B", 1600, "graphs", false, null, 100,
                "https://codeforces.com/problemset/problem/2000/B"
        ));

        jdbcTemplate.update(
                "INSERT INTO problem_comment (id, problem_key, user_id, reply_comment_id, content, created_at, legacy_comment_id) VALUES (?, ?, ?, ?, ?, ?, ?)",
                7L, "2000-B", alice.getId(), null, "Other root", "2026-05-17T09:00:00+08:00", null
        );

        mockMvc.perform(post("/api/problem-comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "problemKey": "2000-A",
                                  "content": "Should fail",
                                  "replyCommentId": 7
                                }
                                """)
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void rejectsUnauthorizedCreate() throws Exception {
        mockMvc.perform(post("/api/problem-comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "problemKey": "2000-A",
                                  "content": "Hello",
                                  "replyCommentId": null
                                }
                                """))
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

    private void seedProblem(LocalDate date, CfProblem problem) {
        dailyProblemRepository.upsertProblems(List.of(problem));
        dailyProblemRepository.insertDailyProblem(date, problem, "admin");
    }
}

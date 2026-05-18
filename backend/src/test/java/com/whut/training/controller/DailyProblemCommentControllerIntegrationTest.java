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
class DailyProblemCommentControllerIntegrationTest {

    private static final String TEST_DB_NAME = "whut-training-daily-problem-comment-controller-test-" + System.nanoTime();

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
    void getTodayCommentsReturnsTwoLevelStructure() throws Exception {
        User alice = createUser("alice");
        User bob = createUser("bob");
        User carol = createUser("carol");
        String[] tokens = issueTokens(alice);
        LocalDate today = LocalDate.now();

        seedTodayProblem(today);
        jdbcTemplate.update(
                "INSERT INTO daily_problem_comment (id, daily_problem_date, problem_key, user_id, reply_comment_id, content, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                1L, today.toString(), "2000-A", alice.getId(), null, "Root", "2026-05-17T09:00:00+08:00"
        );
        jdbcTemplate.update(
                "INSERT INTO daily_problem_comment (id, daily_problem_date, problem_key, user_id, reply_comment_id, content, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                2L, today.toString(), "2000-A", bob.getId(), 1L, "Reply root", "2026-05-17T09:05:00+08:00"
        );
        jdbcTemplate.update(
                "INSERT INTO daily_problem_comment (id, daily_problem_date, problem_key, user_id, reply_comment_id, content, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                3L, today.toString(), "2000-A", carol.getId(), 2L, "Reply reply", "2026-05-17T09:10:00+08:00"
        );
        jdbcTemplate.update(
                "INSERT INTO daily_problem_comment (id, daily_problem_date, problem_key, user_id, reply_comment_id, content, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                4L, today.toString(), "2000-A", bob.getId(), null, "New root", "2026-05-17T10:00:00+08:00"
        );

        mockMvc.perform(get("/api/daily-problem/comments/today")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].id").value(4))
                .andExpect(jsonPath("$.data[0].replies", hasSize(0)))
                .andExpect(jsonPath("$.data[1].id").value(1))
                .andExpect(jsonPath("$.data[1].author.username").value("alice"))
                .andExpect(jsonPath("$.data[1].replies", hasSize(2)))
                .andExpect(jsonPath("$.data[1].replies[0].id").value(2))
                .andExpect(jsonPath("$.data[1].replies[0].replyToUsername").value("alice"))
                .andExpect(jsonPath("$.data[1].replies[1].id").value(3))
                .andExpect(jsonPath("$.data[1].replies[1].replyToUsername").value("bob"));
    }

    @Test
    void getCommentsByHistoricalInstanceReturnsMatchingThreadOnly() throws Exception {
        User alice = createUser("alice");
        User bob = createUser("bob");
        String[] tokens = issueTokens(alice);
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        seedTodayProblem(today);
        jdbcTemplate.update(
                "INSERT INTO daily_problem_comment (id, daily_problem_date, problem_key, user_id, reply_comment_id, content, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                10L, yesterday.toString(), "1999-B", alice.getId(), null, "Historical root", "2026-05-16T09:00:00+08:00"
        );
        jdbcTemplate.update(
                "INSERT INTO daily_problem_comment (id, daily_problem_date, problem_key, user_id, reply_comment_id, content, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                11L, yesterday.toString(), "1999-B", bob.getId(), 10L, "Historical reply", "2026-05-16T09:10:00+08:00"
        );
        jdbcTemplate.update(
                "INSERT INTO daily_problem_comment (id, daily_problem_date, problem_key, user_id, reply_comment_id, content, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                12L, today.toString(), "2000-A", alice.getId(), null, "Today root", "2026-05-17T09:00:00+08:00"
        );

        mockMvc.perform(get("/api/daily-problem/comments")
                        .param("date", yesterday.toString())
                        .param("problemKey", "1999-B")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].problemKey").value("1999-B"))
                .andExpect(jsonPath("$.data[0].content").value("Historical root"))
                .andExpect(jsonPath("$.data[0].replies", hasSize(1)))
                .andExpect(jsonPath("$.data[0].replies[0].id").value(11))
                .andExpect(jsonPath("$.data[0].replies[0].replyToUsername").value("alice"));
    }

    @Test
    void getCommentArchivesIncludesHistoricalCommentedInstances() throws Exception {
        User alice = createUser("alice");
        String[] tokens = issueTokens(alice);
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        seedTodayProblem(today);
        dailyProblemRepository.upsertProblems(List.of(new CfProblem(
                "1999-B",
                1999,
                "B",
                "Archived Problem",
                1600,
                "dp",
                false,
                null,
                100,
                "https://codeforces.com/problemset/problem/1999/B"
        )));

        jdbcTemplate.update(
                "INSERT INTO daily_problem_comment (id, daily_problem_date, problem_key, user_id, reply_comment_id, content, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                20L, today.toString(), "2000-A", alice.getId(), null, "Today root", "2026-05-17T09:00:00+08:00"
        );
        jdbcTemplate.update(
                "INSERT INTO daily_problem_comment (id, daily_problem_date, problem_key, user_id, reply_comment_id, content, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                21L, yesterday.toString(), "1999-B", alice.getId(), null, "Historical root", "2026-05-16T09:00:00+08:00"
        );

        mockMvc.perform(get("/api/daily-problem/comments/archives")
                        .param("limit", "10")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].dailyProblemDate").value(today.toString()))
                .andExpect(jsonPath("$.data[0].problemKey").value("2000-A"))
                .andExpect(jsonPath("$.data[1].dailyProblemDate").value(yesterday.toString()))
                .andExpect(jsonPath("$.data[1].problemKey").value("1999-B"))
                .andExpect(jsonPath("$.data[1].name").value("Archived Problem"));
    }

    @Test
    void loggedInUserCanCreateRootCommentAndReply() throws Exception {
        User alice = createUser("alice");
        User bob = createUser("bob");
        String[] aliceTokens = issueTokens(alice);
        String[] bobTokens = issueTokens(bob);
        LocalDate today = LocalDate.now();

        seedTodayProblem(today);

        mockMvc.perform(post("/api/daily-problem/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"First root\"}")
                        .header("Authorization", "Bearer " + aliceTokens[0])
                        .header("X-Refresh-Token", aliceTokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").value("First root"))
                .andExpect(jsonPath("$.data.replyCommentId").value(nullValue()))
                .andExpect(jsonPath("$.data.author.username").value("alice"));

        Long rootId = jdbcTemplate.queryForObject(
                "SELECT id FROM daily_problem_comment WHERE content = 'First root'",
                Long.class
        );

        mockMvc.perform(post("/api/daily-problem/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Reply once\",\"replyCommentId\":" + rootId + "}")
                        .header("Authorization", "Bearer " + bobTokens[0])
                        .header("X-Refresh-Token", bobTokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.replyCommentId").value(rootId))
                .andExpect(jsonPath("$.data.replyToUsername").value("alice"))
                .andExpect(jsonPath("$.data.author.username").value("bob"));

        mockMvc.perform(get("/api/daily-problem/comments/today")
                        .header("Authorization", "Bearer " + aliceTokens[0])
                        .header("X-Refresh-Token", aliceTokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].content").value("First root"))
                .andExpect(jsonPath("$.data[0].replies", hasSize(1)))
                .andExpect(jsonPath("$.data[0].replies[0].content").value("Reply once"))
                .andExpect(jsonPath("$.data[0].replies[0].replyToUsername").value("alice"));
    }

    @Test
    void loggedInUserCanCreateHistoricalRootComment() throws Exception {
        User alice = createUser("alice");
        String[] tokens = issueTokens(alice);
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        seedTodayProblem(today);
        seedProblem(
                yesterday,
                new CfProblem(
                        "1999-B",
                        1999,
                        "B",
                        "Historical Problem",
                        1600,
                        "dp",
                        false,
                        null,
                        100,
                        "https://codeforces.com/problemset/problem/1999/B"
                )
        );

        mockMvc.perform(post("/api/daily-problem/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Historical root",
                                  "dailyProblemDate": "%s",
                                  "problemKey": "1999-B"
                                }
                                """.formatted(yesterday))
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.dailyProblemDate").value(yesterday.toString()))
                .andExpect(jsonPath("$.data.problemKey").value("1999-B"))
                .andExpect(jsonPath("$.data.content").value("Historical root"))
                .andExpect(jsonPath("$.data.author.username").value("alice"));

        mockMvc.perform(get("/api/daily-problem/comments")
                        .param("date", yesterday.toString())
                        .param("problemKey", "1999-B")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].content").value("Historical root"));
    }

    @Test
    void rejectsHistoricalRootCommentForUnknownInstance() throws Exception {
        User alice = createUser("alice");
        String[] tokens = issueTokens(alice);
        LocalDate today = LocalDate.now();
        LocalDate unknownDate = today.minusDays(3);

        seedTodayProblem(today);

        mockMvc.perform(post("/api/daily-problem/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Should fail",
                                  "dailyProblemDate": "%s",
                                  "problemKey": "1997-Z"
                                }
                                """.formatted(unknownDate))
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void rejectsUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/daily-problem/comments/today"))
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

    private void seedTodayProblem(LocalDate date) {
        CfProblem problem = new CfProblem(
                "2000-A",
                2000,
                "A",
                "Problem A",
                1500,
                "dp,graphs",
                false,
                null,
                100,
                "https://codeforces.com/problemset/problem/2000/A"
        );
        seedProblem(date, problem);
    }

    private void seedProblem(LocalDate date, CfProblem problem) {
        dailyProblemRepository.upsertProblems(List.of(problem));
        dailyProblemRepository.insertDailyProblem(date, problem, "admin");
    }
}

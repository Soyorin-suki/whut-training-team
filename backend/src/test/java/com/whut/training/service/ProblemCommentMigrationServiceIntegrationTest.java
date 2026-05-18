package com.whut.training.service;

import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.repository.ProblemCommentRepository;
import com.whut.training.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
class ProblemCommentMigrationServiceIntegrationTest {

    private static final Path TEST_DB = Paths.get(
            System.getProperty("java.io.tmpdir"),
            "whut-training-problem-comment-migration-test-" + System.nanoTime() + ".db"
    ).toAbsolutePath();

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + TEST_DB.toString().replace("\\", "/"));
    }

    @Autowired
    private ProblemCommentMigrationService problemCommentMigrationService;

    @Autowired
    private ProblemCommentRepository problemCommentRepository;

    @Autowired
    private UserRepository userRepository;

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
    void migrationMergesLegacyDailyThreadsByProblemKeyAndIsIdempotent() {
        User alice = createUser("alice");
        User bob = createUser("bob");
        User carol = createUser("carol");
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        jdbcTemplate.update(
                "INSERT INTO cf_problem (problem_key, contest_id, problem_index, name, rating, tags, is_interactive, source_contest_id, solved_count, source_url, last_synced_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                "2000-A", 2000, "A", "Problem A", 1500, "dp", 0, null, 100,
                "https://codeforces.com/problemset/problem/2000/A", "2026-05-17T00:00:00+08:00"
        );

        jdbcTemplate.update(
                "INSERT INTO daily_problem_comment (id, daily_problem_date, problem_key, user_id, reply_comment_id, content, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                1L, yesterday.toString(), "2000-A", alice.getId(), null, "Yesterday root", "2026-05-16T09:00:00+08:00"
        );
        jdbcTemplate.update(
                "INSERT INTO daily_problem_comment (id, daily_problem_date, problem_key, user_id, reply_comment_id, content, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                2L, yesterday.toString(), "2000-A", bob.getId(), 1L, "Yesterday reply", "2026-05-16T09:10:00+08:00"
        );
        jdbcTemplate.update(
                "INSERT INTO daily_problem_comment (id, daily_problem_date, problem_key, user_id, reply_comment_id, content, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                3L, today.toString(), "2000-A", carol.getId(), null, "Today root", "2026-05-17T09:00:00+08:00"
        );

        problemCommentMigrationService.migrateLegacyComments();
        problemCommentMigrationService.migrateLegacyComments();

        var rows = problemCommentRepository.findCommentsByProblemKey("2000-A");

        assertEquals(3, rows.size());
        assertEquals("Yesterday root", rows.get(0).content());
        assertEquals("Yesterday reply", rows.get(1).content());
        assertEquals("Today root", rows.get(2).content());
        assertEquals(rows.get(0).id(), rows.get(1).replyCommentId());
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM problem_comment", Long.class);
        assertEquals(3L, count);
    }

    private User createUser(String username) {
        User user = new User(null, username, username + "@example.com", "password123", UserRole.USER);
        userRepository.save(user);
        return user;
    }
}

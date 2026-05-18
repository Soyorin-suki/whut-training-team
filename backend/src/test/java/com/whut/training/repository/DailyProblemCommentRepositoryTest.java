package com.whut.training.repository;

import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.UserRole;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
class DailyProblemCommentRepositoryTest {

    private static final String TEST_DB_NAME = "whut-training-daily-problem-comment-repository-test-" + System.nanoTime();

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:" + TEST_DB_NAME + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
    }

    @Autowired
    private DailyProblemCommentRepository dailyProblemCommentRepository;

    @Autowired
    private UserRepository userRepository;

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
    void insertsRootAndReplyCommentsAndFiltersByProblemInstance() {
        User alice = createUser("alice");
        User bob = createUser("bob");
        LocalDate today = LocalDate.now();

        var root = dailyProblemCommentRepository.insertComment(today, "2000-A", alice.getId(), null, "Root comment");
        var reply = dailyProblemCommentRepository.insertComment(today, "2000-A", bob.getId(), root.id(), "Reply comment");
        dailyProblemCommentRepository.insertComment(today, "2000-B", alice.getId(), null, "Other problem");

        var comments = dailyProblemCommentRepository.findCommentsByDailyProblem(today, "2000-A");

        assertEquals(2, comments.size());
        assertEquals(root.id(), comments.get(0).id());
        assertEquals("alice", comments.get(0).authorUsername());
        assertEquals(reply.id(), comments.get(1).id());
        assertEquals(root.id(), comments.get(1).replyCommentId());
        assertEquals("bob", comments.get(1).authorUsername());
        assertTrue(dailyProblemCommentRepository.existsCommentsByDailyProblem(today, "2000-A"));
        assertFalse(dailyProblemCommentRepository.existsCommentsByDailyProblem(today, "2000-C"));

        var storedReply = dailyProblemCommentRepository.findCommentById(reply.id()).orElseThrow();
        assertEquals("Reply comment", storedReply.content());
        assertEquals(root.id(), storedReply.replyCommentId());
        assertNotNull(storedReply.createdAt());
    }

    @Test
    void findCommentByIdReturnsEmptyForUnknownId() {
        assertTrue(dailyProblemCommentRepository.findCommentById(999L).isEmpty());
    }

    @Test
    void findCommentArchivesReturnsHistoricalInstancesIncludingRowsMissingDailyProblem() {
        User alice = createUser("alice");
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        jdbcTemplate.update(
                "INSERT INTO cf_problem (problem_key, contest_id, problem_index, name, rating, tags, is_interactive, source_contest_id, solved_count, source_url, last_synced_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                "1999-B", 1999, "B", "Archived Problem", 1600, "dp", 0, null, 100,
                "https://codeforces.com/problemset/problem/1999/B", "2026-05-17T00:00:00+08:00"
        );
        dailyProblemCommentRepository.insertComment(yesterday, "1999-B", alice.getId(), null, "Old root");
        dailyProblemCommentRepository.insertComment(today, "2000-A", alice.getId(), null, "Today root");

        var archives = dailyProblemCommentRepository.findCommentArchives(10);

        assertEquals(2, archives.size());
        assertEquals(today.toString(), archives.get(0).dailyProblemDate());
        assertEquals("2000-A", archives.get(0).problemKey());
        assertEquals(yesterday.toString(), archives.get(1).dailyProblemDate());
        assertEquals("1999-B", archives.get(1).problemKey());
        assertEquals("Archived Problem", archives.get(1).name());
    }

    private User createUser(String username) {
        User user = new User(null, username, username + "@example.com", "password123", UserRole.USER);
        userRepository.save(user);
        return user;
    }
}

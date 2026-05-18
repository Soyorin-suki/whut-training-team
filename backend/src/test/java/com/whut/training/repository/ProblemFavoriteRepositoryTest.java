package com.whut.training.repository;

import com.whut.training.domain.entity.CfProblem;
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
class ProblemFavoriteRepositoryTest {

    private static final Path TEST_DB = Paths.get(
            System.getProperty("java.io.tmpdir"),
            "whut-training-problem-favorite-repository-test-" + System.nanoTime() + ".db"
    ).toAbsolutePath();

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + TEST_DB.toString().replace("\\", "/"));
    }

    @Autowired
    private ProblemFavoriteRepository problemFavoriteRepository;

    @Autowired
    private DailyProblemRepository dailyProblemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM problem_favorite");
        jdbcTemplate.update("DELETE FROM problem_like");
        jdbcTemplate.update("DELETE FROM user_daily_status");
        jdbcTemplate.update("DELETE FROM user_practice_draw");
        jdbcTemplate.update("DELETE FROM daily_problem");
        jdbcTemplate.update("DELETE FROM cf_problem");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void repeatedInsertDoesNotCreateDuplicateFavorite() {
        User user = createUser("alice");
        dailyProblemRepository.upsertProblems(List.of(problem("2000-A", 2000, "A")));

        assertTrue(problemFavoriteRepository.insertFavorite(user.getId(), "2000-A"));
        assertFalse(problemFavoriteRepository.insertFavorite(user.getId(), "2000-A"));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM problem_favorite WHERE user_id = ? AND problem_key = ?",
                Integer.class,
                user.getId(),
                "2000-A"
        );
        assertEquals(1, count);
    }

    @Test
    void deleteFavoriteRemovesRecord() {
        User user = createUser("alice");
        dailyProblemRepository.upsertProblems(List.of(problem("2000-A", 2000, "A")));
        problemFavoriteRepository.insertFavorite(user.getId(), "2000-A");

        assertTrue(problemFavoriteRepository.deleteFavorite(user.getId(), "2000-A"));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM problem_favorite WHERE user_id = ? AND problem_key = ?",
                Integer.class,
                user.getId(),
                "2000-A"
        );
        assertEquals(0, count);
    }

    @Test
    void findFavoriteStatsReturnsFavoriteFlagAndTimestamp() {
        User owner = createUser("owner");
        User other = createUser("other");
        dailyProblemRepository.upsertProblems(List.of(
                problem("2000-A", 2000, "A"),
                problem("2000-B", 2000, "B")
        ));

        jdbcTemplate.update(
                "INSERT INTO problem_favorite (user_id, problem_key, created_at) VALUES (?, ?, ?)",
                owner.getId(),
                "2000-A",
                "2026-05-17T10:00:00+08:00"
        );
        jdbcTemplate.update(
                "INSERT INTO problem_favorite (user_id, problem_key, created_at) VALUES (?, ?, ?)",
                other.getId(),
                "2000-B",
                "2026-05-17T11:00:00+08:00"
        );

        var stats = problemFavoriteRepository.findFavoriteStats(List.of("2000-A", "2000-B"), owner.getId());

        assertTrue(stats.get("2000-A").favoritedByMe());
        assertEquals("2026-05-17T10:00:00+08:00", stats.get("2000-A").favoritedAt());
        assertFalse(stats.get("2000-B").favoritedByMe());
        assertEquals(null, stats.get("2000-B").favoritedAt());
    }

    @Test
    void findUserFavoritesReturnsNewestFirst() {
        User user = createUser("alice");
        CfProblem dailyProblem = problem("2000-A", 2000, "A");
        CfProblem practiceProblem = problem("2000-B", 2000, "B");
        dailyProblemRepository.upsertProblems(List.of(dailyProblem, practiceProblem));
        dailyProblemRepository.insertDailyProblem(LocalDate.now(), dailyProblem, "admin");
        dailyProblemRepository.insertPracticeDraw(user.getId(), LocalDate.now(), practiceProblem);

        jdbcTemplate.update(
                "INSERT INTO problem_favorite (user_id, problem_key, created_at) VALUES (?, ?, ?)",
                user.getId(),
                "2000-A",
                "2026-05-17T08:00:00+08:00"
        );
        jdbcTemplate.update(
                "INSERT INTO problem_favorite (user_id, problem_key, created_at) VALUES (?, ?, ?)",
                user.getId(),
                "2000-B",
                "2026-05-17T09:00:00+08:00"
        );

        var items = problemFavoriteRepository.findUserFavorites(user.getId(), 1, 10);

        assertEquals(2, items.size());
        assertEquals("2000-B", items.get(0).problemKey());
        assertEquals("PRACTICE", items.get(0).sourceType());
        assertEquals("2000-A", items.get(1).problemKey());
        assertEquals("DAILY", items.get(1).sourceType());
        assertNotNull(items.get(0).favoritedAt());
    }

    private User createUser(String username) {
        User user = new User(null, username, username + "@example.com", "password123", UserRole.USER);
        userRepository.save(user);
        return user;
    }

    private CfProblem problem(String problemKey, int contestId, String problemIndex) {
        return new CfProblem(
                problemKey,
                contestId,
                problemIndex,
                "Problem " + problemIndex,
                1500,
                "dp,graphs",
                false,
                null,
                100,
                "https://codeforces.com/problemset/problem/" + contestId + "/" + problemIndex
        );
    }
}

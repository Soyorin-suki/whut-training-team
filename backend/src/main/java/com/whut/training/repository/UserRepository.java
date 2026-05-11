package com.whut.training.repository;

import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.LeaderboardType;
import com.whut.training.domain.enums.UserRole;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
        User user = new User(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("password"),
                parseRole(rs.getString("role")),
                (Integer) rs.getObject("codeforces_rating"),
                (Integer) rs.getObject("max_rating"),
                parseOnline(rs.getObject("is_online")),
                parseLongValue(rs.getObject("last_online_time_seconds")),
                rs.getString("avatar_url"),
                parseLongValue(rs.getObject("uid"))
        );
        user.setScore(parseIntegerValue(rs.getObject("score")));
        user.setSolvedProblemCount(parseIntegerValue(rs.getObject("solved_problem_count")));
        user.setHardSolvedProblemCount(parseIntegerValue(rs.getObject("hard_solved_problem_count")));
        user.setCurrentStreakDays(parseIntegerValue(rs.getObject("current_streak_days")));
        user.setLongestStreakDays(parseIntegerValue(rs.getObject("longest_streak_days")));
        return user;
    };
    private final RowMapper<LeaderboardUserSnapshot> leaderboardUserRowMapper = (rs, rowNum) -> new LeaderboardUserSnapshot(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("avatar_url"),
            parseIntegerValue(rs.getObject("score"))
    );

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public User save(User user) {
        Integer score = user.getScore() == null ? 0 : user.getScore();
        Integer solvedProblemCount = user.getSolvedProblemCount() == null ? 0 : user.getSolvedProblemCount();
        Integer hardSolvedProblemCount = user.getHardSolvedProblemCount() == null ? 0 : user.getHardSolvedProblemCount();
        Integer currentStreakDays = user.getCurrentStreakDays() == null ? 0 : user.getCurrentStreakDays();
        Integer longestStreakDays = user.getLongestStreakDays() == null ? 0 : user.getLongestStreakDays();
        if (user.getId() == null) {
            jdbcTemplate.update(
                    "INSERT INTO users (username, email, password, role, uid, codeforces_rating, max_rating, is_online, last_online_time_seconds, avatar_url, score, solved_problem_count, hard_solved_problem_count, current_streak_days, longest_streak_days) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    user.getUsername(),
                    user.getEmail(),
                    user.getPassword(),
                    user.getRole() == null ? null : user.getRole().name(),
                    user.getUid(),
                    user.getCodeforcesRating(),
                    user.getMaxRating(),
                    user.getOnline(),
                    user.getLastOnlineTimeSeconds(),
                    user.getAvatarUrl(),
                    score,
                    solvedProblemCount,
                    hardSolvedProblemCount,
                    currentStreakDays,
                    longestStreakDays
            );
            Long id = jdbcTemplate.queryForObject(
                    "SELECT id FROM users WHERE username = ?",
                    Long.class,
                    user.getUsername()
            );
            user.setId(id);
            user.setScore(score);
            user.setSolvedProblemCount(solvedProblemCount);
            user.setHardSolvedProblemCount(hardSolvedProblemCount);
            user.setCurrentStreakDays(currentStreakDays);
            user.setLongestStreakDays(longestStreakDays);
            return user;
        }

        jdbcTemplate.update(
                "UPDATE users SET username = ?, email = ?, password = ?, role = ?, uid = ?, codeforces_rating = ?, max_rating = ?, is_online = ?, last_online_time_seconds = ?, avatar_url = ?, score = ?, solved_problem_count = ?, hard_solved_problem_count = ?, current_streak_days = ?, longest_streak_days = ? WHERE id = ?",
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getRole() == null ? null : user.getRole().name(),
                user.getUid(),
                user.getCodeforcesRating(),
                user.getMaxRating(),
                user.getOnline(),
                user.getLastOnlineTimeSeconds(),
                user.getAvatarUrl(),
                score,
                solvedProblemCount,
                hardSolvedProblemCount,
                currentStreakDays,
                longestStreakDays,
                user.getId()
        );
        user.setScore(score);
        user.setSolvedProblemCount(solvedProblemCount);
        user.setHardSolvedProblemCount(hardSolvedProblemCount);
        user.setCurrentStreakDays(currentStreakDays);
        user.setLongestStreakDays(longestStreakDays);
        return user;
    }

    public List<User> findAll() {
        return jdbcTemplate.query(
                "SELECT id, username, email, password, role, uid, codeforces_rating, max_rating, is_online, last_online_time_seconds, avatar_url, score, solved_problem_count, hard_solved_problem_count, current_streak_days, longest_streak_days FROM users ORDER BY id ASC",
                userRowMapper
        );
    }

    public Optional<User> findById(Long id) {
        List<User> users = jdbcTemplate.query(
                "SELECT id, username, email, password, role, uid, codeforces_rating, max_rating, is_online, last_online_time_seconds, avatar_url, score, solved_problem_count, hard_solved_problem_count, current_streak_days, longest_streak_days FROM users WHERE id = ?",
                userRowMapper,
                id
        );
        return users.stream().findFirst();
    }

    public Optional<User> findByUsername(String username) {
        List<User> users = jdbcTemplate.query(
                "SELECT id, username, email, password, role, uid, codeforces_rating, max_rating, is_online, last_online_time_seconds, avatar_url, score, solved_problem_count, hard_solved_problem_count, current_streak_days, longest_streak_days FROM users WHERE username = ?",
                userRowMapper,
                username
        );
        return users.stream().findFirst();
    }

    public boolean existsByUsername(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM users WHERE username = ?",
                Integer.class,
                username
        );
        return count != null && count > 0;
    }

    public boolean updateUserScore(Long id, Integer userScore) {
        int rows = jdbcTemplate.update(
                "UPDATE users SET score = ? WHERE id = ?",
                userScore,
                id
        );
        return rows > 0;
    }

    public boolean updateUserSolvedProblemStats(Long id, Integer solvedProblemCount, Integer hardSolvedProblemCount) {
        int rows = jdbcTemplate.update(
                "UPDATE users SET solved_problem_count = ?, hard_solved_problem_count = ? WHERE id = ?",
                solvedProblemCount == null ? 0 : solvedProblemCount,
                hardSolvedProblemCount == null ? 0 : hardSolvedProblemCount,
                id
        );
        return rows > 0;
    }

    public boolean updateUserScoreAndStreakStats(Long id, Integer userScore, Integer currentStreakDays, Integer longestStreakDays) {
        int rows = jdbcTemplate.update(
                "UPDATE users SET score = ?, current_streak_days = ?, longest_streak_days = ? WHERE id = ?",
                userScore == null ? 0 : userScore,
                currentStreakDays == null ? 0 : currentStreakDays,
                longestStreakDays == null ? 0 : longestStreakDays,
                id
        );
        return rows > 0;
    }

    public boolean updateUserStreakStats(Long id, Integer currentStreakDays, Integer longestStreakDays) {
        int rows = jdbcTemplate.update(
                "UPDATE users SET current_streak_days = ?, longest_streak_days = ? WHERE id = ?",
                currentStreakDays == null ? 0 : currentStreakDays,
                longestStreakDays == null ? 0 : longestStreakDays,
                id
        );
        return rows > 0;
    }

    public List<Long> findUserIdsRequiringStreakBackfill() {
        return jdbcTemplate.query(
                """
                        SELECT u.id
                        FROM users u
                        WHERE COALESCE(u.current_streak_days, 0) = 0
                          AND COALESCE(u.longest_streak_days, 0) = 0
                          AND EXISTS (
                              SELECT 1
                              FROM user_daily_status s
                              WHERE s.user_id = u.id
                          )
                        ORDER BY u.id ASC
                        """,
                (rs, rowNum) -> rs.getLong("id")
        );
    }

    public Integer getTotal() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM users",
                Integer.class
        );
        return count;
    }

    public long countLeaderboardEntries() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM users",
                Long.class
        );
        return count == null ? 0L : count;
    }

    public List<LeaderboardUserSnapshot> findLeaderboardPage(LeaderboardType leaderboardType, int pageSize, int offset) {
        String metricColumn = resolveLeaderboardMetricColumn(leaderboardType);
        return jdbcTemplate.query(
                """
                        SELECT id, username, avatar_url, COALESCE(%s, 0) AS score
                        FROM users
                        ORDER BY COALESCE(%s, 0) DESC, id ASC
                        LIMIT ? OFFSET ?
                        """.formatted(metricColumn, metricColumn),
                leaderboardUserRowMapper,
                pageSize,
                offset
        );
    }

    public Optional<LeaderboardUserSnapshot> findLeaderboardUserById(LeaderboardType leaderboardType, Long userId) {
        String metricColumn = resolveLeaderboardMetricColumn(leaderboardType);
        List<LeaderboardUserSnapshot> rows = jdbcTemplate.query(
                """
                        SELECT id, username, avatar_url, COALESCE(%s, 0) AS score
                        FROM users
                        WHERE id = ?
                        """.formatted(metricColumn),
                leaderboardUserRowMapper,
                userId
        );
        return rows.stream().findFirst();
    }

    public int countUsersAheadOf(LeaderboardType leaderboardType, Long userId) {
        String metricColumn = resolveLeaderboardMetricColumn(leaderboardType);
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1)
                        FROM users other
                        JOIN users current ON current.id = ?
                        WHERE COALESCE(other.%s, 0) > COALESCE(current.%s, 0)
                           OR (COALESCE(other.%s, 0) = COALESCE(current.%s, 0) AND other.id < current.id)
                        """.formatted(metricColumn, metricColumn, metricColumn, metricColumn),
                Integer.class,
                userId
        );
        return count == null ? 0 : count;
    }

    private String resolveLeaderboardMetricColumn(LeaderboardType leaderboardType) {
        return switch (leaderboardType) {
            case DAILY_TOTAL -> "score";
            case SOLVED_COUNT -> "solved_problem_count";
            case HARD_SOLVED_COUNT -> "hard_solved_problem_count";
            case CURRENT_STREAK -> "current_streak_days";
            case LONGEST_STREAK -> "longest_streak_days";
            default -> throw new IllegalArgumentException("unsupported leaderboard type");
        };
    }

    private UserRole parseRole(String roleText) {
        if (roleText == null || roleText.isBlank()) {
            return null;
        }
        return UserRole.valueOf(roleText);
    }

    private Boolean parseOnline(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        if (rawValue instanceof Boolean value) {
            return value;
        }
        if (rawValue instanceof Number value) {
            return value.intValue() != 0;
        }
        return Boolean.parseBoolean(rawValue.toString());
    }

    private Long parseLongValue(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        if (rawValue instanceof Number value) {
            return value.longValue();
        }
        return Long.parseLong(rawValue.toString());
    }

    private Integer parseIntegerValue(Object rawValue) {
        if (rawValue == null) {
            return 0;
        }
        if (rawValue instanceof Number value) {
            return value.intValue();
        }
        return Integer.parseInt(rawValue.toString());
    }

    public record LeaderboardUserSnapshot(Long userId, String username, String avatarUrl, Integer score) {
    }
}

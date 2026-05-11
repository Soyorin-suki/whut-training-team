package com.whut.training.repository;

import com.whut.training.domain.dto.AdminDailyCheckInItem;
import com.whut.training.domain.dto.AdminDailyPracticeItem;
import com.whut.training.domain.dto.AdminDailyRecordItem;
import com.whut.training.domain.dto.AdminUserTimelineItem;
import com.whut.training.domain.dto.AdminUserTrainingItem;
import com.whut.training.domain.dto.ProblemView;
import com.whut.training.domain.enums.UserRole;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class AdminTrainingRepository {

    private final JdbcTemplate jdbcTemplate;

    public AdminTrainingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long countUsers() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM users", Long.class);
        return count == null ? 0L : count;
    }

    public long countDailyCheckIns(LocalDate date) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM user_daily_status WHERE date = ?",
                Long.class,
                date.toString()
        );
        return count == null ? 0L : count;
    }

    public long countPracticeDraws(LocalDate date) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM user_practice_draw WHERE draw_date = ?",
                Long.class,
                date.toString()
        );
        return count == null ? 0L : count;
    }

    public long countPracticeChecks(LocalDate date) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1)
                        FROM user_practice_draw
                        WHERE draw_date = ?
                          AND submission_id IS NOT NULL
                          AND checked_at IS NOT NULL
                        """,
                Long.class,
                date.toString()
        );
        return count == null ? 0L : count;
    }

    public long countActiveUsers(LocalDate date) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(DISTINCT activity.user_id)
                        FROM (
                            SELECT user_id FROM user_daily_status WHERE date = ?
                            UNION
                            SELECT user_id
                            FROM user_practice_draw
                            WHERE draw_date = ?
                              AND submission_id IS NOT NULL
                              AND checked_at IS NOT NULL
                        ) activity
                        """,
                Long.class,
                date.toString(),
                date.toString()
        );
        return count == null ? 0L : count;
    }

    public long countTodayStreakUsers(LocalDate date) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1)
                        FROM users u
                        JOIN (
                            SELECT user_id, MAX(date) AS last_daily_date
                            FROM user_daily_status
                            GROUP BY user_id
                        ) ds ON ds.user_id = u.id
                        WHERE COALESCE(u.current_streak_days, 0) > 0
                          AND ds.last_daily_date = ?
                        """,
                Long.class,
                date.toString()
        );
        return count == null ? 0L : count;
    }

    public long maxCurrentStreakDays() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(current_streak_days), 0) FROM users",
                Long.class
        );
        return count == null ? 0L : count;
    }

    public long maxLongestStreakDays() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(longest_streak_days), 0) FROM users",
                Long.class
        );
        return count == null ? 0L : count;
    }

    public double averageCurrentStreakDays() {
        Double average = jdbcTemplate.queryForObject(
                """
                        SELECT COALESCE(AVG(current_streak_days), 0)
                        FROM users
                        WHERE COALESCE(current_streak_days, 0) > 0
                        """,
                Double.class
        );
        return average == null ? 0.0d : average;
    }

    public long countDailyRecords(LocalDate startDate, LocalDate endDate) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM daily_problem WHERE date BETWEEN ? AND ?",
                Long.class,
                startDate.toString(),
                endDate.toString()
        );
        return count == null ? 0L : count;
    }

    public List<AdminDailyRecordItem> findDailyRecords(LocalDate startDate, LocalDate endDate, int pageSize, int offset, long totalUsers) {
        return jdbcTemplate.query(
                """
                        SELECT d.date,
                               d.problem_key,
                               d.contest_id,
                               d.problem_index,
                               d.name,
                               d.rating,
                               d.tags,
                               d.source_url,
                               COALESCE(dc.daily_check_in_count, 0) AS daily_check_in_count,
                               COALESCE(pc.practice_draw_count, 0) AS practice_draw_count,
                               COALESCE(pc.practice_check_count, 0) AS practice_check_count
                        FROM daily_problem d
                        LEFT JOIN (
                            SELECT date, COUNT(1) AS daily_check_in_count
                            FROM user_daily_status
                            GROUP BY date
                        ) dc ON dc.date = d.date
                        LEFT JOIN (
                            SELECT draw_date,
                                   COUNT(1) AS practice_draw_count,
                                   SUM(CASE WHEN submission_id IS NOT NULL AND checked_at IS NOT NULL THEN 1 ELSE 0 END) AS practice_check_count
                            FROM user_practice_draw
                            GROUP BY draw_date
                        ) pc ON pc.draw_date = d.date
                        WHERE d.date BETWEEN ? AND ?
                        ORDER BY d.date DESC
                        LIMIT ? OFFSET ?
                        """,
                (rs, rowNum) -> {
                    String date = rs.getString("date");
                    long dailyCheckInCount = rs.getLong("daily_check_in_count");
                    return new AdminDailyRecordItem(
                            date,
                            new ProblemView(
                                    "DAILY",
                                    date,
                                    rs.getString("problem_key"),
                                    rs.getInt("contest_id"),
                                    rs.getString("problem_index"),
                                    rs.getString("name"),
                                    (Integer) rs.getObject("rating"),
                                    rs.getString("tags"),
                                    rs.getString("source_url")
                            ),
                            totalUsers,
                            dailyCheckInCount,
                            Math.max(0L, totalUsers - dailyCheckInCount),
                            rs.getLong("practice_draw_count"),
                            rs.getLong("practice_check_count")
                    );
                },
                startDate.toString(),
                endDate.toString(),
                pageSize,
                offset
        );
    }

    public List<AdminDailyCheckInItem> findDailyCheckIns(LocalDate date) {
        return jdbcTemplate.query(
                """
                        SELECT s.user_id,
                               u.username,
                               u.avatar_url,
                               s.submission_id,
                               s.verdict,
                               s.score,
                               s.checked_at
                        FROM user_daily_status s
                        JOIN users u ON u.id = s.user_id
                        WHERE s.date = ?
                        ORDER BY s.checked_at DESC, s.user_id ASC
                        """,
                (rs, rowNum) -> new AdminDailyCheckInItem(
                        rs.getLong("user_id"),
                        rs.getString("username"),
                        rs.getString("avatar_url"),
                        rs.getLong("submission_id"),
                        rs.getString("verdict"),
                        (Integer) rs.getObject("score"),
                        rs.getString("checked_at")
                ),
                date.toString()
        );
    }

    public List<AdminDailyPracticeItem> findDailyPracticeChecks(LocalDate date) {
        return jdbcTemplate.query(
                """
                        SELECT p.id,
                               p.user_id,
                               u.username,
                               u.avatar_url,
                               p.problem_key,
                               p.contest_id,
                               p.problem_index,
                               p.name,
                               p.rating,
                               p.tags,
                               p.source_url,
                               p.submission_id,
                               p.verdict,
                               p.checked_at
                        FROM user_practice_draw p
                        JOIN users u ON u.id = p.user_id
                        WHERE p.draw_date = ?
                          AND p.submission_id IS NOT NULL
                          AND p.checked_at IS NOT NULL
                        ORDER BY p.checked_at DESC, p.id DESC
                        """,
                (rs, rowNum) -> new AdminDailyPracticeItem(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("username"),
                        rs.getString("avatar_url"),
                        rs.getString("problem_key"),
                        rs.getInt("contest_id"),
                        rs.getString("problem_index"),
                        rs.getString("name"),
                        (Integer) rs.getObject("rating"),
                        rs.getString("tags"),
                        rs.getString("source_url"),
                        rs.getLong("submission_id"),
                        rs.getString("verdict"),
                        rs.getString("checked_at")
                ),
                date.toString()
        );
    }

    public long countUserTrainingEntries(String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword == null) {
            return countUsers();
        }

        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1)
                        FROM users
                        WHERE LOWER(username) LIKE ?
                           OR LOWER(COALESCE(email, '')) LIKE ?
                        """,
                Long.class,
                normalizedKeyword,
                normalizedKeyword
        );
        return count == null ? 0L : count;
    }

    public List<AdminUserTrainingItem> findUserTrainingEntries(String keyword, int pageSize, int offset) {
        String normalizedKeyword = normalizeKeyword(keyword);
        StringBuilder sql = new StringBuilder(
                """
                        SELECT u.id,
                               u.username,
                               u.email,
                               u.role,
                               u.avatar_url,
                               COALESCE(u.score, 0) AS score,
                               COALESCE(u.solved_problem_count, 0) AS solved_problem_count,
                               COALESCE(u.hard_solved_problem_count, 0) AS hard_solved_problem_count,
                               COALESCE(u.current_streak_days, 0) AS current_streak_days,
                               COALESCE(u.longest_streak_days, 0) AS longest_streak_days,
                               COALESCE(ds.daily_check_in_count, 0) AS daily_check_in_count,
                               ds.last_daily_date,
                               COALESCE(ps.practice_check_count, 0) AS practice_check_count,
                               ps.last_practice_checked_at
                        FROM users u
                        LEFT JOIN (
                            SELECT user_id,
                                   COUNT(1) AS daily_check_in_count,
                                   MAX(date) AS last_daily_date
                            FROM user_daily_status
                            GROUP BY user_id
                        ) ds ON ds.user_id = u.id
                        LEFT JOIN (
                            SELECT user_id,
                                   COUNT(1) AS practice_check_count,
                                   MAX(checked_at) AS last_practice_checked_at
                            FROM user_practice_draw
                            WHERE submission_id IS NOT NULL
                              AND checked_at IS NOT NULL
                            GROUP BY user_id
                        ) ps ON ps.user_id = u.id
                        """
        );
        Object[] params;
        if (normalizedKeyword != null) {
            sql.append(" WHERE LOWER(u.username) LIKE ? OR LOWER(COALESCE(u.email, '')) LIKE ? ");
            sql.append(" ORDER BY u.id ASC LIMIT ? OFFSET ? ");
            params = new Object[]{normalizedKeyword, normalizedKeyword, pageSize, offset};
        } else {
            sql.append(" ORDER BY u.id ASC LIMIT ? OFFSET ? ");
            params = new Object[]{pageSize, offset};
        }

        return jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> new AdminUserTrainingItem(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        parseRole(rs.getString("role")),
                        rs.getString("avatar_url"),
                        (Integer) rs.getObject("score"),
                        (Integer) rs.getObject("solved_problem_count"),
                        (Integer) rs.getObject("hard_solved_problem_count"),
                        (Integer) rs.getObject("current_streak_days"),
                        (Integer) rs.getObject("longest_streak_days"),
                        rs.getLong("daily_check_in_count"),
                        rs.getLong("practice_check_count"),
                        rs.getString("last_daily_date"),
                        rs.getString("last_practice_checked_at")
                ),
                params
        );
    }

    public List<AdminUserTimelineItem> findUserTimeline(Long userId, int limit) {
        return jdbcTemplate.query(
                """
                        SELECT activity_type,
                               activity_at,
                               activity_date,
                               draw_id,
                               problem_key,
                               name,
                               rating,
                               source_url,
                               submission_id,
                               verdict,
                               score
                        FROM (
                            SELECT 'DAILY' AS activity_type,
                                   s.checked_at AS activity_at,
                                   s.date AS activity_date,
                                   NULL AS draw_id,
                                   d.problem_key,
                                   d.name,
                                   d.rating,
                                   d.source_url,
                                   s.submission_id,
                                   s.verdict,
                                   s.score
                            FROM user_daily_status s
                            JOIN daily_problem d ON d.date = s.date
                            WHERE s.user_id = ?
                            UNION ALL
                            SELECT 'PRACTICE' AS activity_type,
                                   p.checked_at AS activity_at,
                                   p.draw_date AS activity_date,
                                   p.id AS draw_id,
                                   p.problem_key,
                                   p.name,
                                   p.rating,
                                   p.source_url,
                                   p.submission_id,
                                   p.verdict,
                                   0 AS score
                            FROM user_practice_draw p
                            WHERE p.user_id = ?
                              AND p.submission_id IS NOT NULL
                              AND p.checked_at IS NOT NULL
                        ) timeline
                        ORDER BY activity_at DESC, activity_date DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> new AdminUserTimelineItem(
                        rs.getString("activity_type"),
                        rs.getString("activity_at"),
                        rs.getString("activity_date"),
                        parseLongValue(rs.getObject("draw_id")),
                        rs.getString("problem_key"),
                        rs.getString("name"),
                        (Integer) rs.getObject("rating"),
                        rs.getString("source_url"),
                        rs.getLong("submission_id"),
                        rs.getString("verdict"),
                        (Integer) rs.getObject("score")
                ),
                userId,
                userId,
                limit
        );
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim().toLowerCase();
        if (trimmed.isEmpty()) {
            return null;
        }
        return "%" + trimmed + "%";
    }

    private UserRole parseRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            return null;
        }
        return UserRole.valueOf(rawRole);
    }

    private Long parseLongValue(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        if (rawValue instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(rawValue.toString());
    }
}

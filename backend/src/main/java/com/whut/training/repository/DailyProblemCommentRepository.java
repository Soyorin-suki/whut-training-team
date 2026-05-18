package com.whut.training.repository;

import com.whut.training.domain.dto.DailyProblemCommentArchiveItem;
import com.whut.training.domain.entity.DailyProblemComment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Statement;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class DailyProblemCommentRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<DailyProblemComment> commentRowMapper = (rs, rowNum) -> new DailyProblemComment(
            rs.getLong("id"),
            LocalDate.parse(rs.getString("daily_problem_date")),
            rs.getString("problem_key"),
            rs.getLong("user_id"),
            parseLongValue(rs.getObject("reply_comment_id")),
            rs.getString("content"),
            rs.getString("created_at")
    );

    private final RowMapper<DailyProblemCommentRecord> commentRecordRowMapper = (rs, rowNum) -> new DailyProblemCommentRecord(
            rs.getLong("id"),
            LocalDate.parse(rs.getString("daily_problem_date")),
            rs.getString("problem_key"),
            rs.getLong("user_id"),
            parseLongValue(rs.getObject("reply_comment_id")),
            rs.getString("content"),
            rs.getString("created_at"),
            rs.getString("author_username"),
            rs.getString("author_avatar_url")
    );

    private final RowMapper<DailyProblemCommentArchiveItem> commentArchiveRowMapper = (rs, rowNum) -> new DailyProblemCommentArchiveItem(
            rs.getString("daily_problem_date"),
            rs.getString("problem_key"),
            rs.getString("name"),
            (Integer) rs.getObject("rating"),
            rs.getString("source_url"),
            rs.getInt("comment_count"),
            rs.getString("last_commented_at")
    );

    public DailyProblemCommentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DailyProblemComment insertComment(LocalDate dailyProblemDate, String problemKey, Long userId,
                                             Long replyCommentId, String content) {
        String sql = """
                INSERT INTO daily_problem_comment (
                    daily_problem_date, problem_key, user_id, reply_comment_id, content, created_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                """;
        String nowIso = OffsetDateTime.now().toString();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, dailyProblemDate.toString());
            statement.setString(2, problemKey);
            statement.setLong(3, userId);
            statement.setObject(4, replyCommentId);
            statement.setString(5, content);
            statement.setString(6, nowIso);
            return statement;
        }, keyHolder);
        Long id = keyHolder.getKey() == null ? null : keyHolder.getKey().longValue();
        return new DailyProblemComment(id, dailyProblemDate, problemKey, userId, replyCommentId, content, nowIso);
    }

    public List<DailyProblemCommentRecord> findCommentsByDailyProblem(LocalDate dailyProblemDate, String problemKey) {
        return jdbcTemplate.query(
                """
                        SELECT c.id,
                               c.daily_problem_date,
                               c.problem_key,
                               c.user_id,
                               c.reply_comment_id,
                               c.content,
                               c.created_at,
                               u.username AS author_username,
                               u.avatar_url AS author_avatar_url
                        FROM daily_problem_comment c
                        JOIN users u ON u.id = c.user_id
                        WHERE c.daily_problem_date = ?
                          AND c.problem_key = ?
                        ORDER BY c.created_at ASC, c.id ASC
                        """,
                commentRecordRowMapper,
                dailyProblemDate.toString(),
                problemKey
        );
    }

    public Optional<DailyProblemComment> findCommentById(Long id) {
        List<DailyProblemComment> rows = jdbcTemplate.query(
                """
                        SELECT id, daily_problem_date, problem_key, user_id, reply_comment_id, content, created_at
                        FROM daily_problem_comment
                        WHERE id = ?
                        """,
                commentRowMapper,
                id
        );
        return rows.stream().findFirst();
    }

    public boolean existsCommentsByDailyProblem(LocalDate dailyProblemDate, String problemKey) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1)
                        FROM daily_problem_comment
                        WHERE daily_problem_date = ?
                          AND problem_key = ?
                        """,
                Integer.class,
                dailyProblemDate.toString(),
                problemKey
        );
        return count != null && count > 0;
    }

    public List<DailyProblemCommentArchiveItem> findCommentArchives(int limit) {
        int safeLimit = Math.max(1, Math.min(100, limit));
        return jdbcTemplate.query(
                """
                        SELECT c.daily_problem_date,
                               c.problem_key,
                               COALESCE(dp.name, p.name, c.problem_key) AS name,
                               COALESCE(dp.rating, p.rating) AS rating,
                               COALESCE(dp.source_url, p.source_url) AS source_url,
                               COUNT(1) AS comment_count,
                               MAX(c.created_at) AS last_commented_at
                        FROM daily_problem_comment c
                        LEFT JOIN daily_problem dp
                            ON dp.date = c.daily_problem_date
                           AND dp.problem_key = c.problem_key
                        LEFT JOIN cf_problem p
                            ON p.problem_key = c.problem_key
                        GROUP BY c.daily_problem_date, c.problem_key
                        ORDER BY c.daily_problem_date DESC, MAX(c.created_at) DESC, c.problem_key ASC
                        LIMIT ?
                        """,
                commentArchiveRowMapper,
                safeLimit
        );
    }

    public record DailyProblemCommentRecord(
            Long id,
            LocalDate dailyProblemDate,
            String problemKey,
            Long userId,
            Long replyCommentId,
            String content,
            String createdAt,
            String authorUsername,
            String authorAvatarUrl
    ) {
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
}

package com.whut.training.repository;

import com.whut.training.domain.entity.ProblemComment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ProblemCommentRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<ProblemComment> commentRowMapper = (rs, rowNum) -> new ProblemComment(
            rs.getLong("id"),
            rs.getString("problem_key"),
            rs.getLong("user_id"),
            parseLongValue(rs.getObject("reply_comment_id")),
            rs.getString("content"),
            rs.getString("created_at"),
            parseLongValue(rs.getObject("legacy_comment_id"))
    );

    private final RowMapper<ProblemCommentRecord> commentRecordRowMapper = (rs, rowNum) -> new ProblemCommentRecord(
            rs.getLong("id"),
            rs.getString("problem_key"),
            rs.getLong("user_id"),
            parseLongValue(rs.getObject("reply_comment_id")),
            rs.getString("content"),
            rs.getString("created_at"),
            rs.getString("author_username"),
            rs.getString("author_avatar_url")
    );

    private final RowMapper<LegacyDailyProblemCommentRecord> legacyCommentRowMapper = (rs, rowNum) ->
            new LegacyDailyProblemCommentRecord(
                    rs.getLong("id"),
                    rs.getString("daily_problem_date"),
                    rs.getString("problem_key"),
                    rs.getLong("user_id"),
                    parseLongValue(rs.getObject("reply_comment_id")),
                    rs.getString("content"),
                    rs.getString("created_at")
            );

    public ProblemCommentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ProblemComment insertComment(String problemKey, Long userId, Long replyCommentId, String content) {
        return insertComment(problemKey, userId, replyCommentId, content, OffsetDateTime.now().toString(), null);
    }

    public ProblemComment insertMigratedComment(String problemKey, Long userId, String content, String createdAt,
                                                Long legacyCommentId) {
        return insertComment(problemKey, userId, null, content, createdAt, legacyCommentId);
    }

    private ProblemComment insertComment(String problemKey, Long userId, Long replyCommentId, String content,
                                         String createdAt, Long legacyCommentId) {
        String sql = """
                INSERT INTO problem_comment (
                    problem_key, user_id, reply_comment_id, content, created_at, legacy_comment_id
                ) VALUES (?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, problemKey);
            statement.setLong(2, userId);
            statement.setObject(3, replyCommentId);
            statement.setString(4, content);
            statement.setString(5, createdAt);
            statement.setObject(6, legacyCommentId);
            return statement;
        }, keyHolder);
        Long id = keyHolder.getKey() == null ? null : keyHolder.getKey().longValue();
        return new ProblemComment(id, problemKey, userId, replyCommentId, content, createdAt, legacyCommentId);
    }

    public void updateReplyCommentId(Long id, Long replyCommentId) {
        jdbcTemplate.update(
                "UPDATE problem_comment SET reply_comment_id = ? WHERE id = ?",
                replyCommentId,
                id
        );
    }

    public List<ProblemCommentRecord> findCommentsByProblemKey(String problemKey) {
        return jdbcTemplate.query(
                """
                        SELECT c.id,
                               c.problem_key,
                               c.user_id,
                               c.reply_comment_id,
                               c.content,
                               c.created_at,
                               u.username AS author_username,
                               u.avatar_url AS author_avatar_url
                        FROM problem_comment c
                        JOIN users u ON u.id = c.user_id
                        WHERE c.problem_key = ?
                        ORDER BY c.created_at ASC, c.id ASC
                        """,
                commentRecordRowMapper,
                problemKey
        );
    }

    public Optional<ProblemComment> findCommentById(Long id) {
        List<ProblemComment> rows = jdbcTemplate.query(
                """
                        SELECT id, problem_key, user_id, reply_comment_id, content, created_at, legacy_comment_id
                        FROM problem_comment
                        WHERE id = ?
                        """,
                commentRowMapper,
                id
        );
        return rows.stream().findFirst();
    }

    public List<ProblemComment> findMigratedLegacyComments() {
        return jdbcTemplate.query(
                """
                        SELECT id, problem_key, user_id, reply_comment_id, content, created_at, legacy_comment_id
                        FROM problem_comment
                        WHERE legacy_comment_id IS NOT NULL
                        ORDER BY legacy_comment_id ASC
                        """,
                commentRowMapper
        );
    }

    public List<LegacyDailyProblemCommentRecord> findLegacyComments() {
        return jdbcTemplate.query(
                """
                        SELECT id, daily_problem_date, problem_key, user_id, reply_comment_id, content, created_at
                        FROM daily_problem_comment
                        ORDER BY created_at ASC, id ASC
                        """,
                legacyCommentRowMapper
        );
    }

    public record ProblemCommentRecord(
            Long id,
            String problemKey,
            Long userId,
            Long replyCommentId,
            String content,
            String createdAt,
            String authorUsername,
            String authorAvatarUrl
    ) {
    }

    public record LegacyDailyProblemCommentRecord(
            Long id,
            String dailyProblemDate,
            String problemKey,
            Long userId,
            Long replyCommentId,
            String content,
            String createdAt
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

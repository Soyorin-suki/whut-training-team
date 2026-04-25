package com.whut.training.repository;

import com.whut.training.domain.dto.DailyProblemHistoryItem;
import com.whut.training.domain.entity.CfProblem;
import com.whut.training.domain.entity.DailyProblem;
import com.whut.training.domain.entity.UserDailyStatus;
import com.whut.training.domain.entity.UserPracticeDraw;
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
public class DailyProblemRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<CfProblem> cfProblemRowMapper = (rs, rowNum) -> new CfProblem(
            rs.getString("problem_key"),
            rs.getInt("contest_id"),
            rs.getString("problem_index"),
            rs.getString("name"),
            (Integer) rs.getObject("rating"),
            rs.getString("tags"),
            rs.getInt("is_interactive") != 0,
            (Integer) rs.getObject("source_contest_id"),
            (Integer) rs.getObject("solved_count"),
            rs.getString("source_url")
    );

    private final RowMapper<DailyProblem> dailyProblemRowMapper = (rs, rowNum) -> new DailyProblem(
            rs.getLong("id"),
            LocalDate.parse(rs.getString("date")),
            rs.getString("problem_key"),
            rs.getInt("contest_id"),
            rs.getString("problem_index"),
            rs.getString("name"),
            (Integer) rs.getObject("rating"),
            rs.getString("tags"),
            rs.getString("source_url")
    );

    private final RowMapper<UserDailyStatus> userDailyStatusRowMapper = (rs, rowNum) -> new UserDailyStatus(
            rs.getLong("user_id"),
            LocalDate.parse(rs.getString("date")),
            rs.getLong("submission_id"),
            rs.getString("verdict"),
            rs.getInt("score")
    );

    private final RowMapper<UserPracticeDraw> userPracticeDrawRowMapper = (rs, rowNum) -> new UserPracticeDraw(
            rs.getLong("id"),
            rs.getLong("user_id"),
            LocalDate.parse(rs.getString("draw_date")),
            rs.getString("problem_key"),
            rs.getInt("contest_id"),
            rs.getString("problem_index"),
            rs.getString("name"),
            (Integer) rs.getObject("rating"),
            rs.getString("tags"),
            rs.getString("source_url"),
            (Long) rs.getObject("submission_id"),
            rs.getString("verdict")
    );

    public DailyProblemRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int upsertProblems(List<CfProblem> problems) {
        if (problems.isEmpty()) {
            return 0;
        }
        String nowIso = OffsetDateTime.now().toString();
        jdbcTemplate.batchUpdate(
                """
                        INSERT INTO cf_problem (
                            problem_key, contest_id, problem_index, name, rating, tags,
                            is_interactive, source_contest_id, solved_count, source_url, last_synced_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT(problem_key) DO UPDATE SET
                            contest_id = excluded.contest_id,
                            problem_index = excluded.problem_index,
                            name = excluded.name,
                            rating = excluded.rating,
                            tags = excluded.tags,
                            is_interactive = excluded.is_interactive,
                            source_contest_id = excluded.source_contest_id,
                            solved_count = excluded.solved_count,
                            source_url = excluded.source_url,
                            last_synced_at = excluded.last_synced_at
                        """,
                problems,
                300,
                (ps, problem) -> {
                    ps.setString(1, problem.problemKey());
                    ps.setInt(2, problem.contestId());
                    ps.setString(3, problem.problemIndex());
                    ps.setString(4, problem.name());
                    if (problem.rating() == null) {
                        ps.setObject(5, null);
                    } else {
                        ps.setInt(5, problem.rating());
                    }
                    ps.setString(6, problem.tags());
                    ps.setInt(7, problem.interactive() ? 1 : 0);
                    ps.setObject(8, problem.sourceContestId());
                    ps.setObject(9, problem.solvedCount());
                    ps.setString(10, problem.sourceUrl());
                    ps.setString(11, nowIso);
                }
        );
        return problems.size();
    }

    public long countProblems() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM cf_problem", Long.class);
        return count == null ? 0 : count;
    }

    public Optional<CfProblem> findRandomProblem(Integer minRating, Integer maxRating, LocalDate noRepeatAfterDate) {
        String sql = """
                SELECT problem_key, contest_id, problem_index, name, rating, tags, is_interactive, source_contest_id, solved_count, source_url
                FROM cf_problem p
                WHERE p.source_contest_id IS NULL
                  AND p.is_interactive = 0
                  AND p.rating IS NOT NULL
                  AND p.rating BETWEEN ? AND ?
                  AND NOT EXISTS (
                    SELECT 1 FROM daily_problem d
                    WHERE d.problem_key = p.problem_key
                      AND d.date >= ?
                  )
                ORDER BY RANDOM()
                LIMIT 1
                """;
        List<CfProblem> rows = jdbcTemplate.query(sql, cfProblemRowMapper, minRating, maxRating, noRepeatAfterDate.toString());
        return rows.stream().findFirst();
    }

    public Optional<CfProblem> findRandomProblem(Integer minRating, Integer maxRating) {
        String sql = """
                SELECT problem_key, contest_id, problem_index, name, rating, tags, is_interactive, source_contest_id, solved_count, source_url
                FROM cf_problem p
                WHERE p.source_contest_id IS NULL
                  AND p.is_interactive = 0
                  AND p.rating IS NOT NULL
                  AND p.rating BETWEEN ? AND ?
                ORDER BY RANDOM()
                LIMIT 1
                """;
        List<CfProblem> rows = jdbcTemplate.query(sql, cfProblemRowMapper, minRating, maxRating);
        return rows.stream().findFirst();
    }

    public Optional<DailyProblem> findDailyByDate(LocalDate date) {
        List<DailyProblem> rows = jdbcTemplate.query(
                """
                        SELECT id, date, problem_key, contest_id, problem_index, name, rating, tags, source_url
                        FROM daily_problem
                        WHERE date = ?
                        """,
                dailyProblemRowMapper,
                date.toString()
        );
        return rows.stream().findFirst();
    }

    public DailyProblem insertDailyProblem(LocalDate date, CfProblem problem, String generatedBy) {
        String sql = """
                INSERT INTO daily_problem (
                    date, problem_key, contest_id, problem_index, name, rating, tags, source_url, generated_at, generated_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        String nowIso = OffsetDateTime.now().toString();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, date.toString());
            statement.setString(2, problem.problemKey());
            statement.setInt(3, problem.contestId());
            statement.setString(4, problem.problemIndex());
            statement.setString(5, problem.name());
            statement.setObject(6, problem.rating());
            statement.setString(7, problem.tags());
            statement.setString(8, problem.sourceUrl());
            statement.setString(9, nowIso);
            statement.setString(10, generatedBy);
            return statement;
        }, keyHolder);
        Long id = keyHolder.getKey() == null ? null : keyHolder.getKey().longValue();
        return new DailyProblem(
                id,
                date,
                problem.problemKey(),
                problem.contestId(),
                problem.problemIndex(),
                problem.name(),
                problem.rating(),
                problem.tags(),
                problem.sourceUrl()
        );
    }

    public void deleteDailyByDate(LocalDate date) {
        jdbcTemplate.update("DELETE FROM daily_problem WHERE date = ?", date.toString());
    }

    public Optional<UserDailyStatus> findUserDailyStatus(Long userId, LocalDate date) {
        List<UserDailyStatus> rows = jdbcTemplate.query(
                """
                        SELECT user_id, date, submission_id, verdict, score
                        FROM user_daily_status
                        WHERE user_id = ? AND date = ?
                        """,
                userDailyStatusRowMapper,
                userId,
                date.toString()
        );
        return rows.stream().findFirst();
    }

    public void saveUserDailyStatus(Long userId, LocalDate date, Long submissionId, String verdict, int score) {
        jdbcTemplate.update(
                """
                        INSERT INTO user_daily_status (user_id, date, submission_id, verdict, checked_at, score)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                userId,
                date.toString(),
                submissionId,
                verdict,
                OffsetDateTime.now().toString(),
                score
        );
    }

    public List<DailyProblemHistoryItem> findDailyHistoryForUser(Long userId, int limit) {
        return jdbcTemplate.query(
                """
                        SELECT d.date,
                               d.problem_key,
                               d.name,
                               d.rating,
                               d.source_url,
                               s.submission_id,
                               s.verdict,
                               s.score
                        FROM daily_problem d
                        LEFT JOIN user_daily_status s
                            ON s.user_id = ? AND s.date = d.date
                        ORDER BY d.date DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> new DailyProblemHistoryItem(
                        rs.getString("date"),
                        rs.getString("problem_key"),
                        rs.getString("name"),
                        (Integer) rs.getObject("rating"),
                        rs.getString("source_url"),
                        rs.getObject("submission_id") != null,
                        (Long) rs.getObject("submission_id"),
                        rs.getString("verdict"),
                        (Integer) rs.getObject("score")
                ),
                userId,
                limit
        );
    }

    public UserPracticeDraw insertPracticeDraw(Long userId, LocalDate drawDate, CfProblem problem) {
        String sql = """
                INSERT INTO user_practice_draw (
                    user_id, draw_date, problem_key, contest_id, problem_index, name, rating,
                    tags, source_url, drawn_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        String nowIso = OffsetDateTime.now().toString();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, userId);
            statement.setString(2, drawDate.toString());
            statement.setString(3, problem.problemKey());
            statement.setInt(4, problem.contestId());
            statement.setString(5, problem.problemIndex());
            statement.setString(6, problem.name());
            statement.setObject(7, problem.rating());
            statement.setString(8, problem.tags());
            statement.setString(9, problem.sourceUrl());
            statement.setString(10, nowIso);
            return statement;
        }, keyHolder);
        Long id = keyHolder.getKey() == null ? null : keyHolder.getKey().longValue();

        return new UserPracticeDraw(
                id,
                userId,
                drawDate,
                problem.problemKey(),
                problem.contestId(),
                problem.problemIndex(),
                problem.name(),
                problem.rating(),
                problem.tags(),
                problem.sourceUrl(),
                null,
                null
        );
    }

    public Optional<UserPracticeDraw> findPracticeDrawById(Long drawId, Long userId) {
        List<UserPracticeDraw> rows = jdbcTemplate.query(
                """
                        SELECT id, user_id, draw_date, problem_key, contest_id, problem_index, name,
                               rating, tags, source_url, submission_id, verdict
                        FROM user_practice_draw
                        WHERE id = ? AND user_id = ?
                        """,
                userPracticeDrawRowMapper,
                drawId,
                userId
        );
        return rows.stream().findFirst();
    }

    public void updatePracticeCheck(Long drawId, Long userId, Long submissionId, String verdict) {
        jdbcTemplate.update(
                """
                        UPDATE user_practice_draw
                        SET submission_id = ?, verdict = ?, checked_at = ?
                        WHERE id = ? AND user_id = ?
                        """,
                submissionId,
                verdict,
                OffsetDateTime.now().toString(),
                drawId,
                userId
        );
    }
}

package com.whut.training.repository;

import com.whut.training.domain.dto.ProblemLikeSummary;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ProblemLikeRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProblemLikeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean insertLike(Long userId, String problemKey) {
        try {
            return jdbcTemplate.update(
                    """
                            INSERT INTO problem_like (user_id, problem_key, created_at)
                            VALUES (?, ?, ?)
                            """,
                    userId,
                    problemKey,
                    OffsetDateTime.now().toString()
            ) > 0;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }

    public boolean deleteLike(Long userId, String problemKey) {
        return jdbcTemplate.update(
                "DELETE FROM problem_like WHERE user_id = ? AND problem_key = ?",
                userId,
                problemKey
        ) > 0;
    }

    public boolean existsLike(Long userId, String problemKey) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM problem_like WHERE user_id = ? AND problem_key = ?",
                Integer.class,
                userId,
                problemKey
        );
        return count != null && count > 0;
    }

    public int countLikes(String problemKey) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM problem_like WHERE problem_key = ?",
                Integer.class,
                problemKey
        );
        return count == null ? 0 : count;
    }

    public Map<String, ProblemLikeSummary> findLikeStats(List<String> problemKeys, Long userId) {
        List<String> safeProblemKeys = problemKeys == null
                ? List.of()
                : problemKeys.stream()
                .filter(problemKey -> problemKey != null && !problemKey.isBlank())
                .distinct()
                .toList();
        if (safeProblemKeys.isEmpty()) {
            return Map.of();
        }

        Map<String, ProblemLikeSummary> result = new LinkedHashMap<>();
        for (String problemKey : safeProblemKeys) {
            result.put(problemKey, new ProblemLikeSummary(problemKey, 0, false));
        }

        String placeholders = String.join(", ", Collections.nCopies(safeProblemKeys.size(), "?"));
        List<Object> params = new ArrayList<>();
        params.add(userId == null ? -1L : userId);
        params.addAll(safeProblemKeys);

        jdbcTemplate.query(
                """
                        SELECT problem_key,
                               COUNT(1) AS like_count,
                               MAX(CASE WHEN user_id = ? THEN 1 ELSE 0 END) AS liked_by_me
                        FROM problem_like
                        WHERE problem_key IN (%s)
                        GROUP BY problem_key
                        """.formatted(placeholders),
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> result.put(
                        rs.getString("problem_key"),
                        new ProblemLikeSummary(
                                rs.getString("problem_key"),
                                rs.getInt("like_count"),
                                rs.getInt("liked_by_me") != 0
                        )
                ),
                params.toArray()
        );
        return result;
    }
}

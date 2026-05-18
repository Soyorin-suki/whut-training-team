package com.whut.training.repository;

import com.whut.training.domain.dto.FavoriteProblemItem;
import com.whut.training.domain.dto.ProblemFavoriteSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ProblemFavoriteRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProblemFavoriteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean insertFavorite(Long userId, String problemKey) {
        return jdbcTemplate.update(
                """
                        INSERT INTO problem_favorite (user_id, problem_key, created_at)
                        VALUES (?, ?, ?)
                        ON CONFLICT(user_id, problem_key) DO NOTHING
                        """,
                userId,
                problemKey,
                OffsetDateTime.now().toString()
        ) > 0;
    }

    public boolean deleteFavorite(Long userId, String problemKey) {
        return jdbcTemplate.update(
                "DELETE FROM problem_favorite WHERE user_id = ? AND problem_key = ?",
                userId,
                problemKey
        ) > 0;
    }

    public Map<String, ProblemFavoriteSummary> findFavoriteStats(List<String> problemKeys, Long userId) {
        List<String> safeProblemKeys = problemKeys == null
                ? List.of()
                : problemKeys.stream()
                .filter(problemKey -> problemKey != null && !problemKey.isBlank())
                .distinct()
                .toList();
        if (safeProblemKeys.isEmpty()) {
            return Map.of();
        }

        Map<String, ProblemFavoriteSummary> result = new LinkedHashMap<>();
        for (String problemKey : safeProblemKeys) {
            result.put(problemKey, new ProblemFavoriteSummary(problemKey, false, null));
        }
        if (userId == null) {
            return result;
        }

        String placeholders = String.join(", ", Collections.nCopies(safeProblemKeys.size(), "?"));
        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.addAll(safeProblemKeys);

        jdbcTemplate.query(
                """
                        SELECT problem_key, created_at
                        FROM problem_favorite
                        WHERE user_id = ?
                          AND problem_key IN (%s)
                        """.formatted(placeholders),
                (rs, rowNum) -> new ProblemFavoriteSummary(
                        rs.getString("problem_key"),
                        true,
                        rs.getString("created_at")
                ),
                params.toArray()
        ).forEach(summary -> result.put(summary.problemKey(), summary));

        return result;
    }

    public List<FavoriteProblemItem> findUserFavorites(Long userId, int page, int limit) {
        int offset = Math.max(0, (page - 1) * limit);
        return jdbcTemplate.query(
                """
                        SELECT pf.problem_key,
                               cp.contest_id,
                               cp.problem_index,
                               cp.name,
                               cp.rating,
                               cp.tags,
                               cp.source_url,
                               pf.created_at,
                               CASE
                                   WHEN EXISTS (
                                       SELECT 1
                                       FROM daily_problem d
                                       WHERE d.problem_key = pf.problem_key
                                   ) THEN 'DAILY'
                                   WHEN EXISTS (
                                       SELECT 1
                                       FROM user_practice_draw upd
                                       WHERE upd.problem_key = pf.problem_key
                                   ) THEN 'PRACTICE'
                                   ELSE 'UNKNOWN'
                               END AS source_type
                        FROM problem_favorite pf
                        INNER JOIN cf_problem cp
                            ON cp.problem_key = pf.problem_key
                        WHERE pf.user_id = ?
                        ORDER BY pf.created_at DESC, pf.id DESC
                        LIMIT ? OFFSET ?
                        """,
                (rs, rowNum) -> new FavoriteProblemItem(
                        rs.getString("problem_key"),
                        rs.getInt("contest_id"),
                        rs.getString("problem_index"),
                        rs.getString("name"),
                        (Integer) rs.getObject("rating"),
                        rs.getString("tags"),
                        rs.getString("source_url"),
                        rs.getString("source_type"),
                        rs.getString("created_at")
                ),
                userId,
                limit,
                offset
        );
    }

    public long countUserFavorites(Long userId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM problem_favorite WHERE user_id = ?",
                Long.class,
                userId
        );
        return count == null ? 0L : count;
    }
}

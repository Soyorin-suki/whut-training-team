package com.whut.training.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whut.training.domain.dto.CodeforcesOverview;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Codeforces 用户统计的 MySQL 持久快照。
 */
@Repository
public class CodeforcesProfileSnapshotRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public CodeforcesProfileSnapshotRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<Snapshot> find(Long userId, String handle) {
        List<Snapshot> rows = jdbcTemplate.query(
                """
                SELECT payload_json, synced_at
                FROM cf_profile_snapshot
                WHERE user_id = ? AND LOWER(codeforces_handle) = LOWER(?)
                """,
                (rs, rowNum) -> {
                    try {
                        return new Snapshot(
                                objectMapper.readValue(rs.getString("payload_json"), CodeforcesOverview.class),
                                Instant.parse(rs.getString("synced_at"))
                        );
                    } catch (Exception ex) {
                        return null;
                    }
                },
                userId,
                handle
        );
        return rows.stream().filter(java.util.Objects::nonNull).findFirst();
    }

    public void save(Long userId, String handle, CodeforcesOverview overview) {
        try {
            String payload = objectMapper.writeValueAsString(overview.withStale(false));
            jdbcTemplate.update(
                    """
                    INSERT INTO cf_profile_snapshot(user_id, codeforces_handle, payload_json, synced_at)
                    VALUES (?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        codeforces_handle = VALUES(codeforces_handle),
                        payload_json = VALUES(payload_json),
                        synced_at = VALUES(synced_at)
                    """,
                    userId,
                    handle,
                    payload,
                    overview.syncedAt().toString()
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize Codeforces profile snapshot", ex);
        }
    }

    /**
     * 批量读取现役队员的本地快照，供管理员看板使用，不触发远程 Codeforces 请求。
     */
    public Map<Long, Snapshot> findAllForActiveTeam() {
        return jdbcTemplate.query(
                        """
                        SELECT s.user_id, s.payload_json, s.synced_at
                        FROM cf_profile_snapshot s
                        INNER JOIN users u ON u.id = s.user_id
                        WHERE u.member_type = 'ACTIVE_TEAM'
                        """,
                        (rs, rowNum) -> {
                            try {
                                return Map.entry(
                                        rs.getLong("user_id"),
                                        new Snapshot(
                                                objectMapper.readValue(rs.getString("payload_json"), CodeforcesOverview.class),
                                                Instant.parse(rs.getString("synced_at"))
                                        )
                                );
                            } catch (Exception ex) {
                                return null;
                            }
                        }
                ).stream()
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, ignored) -> first));
    }

    public record Snapshot(CodeforcesOverview overview, Instant syncedAt) {
    }
}

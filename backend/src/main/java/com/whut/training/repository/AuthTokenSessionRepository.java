package com.whut.training.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 认证会话仓储。
 *
 * <p>负责保存 access token / refresh token 对以及清理过期会话。当前实现是数据库持久化而非内存缓存，因此重启后会话仍可保留到过期时间。
 */
@Repository
public class AuthTokenSessionRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<AuthTokenSession> rowMapper = (rs, rowNum) -> new AuthTokenSession(
            rs.getLong("user_id"),
            rs.getString("access_token"),
            rs.getString("refresh_token"),
            rs.getLong("access_expired_at_seconds"),
            rs.getLong("refresh_expired_at_seconds")
    );

    /**
     * 创建认证会话仓储。
     *
     * @param jdbcTemplate JDBC 模板。
     */
    public AuthTokenSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 保存认证会话。
     *
     * @param session 会话对象。
     */
    public void save(AuthTokenSession session) {
        jdbcTemplate.update(
                "INSERT INTO auth_token_session (user_id, access_token, refresh_token, access_expired_at_seconds, refresh_expired_at_seconds, created_at_seconds) VALUES (?, ?, ?, ?, ?, ?)",
                session.userId(),
                session.accessToken(),
                session.refreshToken(),
                session.accessExpiredAtSeconds(),
                session.refreshExpiredAtSeconds(),
                Instant.now().getEpochSecond()
        );
    }

    /**
     * 通过 access token 查询会话。
     *
     * @param accessToken access token。
     * @return 会话。
     */
    public Optional<AuthTokenSession> findByAccessToken(String accessToken) {
        List<AuthTokenSession> rows = jdbcTemplate.query(
                "SELECT user_id, access_token, refresh_token, access_expired_at_seconds, refresh_expired_at_seconds FROM auth_token_session WHERE access_token = ?",
                rowMapper,
                accessToken
        );
        return rows.stream().findFirst();
    }

    /**
     * 通过 refresh token 查询会话。
     *
     * @param refreshToken refresh token。
     * @return 会话。
     */
    public Optional<AuthTokenSession> findByRefreshToken(String refreshToken) {
        List<AuthTokenSession> rows = jdbcTemplate.query(
                "SELECT user_id, access_token, refresh_token, access_expired_at_seconds, refresh_expired_at_seconds FROM auth_token_session WHERE refresh_token = ?",
                rowMapper,
                refreshToken
        );
        return rows.stream().findFirst();
    }

    /**
     * 按 access token 删除会话。
     *
     * @param accessToken access token。
     * @return 删除条数。
     */
    public int deleteByAccessToken(String accessToken) {
        return jdbcTemplate.update("DELETE FROM auth_token_session WHERE access_token = ?", accessToken);
    }

    /**
     * 按 refresh token 删除会话。
     *
     * @param refreshToken refresh token。
     * @return 删除条数。
     */
    public int deleteByRefreshToken(String refreshToken) {
        return jdbcTemplate.update("DELETE FROM auth_token_session WHERE refresh_token = ?", refreshToken);
    }

    /**
     * 删除已过期会话。
     *
     * @param epochSeconds 当前时间戳秒。
     * @return 删除条数。
     */
    public int deleteExpiredBefore(long epochSeconds) {
        return jdbcTemplate.update(
                "DELETE FROM auth_token_session WHERE access_expired_at_seconds <= ? OR refresh_expired_at_seconds <= ?",
                epochSeconds,
                epochSeconds
        );
    }

    /**
     * 认证会话数据结构。
     *
     * @param userId                  用户 ID。
     * @param accessToken             access token。
     * @param refreshToken            refresh token。
     * @param accessExpiredAtSeconds  access token 过期时间戳。
     * @param refreshExpiredAtSeconds refresh token 过期时间戳。
     */
    public record AuthTokenSession(
            Long userId,
            String accessToken,
            String refreshToken,
            Long accessExpiredAtSeconds,
            Long refreshExpiredAtSeconds
    ) {
    }
}

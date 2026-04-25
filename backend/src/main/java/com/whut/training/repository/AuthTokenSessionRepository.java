package com.whut.training.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

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

    public AuthTokenSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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

    public Optional<AuthTokenSession> findByAccessToken(String accessToken) {
        List<AuthTokenSession> rows = jdbcTemplate.query(
                "SELECT user_id, access_token, refresh_token, access_expired_at_seconds, refresh_expired_at_seconds FROM auth_token_session WHERE access_token = ?",
                rowMapper,
                accessToken
        );
        return rows.stream().findFirst();
    }

    public Optional<AuthTokenSession> findByRefreshToken(String refreshToken) {
        List<AuthTokenSession> rows = jdbcTemplate.query(
                "SELECT user_id, access_token, refresh_token, access_expired_at_seconds, refresh_expired_at_seconds FROM auth_token_session WHERE refresh_token = ?",
                rowMapper,
                refreshToken
        );
        return rows.stream().findFirst();
    }

    public int deleteByAccessToken(String accessToken) {
        return jdbcTemplate.update("DELETE FROM auth_token_session WHERE access_token = ?", accessToken);
    }

    public int deleteByRefreshToken(String refreshToken) {
        return jdbcTemplate.update("DELETE FROM auth_token_session WHERE refresh_token = ?", refreshToken);
    }

    public int deleteExpiredBefore(long epochSeconds) {
        return jdbcTemplate.update(
                "DELETE FROM auth_token_session WHERE access_expired_at_seconds <= ? OR refresh_expired_at_seconds <= ?",
                epochSeconds,
                epochSeconds
        );
    }

    public record AuthTokenSession(
            Long userId,
            String accessToken,
            String refreshToken,
            Long accessExpiredAtSeconds,
            Long refreshExpiredAtSeconds
    ) {
    }
}

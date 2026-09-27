package com.yocabs.api.modules.notification.infrastructure;

import com.yocabs.api.modules.notification.application.DeviceTokenRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcDeviceTokenRepository implements DeviceTokenRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcDeviceTokenRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void register(UUID userId, String token, String platform) {
        Timestamp now = Timestamp.from(Instant.now());

        jdbc.update(
                "INSERT INTO device_tokens (token, user_id, platform, created_at, updated_at) "
                        + "VALUES (:token, :userId, :platform, :now, :now) "
                        + "ON CONFLICT (token) DO UPDATE SET user_id = :userId, platform = :platform, updated_at = :now",
                new MapSqlParameterSource()
                        .addValue("token", token)
                        .addValue("userId", userId)
                        .addValue("platform", platform)
                        .addValue("now", now)
        );
    }

    @Override
    public void unregister(UUID userId, String token) {
        jdbc.update(
                "DELETE FROM device_tokens WHERE token = :token AND user_id = :userId",
                new MapSqlParameterSource().addValue("token", token).addValue("userId", userId)
        );
    }

    @Override
    public void discard(String token) {
        jdbc.update("DELETE FROM device_tokens WHERE token = :token", new MapSqlParameterSource("token", token));
    }

    @Override
    public List<String> findTokens(UUID userId) {
        return jdbc.queryForList(
                "SELECT token FROM device_tokens WHERE user_id = :userId",
                new MapSqlParameterSource("userId", userId),
                String.class
        );
    }
}

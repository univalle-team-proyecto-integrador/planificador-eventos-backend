package uv.isj.planificadoreventosbackend.service;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import uv.isj.planificadoreventosbackend.model.HealthResponse;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class HealthService {

    private final JdbcTemplate jdbcTemplate;

    public HealthService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public HealthResponse checkDatabase() {
        boolean connected = isDatabaseConnected();
        String status = connected ? "healthy" : "unhealthy";
        String database = connected ? "connected" : "disconnected";
        return new HealthResponse(status, database, timestamp());
    }

    public boolean isDatabaseConnected() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (DataAccessException e) {
            return false;
        }
    }

    private OffsetDateTime timestamp() {
        return OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
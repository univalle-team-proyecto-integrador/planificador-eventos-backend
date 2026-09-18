package uv.isj.planificadoreventosbackend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import uv.isj.planificadoreventosbackend.model.HealthResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthServiceTest {

    private JdbcTemplate jdbcTemplate;
    private HealthService healthService;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        healthService = new HealthService(jdbcTemplate);
    }

    @Test
    void checkDatabaseConBaseDisponibleReportaHealthyYConnected() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);

        HealthResponse response = healthService.checkDatabase();

        assertThat(response.status()).isEqualTo("healthy");
        assertThat(response.database()).isEqualTo("connected");
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void checkDatabaseConBaseCaidaReportaUnhealthyYDisconnected() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
                .thenThrow(new DataAccessResourceFailureException("sin conexión"));

        HealthResponse response = healthService.checkDatabase();

        assertThat(response.status()).isEqualTo("unhealthy");
        assertThat(response.database()).isEqualTo("disconnected");
        assertThat(response.timestamp()).isNotNull();
    }
}
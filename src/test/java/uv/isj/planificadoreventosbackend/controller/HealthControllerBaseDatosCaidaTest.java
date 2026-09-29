package uv.isj.planificadoreventosbackend.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uv.isj.planificadoreventosbackend.model.HealthResponse;
import uv.isj.planificadoreventosbackend.service.HealthService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthControllerBaseDatosCaidaTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthService healthService;

    @Test
    void baseDeDatosNoDisponibleDevuelve503ConElDetalleDelEstado() throws Exception {
        when(healthService.checkDatabase()).thenReturn(new HealthResponse(
                "unhealthy",
                "disconnected",
                OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS)));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("unhealthy"))
                .andExpect(jsonPath("$.database").value("disconnected"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void baseDeDatosNoDisponibleRespondeIgualConElSubrutaDelHealth() throws Exception {
        when(healthService.checkDatabase()).thenReturn(new HealthResponse(
                "unhealthy",
                "disconnected",
                OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS)));

        mockMvc.perform(get("/api/health/"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.database").value("disconnected"));
    }
}

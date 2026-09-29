package uv.isj.planificadoreventosbackend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "https://planificador-eventos-frontend-ten.vercel.app",
            "https://cualquier-preview.vercel.app"
    })
    void elPreflightAutorizaLosOriginesPermitidos(String origin) throws Exception {
        mockMvc.perform(options("/api/eventos")
                        .header("Origin", origin)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", origin))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost:5174",
            "http://127.0.0.1:5174",
            "https://localhost:5173",
            "https://localhost",
            "https://sitio-malicioso.com",
            "https://vercel.app.otro-dominio.com"
    })
    void elPreflightRechazaLosOriginesNoPermitidos(String origin) throws Exception {
        mockMvc.perform(options("/api/eventos")
                        .header("Origin", origin)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void elPreflightAdmiteLosMetodosQueExponeLaApi() throws Exception {
        String allowMethods = mockMvc.perform(options("/api/eventos")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("Access-Control-Allow-Methods");

        assertThat(allowMethods)
                .contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    }

    @Test
    void laPeticionRealDesdeElFrontendLocalIncluyeLasCabecerasCors() throws Exception {
        mockMvc.perform(get("/api/health")
                        .header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void laPeticionRealDesdeUnOrigenRechazadoSeBloqueaSinCabecerasCors() throws Exception {
        mockMvc.perform(get("/api/health")
                        .header("Origin", "https://sitio-malicioso.com"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void laPoliticaCorsSeAplicaSoloALasRutasDelApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs")
                        .header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}

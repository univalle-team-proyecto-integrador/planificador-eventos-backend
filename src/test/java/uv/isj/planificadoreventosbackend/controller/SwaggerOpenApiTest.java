package uv.isj.planificadoreventosbackend.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SwaggerOpenApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicaLaEspecificacionOpenApiConLosEndpointsDelBackend() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi").isString())
                .andExpect(jsonPath("$.info.title").value("Planificador de Eventos API"))
                .andExpect(jsonPath("$.info.version").value("1.0.0"))
                .andExpect(jsonPath("$.servers[0].url").value("/"))
                .andExpect(jsonPath("$.paths['/api/health']").exists())
                .andExpect(jsonPath("$.paths['/api/eventos']").exists())
                .andExpect(jsonPath("$.paths['/api/eventos/{id}']").exists())
                .andExpect(jsonPath("$.paths['/api/eventos/{id}/subtareas']").exists())
                .andExpect(jsonPath("$.paths['/api/subtareas/{id}/reprogramar']").exists())
                .andExpect(jsonPath("$.paths['/api/subtareas/{id}/estado']").exists())
                .andExpect(jsonPath("$.components.schemas.EventoDTO").exists())
                .andExpect(jsonPath("$.components.schemas.SubtareaDTO").exists())
                .andExpect(jsonPath("$.components.schemas.ReprogramarDTO").exists())
                .andExpect(jsonPath("$.components.schemas.EstadoSubtareaDTO").exists());
    }

    @Test
    void swaggerUiRedirigeALaInterfazConfigurada() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void swaggerUiMuestraLaInterfazHtml() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Swagger UI")));
    }
}

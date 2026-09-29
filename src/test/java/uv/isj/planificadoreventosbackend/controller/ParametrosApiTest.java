package uv.isj.planificadoreventosbackend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ParametrosApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unParametroRequeridoAusenteDevuelveProblemDetail() throws Exception {
        mockMvc.perform(get("/api/subtareas"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Solicitud inválida"))
                .andExpect(jsonPath("$.detail").value("Falta el parámetro obligatorio 'eventoId'"))
                .andExpect(jsonPath("$.errors.eventoId").isNotEmpty());

        mockMvc.perform(get("/api/subtareas/hoy"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Falta el parámetro obligatorio 'usuarioId'"))
                .andExpect(jsonPath("$.errors.usuarioId").isNotEmpty());
    }

    @ParameterizedTest
    @CsvSource({
            "abc",
            "1.5",
            "-",
            "2026-11-10"
    })
    void unEventoIdNoNumericoDevuelveProblemDetail(String eventoId) throws Exception {
        mockMvc.perform(get("/api/subtareas").param("eventoId", eventoId))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Solicitud inválida"))
                .andExpect(jsonPath("$.detail").value(containsString("eventoId")))
                .andExpect(jsonPath("$.errors.eventoId").isNotEmpty());
    }

    @Test
    void unParametroVacioSeReportaComoAusente() throws Exception {
        mockMvc.perform(get("/api/subtareas").param("eventoId", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Solicitud inválida"))
                .andExpect(jsonPath("$.detail").value(containsString("eventoId")));
    }

    @Test
    void unaFechaInvalidaDevuelveProblemDetail() throws Exception {
        mockMvc.perform(get("/api/subtareas/hoy")
                        .param("usuarioId", "1")
                        .param("fecha", "2026-13-45"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Solicitud inválida"))
                .andExpect(jsonPath("$.detail").value(containsString("fecha")))
                .andExpect(jsonPath("$.errors.fecha").isNotEmpty());
    }

    @Test
    void unIdDeRutaNoNumericoDevuelveProblemDetail() throws Exception {
        mockMvc.perform(get("/api/eventos/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Solicitud inválida"))
                .andExpect(jsonPath("$.detail").value(containsString("id")));

        mockMvc.perform(get("/api/subtareas/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(containsString("id")));
    }

    @Test
    void unEstadoDeSubtareaInexistenteDevuelveBadRequest() throws Exception {
        mockMvc.perform(patch("/api/subtareas/{id}/estado", 1)
                        .contentType("application/json")
                        .content("{\"estado\":\"no-existe\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void unMetodoNoSoportadoDevuelveProblemDetailYLaCabeceraAllow() throws Exception {
        mockMvc.perform(post("/api/health"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.title").value("Método no permitido"))
                .andExpect(jsonPath("$.detail").value(containsString("POST")))
                .andExpect(header().string("Allow", containsString("GET")));
    }

    @Test
    void unMetodoNoSoportadoEnRutaDeColeccionEnviaAllow() throws Exception {
        mockMvc.perform(put("/api/tipos-evento"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(header().string("Allow", containsString("GET")));

        mockMvc.perform(delete("/api/tipos-evento"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")));
    }

    @Test
    void unaRutaDesconocidaDevuelveNotFound() throws Exception {
        mockMvc.perform(get("/api/ruta-inexistente"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/subtareas/1/estado-extra"))
                .andExpect(status().isNotFound());
    }
}

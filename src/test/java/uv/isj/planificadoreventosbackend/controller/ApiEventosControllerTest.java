package uv.isj.planificadoreventosbackend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import uv.isj.planificadoreventosbackend.model.TipoEvento;
import uv.isj.planificadoreventosbackend.model.Usuario;
import uv.isj.planificadoreventosbackend.service.TipoEventoService;
import uv.isj.planificadoreventosbackend.service.UsuarioService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiEventosControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private TipoEventoService tipoEventoService;

    @Test
    void crudDeEventoYSubtareaUsaElContratoDelFrontend() throws Exception {
        Usuario usuario = usuarioService.save(usuario("controlador@example.com", "Controlador"));
        TipoEvento tipo = tipoEventoService.save(tipo("Evento de prueba"));

        Map<String, Object> eventoPayload = Map.of(
                "idUsuario", usuario.getIdUsuario(),
                "idTipoEvento", tipo.getIdTipoEvento(),
                "nombre", "Conferencia de integración",
                "cliente", "Equipo de producto",
                "fechaEvento", "2026-12-01T09:00:00",
                "lugar", "Centro de eventos");

        MvcResult eventoResult = mockMvc.perform(post("/api/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventoPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Conferencia de integración"))
                .andReturn();

        JsonNode eventoCreado = objectMapper.readTree(eventoResult.getResponse().getContentAsString());
        int eventoId = eventoCreado.get("idEvento").asInt();

        mockMvc.perform(get("/api/eventos/{id}", eventoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idEvento").value(eventoId))
                .andExpect(jsonPath("$.cliente").value("Equipo de producto"));

        Map<String, Object> subtareaPayload = Map.of(
                "idEvento", eventoId,
                "nombreGestion", "Reservar sala",
                "horasEstimadas", 2,
                "fechaObjetivo", LocalDate.of(2026, 11, 20).toString(),
                "estado", "pendiente");

        MvcResult subtareaResult = mockMvc.perform(post("/api/subtareas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subtareaPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreGestion").value("Reservar sala"))
                .andReturn();

        JsonNode subtareaCreada = objectMapper.readTree(subtareaResult.getResponse().getContentAsString());
        int subtareaId = subtareaCreada.get("idSubtarea").asInt();

        mockMvc.perform(get("/api/subtareas")
                        .param("eventoId", String.valueOf(eventoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idSubtarea").value(subtareaId));

        Map<String, Object> updateSubtaskPayload = Map.of(
                "idSubtarea", subtareaId,
                "idEvento", eventoId,
                "nombreGestion", "Reservar sala",
                "horasEstimadas", 2,
                "fechaObjetivo", LocalDate.of(2026, 11, 20).toString(),
                "estado", "ejecutada");

        mockMvc.perform(put("/api/subtareas/{id}", subtareaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateSubtaskPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ejecutada"));

        mockMvc.perform(delete("/api/subtareas/{id}", subtareaId))
                .andExpect(status().isNoContent());
    }

    @Test
    void eventoConPayloadInvalidoDevuelveBadRequest() throws Exception {
        mockMvc.perform(post("/api/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    private Usuario usuario(String email, String nombre) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setNombre(nombre);
        return usuario;
    }

    private TipoEvento tipo(String nombre) {
        TipoEvento tipo = new TipoEvento();
        tipo.setNombre(nombre);
        return tipo;
    }
}

package uv.isj.planificadoreventosbackend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import uv.isj.planificadoreventosbackend.model.Evento;
import uv.isj.planificadoreventosbackend.model.Subtarea;
import uv.isj.planificadoreventosbackend.model.TipoEvento;
import uv.isj.planificadoreventosbackend.model.Usuario;
import uv.isj.planificadoreventosbackend.model.dto.EventoDTO;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CasosNegativosApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    private Evento crearEventoPersistido() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);

        TipoEvento tipo = new TipoEvento();
        tipo.setNombre("Tipo " + sufijo);

        Usuario usuario = new Usuario();
        usuario.setEmail("negativo-" + sufijo + "@example.com");
        usuario.setPasswordHash("hash-de-prueba");
        usuario.setNombre("Organizador de prueba");
        usuario.setLimiteHorasDiarias(6);

        Evento evento = new Evento();
        evento.setUsuario(usuario);
        evento.setTipoEvento(tipo);
        evento.setNombre("Evento de prueba");
        evento.setCliente("Cliente de prueba");
        evento.setFechaEvento(LocalDateTime.of(2026, 12, 1, 15, 0));
        evento.setLugar("Cali");

        entityManager.persist(tipo);
        entityManager.persist(usuario);
        entityManager.persist(evento);
        entityManager.flush();
        return evento;
    }

    private String jsonEventoValido(Integer idUsuario, Integer idTipoEvento) throws Exception {
        EventoDTO dto = new EventoDTO(
                null, idUsuario, idTipoEvento, "Evento de prueba", "Cliente de prueba",
                LocalDateTime.of(2026, 12, 1, 15, 0), "Cali", null, null);
        return objectMapper.writeValueAsString(dto);
    }

    @Test
    void recursosInexistentesDevuelvenNotFoundConProblemDetail() throws Exception {
        mockMvc.perform(get("/api/eventos/{id}", 9999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.detail").value("No existe el evento con id 9999"));
        mockMvc.perform(put("/api/eventos/{id}", 9999)
                        .contentType("application/json")
                        .content(jsonEventoValido(1, 1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe el evento con id 9999"));
        mockMvc.perform(delete("/api/eventos/{id}", 9999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe el evento con id 9999"));
        mockMvc.perform(get("/api/eventos/{id}/subtareas", 9999))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/subtareas")
                        .param("eventoId", "9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void crearEventoConRelacionesInexistentesDevuelveNotFound() throws Exception {
        Evento evento = crearEventoPersistido();
        Integer usuarioId = evento.getUsuario().getIdUsuario();

        mockMvc.perform(post("/api/eventos")
                        .contentType("application/json")
                        .content(jsonEventoValido(9999, evento.getTipoEvento().getIdTipoEvento())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe el usuario con id 9999"));

        mockMvc.perform(post("/api/eventos")
                        .contentType("application/json")
                        .content(jsonEventoValido(usuarioId, 9999)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe el tipo de evento con id 9999"));
    }

    @Test
    void operarSobreSubtareasInexistentesDevuelveNotFound() throws Exception {
        mockMvc.perform(post("/api/eventos/{id}/subtareas", 9999)
                        .contentType("application/json")
                        .content("{\"nombreGestion\":\"Tarea\",\"fechaObjetivo\":\"2026-11-10\",\"horasEstimadas\":2}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe el evento con id 9999"));

        mockMvc.perform(put("/api/subtareas/{id}", 9999)
                        .contentType("application/json")
                        .content("{\"nombreGestion\":\"Tarea\",\"fechaObjetivo\":\"2026-11-10\",\"horasEstimadas\":2}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe la subtarea con id 9999"));

        mockMvc.perform(delete("/api/subtareas/{id}", 9999))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/subtareas/{id}/estado", 9999)
                        .contentType("application/json")
                        .content("{\"estado\":\"pendiente\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cambiarAEjecutadoUnEstadoInexistenteDevuelveBadRequestEstandarizado() throws Exception {
        Evento evento = crearEventoPersistido();
        Subtarea subtarea = new Subtarea();
        evento.agregarSubtarea(subtarea);
        subtarea.setNombreGestion("Tarea de estado");
        subtarea.setFechaObjetivo(LocalDate.of(2026, 11, 10));
        subtarea.setHorasEstimadas(2);
        subtarea.setEstado(uv.isj.planificadoreventosbackend.model.EstadoSubtarea.pendiente);
        entityManager.persist(subtarea);
        entityManager.flush();

        mockMvc.perform(patch("/api/subtareas/{id}/estado", subtarea.getIdSubtarea())
                        .contentType("application/json")
                        .content("{\"estado\":\"hecha\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Solicitud inválida"));
    }
}
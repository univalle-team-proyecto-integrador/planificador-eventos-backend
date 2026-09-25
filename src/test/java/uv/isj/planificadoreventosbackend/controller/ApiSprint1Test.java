package uv.isj.planificadoreventosbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import uv.isj.planificadoreventosbackend.exception.RecursoNoEncontradoException;
import uv.isj.planificadoreventosbackend.model.Evento;
import uv.isj.planificadoreventosbackend.model.EstadoSubtarea;
import uv.isj.planificadoreventosbackend.model.Subtarea;
import uv.isj.planificadoreventosbackend.model.TipoEvento;
import uv.isj.planificadoreventosbackend.model.Usuario;
import uv.isj.planificadoreventosbackend.model.dto.EventoDTO;
import uv.isj.planificadoreventosbackend.model.dto.SubtareaActualizacionDTO;
import uv.isj.planificadoreventosbackend.model.dto.SubtareaDTO;
import uv.isj.planificadoreventosbackend.repository.EventoRepository;
import uv.isj.planificadoreventosbackend.repository.SubtareaRepository;
import uv.isj.planificadoreventosbackend.service.EventoService;
import uv.isj.planificadoreventosbackend.service.SubtareaService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiSprint1Test {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private SubtareaRepository subtareaRepository;

    @Autowired
    private EventoService eventoService;

    @Autowired
    private SubtareaService subtareaService;

    @Test
    void losRepositoriosFiltranOrdenanYSumanLasSubtareas() {
        BaseFixture base = crearBase(6);
        crearSubtarea(base.evento(), "Tarea posterior", LocalDate.of(2026, 11, 11), 1,
                EstadoSubtarea.pendiente);
        crearSubtarea(base.evento(), "Tarea de tres horas", LocalDate.of(2026, 11, 10), 3,
                EstadoSubtarea.pendiente);
        crearSubtarea(base.evento(), "Tarea ejecutada", LocalDate.of(2026, 11, 10), 2,
                EstadoSubtarea.ejecutada);
        crearSubtarea(base.evento(), "Tarea pospuesta", LocalDate.of(2026, 11, 10), 1,
                EstadoSubtarea.pospuesta);
        entityManager.flush();
        entityManager.clear();

        assertThat(eventoRepository.findByUsuarioId(base.usuario().getIdUsuario()))
                .extracting(Evento::getIdEvento)
                .containsExactly(base.evento().getIdEvento());
        assertThat(subtareaRepository.findByEventoId(base.evento().getIdEvento()))
                .hasSize(4);
        assertThat(subtareaRepository.findNoEjecutadasParaHoy(
                base.usuario().getIdUsuario(),
                LocalDate.of(2026, 11, 10),
                EstadoSubtarea.ejecutada))
                .extracting(Subtarea::getNombreGestion)
                .containsExactly("Tarea pospuesta", "Tarea de tres horas");
        assertThat(subtareaRepository.sumarHorasNoEjecutadasPorFechaYUsuario(
                base.usuario().getIdUsuario(),
                LocalDate.of(2026, 11, 10),
                EstadoSubtarea.ejecutada))
                .isEqualTo(4L);
    }

    @Test
    void listaGestionesNoEjecutadasParaHoyPorOrganizadorYFecha() throws Exception {
        BaseFixture base = crearBase(6);
        LocalDate hoy = LocalDate.of(2026, 9, 25);
        crearSubtarea(base.evento(), "Gestión pendiente de hoy", hoy, 3,
                EstadoSubtarea.pendiente);
        crearSubtarea(base.evento(), "Gestión pospuesta de hoy", hoy, 1,
                EstadoSubtarea.pospuesta);
        crearSubtarea(base.evento(), "Gestión completada de hoy", hoy, 2,
                EstadoSubtarea.ejecutada);
        crearSubtarea(base.evento(), "Gestión de otro día", hoy.plusDays(1), 1,
                EstadoSubtarea.pendiente);

        mockMvc.perform(get("/api/subtareas/hoy")
                        .param("usuarioId", String.valueOf(base.usuario().getIdUsuario()))
                        .param("fecha", hoy.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].nombreGestion", hasItem("Gestión pendiente de hoy")))
                .andExpect(jsonPath("$[*].nombreGestion", hasItem("Gestión pospuesta de hoy")))
                .andExpect(jsonPath("$[*].fechaObjetivo").value(hasItem(hoy.toString())));
    }

    @Test
    void creaConsultaYDetalleDeEvento() throws Exception {
        BaseFixture base = crearBase(6);
        EventoDTO request = dtoEvento(base, "Boda de prueba");

        MvcResult result = mockMvc.perform(post("/api/eventos")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/eventos/")))
                .andExpect(jsonPath("$.nombre").value("Boda de prueba"))
                .andExpect(jsonPath("$.idUsuario").value(base.usuario().getIdUsuario()))
                .andExpect(jsonPath("$.fechaCreacion").isNotEmpty())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        int eventoId = body.path("idEvento").asInt();

        mockMvc.perform(get("/api/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].idEvento", hasItem(eventoId)));
        mockMvc.perform(get("/api/eventos/{id}", eventoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idEvento").value(eventoId))
                .andExpect(jsonPath("$.cliente").value("Cliente de prueba"));
    }

    @Test
    void listaTiposDeEventoParaElFormulario() throws Exception {
        BaseFixture base = crearBase(6);

        mockMvc.perform(get("/api/tipos-evento"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idTipoEvento").value(base.tipoEvento().getIdTipoEvento()))
                .andExpect(jsonPath("$[0].nombre").value(base.tipoEvento().getNombre()));
    }

    @Test
    void agregaSubtareaConEstadoInicialPendiente() throws Exception {
        BaseFixture base = crearBase(6);
        SubtareaDTO request = new SubtareaDTO(
                null,
                null,
                "Confirmar catering",
                LocalDate.of(2026, 11, 15),
                4,
                EstadoSubtarea.ejecutada,
                "No debe respetarse al crear",
                null);

        MvcResult result = mockMvc.perform(post("/api/eventos/{id}/subtareas", base.evento().getIdEvento())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idEvento").value(base.evento().getIdEvento()))
                .andExpect(jsonPath("$.estado").value("pendiente"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(subtareaRepository.findById(body.path("idSubtarea").asInt()))
                .isPresent()
                .get()
                .extracting(Subtarea::getEstado)
                .isEqualTo(EstadoSubtarea.pendiente);
    }

    @Test
    void actualizaLosCamposEditablesDeUnaSubtarea() throws Exception {
        BaseFixture base = crearBase(6);
        Subtarea subtarea = crearSubtarea(
                base.evento(), "Gestión anterior", LocalDate.of(2026, 11, 10), 2,
                EstadoSubtarea.pendiente);
        SubtareaActualizacionDTO request = new SubtareaActualizacionDTO(
                "Gestión actualizada", LocalDate.of(2026, 11, 15), 4);

        mockMvc.perform(put("/api/subtareas/{id}", subtarea.getIdSubtarea())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idSubtarea").value(subtarea.getIdSubtarea()))
                .andExpect(jsonPath("$.nombreGestion").value("Gestión actualizada"))
                .andExpect(jsonPath("$.fechaObjetivo").value("2026-11-15"))
                .andExpect(jsonPath("$.horasEstimadas").value(4));
    }

    @Test
    void actualizaEventoYExponeSubtareasYEliminacion() throws Exception {
        BaseFixture base = crearBase(6);
        EventoDTO request = dtoEvento(base, "Boda actualizada");

        mockMvc.perform(get("/api/eventos")
                        .param("usuarioId", String.valueOf(base.usuario().getIdUsuario())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idEvento").value(base.evento().getIdEvento()));

        mockMvc.perform(put("/api/eventos/{id}", base.evento().getIdEvento())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Boda actualizada"));

        Subtarea subtarea = crearSubtarea(
                base.evento(), "Tarea para consultar", LocalDate.of(2026, 11, 12), 2,
                EstadoSubtarea.pendiente);

        mockMvc.perform(get("/api/eventos/{id}/subtareas", base.evento().getIdEvento()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idSubtarea").value(subtarea.getIdSubtarea()));
        mockMvc.perform(get("/api/subtareas")
                        .param("eventoId", String.valueOf(base.evento().getIdEvento())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idSubtarea").value(subtarea.getIdSubtarea()));

        mockMvc.perform(delete("/api/subtareas/{id}", subtarea.getIdSubtarea()))
                .andExpect(status().isNoContent());
        entityManager.clear();
        assertThat(subtareaRepository.findById(subtarea.getIdSubtarea())).isEmpty();
    }

    @Test
    void validaLosDatosEnLosServicios() {
        BaseFixture base = crearBase(6);
        EventoDTO nombreVacio = new EventoDTO(
                null,
                base.usuario().getIdUsuario(),
                base.tipoEvento().getIdTipoEvento(),
                "   ",
                "Cliente",
                LocalDateTime.of(2026, 12, 1, 15, 0),
                "Cali",
                null);

        assertThatThrownBy(() -> eventoService.crearEvento(nombreVacio))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre");

        SubtareaDTO nombreInvalido = new SubtareaDTO(
                null, null, " ", LocalDate.of(2026, 11, 10), 2,
                null, null, null);
        assertThatThrownBy(() -> subtareaService.agregarSubtarea(base.evento().getIdEvento(), nombreInvalido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre");

        SubtareaDTO horasInvalidas = new SubtareaDTO(
                null, null, "Tarea", LocalDate.of(2026, 11, 10), 0,
                null, null, null);
        assertThatThrownBy(() -> subtareaService.agregarSubtarea(base.evento().getIdEvento(), horasInvalidas))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mayores que cero");
    }

    @Test
    void rechazaCuerposJsonInvalidosConProblemDetail() throws Exception {
        mockMvc.perform(post("/api/eventos")
                        .contentType("application/json")
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Solicitud inválida"))
                .andExpect(jsonPath("$.detail").value("El cuerpo de la solicitud no contiene un JSON válido"));
    }

    @Test
    void reprogramarDevuelveConflictoSinModificarLaSubtarea() throws Exception {
        BaseFixture base = crearBase(5);
        LocalDate fechaNueva = LocalDate.of(2026, 11, 20);
        Subtarea actual = crearSubtarea(
                base.evento(), "Tarea que se reprograma", LocalDate.of(2026, 11, 11), 1,
                EstadoSubtarea.pendiente);
        crearSubtarea(base.evento(), "Ocupación pendiente", fechaNueva, 3,
                EstadoSubtarea.pendiente);
        crearSubtarea(base.evento(), "Ocupación ejecutada", fechaNueva, 3,
                EstadoSubtarea.ejecutada);

        mockMvc.perform(patch("/api/subtareas/{id}/reprogramar", actual.getIdSubtarea())
                        .contentType("application/json")
                        .content("{\"nuevaFecha\":\"2026-11-20\",\"nuevasHoras\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conflicto").value(true))
                .andExpect(jsonPath("$.limiteDiario").value(5.0))
                .andExpect(jsonPath("$.horasTotalesCalculadas").value(6.0))
                .andExpect(jsonPath("$.mensaje").isNotEmpty());

        assertThat(subtareaRepository.findById(actual.getIdSubtarea()))
                .isPresent()
                .get()
                .extracting(Subtarea::getFechaObjetivo)
                .isEqualTo(LocalDate.of(2026, 11, 11));
    }

    @Test
    void elLimiteDiarioNoIncluyeSubtareasDeOtroOrganizador() {
        BaseFixture primerOrganizador = crearBase(2);
        BaseFixture segundoOrganizador = crearBase(2);
        LocalDate fecha = LocalDate.of(2026, 11, 20);
        Subtarea actual = crearSubtarea(
                primerOrganizador.evento(), "Tarea propia", fecha, 1,
                EstadoSubtarea.pendiente);
        crearSubtarea(
                segundoOrganizador.evento(), "Tarea ajena", fecha, 2,
                EstadoSubtarea.pendiente);

        var resultado = subtareaService.reprogramar(
                actual.getIdSubtarea(),
                new uv.isj.planificadoreventosbackend.model.dto.ReprogramarDTO(fecha, 2),
                2.0);

        assertThat(resultado.get("conflicto")).isEqualTo(false);
        assertThat(resultado.get("horasTotalesCalculadas")).isEqualTo(2.0);
    }

    @Test
    void reprogramarActualizaCuandoNoHayConflicto() throws Exception {
        BaseFixture base = crearBase(5);
        LocalDate fechaNueva = LocalDate.of(2026, 11, 21);
        Subtarea actual = crearSubtarea(
                base.evento(), "Tarea reprogramable", LocalDate.of(2026, 11, 12), 1,
                EstadoSubtarea.pendiente);
        crearSubtarea(base.evento(), "Horas ya asignadas", fechaNueva, 2,
                EstadoSubtarea.pendiente);

        mockMvc.perform(patch("/api/subtareas/{id}/reprogramar", actual.getIdSubtarea())
                        .contentType("application/json")
                        .content("{\"nuevaFecha\":\"2026-11-21\",\"nuevasHoras\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conflicto").value(false))
                .andExpect(jsonPath("$.horasTotalesCalculadas").value(5.0))
                .andExpect(jsonPath("$.subtarea.fechaObjetivo").value("2026-11-21"))
                .andExpect(jsonPath("$.subtarea.horasEstimadas").value(3));
    }

    @Test
    void reprogramarEnLaMismaFechaNoCuentaDosVecesLaMismaSubtarea() {
        BaseFixture base = crearBase(3);
        LocalDate fecha = LocalDate.of(2026, 11, 22);
        Subtarea actual = crearSubtarea(
                base.evento(), "Tarea actual", fecha, 2, EstadoSubtarea.pendiente);
        crearSubtarea(base.evento(), "Otra tarea", fecha, 1, EstadoSubtarea.pendiente);

        var resultado = subtareaService.reprogramar(
                actual.getIdSubtarea(),
                new uv.isj.planificadoreventosbackend.model.dto.ReprogramarDTO(fecha, 2),
                3.0);

        assertThat(resultado.get("conflicto")).isEqualTo(false);
        assertThat(resultado.get("horasTotalesCalculadas")).isEqualTo(3.0);
    }

    @Test
    void cambiaEstadoYPermiteReabrirUnaSubtarea() throws Exception {
        BaseFixture base = crearBase(6);
        Subtarea subtarea = crearSubtarea(
                base.evento(), "Tarea para completar", LocalDate.of(2026, 11, 12), 2,
                EstadoSubtarea.pendiente);

        mockMvc.perform(patch("/api/subtareas/{id}/estado", subtarea.getIdSubtarea())
                        .contentType("application/json")
                        .content("{\"estado\":\"pendiente\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("pendiente"));

        mockMvc.perform(patch("/api/subtareas/{id}/estado", subtarea.getIdSubtarea())
                        .contentType("application/json")
                        .content("{\"estado\":\"pospuesta\",\"notaExplicativa\":\"Proveedor sin disponibilidad\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("pospuesta"))
                .andExpect(jsonPath("$.notaExplicativa").value("Proveedor sin disponibilidad"));
    }

    @Test
    void eliminarEventoEliminaSusSubtareasEnCascada() throws Exception {
        BaseFixture base = crearBase(6);
        Subtarea subtarea = crearSubtarea(
                base.evento(), "Tarea que debe eliminarse", LocalDate.of(2026, 11, 12), 2,
                EstadoSubtarea.pendiente);

        mockMvc.perform(delete("/api/eventos/{id}", base.evento().getIdEvento()))
                .andExpect(status().isNoContent());

        assertThat(eventoRepository.findById(base.evento().getIdEvento())).isEmpty();
        assertThat(subtareaRepository.findById(subtarea.getIdSubtarea())).isEmpty();
    }

    @Test
    void traduceRecursosNoEncontradosYErroresDeValidacion() throws Exception {
        mockMvc.perform(get("/api/eventos/{id}", 9999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.detail").value("No existe el evento con id 9999"));

        BaseFixture base = crearBase(6);
        EventoDTO nombreVacio = new EventoDTO(
                null,
                base.usuario().getIdUsuario(),
                base.tipoEvento().getIdTipoEvento(),
                "",
                "Cliente",
                LocalDateTime.of(2026, 12, 1, 15, 0),
                "Cali",
                null);

        mockMvc.perform(post("/api/eventos")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(nombreVacio)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Datos inválidos"))
                .andExpect(jsonPath("$.errors.nombre").isNotEmpty());
    }

    @Test
    void eliminarEventoInexistenteLanzaLaExcepcionDeDominio() {
        assertThatThrownBy(() -> eventoService.eliminarEvento(9999))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("9999");
    }

    private BaseFixture crearBase(int limiteDiario) {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);

        TipoEvento tipo = new TipoEvento();
        tipo.setNombre("Tipo " + sufijo);

        Usuario usuario = new Usuario();
        usuario.setEmail("organizador-" + sufijo + "@example.com");
        usuario.setPasswordHash("hash-de-prueba");
        usuario.setNombre("Organizador de prueba");
        usuario.setLimiteHorasDiarias(limiteDiario);

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

        return new BaseFixture(usuario, tipo, evento);
    }

    private Subtarea crearSubtarea(
            Evento evento,
            String nombre,
            LocalDate fecha,
            Integer horas,
            EstadoSubtarea estado) {
        Subtarea subtarea = new Subtarea();
        evento.agregarSubtarea(subtarea);
        subtarea.setNombreGestion(nombre);
        subtarea.setFechaObjetivo(fecha);
        subtarea.setHorasEstimadas(horas);
        subtarea.setEstado(estado);
        entityManager.persist(subtarea);
        entityManager.flush();
        return subtarea;
    }

    private EventoDTO dtoEvento(BaseFixture base, String nombre) {
        return new EventoDTO(
                null,
                base.usuario().getIdUsuario(),
                base.tipoEvento().getIdTipoEvento(),
                nombre,
                "Cliente de prueba",
                LocalDateTime.of(2026, 12, 1, 15, 0),
                "Cali",
                null);
    }

    private record BaseFixture(Usuario usuario, TipoEvento tipoEvento, Evento evento) {
    }
}

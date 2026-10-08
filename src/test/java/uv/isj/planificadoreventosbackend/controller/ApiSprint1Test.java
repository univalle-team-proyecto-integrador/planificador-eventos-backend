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
import uv.isj.planificadoreventosbackend.model.dto.CapacidadDTO;
import uv.isj.planificadoreventosbackend.model.dto.EventoDTO;
import uv.isj.planificadoreventosbackend.model.dto.ReprogramarDTO;
import uv.isj.planificadoreventosbackend.model.dto.SubtareaActualizacionDTO;
import uv.isj.planificadoreventosbackend.model.dto.SubtareaDTO;
import uv.isj.planificadoreventosbackend.repository.EventoRepository;
import uv.isj.planificadoreventosbackend.repository.SubtareaRepository;
import uv.isj.planificadoreventosbackend.repository.UsuarioRepository;
import uv.isj.planificadoreventosbackend.service.EventoService;
import uv.isj.planificadoreventosbackend.service.SubtareaService;
import uv.isj.planificadoreventosbackend.service.UsuarioService;

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
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EventoService eventoService;

    @Autowired
    private SubtareaService subtareaService;

    @Autowired
    private UsuarioService usuarioService;

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

        SubtareaDTO subtarea = new SubtareaDTO(
                null, null, "Confirmar banquete", LocalDate.of(2026, 11, 12), 3,
                null, null, null);
        mockMvc.perform(post("/api/eventos/{id}/subtareas", eventoId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(subtarea)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/eventos/{id}", eventoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idEvento").value(eventoId))
                .andExpect(jsonPath("$.cliente").value("Cliente de prueba"))
                .andExpect(jsonPath("$.subtareas[0].nombreGestion").value("Confirmar banquete"))
                .andExpect(jsonPath("$.subtareas[0].estado").value("pendiente"));
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
                null,
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
    void rechazaHorasCeroAlCrearSubtarea() throws Exception {
        BaseFixture base = crearBase(6);
        SubtareaDTO request = new SubtareaDTO(
                null, null, "Tarea sin horas", LocalDate.of(2026, 11, 10), 0,
                null, null, null);

        mockMvc.perform(post("/api/eventos/{id}/subtareas", base.evento().getIdEvento())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Datos inválidos"))
                .andExpect(jsonPath("$.errors.horasEstimadas").isNotEmpty());
    }

    @Test
    void rechazaHorasNegativasAlCrearSubtarea() throws Exception {
        BaseFixture base = crearBase(6);
        SubtareaDTO request = new SubtareaDTO(
                null, null, "Tarea con horas negativas", LocalDate.of(2026, 11, 10), -2,
                null, null, null);

        mockMvc.perform(post("/api/eventos/{id}/subtareas", base.evento().getIdEvento())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Datos inválidos"))
                .andExpect(jsonPath("$.errors.horasEstimadas").isNotEmpty());
    }

    @Test
    void rechazaHorasAlfanumericasAlCrearSubtarea() throws Exception {
        BaseFixture base = crearBase(6);

        mockMvc.perform(post("/api/eventos/{id}/subtareas", base.evento().getIdEvento())
                        .contentType("application/json")
                        .content("{\"nombreGestion\":\"Tarea\",\"fechaObjetivo\":\"2026-11-10\",\"horasEstimadas\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Solicitud inválida"));
    }

    @Test
    void rechazaHorasNulasAlCrearSubtarea() throws Exception {
        BaseFixture base = crearBase(6);
        SubtareaDTO request = new SubtareaDTO(
                null, null, "Tarea sin horas", LocalDate.of(2026, 11, 10), null,
                null, null, null);

        mockMvc.perform(post("/api/eventos/{id}/subtareas", base.evento().getIdEvento())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Datos inválidos"))
                .andExpect(jsonPath("$.errors.horasEstimadas").isNotEmpty());
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
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Límite diario excedido"))
                .andExpect(jsonPath("$.detail").value("La reprogramación supera el límite diario de 5 horas"))
                .andExpect(jsonPath("$.limiteDiario").value(5))
                .andExpect(jsonPath("$.horasAsignadasPreviamente").value(3))
                .andExpect(jsonPath("$.horasSolicitadas").value(3))
                .andExpect(jsonPath("$.horasPlanificadasTotales").value(6))
                .andExpect(jsonPath("$.excedente").value(1))
                .andExpect(jsonPath("$.fecha").value("2026-11-20"))
                .andExpect(jsonPath("$.idSubtarea").value(actual.getIdSubtarea()));

        assertThat(subtareaRepository.findById(actual.getIdSubtarea()))
                .isPresent()
                .get()
                .extracting(Subtarea::getFechaObjetivo)
                .isEqualTo(LocalDate.of(2026, 11, 11));
    }

    @Test
    void elExcedenteIndicaCuantasHorasHayQueLiberar() throws Exception {
        BaseFixture base = crearBase(4);
        LocalDate fechaNueva = LocalDate.of(2026, 11, 23);
        Subtarea actual = crearSubtarea(
                base.evento(), "Tarea larga", LocalDate.of(2026, 11, 12), 1,
                EstadoSubtarea.pendiente);
        crearSubtarea(base.evento(), "Carga existente", fechaNueva, 4,
                EstadoSubtarea.pendiente);

        mockMvc.perform(patch("/api/subtareas/{id}/reprogramar", actual.getIdSubtarea())
                        .contentType("application/json")
                        .content("{\"nuevaFecha\":\"2026-11-23\",\"nuevasHoras\":4}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.limiteDiario").value(4))
                .andExpect(jsonPath("$.horasPlanificadasTotales").value(8))
                .andExpect(jsonPath("$.excedente").value(4));
    }

    @Test
    void elLimiteDiarioNoIncluyeSubtareasDeOtroOrganizador() {
        BaseFixture primerOrganizador = crearBase(3);
        BaseFixture segundoOrganizador = crearBase(1);
        LocalDate fecha = LocalDate.of(2026, 11, 20);
        Subtarea actual = crearSubtarea(
                primerOrganizador.evento(), "Tarea propia", fecha, 1,
                EstadoSubtarea.pendiente);
        crearSubtarea(
                segundoOrganizador.evento(), "Tarea ajena", fecha, 2,
                EstadoSubtarea.pendiente);

        SubtareaDTO resultado = subtareaService.reprogramar(
                actual.getIdSubtarea(),
                new ReprogramarDTO(fecha, 2));

        assertThat(resultado.fechaObjetivo()).isEqualTo(fecha);
        assertThat(resultado.horasEstimadas()).isEqualTo(2);
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
                .andExpect(jsonPath("$.idSubtarea").value(actual.getIdSubtarea()))
                .andExpect(jsonPath("$.fechaObjetivo").value("2026-11-21"))
                .andExpect(jsonPath("$.horasEstimadas").value(3));

        entityManager.flush();
        entityManager.clear();
        assertThat(subtareaRepository.findById(actual.getIdSubtarea()))
                .isPresent()
                .get()
                .extracting(Subtarea::getHorasEstimadas)
                .isEqualTo(3);
    }

    @Test
    void reprogramarEnLaMismaFechaNoCuentaDosVecesLaMismaSubtarea() {
        BaseFixture base = crearBase(3);
        LocalDate fecha = LocalDate.of(2026, 11, 22);
        Subtarea actual = crearSubtarea(
                base.evento(), "Tarea actual", fecha, 2, EstadoSubtarea.pendiente);
        crearSubtarea(base.evento(), "Otra tarea", fecha, 1, EstadoSubtarea.pendiente);

        SubtareaDTO resultado = subtareaService.reprogramar(
                actual.getIdSubtarea(),
                new ReprogramarDTO(fecha, 2));

        assertThat(resultado.horasEstimadas()).isEqualTo(2);
    }

    @Test
    void consultaLaCapacidadDiariaContandoSoloLoNoEjecutado() throws Exception {
        BaseFixture base = crearBase(6);
        LocalDate fecha = LocalDate.of(2026, 11, 20);
        crearSubtarea(base.evento(), "Pendiente de ese día", fecha, 3,
                EstadoSubtarea.pendiente);
        crearSubtarea(base.evento(), "Pospuesta de ese día", fecha, 2,
                EstadoSubtarea.pospuesta);
        crearSubtarea(base.evento(), "Ejecutada de ese día", fecha, 4,
                EstadoSubtarea.ejecutada);
        crearSubtarea(base.evento(), "Trabajo de otro día", fecha.plusDays(1), 5,
                EstadoSubtarea.pendiente);

        mockMvc.perform(get("/api/usuarios/capacidad")
                        .param("usuarioId", String.valueOf(base.usuario().getIdUsuario()))
                        .param("fecha", fecha.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(base.usuario().getIdUsuario()))
                .andExpect(jsonPath("$.limiteHorasDiarias").value(6))
                .andExpect(jsonPath("$.fecha").value("2026-11-20"))
                .andExpect(jsonPath("$.horasPlanificadas").value(5))
                .andExpect(jsonPath("$.horasDisponibles").value(1));
    }

    @Test
    void lasHorasDisponiblesNoBajanDeCeroEnUnaFechaSobrecargada() {
        BaseFixture base = crearBase(2);
        LocalDate fecha = LocalDate.of(2026, 11, 21);
        crearSubtarea(base.evento(), "Sobrecarga del día", fecha, 5,
                EstadoSubtarea.pendiente);

        CapacidadDTO capacidad = usuarioService.obtenerCapacidad(
                base.usuario().getIdUsuario(), fecha);

        assertThat(capacidad.horasPlanificadas()).isEqualTo(5);
        assertThat(capacidad.horasDisponibles()).isZero();
    }

    @Test
    void laCapacidadDeUnOrganizadorInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/usuarios/capacidad")
                        .param("usuarioId", "9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.detail").value("No existe el usuario con id 9999"));
    }

    @Test
    void actualizaElLimiteDiarioYLoDevuelveRecalculado() throws Exception {
        BaseFixture base = crearBase(6);

        mockMvc.perform(put("/api/usuarios/capacidad")
                        .param("usuarioId", String.valueOf(base.usuario().getIdUsuario()))
                        .contentType("application/json")
                        .content("{\"limiteHorasDiarias\":8}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(base.usuario().getIdUsuario()))
                .andExpect(jsonPath("$.limiteHorasDiarias").value(8))
                .andExpect(jsonPath("$.horasDisponibles").value(8));

        entityManager.flush();
        entityManager.clear();
        assertThat(usuarioRepository.findById(base.usuario().getIdUsuario()))
                .isPresent()
                .get()
                .extracting(Usuario::getLimiteHorasDiarias)
                .isEqualTo(8);
    }

    @Test
    void rechazaLimitesDeHorasFueraDelRangoConMensajeEnEspanol() throws Exception {
        BaseFixture base = crearBase(6);
        String usuarioId = String.valueOf(base.usuario().getIdUsuario());

        mockMvc.perform(put("/api/usuarios/capacidad")
                        .param("usuarioId", usuarioId)
                        .contentType("application/json")
                        .content("{\"limiteHorasDiarias\":17}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Datos inválidos"))
                .andExpect(jsonPath("$.errors.limiteHorasDiarias")
                        .value("El límite de horas diarias no puede superar las 16 horas"));

        mockMvc.perform(put("/api/usuarios/capacidad")
                        .param("usuarioId", usuarioId)
                        .contentType("application/json")
                        .content("{\"limiteHorasDiarias\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.limiteHorasDiarias")
                        .value("El límite de horas diarias debe ser al menos 1 hora"));

        mockMvc.perform(put("/api/usuarios/capacidad")
                        .param("usuarioId", usuarioId)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.limiteHorasDiarias")
                        .value("El límite de horas diarias es obligatorio"));

        entityManager.flush();
        entityManager.clear();
        assertThat(usuarioRepository.findById(base.usuario().getIdUsuario()))
                .isPresent()
                .get()
                .extracting(Usuario::getLimiteHorasDiarias)
                .isEqualTo(6);
    }

    @Test
    void elServicioRechazaLimitesFueraDeRangoSinPasarPorElDto() {
        BaseFixture base = crearBase(6);
        Integer usuarioId = base.usuario().getIdUsuario();

        assertThatThrownBy(() -> usuarioService.actualizarLimite(usuarioId, 17))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entre 1 y 16 horas");
        assertThatThrownBy(() -> usuarioService.actualizarLimite(usuarioId, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entre 1 y 16 horas");
        assertThatThrownBy(() -> usuarioService.actualizarLimite(usuarioId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obligatorio");
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
                null,
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
                null,
                null);
    }

    private record BaseFixture(Usuario usuario, TipoEvento tipoEvento, Evento evento) {
    }
}

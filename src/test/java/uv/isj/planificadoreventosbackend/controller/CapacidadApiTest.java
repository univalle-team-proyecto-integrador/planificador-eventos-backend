package uv.isj.planificadoreventosbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import uv.isj.planificadoreventosbackend.model.EstadoSubtarea;
import uv.isj.planificadoreventosbackend.model.Evento;
import uv.isj.planificadoreventosbackend.model.Subtarea;
import uv.isj.planificadoreventosbackend.model.TipoEvento;
import uv.isj.planificadoreventosbackend.model.Usuario;
import uv.isj.planificadoreventosbackend.security.JwtService;

/**
 * Capacidad diaria y conflicto de límite en la reprogramación, de extremo a
 * extremo y con token: a diferencia de ApiSprint1Test, aquí el propietario sale
 * del JWT, que es como funciona en producción.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CapacidadApiTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 11, 20);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void reiniciarSecuencias() {
        entityManager.createNativeQuery("ALTER TABLE usuario ALTER COLUMN id_usuario RESTART WITH 1")
                .executeUpdate();
        entityManager.createNativeQuery("ALTER TABLE evento ALTER COLUMN id_evento RESTART WITH 1")
                .executeUpdate();
        entityManager.createNativeQuery("ALTER TABLE subtarea ALTER COLUMN id_subtarea RESTART WITH 1")
                .executeUpdate();
    }

    // --- Capacidad ----------------------------------------------------------

    @Test
    @DisplayName("La capacidad exige token")
    void capacidadSinTokenResponde401() throws Exception {
        mockMvc.perform(get("/api/users/capacity"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("No autenticado"));
    }

    @Test
    @DisplayName("La capacidad devuelve límite, comprometido y disponible")
    void capacidadDelUsuarioDelToken() throws Exception {
        Usuario dueno = crearUsuario(6);
        Evento evento = crearEvento(dueno);
        crearSubtarea(evento, "Pendiente", FECHA, 3, EstadoSubtarea.pendiente);
        crearSubtarea(evento, "Pospuesta", FECHA, 2, EstadoSubtarea.pospuesta);

        mockMvc.perform(get("/api/users/capacity")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(dueno))
                        .param("fecha", FECHA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(dueno.getIdUsuario()))
                .andExpect(jsonPath("$.limiteHorasDiarias").value(6))
                .andExpect(jsonPath("$.fecha").value(FECHA.toString()))
                .andExpect(jsonPath("$.horasPlanificadas").value(5))
                .andExpect(jsonPath("$.horasDisponibles").value(1));
    }

    @Test
    @DisplayName("Las subtareas ejecutadas no cuentan para la capacidad")
    void laCapacidadExcluyeLasEjecutadas() throws Exception {
        Usuario dueno = crearUsuario(8);
        Evento evento = crearEvento(dueno);
        crearSubtarea(evento, "Ya hecha", FECHA, 5, EstadoSubtarea.ejecutada);

        mockMvc.perform(get("/api/users/capacity")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(dueno))
                        .param("fecha", FECHA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horasPlanificadas").value(0))
                .andExpect(jsonPath("$.horasDisponibles").value(8));
    }

    @Test
    @DisplayName("La capacidad no ve las horas de otro organizador")
    void laCapacidadAislaPorPropietario() throws Exception {
        Usuario dueno = crearUsuario(6);
        Usuario ajeno = crearUsuario(6);
        crearSubtarea(crearEvento(ajeno), "Trabajo ajeno", FECHA, 4, EstadoSubtarea.pendiente);

        mockMvc.perform(get("/api/users/capacity")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(dueno))
                        .param("fecha", FECHA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horasPlanificadas").value(0))
                .andExpect(jsonPath("$.horasDisponibles").value(6));
    }

    // --- Ajuste del límite --------------------------------------------------

    @Test
    @DisplayName("Ajustar el límite persiste y devuelve la capacidad recalculada")
    void ajustarLimiteDevuelveLaCapacidadRecalculada() throws Exception {
        Usuario dueno = crearUsuario(6);
        crearSubtarea(crearEvento(dueno), "Pendiente", FECHA, 4, EstadoSubtarea.pendiente);

        mockMvc.perform(put("/api/users/capacity")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(dueno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"limiteHorasDiarias\":8}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limiteHorasDiarias").value(8));

        entityManager.flush();
        entityManager.clear();
        assertThat(usuarioActualizado(dueno.getIdUsuario()).getLimiteHorasDiarias()).isEqualTo(8);
    }

    @Test
    @DisplayName("Un límite fuera de 1 a 16 se rechaza con el mensaje en español")
    void ajustarLimiteFueraDeRangoResponde400() throws Exception {
        Usuario dueno = crearUsuario(6);

        mockMvc.perform(put("/api/users/capacity")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(dueno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"limiteHorasDiarias\":17}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Datos inválidos"))
                .andExpect(jsonPath("$.errors.limiteHorasDiarias")
                        .value("El límite de horas diarias no puede superar las 16 horas"));

        mockMvc.perform(put("/api/users/capacity")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(dueno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"limiteHorasDiarias\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.limiteHorasDiarias")
                        .value("El límite de horas diarias debe ser al menos 1 hora"));

        mockMvc.perform(put("/api/users/capacity")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(dueno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.limiteHorasDiarias")
                        .value("El límite de horas diarias es obligatorio"));

        entityManager.flush();
        entityManager.clear();
        assertThat(usuarioActualizado(dueno.getIdUsuario()).getLimiteHorasDiarias()).isEqualTo(6);
    }

    @Test
    @DisplayName("Ajustar el límite exige token")
    void ajustarLimiteSinTokenResponde401() throws Exception {
        mockMvc.perform(put("/api/users/capacity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"limiteHorasDiarias\":8}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("No autenticado"));
    }

    // --- 409 en la reprogramación -------------------------------------------

    @Test
    @DisplayName("Reprogramar por encima del límite responde 409 sin guardar")
    void reprogramarConSobrecargaResponde409() throws Exception {
        Usuario dueno = crearUsuario(5);
        Evento evento = crearEvento(dueno);
        Subtarea mover = crearSubtarea(evento, "Se reprograma", FECHA.plusDays(1), 1,
                EstadoSubtarea.pendiente);
        crearSubtarea(evento, "Ya ocupado", FECHA, 3, EstadoSubtarea.pendiente);

        mockMvc.perform(patch("/api/subtareas/{id}/reprogramar", mover.getIdSubtarea())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(dueno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevaFecha\":\"" + FECHA + "\",\"nuevasHoras\":3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Límite diario excedido"))
                .andExpect(jsonPath("$.detail")
                        .value("La reprogramación supera el límite diario de 5 horas"))
                .andExpect(jsonPath("$.limiteDiario").value(5))
                .andExpect(jsonPath("$.horasAsignadasPreviamente").value(3))
                .andExpect(jsonPath("$.horasSolicitadas").value(3))
                .andExpect(jsonPath("$.horasPlanificadasTotales").value(6))
                .andExpect(jsonPath("$.excedente").value(1))
                .andExpect(jsonPath("$.fecha").value(FECHA.toString()))
                .andExpect(jsonPath("$.idSubtarea").value(mover.getIdSubtarea()));

        entityManager.flush();
        entityManager.clear();
        assertThat(subtareaActualizada(mover.getIdSubtarea(), dueno.getIdUsuario())
                .getFechaObjetivo()).isEqualTo(FECHA.plusDays(1));
    }

    @Test
    @DisplayName("Cuando cabe en el límite responde 200 y guarda")
    void reprogramarDentroDelLimiteResponde200() throws Exception {
        Usuario dueno = crearUsuario(5);
        Evento evento = crearEvento(dueno);
        Subtarea mover = crearSubtarea(evento, "Se reprograma", FECHA.plusDays(1), 1,
                EstadoSubtarea.pendiente);
        crearSubtarea(evento, "Ya ocupado", FECHA, 2, EstadoSubtarea.pendiente);

        mockMvc.perform(patch("/api/subtareas/{id}/reprogramar", mover.getIdSubtarea())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(dueno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevaFecha\":\"" + FECHA + "\",\"nuevasHoras\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idSubtarea").value(mover.getIdSubtarea()))
                .andExpect(jsonPath("$.fechaObjetivo").value(FECHA.toString()))
                .andExpect(jsonPath("$.horasEstimadas").value(3));

        entityManager.flush();
        entityManager.clear();
        Subtarea guardada = subtareaActualizada(mover.getIdSubtarea(), dueno.getIdUsuario());
        assertThat(guardada.getFechaObjetivo()).isEqualTo(FECHA);
        assertThat(guardada.getHorasEstimadas()).isEqualTo(3);
    }

    @Test
    @DisplayName("El límite del otro organizador no cuenta en la reprogramación")
    void reprogramarUsaElLimiteDelPropietarioDelToken() throws Exception {
        Usuario dueno = crearUsuario(5);
        Usuario ajeno = crearUsuario(1);
        crearSubtarea(crearEvento(ajeno), "Carga ajena", FECHA, 8, EstadoSubtarea.pendiente);
        Subtarea propia = crearSubtarea(crearEvento(dueno), "Propia", FECHA.plusDays(1), 1,
                EstadoSubtarea.pendiente);

        mockMvc.perform(patch("/api/subtareas/{id}/reprogramar", propia.getIdSubtarea())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(dueno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevaFecha\":\"" + FECHA + "\",\"nuevasHoras\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horasEstimadas").value(4));
    }

    // --- Línea base del desfase ---------------------------------------------

    @Test
    @DisplayName("Reprogramar guarda la fecha con la que se planificó")
    void reprogramarGuardaLaFechaOriginal() throws Exception {
        Usuario dueno = crearUsuario(8);
        Evento evento = crearEvento(dueno);
        Subtarea mover = crearSubtarea(evento, "Se adelanta", FECHA.plusDays(4), 2,
                EstadoSubtarea.pendiente);

        mockMvc.perform(patch("/api/subtareas/{id}/reprogramar", mover.getIdSubtarea())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(dueno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevaFecha\":\"" + FECHA + "\",\"nuevasHoras\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaObjetivo").value(FECHA.toString()))
                .andExpect(jsonPath("$.fechaObjetivoOriginal").value(FECHA.plusDays(4).toString()));
    }

    @Test
    @DisplayName("Reprogramar dos veces no arrastra la línea base")
    void reprogramarVariasVecesMantieneLaPrimeraFecha() throws Exception {
        Usuario dueno = crearUsuario(16);
        Evento evento = crearEvento(dueno);
        Subtarea mover = crearSubtarea(evento, "Va y viene", FECHA.plusDays(5), 1,
                EstadoSubtarea.pendiente);

        reprogramar(mover.getIdSubtarea(), dueno, FECHA.plusDays(1));
        reprogramar(mover.getIdSubtarea(), dueno, FECHA.plusDays(2));

        entityManager.flush();
        entityManager.clear();
        Subtarea guardada = subtareaActualizada(mover.getIdSubtarea(), dueno.getIdUsuario());
        assertThat(guardada.getFechaObjetivo()).isEqualTo(FECHA.plusDays(2));
        assertThat(guardada.getFechaObjetivoOriginal()).isEqualTo(FECHA.plusDays(5));
    }

    @Test
    @DisplayName("Cambiar solo las horas no marca la gestión como reprogramada")
    void reprogramarSinCambioDeFechaNoFijaLineaBase() throws Exception {
        Usuario dueno = crearUsuario(8);
        Evento evento = crearEvento(dueno);
        Subtarea misma = crearSubtarea(evento, "Solo cambian las horas", FECHA, 2,
                EstadoSubtarea.pendiente);

        mockMvc.perform(patch("/api/subtareas/{id}/reprogramar", misma.getIdSubtarea())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(dueno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevaFecha\":\"" + FECHA + "\",\"nuevasHoras\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaObjetivoOriginal").doesNotExist());
    }

    @Test
    @DisplayName("Editar la fecha también deja la línea base original")
    void editarSubtareaFijaLaFechaOriginal() throws Exception {
        Usuario dueno = crearUsuario(8);
        Evento evento = crearEvento(dueno);
        Subtarea editar = crearSubtarea(evento, "Se posterga", FECHA, 3,
                EstadoSubtarea.pendiente);

        mockMvc.perform(put("/api/subtareas/{id}", editar.getIdSubtarea())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(dueno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreGestion\":\"Se posterga\",\"fechaObjetivo\":\""
                                + FECHA.plusDays(3) + "\",\"horasEstimadas\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaObjetivo").value(FECHA.plusDays(3).toString()))
                .andExpect(jsonPath("$.fechaObjetivoOriginal").value(FECHA.toString()));
    }

    // --- US-08: resolver el conflicto reduciendo horas ------------------------

    @Test
    @DisplayName("Reducir horas resuelve el conflicto y lo dice")
    void reducirHorasResuelveElConflicto() throws Exception {
        Usuario dueno = crearUsuario(6);
        Evento evento = crearEvento(dueno);
        // El día tiene 5h ajenas + 4h de esta = 9h sobre un límite de 6.
        Subtarea propia = crearSubtarea(evento, "Carga del día", FECHA, 4, EstadoSubtarea.pendiente);
        crearSubtarea(evento, "Otra carga", FECHA, 5, EstadoSubtarea.pendiente);

        mockMvc.perform(put("/api/subtareas/{id}", propia.getIdSubtarea())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(dueno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreGestion\":\"Carga del día\",\"fechaObjetivo\":\""
                                + FECHA + "\",\"horasEstimadas\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horasEstimadas").value(1))
                .andExpect(jsonPath("$.resuelto").value(true));
    }

    @Test
    @DisplayName("Reducir sin llegar a resolver guarda y avisa que persiste")
    void reducirHorasSinAlcanzarGuardaYAvisa() throws Exception {
        Usuario dueno = crearUsuario(6);
        Evento evento = crearEvento(dueno);
        // 5h ajenas + esta de 6h = 11h sobre un límite de 6. Bajar a 4h deja
        // 9h: sigue pasándose, pero es menos que antes, así que guarda.
        Subtarea propia = crearSubtarea(evento, "Se reduce", FECHA, 6, EstadoSubtarea.pendiente);
        crearSubtarea(evento, "Otra carga", FECHA, 5, EstadoSubtarea.pendiente);

        mockMvc.perform(put("/api/subtareas/{id}", propia.getIdSubtarea())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(dueno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreGestion\":\"Se reduce\",\"fechaObjetivo\":\""
                                + FECHA + "\",\"horasEstimadas\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horasEstimadas").value(4))
                .andExpect(jsonPath("$.resuelto").value(false));
    }

    @Test
    @DisplayName("Aumentar horas por encima del límite responde 409 sin guardar")
    void aumentarHorasQueEmpeoranElDiaResponde409() throws Exception {
        Usuario dueno = crearUsuario(6);
        Evento evento = crearEvento(dueno);
        Subtarea propia = crearSubtarea(evento, "Crece", FECHA, 2, EstadoSubtarea.pendiente);
        crearSubtarea(evento, "Otra carga", FECHA, 5, EstadoSubtarea.pendiente);

        mockMvc.perform(put("/api/subtareas/{id}", propia.getIdSubtarea())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(dueno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreGestion\":\"Crece\",\"fechaObjetivo\":\""
                                + FECHA + "\",\"horasEstimadas\":6}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Límite diario excedido"));

        entityManager.flush();
        entityManager.clear();
        assertThat(subtareaActualizada(propia.getIdSubtarea(), dueno.getIdUsuario())
                .getHorasEstimadas()).isEqualTo(2);
    }

    @Test
    @DisplayName("Renombrar una gestión en un día sobrecargado no da 409")
    void renombrarNoDisparaElLimite() throws Exception {
        Usuario dueno = crearUsuario(4);
        Evento evento = crearEvento(dueno);
        // El día ya está por encima del límite de 4h antes de editar nada.
        Subtarea propia = crearSubtarea(evento, "Nombre viejo", FECHA, 6, EstadoSubtarea.pendiente);

        mockMvc.perform(put("/api/subtareas/{id}", propia.getIdSubtarea())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(dueno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreGestion\":\"Nombre nuevo\",\"fechaObjetivo\":\""
                                + FECHA + "\",\"horasEstimadas\":6}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreGestion").value("Nombre nuevo"));
    }

    private void reprogramar(int idSubtarea, Usuario dueno, LocalDate nuevaFecha) throws Exception {
        mockMvc.perform(patch("/api/subtareas/{id}/reprogramar", idSubtarea)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(dueno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevaFecha\":\"" + nuevaFecha + "\",\"nuevasHoras\":1}"))
                .andExpect(status().isOk());
    }

    // --- Fixtures -----------------------------------------------------------

    private Usuario crearUsuario(int limiteHorasDiarias) {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);

        Usuario usuario = new Usuario();
        usuario.setEmail("organizador-" + sufijo + "@example.com");
        usuario.setPasswordHash("hash-de-prueba");
        usuario.setNombre("Organizador de prueba");
        usuario.setLimiteHorasDiarias(limiteHorasDiarias);

        entityManager.persist(usuario);
        entityManager.flush();
        return usuario;
    }

    private Evento crearEvento(Usuario usuario) {
        TipoEvento tipo = new TipoEvento();
        tipo.setNombre("Tipo " + UUID.randomUUID().toString().substring(0, 8));

        Evento evento = new Evento();
        evento.setUsuario(usuario);
        evento.setTipoEvento(tipo);
        evento.setNombre("Evento de prueba");
        evento.setCliente("Cliente de prueba");
        evento.setFechaEvento(java.time.LocalDateTime.of(2026, 12, 1, 15, 0));
        evento.setLugar("Cali");

        entityManager.persist(tipo);
        entityManager.persist(evento);
        entityManager.flush();
        return evento;
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

    private Usuario usuarioActualizado(Integer idUsuario) {
        return entityManager.find(Usuario.class, idUsuario);
    }

    private Subtarea subtareaActualizada(Integer idSubtarea, Integer idUsuario) {
        return entityManager.createQuery(
                        "SELECT s FROM Subtarea s WHERE s.idSubtarea = :id "
                                + "AND s.evento.usuario.idUsuario = :usuario", Subtarea.class)
                .setParameter("id", idSubtarea)
                .setParameter("usuario", idUsuario)
                .getSingleResult();
    }

    private String tokenDe(Usuario usuario) {
        return jwtService.generarToken(usuario.getIdUsuario(), usuario.getEmail());
    }
}
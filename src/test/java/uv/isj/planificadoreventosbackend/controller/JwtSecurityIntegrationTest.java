package uv.isj.planificadoreventosbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import uv.isj.planificadoreventosbackend.model.Evento;
import uv.isj.planificadoreventosbackend.model.EstadoSubtarea;
import uv.isj.planificadoreventosbackend.model.Subtarea;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import uv.isj.planificadoreventosbackend.model.TipoEvento;
import uv.isj.planificadoreventosbackend.model.Usuario;
import uv.isj.planificadoreventosbackend.repository.EventoRepository;
import uv.isj.planificadoreventosbackend.security.JwtService;

/**
 * US-11 con la protección activa. A diferencia del resto de la suite, aquí
 * app.security.protect-subtareas=true: /api/subtareas/** exige token de verdad.
 *
 * <p>Es la red que detecta una regresión de CORS contra seguridad: el preflight
 * de una ruta protegida debe seguir respondiendo 200 y no 401.
 */
@SpringBootTest(properties = "app.security.protect-subtareas=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JwtSecurityIntegrationTest {

    private static final String CONTRASENA = "Planificador2026";

    /** Debe coincidir con app.security.jwt.expiration-seconds del perfil test. */
    private static final long EXPIRATION_SEGUNDOS = 3600L;

    /** Debe coincidir con app.security.jwt.secret del perfil test. */
    private static final String SECRETO_DE_PRUEBAS = "secreto-de-pruebas-us11-minimo-32-caracteres";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EventoRepository eventoRepository;

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

    // --- 401 en rutas protegidas -------------------------------------------

    @Test
    @DisplayName("Sin token, una subtarea responde 401 con ProblemDetail")
    void sinTokenRespondeProblemDetail() throws Exception {
        mockMvc.perform(get("/api/subtareas/hoy").param("usuarioId", "1"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("No autenticado"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    @DisplayName("Un token corrupto no autentica y responde 401")
    void tokenCorruptoResponde401() throws Exception {
        mockMvc.perform(get("/api/subtareas/hoy")
                        .param("usuarioId", "1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer no-es-un-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("No autenticado"));
    }

    @Test
    @DisplayName("Un token firmado con otro secreto no autentica")
    void tokenDeOtroSecretoResponde401() throws Exception {
        String secretoAjeno = "otro-secreto-de-pruebas-que-tiene-32-caracteres";
        String tokenAjeno = tokenFirmadoConSecreto(secretoAjeno, 1, "intruso@uni.edu");

        mockMvc.perform(get("/api/subtareas/hoy")
                        .param("usuarioId", "1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAjeno))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El perfil exige token siempre")
    void perfilSinTokenResponde401() throws Exception {
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("No autenticado"));
    }

    // --- Rutas que deben seguir abiertas ----------------------------------

    @Test
    @DisplayName("Health sigue abierto: es el healthCheckPath de Render")
    void healthSigueAbierto() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("OpenAPI sigue abierto")
    void openApiSigueAbierto() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("El preflight de una ruta protegida responde 200, no 401")
    void preflightDeRutaProtegidaResponde200() throws Exception {
        mockMvc.perform(options("/api/subtareas/1")
                        .header(HttpHeaders.ORIGIN, "https://planificador-eventos-frontend-ten.vercel.app")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "https://planificador-eventos-frontend-ten.vercel.app"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    // --- Flujo completo ----------------------------------------------------

    @Test
    @DisplayName("Registro, login y perfil encadenan con el mismo token")
    void registroLoginYPerfil() throws Exception {
        String email = "nuevo-" + UUID.randomUUID().toString().substring(0, 8) + "@uni.edu";

        var registro = mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","nombre":"Nueva Organizadora","password":"%s"}
                                """.formatted(email, CONTRASENA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.usuario.email").value(email))
                .andExpect(jsonPath("$.usuario.limiteHorasDiarias").value(6))
                .andReturn();
        String token = json(registro).path("token").asText();

        // El hash guardado debe ser BCrypt, nunca la contraseña en claro.
        var login = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, CONTRASENA)))
                .andExpect(status().isOk())
                .andReturn();
        String tokenLogin = json(login).path("token").asText();

        assertThat(tokenLogin).isNotBlank();

        mockMvc.perform(get("/api/users/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.nombre").value("Nueva Organizadora"));
    }

    @Test
    @DisplayName("Un token bien firmado pero vencido no autentica")
    void tokenVencidoResponde401() throws Exception {
        Usuario usuario = crearUsuario("vencido");
        String vencido = tokenVencido(usuario.getIdUsuario(), usuario.getEmail());

        // Con el mismo token, /api/users/profile cae en el punto exacto de la
        // expiracion: la firma es correcta, solo caduca el claim exp.
        mockMvc.perform(get("/api/users/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + vencido))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("No autenticado"));

        mockMvc.perform(get("/api/subtareas/hoy")
                        .param("usuarioId", "1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + vencido))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("La caducidad sale de JWT_EXPIRATION_SECONDS y el token la respeta")
    void caducidadVieneDeLaConfiguracionEnSegundos() throws Exception {
        String email = "caduca-" + UUID.randomUUID().toString().substring(0, 8) + "@uni.edu";

        var registro = mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","nombre":"Caduca","password":"%s"}
                                """.formatted(email, CONTRASENA)))
                .andExpect(status().isCreated())
                .andReturn();

        // expiresIn es la unidad de la configuracion, en segundos.
        assertThat(json(registro).path("expiresIn").asLong()).isEqualTo(EXPIRATION_SEGUNDOS);

        // El claim exp del token tiene que coincidir con expiresIn: si la
        // conversion se desincroniza, el DTO miente sobre la caducidad real.
        var decodificado = jwtDecoder.decode(json(registro).path("token").asText());
        assertThat(decodificado.getExpiresAt().getEpochSecond()
                - decodificado.getIssuedAt().getEpochSecond()).isEqualTo(EXPIRATION_SEGUNDOS);
    }

    @Test
    @DisplayName("El correo se normaliza a minúsculas y el duplicado da 409")
    void correoDuplicadoResponde409() throws Exception {
        String email = "dup-" + UUID.randomUUID().toString().substring(0, 8) + "@uni.edu";
        String cuerpo = """
                {"email":"%s","nombre":"Usuario","password":"%s"}
                """.formatted(email, CONTRASENA);

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated());

        // Mismo correo en mayusculas: debe normalizarse y chocar con el UNIQUE.
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","nombre":"Otro Usuario","password":"%s"}
                                """.formatted(email.toUpperCase(), CONTRASENA)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Correo ya registrado"));
    }

    @Test
    @DisplayName("Credenciales incorrectas dan 401 sin revelar si el correo existe")
    void credencialesIncorrectasNoRevelanElCorreo() throws Exception {
        Usuario existente = crearUsuario("existente@uni.edu");

        String correoInexistente = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nadie@uni.edu","password":"%s"}
                                """.formatted(CONTRASENA)))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String correoExistente = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"ClaveEquivocada1"}
                                """.formatted(existente.getEmail())))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(correoInexistente).isEqualTo(correoExistente);
    }

    @Test
    @DisplayName("Una contraseña débil da 400 con el detalle del campo")
    void contrasenaDebilDa400() throws Exception {
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"debil@uni.edu","nombre":"Débil","password":"corta"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Datos inválidos"))
                .andExpect(jsonPath("$.errors.password").isNotEmpty());
    }

    // --- Aislamiento entre usuarios ---------------------------------------

    @Test
    @DisplayName("Un usuario autenticado no lee la subtarea de otro")
    void noLeeSubtareaAjena() throws Exception {
        Usuario ajeno = crearUsuario("ajeno@uni.edu");
        Subtarea deOtro = crearEventoYSubtarea(ajeno, "Tarea ajena", 2);

        Usuario propio = crearUsuario("propio@uni.edu");
        String token = tokenDe(propio);

        mockMvc.perform(get("/api/subtareas/{id}", deOtro.getIdSubtarea())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un usuario autenticado no edita la subtarea de otro")
    void noEditaSubtareaAjena() throws Exception {
        Usuario ajeno = crearUsuario("ajeno-edit@uni.edu");
        Subtarea deOtro = crearEventoYSubtarea(ajeno, "Tarea ajena", 3);

        Usuario propio = crearUsuario("propio-edit@uni.edu");
        String token = tokenDe(propio);

        mockMvc.perform(patch("/api/subtareas/{id}/estado", deOtro.getIdSubtarea())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"ejecutada"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un usuario autenticado no borra la subtarea de otro")
    void noBorraSubtareaAjena() throws Exception {
        Usuario ajeno = crearUsuario("ajeno-del@uni.edu");
        Subtarea deOtro = crearEventoYSubtarea(ajeno, "Tarea ajena", 1);

        Usuario propio = crearUsuario("propio-del@uni.edu");
        String token = tokenDe(propio);

        mockMvc.perform(delete("/api/subtareas/{id}", deOtro.getIdSubtarea())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Con token, el query param usuarioId se ignora: manda el del token")
    void elTokenPrevaleceSobreElQueryParam() throws Exception {
        Usuario victimario = crearUsuario("victima@uni.edu");
        crearEventoYSubtarea(victimario, "Tarea de la víctima", 2);

        Usuario atacante = crearUsuario("atacante@uni.edu");
        crearEventoYSubtarea(atacante, "Tarea del atacante", 1);

        String token = tokenDe(atacante);

        // Pide la carga del día de la víctima: el token del atacante manda.
        mockMvc.perform(get("/api/subtareas/hoy")
                        .param("usuarioId", String.valueOf(victimario.getIdUsuario()))
                        .param("fecha", "2026-11-10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombreGestion").value("Tarea del atacante"));
    }

    @Test
    @DisplayName("Con token, el detalle de un evento ajeno responde 404")
    void elDetalleDeUnEventoAjenoResponde404() throws Exception {
        Usuario victimario = crearUsuario("victima-evento@uni.edu");
        Evento eventoAjeno = crearEvento(victimario, "Evento ajeno");

        Usuario atacante = crearUsuario("atacante-evento@uni.edu");
        String token = tokenDe(atacante);

        mockMvc.perform(get("/api/eventos/{id}", eventoAjeno.getIdEvento())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Con token, el propietario sí ve su propio evento con sus subtareas")
    void elDuenoVeSuEventoConSusSubtareas() throws Exception {
        Usuario dueno = crearUsuario("dueno@uni.edu");
        Evento evento = crearEvento(dueno, "Evento propio");
        crearSubtarea(evento, "Tarea propia", 2);

        String token = tokenDe(dueno);

        mockMvc.perform(get("/api/eventos/{id}", evento.getIdEvento())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Evento propio"))
                .andExpect(jsonPath("$.subtareas.length()").value(1))
                .andExpect(jsonPath("$.subtareas[0].nombreGestion").value("Tarea propia"));
    }

    @Test
    @DisplayName("Con token, listar eventos ignora el usuarioId del query y devuelve solo los suyos")
    void listarEventosDevuelveSoloLosDelToken() throws Exception {
        Usuario dueno = crearUsuario("lista-dueno@uni.edu");
        crearEvento(dueno, "Evento del dueño");

        Usuario victima = crearUsuario("lista-victima@uni.edu");
        crearEvento(victima, "Evento de la víctima");

        String token = tokenDe(dueno);

        mockMvc.perform(get("/api/eventos")
                        .param("usuarioId", String.valueOf(victima.getIdUsuario()))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Evento del dueño"));
    }

    @Test
    @DisplayName("Con token, crear un evento lo asigna al dueño aunque el cuerpo pida otro usuario")
    void crearEventoAsignaElPropietarioDelToken() throws Exception {
        Usuario dueno = crearUsuario("crear-dueno@uni.edu");
        Usuario victima = crearUsuario("crear-victima@uni.edu");
        TipoEvento tipo = tipoEventoPersistido();

        String token = tokenDe(dueno);

        mockMvc.perform(post("/api/eventos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonEvento(victima.getIdUsuario(), tipo.getIdTipoEvento())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idUsuario").value(dueno.getIdUsuario()));
    }

    @Test
    @DisplayName("Con token, crear un evento sin idUsuario en el cuerpo lo asigna al dueño del token")
    void crearEventoSinIdUsuarioUsaElPropietarioDelToken() throws Exception {
        Usuario dueno = crearUsuario("crear-sin-id-dueno@uni.edu");
        TipoEvento tipo = tipoEventoPersistido();

        mockMvc.perform(post("/api/eventos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(dueno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonEventoSinIdUsuario(tipo.getIdTipoEvento())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idUsuario").value(dueno.getIdUsuario()));
    }

    @Test
    @DisplayName("Con token, hoy sin usuarioId en el query devuelve solo lo del token")
    void hoySinUsuarioIdEnElQueryUsaElPropietarioDelToken() throws Exception {
        Usuario victima = crearUsuario("hoy-sin-id-victima@uni.edu");
        crearEventoYSubtarea(victima, "Tarea de la víctima", 2);

        Usuario atacante = crearUsuario("hoy-sin-id-atacante@uni.edu");
        crearEventoYSubtarea(atacante, "Tarea del atacante", 1);

        // Sin usuarioId en el query: el propietario sale solo del token.
        mockMvc.perform(get("/api/subtareas/hoy")
                        .param("fecha", "2026-11-10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(atacante)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombreGestion").value("Tarea del atacante"));
    }

    @Test
    @DisplayName("Con token, actualizar un evento ajeno responde 404 y no lo modifica")
    void actualizarUnEventoAjenoResponde404() throws Exception {
        Usuario victima = crearUsuario("update-victima@uni.edu");
        Evento eventoAjeno = crearEvento(victima, "Evento de la víctima");
        String tokenDeLaVictima = tokenDe(victima);

        Usuario atacante = crearUsuario("update-atacante@uni.edu");
        TipoEvento tipo = tipoEventoPersistido();
        String token = tokenDe(atacante);

        mockMvc.perform(put("/api/eventos/{id}", eventoAjeno.getIdEvento())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonEvento(atacante.getIdUsuario(), tipo.getIdTipoEvento())))
                .andExpect(status().isNotFound());

        // La víctima sigue viendo su evento intacto.
        mockMvc.perform(get("/api/eventos/{id}", eventoAjeno.getIdEvento())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDeLaVictima))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Evento de la víctima"));
    }

    @Test
    @DisplayName("Con token, eliminar un evento ajeno responde 404 y no lo borra")
    void eliminarUnEventoAjenoResponde404() throws Exception {
        Usuario victima = crearUsuario("delete-victima@uni.edu");
        Evento eventoAjeno = crearEvento(victima, "Evento de la víctima");

        Usuario atacante = crearUsuario("delete-atacante@uni.edu");
        String token = tokenDe(atacante);

        mockMvc.perform(delete("/api/eventos/{id}", eventoAjeno.getIdEvento())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());

        assertThat(eventoRepository.findByIdYUsuarioId(
                        eventoAjeno.getIdEvento(), victima.getIdUsuario()))
                .isPresent();
    }

    // --- Utilidades --------------------------------------------------------

    private Usuario crearUsuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setNombre("Usuario de prueba");
        usuario.setPasswordHash(passwordEncoder.encode(CONTRASENA));
        usuario.setLimiteHorasDiarias(6);
        entityManager.persist(usuario);
        entityManager.flush();
        return usuario;
    }

    private TipoEvento tipoEventoPersistido() {
        TipoEvento tipo = new TipoEvento();
        tipo.setNombre("Tipo " + UUID.randomUUID().toString().substring(0, 8));
        entityManager.persist(tipo);
        entityManager.flush();
        return tipo;
    }

    private String jsonEvento(Integer usuarioId, Integer idTipoEvento) {
        return String.join("\n",
                "{",
                "  \"idUsuario\": " + usuarioId + ",",
                "  \"idTipoEvento\": " + idTipoEvento + ",",
                "  \"nombre\": \"Boda de prueba\",",
                "  \"cliente\": \"María y Luis\",",
                "  \"fechaEvento\": \"2026-12-01T15:00:00\",",
                "  \"lugar\": \"Cali\"",
                "}");
    }

    private String jsonEventoSinIdUsuario(Integer idTipoEvento) {
        return String.join("\n",
                "{",
                "  \"idTipoEvento\": " + idTipoEvento + ",",
                "  \"nombre\": \"Boda de prueba\",",
                "  \"cliente\": \"María y Luis\",",
                "  \"fechaEvento\": \"2026-12-01T15:00:00\",",
                "  \"lugar\": \"Cali\"",
                "}");
    }

    private Evento crearEvento(Usuario usuario, String nombre) {
        TipoEvento tipo = new TipoEvento();
        tipo.setNombre("Tipo " + UUID.randomUUID().toString().substring(0, 8));

        Evento evento = new Evento();
        evento.setUsuario(usuario);
        evento.setTipoEvento(tipo);
        evento.setNombre(nombre);
        evento.setCliente("Cliente de prueba");
        evento.setFechaEvento(LocalDateTime.of(2026, 12, 1, 15, 0));
        evento.setLugar("Cali");

        entityManager.persist(tipo);
        entityManager.persist(evento);
        entityManager.flush();
        return evento;
    }

    private Subtarea crearSubtarea(Evento evento, String nombre, int horas) {
        Subtarea subtarea = new Subtarea();
        evento.agregarSubtarea(subtarea);
        subtarea.setNombreGestion(nombre);
        subtarea.setFechaObjetivo(LocalDate.of(2026, 11, 10));
        subtarea.setHorasEstimadas(horas);
        subtarea.setEstado(EstadoSubtarea.pendiente);
        entityManager.flush();
        return subtarea;
    }

    private Subtarea crearEventoYSubtarea(Usuario usuario, String nombre, int horas) {
        Evento evento = crearEvento(usuario, "Evento de " + usuario.getEmail());
        return crearSubtarea(evento, nombre, horas);
    }

    private String tokenDe(Usuario usuario) {
        return jwtService.generarToken(usuario.getIdUsuario(), usuario.getEmail());
    }

    /** Token firmado con un secreto distinto al configurado en la app. */
    private String tokenFirmadoConSecreto(String secreto, Integer idUsuario, String email) {
        return JwtServiceConSecretoHelper.firmar(secreto, idUsuario, email);
    }

    /**
     * Token correctamente firmado con el secreto de la app, pero ya vencido.
     * Distingue el rechazo por caducidad del rechazo por firma inválida: si el
     * decoder no mirara el claim {@code exp}, esta prueba devolvería 200.
     */
    private String tokenVencido(Integer idUsuario, String email) {
        return JwtServiceConSecretoHelper.firmarVencido(SECRETO_DE_PRUEBAS, idUsuario, email);
    }

    private tools.jackson.databind.JsonNode json(org.springframework.test.web.servlet.MvcResult result)
            throws Exception {
        return new tools.jackson.databind.json.JsonMapper()
                .readTree(result.getResponse().getContentAsString());
    }
}

package uv.isj.planificadoreventosbackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import uv.isj.planificadoreventosbackend.security.JwtAuthenticationFilter;
import uv.isj.planificadoreventosbackend.security.JwtService;
import uv.isj.planificadoreventosbackend.security.RestAuthenticationEntryPoint;

/**
 * Cadena de seguridad stateless para US-11.
 *
 * <p>El acceso a los datos del organizador depende de
 * {@code app.security.protect-subtareas} para poder cerrarlo por configuración
 * una vez que el frontend envíe el token, sin redesplegar. Con la bandera en
 * {@code false} la capa de datos sigue aislando por propietario (ver
 * CurrentUserProvider), de modo que el comportamiento observable es el de
 * siempre.
 *
 * <p><strong>Alcance del flag.</strong> El nombre dice "subtareas", pero lo
 * que gobierna es el acceso a los datos del organizador en general, así que
 * cubre tanto {@code /api/eventos/**} como {@code /api/subtareas/**}. Antes de
 * esta aclaración, {@code /api/eventos/**} caía en
 * {@code anyRequest().permitAll()} y la exigencia de token llegaba más tarde, desde
 * {@code CurrentUserProvider} en el controlador: la cadena declaraba público lo
 * que en realidad respondía 401. Aplicar la misma regla a ambos grupos deja la
 * cadena y el controlador de acuerdo sin cambiar el comportamiento observable
 * en ninguno de los dos valores de la bandera.
 *
 * <p>El perfil exige token siempre: no tiene sentido una vista de perfil
 * anónima y el frontend ya la pide autenticada.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final CorsConfigurationSource corsConfigurationSource;
    private final boolean protegerSubtareas;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            CorsConfigurationSource corsConfigurationSource,
            @Value("${app.security.protect-subtareas:false}") boolean protegerSubtareas) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.corsConfigurationSource = corsConfigurationSource;
        this.protegerSubtareas = protegerSubtareas;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Interruptor de despliegue: las rutas de eventos y subtareas solo exigen
        // token cuando app.security.protect-subtareas está activo en el
        // entorno. Con la bandera apagada el acceso sigue abierto, pero
        // CurrentUserProvider acota los datos al usuario legado.
        //
        // El TrustResolver no es opcional: AnonymousAuthenticationToken tiene
        // isAuthenticated() == true, así que un "autenticado &&" a secas
        // dejaría pasar justamente las peticiones sin token que hay que
        // rechazar. Es el mismo criterio que usa authenticated().
        AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();
        AuthorizationManager<RequestAuthorizationContext> accesoProtegido =
                (autenticacion, contexto) -> {
                    Authentication actual = autenticacion.get();
                    boolean permitida = !protegerSubtareas
                            || (actual != null
                                    && actual.isAuthenticated()
                                    && !trustResolver.isAnonymous(actual));
                    return new AuthorizationDecision(permitida);
                };

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sesion -> sesion
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(excepciones -> excepciones
                        .authenticationEntryPoint(authenticationEntryPoint))
                .authorizeHttpRequests(reglas -> reglas
                        // Preflight: responde el CorsFilter, sin credenciales que validar.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Health es el healthCheckPath de Render; bloquearlo deja el servicio unhealthy.
                        .requestMatchers("/api/health", "/api/health/").permitAll()
                        // OpenAPI y Swagger: públicos por diseño y cubiertos por pruebas.
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**",
                                "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        // Alta y acceso: deben funcionar sin token.
                        .requestMatchers("/api/users/register", "/api/users/register/",
                                "/api/users/login", "/api/users/login/").permitAll()
                        .requestMatchers("/api/users/profile", "/api/users/profile/").authenticated()
                        // Capacidad y límite diario: siempre autenticados. Sin esta
                        // regla caerían en anyRequest().permitAll() de más abajo y
                        // quedarían abiertos; idUsuarioRequerido() refuerza en el service.
                        .requestMatchers("/api/users/capacity", "/api/users/capacity/").authenticated()
                        .requestMatchers(patronesProtegidos()).access(accesoProtegido)
                        .anyRequest().permitAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Rutas que exigen token cuando la bandera de despliegue está activa.
     * Eventos y subtareas van juntos porque ambas leen el propietario a través
     * de CurrentUserProvider, que es quien corta el acceso sin token.
     */
    private String[] patronesProtegidos() {
        return new String[] { "/api/eventos/**", "/api/subtareas/**" };
    }

    @Bean
    public static JwtEncoder jwtEncoder(
            @Value("${app.security.jwt.secret}") String secreto) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(JwtService.claveDesdeSecreto(secreto)));
    }

    @Bean
    public static JwtDecoder jwtDecoder(
            @Value("${app.security.jwt.secret}") String secreto) {
        return NimbusJwtDecoder.withSecretKey(JwtService.claveDesdeSecreto(secreto))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}

package uv.isj.planificadoreventosbackend.security;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lee la cabecera {@code Authorization: Bearer <token>}, valida el token y
 * publica la identidad en el {@link SecurityContextHolder}.
 *
 * <p>Un token ausente NO es un error: la petición sigue su curso y es
 * {@link RestAuthenticationEntryPoint} quien responde 401 si la ruta lo exige.
 * Un token presente pero inválido (firma, expiración, formato) tampoco se
 * propaga como identidad: se descarta y la petición queda anónima, de modo que
 * la ruta protegida responde 401 en lugar de operar con un token roto.
 *
 * <p>Las peticiones OPTIONS (preflight CORS) nunca llevan credenciales, por lo
 * que se ignoran aquí para que el filtro CORS de la cadena de seguridad pueda
 * responderlas.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtDecoder jwtDecoder;

    public JwtAuthenticationFilter(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extraerToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            autenticar(token, request);
        }

        filterChain.doFilter(request, response);
    }

    private String extraerToken(HttpServletRequest request) {
        String cabecera = request.getHeader("Authorization");
        if (cabecera == null || !cabecera.startsWith(PREFIJO_BEARER)) {
            return null;
        }
        String token = cabecera.substring(PREFIJO_BEARER.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private void autenticar(String token, HttpServletRequest request) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            Integer idUsuario = Integer.valueOf(jwt.getSubject());
            UsernamePasswordAuthenticationToken autenticacion = new UsernamePasswordAuthenticationToken(
                    idUsuario,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_USUARIO")));
            autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(autenticacion);
        } catch (JwtException | IllegalArgumentException excepcion) {
            LOGGER.debug("Token JWT rechazado: {}", excepcion.getMessage());
            SecurityContextHolder.clearContext();
        }
    }
}

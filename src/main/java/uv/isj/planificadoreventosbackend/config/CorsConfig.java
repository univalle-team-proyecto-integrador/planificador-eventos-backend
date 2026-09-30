package uv.isj.planificadoreventosbackend.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS en dos capas, con una sola fuente de verdad.
 *
 * <p>Spring Security corre su propio {@code CorsFilter} antes que
 * DispatcherServlet. Si el preflight OPTIONS lo alcanza, el filtro JWT lo trata
 * como petición anónima y devuelve 401, y el navegador bloquea la llamada
 * entera. Por eso la misma lista de orígenes que se registra para el MVC se
 * publica además como {@link CorsConfigurationSource}, que SecurityConfig
 * inyecta en su cadena. Al haber dos capas con la misma definición, una
 * petición CORS nunca recibe cabeceras duplicadas.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfig(@Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOriginPatterns(List.of(allowedOrigins));
        configuracion.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("*"));
        configuracion.setAllowCredentials(true);
        configuracion.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        // Solo /api/**, igual que el registro del MVC: aplicar CORS a "/**"
        // haría que cualquier ruta devolviera cabeceras de origen permitido.
        fuente.registerCorsConfiguration("/api/**", configuracion);
        return fuente;
    }
}

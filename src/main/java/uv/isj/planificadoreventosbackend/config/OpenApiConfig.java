package uv.isj.planificadoreventosbackend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    /** Nombre referenciado con @SecurityRequirement(name = "bearerAuth"). */
    public static final String ESQUEMA_BEARER = "bearerAuth";

    @Bean
    public OpenAPI planificadorEventosOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Planificador de Eventos API")
                        .description("API REST para gestionar eventos, tipos de evento, subtareas y la carga diaria del organizador.")
                        .version("1.0.0"))
                .addServersItem(new Server()
                        .description("Servidor actual")
                        .url("/"))
                .components(new Components().addSecuritySchemes(ESQUEMA_BEARER,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token emitido por POST /api/users/login. "
                                        + "Envíalo como 'Authorization: Bearer <token>'.")));
    }
}

package uv.isj.planificadoreventosbackend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI planificadorEventosOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Planificador de Eventos API")
                        .description("API REST para gestionar eventos, tipos de evento, subtareas y la carga diaria del organizador.")
                        .version("1.0.0"))
                .addServersItem(new Server()
                        .description("Servidor actual")
                        .url("/"));
    }
}

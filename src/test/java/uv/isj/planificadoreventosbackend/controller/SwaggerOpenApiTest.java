package uv.isj.planificadoreventosbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SwaggerOpenApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @Test
    void publicaLaEspecificacionOpenApiConLosEndpointsDelBackend() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi").isString())
                .andExpect(jsonPath("$.info.title").value("Planificador de Eventos API"))
                .andExpect(jsonPath("$.info.version").value("1.0.0"))
                .andExpect(jsonPath("$.servers[0].url").value("/"))
                .andExpect(jsonPath("$.paths['/api/health']").exists())
                .andExpect(jsonPath("$.paths['/api/tipos-evento']").exists())
                .andExpect(jsonPath("$.paths['/api/eventos']").exists())
                .andExpect(jsonPath("$.paths['/api/eventos/{id}']").exists())
                .andExpect(jsonPath("$.paths['/api/eventos/{id}/subtareas']").exists())
                .andExpect(jsonPath("$.paths['/api/subtareas']").exists())
                .andExpect(jsonPath("$.paths['/api/subtareas/hoy']").exists())
                .andExpect(jsonPath("$.paths['/api/subtareas/{id}']").exists())
                .andExpect(jsonPath("$.paths['/api/subtareas/{id}/reprogramar']").exists())
                .andExpect(jsonPath("$.paths['/api/subtareas/{id}/estado']").exists())
                .andExpect(jsonPath("$.paths['/api/eventos/{id}'].get.responses['200'].content['application/json']").exists())
                .andExpect(jsonPath("$.paths['/api/eventos/{id}'].get.responses['200'].content['*/*']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/eventos'].post.requestBody.content['application/json']").exists())
                .andExpect(jsonPath("$.paths['/api/users/capacity']").exists())
                .andExpect(jsonPath("$.paths['/api/subtareas/{id}/reprogramar'].patch.responses['409']").exists())
                .andExpect(jsonPath("$.components.schemas.CapacidadDTO").exists())
                .andExpect(jsonPath("$.components.schemas.LimiteHorasDTO").exists())
                .andExpect(jsonPath("$.components.schemas.EventoDTO").exists())
                .andExpect(jsonPath("$.components.schemas.SubtareaDTO").exists())
                .andExpect(jsonPath("$.components.schemas.SubtareaActualizacionDTO").exists())
                .andExpect(jsonPath("$.components.schemas.TipoEventoDTO").exists())
                .andExpect(jsonPath("$.components.schemas.ReprogramarDTO").exists())
                .andExpect(jsonPath("$.components.schemas.EstadoSubtareaDTO").exists());
    }

    /**
     * Regresión: Swagger UI decide si adjunta la cabecera Authorization a partir
     * del campo {@code security} de cada operación. Una operación protegida por
     * la cadena de seguridad pero declarada sin {@code @SecurityRequirement}
     * aparece sin candado en la interfaz, el cliente no le manda el token y
     * recibe un 401 aunque su sesión sea válida.
     *
     * <p>La lista se construye recorriendo el spec: se comprueba que cada
     * operación protegida declare {@code bearerAuth} y, en sentido inverso,
     * que ninguna ruta pública lo declare. Así el fallo se ve al agregar un
     * endpoint, sin importar en qué archivo viva.
     */
    @Test
    void lasOperacionesProtegidasDeclaranElEsquemaBearer() throws Exception {
        JsonNode paths = spec().path("paths");

        Map<String, Boolean> protegidas = new LinkedHashMap<>();
        protegidas.put("/api/users/capacity#get", true);
        protegidas.put("/api/users/capacity#put", true);
        protegidas.put("/api/users/profile#get", true);
        protegidas.put("/api/subtareas/hoy#get", true);
        protegidas.put("/api/subtareas/hoy/agrupado#get", true);
        protegidas.put("/api/subtareas#get", true);
        protegidas.put("/api/subtareas/{id}#get", true);
        protegidas.put("/api/subtareas/{id}#put", true);
        protegidas.put("/api/subtareas/{id}#delete", true);
        protegidas.put("/api/subtareas/{id}/reprogramar#patch", true);
        protegidas.put("/api/subtareas/{id}/estado#patch", true);
        protegidas.put("/api/eventos#get", true);
        protegidas.put("/api/eventos#post", true);
        protegidas.put("/api/eventos/{id}#get", true);
        protegidas.put("/api/eventos/{id}#put", true);
        protegidas.put("/api/eventos/{id}#delete", true);
        protegidas.put("/api/eventos/{id}/subtareas#get", true);
        protegidas.put("/api/eventos/{id}/subtareas#post", true);

        for (Map.Entry<String, Boolean> operacion : protegidas.entrySet()) {
            String[] partes = operacion.getKey().split("#");
            JsonNode security = paths.path(partes[0]).path(partes[1]).path("security");

            assertThat(security.isArray())
                    .withFailMessage("%s %s debe declarar security bearerAuth para que Swagger UI "
                            + "adjunte la cabecera Authorization", partes[1].toUpperCase(), partes[0])
                    .isTrue();
            assertThat(security.toString())
                    .withFailMessage("%s %s no referencia el esquema bearerAuth",
                            partes[1].toUpperCase(), partes[0])
                    .contains("bearerAuth");
        }

        // Publicas: register y login son justamente por donde se obtiene
        // el token, y health es el healthCheckPath de Render.
        List<String> publicas = List.of(
                "/api/health#get",
                "/api/health/#get",
                "/api/tipos-evento#get",
                "/api/users/login#post",
                "/api/users/login/#post",
                "/api/users/register#post",
                "/api/users/register/#post");

        for (String operacion : publicas) {
            String[] partes = operacion.split("#");
            JsonNode security = paths.path(partes[0]).path(partes[1]).path("security");

            assertThat(security.isMissingNode() || security.isNull())
                    .withFailMessage("%s %s es pública y no debe declarar seguridad",
                            partes[1].toUpperCase(), partes[0])
                    .isTrue();
        }
    }

    private JsonNode spec() throws Exception {
        String documento = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(documento);
    }

    @Test
    void ningunaOperacionDocumentadaUsaElMediaTypeGenerico() throws Exception {
        String documento = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(documento).doesNotContain("\"*/*\"");
    }

    @Test
    void todasLasRespuestasQueDevuelvenCuerpoDeclaranApplicationJson() throws Exception {
        JsonNode documento = objectMapper.readTree(
                mockMvc.perform(get("/v3/api-docs"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());

        List<String[]> operaciones = new ArrayList<>();
        for (Map.Entry<String, JsonNode> ruta : documento.path("paths").properties()) {
            for (Map.Entry<String, JsonNode> operacion : ruta.getValue().properties()) {
                if (!operacion.getKey().startsWith("x-")) {
                    operaciones.add(new String[]{ruta.getKey(), operacion.getKey()});
                }
            }
        }

        assertThat(operaciones).isNotEmpty();

        List<String> sinJson = new ArrayList<>();
        int conCuerpo = 0;
        for (String[] operacion : operaciones) {
            JsonNode respuestas = documento
                    .path("paths")
                    .path(operacion[0])
                    .path(operacion[1])
                    .path("responses");
            for (JsonNode respuesta : respuestas.values()) {
                JsonNode contenido = respuesta.path("content");
                if (contenido.isMissingNode() || contenido.isEmpty()) {
                    continue;
                }
                conCuerpo++;
                if (!contenido.has("application/json")) {
                    sinJson.add(operacion[0] + " " + operacion[1] + " " + contenido.propertyNames());
                }
            }
        }

        assertThat(conCuerpo)
                .as("la especificación documenta respuestas con cuerpo")
                .isPositive();
        assertThat(sinJson)
                .as("toda respuesta con cuerpo declara application/json")
                .isEmpty();
    }

    @Test
    void documentaLosParametrosDeConsultaYLosCuerposDeCadaOperacion() throws Exception {
        JsonNode documento = objectMapper.readTree(
                mockMvc.perform(get("/v3/api-docs"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());

        JsonNode listarSubtareas = documento.path("paths").path("/api/subtareas").path("get");
        assertThat(listarSubtareas.path("parameters").isArray()).isTrue();
        assertThat(listarSubtareas.path("parameters").toString()).contains("eventoId");

        JsonNode gestionarHoy = documento.path("paths").path("/api/subtareas/hoy").path("get");
        assertThat(gestionarHoy.path("parameters").toString())
                .contains("usuarioId")
                .contains("fecha");

        assertThat(documento.path("paths").path("/api/eventos/{id}").path("get")
                .path("parameters").toString()).contains("id");
        assertThat(documento.path("paths").path("/api/subtareas/{id}/estado").path("patch")
                .path("requestBody").path("content").has("application/json")).isTrue();
        assertThat(documento.path("paths").path("/api/subtareas/{id}/reprogramar").path("patch")
                .path("requestBody").path("content").has("application/json")).isTrue();
    }

    @Test
    void documentaLosCodigosDeRespuestaEsperados() throws Exception {
        JsonNode documento = objectMapper.readTree(
                mockMvc.perform(get("/v3/api-docs"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());

        JsonNode respuestas = documento.path("paths").path("/api/health").path("get")
                .path("responses");
        assertThat(respuestas.has("200")).isTrue();
        assertThat(respuestas.has("503")).isTrue();

        assertThat(documento.path("paths").path("/api/eventos").path("post")
                .path("responses").has("201")).isTrue();
        assertThat(documento.path("paths").path("/api/eventos/{id}").path("get")
                .path("responses").has("404")).isTrue();
        assertThat(documento.path("paths").path("/api/subtareas/{id}").path("delete")
                .path("responses").has("204")).isTrue();
    }

    @Test
    void documentaLosEsquemasDeTodosLosModelosDeRespuesta() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.HealthResponse").exists())
                .andExpect(jsonPath("$.components.schemas.HealthResponse.properties.status")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.EventoDTO.required", hasItem("nombre")))
                .andExpect(jsonPath("$.components.schemas.SubtareaDTO.required")
                        .value(hasItem("fechaObjetivo")))
                .andExpect(jsonPath("$.components.schemas.ReprogramarDTO.required")
                        .value(hasItem("nuevaFecha")));
    }

    @Test
    void swaggerUiRedirigeALaInterfazConfigurada() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void swaggerUiMuestraLaInterfazHtml() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Swagger UI")));
    }
}

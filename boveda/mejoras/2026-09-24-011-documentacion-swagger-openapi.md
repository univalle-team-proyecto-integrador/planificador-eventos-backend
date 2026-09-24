---
tipo: mejora
---

# Documentación Swagger UI y OpenAPI

- **Fecha:** 2026-09-24
- **Área:** API / documentación
- **Estado:** hecha

## Descripción

Se conectó la documentación interactiva de la API al backend mediante springdoc. La página informativa `https://swagger.io/product/why-swagger/` no es el destino de integración: la interfaz que consume esta API es la que publica el propio backend.

La especificación usa un servidor relativo (`/`), por lo que Swagger UI puede ejecutar las peticiones desde el mismo host tanto en localhost como en Render. Se habilitó `Try it out` y se documentaron las operaciones y DTOs principales.

## Cambios

- `config/OpenApiConfig.java` — metadatos de OpenAPI: título, descripción, versión y servidor relativo.
- `src/main/resources/application.properties` — habilitación explícita de `/v3/api-docs` y `/swagger-ui.html`, ordenamiento, filtro, duración y `Try it out`.
- `controller/HealthController.java`, `EventoController.java`, `SubtareaController.java` — etiquetas, resumen de operaciones, parámetros y respuestas HTTP documentadas.
- `model/dto/EventoDTO.java`, `SubtareaDTO.java`, `ReprogramarDTO.java`, `EstadoSubtareaDTO.java` y `HealthResponse.java` — esquemas, ejemplos, campos obligatorios y restricciones.
- `src/test/.../SwaggerOpenApiTest.java` — verifica el JSON OpenAPI, sus rutas, sus esquemas, la redirección de Swagger UI y la interfaz HTML.
- `README.md` y `AGENTS.md` — URLs locales y públicas, explicación del uso de `Try it out` y aclaración sobre la URL informativa de Swagger.

## Verificación

- `./mvnw -Dtest=SwaggerOpenApiTest test` → **BUILD SUCCESS**, 3 tests.
- `./mvnw test` → **BUILD SUCCESS**, 21 tests, 0 fallos y 0 errores.
- `./mvnw package` → **BUILD SUCCESS**; jar ejecutable generado.
- El intento de arranque con el `.env` disponible no pudo conectarse porque contiene un host Supabase de ejemplo; no se copiaron ni modificaron secretos. La ruta real debe usar credenciales válidas.
- La URL pública de Render no permite verificar esta implementación: actualmente devuelve un servicio **Django REST Framework** distinto (`/api/health` responde `status: ok`) y `/v3/api-docs` y `/swagger-ui.html` responden 404. El servicio Spring de este repositorio debe crearse o corregirse en Render, configurar `DB_PASSWORD` y volver a desplegarse.
- El commit local contiene la implementación y la documentación; el push a GitHub no pudo ejecutarse porque el entorno no tiene credenciales HTTPS ni `gh`/SSH configurado.

## Decisiones

- No se añadió CORS para `https://swagger.io` porque Swagger UI se abre en el mismo origen que el backend y las solicitudes de `Try it out` son same-origin.
- `/v3/api-docs` es la fuente de verdad; `/swagger-ui.html` es la interfaz consumidora de esa especificación.
- El estado de despliegue externo se mantiene separado del estado de implementación: el código y las pruebas están listos, pero Render requiere que su servicio y credenciales estén configurados.

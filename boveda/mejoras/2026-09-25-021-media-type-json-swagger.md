---
tipo: mejora
---

# Media type JSON en Swagger UI

- **Fecha:** 2026-09-25
- **Área:** API / documentación
- **Estado:** hecha

## Descripción

La especificación OpenAPI describía las respuestas con el media type comodín `*/*`, lo que hacía que Swagger UI no mostrara de forma clara que la API responde JSON. Se fijó `application/json` como tipo de respuesta predeterminado, manteniendo `application/json` en los cuerpos de las operaciones de escritura.

## Cambios

- `src/main/resources/application.properties` — se agregó `springdoc.default-produces-media-type=application/json`.
- `src/test/java/uv/isj/planificadoreventosbackend/controller/SwaggerOpenApiTest.java` — se verifica que la respuesta y el cuerpo de solicitud se documenten como `application/json` y que `*/*` no aparezca en la respuesta de eventos.
- `test/documentacion-swagger.md` — se aclaró el media type mostrado por Swagger UI.
- `boveda/mejoras/README.md` y `boveda/lienzo-maestro.canvas` — se registró la mejora 021.

## Verificación

- `./mvnw -Dtest=SwaggerOpenApiTest test` → **BUILD SUCCESS**, 3 pruebas, 0 fallos.
- `./mvnw test` → **BUILD SUCCESS**, 70 pruebas, 0 fallos y 0 errores.
- `python3 -m json.tool boveda/lienzo-maestro.canvas` → JSON válido.

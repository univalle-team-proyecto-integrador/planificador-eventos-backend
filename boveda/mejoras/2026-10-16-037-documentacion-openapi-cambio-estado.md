---
tipo: mejora
---

# Documentación OpenAPI del cambio de estado de subtarea

- **Fecha:** 2026-10-16
- **Área:** documentación (OpenAPI/springdoc)
- **Estado:** hecha

## Descripción

`PATCH /api/subtareas/{id}/estado` estaba pobremente documentado: un bloque de
`@ApiResponses` sin `content`, un `@ApiResponse` de 401 **duplicado** y sin schema, y un
`@Operation` de una sola línea. Como springdoc deduce el esquema del tipo de retorno del
método, los 400/401/404 aparecían publicados como si devolvieran `SubtareaDTO`, que es una
forma que el backend nunca devuelve.

Se documentan los cuatro códigos (200/400/401/404) con `@Content(mediaType =
"application/json")` y schema explícito, se elimina el 401 duplicado y se introduce
`ProblemaDTO`, un sobre reutilizable en formato RFC 7807 (`type`, `title`, `status`,
`detail`, `errors`) para los errores de toda la API. Los conflictos de capacidad siguen
documentándose con `ProblemaCapacidadDTO`, que además del mensaje lleva el detalle del
excedente. No hay cambio funcional: solo cambia lo que promete el contrato publicado.

## Cambios

- `controller/SubtareaController.java` — `cambiarEstado`: `@Operation` con descripción
  completa (estados admitidos, nota opcional, reabrir a pendiente, 404 de ajena) y
  `@ApiResponse` de 200 (`SubtareaDTO`), 400/401/404 (`ProblemaDTO`); se borra el 401
  duplicado.
- `model/dto/ProblemaDTO.java` (**nuevo**) — schema RFC 7807 reutilizable para toda la
  API. Las propiedades van aplanadas en la raíz, no anidadas bajo `properties`, porque
  Spring serializa el `ProblemDetail` así (mismo criterio que ya fijó
  `ProblemaCapacidadDTO` en la mejora 032).
- `model/dto/EstadoSubtareaDTO.java` — `@Schema` enriquecidos: los tres valores del estado
  explicados uno a uno y `notaExplicativa` marcada como `nullable` con ejemplo.
- `model/dto/SubtareaDTO.java` — descripciones de `estado` y `notaExplicativa` alineadas
  con la DTO de entrada (valores admitidos y `nullable`).
- `controller/SwaggerOpenApiTest.java` — dos pruebas nuevas: una fija los cuatro códigos de
  `/api/subtareas/{id}/estado`, el `$ref` a `ProblemaDTO` en los errores y a `SubtareaDTO`
  en el 200; la otra verifica el sobre `ProblemaDTO` en components y el enum/ejemplos de
  `EstadoSubtareaDTO`.

## Verificación

- Sin cambio funcional: la verificación es la suite de pruebas, en particular las dos
  nuevas de `SwaggerOpenApiTest` que leen `/v3/api-docs` y fijan el contrato publicado.
- El cambio a la fecha de este registro sigue **sin commitear** (junto con el 409
  documentado de `EventoController.agregarSubtarea`, de la mejora 036).

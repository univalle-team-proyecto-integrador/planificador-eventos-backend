# Acta de validación end-to-end — Sprint 1

- **Fecha:** 2026-09-25
- **Fecha de cierre:** 2026-09-25
- **Área:** coordinación / QA
- **Estado:** completada (pendiente solo evidencia externa del tablero Jira)

## Objetivo

Cerrar la tarea de coordinación SCRUM-1-COORD-1: demostrar que el flujo del Sprint 1 funciona de punta a punta (**frontend Vercel → backend Render → Supabase**) y que cada historia tiene evidencia.

## Mapa historia → evidencia

| Historia Sprint 1 | Evidencia esperada | Estado | Dónde |
| --- | --- | --- | --- |
| Crear evento (FE+BE) | Postman 201 + `Location`; Chrome flujo crear; test MockMvc `creaConsultaYDetalleDeEvento` | ✅ HECHO (postman.md, frontend-chrome.md) / tests verdes | `test/postman.md`, `test/frontend-chrome.md` |
| Ver detalle de evento con subtareas | `GET /api/eventos/{id}` devuelve `subtareas`; prueba `./mvnw test` | ✅ HECHO — verificado en vivo tras redeploy `a48d302` (`GET /api/eventos/2` → `"subtareas":[{...}]`) | `test/postman.md`, `ApiSprint1Test` |
| Gestión de subtareas (crear/editar/eliminar/estado) | Postman 201/200/200/204 + PATCH estado; tests `actualizaLosCamposEditables`, `cambiaEstadoYPermiteReabrir` | ✅ HECHO / tests verdes | ídem |
| Catálogo de tipos de evento | Postman 200; `SwaggerOpenApiTest` | ✅ HECHO / tests verdes | ídem |
| Errores estandarizados y validación de horas | Postman casos negativos (0, -2, "abc", null → 400); tests `rechazaHorasCero/Negativas/Alfanumericas/NulasAlCrearSubtarea`, `traduceRecursosNoEncontradosYErroresDeValidacion` | ✅ HECHO — tests verdes y casos añadidos a la colección Postman (`test/postman/`) | `test/postman.md`, `ApiSprint1Test`, `CasosNegativosApiTest` |
| Manejo de errores de UI y modo offline | Toast de error de conexión, sin navegación, UI no bloqueada; no se crea nada estando offline | ✅ HECHO / PASSED (QA-303) | `test/casos-qa/qa-offline-manejo-error.md`, D-007 |
| Envío inválido a la API | `400` con `errors` por campo; sin registro en BD | ✅ HECHO / PASSED (QA-203) | `test/casos-qa/qa-203-validacion-envio-invalido.md` |
| Swagger como documentación de la API | `GET /v3/api-docs` 200 y `/swagger-ui.html` | ✅ HECHO | `test/documentacion-swagger.md`, mejora 017 |
| Verificar cadena desplegada | `bash scripts/verificar-despliegue.sh` | ✅ HECHO (mejora 015) | `test/validacion-despliegue.md` |
| Tablero Jira | Captura/export en `test/evidencia-jira-sprint1/` | ⏳ PENDIENTE (requiere acceso a Jira) | carpeta `test/evidencia-jira-sprint1/` |

## Resultados acumulados

- `./mvnw test` → **69 tests, 0 fallos** (incluye horas 0, negativas, alfanuméricas y nulas a nivel HTTP, casos negativos 404/400 estandarizados y unitarios de servicio).
- Frontend: **vitest 15 tests PASSED** (`npm run test`), `npm run lint` OK y `npm run build` OK.
- Cadena desplegada verificada en vivo: health UP, `/api/health` con BD conectada, detalle de evento con `subtareas` embebidas, catálogo de tipos, lista de eventos del usuario 1.
- Despliegues: backend `a48d302` (Render), frontend `37cce91` (Vercel).

## Casos QA registrados

- `test/casos-qa/qa-203-validacion-envio-invalido.md` — PASSED
- `test/casos-qa/qa-offline-manejo-error.md` — PASSED

## Cierre

Acta completada: el flujo end-to-end del Sprint 1 queda verificado con evidencia en el repositorio. Única acción externa pendiente: capturar el tablero Jira en `test/evidencia-jira-sprint1/` (SCRUM-1-COORD-2) y registrar el resultado en la mejora bóveda `2026-09-25-019-cierre-brechas-sprint-1.md`.
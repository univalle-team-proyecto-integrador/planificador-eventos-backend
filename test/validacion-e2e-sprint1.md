# Acta de validación end-to-end — Sprint 1

- **Fecha:** 2026-09-25
- **Área:** coordinación / QA
- **Estado:** en curso (pendiente re-ejecutar Postman y Chrome tras el redeploy con el detalle de evento que incluye subtareas)

## Objetivo

Cerrar la tarea de coordinación SCRUM-1-COORD-1: demostrar que el flujo del Sprint 1 funciona de punta a punta (**frontend Vercel → backend Render → Supabase**) y que cada historia tiene evidencia.

## Mapa historia → evidencia

| Historia Sprint 1 | Evidencia esperada | Estado | Dónde |
| --- | --- | --- | --- |
| Crear evento (FE+BE) | Postman 201 + `Location`; Chrome flujo crear; test MockMvc `creaConsultaYDetalleDeEvento` | ✅ HECHO (postman.md, frontend-chrome.md) / tests verdes | `test/postman.md`, `test/frontend-chrome.md` |
| Ver detalle de evento con subtareas | `GET /api/eventos/{id}` de la colección Postman devuelve `subtareas`; prueba `./mvnw test` | ⏳ PENDIENTE tras redeploy; tests ✓ | `test/postman.md`, `ApiSprint1Test` |
| Gestión de subtareas (crear/editar/eliminar/estado) | Postman 201/200/200/204 + PATCH estado; tests `actualizaLosCamposEditables`, `cambiaEstadoYPermiteReabrir` | ✅ HECHO / tests verdes | ídem |
| Catálogo de tipos de evento | Postman 200; `SwaggerOpenApiTest` | ✅ HECHO / tests verdes | ídem |
| Errores estandarizados y validación de horas | Postman casos negativos (0, -2, "abc", null → 400); tests `rechazaHorasCero/Negativas/Alfanumericas/NulasAlCrearSubtarea`, `traduceRecursosNoEncontradosYErroresDeValidacion` | ✅ HECHO en tests; ⏳ re-ejecutar en Postman | `test/postman.md` |
| Swagger como documentación de la API | `GET /v3/api-docs` 200 y `/swagger-ui.html` | ✅ HECHO | `test/documentacion-swagger.md`, mejora 017 |
| Verificar cadena desplegada | `bash scripts/verificar-despliegue.sh` | ✅ HECHO (mejora 015) | `test/validacion-despliegue.md` |
| Tablero Jira | Captura/export en `test/evidencia-jira-sprint1/` | ⏳ PENDIENTE (requiere acceso a Jira) | carpeta `test/evidencia-jira-sprint1/` |

## Resultados acumulados

- `./mvnw test` → 30 tests, 0 fallos (incluye SCRUM-8-QA-1: horas 0, negativas, alfanuméricas y nulas a nivel HTTP).
- `npm run lint` y `npm run build` frontend → OK.
- Cadena desplegada: health → datos Supabase → CORS → bundle frontend verificados (mejoras 014/015/017).

## Cierre

Completar esta acta cuando se re-ejecute la colección Postman contra `https://planificador-eventos-backend-1.onrender.com` tras el redeploy y se peguen las capturas de Jira; actualizar `Estado:` a **completada** y enlazar las capturas.
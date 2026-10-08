---
tipo: mejora
---

# Cierre de brechas Sprint 1: detalle de evento con subtareas y pruebas de horas

- **Fecha:** 2026-09-25
- **Área:** API / QA / coordinación
- **Estado:** hecha

## Descripción

Se cierran las brechas detectadas en el análisis de cumplimiento del Sprint 1: el detalle `GET /api/eventos/{id}` ahora incluye la lista de subtareas (SCRUM-8-BE-2), se añadieron pruebas HTTP para horas inválidas al crear subtareas (SCRUM-8-QA-1) y se preparó la evidencia de coordinación (tablero Jira y acta e2e, SCRUM-1-COORD-1/2).

## Cambios

- `src/main/java/uv/isj/planificadoreventosbackend/model/dto/EventoDTO.java` — nuevo miembro read-only `List<SubtareaDTO> subtareas`.
- `src/main/java/uv/isj/planificadoreventosbackend/service/EventoService.java` — inyecta `SubtareaRepository`; `obtenerPorId` puebla `subtareas`; `obtenerTodos` las expone vacías (`[]`) para evitar N+1.
- `src/test/java/uv/isj/planificadoreventosbackend/controller/ApiSprint1Test.java` — `creaConsultaYDetalleDeEvento` ahora crea una subtarea y valida `$.subtareas`; nuevos tests `rechazaHorasCeroAlCrearSubtarea`, `rechazaHorasNegativasAlCrearSubtarea`, `rechazaHorasAlfanumericasAlCrearSubtarea`, `rechazaHorasNulasAlCrearSubtarea` (400 con ProblemDetail).
- `test/validacion-e2e-sprint1.md` (nuevo) — acta end-to-end del Sprint 1 con mapa historia → evidencia.
- `test/evidencia-jira-sprint1/README.md` (nuevo) — carpeta y checklist para la evidencia del tablero Jira.
- `test/README.md` — mapa actualizado con los dos documentos nuevos.
- `boveda/mejoras/README.md` — fila 019.
- `boveda/lienzo-maestro.canvas` — nodo enlazado.

## Verificación

- `./mvnw test` → 30 tests, 0 fallos (antes 24). Incluye detalle con `$.subtareas` y los 4 casos negativos de horas.
- Pendiente tras redeploy en Render: re-ejecutar la colección Postman (casos negativos y `GET /api/eventos/{id}` con `subtareas`) y pegar la captura de Jira en `test/evidencia-jira-sprint1/`.
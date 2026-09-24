---
tipo: mejora
---
# Evidencias de validación: Swagger (API) + Postman (backend) + Chrome (frontend)

- **Fecha:** 2026-09-24
- **Área:** documentación / pruebas
- **Estado:** hecha

## Descripción

Por exigencia de la entrega se validan las tres piezas con herramientas concretas y evidencia capturable: la **documentación de la API** con Swagger, el **backend** con Postman y el **frontend** en el navegador Chrome. Se documentó el paso a paso, se aclaró el rol real de Swagger (no es un puente entre front y back) y se dejó la colección Postman lista para importar.

## Cambios

- `test/documentacion-swagger.md` (nuevo) — Swagger como documentación oficial de la API: URLs (`/swagger-ui.html`, `/v3/api-docs`), rol (mismo host, servidor relativo `/`, sin CORS), cómo documentar/probar con *Try it out*, tabla de endpoints y cuerpos de ejemplo.
- `test/postman.md` (nuevo) — pruebas del backend con Postman: importar la spec (opción Link) o la colección lista, entorno con variables, secuencia de 16 peticiones con códigos esperados y limpieza, evidencia a capturar.
- `test/postman/planificador-eventos-backend.postman_collection.json` (nuevo) — colección Postman v2.1 lista para **Import**, con 6 carpetas (Health, Tipos de evento, Eventos, Subtareas, Casos negativos, Documentación) y variables `{{baseUrl}}`, `{{usuarioId}}`, `{{idTipoEvento}}`, `{{idEvento}}`, `{{idSubtarea}}`.
- `test/postman/planificador-eventos-backend.postman_environment.json` (nuevo) — entorno con `baseUrl = https://planificador-eventos-backend-1.onrender.com` y `usuarioId = 1`.
- `test/frontend-chrome.md` (nuevo) — pruebas del frontend SPA en Chrome: F12 → Network, checklist de 12 flujos (carga, crear, detalle, editar, subtareas, reprogramar, estado, progreso, estados de UI) con sus llamadas `/api/**` esperadas, evidencia y notas (CORS, cold start, login placeholder).
- `test/conexion.md` — sección «El rol de Swagger (no es un puente)» con esquema front → `/api/**` directo vs Swagger mismo-origen.
- `test/terminos-clave.md` — entrada `Swagger/OpenAPI`.
- `test/README.md` — mapa actualizado con los tres documentos y la carpeta `test/postman/`.
- `boveda/mejoras/README.md` — fila 017.
- `boveda/lienzo-maestro.canvas` — nodo enlazado.

## Verificación

- Cuerpos de Postman generados a partir de los DTOs reales (`model/dto/EventoDTO`, `SubtareaDTO`, `SubtareaActualizacionDTO`, `ReprogramarDTO`, `EstadoSubtareaDTO`), leídos del código.
- Rutas del frontend confirmadas en `src/routes/AppRoutes.jsx` (`/hoy`, `/crear`, `/evento/:id`, `/progreso`, `/login` placeholder) y flujo real contra `api.js`.
- `python3 -m json.tool` → colección y entorno Postman **JSON válidos** (validación de sintaxis, ajena al stack Java/React).
- `git diff --check` → sin errores.

## Pendientes

- Ejecutar la evidencia con las herramientas (capturas de Swagger, Postman y Chrome) siguiendo los tres documentos; no requiere cambios de código.
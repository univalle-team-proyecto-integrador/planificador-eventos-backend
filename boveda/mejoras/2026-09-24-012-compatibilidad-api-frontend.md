---
tipo: mejora
---

# Compatibilidad de la API con el frontend

- **Fecha:** 2026-09-24
- **Área:** API / integración
- **Estado:** hecha

## Descripción

Se alineó la API del backend con las operaciones que necesita la interfaz para crear, consultar, editar, completar, reabrir y eliminar eventos y subtareas. Se tomó como referencia el contrato de `backend/main` y se añadieron únicamente los endpoints de código faltantes para el flujo actual del frontend.

## Cambios

- `controller/EventoController.java` — añadió `GET /api/eventos/{id}/subtareas` y `PUT /api/eventos/{id}`; el listado acepta el filtro opcional `usuarioId`.
- `controller/SubtareaController.java` — añadió la consulta de subtareas por evento, `GET /api/subtareas/{id}` y `DELETE /api/subtareas/{id}`.
- `service/EventoService.java` — incorporó la actualización transaccional de un evento existente.
- `service/SubtareaService.java` — añadió la consulta por evento y la eliminación sincronizada con la relación del evento; permite `pendiente` para reabrir subtareas.
- `model/dto/EstadoSubtareaDTO.java` y anotaciones OpenAPI — documentó los tres estados válidos.
- `src/test/.../ApiSprint1Test.java` — cubrió actualización de evento, consulta de subtareas por ambas rutas, reapertura y eliminación.
- `README.md` — actualizó la tabla de endpoints y la regla de estados.
- `../planificador-eventos-frontend/src/services/api.js` y `src/pages/ProgresoPage.jsx` — el cliente usa el contrato anidado de eventos y el endpoint PATCH de estado.
- `../planificador-eventos-frontend/ARCHITECTURE.md` — documenta el contrato HTTP vigente.

No se modificaron `render.yaml`, credenciales, variables de despliegue ni el DDL de Supabase.

## Verificación

- `./mvnw test` → BUILD SUCCESS.
- `./mvnw package` → BUILD SUCCESS.
- `npm run lint` → 0 warnings y 0 errors.
- `npm run build` → BUILD SUCCESS.
- `git diff --check` → sin errores de formato.

---
tipo: mejora
---

# API REST de eventos y subtareas

- **Fecha:** 2026-09-24
- **Área:** API / persistencia
- **Estado:** hecha

## Descripción

Se implementaron los controladores REST que conectan la capa de servicios con el contrato DTO utilizado por el frontend. La API permite crear, consultar, actualizar y eliminar eventos, así como consultar, crear, actualizar y eliminar subtareas de un evento.

## Cambios

- `controller/EventoController.java` — CRUD de eventos en `/api/eventos`, con filtro opcional por `usuarioId` y resolución de las relaciones `Usuario` y `TipoEvento`.
- `controller/SubtareaController.java` — CRUD de subtareas en `/api/subtareas`, incluyendo consulta por `eventoId` y estado inicial `pendiente`.
- `exception/ManejadorExcepcionesGlobal.java` — respuestas 404 para recursos inexistentes y 400 para validaciones o cuerpos JSON inválidos.
- `src/test/java/.../controller/ApiEventosControllerTest.java` — prueba de integración MockMvc del flujo evento → subtarea → actualización → eliminación y validación de payload inválido.

## Contrato

- `GET /api/eventos?usuarioId={id}`
- `GET /api/eventos/{id}`
- `POST /api/eventos`
- `PUT /api/eventos/{id}`
- `DELETE /api/eventos/{id}`
- `GET /api/subtareas?eventoId={id}`
- `GET /api/subtareas/{id}`
- `POST /api/subtareas`
- `PUT /api/subtareas/{id}`
- `DELETE /api/subtareas/{id}`

## Verificación

- `./mvnw test` — BUILD SUCCESS, 18 tests en verde.
- La prueba MockMvc cubre el payload que envía `planificador-eventos-frontend` y la respuesta que consume `EventDetailView`.

## Pendiente de despliegue

- Revisar y hacer commit de estos cambios en `backend/lead`.
- Fusionar mediante el flujo del coordinador hacia `main`.
- Configurar las variables de conexión de Render y ejecutar la prueba end-to-end desde el frontend desplegado.

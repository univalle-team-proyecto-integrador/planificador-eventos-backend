---
tipo: mejora
---

# Edición de subtareas y catálogo de tipos

- **Fecha:** 2026-09-24
- **Área:** API / persistencia / integración
- **Estado:** hecha

## Descripción

Se completaron las operaciones que el frontend necesitaba para editar una subtarea y para obtener el catálogo de tipos de evento sin depender de IDs hardcodeados. También se corrigió el cálculo de carga diaria para que considere únicamente las tareas del organizador que realiza la operación.

## Cambios

- `controller/TipoEventoController.java` y `service/TipoEventoService.java` — endpoint `GET /api/tipos-evento`.
- `model/dto/TipoEventoDTO.java` — contrato del catálogo de tipos.
- `model/dto/SubtareaActualizacionDTO.java` — DTO validado para editar nombre, fecha objetivo y horas.
- `controller/SubtareaController.java` y `service/SubtareaService.java` — endpoint `PUT /api/subtareas/{id}`.
- `repository/SubtareaRepository.java` — consultas de carga diaria y tareas pendientes filtradas por usuario.
- `src/test/.../ApiSprint1Test.java` y `SwaggerOpenApiTest.java` — pruebas de catálogo, edición y aislamiento de usuarios.
- `../planificador-eventos-frontend/src/services/api.js` — métodos de catálogo y actualización completa.
- `../planificador-eventos-frontend/src/views/CreateEventView.jsx` — carga dinámica del catálogo.
- `../planificador-eventos-frontend/src/views/EventDetailView.jsx` — edición de subtareas, nombre de tipo y respuesta obligatoria del servidor.
- `README.md`, `AGENTS.md` y `ARCHITECTURE.md` — contrato y estructura actualizados.

No se modificaron `render.yaml`, credenciales, variables de despliegue ni el DDL de Supabase.

## Verificación

- `./mvnw clean test` → BUILD SUCCESS.
- `./mvnw package` → BUILD SUCCESS.
- `npm run lint` → 0 warnings y 0 errors.
- `npm run build` → BUILD SUCCESS.
- `git diff --check` → sin errores de formato.

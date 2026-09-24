---
tipo: mejora
---

# API REST del Sprint 1: eventos y subtareas

- **Fecha:** 2026-09-23
- **Área:** API / persistencia / documentación
- **Estado:** hecha

## Descripción

Se completó la API REST de eventos y subtareas sobre la arquitectura en capas existente. La implementación se adaptó al modelo ya definido en el proyecto: identificadores `Integer`, `fechaObjetivo`, `horasEstimadas`, estados en minúsculas y Java 21. Se asignó la nota 010 para evitar una colisión de numeración con las mejoras 007–009 que ya existen en el worktree de `backend/lead`.

La API valida los datos en DTO y servicio, resuelve las relaciones de usuario, tipo de evento y evento, traduce errores a respuestas `ProblemDetail`, comprueba el límite diario antes de reprogramar y elimina las subtareas junto con su evento.

## Cambios

- `model/Evento.java` — relación `OneToMany` con cascada y `orphanRemoval`, más el método que mantiene sincronizada la subtarea con su evento.
- `model/dto/ReprogramarDTO.java` — DTO validado con `nuevaFecha` y `nuevasHoras` enteras, acorde con `horas_estimadas` del esquema actual.
- `model/dto/EstadoSubtareaDTO.java` — DTO validado para estado y nota explicativa.
- `model/dto/SubtareaDTO.java` — `idEvento` deja de exigirse en el cuerpo porque la relación se toma del endpoint anidado.
- `repository/EventoRepository.java` — repositorio JPA, búsquedas por usuario y `EntityGraph` para las relaciones requeridas al mapear DTOs.
- `repository/SubtareaRepository.java` — búsqueda por evento, consulta JPQL de la vista de una fecha y suma de horas no ejecutadas.
- `repository/TipoEventoRepository.java` y `repository/UsuarioRepository.java` — repositorios JPA necesarios para resolver las FKs al crear un evento.
- `exception/RecursoNoEncontradoException.java` — excepción de dominio para recursos inexistentes.
- `exception/GlobalExceptionHandler.java` — respuestas 404, 400 y errores de Bean Validation en formato `ProblemDetail`.
- `service/EventoService.java` — listado, detalle, creación validada y eliminación transaccional en cascada.
- `service/SubtareaService.java` — alta con estado `pendiente`, reprogramación con cálculo de carga diaria, cambio de estado y resolución del límite desde el organizador.
- `controller/EventoController.java` — endpoints de lista, detalle, creación, alta anidada de subtarea y eliminación.
- `controller/SubtareaController.java` — endpoints PATCH para reprogramación y cambio de estado.
- `src/test/.../ApiSprint1Test.java` — 11 pruebas de integración para repositorios, validaciones, servicios, cascada, conflictos y códigos HTTP.
- `README.md` y `AGENTS.md` — estructura, endpoints y estado actualizados.

## Reglas de negocio

- Una subtarea nueva siempre inicia en `pendiente`, aunque el cliente intente enviar otro estado.
- La reprogramación suma las horas no ejecutadas de la fecha destino y compara el resultado con el límite diario del organizador.
- Si la subtarea ya estaba en la fecha destino, su asignación anterior se resta para no contarla dos veces.
- Un conflicto devuelve `conflicto: true`, `limiteDiario`, `horasTotalesCalculadas` y un mensaje explicativo, sin persistir cambios.
- El endpoint de estado solo admite `ejecutada` o `pospuesta`.
- El esquema no cambió: el DDL ya contenía `ON DELETE CASCADE` para `subtarea.id_evento`.

## Verificación

- `./mvnw test` → **BUILD SUCCESS**, 18 tests, 0 fallos y 0 errores.
- `./mvnw -Dtest=ApiSprint1Test test` → **BUILD SUCCESS**, 11 tests de la nueva API.
- `./mvnw package` → **BUILD SUCCESS**; jar ejecutable generado en `target/planificador-eventos-backend-0.0.1-SNAPSHOT.jar`.
- `git diff --check` → sin errores de formato.

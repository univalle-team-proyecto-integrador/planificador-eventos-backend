---
tipo: mejora
---

# Quita las horas estimadas del evento (siguen en las gestiones)

- **Fecha:** 2026-10-14
- **Área:** API / persistencia
- **Estado:** hecha

## Descripción

Revierte la mejora `2026-10-08-031-horas-estimadas-del-evento.md`: el evento deja de
declarar `horasEstimadas`. El campo había resultado ser redundante —el dato de esfuerzo
vive en las gestiones (subtareas), que es de donde sale el total real que muestra la
pantalla de progreso— y su mantenimiento costaba más de lo que aportaba: constructor
del DTO, validación, mapeo y una veintena de call sites de prueba.

La columna `evento.horas_estimadas` se retiró directamente en la base de Supabase; con
`ddl-auto=validate`, el mapeo tenía que dejar de declararla o el arranque fallaba.

## Cambios

- `model/Evento.java` — se elimina el campo `horasEstimadas`.
- `model/dto/EventoDTO.java` — se elimina el campo (y su `@NotNull @Min(1)`).
- `service/EventoService.java` — se eliminan el guardado en `crearEvento`/`actualizarEvento`,
  la validación en `validar` y el mapeo.
- Tests ajustados al contrato nuevo: `ApiSprint1Test`, `CasosNegativosApiTest`,
  `JwtSecurityIntegrationTest`, `ModeloEntidadesTest` y `EventoServiceTest` (constructores
  del DTO y casos de horas propias). Las pruebas de subtarea quedan intactas: las horas
  estimadas siguen siendo un dato de la gestión.

## Verificación

- Commit `905acd1` (2026-10-14, `refactor: quita horas estimadas del evento (siguen en
  las gestiones)`); la suite de evento se ajusta en el mismo commit y las pruebas de
  subtarea quedan intactas.
- Esta mejora deja **obsoleta** la `2026-10-08-031-horas-estimadas-del-evento.md`, cuya
  feature ya no existe.

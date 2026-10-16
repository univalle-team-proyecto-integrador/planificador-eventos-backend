---
tipo: mejora
---

# Límite diario también al crear una subtarea

- **Fecha:** 2026-10-14
- **Área:** API, reglas de negocio
- **Estado:** hecha

## Descripción

Hasta ahora el límite diario solo se evaluaba al **reprogramar** (mejora 030) y al
**editar** una subtarea (mejora `2026-10-09-033-editar-subtarea-evalua-el-limite.md`).
`POST /api/eventos/{id}/subtareas` aceptaba sobrecarga en silencio: se podía crear una
gestión que dejara el día muy por encima del límite, y el 409 aparecía recién cuando esa
misma gestión se movía o se editaba. El hueco estaba diagnosticado como pendiente en la
mejora 031.

`agregarSubtarea` ahora aplica la misma regla que los otros dos caminos: suma las horas no
ejecutadas del usuario en la fecha objetivo y, si con las horas solicitadas se pasa del
límite, no crea nada y responde **409**.

## Cambios

- `service/SubtareaService.java` — `agregarSubtarea` valida el límite diario con la misma
  `horasNoEjecutadasEn` que comparten reprogramar y editar (sin subtarea propia que
  restar, porque la que se crea todavía no existe). Si se excede, lanza
  `CapacidadExcedidaException` con `idSubtarea=null`: no hay id porque la gestión no llegó
  a guardarse. El 409 sale por el manejador ya existente, con las cifras del conflicto en
  la raíz del `ProblemDetail`.
- `controller/EventoController.java` — `agregarSubtarea` documenta el 409 en OpenAPI con
  `ProblemaCapacidadDTO` y `application/json`, igual que los otros endpoints de capacidad.
- `controller/CapacidadApiTest.java` — caso nuevo `crearSubtareaQueSuperaLimiteDevuelve409`:
  verifica los campos planos del conflicto, que `idSubtarea` no aparece en el cuerpo y que
  la subtarea no se crea (el recuento en base sigue en 1).

## Verificación

- Commit `33483fa` (2026-10-14, `feat: aplicar límite diario al crear subtarea`) — el
  propio commit reporta la suite en verde (178 pruebas). La documentación OpenAPI del
  controlador quedó como cambio local pendiente de commit junto con la mejora 037.

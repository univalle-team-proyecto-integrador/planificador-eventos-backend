---
tipo: mejora
---

# Editar una subtarea evalúa el límite diario (US-08)

- **Fecha:** 2026-10-09
- **Área:** API, reglas de negocio
- **Estado:** hecha

## Descripción

`PUT /api/subtareas/{id}` guardaba las horas a ciegas: se podía bajar a 1 h o
subir a 50 h y nadie miraba el límite diario. Eso dejaba sin cubrir el segundo
escenario de US-08, que es justamente *reducir horas y recalcular el
conflicto*. El propio contrato lo admitía como limitación conocida.

La regla nueva es **"solo si empeora"**: el 409 se lanza cuando el día queda por
encima del límite **y** peor que antes de la edición.

- **Reducir horas nunca da 409**, aunque el día siga pasándose. El guardado
  ocurre y la respuesta trae `resuelto: false` para que el frontend diga que el
  conflicto persiste. Así "reducir horas" siempre deja avanzar, que es lo que
  pide el escenario, en vez de dejar al usuario atrapado en un conflicto que no
  puede cerrar reduciendo.
- **Renombrar no toca el límite.** Un cambio de nombre con la misma fecha y las
  mismas horas ni entra al cálculo: editar el nombre de una gestión en un día ya
  sobrecargado no puede empezar a dar 409 donde antes guardaba.

## Cambios

- `service/SubtareaService.java` — `actualizarSubtarea` evalúa el límite y lanza
  `CapacidadExcedidaException` solo si la edición empeora el día. El 409 sale por
  el mismo manejador que reprogramar, así que las cifras del conflicto son las
  mismas y el frontend no necesita una rama nueva.
- `service/SubtareaService.java` — `horasNoEjecutadasEn(usuario, fecha, propia)`
  extrae la cuenta que ya usaba `reprogramar`: suma de no ejecutadas, restando
  la propia subtarea si sigue en esa fecha para no contarla dos veces. Los dos
  caminos comparten la regla en vez de duplicarla.
- `model/dto/SubtareaDTO.java` — campo `resuelto` (el `resolved` de la spec) con
  un constructor sin él, para que lecturas y altas no cambien el contrato de
  quien ya consume la DTO.

## Verificación

- `./mvnw test -Dlombok.version=1.18.48` → **182 pruebas, 0 fallos** (178 antes,
  +4).
- Las 4 pruebas nuevas cubren: reducir y resolver, reducir sin alcanzar (guarda y
  `resuelto: false`), aumentar que empeora el día (409 y no guarda), y renombrar
  en un día sobrecargado (200).

## Notas

- El campo `resuelto` solo viene en las respuestas de edición. Es `null` en
  lecturas y altas, que es lo que permite el constructor delegante.
- Coincide con el criterio de QA de US-08: *probar reducir las horas pero
  dejando un valor que aún exceda el límite, para confirmar que el aviso
  persiste*. Ese caso guarda y avisa; no bloquea.

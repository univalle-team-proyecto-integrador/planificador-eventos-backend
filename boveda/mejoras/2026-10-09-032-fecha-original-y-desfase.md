---
tipo: mejora
---

# Línea base de la fecha para derivar postergada / adelantada

- **Fecha:** 2026-10-09
- **Área:** persistencia, API
- **Estado:** hecha

## Descripción

Para marcar una gestión reprogramada como *postergada* o *adelantada* hace
falta saber contra qué fecha comparar. `SubtareaService.reprogramar` hacía
`setFechaObjetivo(nuevaFecha)` y con ello **pisaba la fecha anterior sin dejar
rastro**: no había ningún campo donde recuperarla ni forma de derivar el desfase.

Se agrega `subtarea.fecha_objetivo_original`, que guarda la fecha con la que la
gestión se planificó. El desfase lo deriva el frontend comparando ambas fechas,
en vez de guardar un estado más en la base que pueda desincronizarse.

La línea base se fija la **primera vez que la fecha cambia** y **no se
recalcula** después: reprogramar tres veces sigue midiendo contra la
planificación original, no contra la última movida. Se fija tanto al reprogramar
como al editar con `PUT /api/subtareas/{id}`, que perdía la fecha igual.

## Cambios

- `db/ddl-supabase.sql` — columna `fecha_objetivo_original DATE` en el
  `CREATE TABLE` y migración `ALTER TABLE` comentada. **Ejecutar antes de
  desplegar**: con `ddl-auto=validate` la app no arranca si la entidad declara
  una columna que la base no tiene.
- `model/Subtarea.java` — campo `fechaObjetivoOriginal`, nullable sin
  `@NotNull`: las subtareas previas no tienen línea base recuperable y no se
  inventa ninguna.
- `model/dto/SubtareaDTO.java` — el campo se expone con
  `accessMode = READ_ONLY`; sin él el frontend nunca lo recibe.
- `service/SubtareaService.java` — `fijarLineaBaseSiFalta(...)`, llamado desde
  `reprogramar` y `actualizarSubtarea`. No actúa si la fecha no cambia, para no
  marcar como reprogramada una gestión a la que solo se le ajustaron las horas.
- `service/EventoService.java` — `aSubtareaDto` mapea el campo nuevo.
- `model/dto/ProblemaCapacidadDTO.java` — **nuevo**. El `@ApiResponse` del 409 no
  declaraba `content`, así que springdoc deducía `SubtareaDTO` (el tipo de
  retorno del método) y documentaba una forma que el backend nunca devuelve.
  Este schema fija la real: las propiedades del `ProblemDetail` van **aplanadas
  en la raíz**, no anidadas bajo `properties`.
- `controller/SubtareaController.java` — el 409 referencia `ProblemaCapacidadDTO`.
  Se declara `application/json` y no `application/problem+json` porque el repo
  tiene una regla (y un test) de que toda respuesta con cuerpo va como JSON.
- `src/test/.../CapacidadApiTest.java` — 4 pruebas nuevas: guarda la línea base,
  no la recalcula al reprogramar otra vez, no marca cuando solo cambian las
  horas, y `PUT` también la fija.

## Verificación

- `./mvnw test -Dlombok.version=1.18.48` → **178 pruebas, 0 fallos** (174 antes,
  +4 nuevas).
- `./mvnw package` → BUILD SUCCESS.
- End-to-end contra el backend real con H2 (no contra el mock):
  - al crear, `fechaObjetivoOriginal` viene `null`;
  - adelantar 10 → 08 deja `orig=2026-11-10`;
  - reprogramar de nuevo a 11-25 deja `orig=2026-11-10` (no se recalcula);
  - misma fecha con otras horas no marca;
  - `PUT /api/subtareas/{id}` de 11-05 a 11-12 deja `orig=2026-11-05`;
  - el 409 llega con las 11 claves **en la raíz**, `properties` ausente;
  - el 409 no persiste el cambio;
  - `/v3/api-docs` ya declara `ProblemaCapacidadDTO` en el 409.

## Notas

- **Lombok no compila con el JDK 27 local.** Hay que pasar
  `-Dlombok.version=1.18.48`; la versión que fija Spring Boot (1.18.32) falla
  con `ExceptionInInitializerError`. En Docker y Render el build usa
  `maven:3.9-eclipse-temurin-21`, así que no les afecta.
- Las subtareas que ya existían en Supabase quedan sin línea base hasta su
  próximo cambio de fecha: la fecha anterior no se puede recuperar.

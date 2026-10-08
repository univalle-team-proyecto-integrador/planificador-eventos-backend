---
tipo: mejora
---

# Capacidad diaria del organizador y conflicto 409 al reprogramar

- **Fecha:** 2026-10-08
- **Área:** API
- **Estado:** hecha

## Descripción

Se expuso la capacidad de trabajo diaria del organizador (`GET`/`PUT /api/users/capacity`)
y se corrigió el contrato de la reprogramación. Antes, `PATCH /api/subtareas/{id}/reprogramar`
devolvía siempre 200 con una bandera `conflicto` dentro del body: el cliente tenía que leer
esa bandera para saber si la reprogramación se había guardado. Ahora el código HTTP lo
expresa: **200** cuando cabe en el límite y sí guarda, **409** cuando se excede y no guarda
nada.

## Cambios

- `src/main/java/uv/isj/planificadoreventosbackend/controller/UsuarioController.java` — se agregaron `GET` y `PUT /capacity` al controlador que ya servía `register`, `login` y `profile`, con `@SecurityRequirement(name = "bearerAuth")`.
- `src/main/java/uv/isj/planificadoreventosbackend/service/UsuarioService.java` (nuevo) — consulta y ajuste del límite; expone `LIMITE_MINIMO_HORAS` y `LIMITE_MAXIMO_HORAS` para que `SubtareaService` comparta el mismo rango.
- `src/main/java/uv/isj/planificadoreventosbackend/model/dto/CapacidadDTO.java` (nuevo) — límite, fecha evaluada, horas comprometidas y disponibles.
- `src/main/java/uv/isj/planificadoreventosbackend/model/dto/LimiteHorasDTO.java` (nuevo) — `@Min(1)`/`@Max(16)` con mensajes en español.
- `src/main/java/uv/isj/planificadoreventosbackend/exception/CapacidadExcedidaException.java` (nuevo) — lleva los números del conflicto para el 409.
- `src/main/java/uv/isj/planificadoreventosbackend/exception/GlobalExceptionHandler.java` — manejador de `CapacidadExcedidaException` que devuelve 409 con `limiteDiario`, `horasAsignadasPreviamente`, `horasSolicitadas`, `horasPlanificadasTotales`, `excedente`, `fecha` e `idSubtarea`.
- `src/main/java/uv/isj/planificadoreventosbackend/config/SecurityConfig.java` — `/api/users/capacity` quedó en `.authenticated()`. Sin esto caería en el `anyRequest().permitAll()` final y la ruta habría quedado abierta.
- `src/main/java/uv/isj/planificadoreventosbackend/service/SubtareaService.java` — `reprogramar(usuarioId, id, dto)` resuelve el límite desde el usuario del evento y lanza `CapacidadExcedidaException`; se eliminó `obtenerLimiteDiario` y la aritmética pasó de `double` a `int`.
- `src/main/java/uv/isj/planificadoreventosbackend/controller/SubtareaController.java` — devuelve la `SubtareaDTO` directa y documenta el 409. Además se limpiaron el import duplicado de `HoyResponseDTO`, dos bloques de javadoc huérfanos y la indentación de dos `@GetMapping`.
- `src/test/java/uv/isj/planificadoreventosbackend/controller/CapacidadApiTest.java` (nuevo) — 10 pruebas de extremo a extremo con token JWT.
- `src/test/java/uv/isj/planificadoreventosbackend/service/UsuarioServiceTest.java` (nuevo) — 10 pruebas unitarias del servicio.
- `src/test/java/uv/isj/planificadoreventosbackend/controller/ApiSprint1Test.java` y `service/SubtareaServiceTest.java` — se migraron los 10 call sites de `reprogramar` al contrato nuevo.
- `src/test/java/uv/isj/planificadoreventosbackend/controller/SwaggerOpenApiTest.java` — se verifican la ruta nueva, los esquemas y el 409 documentado.
- `README.md` — contrato HTTP, sección de límite diario y hoja de ruta.

## Decisiones

- **Rutas en inglés** (`/api/users/capacity`) porque ya existen `/api/users/register`,
  `login` y `profile`. **DTOs en español** (`CapacidadDTO`, `limiteHorasDiarias`) para
  quedar coherentes con `UsuarioDTO`, `SubtareaDTO` y la entidad `Usuario`.
- **El propietario sale siempre del token.** No hay `usuarioId` en el query: cambiarlo
  no da los datos de otra cuenta, el mismo criterio de aislamiento que ya aplican
  `/api/eventos` y `/api/subtareas`.
- **Capacidad exige token siempre**, igual que `/profile`, y **no** se ató a
  `PROTECT_SUBTAREAS`: esa bandera existe para el despliegue gradual de subtareas, y
  colgar la capacidad de ella la bloquearía justo cuando el cliente ya envíe el token.
- **No hay campo `forzar`**: reprogramar por encima del límite no está permitido; el
  usuario ajusta fecha u horas.
- **El `excedente` dice cuántas horas hay que liberar**, no solo que hubo conflicto.
- **200 devuelve la `SubtareaDTO` plana**: con el 409, el sobre `{conflicto, subtarea}`
  quedaba redundante.
- **El cálculo de horas ya existía**:
  `sumarHorasNoEjecutadasPorFechaYUsuario` ya suma por fecha y ya excluye `ejecutada`.
  Se reutilizó y se出卖aron pruebas de que se invoca con `EstadoSubtarea.ejecutada`.
- No hizo falta tocar `db/ddl-supabase.sql`: el
  `CHECK (limite_horas_diarias BETWEEN 1 AND 16)` ya estaba.

## Verificación

- `./mvnw test` ✅ — **168 pruebas en verde** (148 antes de esta mejora).
- Se cubren: horas planificadas que excluyen `ejecutada`, horas disponibles que no bajan de
  cero, aislamiento por propietario en capacidad y reprogramación, 401 sin token, 400 con
  los tres mensajes de rango, 409 con `excedente`, y 200 con persistencia comprobada
  contra el repositorio.

### Nota de entorno

La máquina local tiene JDK 27 y el Lombok gestionado por Spring Boot 4.1.1 (1.18.46) no
compila ahí. Se verificó con `./mvnw -Dlombok.version=1.18.48`. El `pom.xml` no se modificó:
en Docker y Render se compila con JDK 21.

## Pendiente

- **Frontend:** `GET`/`PUT /api/users/capacity` todavía no tienen consumidor en
  `planificador-eventos-frontend`. El cliente ya adjunta `Authorization: Bearer`, así que
  solo falta el método en `src/services/api.js` y la pantalla que lo consuma.
- **Reprogramar una subtarea `ejecutada`** no la reabre a `pendiente`, así que sigue sin
  contar para el límite. Si debería reabrirse, es un cambio aparte.
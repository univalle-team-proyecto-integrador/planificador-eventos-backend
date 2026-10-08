---
tipo: mejora
---

# Capacidad diaria del organizador y conflicto 409 en la reprogramación

- **Fecha:** 2026-10-08
- **Área:** API
- **Estado:** hecha

## Descripción

Se expuso el límite de horas diarias del organizador como recurso editable y se corrigió el
contrato de la reprogramación de subtareas. Antes, `PATCH /api/subtareas/{id}/reprogramar`
devolvía siempre 200 con una bandera `conflicto` dentro del body: el cliente tenía que leer
esa bandera para saber si la reprogramación se había guardado o no. Ahora el código HTTP
expresa el resultado: **200** cuando cabe en el límite y se guarda, **409** cuando se excede y
no se guarda nada.

## Cambios

- `src/main/java/uv/isj/planificadoreventosbackend/controller/UsuarioController.java` (nuevo) — `GET` y `PUT /api/usuarios/capacidad`, con el organizador en `?usuarioId=` igual que `/api/eventos` y `/api/subtareas/hoy`.
- `src/main/java/uv/isj/planificadoreventosbackend/service/UsuarioService.java` (nuevo) — consulta y ajuste del límite; expone `LIMITE_MINIMO_HORAS` y `LIMITE_MAXIMO_HORAS` para que `SubtareaService` use la misma fuente.
- `src/main/java/uv/isj/planificadoreventosbackend/model/dto/CapacidadDTO.java` (nuevo) — límite, fecha evaluada, horas comprometidas y horas disponibles.
- `src/main/java/uv/isj/planificadoreventosbackend/model/dto/LimiteHorasDTO.java` (nuevo) — `limiteHorasDiarias` con `@Min(1)`/`@Max(16)` y mensajes en español.
- `src/main/java/uv/isj/planificadoreventosbackend/exception/CapacidadExcedidaException.java` (nuevo) — lleva los números del conflicto para el 409.
- `src/main/java/uv/isj/planificadoreventosbackend/exception/GlobalExceptionHandler.java` — manejador de `CapacidadExcedidaException` que devuelve 409 con `limiteDiario`, `horasAsignadasPreviamente`, `horasSolicitadas`, `horasPlanificadasTotales`, `excedente`, `fecha` e `idSubtarea`.
- `src/main/java/uv/isj/planificadoreventosbackend/service/SubtareaService.java` — `reprogramar(id, dto)` resuelve el límite internamente y lanza `CapacidadExcedidaException`; se eliminó `obtenerLimiteDiario` y la aritmética pasó de `double` a `int`.
- `src/main/java/uv/isj/planificadoreventosbackend/controller/SubtareaController.java` — devuelve la `SubtareaDTO` directa y documenta el 409.
- `src/test/java/uv/isj/planificadoreventosbackend/controller/ApiSprint1Test.java` — se actualizaron los 4 tests de reprogramación y se agregaron 7 de capacidad y conflicto.
- `src/test/java/uv/isj/planificadoreventosbackend/controller/SwaggerOpenApiTest.java` — se verifican el endpoint nuevo, los esquemas `CapacidadDTO`/`LimiteHorasDTO` y el 409 documentado.
- `src/test/java/uv/isj/planificadoreventosbackend/service/UsuarioServiceTest.java` (nuevo) — 10 pruebas unitarias del servicio de capacidad, en el estilo de Mockito que usa `main`.
- `src/test/java/uv/isj/planificadoreventosbackend/service/SubtareaServiceTest.java` — se migraron los 13 call sites de `reprogramar` al contrato nuevo; se conservó la intención de cada prueba.
- `README.md` — contrato HTTP, sección de límite diario y hoja de ruta.

## Fusión con `main`

El trabajo se desarrolló sobre `c97fcf8`, pero `main` había avanzado 7 commits
(`a48d302` "cierra brechas del sprint 1"). Antes de fusionar se hizo commit propio y luego
`git merge origin/main`:

- **Conflicto en `boveda/`**: `main` renombró la mejora 014 a 020 y renumeró la serie hasta el 020, así que esta nota pasó de **015 a 021** (archivo, índice y nodos `m021`/`p021` del canvas).
- **`ApiSprint1Test.java`** se fusionó solo; `SubtareaServiceTest`, `EventoServiceTest`, `TipoEventoServiceTest` y `CasosNegativosApiTest` entraron intactos.
- **13 pruebas de `SubtareaServiceTest`** que assertaban `resultado.get("conflicto")` con la firma `reprogramar(id, dto, limite)` se migraron a `reprogramar(id, dto)` + `CapacidadExcedidaException`. Ninguna se eliminó: se sigue verificando el conflicto, que no se guarde y que el mismo día no se cuente dos veces.
- Se conserva el `prepareThreshold=0` que `main` agrego para el pooler de Supabase.

## Decisiones

- **Ruta en español**: `/api/usuarios/capacidad`, sin barra final (Spring Boot 4 no hace match de barra final). El pedido inicial decía `/api/users/capacity/` y `PATCH /api/subtasks/<id>/`, que rompía la convención de `/api/subtareas` y `/api/tipos-evento`.
- **Identificador por query param**: `?usuarioId=` replica el patrón existente en lugar de un path param.
- **200 con la `SubtareaDTO` plana**: con el 409, el sobre `{conflicto, subtarea}` era redundante. El frontend no consumía este endpoint todavía, así que no hubo compatibilidad que preservar.
- **Sin `forzar`**: reprogramar por encima del límite no está permitido; el usuario ajusta fecha u horas. Queda pendiente si el negocio lo necesita.
- **`excedente` en el 409**: dice cuántas horas hay que liberar, no solo que hubo conflicto.
- **Validación en dos capas**: Bean Validation en el DTO (mensaje en español por campo, en `errors.limiteHorasDiarias`) y `IllegalArgumentException` en el service, igual que ya hacía `SubtareaService` con `validarActualizacion`.
- **No hizo falta tocar `db/ddl-supabase.sql`**: el `CHECK (limite_horas_diarias BETWEEN 1 AND 16)` ya estaba.
- **Firmas en enteros**: `horasEstimadas` y `limiteHorasDiarias` son `Integer`; el `double` anterior no aportaba precisión.
- `UsuarioDTO` sigue sin uso: tiene `@Min(1) @Max(16)` pero no lo expone ningún endpoint.

## Verificación

- `./mvnw test` ✅ — **88 pruebas exitosas** sobre el árbol ya fusionado con `main` (27 antes de esta mejora; 34 antes de la fusión, +10 de `UsuarioServiceTest`, +24 de las clases que trajo `main` y +20 de las migradas).
- Se cubren: horas planificadas que excluyen `ejecutada`, horas disponibles que no bajan de cero, 404 de organizador inexistente, 400 con los tres mensajes de rango, 409 con `excedente`, y 200 con persistencia comprobada contra el repositorio.

### Nota de entorno

La máquina local solo tiene JDK 27 y el Lombok que gestiona Spring Boot 4.1.1 (1.18.46) no
compila ahí (`java.lang.ExceptionInInitializerError: com.sun.tools.javac.tree.EndPosTable`).
Se verificó con `./mvnw -Dlombok.version=1.18.48 test`. **El `pom.xml` no se modificó**:
es un problema del entorno local, no del proyecto, que compila con JDK 21 en Docker y Render.
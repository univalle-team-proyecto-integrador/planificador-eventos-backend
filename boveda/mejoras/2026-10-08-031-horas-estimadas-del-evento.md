---
tipo: mejora
---

# Horas estimadas en el evento

- **Fecha:** 2026-10-08
- **Área:** API / persistencia
- **Estado:** hecha

## Descripción

El evento pasa a declarar sus horas estimadas al crearse, igual que ya hacía cada subtarea. Antes
el único dato de esfuerzo era el de las subtareas, así que no había forma de registrar una
estimación del evento completo hasta después de desglosarlo en tareas.

El campo es **informativo**: no es un tope, no genera 409, y no reemplaza al total real que
suman las subtareas (que es lo que usan `WorkloadSummary` y la pantalla de progreso). Esa
separación deliberada deja libres los modales de Conflicto y Reducir horas de la lista de
diseño, que son del límite diario y ya funcionan.

## Cambios

- `db/ddl-supabase.sql` — la columna entra en el `CREATE TABLE` de `evento` y además queda
  comentada la migración para bases que ya tienen la tabla creada.
- `src/main/java/uv/isj/planificadoreventosbackend/model/Evento.java` — `horasEstimadas`, con
  `= 6` por defecto para el alta de la columna.
- `src/main/java/uv/isj/planificadoreventosbackend/model/dto/EventoDTO.java` — el campo con
  `@NotNull @Min(1)`, en la posición que le toca al contrato.
- `src/main/java/uv/isj/planificadoreventosbackend/service/EventoService.java` — se guarda en
  `crearEvento` y `actualizarEvento`, y `validar` lo comprueba como segunda capa.
- Tests: 3 nuevos en `EventoServiceTest`, 1 en `ModeloEntidadesTest`, y los 16 constructores
  de `EventoDTO` que existían en la suite actualizados con las horas.

## Migración (importante)

```sql
ALTER TABLE evento ADD COLUMN horas_estimadas INT NOT NULL DEFAULT 6;
ALTER TABLE evento ALTER COLUMN horas_estimadas DROP DEFAULT;
ALTER TABLE evento ADD CONSTRAINT chk_horas_evento CHECK (horas_estimadas > 0);
```

El `DEFAULT 6` es **obligatorio**: sin él, `ADD COLUMN ... NOT NULL` falla sobre filas
existentes. El `DROP DEFAULT` hace que a partir de ahí la columna deje de rellenarse sola, que
es lo que valida el DTO.

**El SQL tiene que correr antes de desplegar el código.** Con `ddl-auto=validate` Hibernate no
crea columnas: si el código llega primero, la aplicación no arranca. Y los tests no lo
detectan, porque el perfil de test usa `create-drop` y H2 genera el esquema desde la entidad.

## Decisiones

- **Obligatorio, con default 6.** Igual que `horasEstimadas` en `Subtarea`: mismo nombre de
  campo, mismo `@NotNull @Min(1)`, mismo `CHECK (> 0)` en la base.
- **Informativo y no tope.** Si algún día debe bloquear, hará falta una regla y un 409 propio;
  no se quiso anticipar.
- **El total real no cambia.** `getTaskMetrics` y `WorkloadSummary` siguen sumando las
  subtareas. El estimado se muestra aparte.
- **No se toca** `SubtareaRepository`, `SecurityConfig` ni `EventoRepository` (que extiende
  `Repository` a propósito y lo verifica `EventoRepositoryAislamientoTest` por reflexión).

## Pendientes que siguen abiertos

1. **¿El estimado se puede exceder?** Sin regla no hay conflicto que detectar.
2. **Si las subtareas suman más que el estimado, ¿qué muestra el resumen de carga?**
3. **Hueco ya diagnosticado:** el límite diario solo se valida en `PATCH /reprogramar`;
   `POST /subtareas` y `PUT /subtareas` aceptan sobrecarga en silencio.
4. El frontend todavía no consume `/api/users/capacity`, así que no hay pantalla para
   configurar el límite diario.

## Verificación

- `./mvnw test` ✅ — 172 pruebas en verde (168 antes).
- Verificado en verde con JDK 21 vía Docker; en local con `-Dlombok.version=1.18.48`, porque la
  máquina tiene JDK 27 y el Lombok de Spring Boot 4.1.1 no compila ahí.
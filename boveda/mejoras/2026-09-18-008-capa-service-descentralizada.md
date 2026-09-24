---
tipo: mejora
---

# Capa service descentralizada: una clase por entidad

- **Fecha:** 2026-09-18
- **Área:** API / persistencia
- **Estado:** hecha

## Descripción

Se construyó la capa de negocio descentralizada: un `@Service` por entidad que inyecta su repositorio y expone los tres grupos de métodos del CRUD — buscar (`findAll`, `findById`, derivadas), save (un único método condicional que abarca insertar y actualizar) y delete. Los servicios trabajan con entidades JPA (decisión del equipo): el llamador (futuro controller) asigna las relaciones de FK antes de guardar.

## Cambios

- `exception/RecursoNoEncontradoException.java` — runtime exception para id inexistente (lista para mapear a 404 en controllers).
- `service/TipoEventoService.java` — findAll, findById, findByNombre, save, delete.
- `service/UsuarioService.java` — findAll, findById, findByEmail, existsByEmail, save (asigna `cambiar-contrasena` como placeholder cuando el hash viene vacío), delete.
- `service/EventoService.java` — findAll, findById, findByUsuario, save, delete.
- `service/SubtareaService.java` — findAll, findById, findByEvento, findByEstado, save, delete.
- `src/test/java/.../service/ServiciosTest.java` — smoke test con H2 (perfil `test`, `@Transactional`): insert + placeholder, update con `save` sobre id existente, consultas derivadas con FKs asignados por el llamador, delete y excepción ante id inexistente.

## Decisiones

- **`@Transactional` por método**: lectura → `readOnly = true`; escritura → transaccional normal.
- **save condicional único**: `id == null` → INSERT; `id != null` → valida existencia (lanza `RecursoNoEncontradoException`) y actualiza.
- **Inyección estrictamente mínima**: cada service inyecta solo el repo de su entidad (los repos de FK no se inyectan aún en `EventoService`/`SubtareaService` porque el mapeo FK lo hace el llamador al trabajar con entidades).

## Verificación

- `./mvnw test` — BUILD SUCCESS, 16 tests (5 nuevos en `ServiciosTest`).

## Pendiente

- Cuando se construyan los endpoints, el mapeo DTO ↔ Entidad (resolver `idUsuario`, `idTipoEvento`, `idEvento`) irá en la capa de controller.
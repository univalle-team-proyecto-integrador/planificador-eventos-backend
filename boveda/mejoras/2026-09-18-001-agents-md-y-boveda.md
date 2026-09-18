---
tipo: mejora
---

# AGENTS.md y bóveda Obsidian

- **Fecha:** 2026-09-18
- **Área:** documentación / infraestructura
- **Estado:** hecha

## Descripción

Se creó `AGENTS.md` con las directrices para futuras sesiones de trabajo (comandos, gotchas verificados y flujo de la bóveda) y se inicializó la bóveda Obsidian `boveda/` que registra cada mejora y se apoya en un lienzo maestro.

## Cambios

- `AGENTS.md` — instrucciones para agentes: comandos Maven, gotchas de Spring Boot 4.x y de base de datos, estructura del proyecto y flujo de bóveda.
- `boveda/README.md` — índice de la bóveda y cómo registrar mejoras.
- `boveda/lienzo-maestro.canvas` — lienzo maestro de Obsidian con nodos por mejora.
- `boveda/mejoras/plantilla-mejora.md` — plantilla de registro de mejoras.
- `boveda/mejoras/README.md` — índice cronológico de mejoras.

## Verificación

- `./mvnw test` — confirmado que falla por `Failed to determine a suitable driver class` (sin DataSource configurado); este estado quedó documentado en `AGENTS.md`.
- La bóveda abre como carpeta en Obsidian (`.canvas` y notas Markdown con formato estándar).
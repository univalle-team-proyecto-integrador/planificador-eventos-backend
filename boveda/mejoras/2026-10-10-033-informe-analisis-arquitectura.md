---
tipo: mejora
---

# Informe de análisis y arquitectura del backend

- **Fecha:** 2026-10-10
- **Área:** documentación
- **Estado:** hecha

## Descripción

Se revisó el repositorio completo (código, DDL, seguridad, tests, scripts de despliegue y bóveda) con rol de investigador y arquitecto, y se plasmó el análisis en un informe reutilizable. El informe identifica fortalezas, dos hallazgos críticos (`/api/eventos/**` abierto en la cadena de seguridad y desfase de zona horaria en la vista "Hoy") y un roadmap priorizado P0→P2.

Además se creó la carpeta `docs/` como destino de la documentación del proyecto y se dejó en el README la sección donde se subirá el enlace a la carpeta completa.

## Cambios

- `docs/informe-arquitectura.md` — informe completo de análisis y arquitectura (nuevo)
- `README.md` — nueva sección "Documentación del proyecto" con el hueco para el enlace externo y enlaces a `docs/`; `docs/` incluido en la estructura del proyecto
- `boveda/mejoras/README.md` — índice actualizado con la mejora 033
- `boveda/lienzo-maestro.canvas` — nodos m033/p033 y aristas enlazadas al registro

## Verificación

- Revisión manual del informe y del renderizado del README.
- Sin cambios de código: `./mvnw test` no es necesario para este cambio (solo documentación).
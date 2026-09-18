# Bóveda Obsidian — Planificador de Eventos (Backend)

Esta carpeta (`boveda/`) es una bóveda de Obsidian versionada en este repositorio. Sirve como bitácora del desarrollo: cada mejora realizada sobre el código debe quedar registrada aquí en la misma sesión en que se hace.

## Estructura

- `lienzo-maestro.canvas` — lienzo maestro de Obsidian con el mapa del proyecto; cada mejora tiene un nodo enlazado a su registro.
- `mejoras/` — registros de mejoras, un archivo por mejora.
- `mejoras/plantilla-mejora.md` — plantilla para crear un registro nuevo.
- `mejoras/README.md` — índice de todas las mejoras registradas.

## Cómo registrar una mejora

1. Copia `mejoras/plantilla-mejora.md` a `mejoras/AAAA-MM-DD-NNN-breve-slug.md`.
2. Rellena los campos (fecha, área, descripción, verificación).
3. Añade la fila correspondiente al índice `mejoras/README.md`.
4. Añade un nodo en `lienzo-maestro.canvas` enlazado al nuevo archivo.

Notas y registros se escriben en español.
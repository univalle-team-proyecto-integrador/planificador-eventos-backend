# Plantilla de caso de prueba

Usá esta plantilla para registrar cada caso de prueba nuevo en `test/` (un archivo por caso).

---

# CP-<NNN>: <título breve>

- **Fecha:** AAAA-MM-DD
- **Área:** (entorno / conexión / validación-despliegue / pruebas-local)
- **Tipo:** (humo / regresión / e2e / configuración)
- **Estado:** (pasó / falló / en curso)

## Objetivo

¿Qué se quiere demostrar?

## Precondiciones

- (ej.) Backend desplegado en `-1`, frontend en Vercel, `.env` local configurado.

## Pasos

1. Comando / acción.
2. ...

## Resultado esperado

- (ej.) `/api/health` → `{"status":"healthy","database":"connected"}`.

## Resultado obtenido

- (ej.) 200 OK con body esperado.

## Evidencia

- Salida de comando, URL, screenshot, etc.

## Conclusión

- ¿Pasa? ¿Qué se corrigió? ¿Qué quedó pendiente?
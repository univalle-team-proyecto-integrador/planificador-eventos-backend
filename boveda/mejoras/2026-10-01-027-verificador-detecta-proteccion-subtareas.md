---
tipo: mejora
---

# El verificador de despliegue detecta si PROTECT_SUBTAREAS sigue abierto

- **Fecha:** 2026-10-01
- **Área:** infraestructura, despliegue
- **Estado:** hecha

## Descripción

Con US-11 ya desplegado y validado en los dos extremos, el verificador daba **8 correctos, 0 fallidos** mientras `/api/subtareas/**` seguía respondiendo a peticiones anónimas. El plan de despliegue (mejora 024) define dos estados legítimos y bien distintos: durante la transición `PROTECT_SUBTAREAS=false`, y el estado final de US-11 con `true`. El script no miraba ninguno de los dos, así que un despliegue a medio camino se reportaba igual que uno terminado.

Es el mismo patrón de la mejora 025, distinto matiz: un chequeo que compara la respuesta con `[]` no distingue "no hay datos" de "no me dejan mirar", y acá faltaba el paso entero.

## Cambio

Nuevo bloque en el paso 3 de `verificar-despliegue.sh`, dentro de la rama que ya exige `AUTH_OK`:

- Si `/api/subtareas/hoy` **sin token** responde `401`, la ventana está cerrada: `[OK]`.
- Si responde `200` y `PDE_ESPERAR_PROTECCION` no está en `true`, avisa con `[warn]` nombrando la causa exacta y reminds dónde se cambia.
- Si responde `200` y `PDE_ESPERAR_PROTECCION=true`, falla con `[FAIL]` e imprime la ruta del dashboard.

El modo estricto existe porque el script se usa en dos momentos distintos: uno es validar la cadena durante el despliegue y otro es confirmar que US-11 quedó cerrado. Con una sola bandera el mensaje tendría que mentir en uno de los dos casos.

## Verificación

- `bash -n` pasa.
- Ejecución normal contra producción: `[warn] ... PROTECT_SUBTAREAS=false (ventana de compatibilidad abierta)`, resumen 8 correctos 0 fallidos.
- `PDE_ESPERAR_PROTECCION=true`: `[FAIL] /api/subtareas/hoy respondió 200 sin token`, resumen 8 correctos 1 fallido, con la instrucción a pantalla.

Los dos modos se comprobaron contra Render de verdad, no solo por lectura del código.

## Estado del despliegue tras esta nota

Verificado en producción el 2026-10-01:

- Backend Render con 17 rutas y US-11 activo. Caducidad comprobada decodificando el token: `exp - iat = 28800` (8 h).
- Frontend Vercel sirviendo `index-DLuf7OAC.js`, que ya incluye `login`, `register`, `profile`, el token en `localStorage` y la regla de contraseña 8–72 con letra y número.
- Aislamiento comprobado con dos cuentas reales: seis operaciones de la cuenta B sobre datos de A devuelven todas 404.
- `PROTECT_SUBTAREAS` sigue en `false`. Es lo único que falta para cerrar US-11, y es un paso manual del dashboard.

Nota de operación: el remoto del frontend usaba HTTPS sin credenciales y `git push` fallaba pidiendo usuario. Se cambió a SSH (`git@github.com:...`), mismo repositorio y mismo owner, que es el transporte que ya funcionaba en el backend.
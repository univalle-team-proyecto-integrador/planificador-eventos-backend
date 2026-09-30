---
tipo: mejora
---

# Corrección del script de verificación y de la colección Postman para US-11

- **Fecha:** 2026-09-30
- **Área:** infraestructura, pruebas
- **Estado:** hecha

## Descripción

Al revisar los commits de US-11 (mejora 024) aparecieron dos problemas en las herramientas que se usan para **verificar el despliegue**. Los dos importan justo en el momento en que más daño hacen: cuando se sube `PROTECT_SUBTAREAS=true` y hay que confirmar que la cadena sigue viva.

### 1. `verificar-despliegue.sh` daba un falso positivo

El paso 2 pedía `GET /api/eventos?usuarioId=1` sin token y validaba:

```bash
[ -n "$eventos" ] && [ "$eventos" != "[]" ] && ok "..." || warn "..."
```

Con la protección activa esa petición responde **401** con un `ProblemDetail`, que es un JSON **no vacío**: el chequeo pasaba y pintaba `[OK] eventos del usuario 1 devuelven datos` mientras el backend estaba rechazando todo. Verde mentiroso.

La causa de fondo es que un chequeo de "hay datos" no distingue un arreglo de un error. Ahora se exige que la respuesta **empiece por `[`**, y además el script obtiene un token de verdad.

### 2. La colección Postman no podía ni importar

`{{baseUrl}}` aparecía en 54 URLs y **nunca se definió**: la clave `variable` no existía en ningún commit. Quien importara la colección en otro equipo tendría todas las peticiones rotas. Lo mismo pasaba con `{{idTipoEvento}}` y con `{{usuarioId}}`.

Además el flujo no corría de una pasada: `Crear evento` y `Añadir subtarea` no guardaban los ids, así que las 6 peticiones siguientes recibían 400 con el literal `{{idEvento}}` en la URL.

## Cambios

### `scripts/verificar-despliegue.sh`

- **Autentica antes de verificar.** Registra una cuenta fija de verificación (`verificacion.despliegue@eventflow.co`); si ya existe responde 409 y no es fallo, así que en las siguientes ejecuciones solo hace login. Se puede saltar con `PDE_VERIFY_TOKEN`.
- **Verifica el token de verdad** contra `/api/users/profile` y distingue el 401 por `JWT_SECRET` distinto, que es el fallo más probable al desplegar.
- **Exige un arreglo JSON** en `/api/eventos`: un `ProblemDetail` empieza por `{` y ya no puede pasar por una respuesta sana.
- Comprueba `/api/subtareas/hoy` con token (envía `usuarioId` porque el endpoint aún lo exige).
- Comprueba que el `usuarioId` del query se ignora, que es el comportamiento de aislamiento anunciado. Solo si `/api/eventos` devolvió un arreglo, para no repetir el falso positivo.
- `PDE_BACKEND` y `PDE_FRONTEND` ahora son configurables por entorno; los pasos quedaron numerados 1 a 5.
- Los fallos de auth son `[FAIL]`, no `[warn]`: si no se puede autenticar, el resto de los datos no significa nada.
- **`AUTH_OK`**: sin un token verificado, los pasos de datos se marcan como omitidos en lugar de darse por buenos. Un backend viejo con `PROTECT_SUBTAREAS=false` responde `/api/eventos` sin auth, así que esa comprobación podía quedar verde sobre un despliegue que ya no servía US-11.
- Un 404 en `/api/users/profile` se reporta como lo que es: el backend desplegado todavía no tiene US-11.

### `test/postman/planificador-eventos-backend.postman_collection.json`

- **Declara las variables** que ya se usaban: `baseUrl`, `email`, `password`, `nombre`, `token`, `usuarioId`, `idTipoEvento`, `idEvento`, `idSubtarea`.
- **Nuevo grupo `0. Autenticación (US-11)`** con `register`, `login` y `profile`. El script de test de los dos primeros guarda el token en la variable de colección `{{token}}`.
- **`Authorization: Bearer {{token}}`** en los 3 grupos que quedarán protegidos (Eventos, Subtareas, Casos negativos). Los públicos se dejan sin auth.
- Los scripts de `Crear evento` y `Añadir subtarea` guardan `idEvento` e `idSubtarea`, y el flujo completo corre en una sola pasada.
- `Reprogramar subtarea` enviaba `{fechaObjetivo, horasEstimadas}` contra un DTO que pide `{nuevaFecha, nuevasHoras}`: recibía 400 y era un error de la colección, no del backend.
- `Eliminar evento` se movió al final. Estaba en el grupo de Eventos y borraba en cascada, así que el grupo de Subtareas siempre recibía 404.

## Verificación

Se levantó la aplicación con el perfil `test` (H2) y `app.security.protect-subtareas=true`, con el catálogo seedeado, y se ejecutó la colección completa con newman:

- **30 peticiones, 0 fallos.** Register 201 → login 200 → profile 200 → crear evento 201 → detalle 200 → actualizar 200 → añadir subtarea 201 → reprogramar 200 → eliminar 204. Los casos negativos siguen dando 400/404 como deben.
- El script se probó en los tres escenarios: con token válido (verde), con `PDE_VERIFY_TOKEN` inválido (falla con el mensaje de `JWT_SECRET`) y con la protección activa y sin token (falla, que es justo lo que antes pasaba por verde).
- `./mvnw test` → 141 pruebas, 0 fallos.

## Hallazgo: el backend de Render todavía no tiene US-11

Al ejecutar el script contra `https://planificador-eventos-backend-1.onrender.com` (sin `PDE_BACKEND`) apareció lo que el script viejo **no podía** detectar:

```
[FAIL] registro inesperado → {"timestamp":"...","status":404,"error":"Not Found","path":"/api/users/register"}
[FAIL] /api/users/profile → 404: el backend desplegado no tiene US-11 (falta hacer push y redesplegar)
```

El deployed responde con el JSON de error por defecto de Spring, no con `ProblemDetail`, y `/api/users/**` da 404. O sea, Render sigue con el commit anterior a US-11: es esperado, porque el backend **nunca se ha pusheado**. Lo que faltaba era que la herramienta de verificación lo dijera con claridad en vez de aparentar que todo iba bien.

Estado real del despliegue en este momento: `/api/health` correcto, base de datos conectada, CORS authorizing a Vercel y el bundle del frontend apuntando al backend correcto. US-11 es lo único que falta, y solo se activa cuando haya push y redespliegue.

## Pendiente

- `POST /api/eventos` exige `idUsuario` en el cuerpo (`@NotNull`) y **después lo ignora**: el propietario sale del token. Se detectó al validar el flujo. El frontend lo envía por herencia de `VITE_USER_ID`, así que nada falla hoy, pero es contradictorio. Decidir si `idUsuario` pasa a ser opcional y el frontend deja de mandarlo.
- BCrypt trunca a 72 **bytes** mientras `@Size(max = 72)` cuenta caracteres: una contraseña larga con acentos se recorta en silencio. Preexistente, pero ahora el registro es público.

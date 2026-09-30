---
tipo: mejora
---

# La caducidad del token se configura en segundos (JWT_EXPIRATION_SECONDS)

- **Fecha:** 2026-09-30
- **Área:** configuración, seguridad
- **Estado:** hecha

## Descripción

Al intentar validar los tokens en el despliegue apareció un desajuste que venía de US-11 (mejora 024): **la variable de entorno documentada no era la que leía la aplicación**.

`render.yaml`, `.env.example` y el `README` documentaban `JWT_EXPIRATION_SECONDS=28800` (8 horas), pero `application.properties` leía `JWT_EXPIRATION_MINUTES` con un default de 120. Dos consecuencias:

- La variable de Render se ignoraba **en silencio**: poner `JWT_EXPIRATION_SECONDS=28800` no cambiaba nada.
- Los tokens duraban 120 minutos, no las 8 horas que decía la documentación. Nadie se habría dado cuenta salvo que alguien se quejara de que lo expulsa a las dos horas.

La causa de fondo no era un typo, sino una decisión de unidad tomada en el código sin mirar qué unidad usaba el resto del proyecto. `AuthResponseDTO.expiresIn` ya venía en **segundos** y así lo consume el frontend, así que la app era la que estaba fuera de norma.

## Cambios

- `JwtService` pasa a trabajar con `expirationSegundos`: el claim `exp` se calcula con `ChronoUnit.SECONDS` y `getExpirationSegundos()` ya no multiplica por 60.
- `application.properties`: `app.security.jwt.expiration-seconds=${JWT_EXPIRATION_SECONDS:28800}`.
- `application-test.properties`: `app.security.jwt.expiration-seconds=3600` (la misma hora de antes, expresada en segundos).
- `.env` local: `JWT_EXPIRATION_SECONDS=28800`.
- No queda ninguna referencia a `JWT_EXPIRATION_MINUTES` en el repo, salvo el nombre de la variable local corregido.

## Pruebas

Nueva prueba en `JwtSecurityIntegrationTest`:

```
La caducidad sale de JWT_EXPIRATION_SECONDS y el token la respeta
```

Comprueba dos cosas a la vez, porque el fallo anterior era que las dos se desincronizaran:

- `expiresIn` de la respuesta es igual a los segundos configurados.
- La ventana real del token (`exp` menos `iat`) es **exactamente la misma**. Si alguien vuelve a meter una conversión, el DTO mentiría sobre la caducidad efectiva y la prueba lo detecta.

`./mvnw test` → 142 pruebas, 0 fallos (era 141).

## Nota operativa

El `.env` local de desarrollo se había quedado viejo: solo tenía `DB_URL`, `DB_USER` y `DB_PASSWORD`, los tres anteriores a US-11. Como `app.security.jwt.secret` no tiene default, `./mvnw spring-boot:run` **ya no arrancaba** en local. Se le añadieron `JWT_SECRET`, `JWT_EXPIRATION_SECONDS`, `PROTECT_SUBTAREAS` y `LEGACY_USER_ID`. El secreto se generó con `openssl rand -base64 48` y vive solo en el `.env` local, que sigue ignorado por git.

## Pendiente

- Tras promover el deploy, `PROTECT_SUBTAREAS` debe subir a `true` en Render: hasta entonces `/api/subtareas/**` sigue abierta y acotada a `LEGACY_USER_ID`.
- Sigue pendiente decidir si `EventoDTO.idUsuario` pasa a ser opcional (hoy es obligatorio y se ignora) y si `/api/subtareas/hoy` deja de exigir `usuarioId` en el query.

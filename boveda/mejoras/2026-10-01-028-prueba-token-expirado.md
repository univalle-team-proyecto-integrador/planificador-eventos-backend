---
tipo: mejora
---

# Prueba de token expirado: el último requisito de US-11 sin evidencia

- **Fecha:** 2026-10-01
- **Área:** pruebas, seguridad
- **Estado:** hecha

## Descripción

Al auditar US-11 contra la lista de requisitos de la etapa 1, ocho de los nueve estaban verificados contra producción. El hueco era **token inválido o expirado**: la firma inválida tenía pruebas y estaba comprobada en Render, pero la caducidad solo se daba por buena leyendo que `NimbusJwtDecoder` valida `exp` por defecto.

Eso no es una verificación. Es confiar en el framework, y es justo el tipo de confianza que en este proyecto ya había salido caro: las mejoras 025 y 026 fueron dos casos de código que parecía correcto y no lo era.

## El detalle que costó encontrar

Para firmar un token vencido hubo que entender una restricción del formato JWT: `exp` tiene que ser **posterior** a `iat`. El primer intento desplazaba los dos instantes al pasado por la misma cantidad, así que el token se firmaba, pero con `exp` todavía 50 minutos en el futuro. La prueba falló pidiendo 401 y recibió **200**.

Se lee como un agujero de seguridad y no lo era: era aritmética mal planteada en el helper. Lo peligroso es el tiempo que se pierde en investigarlo como si fuera un fallo de producción. Por eso la API del helper quedó explícita en vez de paramétrica:

- `firmar(secreto, idUsuario, email)`: token válido, vence en 60 minutos.
- `firmarVencido(secreto, idUsuario, email)`: emitido hace 2 horas, vencido hace 1.

Un parámetro `minutosVigencia` con valor negativo era exactamente la trampa que hizo fallar la primera versión.

## Cambios

- `JwtServiceConSecretoHelper`: se separa el caso "secreto ajeno" del caso "vencido", con un método privado que recibe los instantes explícitos. El nombre del helper también cambió: ya no es "con secreto ajeno" porque ahora firma con el secreto real de la app para probar la caducidad.
- `JwtSecurityIntegrationTest.tokenVencidoResponde401`: firma con `SECRETO_DE_PRUEBAS`, el mismo valor que `application-test.properties`, y exige 401 en `/api/users/profile` y en `/api/subtareas/hoy`. La firma es correcta; lo único vencido es el claim `exp`. Esa es la razón de ser la prueba: separa el rechazo por caducidad del rechazo por firma.

## Verificación

Una prueba que pasa no prueba nada si nunca falla. Se comprobó con mutación: se relaja el `JwtTimestampValidator` del `JwtDecoder` a una tolerancia de diez años, con lo que cualquier token vencido se aceptaría.

```
Tests run: 23, Failures: 1, Errors: 0
tokenVencidoResponde401:215 Status expected:<401> but was:<200>
```

Falla justo en la aserción que debe, y las otras 22 pruebas siguen pasando: el fallo es específico de la caducidad, no un efecto colateral. Con la mutación revertida, `./mvnw test` → **143 pruebas, 0 fallos** (eran 142).

Los dos intentos de mutación previos no contaron: `setJwtValidator` con una lambda que no compilaba, y un `build(Duration)` inexistente. Fallaron al arrancar el contexto, no al ejecutar la prueba, así que no demostraban nada. Hubo que llegar a una mutación que compilara para que la comprobación fuera válida.

## Estado de US-11

Los nueve requisitos de la etapa 1 tienen evidencia:

- `POST /api/users/register/`, login y `GET /api/users/profile/`: 201/200/401 en producción, con y sin barra final.
- JWT HS256 con `bearerAuth` en Swagger.
- Respuesta `{token, tokenType, expiresIn, usuario}`, con `expiresIn` igual a la ventana real del token.
- Correo duplicado 409, credenciales incorrectas 401, usuario inexistente 401, token inválido 401, **token vencido 401**.

Sigue pendiente el paso manual de Render: subir `PROTECT_SUBTAREAS` a `true`.
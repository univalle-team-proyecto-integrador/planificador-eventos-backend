package uv.isj.planificadoreventosbackend.controller;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Genera tokens con cualquier secreto, para comprobar qué acepta y qué rechaza
 * la validación: firma ajena (secreto incorrecto) y caducidad (exp vencido).
 */
final class JwtServiceConSecretoHelper {

    /** Vigencia por defecto de los tokens que firma este helper. */
    private static final int VIGENCIA_MINUTOS = 60;

    private JwtServiceConSecretoHelper() {
    }

    /** Token válido: se firma ahora y vence en {@link #VIGENCIA_MINUTOS}. */
    static String firmar(String secreto, Integer idUsuario, String email) {
        Instant ahora = Instant.now();
        return firmar(secreto, idUsuario, email,
                ahora, ahora.plus(VIGENCIA_MINUTOS, ChronoUnit.MINUTES));
    }

    /**
     * Token ya vencido: se emitió hace 2 horas y venció hace 1. Los dos
     * instantes van en el pasado a propósito, porque el JWT exige que
     * {@code exp} sea posterior a {@code iat} y no se puede "caducar" un token
     * desplazando solo {@code exp}.
     */
    static String firmarVencido(String secreto, Integer idUsuario, String email) {
        Instant ahora = Instant.now();
        return firmar(secreto, idUsuario, email,
                ahora.minus(2, ChronoUnit.HOURS), ahora.minus(1, ChronoUnit.HOURS));
    }

    private static String firmar(
            String secreto, Integer idUsuario, String email, Instant emitido, Instant expira) {
        SecretKey clave = new SecretKeySpec(
                secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(clave));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("planificador-eventos-backend-test")
                .issuedAt(emitido)
                .expiresAt(expira)
                .subject(String.valueOf(idUsuario))
                .claim("email", email)
                .build();

        return encoder.encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}

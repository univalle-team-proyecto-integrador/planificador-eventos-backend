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
 * Genera tokens con un secreto que NO es el de la aplicación, para comprobar
 * que la validación de firma rechaza lo que no emitido por el backend.
 */
final class JwtServiceConSecretoHelper {

    private JwtServiceConSecretoHelper() {
    }

    static String firmar(String secreto, Integer idUsuario, String email) {
        SecretKey clave = new SecretKeySpec(
                secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(clave));

        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("planificador-eventos-backend-test")
                .issuedAt(ahora)
                .expiresAt(ahora.plus(60, ChronoUnit.MINUTES))
                .subject(String.valueOf(idUsuario))
                .claim("email", email)
                .build();

        return encoder.encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}

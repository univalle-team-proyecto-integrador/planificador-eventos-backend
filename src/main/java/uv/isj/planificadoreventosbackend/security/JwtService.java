package uv.isj.planificadoreventosbackend.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * Genera y describe los tokens JWT de la aplicación.
 *
 * <p>La criptografía (HS256) la resuelve Nimbus a través de
 * {@link JwtEncoder}; esta clase solo compone los claims. La firma y la
 * expiración se validan en {@link org.springframework.security.oauth2.jwt.JwtDecoder},
 * registrado en SecurityConfig.
 */
@Component
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final long expirationSegundos;

    public JwtService(
            JwtEncoder jwtEncoder,
            @Value("${app.security.jwt.issuer}") String issuer,
            @Value("${app.security.jwt.expiration-seconds}") long expirationSegundos) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.expirationSegundos = expirationSegundos;
    }

    public String generarToken(Integer idUsuario, String email) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(ahora)
                .expiresAt(ahora.plus(expirationSegundos, ChronoUnit.SECONDS))
                .subject(String.valueOf(idUsuario))
                .claim("email", email)
                .claim("scope", "ROLE_USUARIO")
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long getExpirationSegundos() {
        return expirationSegundos;
    }

    /**
     * Convierte el secreto de texto en una clave utilizable por HMAC-SHA256.
     * Exigido que tenga al menos 256 bits: por debajo, HS256 no es seguro.
     */
    public static SecretKey claveDesdeSecreto(String secreto) {
        if (secreto == null || secreto.isBlank()) {
            throw new IllegalStateException(
                    "Falta JWT_SECRET: define un secreto de al menos 32 caracteres");
        }
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET es demasiado corto: se requieren al menos 32 caracteres (256 bits) para HS256");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    public static Date instanteActual() {
        return Date.from(Instant.now());
    }
}

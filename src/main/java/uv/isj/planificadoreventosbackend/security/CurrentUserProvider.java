package uv.isj.planificadoreventosbackend.security;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import uv.isj.planificadoreventosbackend.exception.SinAutenticacionException;

/**
 * Resuelve el propietario de la petición.
 *
 * <p>Es la única pieza que decide de dónde sale el {@code idUsuario}, y por eso
 * la responsabilidad del aislamiento de datos queda concentrada aquí:
 *
 * <ul>
 *   <li>Si hay un token válido, manda el {@code idUsuario} del token y el
 *       parámetro {@code usuarioId} que envíe el cliente se ignora. Así no hay
 *       escalada horizontal: pedir datos de otra cuenta no es posible
 *       cambiando un query param.</li>
 *   <li>Si la petición es anónima, se usa {@code app.security.legacy-user-id}
 *       únicamente mientras {@code app.security.protect-subtareas} esté en
 *       {@code false}. Esa es la ventana de compatibilidad con el frontend
 *       anterior a US-11, que fija {@code VITE_USER_ID=1}: el comportamiento
 *       observable no cambia, pero el aislamiento por propietario ya está
 *       activo en la capa de datos.</li>
 * </ul>
 */
@Component
public class CurrentUserProvider {

    private final boolean protegerSubtareas;
    private final Integer idUsuarioLegado;

    public CurrentUserProvider(
            @Value("${app.security.protect-subtareas:false}") boolean protegerSubtareas,
            @Value("${app.security.legacy-user-id:1}") Integer idUsuarioLegado) {
        this.protegerSubtareas = protegerSubtareas;
        this.idUsuarioLegado = idUsuarioLegado;
    }

    public boolean esAutenticado() {
        return idUsuarioAutenticado().isPresent();
    }

    public Optional<Integer> idUsuarioAutenticado() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null
                || !autenticacion.isAuthenticated()
                || !(autenticacion.getPrincipal() instanceof Integer idUsuario)) {
            return Optional.empty();
        }
        return Optional.of(idUsuario);
    }

    /**
     * Propietario efectivo de la petición. Exige siempre un identificador: el
     * repositorio nunca recibe consultas sin dueño.
     */
    public Integer idUsuarioActual() {
        return idUsuarioAutenticado()
                .orElseGet(this::idUsuarioLegadoSiProcede);
    }

    /** Igual que {@link #idUsuarioActual()} pero falla si no hay token. */
    public Integer idUsuarioRequerido() {
        return idUsuarioAutenticado()
                .orElseThrow(() -> new SinAutenticacionException(
                        "Se requiere un token de autenticación válido"));
    }

    private Integer idUsuarioLegadoSiProcede() {
        if (protegerSubtareas) {
            throw new SinAutenticacionException(
                    "Se requiere un token de autenticación válido");
        }
        if (idUsuarioLegado == null || idUsuarioLegado <= 0) {
            throw new SinAutenticacionException(
                    "Se requiere un token de autenticación válido");
        }
        return idUsuarioLegado;
    }
}

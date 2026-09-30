package uv.isj.planificadoreventosbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AuthResponseDTO", description = "Token de acceso y datos del usuario autenticado")
public record AuthResponseDTO(
        @Schema(description = "Token JWT para la cabecera Authorization: Bearer")
        String token,

        @Schema(description = "Tipo de token", example = "Bearer")
        String tokenType,

        @Schema(description = "Segundos de validez del token", example = "7200")
        long expiresIn,

        @Schema(description = "Usuario dueño de la sesión")
        UsuarioDTO usuario) {
}

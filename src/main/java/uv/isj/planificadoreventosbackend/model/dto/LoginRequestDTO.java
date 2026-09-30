package uv.isj.planificadoreventosbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "LoginRequestDTO", description = "Credenciales de acceso")
public record LoginRequestDTO(
        @Schema(description = "Correo registrado", example = "santiago@uni.edu",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Email
        @Size(max = 150)
        String email,

        @Schema(description = "Contraseña", example = "Planificador2026",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 72)
        String password) {
}

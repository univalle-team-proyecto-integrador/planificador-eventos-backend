package uv.isj.planificadoreventosbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "RegistroRequestDTO", description = "Datos de alta de un usuario")
public record RegistroRequestDTO(
        @Schema(description = "Correo institucional", example = "santiago@uni.edu",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Email
        @Size(max = 150)
        String email,

        @Schema(description = "Nombre completo", example = "Santiago Pérez",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 100)
        String nombre,

        @Schema(description = "Contraseña: 8 a 72 caracteres, con al menos una letra y un número",
                example = "Planificador2026",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(min = 8, max = 72)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "La contraseña debe incluir al menos una letra y un número")
        String password,

        @Schema(description = "Límite de horas diarias; si se omite se aplica 6",
                example = "6", minimum = "1", maximum = "16")
        @Min(1)
        @Max(16)
        Integer limiteHorasDiarias) {
}

package uv.isj.planificadoreventosbackend.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioDTO(
        Integer idUsuario,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 100) String nombre,
        @NotNull @Min(1) @Max(16) Integer limiteHorasDiarias) {
}
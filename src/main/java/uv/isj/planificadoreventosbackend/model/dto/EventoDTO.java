package uv.isj.planificadoreventosbackend.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record EventoDTO(
        Integer idEvento,
        @NotNull Integer idUsuario,
        @NotNull Integer idTipoEvento,
        @NotBlank @Size(max = 150) String nombre,
        @NotBlank @Size(max = 150) String cliente,
        @NotNull LocalDateTime fechaEvento,
        @NotBlank @Size(max = 255) String lugar,
        LocalDateTime fechaCreacion) {
}
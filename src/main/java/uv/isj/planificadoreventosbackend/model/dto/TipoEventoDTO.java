package uv.isj.planificadoreventosbackend.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TipoEventoDTO(
        Integer idTipoEvento,
        @NotBlank @Size(max = 50) String nombre) {
}
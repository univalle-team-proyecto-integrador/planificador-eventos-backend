package uv.isj.planificadoreventosbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(name = "CapacidadDTO", description = "Capacidad de trabajo diario de un organizador")
public record CapacidadDTO(
        @Schema(description = "Identificador del organizador", example = "1")
        Integer usuarioId,
        @Schema(description = "Límite diario configurado, entre 1 y 16 horas", example = "6")
        Integer limiteHorasDiarias,
        @Schema(description = "Fecha evaluada", example = "2026-11-20")
        LocalDate fecha,
        @Schema(description = "Horas ya comprometidas en esa fecha; no cuenta las ejecutadas", example = "5")
        Integer horasPlanificadas,
        @Schema(description = "Horas que aún se pueden asignar; nunca es negativo", example = "1")
        Integer horasDisponibles) {
}
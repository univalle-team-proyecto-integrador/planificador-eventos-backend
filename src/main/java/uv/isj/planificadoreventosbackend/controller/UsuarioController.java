package uv.isj.planificadoreventosbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uv.isj.planificadoreventosbackend.model.dto.CapacidadDTO;
import uv.isj.planificadoreventosbackend.model.dto.LimiteHorasDTO;
import uv.isj.planificadoreventosbackend.service.UsuarioService;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "Capacidad de trabajo diario del organizador")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/capacidad")
    @Operation(summary = "Consultar la capacidad de trabajo de una fecha")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Capacidad del organizador para la fecha consultada"),
            @ApiResponse(responseCode = "400", description = "El organizador no es válido"),
            @ApiResponse(responseCode = "404", description = "El organizador no existe")
    })
    public CapacidadDTO obtenerCapacidad(
            @Parameter(description = "Identificador del organizador", example = "1")
            @RequestParam Integer usuarioId,
            @Parameter(description = "Fecha a evaluar en formato ISO; por omisión, el hoy de Colombia",
                    example = "2026-11-20")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return usuarioService.obtenerCapacidad(usuarioId, fecha);
    }

    @PutMapping("/capacidad")
    @Operation(summary = "Ajustar el límite de horas diarias del organizador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Límite actualizado con la capacidad recalculada"),
            @ApiResponse(responseCode = "400", description = "El límite está fuera del rango de 1 a 16 horas"),
            @ApiResponse(responseCode = "404", description = "El organizador no existe")
    })
    public CapacidadDTO actualizarCapacidad(
            @Parameter(description = "Identificador del organizador", example = "1")
            @RequestParam Integer usuarioId,
            @Valid @RequestBody LimiteHorasDTO dto) {
        return usuarioService.actualizarLimite(usuarioId, dto.limiteHorasDiarias());
    }
}
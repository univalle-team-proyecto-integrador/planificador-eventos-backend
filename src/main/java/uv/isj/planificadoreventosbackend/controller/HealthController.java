package uv.isj.planificadoreventosbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uv.isj.planificadoreventosbackend.model.HealthResponse;
import uv.isj.planificadoreventosbackend.service.HealthService;

@RestController
@Tag(name = "Salud", description = "Comprobación del backend y de la conexión a la base de datos")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping({"/api/health", "/api/health/"})
    @Operation(summary = "Consultar el estado del servicio")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "La aplicación y la base de datos están disponibles"),
            @ApiResponse(responseCode = "503", description = "La base de datos no está disponible")
    })
    public ResponseEntity<HealthResponse> health() {
        HealthResponse health = healthService.checkDatabase();
        HttpStatus status = "healthy".equals(health.status()) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(health);
    }
}

package uv.isj.planificadoreventosbackend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import uv.isj.planificadoreventosbackend.model.HealthResponse;
import uv.isj.planificadoreventosbackend.service.HealthService;

@RestController
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping({"/api/health", "/api/health/"})
    public ResponseEntity<HealthResponse> health() {
        HealthResponse health = healthService.checkDatabase();
        HttpStatus status = "healthy".equals(health.status()) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(health);
    }
}
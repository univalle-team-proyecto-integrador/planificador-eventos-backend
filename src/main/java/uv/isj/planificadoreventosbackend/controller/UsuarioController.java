package uv.isj.planificadoreventosbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uv.isj.planificadoreventosbackend.model.dto.AuthResponseDTO;
import uv.isj.planificadoreventosbackend.model.dto.CapacidadDTO;
import uv.isj.planificadoreventosbackend.model.dto.LimiteHorasDTO;
import uv.isj.planificadoreventosbackend.model.dto.LoginRequestDTO;
import uv.isj.planificadoreventosbackend.model.dto.RegistroRequestDTO;
import uv.isj.planificadoreventosbackend.model.dto.UsuarioDTO;
import uv.isj.planificadoreventosbackend.security.CurrentUserProvider;
import uv.isj.planificadoreventosbackend.service.AuthService;
import uv.isj.planificadoreventosbackend.service.UsuarioService;

/**
 * US-11. Se registra sin barra final porque Spring Boot 4 ya no la añade por
 * defecto, y con barra porque es como la invoca el cliente.
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Usuarios", description = "Registro, autenticación y perfil del usuario")
public class UsuarioController {

    private final AuthService authService;
    private final CurrentUserProvider currentUserProvider;
    private final UsuarioService usuarioService;

    public UsuarioController(
            AuthService authService,
            CurrentUserProvider currentUserProvider,
            UsuarioService usuarioService) {
        this.authService = authService;
        this.currentUserProvider = currentUserProvider;
        this.usuarioService = usuarioService;
    }

    @PostMapping({ "/register", "/register/" })
    @Operation(summary = "Crear una cuenta")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cuenta creada"),
            @ApiResponse(responseCode = "400", description = "Los datos no son válidos"),
            @ApiResponse(responseCode = "409", description = "El correo ya está registrado")
    })
    public ResponseEntity<AuthResponseDTO> registrar(
            @Valid @RequestBody RegistroRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(dto));
    }

    @PostMapping({ "/login", "/login/" })
    @Operation(summary = "Iniciar sesión")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión iniciada"),
            @ApiResponse(responseCode = "400", description = "Los datos no son válidos"),
            @ApiResponse(responseCode = "401", description = "Correo o contraseña incorrectos")
    })
    public AuthResponseDTO login(@Valid @RequestBody LoginRequestDTO dto) {
        return authService.login(dto);
    }

    @GetMapping({ "/profile", "/profile/" })
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Consultar el perfil autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil del usuario del token"),
            @ApiResponse(responseCode = "401", description = "Falta el token o no es válido")
    })
    public UsuarioDTO perfil() {
        return authService.perfil(currentUserProvider.idUsuarioRequerido());
    }

    /**
     * Capacidad del usuario del token. No acepta un id por query: el propietario
     * se resuelve siempre desde el token, igual que en el resto de la API.
     */
    @GetMapping({ "/capacity", "/capacity/" })
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Consultar la capacidad de trabajo diario",
            description = "Devuelve el límite configurado, las horas ya comprometidas en la "
                    + "fecha y las que quedan disponibles. Las subtareas ejecutadas no cuentan.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Capacidad del organizador autenticado"),
            @ApiResponse(responseCode = "401", description = "Falta el token o no es válido")
    })
    public CapacidadDTO capacidad(
            @Parameter(description = "Fecha a evaluar en formato ISO; por omisión, el hoy de Colombia",
                    example = "2026-11-20")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return usuarioService.obtenerCapacidad(currentUserProvider.idUsuarioRequerido(), fecha);
    }

    @PutMapping({ "/capacity", "/capacity/" })
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Ajustar el límite de horas diarias")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Límite actualizado con la capacidad recalculada"),
            @ApiResponse(responseCode = "400",
                    description = "El límite está fuera del rango de 1 a 16 horas"),
            @ApiResponse(responseCode = "401", description = "Falta el token o no es válido")
    })
    public CapacidadDTO actualizarCapacidad(@Valid @RequestBody LimiteHorasDTO dto) {
        return usuarioService.actualizarLimite(
                currentUserProvider.idUsuarioRequerido(),
                dto.limiteHorasDiarias());
    }
}

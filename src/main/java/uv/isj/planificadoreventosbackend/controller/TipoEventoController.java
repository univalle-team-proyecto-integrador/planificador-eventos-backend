package uv.isj.planificadoreventosbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uv.isj.planificadoreventosbackend.model.dto.TipoEventoDTO;
import uv.isj.planificadoreventosbackend.service.TipoEventoService;

@RestController
@RequestMapping("/api/tipos-evento")
@Tag(name = "Tipos de evento", description = "Catálogo de tipos de evento disponibles")
public class TipoEventoController {

    private final TipoEventoService tipoEventoService;

    public TipoEventoController(TipoEventoService tipoEventoService) {
        this.tipoEventoService = tipoEventoService;
    }

    @GetMapping
    @Operation(summary = "Listar tipos de evento")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Catálogo de tipos de evento")
    })
    public List<TipoEventoDTO> listar() {
        return tipoEventoService.obtenerTodos();
    }
}

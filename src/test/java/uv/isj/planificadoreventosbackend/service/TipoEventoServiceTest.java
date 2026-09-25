package uv.isj.planificadoreventosbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import uv.isj.planificadoreventosbackend.model.TipoEvento;
import uv.isj.planificadoreventosbackend.model.dto.TipoEventoDTO;
import uv.isj.planificadoreventosbackend.repository.TipoEventoRepository;

@ExtendWith(MockitoExtension.class)
class TipoEventoServiceTest {

    @Mock
    private TipoEventoRepository tipoEventoRepository;

    @Test
    void listaLosTiposDeEventoOrdenadosPorNombre() {
        TipoEvento boda = new TipoEvento();
        boda.setIdTipoEvento(1);
        boda.setNombre("Boda");

        TipoEvento taller = new TipoEvento();
        taller.setIdTipoEvento(2);
        taller.setNombre("Taller");

        when(tipoEventoRepository.findAll(any(Sort.class))).thenReturn(List.of(boda, taller));

        TipoEventoService servicio = new TipoEventoService(tipoEventoRepository);
        List<TipoEventoDTO> resultado = servicio.obtenerTodos();

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).idTipoEvento()).isEqualTo(1);
        assertThat(resultado.get(0).nombre()).isEqualTo("Boda");
        assertThat(resultado.get(1).nombre()).isEqualTo("Taller");
        verify(tipoEventoRepository).findAll(any(Sort.class));
    }

    @Test
    void devuelveListaVaciaCuandoNoHayTipos() {
        when(tipoEventoRepository.findAll(any(Sort.class))).thenReturn(List.of());

        TipoEventoService servicio = new TipoEventoService(tipoEventoRepository);

        assertThat(servicio.obtenerTodos()).isEmpty();
    }
}
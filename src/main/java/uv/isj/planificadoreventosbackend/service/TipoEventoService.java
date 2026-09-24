package uv.isj.planificadoreventosbackend.service;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uv.isj.planificadoreventosbackend.model.TipoEvento;
import uv.isj.planificadoreventosbackend.model.dto.TipoEventoDTO;
import uv.isj.planificadoreventosbackend.repository.TipoEventoRepository;

@Service
public class TipoEventoService {

    private final TipoEventoRepository tipoEventoRepository;

    public TipoEventoService(TipoEventoRepository tipoEventoRepository) {
        this.tipoEventoRepository = tipoEventoRepository;
    }

    @Transactional(readOnly = true)
    public List<TipoEventoDTO> obtenerTodos() {
        return tipoEventoRepository.findAll(Sort.by("nombre")).stream()
                .map(this::aDto)
                .toList();
    }

    private TipoEventoDTO aDto(TipoEvento tipoEvento) {
        return new TipoEventoDTO(
                tipoEvento.getIdTipoEvento(),
                tipoEvento.getNombre());
    }
}

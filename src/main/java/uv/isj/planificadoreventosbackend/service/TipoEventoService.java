package uv.isj.planificadoreventosbackend.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uv.isj.planificadoreventosbackend.exception.RecursoNoEncontradoException;
import uv.isj.planificadoreventosbackend.model.TipoEvento;
import uv.isj.planificadoreventosbackend.repository.TipoEventoRepository;

@Service
public class TipoEventoService {

    private final TipoEventoRepository tipoEventoRepository;

    public TipoEventoService(TipoEventoRepository tipoEventoRepository) {
        this.tipoEventoRepository = tipoEventoRepository;
    }

    @Transactional(readOnly = true)
    public List<TipoEvento> findAll() {
        return tipoEventoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<TipoEvento> findById(Integer idTipoEvento) {
        return tipoEventoRepository.findById(idTipoEvento);
    }

    @Transactional(readOnly = true)
    public Optional<TipoEvento> findByNombre(String nombre) {
        return tipoEventoRepository.findByNombre(nombre);
    }

    @Transactional
    public TipoEvento save(TipoEvento tipoEvento) {
        if (tipoEvento.getIdTipoEvento() != null) {
            tipoEventoRepository.findById(tipoEvento.getIdTipoEvento())
                    .orElseThrow(RecursoNoEncontradoException::new);
        }
        return tipoEventoRepository.save(tipoEvento);
    }

    @Transactional
    public void delete(Integer idTipoEvento) {
        tipoEventoRepository.findById(idTipoEvento)
                .orElseThrow(RecursoNoEncontradoException::new);
        tipoEventoRepository.deleteById(idTipoEvento);
    }
}
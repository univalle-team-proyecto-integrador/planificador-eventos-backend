package uv.isj.planificadoreventosbackend.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uv.isj.planificadoreventosbackend.exception.RecursoNoEncontradoException;
import uv.isj.planificadoreventosbackend.model.EstadoSubtarea;
import uv.isj.planificadoreventosbackend.model.Subtarea;
import uv.isj.planificadoreventosbackend.repository.SubtareaRepository;

@Service
public class SubtareaService {

    private final SubtareaRepository subtareaRepository;

    public SubtareaService(SubtareaRepository subtareaRepository) {
        this.subtareaRepository = subtareaRepository;
    }

    @Transactional(readOnly = true)
    public List<Subtarea> findAll() {
        return subtareaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Subtarea> findById(Integer idSubtarea) {
        return subtareaRepository.findById(idSubtarea);
    }

    @Transactional(readOnly = true)
    public List<Subtarea> findByEvento(Integer idEvento) {
        return subtareaRepository.findByEvento_IdEvento(idEvento);
    }

    @Transactional(readOnly = true)
    public List<Subtarea> findByEstado(EstadoSubtarea estado) {
        return subtareaRepository.findByEstado(estado);
    }

    @Transactional
    public Subtarea save(Subtarea subtarea) {
        if (subtarea.getIdSubtarea() != null) {
            subtareaRepository.findById(subtarea.getIdSubtarea())
                    .orElseThrow(RecursoNoEncontradoException::new);
        }
        return subtareaRepository.save(subtarea);
    }

    @Transactional
    public void delete(Integer idSubtarea) {
        subtareaRepository.findById(idSubtarea)
                .orElseThrow(RecursoNoEncontradoException::new);
        subtareaRepository.deleteById(idSubtarea);
    }
}
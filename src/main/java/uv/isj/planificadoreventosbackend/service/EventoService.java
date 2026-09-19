package uv.isj.planificadoreventosbackend.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uv.isj.planificadoreventosbackend.exception.RecursoNoEncontradoException;
import uv.isj.planificadoreventosbackend.model.Evento;
import uv.isj.planificadoreventosbackend.repository.EventoRepository;

@Service
public class EventoService {

    private final EventoRepository eventoRepository;

    public EventoService(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    @Transactional(readOnly = true)
    public List<Evento> findAll() {
        return eventoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Evento> findById(Integer idEvento) {
        return eventoRepository.findById(idEvento);
    }

    @Transactional(readOnly = true)
    public List<Evento> findByUsuario(Integer idUsuario) {
        return eventoRepository.findByUsuario_IdUsuario(idUsuario);
    }

    @Transactional
    public Evento save(Evento evento) {
        if (evento.getIdEvento() != null) {
            eventoRepository.findById(evento.getIdEvento())
                    .orElseThrow(RecursoNoEncontradoException::new);
        }
        return eventoRepository.save(evento);
    }

    @Transactional
    public void delete(Integer idEvento) {
        eventoRepository.findById(idEvento)
                .orElseThrow(RecursoNoEncontradoException::new);
        eventoRepository.deleteById(idEvento);
    }
}
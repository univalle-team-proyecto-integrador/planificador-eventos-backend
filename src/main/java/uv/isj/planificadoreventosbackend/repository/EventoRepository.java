package uv.isj.planificadoreventosbackend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import uv.isj.planificadoreventosbackend.model.Evento;

public interface EventoRepository extends JpaRepository<Evento, Integer> {

    List<Evento> findByUsuario_IdUsuario(Integer idUsuario);
}
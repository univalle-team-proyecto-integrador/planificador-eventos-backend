package uv.isj.planificadoreventosbackend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import uv.isj.planificadoreventosbackend.model.EstadoSubtarea;
import uv.isj.planificadoreventosbackend.model.Subtarea;

public interface SubtareaRepository extends JpaRepository<Subtarea, Integer> {

    List<Subtarea> findByEvento_IdEvento(Integer idEvento);

    List<Subtarea> findByEstado(EstadoSubtarea estado);
}
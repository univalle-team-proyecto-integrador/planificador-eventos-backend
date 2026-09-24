package uv.isj.planificadoreventosbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uv.isj.planificadoreventosbackend.model.TipoEvento;

public interface TipoEventoRepository extends JpaRepository<TipoEvento, Integer> {
}

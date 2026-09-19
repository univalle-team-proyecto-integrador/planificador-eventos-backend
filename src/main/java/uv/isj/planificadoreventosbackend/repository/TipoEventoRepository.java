package uv.isj.planificadoreventosbackend.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import uv.isj.planificadoreventosbackend.model.TipoEvento;

public interface TipoEventoRepository extends JpaRepository<TipoEvento, Integer> {

    Optional<TipoEvento> findByNombre(String nombre);
}
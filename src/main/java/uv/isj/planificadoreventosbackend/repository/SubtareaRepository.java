package uv.isj.planificadoreventosbackend.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uv.isj.planificadoreventosbackend.model.EstadoSubtarea;
import uv.isj.planificadoreventosbackend.model.Subtarea;

public interface SubtareaRepository extends JpaRepository<Subtarea, Integer> {

    @Query("SELECT s FROM Subtarea s WHERE s.evento.idEvento = :eventoId")
    List<Subtarea> findByEventoId(@Param("eventoId") Integer eventoId);

    @Query("""
            SELECT s
            FROM Subtarea s
            WHERE s.fechaObjetivo = :fecha
              AND s.estado <> :estado
            ORDER BY s.fechaObjetivo ASC, s.horasEstimadas ASC
            """)
    List<Subtarea> findNoEjecutadasParaHoy(
            @Param("fecha") LocalDate fecha,
            @Param("estado") EstadoSubtarea estado);

    @Query("""
            SELECT COALESCE(SUM(s.horasEstimadas), 0)
            FROM Subtarea s
            WHERE s.fechaObjetivo = :fecha
              AND s.estado <> :estado
            """)
    long sumarHorasNoEjecutadasPorFecha(
            @Param("fecha") LocalDate fecha,
            @Param("estado") EstadoSubtarea estado);
}

package uv.isj.planificadoreventosbackend.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import uv.isj.planificadoreventosbackend.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);
}
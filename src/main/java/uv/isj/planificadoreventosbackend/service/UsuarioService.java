package uv.isj.planificadoreventosbackend.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uv.isj.planificadoreventosbackend.exception.RecursoNoEncontradoException;
import uv.isj.planificadoreventosbackend.model.Usuario;
import uv.isj.planificadoreventosbackend.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private static final String PASSWORD_PLACEHOLDER = "cambiar-contrasena";

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<Usuario> findAll() {
        return usuarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> findById(Integer idUsuario) {
        return usuarioRepository.findById(idUsuario);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> findByEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    @Transactional
    public Usuario save(Usuario usuario) {
        if (usuario.getIdUsuario() != null) {
            usuarioRepository.findById(usuario.getIdUsuario())
                    .orElseThrow(RecursoNoEncontradoException::new);
        }
        if (usuario.getPasswordHash() == null || usuario.getPasswordHash().isBlank()) {
            usuario.setPasswordHash(PASSWORD_PLACEHOLDER);
        }
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void delete(Integer idUsuario) {
        usuarioRepository.findById(idUsuario)
                .orElseThrow(RecursoNoEncontradoException::new);
        usuarioRepository.deleteById(idUsuario);
    }
}
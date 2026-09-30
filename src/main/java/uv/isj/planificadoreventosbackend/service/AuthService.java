package uv.isj.planificadoreventosbackend.service;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uv.isj.planificadoreventosbackend.exception.CredencialesInvalidasException;
import uv.isj.planificadoreventosbackend.exception.EmailDuplicadoException;
import uv.isj.planificadoreventosbackend.exception.RecursoNoEncontradoException;
import uv.isj.planificadoreventosbackend.model.Usuario;
import uv.isj.planificadoreventosbackend.model.dto.AuthResponseDTO;
import uv.isj.planificadoreventosbackend.model.dto.LoginRequestDTO;
import uv.isj.planificadoreventosbackend.model.dto.RegistroRequestDTO;
import uv.isj.planificadoreventosbackend.model.dto.UsuarioDTO;
import uv.isj.planificadoreventosbackend.repository.UsuarioRepository;
import uv.isj.planificadoreventosbackend.security.JwtService;

/** Alta de usuarios, verificación de credenciales y emisión del token. */
@Service
public class AuthService {

    private static final int LIMITE_HORAS_POR_DEFECTO = 6;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponseDTO registrar(RegistroRequestDTO dto) {
        String email = normalizar(dto.email());
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new EmailDuplicadoException("El correo ya está registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setNombre(dto.nombre().trim());
        usuario.setPasswordHash(passwordEncoder.encode(dto.password()));
        usuario.setLimiteHorasDiarias(
                dto.limiteHorasDiarias() == null ? LIMITE_HORAS_POR_DEFECTO : dto.limiteHorasDiarias());

        Usuario guardado;
        try {
            guardado = usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException excepcion) {
            // Carrera entre dos altas con el mismo correo: la restricción
            // UNIQUE de la tabla es la autoridad final.
            throw new EmailDuplicadoException("El correo ya está registrado");
        }

        return aRespuesta(guardado);
    }

    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO dto) {
        String email = normalizar(dto.email());
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new CredencialesInvalidasException("Correo o contraseña incorrectos"));

        if (!passwordEncoder.matches(dto.password(), usuario.getPasswordHash())) {
            // Mismo mensaje que el correo inexistente: no revelamos qué correos
            // están dados de alta.
            throw new CredencialesInvalidasException("Correo o contraseña incorrectos");
        }

        return aRespuesta(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioDTO perfil(Integer idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .map(AuthService::aUsuarioDto)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el usuario con id " + idUsuario));
    }

    private AuthResponseDTO aRespuesta(Usuario usuario) {
        String token = jwtService.generarToken(usuario.getIdUsuario(), usuario.getEmail());
        return new AuthResponseDTO(
                token,
                "Bearer",
                jwtService.getExpirationSegundos(),
                aUsuarioDto(usuario));
    }

    private static UsuarioDTO aUsuarioDto(Usuario usuario) {
        return new UsuarioDTO(
                usuario.getIdUsuario(),
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getLimiteHorasDiarias());
    }

    private static String normalizar(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

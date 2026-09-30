package uv.isj.planificadoreventosbackend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.Repository;

/**
 * Espejo de {@link SubtareaRepositoryAislamientoTest} para eventos. Sin este
 * candado, {@code findAll()} o {@code findById(id)} volvían a aparecer y
 * cualquier endpoint podía leer o borrar la cuenta ajena.
 */
class EventoRepositoryAislamientoTest {

    private static final List<String> METODOS_PROHIBIDOS = List.of(
            "findAll",
            "findAllById",
            "count",
            "existsById",
            "deleteById",
            "deleteAll");

    @Test
    @DisplayName("El repositorio no expone findAll ni el resto de métodos genéricos")
    void noExponeConsultasGlobales() {
        Set<String> declarados = Arrays.stream(EventoRepository.class.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertThat(declarados).doesNotContainAnyElementsOf(METODOS_PROHIBIDOS);
    }

    @Test
    @DisplayName("findById sin propietario no existe: solo la variante acotada")
    void noExponeFindByIdSinPropietario() {
        List<Method> metodos = Arrays.stream(EventoRepository.class.getDeclaredMethods())
                .filter(metodo -> metodo.getName().startsWith("findById"))
                .toList();

        assertThat(metodos)
                .isNotEmpty()
                .allSatisfy(metodo -> assertThat(metodo.getName()).isEqualTo("findByIdYUsuarioId"))
                .allSatisfy(metodo -> assertThat(metodo.getParameterCount()).isEqualTo(2));
    }

    @Test
    @DisplayName("No hereda de JpaRepository ni de CrudRepository")
    void noExtiendeLasInterfacesQueAportanFindAll() {
        assertThat(JpaRepository.class.isAssignableFrom(EventoRepository.class)).isFalse();
        assertThat(CrudRepository.class.isAssignableFrom(EventoRepository.class)).isFalse();
        assertThat(Repository.class.isAssignableFrom(EventoRepository.class)).isTrue();
    }

    @Test
    @DisplayName("Toda lectura declarada exige el idUsuario del propietario")
    void todaLecturaExigePropietario() {
        List<Method> lecturas = Arrays.stream(EventoRepository.class.getDeclaredMethods())
                .filter(metodo -> metodo.getName().startsWith("find"))
                .toList();

        assertThat(lecturas).isNotEmpty();
        for (Method lectura : lecturas) {
            List<String> nombres = Arrays.stream(lectura.getParameters())
                    .map(parametro -> parametro.getName())
                    .toList();
            assertThat(nombres)
                    .as("El método %s debe recibir el propietario", lectura.getName())
                    .contains("usuarioId");
        }
    }
}

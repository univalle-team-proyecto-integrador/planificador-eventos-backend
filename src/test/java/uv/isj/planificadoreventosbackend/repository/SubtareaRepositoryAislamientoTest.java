package uv.isj.planificadoreventosbackend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.Repository;

/**
 * El criterio de US-11 pide que el repositorio no exponga un {@code findAll()}
 * genérico. Comprobarlo por reflexión, y no por convención, es lo que sostiene
 * la garantía: si alguien vuelve a extender JpaRepository o CrudRepository,
 * estos métodos heredados reaparecen y la prueba falla.
 */
class SubtareaRepositoryAislamientoTest {

    private static final List<String> METODOS_PROHIBIDOS = List.of(
            "findAll",
            "findAllById",
            "findAllId",
            "findAllBy",
            "count",
            "existsById",
            "deleteById",
            "deleteAll");

    @Test
    @DisplayName("El repositorio no expone findAll ni el resto de métodos genéricos")
    void noExponeConsultasGlobales() {
        Set<String> declarados = Arrays.stream(SubtareaRepository.class.getDeclaredMethods())
                .map(Method::getName)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(declarados).doesNotContainAnyElementsOf(METODOS_PROHIBIDOS);
    }

    @Test
    @DisplayName("findById sin propietario no existe: solo la variante acotada")
    void noExponeFindByIdSinPropietario() {
        List<Method> metodos = Arrays.stream(SubtareaRepository.class.getDeclaredMethods())
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
        assertThat(JpaRepository.class.isAssignableFrom(SubtareaRepository.class)).isFalse();
        assertThat(CrudRepository.class.isAssignableFrom(SubtareaRepository.class)).isFalse();
        assertThat(Repository.class.isAssignableFrom(SubtareaRepository.class)).isTrue();
    }

    @Test
    @DisplayName("Toda lectura declarada exige el idUsuario del propietario")
    void todaLecturaExigePropietario() {
        List<Method> lecturas = Arrays.stream(SubtareaRepository.class.getDeclaredMethods())
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

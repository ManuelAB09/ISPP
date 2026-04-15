package es.us.meerkat.backend.service.recommendations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.us.meerkat.backend.entity.events.Evento;
import es.us.meerkat.backend.entity.recommendations.Valoracion;
import es.us.meerkat.backend.entity.tutors.Tutor;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.recommendations.ValoracionRepository;
import es.us.meerkat.backend.repository.tutors.TutorRepository;

@ExtendWith(MockitoExtension.class)
class ValoracionServiceTest {

    @Mock private ValoracionRepository valoracionRepository;
    @Mock private TutorRepository tutorRepository;

    @InjectMocks private ValoracionService valoracionService;

    // ================================================================
    // guardarValoracion
    // ================================================================

    @Test
    void guardarValoracionShouldSaveWhenValid() {
        Usuario alumno =
                Usuario.builder().id(1L).nombre("Al").email("a@t.com").password("p").build();
        Usuario tutorUser =
                Usuario.builder().id(2L).nombre("Tu").email("t@t.com").password("p").build();
        Tutor tutor = new Tutor();
        tutor.setId(10L);
        tutor.setUsuario(tutorUser);
        Evento evento = new Evento();
        evento.setId(100L);

        Valoracion valoracion = new Valoracion();
        valoracion.setProfesor(tutor);
        valoracion.setAlumno(alumno);
        valoracion.setEvento(evento);
        valoracion.setPuntuacion(5);

        when(tutorRepository.findById(10L)).thenReturn(Optional.of(tutor));
        when(valoracionRepository.existsByAlumnoIdAndEventoId(1L, 100L)).thenReturn(false);
        when(valoracionRepository.save(any(Valoracion.class)))
                .thenAnswer(
                        inv -> {
                            Valoracion v = inv.getArgument(0);
                            v.setId(1L);
                            return v;
                        });

        Valoracion result = valoracionService.guardarValoracion(valoracion);

        assertThat(result.getId()).isEqualTo(1L);
        verify(valoracionRepository).save(valoracion);
    }

    @Test
    void guardarValoracionShouldThrowWhenSelfRating() {
        Usuario user = Usuario.builder().id(1L).nombre("U").email("u@t.com").password("p").build();
        Tutor tutor = new Tutor();
        tutor.setId(10L);
        tutor.setUsuario(user);

        Valoracion valoracion = new Valoracion();
        valoracion.setProfesor(tutor);
        valoracion.setAlumno(user);

        when(tutorRepository.findById(10L)).thenReturn(Optional.of(tutor));

        assertThatThrownBy(() -> valoracionService.guardarValoracion(valoracion))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No puedes valorarte a ti mismo");
    }

    @Test
    void guardarValoracionShouldThrowWhenTutorNotFound() {
        Usuario alumno =
                Usuario.builder().id(1L).nombre("Al").email("a@t.com").password("p").build();
        Tutor tutor = new Tutor();
        tutor.setId(99L);

        Valoracion valoracion = new Valoracion();
        valoracion.setProfesor(tutor);
        valoracion.setAlumno(alumno);

        when(tutorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> valoracionService.guardarValoracion(valoracion))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tutor no encontrado");
    }

    @Test
    void guardarValoracionShouldThrowWhenAlreadyRated() {
        Usuario alumno =
                Usuario.builder().id(1L).nombre("Al").email("a@t.com").password("p").build();
        Usuario tutorUser =
                Usuario.builder().id(2L).nombre("Tu").email("t@t.com").password("p").build();
        Tutor tutor = new Tutor();
        tutor.setId(10L);
        tutor.setUsuario(tutorUser);
        Evento evento = new Evento();
        evento.setId(100L);

        Valoracion valoracion = new Valoracion();
        valoracion.setProfesor(tutor);
        valoracion.setAlumno(alumno);
        valoracion.setEvento(evento);

        when(tutorRepository.findById(10L)).thenReturn(Optional.of(tutor));
        when(valoracionRepository.existsByAlumnoIdAndEventoId(1L, 100L)).thenReturn(true);

        assertThatThrownBy(() -> valoracionService.guardarValoracion(valoracion))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya has valorado este evento");
    }

    @Test
    void guardarValoracionShouldSkipSelfCheckWhenProfesorIsNull() {
        Valoracion valoracion = new Valoracion();
        valoracion.setProfesor(null);
        valoracion.setAlumno(null);
        valoracion.setPuntuacion(3);

        when(valoracionRepository.save(any(Valoracion.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Valoracion result = valoracionService.guardarValoracion(valoracion);

        assertThat(result.getPuntuacion()).isEqualTo(3);
    }

    @Test
    void guardarValoracionShouldSkipDuplicateCheckWhenEventoIsNull() {
        Usuario alumno =
                Usuario.builder().id(1L).nombre("Al").email("a@t.com").password("p").build();
        Usuario tutorUser =
                Usuario.builder().id(2L).nombre("Tu").email("t@t.com").password("p").build();
        Tutor tutor = new Tutor();
        tutor.setId(10L);
        tutor.setUsuario(tutorUser);

        Valoracion valoracion = new Valoracion();
        valoracion.setProfesor(tutor);
        valoracion.setAlumno(alumno);
        valoracion.setEvento(null);
        valoracion.setPuntuacion(4);

        when(tutorRepository.findById(10L)).thenReturn(Optional.of(tutor));
        when(valoracionRepository.save(any(Valoracion.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Valoracion result = valoracionService.guardarValoracion(valoracion);

        assertThat(result.getPuntuacion()).isEqualTo(4);
    }

    // ================================================================
    // isAlreadyRated
    // ================================================================

    @Test
    void isAlreadyRatedShouldReturnTrueWhenExists() {
        when(valoracionRepository.existsByAlumnoIdAndEventoId(1L, 100L)).thenReturn(true);

        assertThat(valoracionService.isAlreadyRated(1L, 100L)).isTrue();
    }

    @Test
    void isAlreadyRatedShouldReturnFalseWhenNotExists() {
        when(valoracionRepository.existsByAlumnoIdAndEventoId(1L, 100L)).thenReturn(false);

        assertThat(valoracionService.isAlreadyRated(1L, 100L)).isFalse();
    }

    // ================================================================
    // obtenerValoracionesPorProfesor
    // ================================================================

    @Test
    void obtenerValoracionesPorProfesorShouldReturnList() {
        Valoracion v = new Valoracion();
        v.setId(1L);
        v.setPuntuacion(5);
        when(valoracionRepository.findByProfesorId(10L)).thenReturn(List.of(v));

        List<Valoracion> result = valoracionService.obtenerValoracionesPorProfesor(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPuntuacion()).isEqualTo(5);
    }

    // ================================================================
    // obtenerMediaPorProfesor
    // ================================================================

    @Test
    void obtenerMediaPorProfesorShouldReturnAverage() {
        when(valoracionRepository.findMediaByProfesorId(10L)).thenReturn(4.5);

        assertThat(valoracionService.obtenerMediaPorProfesor(10L)).isEqualTo(4.5);
    }

    @Test
    void obtenerMediaPorProfesorShouldReturnNullWhenNoRatings() {
        when(valoracionRepository.findMediaByProfesorId(10L)).thenReturn(null);

        assertThat(valoracionService.obtenerMediaPorProfesor(10L)).isNull();
    }

    // ================================================================
    // contarValoracionesPorProfesor
    // ================================================================

    @Test
    void contarValoracionesPorProfesorShouldReturnCount() {
        when(valoracionRepository.countByProfesorId(10L)).thenReturn(25L);

        assertThat(valoracionService.contarValoracionesPorProfesor(10L)).isEqualTo(25L);
    }

    // ================================================================
    // calcularNivel
    // ================================================================

    @Test
    void calcularNivelShouldReturnPrincipianteWhenNullValues() {
        when(valoracionRepository.countByProfesorId(10L)).thenReturn(null);
        when(valoracionRepository.findMediaByProfesorId(10L)).thenReturn(null);

        assertThat(valoracionService.calcularNivel(10L)).isEqualTo("principiante");
    }

    @Test
    void calcularNivelShouldReturnPrincipianteWhenFewRatings() {
        when(valoracionRepository.countByProfesorId(10L)).thenReturn(5L);
        when(valoracionRepository.findMediaByProfesorId(10L)).thenReturn(4.0);

        assertThat(valoracionService.calcularNivel(10L)).isEqualTo("principiante");
    }

    @Test
    void calcularNivelShouldReturnPrincipianteWhenLowAverage() {
        when(valoracionRepository.countByProfesorId(10L)).thenReturn(20L);
        when(valoracionRepository.findMediaByProfesorId(10L)).thenReturn(2.5);

        assertThat(valoracionService.calcularNivel(10L)).isEqualTo("principiante");
    }

    @Test
    void calcularNivelShouldReturnAvanzadoWhenMediumRatingsAndGoodAverage() {
        when(valoracionRepository.countByProfesorId(10L)).thenReturn(30L);
        when(valoracionRepository.findMediaByProfesorId(10L)).thenReturn(3.5);

        assertThat(valoracionService.calcularNivel(10L)).isEqualTo("avanzado");
    }

    @Test
    void calcularNivelShouldReturnAvanzadoAtBoundary() {
        when(valoracionRepository.countByProfesorId(10L)).thenReturn(10L);
        when(valoracionRepository.findMediaByProfesorId(10L)).thenReturn(3.0);

        assertThat(valoracionService.calcularNivel(10L)).isEqualTo("avanzado");
    }

    @Test
    void calcularNivelShouldReturnExpertoWhenManyHighRatings() {
        when(valoracionRepository.countByProfesorId(10L)).thenReturn(60L);
        when(valoracionRepository.findMediaByProfesorId(10L)).thenReturn(4.8);

        assertThat(valoracionService.calcularNivel(10L)).isEqualTo("experto");
    }

    @Test
    void calcularNivelShouldReturnPrincipianteWhenManyButNotHighEnough() {
        when(valoracionRepository.countByProfesorId(10L)).thenReturn(60L);
        when(valoracionRepository.findMediaByProfesorId(10L)).thenReturn(4.0);

        assertThat(valoracionService.calcularNivel(10L)).isEqualTo("principiante");
    }

    @Test
    void calcularNivelShouldReturnAvanzadoWhenExactly50AndMedia3() {
        when(valoracionRepository.countByProfesorId(10L)).thenReturn(50L);
        when(valoracionRepository.findMediaByProfesorId(10L)).thenReturn(3.0);

        assertThat(valoracionService.calcularNivel(10L)).isEqualTo("avanzado");
    }

    @Test
    void calcularNivelShouldReturnExpertoAtBoundary() {
        when(valoracionRepository.countByProfesorId(10L)).thenReturn(51L);
        when(valoracionRepository.findMediaByProfesorId(10L)).thenReturn(4.5);

        assertThat(valoracionService.calcularNivel(10L)).isEqualTo("experto");
    }
}

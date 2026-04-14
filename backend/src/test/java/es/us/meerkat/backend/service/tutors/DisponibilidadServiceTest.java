package es.us.meerkat.backend.service.tutors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.us.meerkat.backend.dto.tutors.CreateDisponibilidadRequest;
import es.us.meerkat.backend.dto.tutors.DisponibilidadTutorResponse;
import es.us.meerkat.backend.entity.tutors.DisponibilidadTutor;
import es.us.meerkat.backend.entity.tutors.Tutor;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.tutors.DisponibilidadTutorRepository;
import es.us.meerkat.backend.repository.tutors.TutorRepository;

@ExtendWith(MockitoExtension.class)
class DisponibilidadServiceTest {

    @Mock private DisponibilidadTutorRepository disponibilidadRepository;

    @Mock private TutorRepository tutorRepository;

    @InjectMocks private DisponibilidadService disponibilidadService;

    @Test
    void crearDisponibilidadShouldThrowWhenTutorNotFound() {
        CreateDisponibilidadRequest request = new CreateDisponibilidadRequest();
        request.setEsRecurrente(false);
        request.setHoraInicio(LocalTime.of(10, 0));
        request.setHoraFin(LocalTime.of(11, 0));

        when(tutorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> disponibilidadService.crearDisponibilidad(1L, request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tutor no encontrado");
    }

    @Test
    void crearDisponibilidadShouldThrowWhenUserIsNotTutorOwner() {
        CreateDisponibilidadRequest request = new CreateDisponibilidadRequest();
        request.setEsRecurrente(false);
        request.setFechaPuntual(LocalDateTime.of(2025, 3, 15, 10, 30));
        request.setHoraInicio(LocalTime.of(10, 0));
        request.setHoraFin(LocalTime.of(11, 0));

        Usuario tutorOwner = new Usuario();
        tutorOwner.setId(99L);
        tutorOwner.setEmail("tutor@meerkat.es");

        Tutor tutor = new Tutor();
        tutor.setId(1L);
        tutor.setUsuario(tutorOwner);

        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutor));

        assertThatThrownBy(() -> disponibilidadService.crearDisponibilidad(1L, request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No tienes permisos para modificar disponibilidades de otro tutor");
    }

    @Test
    void crearDisponibilidadShouldThrowWhenStartTimeAfterEndTime() {
        CreateDisponibilidadRequest request = new CreateDisponibilidadRequest();
        request.setEsRecurrente(false);
        request.setFechaPuntual(LocalDateTime.of(2025, 3, 15, 12, 0));
        request.setHoraInicio(LocalTime.of(14, 0));
        request.setHoraFin(LocalTime.of(11, 0));

        Usuario tutorOwner = new Usuario();
        tutorOwner.setId(1L);
        tutorOwner.setEmail("tutor@meerkat.es");

        Tutor tutor = new Tutor();
        tutor.setId(1L);
        tutor.setUsuario(tutorOwner);

        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutor));

        assertThatThrownBy(() -> disponibilidadService.crearDisponibilidad(1L, request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La hora de inicio debe ser anterior a la de fin");
    }

    @Test
    void crearDisponibilidadShouldThrowWhenRecurrentButNoDayOfWeek() {
        CreateDisponibilidadRequest request = new CreateDisponibilidadRequest();
        request.setEsRecurrente(true);
        request.setDiaSemana(null);
        request.setHoraInicio(LocalTime.of(10, 0));
        request.setHoraFin(LocalTime.of(11, 0));

        Usuario tutorOwner = new Usuario();
        tutorOwner.setId(1L);
        tutorOwner.setEmail("tutor@meerkat.es");

        Tutor tutor = new Tutor();
        tutor.setId(1L);
        tutor.setUsuario(tutorOwner);

        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutor));

        assertThatThrownBy(() -> disponibilidadService.crearDisponibilidad(1L, request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Día de semana requerido para disponibilidad recurrente");
    }

    @Test
    void crearDisponibilidadShouldSuccessfullyCreateNonRecurrentDisponibilidad() {
        CreateDisponibilidadRequest request = new CreateDisponibilidadRequest();
        request.setEsRecurrente(false);
        request.setFechaPuntual(LocalDateTime.of(2025, 3, 15, 10, 30));
        request.setHoraInicio(LocalTime.of(10, 0));
        request.setHoraFin(LocalTime.of(11, 0));
        request.setModalidad("PRESENCIAL");
        request.setUbicacionPresencial("Sevilla");

        Usuario tutorOwner = new Usuario();
        tutorOwner.setId(1L);
        tutorOwner.setEmail("tutor@meerkat.es");

        Tutor tutor = new Tutor();
        tutor.setId(1L);
        tutor.setUsuario(tutorOwner);

        DisponibilidadTutor disponibilidad =
                DisponibilidadTutor.builder()
                        .id(1L)
                        .tutor(tutor)
                        .esRecurrente(false)
                        .fechaPuntual(LocalDateTime.of(2025, 3, 15, 10, 30))
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .modalidad("PRESENCIAL")
                        .ubicacionPresencial("Sevilla")
                        .activa(true)
                        .build();

        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutor));
        when(disponibilidadRepository.save(any(DisponibilidadTutor.class)))
                .thenReturn(disponibilidad);

        DisponibilidadTutorResponse response =
                disponibilidadService.crearDisponibilidad(1L, request, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        verify(disponibilidadRepository).save(any(DisponibilidadTutor.class));
    }

    @Test
    void crearDisponibilidadShouldSuccessfullyCreateRecurrentDisponibilidad() {
        CreateDisponibilidadRequest request = new CreateDisponibilidadRequest();
        request.setEsRecurrente(true);
        request.setDiaSemana(DayOfWeek.MONDAY);
        request.setHoraInicio(LocalTime.of(10, 0));
        request.setHoraFin(LocalTime.of(11, 0));
        request.setModalidad("VIRTUAL");

        Usuario tutorOwner = new Usuario();
        tutorOwner.setId(1L);
        tutorOwner.setEmail("tutor@meerkat.es");

        Tutor tutor = new Tutor();
        tutor.setId(1L);
        tutor.setUsuario(tutorOwner);

        DisponibilidadTutor disponibilidad =
                DisponibilidadTutor.builder()
                        .id(2L)
                        .tutor(tutor)
                        .esRecurrente(true)
                        .diaSemana(DayOfWeek.MONDAY)
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .modalidad("VIRTUAL")
                        .activa(true)
                        .build();

        when(tutorRepository.findById(1L)).thenReturn(Optional.of(tutor));
        when(disponibilidadRepository.save(any(DisponibilidadTutor.class)))
                .thenReturn(disponibilidad);

        DisponibilidadTutorResponse response =
                disponibilidadService.crearDisponibilidad(1L, request, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(2L);
        verify(disponibilidadRepository).save(any(DisponibilidadTutor.class));
    }

    @Test
    void getDisponibilidadesShouldReturnAllActiveDisponibilidades() {
        Usuario tutorOwner = new Usuario();
        tutorOwner.setId(1L);

        Tutor tutor = new Tutor();
        tutor.setId(1L);
        tutor.setUsuario(tutorOwner);

        DisponibilidadTutor disp1 =
                DisponibilidadTutor.builder()
                        .id(1L)
                        .tutor(tutor)
                        .esRecurrente(true)
                        .diaSemana(DayOfWeek.MONDAY)
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .activa(true)
                        .build();

        DisponibilidadTutor disp2 =
                DisponibilidadTutor.builder()
                        .id(2L)
                        .tutor(tutor)
                        .esRecurrente(true)
                        .diaSemana(DayOfWeek.TUESDAY)
                        .horaInicio(LocalTime.of(14, 0))
                        .horaFin(LocalTime.of(15, 0))
                        .activa(true)
                        .build();

        when(disponibilidadRepository.findByTutorIdAndActivaTrue(1L))
                .thenReturn(List.of(disp1, disp2));

        List<DisponibilidadTutorResponse> responses = disponibilidadService.getDisponibilidades(1L);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo(1L);
        assertThat(responses.get(1).getId()).isEqualTo(2L);
    }

    // ── getDisponibilidadesPorFecha ────────────────────────────────────

    @Test
    void getDisponibilidadesPorFechaShouldReturnMatchingDisponibilidades() {
        Usuario owner = new Usuario();
        owner.setId(1L);
        Tutor tutor = new Tutor();
        tutor.setId(1L);
        tutor.setUsuario(owner);

        DisponibilidadTutor disp =
                DisponibilidadTutor.builder()
                        .id(1L)
                        .tutor(tutor)
                        .esRecurrente(true)
                        .diaSemana(DayOfWeek.MONDAY)
                        .horaInicio(LocalTime.of(9, 0))
                        .horaFin(LocalTime.of(10, 0))
                        .activa(true)
                        .build();

        LocalDate monday = LocalDate.of(2027, 6, 14); // a Monday
        when(disponibilidadRepository.findDisponibilidadesPara(1L, DayOfWeek.MONDAY, monday))
                .thenReturn(List.of(disp));

        List<DisponibilidadTutorResponse> result =
                disponibilidadService.getDisponibilidadesPorFecha(1L, monday);

        assertThat(result).hasSize(1);
    }

    @Test
    void getDisponibilidadesPorFechaShouldReturnEmptyWhenNoMatch() {
        LocalDate friday = LocalDate.of(2027, 6, 18);
        when(disponibilidadRepository.findDisponibilidadesPara(1L, DayOfWeek.FRIDAY, friday))
                .thenReturn(Collections.emptyList());

        List<DisponibilidadTutorResponse> result =
                disponibilidadService.getDisponibilidadesPorFecha(1L, friday);

        assertThat(result).isEmpty();
    }

    // ── desactivarDisponibilidad ───────────────────────────────────────

    @Test
    void desactivarDisponibilidadShouldSetActivaFalse() {
        Usuario owner = new Usuario();
        owner.setId(1L);
        Tutor tutor = new Tutor();
        tutor.setId(1L);
        tutor.setUsuario(owner);

        DisponibilidadTutor disp =
                DisponibilidadTutor.builder().id(5L).tutor(tutor).activa(true).build();

        when(disponibilidadRepository.findById(5L)).thenReturn(Optional.of(disp));

        disponibilidadService.desactivarDisponibilidad(5L, 1L);

        assertThat(disp.getActiva()).isFalse();
        verify(disponibilidadRepository).save(disp);
    }

    @Test
    void desactivarDisponibilidadShouldThrowWhenNotFound() {
        when(disponibilidadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> disponibilidadService.desactivarDisponibilidad(99L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Disponibilidad no encontrada");
    }

    @Test
    void desactivarDisponibilidadShouldThrowWhenNotOwner() {
        Usuario owner = new Usuario();
        owner.setId(2L);
        Tutor tutor = new Tutor();
        tutor.setId(1L);
        tutor.setUsuario(owner);

        DisponibilidadTutor disp =
                DisponibilidadTutor.builder().id(5L).tutor(tutor).activa(true).build();

        when(disponibilidadRepository.findById(5L)).thenReturn(Optional.of(disp));

        assertThatThrownBy(() -> disponibilidadService.desactivarDisponibilidad(5L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No tienes permisos para modificar esta disponibilidad");
    }

    // ── actualizarDisponibilidad ───────────────────────────────────────

    @Test
    void actualizarDisponibilidadShouldUpdateFieldsAndSave() {
        Usuario owner = new Usuario();
        owner.setId(1L);
        Tutor tutor = new Tutor();
        tutor.setId(1L);
        tutor.setUsuario(owner);

        DisponibilidadTutor disp =
                DisponibilidadTutor.builder()
                        .id(5L)
                        .tutor(tutor)
                        .esRecurrente(true)
                        .diaSemana(DayOfWeek.MONDAY)
                        .horaInicio(LocalTime.of(9, 0))
                        .horaFin(LocalTime.of(10, 0))
                        .activa(true)
                        .build();

        when(disponibilidadRepository.findById(5L)).thenReturn(Optional.of(disp));
        when(disponibilidadRepository.findByTutorIdAndActivaTrue(1L)).thenReturn(List.of(disp));
        when(disponibilidadRepository.save(any(DisponibilidadTutor.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CreateDisponibilidadRequest request = new CreateDisponibilidadRequest();
        request.setEsRecurrente(true);
        request.setDiaSemana(DayOfWeek.TUESDAY);
        request.setHoraInicio(LocalTime.of(14, 0));
        request.setHoraFin(LocalTime.of(16, 0));
        request.setModalidad("PRESENCIAL");
        request.setUbicacionPresencial("Aula 1");

        DisponibilidadTutorResponse result =
                disponibilidadService.actualizarDisponibilidad(5L, request, 1L);

        assertThat(result).isNotNull();
        verify(disponibilidadRepository).save(any(DisponibilidadTutor.class));
    }

    @Test
    void actualizarDisponibilidadShouldThrowWhenNotOwner() {
        Usuario owner = new Usuario();
        owner.setId(2L);
        Tutor tutor = new Tutor();
        tutor.setId(1L);
        tutor.setUsuario(owner);

        DisponibilidadTutor disp = DisponibilidadTutor.builder().id(5L).tutor(tutor).build();

        when(disponibilidadRepository.findById(5L)).thenReturn(Optional.of(disp));

        CreateDisponibilidadRequest request = new CreateDisponibilidadRequest();
        request.setEsRecurrente(false);
        request.setFechaPuntual(LocalDateTime.of(2027, 6, 15, 10, 0));
        request.setHoraInicio(LocalTime.of(10, 0));
        request.setHoraFin(LocalTime.of(11, 0));

        assertThatThrownBy(() -> disponibilidadService.actualizarDisponibilidad(5L, request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No tienes permisos para modificar esta disponibilidad");
    }

    // ── eliminarTodasDisponibilidades ──────────────────────────────────

    @Test
    void eliminarTodasDisponibilidadesShouldDeleteAllByTutorId() {
        disponibilidadService.eliminarTodasDisponibilidades(1L);

        verify(disponibilidadRepository).deleteAllByTutorId(1L);
    }
}

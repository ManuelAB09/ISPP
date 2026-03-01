package es.us.meerkat.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import es.us.meerkat.backend.dto.TutorProfileRequest;
import es.us.meerkat.backend.dto.TutorProfileResponse;
import es.us.meerkat.backend.entity.EstadoTransaccion;
import es.us.meerkat.backend.entity.TipoTransaccion;
import es.us.meerkat.backend.entity.TransaccionPago;
import es.us.meerkat.backend.entity.Tutor;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.TransaccionPagoRepository;
import es.us.meerkat.backend.repository.TutorRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class TutorServiceTest {

    @Mock private TutorRepository tutorRepository;

    @Mock private UsuarioRepository usuarioRepository;

    @Mock private TransaccionPagoRepository transaccionPagoRepository;

    @InjectMocks private TutorService tutorService;

    @Test
    void crearPerfilShouldSaveTutorWhenUserIsTeacherAndHasNoProfile() {
        Long usuarioId = 1L;
        Usuario usuario = buildUsuario(usuarioId, true);
        TutorProfileRequest request = buildTutorRequest();

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByUs(usuario)).thenReturn(Optional.empty());

        TutorProfileResponse response = tutorService.crearPerfil(usuarioId, request);

        ArgumentCaptor<Tutor> captor = ArgumentCaptor.forClass(Tutor.class);
        verify(tutorRepository).save(captor.capture());
        Tutor savedTutor = captor.getValue();
        assertThat(savedTutor.getUs()).isEqualTo(usuario);
        assertThat(savedTutor.getEspecialidades()).containsExactly("Matemáticas", "Física");
        assertThat(savedTutor.getTarifaHora()).isEqualByComparingTo("25.00");
        assertThat(savedTutor.getDisponibilidad()).isEqualTo("L-V 16:00-20:00");
        assertThat(savedTutor.getVerificado()).isFalse();
        assertThat(savedTutor.getClassroomConectado()).isFalse();

        assertThat(response.getUserId()).isEqualTo(usuarioId);
        assertThat(response.getTarifaHora()).isEqualByComparingTo("25.00");
        assertThat(response.getEspecialidades()).containsExactly("Matemáticas", "Física");
    }

    @Test
    void crearPerfilShouldFailWhenUserHasNoTeacherRole() {
        Long usuarioId = 1L;
        Usuario usuario = buildUsuario(usuarioId, false);

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> tutorService.crearPerfil(usuarioId, buildTutorRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("El usuario no tiene rol de profesor");

        verify(tutorRepository, never()).save(any(Tutor.class));
    }

    @Test
    void crearPerfilShouldFailWhenProfileAlreadyExists() {
        Long usuarioId = 1L;
        Usuario usuario = buildUsuario(usuarioId, true);

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByUs(usuario)).thenReturn(Optional.of(new Tutor()));

        assertThatThrownBy(() -> tutorService.crearPerfil(usuarioId, buildTutorRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("El perfil ya existe");
    }

    @Test
    void editarPerfilShouldUpdateTutorWhenOwnershipMatches() {
        Long usuarioId = 1L;
        Long tutorId = 10L;
        Usuario usuario = buildUsuario(usuarioId, true);

        Tutor tutor = new Tutor();
        tutor.setId(tutorId);
        tutor.setUs(usuario);
        tutor.setCreatedAt(LocalDateTime.now());

        TutorProfileRequest request = buildTutorRequest();

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByIdAndUsId(tutorId, usuarioId)).thenReturn(Optional.of(tutor));

        TutorProfileResponse response = tutorService.editarPerfil(usuarioId, tutorId, request);

        verify(tutorRepository).save(tutor);
        assertThat(tutor.getEspecialidades()).containsExactly("Matemáticas", "Física");
        assertThat(tutor.getTarifaHora()).isEqualByComparingTo("25.00");
        assertThat(tutor.getDisponibilidad()).isEqualTo("L-V 16:00-20:00");
        assertThat(response.getId()).isEqualTo(tutorId);
    }

    @Test
    void obtenerTutoresVerificadosShouldUseSimpleQueryWithoutFilters() {
        Tutor tutor = buildTutor(7L, buildUsuario(1L, true), true);
        Page<Tutor> page = new PageImpl<>(List.of(tutor));

        when(tutorRepository.findByVerificadoTrue(PageRequest.of(0, 20))).thenReturn(page);

        Page<TutorProfileResponse> response =
                tutorService.obtenerTutoresVerificados(null, null, null, 0, 20);

        assertThat(response.getTotalElements()).isEqualTo(1);
        verify(tutorRepository).findByVerificadoTrue(PageRequest.of(0, 20));
    }

    @Test
    void obtenerTutoresVerificadosShouldApplyFiltersWhenProvided() {
        Tutor tutor = buildTutor(7L, buildUsuario(1L, true), true);
        Page<Tutor> page = new PageImpl<>(List.of(tutor));

        when(tutorRepository.findVerificadosByEspecialidadAndTarifa(
                        "mates", BigDecimal.TEN, BigDecimal.valueOf(40), PageRequest.of(0, 10)))
                .thenReturn(page);

        Page<TutorProfileResponse> response =
                tutorService.obtenerTutoresVerificados(
                        "mates", BigDecimal.TEN, BigDecimal.valueOf(40), 0, 10);

        assertThat(response.getTotalElements()).isEqualTo(1);
        verify(tutorRepository)
                .findVerificadosByEspecialidadAndTarifa(
                        "mates", BigDecimal.TEN, BigDecimal.valueOf(40), PageRequest.of(0, 10));
    }

    @Test
    void solicitarVerificacionShouldCreatePendingPaymentTransaction() {
        Long usuarioId = 1L;
        Long tutorId = 10L;
        Usuario usuario = buildUsuario(usuarioId, true);
        Tutor tutor = buildTutor(tutorId, usuario, false);

        when(tutorRepository.findByIdAndUsId(tutorId, usuarioId)).thenReturn(Optional.of(tutor));
        when(transaccionPagoRepository.existsByTutorIdAndTipoAndEstado(
                        tutorId, TipoTransaccion.PAGO_VERIFICACION, EstadoTransaccion.PENDIENTE))
                .thenReturn(false);

        tutorService.solicitarVerificacion(usuarioId, tutorId);

        ArgumentCaptor<TransaccionPago> captor = ArgumentCaptor.forClass(TransaccionPago.class);
        verify(transaccionPagoRepository).save(captor.capture());
        TransaccionPago saved = captor.getValue();
        assertThat(saved.getTipo()).isEqualTo(TipoTransaccion.PAGO_VERIFICACION);
        assertThat(saved.getEstado()).isEqualTo(EstadoTransaccion.PENDIENTE);
        assertThat(saved.getMoneda()).isEqualTo("EUR");
        assertThat(saved.getMonto()).isEqualByComparingTo("19.99");
        assertThat(saved.getTutor()).isEqualTo(tutor);
        assertThat(saved.getUsuario()).isEqualTo(usuario);
    }

    @Test
    void solicitarVerificacionShouldFailWhenTutorAlreadyVerified() {
        Long usuarioId = 1L;
        Long tutorId = 10L;
        Tutor tutor = buildTutor(tutorId, buildUsuario(usuarioId, true), true);

        when(tutorRepository.findByIdAndUsId(tutorId, usuarioId)).thenReturn(Optional.of(tutor));

        assertThatThrownBy(() -> tutorService.solicitarVerificacion(usuarioId, tutorId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("El tutor ya está verificado");

        verify(transaccionPagoRepository, never()).save(any(TransaccionPago.class));
    }

    @Test
    void solicitarVerificacionShouldFailWhenThereIsPendingRequest() {
        Long usuarioId = 1L;
        Long tutorId = 10L;
        Tutor tutor = buildTutor(tutorId, buildUsuario(usuarioId, true), false);

        when(tutorRepository.findByIdAndUsId(tutorId, usuarioId)).thenReturn(Optional.of(tutor));
        when(transaccionPagoRepository.existsByTutorIdAndTipoAndEstado(
                        tutorId, TipoTransaccion.PAGO_VERIFICACION, EstadoTransaccion.PENDIENTE))
                .thenReturn(true);

        assertThatThrownBy(() -> tutorService.solicitarVerificacion(usuarioId, tutorId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Ya existe una solicitud pendiente");
    }

    @Test
    void obtenerEstadoVerificacionShouldReturnVerificadoWhenTutorIsVerified() {
        Long usuarioId = 1L;
        Long tutorId = 10L;
        Tutor tutor = buildTutor(tutorId, buildUsuario(usuarioId, true), true);

        when(tutorRepository.findByIdAndUsId(tutorId, usuarioId)).thenReturn(Optional.of(tutor));

        String estado = tutorService.obtenerEstadoVerificacion(usuarioId, tutorId);

        assertThat(estado).isEqualTo("VERIFICADO");
    }

    @Test
    void obtenerEstadoVerificacionShouldReturnLatestTransactionState() {
        Long usuarioId = 1L;
        Long tutorId = 10L;
        Tutor tutor = buildTutor(tutorId, buildUsuario(usuarioId, true), false);

        TransaccionPago tx = new TransaccionPago();
        tx.setEstado(EstadoTransaccion.COMPLETADA);

        when(tutorRepository.findByIdAndUsId(tutorId, usuarioId)).thenReturn(Optional.of(tutor));
        when(transaccionPagoRepository.findTopByTutorIdAndTipoOrderByIniciadoAtDesc(
                        tutorId, TipoTransaccion.PAGO_VERIFICACION))
                .thenReturn(Optional.of(tx));

        String estado = tutorService.obtenerEstadoVerificacion(usuarioId, tutorId);

        assertThat(estado).isEqualTo("COMPLETADA");
    }

    @Test
    void obtenerEstadoVerificacionShouldReturnSinSolicitudWhenNoTransactionsExist() {
        Long usuarioId = 1L;
        Long tutorId = 10L;
        Tutor tutor = buildTutor(tutorId, buildUsuario(usuarioId, true), false);

        when(tutorRepository.findByIdAndUsId(tutorId, usuarioId)).thenReturn(Optional.of(tutor));
        when(transaccionPagoRepository.findTopByTutorIdAndTipoOrderByIniciadoAtDesc(
                        tutorId, TipoTransaccion.PAGO_VERIFICACION))
                .thenReturn(Optional.empty());

        String estado = tutorService.obtenerEstadoVerificacion(usuarioId, tutorId);

        assertThat(estado).isEqualTo("SIN_SOLICITUD");
    }

    private Usuario buildUsuario(final Long id, final boolean esTutor) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre("Profesor " + id);
        usuario.setEmail("profesor" + id + "@meerkat.es");
        usuario.setEsTutor(esTutor);
        return usuario;
    }

    private Tutor buildTutor(final Long tutorId, final Usuario usuario, final boolean verificado) {
        Tutor tutor = new Tutor();
        tutor.setId(tutorId);
        tutor.setUs(usuario);
        tutor.setEspecialidades(List.of("Matemáticas"));
        tutor.setTarifaHora(BigDecimal.valueOf(30));
        tutor.setDisponibilidad("L-V");
        tutor.setBio("Bio tutor");
        tutor.setVerificado(verificado);
        tutor.setClassroomConectado(false);
        tutor.setCreatedAt(LocalDateTime.now());
        return tutor;
    }

    private TutorProfileRequest buildTutorRequest() {
        TutorProfileRequest request = new TutorProfileRequest();
        request.setEspecialidades(List.of("Matemáticas", "Física"));
        request.setTarifaHora(new BigDecimal("25.00"));
        request.setDisponibilidad("L-V 16:00-20:00");
        request.setBio("Profesor con 5 años de experiencia");
        return request;
    }
}

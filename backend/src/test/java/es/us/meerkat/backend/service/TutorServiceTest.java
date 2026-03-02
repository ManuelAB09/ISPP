package es.us.meerkat.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
    void crearPerfilShouldCreateNewTutorProfile() {
        Long usuarioId = 1L;
        Usuario usuario = buildUsuario(usuarioId, true);
        TutorProfileRequest request = new TutorProfileRequest();
        request.setEspecialidades(java.util.List.of("Matemáticas", "Física"));
        request.setTarifaHora(new BigDecimal("50.00"));
        request.setDisponibilidad("Lunes a Viernes 15:00-19:00");
        request.setBio("Profesor de matemáticas con 10 años de experiencia");

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByUs(usuario)).thenReturn(Optional.empty());
        when(tutorRepository.save(any(Tutor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TutorProfileResponse response = tutorService.crearPerfil(usuarioId, request);

        ArgumentCaptor<Tutor> captor = ArgumentCaptor.forClass(Tutor.class);
        verify(tutorRepository).save(captor.capture());

        Tutor saved = captor.getValue();
        assertThat(saved.getEspecialidades()).isEqualTo(request.getEspecialidades());
        assertThat(saved.getTarifaHora()).isEqualTo(request.getTarifaHora());
        assertThat(saved.getVerificado()).isFalse();
        assertThat(saved.getClassroomConectado()).isFalse();
    }

    @Test
    void crearPerfilShouldFailWhenUserIsNotTutor() {
        Long usuarioId = 1L;
        Usuario usuario = buildUsuario(usuarioId, false);
        TutorProfileRequest request = new TutorProfileRequest();

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> tutorService.crearPerfil(usuarioId, request))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void crearPerfilShouldFailWhenUserAlreadyHasProfile() {
        Long usuarioId = 1L;
        Usuario usuario = buildUsuario(usuarioId, true);
        Tutor existingTutor = buildTutor(1L, usuarioId);
        TutorProfileRequest request = new TutorProfileRequest();

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByUs(usuario)).thenReturn(Optional.of(existingTutor));

        assertThatThrownBy(() -> tutorService.crearPerfil(usuarioId, request))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void editarPerfilShouldUpdateTutorData() {
        Long usuarioId = 1L;
        Long tutorId = 1L;
        Usuario usuario = buildUsuario(usuarioId, true);
        Tutor tutor = buildTutor(tutorId, usuarioId);

        TutorProfileRequest request = new TutorProfileRequest();
        request.setEspecialidades(java.util.List.of("Nuevas especialidades"));
        request.setTarifaHora(new BigDecimal("60.00"));
        request.setDisponibilidad("Nuevo horario");
        request.setBio("Nuevo bio");

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByIdAndUsId(tutorId, usuarioId)).thenReturn(Optional.of(tutor));
        when(tutorRepository.save(any(Tutor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TutorProfileResponse response = tutorService.editarPerfil(usuarioId, tutorId, request);

        ArgumentCaptor<Tutor> captor = ArgumentCaptor.forClass(Tutor.class);
        verify(tutorRepository).save(captor.capture());

        assertThat(captor.getValue().getEspecialidades()).isEqualTo(request.getEspecialidades());
        assertThat(captor.getValue().getTarifaHora()).isEqualTo(new BigDecimal("60.00"));
    }

    @Test
    void editarPerfilShouldFailWhenTutorNotBelongsToUser() {
        Long usuarioId = 1L;
        Long tutorId = 1L;
        Usuario usuario = buildUsuario(usuarioId, true);
        Tutor tutor = buildTutor(tutorId, 999L);

        TutorProfileRequest request = new TutorProfileRequest();

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByIdAndUsId(tutorId, usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tutorService.editarPerfil(usuarioId, tutorId, request))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void obtenerPerfilPublicoShouldReturnPublicProfile() {
        Long tutorId = 1L;
        Tutor tutor = buildTutor(tutorId, 1L);

        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(tutor));

        var result = tutorService.obtenerPerfilPublico(tutorId);

        assertThat(result).isNotNull();
    }

    @Test
    void obtenerPerfilesPorUsuarioShouldReturnUserProfiles() {
        Long usuarioId = 1L;
        Tutor tutor = buildTutor(1L, usuarioId);

        when(tutorRepository.findAllByUsId(usuarioId)).thenReturn(java.util.List.of(tutor));

        var result = tutorService.obtenerPerfilesPorUsuario(usuarioId);

        assertThat(result).isNotNull();
    }

    @Test
    void obtenerPerfilDelUsuarioShouldReturnUserSpecificProfile() {
        Long usuarioId = 1L;
        Long tutorId = 1L;
        Tutor tutor = buildTutor(tutorId, usuarioId);

        when(tutorRepository.findByIdAndUsId(tutorId, usuarioId)).thenReturn(Optional.of(tutor));

        var result = tutorService.obtenerPerfilDelUsuario(usuarioId, tutorId);

        assertThat(result).isNotNull();
    }

    @Test
    void obtenerTutoresVerificadosShouldReturnVerifiedTutors() {
        when(tutorRepository.findVerificadosByEspecialidadAndTarifa(
                        null, new BigDecimal("0"), new BigDecimal("1000"), PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        var result =
                tutorService.obtenerTutoresVerificados(
                        null, new BigDecimal("0"), new BigDecimal("1000"), 0, 10);

        assertThat(result).isNotNull();
    }

    @Test
    void solicitarVerificacionShouldMarkAsPendingVerification() {
        Long usuarioId = 1L;
        Long tutorId = 1L;
        Tutor tutor = buildTutor(tutorId, usuarioId);
        tutor.setVerificado(false);

        when(tutorRepository.findByIdAndUsId(tutorId, usuarioId)).thenReturn(Optional.of(tutor));
        when(tutorRepository.save(any(Tutor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        tutorService.solicitarVerificacion(usuarioId, tutorId);

        verify(tutorRepository).save(any(Tutor.class));
    }

    @Test
    void obtenerEstadoVerificacionShouldReturnVerificationStatus() {
        Long usuarioId = 1L;
        Long tutorId = 1L;
        Tutor tutor = buildTutor(tutorId, usuarioId);
        tutor.setVerificado(true);

        when(tutorRepository.findByIdAndUsId(tutorId, usuarioId)).thenReturn(Optional.of(tutor));

        String status = tutorService.obtenerEstadoVerificacion(usuarioId, tutorId);

        assertThat(status).isNotNull();
    }

    @Test
    void obtenerTutorPorUsuarioIdShouldReturnTutor() {
        Long usuarioId = 1L;
        Tutor tutor = buildTutor(1L, usuarioId);

        when(tutorRepository.findAllByUsId(usuarioId)).thenReturn(java.util.List.of(tutor));

        var result = tutorService.obtenerTutorPorUsuarioId(usuarioId);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(tutor);
    }

    @Test
    void obtenerTutorPorIdShouldReturnTutor() {
        Long tutorId = 1L;
        Tutor tutor = buildTutor(tutorId, 1L);

        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(tutor));

        var result = tutorService.obtenerTutorPorId(tutorId);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(tutor);
    }

    @Test
    void conectarClassroomShouldLinkGoogleClassroom() {
        Long usuarioId = 1L;
        Tutor tutor = buildTutor(1L, usuarioId);
        String googleEmail = "profesor@gmail.com";

        when(tutorRepository.findAllByUsId(usuarioId)).thenReturn(java.util.List.of(tutor));
        when(tutorRepository.save(any(Tutor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Tutor result = tutorService.conectarClassroom(usuarioId, googleEmail);

        assertThat(result).isNotNull();
        verify(tutorRepository).save(any(Tutor.class));
    }

    @Test
    void desconectarClassroomShouldUnlinkGoogleClassroom() {
        Long usuarioId = 1L;
        Tutor tutor = buildTutor(1L, usuarioId);
        tutor.setClassroomConectado(true);

        when(tutorRepository.findAllByUsId(usuarioId)).thenReturn(java.util.List.of(tutor));
        when(tutorRepository.save(any(Tutor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        tutorService.desconectarClassroom(usuarioId);

        verify(tutorRepository).save(any(Tutor.class));
    }

    @Test
    void tienePagoVerificacionPendienteShouldReturnTrueWhenExists() {
        Long tutorId = 1L;

        when(transaccionPagoRepository.existsByTutorIdAndTipoAndEstado(
                        tutorId, TipoTransaccion.PAGO_VERIFICACION, EstadoTransaccion.PENDIENTE))
                .thenReturn(true);

        boolean result = tutorService.tienePagoVerificacionPendiente(tutorId);

        assertThat(result).isTrue();
    }

    @Test
    void tienePagoVerificacionPendienteShouldReturnFalseWhenNotExists() {
        Long tutorId = 1L;

        when(transaccionPagoRepository.existsByTutorIdAndTipoAndEstado(
                        tutorId, TipoTransaccion.PAGO_VERIFICACION, EstadoTransaccion.PENDIENTE))
                .thenReturn(false);

        boolean result = tutorService.tienePagoVerificacionPendiente(tutorId);

        assertThat(result).isFalse();
    }

    @Test
    void activarVerificacionShouldVerifyTutorAndCompleteTransaction() {
        Long tutorId = 1L;
        Long usuarioId = 1L;
        Tutor tutor = buildTutor(tutorId, usuarioId);
        tutor.setVerificado(false);

        TransaccionPago transaccion = buildTransaccion(1L, tutorId);
        transaccion.setEstado(EstadoTransaccion.COMPLETADA);

        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(transaccionPagoRepository.findTopByTutorIdAndTipoOrderByIniciadoAtDesc(
                        tutorId, TipoTransaccion.PAGO_VERIFICACION))
                .thenReturn(Optional.of(transaccion));
        when(tutorRepository.save(any(Tutor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        tutorService.activarVerificacion(tutorId);

        ArgumentCaptor<Tutor> captor = ArgumentCaptor.forClass(Tutor.class);
        verify(tutorRepository).save(captor.capture());

        assertThat(captor.getValue().getVerificado()).isTrue();
    }

    // Helper methods
    private Usuario buildUsuario(Long id, boolean esTutor) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre("Test User");
        usuario.setEmail("test@example.com");
        usuario.setEsTutor(esTutor);
        return usuario;
    }

    private Tutor buildTutor(Long id, Long usuarioId) {
        Usuario usuario = buildUsuario(usuarioId, true);
        Tutor tutor = new Tutor();
        tutor.setId(id);
        tutor.setUs(usuario);
        tutor.setEspecialidades(java.util.List.of("Matemáticas"));
        tutor.setTarifaHora(new BigDecimal("50.00"));
        tutor.setDisponibilidad("Lunes a Viernes");
        tutor.setBio("Profesor de matemáticas");
        tutor.setVerificado(false);
        tutor.setClassroomConectado(false);
        tutor.setCreatedAt(LocalDateTime.now());
        return tutor;
    }

    private TransaccionPago buildTransaccion(Long id, Long tutorId) {
        Tutor tutor = buildTutor(tutorId, 1L);
        TransaccionPago transaccion = new TransaccionPago();
        transaccion.setId(id);
        transaccion.setTutor(tutor);
        transaccion.setTipo(TipoTransaccion.PAGO_VERIFICACION);
        transaccion.setMonto(new BigDecimal("9.99"));
        transaccion.setEstado(EstadoTransaccion.PENDIENTE);
        return transaccion;
    }
}

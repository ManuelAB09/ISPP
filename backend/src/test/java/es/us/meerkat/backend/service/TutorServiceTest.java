package es.us.meerkat.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

import es.us.meerkat.backend.dto.CreateTutorRequest;
import es.us.meerkat.backend.dto.TutorProfileRequest;
import es.us.meerkat.backend.dto.TutorProfileResponse;
import es.us.meerkat.backend.dto.UpdateTutorRequest;
import es.us.meerkat.backend.entity.EstadoTransaccion;
import es.us.meerkat.backend.entity.TipoTransaccion;
import es.us.meerkat.backend.entity.TransaccionPago;
import es.us.meerkat.backend.entity.Tutor;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.TransaccionPagoRepository;
import es.us.meerkat.backend.repository.TutorRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import es.us.meerkat.backend.service.tutors.TutorService;

@ExtendWith(MockitoExtension.class)
class TutorServiceTest {

    @Mock private TutorRepository tutorRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TransaccionPagoRepository transaccionPagoRepository;

    @InjectMocks private TutorService tutorService;

    // =============================================
    // crearPerfil(Long, TutorProfileRequest)
    // =============================================

    @Test
    void createProfileShouldCreateProfileAndReturnResponse() {
        Long userId = 1L;
        Usuario usuario = buildUsuario(userId, true);
        TutorProfileRequest request = buildProfileRequest();

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByUsuario(usuario)).thenReturn(Optional.empty());
        when(tutorRepository.save(any(Tutor.class)))
                .thenAnswer(
                        inv -> {
                            Tutor t = inv.getArgument(0);
                            t.setId(10L);
                            return t;
                        });

        TutorProfileResponse response = tutorService.crearPerfil(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getTarifaHora()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(response.getVerificado()).isFalse();

        ArgumentCaptor<Tutor> captor = ArgumentCaptor.forClass(Tutor.class);
        verify(tutorRepository).save(captor.capture());
        assertThat(captor.getValue().getUsuario()).isEqualTo(usuario);
        assertThat(captor.getValue().getEspecialidades()).containsExactly("Matemáticas", "Física");
    }

    @Test
    void createProfileShouldFailWhenUserNotFound() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tutorService.crearPerfil(99L, buildProfileRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrado");
    }

    @Test
    void createProfileShouldFailWhenUserIsNotTutor() {
        Long userId = 1L;
        Usuario usuario = buildUsuario(userId, false);
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> tutorService.crearPerfil(userId, buildProfileRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("rol de profesor");
    }

    @Test
    void createProfileShouldFailWhenProfileAlreadyExists() {
        Long userId = 1L;
        Usuario usuario = buildUsuario(userId, true);
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByUsuario(usuario))
                .thenReturn(Optional.of(buildTutor(10L, usuario)));

        assertThatThrownBy(() -> tutorService.crearPerfil(userId, buildProfileRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("perfil ya existe");
    }

    // =============================================
    // editarPerfil
    // =============================================

    @Test
    void editProfileShouldUpdateTutorData() {
        Long userId = 1L;
        Long tutorId = 10L;
        Usuario usuario = buildUsuario(userId, true);
        Tutor tutor = buildTutor(tutorId, usuario);
        TutorProfileRequest request = buildProfileRequest();
        request.setBio("Nueva bio");

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByIdAndUsuarioId(tutorId, userId)).thenReturn(Optional.of(tutor));
        when(tutorRepository.save(any(Tutor.class))).thenAnswer(inv -> inv.getArgument(0));

        TutorProfileResponse response = tutorService.editarPerfil(userId, tutorId, request);

        assertThat(response.getBio()).isEqualTo("Nueva bio");
        verify(tutorRepository).save(tutor);
    }

    @Test
    void editProfileShouldFailWhenTutorNotFound() {
        Long userId = 1L;
        Long tutorId = 99L;
        when(usuarioRepository.findById(userId))
                .thenReturn(Optional.of(buildUsuario(userId, true)));
        when(tutorRepository.findByIdAndUsuarioId(tutorId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tutorService.editarPerfil(userId, tutorId, buildProfileRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrado");
    }

    // =============================================
    // obtenerPerfilPublico
    // =============================================

    @Test
    void getPublicProfileShouldReturnProfile() {
        Long tutorId = 10L;
        Tutor tutor = buildTutor(tutorId, buildUsuario(1L, true));
        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(tutor));

        TutorProfileResponse response = tutorService.obtenerPerfilPublico(tutorId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(tutorId);
    }

    @Test
    void getPublicProfileShouldFailWhenTutorNotFound() {
        when(tutorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tutorService.obtenerPerfilPublico(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrado");
    }

    // =============================================
    // obtenerPerfilesPorUsuario
    // =============================================

    @Test
    void getProfilesByUserShouldReturnList() {
        Long userId = 1L;
        Usuario usuario = buildUsuario(userId, true);
        Tutor tutor = buildTutor(10L, usuario);

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findAllByUsuarioId(userId)).thenReturn(List.of(tutor));

        List<TutorProfileResponse> result = tutorService.obtenerPerfilesPorUsuario(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(10L);
    }

    // =============================================
    // obtenerPerfilDelUsuario
    // =============================================

    @Test
    void getUserProfileShouldFailWhenNotFound() {
        when(tutorRepository.findByIdAndUsuarioId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tutorService.obtenerPerfilDelUsuario(1L, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrado");
    }

    // =============================================
    // obtenerTutoresVerificados
    // =============================================

    @Test
    void getVerifiedTutorsShouldUseSimpleQueryWhenNoFilters() {
        Tutor tutor = buildTutor(10L, buildUsuario(1L, true));
        tutor.setVerificado(true);
        Page<Tutor> page = new PageImpl<>(List.of(tutor));

        when(tutorRepository.findAllFiltrados(isNull(), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(page);

        Page<TutorProfileResponse> result =
                tutorService.obtenerTutoresVerificados(null, null, null, 0, 20);

        assertThat(result.getContent()).hasSize(1);
        verify(tutorRepository)
                .findAllFiltrados(isNull(), isNull(), isNull(), any(PageRequest.class));
    }

    @Test
    void getVerifiedTutorsShouldUseFilterQueryWhenFiltersProvided() {
        Tutor tutor = buildTutor(10L, buildUsuario(1L, true));
        tutor.setVerificado(true);
        Page<Tutor> page = new PageImpl<>(List.of(tutor));

        when(tutorRepository.findAllFiltrados(
                        eq("Matemáticas"),
                        any(BigDecimal.class),
                        any(BigDecimal.class),
                        any(PageRequest.class)))
                .thenReturn(page);

        Page<TutorProfileResponse> result =
                tutorService.obtenerTutoresVerificados(
                        "Matemáticas", new BigDecimal("10"), new BigDecimal("50"), 0, 20);

        assertThat(result.getContent()).hasSize(1);
    }

    // =============================================
    // Classroom: conectar / desconectar
    // =============================================

    @Test
    void connectClassroomShouldSetConnectedAndSaveEmail() {
        Long userId = 1L;
        Usuario usuario = buildUsuario(userId, true);
        Tutor tutor = buildTutor(10L, usuario);

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByUsuario(usuario)).thenReturn(Optional.of(tutor));
        when(tutorRepository.save(any(Tutor.class))).thenAnswer(inv -> inv.getArgument(0));

        Tutor result = tutorService.conectarClassroom(userId, "tutor@gmail.com");

        assertThat(result.getClassroomConectado()).isTrue();
        assertThat(result.getEmailClassroom()).isEqualTo("tutor@gmail.com");
        verify(tutorRepository).save(tutor);
    }

    @Test
    void connectClassroomShouldFailWhenNoTutorProfile() {
        Long userId = 1L;
        Usuario usuario = buildUsuario(userId, true);

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByUsuario(usuario)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tutorService.conectarClassroom(userId, "email@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("perfil de tutor");
    }

    @Test
    void disconnectClassroomShouldClearClassroomData() {
        Long userId = 1L;
        Usuario usuario = buildUsuario(userId, true);
        Tutor tutor = buildTutor(10L, usuario);
        tutor.setClassroomConectado(true);
        tutor.setEmailClassroom("tutor@gmail.com");

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByUsuario(usuario)).thenReturn(Optional.of(tutor));

        tutorService.desconectarClassroom(userId);

        assertThat(tutor.getClassroomConectado()).isFalse();
        assertThat(tutor.getEmailClassroom()).isNull();
        verify(tutorRepository).save(tutor);
    }

    // =============================================
    // crearPerfil(Long, CreateTutorRequest)
    // =============================================

    @Test
    void createProfileWithCreateRequestShouldCreateNewTutor() {
        Long userId = 1L;
        Usuario usuario = buildUsuario(userId, true);
        CreateTutorRequest request =
                CreateTutorRequest.builder()
                        .biografia("Profesor experimentado")
                        .tarifaPorHora(new BigDecimal("30.00"))
                        .especialidades(List.of("Inglés"))
                        .disponibilidad("Mañanas")
                        .build();

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByUsuario(usuario)).thenReturn(Optional.empty());
        when(tutorRepository.save(any(Tutor.class)))
                .thenAnswer(
                        inv -> {
                            Tutor t = inv.getArgument(0);
                            t.setId(20L);
                            return t;
                        });

        Tutor result = tutorService.crearPerfil(userId, request);

        assertThat(result.getBio()).isEqualTo("Profesor experimentado");
        assertThat(result.getTarifaHora()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(result.getVerificado()).isFalse();
    }

    @Test
    void createProfileWithCreateRequestShouldFailWhenProfileExists() {
        Long userId = 1L;
        Usuario usuario = buildUsuario(userId, true);

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByUsuario(usuario))
                .thenReturn(Optional.of(buildTutor(10L, usuario)));

        CreateTutorRequest request =
                CreateTutorRequest.builder()
                        .biografia("Bio")
                        .tarifaPorHora(BigDecimal.TEN)
                        .especialidades(List.of("X"))
                        .build();

        assertThatThrownBy(() -> tutorService.crearPerfil(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya tiene un perfil");
    }

    // =============================================
    // actualizarPerfil
    // =============================================

    @Test
    void updateProfileShouldOnlyUpdateNonNullFields() {
        Long userId = 1L;
        Usuario usuario = buildUsuario(userId, true);
        Tutor tutor = buildTutor(10L, usuario);
        tutor.setBio("Bio original");
        tutor.setTarifaHora(new BigDecimal("20.00"));

        UpdateTutorRequest request =
                UpdateTutorRequest.builder().biografia("Bio actualizada").build();

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByUsuario(usuario)).thenReturn(Optional.of(tutor));
        when(tutorRepository.save(any(Tutor.class))).thenAnswer(inv -> inv.getArgument(0));

        Tutor result = tutorService.actualizarPerfil(userId, request);

        assertThat(result.getBio()).isEqualTo("Bio actualizada");
        // tarifaHora no estaba en request → se mantiene
        assertThat(result.getTarifaHora()).isEqualByComparingTo(new BigDecimal("20.00"));
    }

    // =============================================
    // solicitarVerificacion
    // =============================================

    @Test
    void requestVerificationShouldCreatePendingTransaction() {
        Long userId = 1L;
        Long tutorId = 10L;
        Usuario usuario = buildUsuario(userId, true);
        Tutor tutor = buildTutor(tutorId, usuario);
        tutor.setVerificado(false);

        when(tutorRepository.findByIdAndUsuarioId(tutorId, userId)).thenReturn(Optional.of(tutor));
        when(transaccionPagoRepository.existsByTutorIdAndTipoAndEstado(
                        tutorId, TipoTransaccion.PAGO_VERIFICACION, EstadoTransaccion.PENDIENTE))
                .thenReturn(false);

        tutorService.solicitarVerificacion(userId, tutorId);

        ArgumentCaptor<TransaccionPago> captor = ArgumentCaptor.forClass(TransaccionPago.class);
        verify(transaccionPagoRepository).save(captor.capture());
        TransaccionPago tx = captor.getValue();
        assertThat(tx.getTipo()).isEqualTo(TipoTransaccion.PAGO_VERIFICACION);
        assertThat(tx.getEstado()).isEqualTo(EstadoTransaccion.PENDIENTE);
        assertThat(tx.getMonto()).isEqualByComparingTo(new BigDecimal("19.99"));
    }

    @Test
    void requestVerificationShouldFailWhenAlreadyVerified() {
        Long userId = 1L;
        Long tutorId = 10L;
        Tutor tutor = buildTutor(tutorId, buildUsuario(userId, true));
        tutor.setVerificado(true);

        when(tutorRepository.findByIdAndUsuarioId(tutorId, userId)).thenReturn(Optional.of(tutor));

        assertThatThrownBy(() -> tutorService.solicitarVerificacion(userId, tutorId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ya está verificado");
    }

    // =============================================
    // activarVerificacion
    // =============================================

    @Test
    void activateVerificationShouldMarkVerifiedAndCompleteTransaction() {
        Long tutorId = 10L;
        Usuario usuario = buildUsuario(1L, true);
        Tutor tutor = buildTutor(tutorId, usuario);
        tutor.setVerificado(false);

        TransaccionPago tx =
                TransaccionPago.builder()
                        .id(100L)
                        .tipo(TipoTransaccion.PAGO_VERIFICACION)
                        .estado(EstadoTransaccion.PENDIENTE)
                        .monto(new BigDecimal("19.99"))
                        .moneda("EUR")
                        .usuario(usuario)
                        .tutor(tutor)
                        .build();

        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(transaccionPagoRepository.findTopByTutorIdAndTipoOrderByIniciadoAtDesc(
                        tutorId, TipoTransaccion.PAGO_VERIFICACION))
                .thenReturn(Optional.of(tx));

        tutorService.activarVerificacion(tutorId);

        assertThat(tutor.getVerificado()).isTrue();
        assertThat(tx.getEstado()).isEqualTo(EstadoTransaccion.COMPLETADA);
        assertThat(tx.getCompletadoAt()).isNotNull();
        verify(tutorRepository).save(tutor);
        verify(transaccionPagoRepository).save(tx);
    }

    // =============================================
    // obtenerEstadoVerificacion
    // =============================================

    @Test
    void getVerificationStatusShouldReturnVerifiedWhenTutorIsVerified() {
        Long userId = 1L;
        Long tutorId = 10L;
        Tutor tutor = buildTutor(tutorId, buildUsuario(userId, true));
        tutor.setVerificado(true);

        when(tutorRepository.findByIdAndUsuarioId(tutorId, userId)).thenReturn(Optional.of(tutor));

        String estado = tutorService.obtenerEstadoVerificacion(userId, tutorId);

        assertThat(estado).isEqualTo("VERIFICADO");
    }

    @Test
    void getVerificationStatusShouldReturnNoRequestWhenNoTransaction() {
        Long userId = 1L;
        Long tutorId = 10L;
        Tutor tutor = buildTutor(tutorId, buildUsuario(userId, true));
        tutor.setVerificado(false);

        when(tutorRepository.findByIdAndUsuarioId(tutorId, userId)).thenReturn(Optional.of(tutor));
        when(transaccionPagoRepository.findTopByTutorIdAndTipoOrderByIniciadoAtDesc(
                        tutorId, TipoTransaccion.PAGO_VERIFICACION))
                .thenReturn(Optional.empty());

        String estado = tutorService.obtenerEstadoVerificacion(userId, tutorId);

        assertThat(estado).isEqualTo("SIN_SOLICITUD");
    }

    // =============================================
    // tienePagoVerificacionPendiente
    // =============================================

    @Test
    void hasPendingVerificationPaymentShouldReturnTrue() {
        when(transaccionPagoRepository.existsByTutorIdAndTipoAndEstado(
                        10L, TipoTransaccion.PAGO_VERIFICACION, EstadoTransaccion.PENDIENTE))
                .thenReturn(true);

        assertThat(tutorService.tienePagoVerificacionPendiente(10L)).isTrue();
    }

    // =============================================
    // obtenerTutorPorUsuarioId / obtenerTutorPorId
    // =============================================

    @Test
    void getTutorByUserIdShouldReturnTutor() {
        Long userId = 1L;
        Usuario usuario = buildUsuario(userId, true);
        Tutor tutor = buildTutor(10L, usuario);

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(tutorRepository.findByUsuario(usuario)).thenReturn(Optional.of(tutor));

        Optional<Tutor> result = tutorService.obtenerTutorPorUsuarioId(userId);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(10L);
    }

    @Test
    void getTutorByIdShouldReturnTutor() {
        Tutor tutor = buildTutor(10L, buildUsuario(1L, true));
        when(tutorRepository.findById(10L)).thenReturn(Optional.of(tutor));

        assertThat(tutorService.obtenerTutorPorId(10L)).isPresent();
    }

    // =============================================
    // Helpers
    // =============================================

    private Usuario buildUsuario(Long id, boolean esTutor) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre("Usuario " + id);
        u.setEmail("user" + id + "@meerkat.es");
        u.setEsTutor(esTutor);
        return u;
    }

    private Tutor buildTutor(Long id, Usuario usuario) {
        Tutor t = new Tutor();
        t.setId(id);
        t.setUsuario(usuario);
        t.setEspecialidades(List.of("Matemáticas"));
        t.setTarifaHora(new BigDecimal("25.00"));
        t.setDisponibilidad("Tardes");
        t.setBio("Bio de prueba");
        t.setVerificado(false);
        t.setClassroomConectado(false);
        t.setCreatedAt(LocalDateTime.now());
        return t;
    }

    private TutorProfileRequest buildProfileRequest() {
        TutorProfileRequest r = new TutorProfileRequest();
        r.setEspecialidades(List.of("Matemáticas", "Física"));
        r.setTarifaHora(new BigDecimal("25.00"));
        r.setDisponibilidad("Tardes y fines de semana");
        r.setBio("Profesor con experiencia");
        return r;
    }
}

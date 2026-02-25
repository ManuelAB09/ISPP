package es.us.meerkat.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import lombok.RequiredArgsConstructor;

/**
 * Servicio para gestionar la lógica de negocio relacionada con los tutores.
 *
 * <p>Incluye creación, edición, verificación y obtención de perfiles de tutor.
 */
@Service
@RequiredArgsConstructor
public class TutorService {

    /** Repositorio para acceder a la información de tutores. */
    private final TutorRepository tutorRepository;

    /** Repositorio para acceder a la información de usuarios. */
    private final UsuarioRepository usuarioRepository;

    private final TransaccionPagoRepository transaccionPagoRepository;

    // ===============================
    // CREAR PERFIL PROFESOR
    // ===============================

    /**
     * Crea un perfil de tutor para un usuario dado.
     *
     * @param usuarioIdParam Identificador del usuario.
     * @param requestParam Datos del perfil del tutor.
     * @return DTO con los datos del perfil creado.
     */
    @Transactional
    public TutorProfileResponse crearPerfil(
            final Long usuarioIdParam, final TutorProfileRequest requestParam) {

        final Usuario usuario =
                usuarioRepository
                        .findById(usuarioIdParam)
                        .orElseThrow(() -> new RuntimeException("User no encontrado"));

        if (!usuario.getEsTutor()) {
            throw new RuntimeException("El usuario no tiene rol de profesor");
        }

        if (tutorRepository.findByUs(usuario).isPresent()) {
            throw new RuntimeException("El perfil ya existe");
        }

        final Tutor tutor = new Tutor();
        tutor.setUs(usuario);
        tutor.setEspecialidades(requestParam.getEspecialidades());
        tutor.setTarifaHora(requestParam.getTarifaHora());
        tutor.setDisponibilidad(requestParam.getDisponibilidad());
        tutor.setBio(requestParam.getBio());
        tutor.setCreatedAt(LocalDateTime.now());
        tutor.setVerificado(false);
        tutor.setClassroomConectado(false);

        tutorRepository.save(tutor);

        return mapToResponse(tutor);
    }

    // ===============================
    // EDITAR PERFIL PROFESOR
    // ===============================

    /**
     * Edita el perfil de un tutor existente.
     *
     * @param usuarioIdParam Identificador del usuario.
     * @param requestParam Datos del perfil a actualizar.
     * @return DTO con los datos actualizados del tutor.
     */
    @Transactional
    public TutorProfileResponse editarPerfil(
            final Long usuarioIdParam,
            final Long tutorIdParam,
            final TutorProfileRequest requestParam) {

        final Usuario usuario =
                usuarioRepository
                        .findById(usuarioIdParam)
                        .orElseThrow(() -> new RuntimeException("User no encontrado"));

        final Tutor tutor =
                tutorRepository
                        .findByIdAndUsId(tutorIdParam, usuarioIdParam)
                        .orElseThrow(
                                () -> new RuntimeException("Tutor no encontrado o sin permisos"));
        // 🔐 Verificación de seguridad MUY IMPORTANTE
        if (!tutor.getUs().getId().equals(usuario.getId())) {
            throw new RuntimeException("No tienes permiso para editar este tutor");
        }

        tutor.setEspecialidades(requestParam.getEspecialidades());
        tutor.setTarifaHora(requestParam.getTarifaHora());
        tutor.setDisponibilidad(requestParam.getDisponibilidad());
        tutor.setBio(requestParam.getBio());

        tutorRepository.save(tutor);

        return mapToResponse(tutor);
    }

    // ===============================
    // VER PERFIL PÚBLICO
    // ===============================

    /**
     * Obtiene el perfil público de un tutor.
     *
     * @param tutorIdParam Identificador del tutor.
     * @return DTO con los datos públicos del tutor.
     */
    public TutorProfileResponse obtenerPerfilPublico(final Long tutorIdParam) {

        final Tutor tutor =
                tutorRepository
                        .findById(tutorIdParam)
                        .orElseThrow(() -> new RuntimeException("Tutor no encontrado"));

        return mapToResponse(tutor);
    }

    @Transactional(readOnly = true)
    public List<TutorProfileResponse> obtenerPerfilesPorUsuario(final Long usuarioIdParam) {

        final Usuario usuario =
                usuarioRepository
                        .findById(usuarioIdParam)
                        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        final List<Tutor> tutores = tutorRepository.findAllByUsId(usuario.getId());

        if (tutores.isEmpty()) {
            throw new RuntimeException("No tienes perfiles de tutor creados");
        }

        return tutores.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public TutorProfileResponse obtenerPerfilDelUsuario(
            final Long usuarioIdParam, final Long tutorIdParam) {

        final Tutor tutor =
                tutorRepository
                        .findByIdAndUsId(tutorIdParam, usuarioIdParam)
                        .orElseThrow(
                                () -> new RuntimeException("Tutor no encontrado o sin permisos"));

        return mapToResponse(tutor);
    }

    /**
     * Mapea un objeto {@link Tutor} a {@link TutorProfileResponse}.
     *
     * @param tutorParam Tutor a mapear.
     * @return DTO con la información del tutor.
     */
    private TutorProfileResponse mapToResponse(final Tutor tutor) {
        return TutorProfileResponse.builder()
                .id(tutor.getId())
                .userId(tutor.getUs().getId())
                .usuario(
                        TutorProfileResponse.UsuarioDto.builder()
                                .id(tutor.getUs().getId())
                                .nombre(tutor.getUs().getNombre())
                                // .foto(tutor.getUs().)
                                // .bio(tutor.getUs().getBio())
                                // .intereses(tutor.getUs().getIntereses())
                                .esTutor(tutor.getUs().getEsTutor())
                                .build())
                .especialidades(tutor.getEspecialidades())
                .tarifaHora(tutor.getTarifaHora())
                .disponibilidad(tutor.getDisponibilidad())
                .bio(tutor.getBio())
                .verificado(tutor.getVerificado())
                .classroomConectado(tutor.getClassroomConectado())
                .createdAt(tutor.getCreatedAt().toString())
                .build();
    }

    /**
     * Obtiene todos los tutores verificados aplicando filtros opcionales y paginación.
     *
     * @param especialidad Filtro por especialidad (opcional)
     * @param tarifaMin Tarifa mínima por hora (opcional, default 0)
     * @param tarifaMax Tarifa máxima por hora (opcional, default Double.MAX_VALUE)
     * @param page Número de página (0-indexed)
     * @param size Tamaño de página
     * @return Página de tutores filtrados
     */
    public Page<Tutor> obtenerTutoresVerificados(
            String especialidad, BigDecimal tarifaMin, BigDecimal tarifaMax, int page, int size) {
        // Valores por defecto
        String espec = (especialidad != null) ? especialidad : "";
        BigDecimal min = (tarifaMin != null) ? tarifaMin : BigDecimal.ZERO;
        BigDecimal max = (tarifaMax != null) ? tarifaMax : new BigDecimal(Double.MAX_VALUE);

        PageRequest pageable = PageRequest.of(page, size);

        return tutorRepository
                .findByVerificadoTrueAndEspecialidadesContainingIgnoreCaseAndTarifaHoraBetween(
                        espec, min, max, pageable);
    }

    // ===============================
    // VERIFICACIÓN DE TUTOR
    // ===============================

    /**
     * Permite al tutor iniciar la solicitud de verificación. No procesa el pago, solo prepara la
     * solicitud.
     *
     * @param tutorIdParam Identificador del tutor.
     * @return Tutor actualizado con estado de verificación pendiente.
     */
    @Transactional
    public void solicitarVerificacion(final Long usuarioIdParam, final Long tutorIdParam) {

        final Tutor tutor =
                tutorRepository
                        .findByIdAndUsId(tutorIdParam, usuarioIdParam)
                        .orElseThrow(
                                () -> new RuntimeException("Tutor no encontrado o sin permisos"));

        if (Boolean.TRUE.equals(tutor.getVerificado())) {
            throw new RuntimeException("El tutor ya está verificado");
        }

        boolean existePendiente =
                transaccionPagoRepository.existsByTutorIdAndTipoAndEstado(
                        tutorIdParam,
                        TipoTransaccion.PAGO_VERIFICACION,
                        EstadoTransaccion.PENDIENTE);

        if (existePendiente) {
            throw new RuntimeException("Ya existe una solicitud pendiente");
        }

        TransaccionPago transaccion =
                TransaccionPago.builder()
                        .tipo(TipoTransaccion.PAGO_VERIFICACION)
                        .monto(new BigDecimal("19.99"))
                        .moneda("EUR")
                        .estado(EstadoTransaccion.PENDIENTE)
                        .usuario(tutor.getUs())
                        .tutor(tutor)
                        .build();

        transaccionPagoRepository.save(transaccion);
    }

    /**
     * Consulta el estado de verificación de un tutor.
     *
     * @param tutorIdParam Identificador del tutor.
     * @return Estado de verificación: "VERIFICADO" o "PENDIENTE_REVISION".
     */
    @Transactional(readOnly = true)
    public String obtenerEstadoVerificacion(final Long usuarioIdParam, final Long tutorIdParam) {

        final Tutor tutor =
                tutorRepository
                        .findByIdAndUsId(tutorIdParam, usuarioIdParam)
                        .orElseThrow(
                                () -> new RuntimeException("Tutor no encontrado o sin permisos"));

        if (Boolean.TRUE.equals(tutor.getVerificado())) {
            return "VERIFICADO";
        }

        return transaccionPagoRepository
                .findTopByTutorIdAndTipoOrderByIniciadoAtDesc(
                        tutorIdParam, TipoTransaccion.PAGO_VERIFICACION)
                .map(tx -> tx.getEstado().name())
                .orElse("SIN_SOLICITUD");
    }
}

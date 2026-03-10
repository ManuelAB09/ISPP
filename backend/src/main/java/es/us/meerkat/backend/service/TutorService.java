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
import es.us.meerkat.backend.dto.UbicacionResponse;
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

        if (tutorRepository.findByUsuario(usuario).isPresent()) {
            throw new RuntimeException("El perfil ya existe");
        }

        final Tutor tutor = new Tutor();
        tutor.setUsuario(usuario);
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
                        .findByIdAndUsuarioId(tutorIdParam, usuarioIdParam)
                        .orElseThrow(
                                () -> new RuntimeException("Tutor no encontrado o sin permisos"));
        // 🔐 Verificación de seguridad MUY IMPORTANTE
        if (!tutor.getUsuario().getId().equals(usuario.getId())) {
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

        final List<Tutor> tutores = tutorRepository.findAllByUsuarioId(usuario.getId());

        // Devolver lista vacía si no hay perfiles; el controlador y el cliente
        // saben cómo manejarlo.
        return tutores.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public TutorProfileResponse obtenerPerfilDelUsuario(
            final Long usuarioIdParam, final Long tutorIdParam) {

        final Tutor tutor =
                tutorRepository
                        .findByIdAndUsuarioId(tutorIdParam, usuarioIdParam)
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
                .userId(tutor.getUsuario().getId())
                .usuario(
                        TutorProfileResponse.UsuarioDto.builder()
                                .id(tutor.getUsuario().getId())
                                .nombre(tutor.getUsuario().getNombre())
                                .foto(tutor.getUsuario().getFoto())
                                .bio(tutor.getUsuario().getBio())
                                .intereses(tutor.getUsuario().getIntereses())
                                .esTutor(tutor.getUsuario().getEsTutor())
                                .build())
                .especialidades(tutor.getEspecialidades())
                .tarifaHora(tutor.getTarifaHora())
                .disponibilidad(tutor.getDisponibilidad())
                .bio(tutor.getBio())
                .verificado(tutor.getVerificado())
                .classroomConectado(tutor.getClassroomConectado())
                .ubicacion(
                        tutor.getUsuario().getUbicacion() != null
                                ? UbicacionResponse.builder()
                                        .id(tutor.getUsuario().getUbicacion().getId())
                                        .nombre(tutor.getUsuario().getUbicacion().getNombre())
                                        .direccion(tutor.getUsuario().getUbicacion().getDireccion())
                                        .latitud(tutor.getUsuario().getUbicacion().getLatitud())
                                        .longitud(tutor.getUsuario().getUbicacion().getLongitud())
                                        .tipo(tutor.getUsuario().getUbicacion().getTipo())
                                        .coste(tutor.getUsuario().getUbicacion().getCoste())
                                        .build()
                                : null)
                .createdAt(tutor.getCreatedAt() != null ? tutor.getCreatedAt().toString() : null)
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
    public Page<TutorProfileResponse> obtenerTutoresVerificados(
            String especialidad, BigDecimal tarifaMin, BigDecimal tarifaMax, int page, int size) {

        PageRequest pageable = PageRequest.of(page, size);

        Page<Tutor> pageResult =
                tutorRepository.findVerificadosFiltrados(
                        especialidad, tarifaMin, tarifaMax, pageable);

        return pageResult.map(this::mapToResponse);
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
                        .findByIdAndUsuarioId(tutorIdParam, usuarioIdParam)
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
                        .usuario(tutor.getUsuario())
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
                        .findByIdAndUsuarioId(tutorIdParam, usuarioIdParam)
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

    // ===============================
    // NUEVOS MÉTODOS PARA CONTROLADOR
    // ===============================

    /**
     * Obtiene el tutor de un usuario por su ID.
     *
     * @param usuarioId ID del usuario
     * @return Tutor si existe
     */
    public java.util.Optional<Tutor> obtenerTutorPorUsuarioId(Long usuarioId) {
        Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        return tutorRepository.findByUsuario(usuario);
    }

    /**
     * Obtiene un tutor por su ID.
     *
     * @param tutorId ID del tutor
     * @return Tutor si existe
     */
    public java.util.Optional<Tutor> obtenerTutorPorId(Long tutorId) {
        return tutorRepository.findById(tutorId);
    }

    /**
     * Crea un nuevo tutor usando CreateTutorRequest.
     *
     * @param usuarioId ID del usuario
     * @param request datos del tutor
     * @return tutor creado
     */
    @Transactional
    public Tutor crearPerfil(Long usuarioId, es.us.meerkat.backend.dto.CreateTutorRequest request) {
        Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (tutorRepository.findByUsuario(usuario).isPresent()) {
            throw new IllegalArgumentException("El usuario ya tiene un perfil de tutor");
        }

        Tutor tutor = new Tutor();
        tutor.setUsuario(usuario);
        tutor.setEspecialidades(request.getEspecialidades());
        tutor.setTarifaHora(request.getTarifaPorHora());
        tutor.setBio(request.getBiografia());
        tutor.setDisponibilidad(request.getDisponibilidad());
        tutor.setVerificado(false);
        tutor.setClassroomConectado(false);
        tutor.setCreatedAt(LocalDateTime.now());

        return tutorRepository.save(tutor);
    }

    /**
     * Actualiza el perfil de un tutor.
     *
     * @param usuarioId ID del usuario
     * @param request datos actualizados
     * @return tutor actualizado
     */
    @Transactional
    public Tutor actualizarPerfil(
            Long usuarioId, es.us.meerkat.backend.dto.UpdateTutorRequest request) {
        Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Tutor tutor =
                tutorRepository
                        .findByUsuario(usuario)
                        .orElseThrow(
                                () -> new IllegalArgumentException("No tienes perfil de tutor"));

        if (request.getEspecialidades() != null) {
            tutor.setEspecialidades(request.getEspecialidades());
        }
        if (request.getTarifaPorHora() != null) {
            tutor.setTarifaHora(request.getTarifaPorHora());
        }
        if (request.getBiografia() != null) {
            tutor.setBio(request.getBiografia());
        }
        if (request.getDisponibilidad() != null) {
            tutor.setDisponibilidad(request.getDisponibilidad());
        }

        return tutorRepository.save(tutor);
    }

    /**
     * Conecta Google Classroom a un tutor.
     *
     * @param usuarioId ID del usuario
     * @param googleEmail email de google
     * @return tutor actualizado
     */
    @Transactional
    public Tutor conectarClassroom(Long usuarioId, String googleEmail) {
        Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Tutor tutor =
                tutorRepository
                        .findByUsuario(usuario)
                        .orElseThrow(
                                () -> new IllegalArgumentException("No tienes perfil de tutor"));

        tutor.setClassroomConectado(true);
        tutor.setEmailClassroom(googleEmail);

        return tutorRepository.save(tutor);
    }

    /**
     * Desconecta Google Classroom de un tutor.
     *
     * @param usuarioId ID del usuario
     */
    @Transactional
    public void desconectarClassroom(Long usuarioId) {
        Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Tutor tutor =
                tutorRepository
                        .findByUsuario(usuario)
                        .orElseThrow(
                                () -> new IllegalArgumentException("No tienes perfil de tutor"));

        tutor.setClassroomConectado(false);
        tutor.setEmailClassroom(null);

        tutorRepository.save(tutor);
    }

    /**
     * Comprueba si un tutor ya tiene un pago de verificación pendiente.
     *
     * @param tutorId ID del tutor
     * @return true si ya existe una transacción pendiente
     */
    public boolean tienePagoVerificacionPendiente(Long tutorId) {
        return transaccionPagoRepository.existsByTutorIdAndTipoAndEstado(
                tutorId, TipoTransaccion.PAGO_VERIFICACION, EstadoTransaccion.PENDIENTE);
    }

    /**
     * Activa la verificación del tutor tras confirmación de pago por Stripe. Marca el tutor como
     * verificado y completa la transacción.
     *
     * @param tutorId ID del tutor
     */
    @Transactional
    public void activarVerificacion(Long tutorId) {
        Tutor tutor =
                tutorRepository
                        .findById(tutorId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Tutor no encontrado: " + tutorId));

        // Activar verificación en el perfil
        tutor.setVerificado(true);
        tutorRepository.save(tutor);

        // Completar la transacción pendiente si existe
        transaccionPagoRepository
                .findTopByTutorIdAndTipoOrderByIniciadoAtDesc(
                        tutorId, TipoTransaccion.PAGO_VERIFICACION)
                .ifPresent(
                        tx -> {
                            tx.setEstado(EstadoTransaccion.COMPLETADA);
                            tx.setCompletadoAt(java.time.LocalDateTime.now());
                            transaccionPagoRepository.save(tx);
                        });
    }
}

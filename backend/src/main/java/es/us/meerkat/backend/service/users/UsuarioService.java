package es.us.meerkat.backend.service.users;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import es.us.meerkat.backend.dto.maps.UbicacionResponse;
import es.us.meerkat.backend.dto.users.ChangePasswordRequest;
import es.us.meerkat.backend.dto.users.UpdateUserRequest;
import es.us.meerkat.backend.dto.users.UserDetailResponse;
import es.us.meerkat.backend.dto.users.UserPublicResponse;
import es.us.meerkat.backend.dto.users.VisibilityRequest;
import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.maps.Ubicacion;
import es.us.meerkat.backend.entity.suscriptions.TipoPlanComunidad;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.exception.ValidationException;
import es.us.meerkat.backend.repository.chats.MensajeComunidadRepository;
import es.us.meerkat.backend.repository.communities.ComunidadRepository;
import es.us.meerkat.backend.repository.communities.InstitutionRepository;
import es.us.meerkat.backend.repository.communities.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.communities.SolicitudComunidadRepository;
import es.us.meerkat.backend.repository.events.AsistenciaEventoRepository;
import es.us.meerkat.backend.repository.events.EventoRepository;
import es.us.meerkat.backend.repository.google.GoogleClassroomConnectionRepository;
import es.us.meerkat.backend.repository.maps.UbicacionRepository;
import es.us.meerkat.backend.repository.notifications.PreferenciasNotificacionRepository;
import es.us.meerkat.backend.repository.suscriptions.SuscripcionRepository;
import es.us.meerkat.backend.repository.suscriptions.TransaccionPagoRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

/**
 * Servicio para gestionar la lógica de negocio de usuarios.
 *
 * <p>Cubre los endpoints de /api/v1/users del OpenAPI: obtener perfil propio, actualizar, cambiar
 * contraseña, eliminar cuenta, visibilidad y ver perfiles públicos.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    /** Prefijo público de los avatares predefinidos de Renata. */
    private static final String RENATA_AVATAR_PUBLIC_PREFIX = "/static/images/renata/";

    /** Patrón classpath para leer avatares predefinidos empaquetados en backend. */
    private static final String RENATA_AVATAR_CLASSPATH_PATTERN =
            "classpath:/static/static/images/renata/*.*";

    /** Longitud mínima requerida para las contraseñas. */
    private static final int MIN_PASSWORD_LENGTH = 8;

    /** Tamaño máximo de foto de perfil en bytes (5MB). */
    private static final long MAX_PROFILE_PHOTO_SIZE_BYTES = 5L * 1024L * 1024L;

    /** MIME types permitidos para foto de perfil. */
    private static final Set<String> ALLOWED_PROFILE_PHOTO_MIME_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    /** Patrón para parsear coordenadas en formato "latitud,longitud". */
    private static final Pattern COORDINATE_PAIR_PATTERN =
            Pattern.compile("^\\s*([-+]?\\d+(?:\\.\\d+)?)\\s*,\\s*([-+]?\\d+(?:\\.\\d+)?)\\s*$");

    /** Repositorio para acceder a la información de usuarios. */
    private final UsuarioRepository usuarioRepository;

    /** Repositorio para resolver ubicaciones por nombre. */
    private final UbicacionRepository ubicacionRepository;

    /** Repositorio para gestionar membresías de comunidades. */
    private final MiembroComunidadRepository miembroComunidadRepository;

    /** Codificador de contraseñas BCrypt. */
    private final BCryptPasswordEncoder passwordEncoder;

    /** Resolver para localizar recursos estáticos en classpath. */
    private final ResourcePatternResolver resourcePatternResolver;

    /** Repositorio para acceder a la información de las comunidades. */
    private final ComunidadRepository comunidadRepository;

    /** Repositorio para gestionar suscripciones. */
    private final SuscripcionRepository suscripcionRepository;

    /** Repositorio para gestionar transacciones de pago. */
    private final TransaccionPagoRepository transaccionPagoRepository;

    /** Repositorio para gestionar asistencias a eventos. */
    private final AsistenciaEventoRepository asistenciaEventoRepository;

    /** Repositorio para gestionar eventos. */
    private final EventoRepository eventoRepository;

    /** Repositorio para gestionar solicitudes de comunidad. */
    private final SolicitudComunidadRepository solicitudComunidadRepository;

    /** Repositorio para gestionar mensajes de comunidad. */
    private final MensajeComunidadRepository mensajeComunidadRepository;

    /** Repositorio para gestionar conexiones con Google Classroom. */
    private final GoogleClassroomConnectionRepository googleClassroomConnectionRepository;

    /** Repositorio para gestionar instituciones. */
    private final InstitutionRepository institutionRepository;

    /** Repositorio para gestionar preferencias de notificación. */
    private final PreferenciasNotificacionRepository preferenciasNotificacionRepository;

    /** EntityManager para operaciones de limpieza de sesión. */
    private final EntityManager entityManager;

    private static final int MAX_FREE_COMMUNITIES = 3;

    // ===============================
    // GET /api/v1/users/me
    // ===============================

    /**
     * Devuelve el perfil completo del usuario autenticado.
     *
     * @param usuario Usuario autenticado extraído del contexto.
     * @return Perfil completo del usuario.
     */
    @Transactional
    public UserDetailResponse obtenerPerfilPropio(final Usuario usuario) {
        Usuario usuarioActualizado =
                usuarioRepository
                        .findByEmail(usuario.getEmail())
                        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (usuarioActualizado.getTutor() != null) {
            usuarioActualizado.getTutor();
        }
        return mapToDetailResponse(usuarioActualizado);
    }

    // ===============================
    // PUT /api/v1/users/me
    // ===============================

    /**
     * Actualiza la información personal del usuario autenticado.
     *
     * <p>Solo modifica los campos que no sean nulos en el request.
     *
     * @param usuario Usuario autenticado.
     * @param requestParam Datos a actualizar.
     * @return Perfil actualizado.
     */
    @Transactional
    public UserDetailResponse actualizarPerfil(
            final Usuario usuario, final UpdateUserRequest requestParam) {

        if (requestParam.getNombre() != null) {
            usuario.setNombre(requestParam.getNombre());
        }
        if (requestParam.getFoto() != null) {
            usuario.setFoto(normalizarFotoPerfil(requestParam.getFoto()));
        }
        if (requestParam.getFotoBackgroundColor() != null) {
            usuario.setFotoBackgroundColor(requestParam.getFotoBackgroundColor());
        }
        if (requestParam.getBio() != null) {
            usuario.setBio(requestParam.getBio());
        }
        if (requestParam.getIntereses() != null) {
            usuario.setIntereses(requestParam.getIntereses());
        }
        if (requestParam.getEsTutor() != null) {
            usuario.setEsTutor(requestParam.getEsTutor());
        }
        if (requestParam.getUniversidad() != null) {
            usuario.setUniversidad(requestParam.getUniversidad());
        }
        if (requestParam.getGrado() != null) {
            usuario.setGrado(requestParam.getGrado());
        }
        if (requestParam.getNivelEstudios() != null) {
            usuario.setNivelEstudios(requestParam.getNivelEstudios());
        }
        if (requestParam.getBaseFormativa() != null) {
            usuario.setBaseFormativa(requestParam.getBaseFormativa());
        }
        if (requestParam.getUbicacion() != null) {
            String nombreUbicacion = requestParam.getUbicacion().trim();
            if (nombreUbicacion.isEmpty()) {
                usuario.setUbicacion(null);
            } else {
                Double[] coords = parseCoordinatePair(nombreUbicacion);
                Ubicacion ubicacion =
                        ubicacionRepository
                                .findByNombre(nombreUbicacion)
                                .orElseGet(
                                        () ->
                                                ubicacionRepository.save(
                                                        Ubicacion.builder()
                                                                .nombre(nombreUbicacion)
                                                                .direccion(nombreUbicacion)
                                                                .latitud(
                                                                        coords != null
                                                                                ? coords[0]
                                                                                : 0.0)
                                                                .longitud(
                                                                        coords != null
                                                                                ? coords[1]
                                                                                : 0.0)
                                                                .tipo("general")
                                                                .coste("desconocido")
                                                                .build()));
                usuario.setUbicacion(ubicacion);
            }
        }
        if (requestParam.getAutenticacionDosFactores() != null) {
            usuario.setAutenticacionDosFactores(requestParam.getAutenticacionDosFactores());
        }
        if (requestParam.getVisibleEnListados() != null) {
            usuario.setVisibleEnListados(requestParam.getVisibleEnListados());
        }
        if (requestParam.getNotificacionesPush() != null) {
            usuario.setNotificacionesPush(requestParam.getNotificacionesPush());
        }

        usuarioRepository.save(usuario);
        return mapToDetailResponse(usuario);
    }

    /**
     * Actualiza la foto de perfil del usuario autenticado a partir de un archivo.
     *
     * @param usuario Usuario autenticado.
     * @param file Archivo de imagen recibido en multipart.
     * @return Perfil actualizado.
     */
    @Transactional
    public UserDetailResponse actualizarFotoPerfil(
            final Usuario usuario, final MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Archivo de imagen requerido");
        }

        if (file.getSize() > MAX_PROFILE_PHOTO_SIZE_BYTES) {
            throw new IllegalArgumentException("La foto supera el límite de 5MB");
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_PROFILE_PHOTO_MIME_TYPES.contains(mimeType)) {
            throw new IllegalArgumentException("Formato no permitido. Solo JPG, PNG o WEBP");
        }

        try {
            String base64 = Base64.getEncoder().encodeToString(file.getBytes());
            String dataUri = "data:" + mimeType + ";base64," + base64;
            usuario.setFoto(dataUri);
            usuarioRepository.save(usuario);
            return mapToDetailResponse(usuario);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo procesar la imagen", e);
        }
    }

    // ===============================
    // DELETE /api/v1/users/me
    // ===============================

    /**
     * Elimina permanentemente la cuenta del usuario autenticado.
     *
     * <p>Esta acción es irreversible. El frontend debe mostrar confirmación antes de llamar a este
     * endpoint.
     *
     * @param usuario Usuario autenticado a eliminar.
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public void eliminarCuenta(final Usuario usuario) {
        if (usuario == null || usuario.getId() == null) {
            throw new ValidationException("Usuario no autenticado");
        }

        final Long usuarioId = usuario.getId();

        // ═══════════════════════════════════════════════════════
        // FASE A: Read-receipts (antes de eliminar mensajes)
        // ═══════════════════════════════════════════════════════
        entityManager
                .createQuery(
                        "DELETE FROM MensajeComunidadLeido ml WHERE ml.usuario.id = :id"
                                + " OR ml.mensajeComunidad.usuario.id = :id")
                .setParameter("id", usuarioId)
                .executeUpdate();
        entityManager
                .createQuery(
                        "DELETE FROM MensajeLeido ml WHERE ml.usuario.id = :id"
                                + " OR ml.mensaje.emisor.id = :id"
                                + " OR ml.mensaje.receptor.id = :id")
                .setParameter("id", usuarioId)
                .executeUpdate();

        // ═══════════════════════════════════════════════════════
        // FASE B: Comentarios y anuncios
        // ═══════════════════════════════════════════════════════
        entityManager
                .createQuery("DELETE FROM ComentarioAnuncio c WHERE c.anuncio.usuario.id = :id")
                .setParameter("id", usuarioId)
                .executeUpdate();
        entityManager
                .createQuery("DELETE FROM ComentarioAnuncio c WHERE c.usuario.id = :id")
                .setParameter("id", usuarioId)
                .executeUpdate();
        entityManager
                .createQuery("DELETE FROM Anuncio a WHERE a.usuario.id = :id")
                .setParameter("id", usuarioId)
                .executeUpdate();

        // ═══════════════════════════════════════════════════════
        // FASE C: Classroom, entregas, cuestionarios
        // ═══════════════════════════════════════════════════════
        entityManager
                .createQuery("DELETE FROM CalificacionClassroom c WHERE c.alumno.id = :id")
                .setParameter("id", usuarioId)
                .executeUpdate();
        entityManager
                .createQuery("DELETE FROM EntregaTarea e WHERE e.alumno.id = :id")
                .setParameter("id", usuarioId)
                .executeUpdate();
        entityManager
                .createQuery("DELETE FROM CuestionarioIntento ci WHERE ci.usuario.id = :id")
                .setParameter("id", usuarioId)
                .executeUpdate();
        entityManager
                .createNativeQuery("DELETE FROM cuestionario_alumnos WHERE usuario_id = :id")
                .setParameter("id", usuarioId)
                .executeUpdate();
        entityManager
                .createQuery(
                        "UPDATE Cuestionario c SET c.creador = null" + " WHERE c.creador.id = :id")
                .setParameter("id", usuarioId)
                .executeUpdate();

        // ═══════════════════════════════════════════════════════
        // FASE D: Feedback
        // ═══════════════════════════════════════════════════════
        entityManager
                .createQuery("DELETE FROM FeedbackRecomendacion fr WHERE fr.usuario.id = :id")
                .setParameter("id", usuarioId)
                .executeUpdate();
        entityManager
                .createQuery(
                        "DELETE FROM Feedback f WHERE f.alumno.id = :id"
                                + " OR f.profesor.id = :id")
                .setParameter("id", usuarioId)
                .executeUpdate();

        // ═══════════════════════════════════════════════════════
        // FASE E: Notificaciones, alertas, alarmas
        // ═══════════════════════════════════════════════════════
        entityManager
                .createQuery("DELETE FROM Notificacion n WHERE n.usuario.id = :id")
                .setParameter("id", usuarioId)
                .executeUpdate();
        entityManager
                .createQuery(
                        "DELETE FROM AlertaEvento a WHERE a.usuario.id = :id"
                                + " OR a.evento.creador.id = :id")
                .setParameter("id", usuarioId)
                .executeUpdate();
        entityManager
                .createQuery(
                        "DELETE FROM AlarmaPersonalizada a WHERE a.usuario.id = :id"
                                + " OR a.evento.creador.id = :id")
                .setParameter("id", usuarioId)
                .executeUpdate();

        // ═══════════════════════════════════════════════════════
        // FASE F: Actividad, invitaciones, grabaciones
        // ═══════════════════════════════════════════════════════
        entityManager
                .createQuery("DELETE FROM ActividadUsuario a WHERE a.usuario.id = :id")
                .setParameter("id", usuarioId)
                .executeUpdate();
        entityManager
                .createQuery(
                        "UPDATE InvitacionMiembro i SET i.usuarioAceptador = null"
                                + " WHERE i.usuarioAceptador.id = :id")
                .setParameter("id", usuarioId)
                .executeUpdate();
        entityManager
                .createQuery(
                        "DELETE FROM InvitacionMiembro i" + " WHERE i.usuarioInvitador.id = :id")
                .setParameter("id", usuarioId)
                .executeUpdate();
        entityManager
                .createQuery(
                        "UPDATE GrabacionClase g SET g.subidoPor = null"
                                + " WHERE g.subidoPor.id = :id")
                .setParameter("id", usuarioId)
                .executeUpdate();

        // ═══════════════════════════════════════════════════════
        // FASE G: Mensajes privados
        // ═══════════════════════════════════════════════════════
        entityManager
                .createQuery(
                        "DELETE FROM Mensaje m WHERE m.emisor.id = :id" + " OR m.receptor.id = :id")
                .setParameter("id", usuarioId)
                .executeUpdate();

        // ═══════════════════════════════════════════════════════
        // FASE H: Google Calendar
        // ═══════════════════════════════════════════════════════
        entityManager
                .createQuery(
                        "DELETE FROM GoogleCalendarEvento g WHERE g.usuario.id = :id"
                                + " OR g.evento.creador.id = :id")
                .setParameter("id", usuarioId)
                .executeUpdate();
        entityManager
                .createQuery("DELETE FROM GoogleCalendarToken g WHERE g.usuario.id = :id")
                .setParameter("id", usuarioId)
                .executeUpdate();
        entityManager
                .createQuery("DELETE FROM GoogleCalendarBooking g WHERE g.usuario.id = :id")
                .setParameter("id", usuarioId)
                .executeUpdate();

        // ═══════════════════════════════════════════════════════
        // FASE I: Solicitudes de contratación y valoraciones
        // ═══════════════════════════════════════════════════════
        entityManager
                .createQuery(
                        "DELETE FROM SolicitudContratacionDirecta s" + " WHERE s.alumno.id = :id")
                .setParameter("id", usuarioId)
                .executeUpdate();
        entityManager
                .createQuery("DELETE FROM ValoracionTutor v WHERE v.usuario.id = :id")
                .setParameter("id", usuarioId)
                .executeUpdate();

        // ═══════════════════════════════════════════════════════
        // FASE J: Limpieza de Tutor (si el usuario es tutor)
        // ═══════════════════════════════════════════════════════
        if (usuario.getTutor() != null) {
            final Long tutorId = usuario.getTutor().getId();
            entityManager
                    .createQuery("DELETE FROM ValoracionTutor v WHERE v.tutor.id = :tid")
                    .setParameter("tid", tutorId)
                    .executeUpdate();
            entityManager
                    .createQuery("DELETE FROM Valoracion v WHERE v.profesor.id = :tid")
                    .setParameter("tid", tutorId)
                    .executeUpdate();
            entityManager
                    .createQuery(
                            "DELETE FROM SolicitudContratacionDirecta s"
                                    + " WHERE s.tutor.id = :tid")
                    .setParameter("tid", tutorId)
                    .executeUpdate();
            entityManager
                    .createQuery("DELETE FROM TutorContratacion t WHERE t.tutor.id = :tid")
                    .setParameter("tid", tutorId)
                    .executeUpdate();
            entityManager
                    .createQuery("DELETE FROM DisponibilidadTutor d WHERE d.tutor.id = :tid")
                    .setParameter("tid", tutorId)
                    .executeUpdate();
            entityManager
                    .createQuery(
                            "UPDATE TransaccionPago t SET t.tutor = null"
                                    + " WHERE t.tutor.id = :tid")
                    .setParameter("tid", tutorId)
                    .executeUpdate();
            entityManager
                    .createQuery("UPDATE Mensaje m SET m.tutor = null" + " WHERE m.tutor.id = :tid")
                    .setParameter("tid", tutorId)
                    .executeUpdate();
        }

        // Nullify grabaciones referencing user's events before deleting events
        entityManager
                .createQuery(
                        "UPDATE GrabacionClase g SET g.evento = null"
                                + " WHERE g.evento.creador.id = :id")
                .setParameter("id", usuarioId)
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();

        // ═══════════════════════════════════════════════════════
        // PASOS ORIGINALES
        // ═══════════════════════════════════════════════════════

        // PASO 1: Eliminar eventos y asistencias
        asistenciaEventoRepository.deleteByUsuarioId(usuarioId);
        asistenciaEventoRepository.deleteByEventoCreadorId(usuarioId);
        eventoRepository.deleteByUsuarioId(usuarioId);

        // PASO 1b: Eliminar mensajes de comunidad del usuario
        mensajeComunidadRepository.deleteByUsuarioId(usuarioId);

        // PASO 2: Limpiar la sesión de Hibernate
        entityManager.clear();

        // PASO 3: Manipular comunidades
        List<Comunidad> comunidadesUsuario = comunidadRepository.findByCreadorId(usuarioId);
        for (Comunidad comunidad : comunidadesUsuario) {
            List<Usuario> miembrosMasAntiguos =
                    miembroComunidadRepository.findMiembrosMasAntiguosEnComunidad(
                            comunidad.getId(), usuarioId);

            if (miembrosMasAntiguos.isEmpty()) {
                comunidadRepository.delete(comunidad);
            } else {
                Usuario miembroMasAntiguo =
                        miembrosMasAntiguos.stream()
                                .filter(
                                        m ->
                                                comunidadRepository.countByCreadorIdAndTipoPlan(
                                                                m.getId(), TipoPlanComunidad.FREE)
                                                        < MAX_FREE_COMMUNITIES)
                                .findFirst()
                                .orElse(null);

                if (miembroMasAntiguo != null) {
                    comunidad.setCreador(miembroMasAntiguo);
                    comunidadRepository.save(comunidad);
                } else {
                    comunidadRepository.delete(comunidad);
                }
            }
        }

        // PASO 4: Eliminar otras relaciones
        solicitudComunidadRepository.deleteBySolicitanteId(usuarioId);
        solicitudComunidadRepository.deleteByRespondidaPorId(usuarioId);
        googleClassroomConnectionRepository.deleteByUsuarioId(usuarioId);
        institutionRepository.deleteByUsuarioAdminId(usuarioId);
        transaccionPagoRepository.deleteByUsuarioId(usuarioId);
        suscripcionRepository.deleteByUsuarioId(usuarioId);
        miembroComunidadRepository.deleteByUsuarioId(usuarioId);

        // PASO 5: Eliminar preferencias de notificación
        preferenciasNotificacionRepository
                .findByUsuarioId(usuarioId)
                .ifPresent(preferenciasNotificacionRepository::delete);

        // PASO 6: Eliminar el usuario
        usuarioRepository.delete(usuario);
    }

    // ===============================
    // PUT /api/v1/users/me/password
    // ===============================

    /**
     * Cambia la contraseña del usuario autenticado.
     *
     * <p>Verifica la contraseña actual antes de aplicar el cambio.
     *
     * @param usuario Usuario autenticado.
     * @param requestParam Contraseña actual y nueva.
     * @throws ValidationException si la contraseña actual es incorrecta o la nueva no cumple los
     *     requisitos.
     */
    @Transactional
    public void cambiarPassword(final Usuario usuario, final ChangePasswordRequest requestParam) {

        if (!passwordEncoder.matches(requestParam.getCurrentPassword(), usuario.getPassword())) {
            throw new ValidationException("La contraseña actual es incorrecta");
        }

        if (requestParam.getNewPassword() == null
                || requestParam.getNewPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("La nueva contraseña debe tener al menos 8 caracteres");
        }

        // Validar que la nueva contraseña no sea igual a la actual
        if (passwordEncoder.matches(requestParam.getNewPassword(), usuario.getPassword())) {
            throw new ValidationException("La nueva contraseña no puede ser igual a la anterior");
        }

        usuario.setPassword(passwordEncoder.encode(requestParam.getNewPassword()));
        usuarioRepository.save(usuario);
    }

    // ===============================
    // PUT /api/v1/users/me/visibility
    // ===============================

    /**
     * Actualiza la visibilidad del perfil en listados públicos.
     *
     * @param usuario Usuario autenticado.
     * @param requestParam Nueva configuración de visibilidad.
     * @return Perfil actualizado.
     */
    @Transactional
    public UserDetailResponse actualizarVisibilidad(
            final Usuario usuario, final VisibilityRequest requestParam) {

        if (requestParam.getVisibleEnListados() != null) {
            usuario.setVisibleEnListados(requestParam.getVisibleEnListados());
            usuarioRepository.save(usuario);
        }

        return mapToDetailResponse(usuario);
    }

    // ===============================
    // GET /api/v1/users/{userId}
    // ===============================

    /**
     * Devuelve el perfil público de un usuario por su ID.
     *
     * <p>Solo expone datos que el usuario ha hecho públicos.
     *
     * @param usuarioId Identificador del usuario.
     * @return Perfil público del usuario.
     * @throws RuntimeException si el usuario no existe.
     */
    public UserPublicResponse obtenerPerfilPublico(final Long usuarioId) {

        final Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return mapToPublicResponse(usuario);
    }

    /**
     * Devuelve la lista de avatares predefinidos disponibles para foto de perfil.
     *
     * @return Lista de rutas públicas de avatares de Renata.
     */
    public List<String> obtenerAvataresPerfilDisponibles() {
        Set<String> fileNames = obtenerNombresAvataresRenata();
        if (fileNames.isEmpty()) {
            return List.of();
        }

        return fileNames.stream()
                .sorted()
                .map(fileName -> RENATA_AVATAR_PUBLIC_PREFIX + fileName)
                .toList();
    }

    // ===============================
    // MÉTODOS AUXILIARES
    // ===============================

    /**
     * Mapea {@link Usuario} a {@link UserDetailResponse}.
     *
     * @param usuario Usuario a mapear.
     * @return DTO con datos completos del usuario.
     */
    private UserDetailResponse mapToDetailResponse(final Usuario usuario) {
        return UserDetailResponse.builder()
                .id(usuario.getId())
                .email(usuario.getEmail())
                .nombre(usuario.getNombre())
                .foto(usuario.getFoto())
                .fotoBackgroundColor(usuario.getFotoBackgroundColor())
                .bio(usuario.getBio())
                .universidad(usuario.getUniversidad())
                .grado(usuario.getGrado())
                .ubicacion(
                        usuario.getUbicacion() != null
                                ? UbicacionResponse.builder()
                                        .id(usuario.getUbicacion().getId())
                                        .nombre(usuario.getUbicacion().getNombre())
                                        .direccion(usuario.getUbicacion().getDireccion())
                                        .latitud(usuario.getUbicacion().getLatitud())
                                        .longitud(usuario.getUbicacion().getLongitud())
                                        .tipo(usuario.getUbicacion().getTipo())
                                        .coste(usuario.getUbicacion().getCoste())
                                        .build()
                                : null)
                .nivelEstudios(usuario.getNivelEstudios())
                .baseFormativa(usuario.getBaseFormativa())
                .intereses(usuario.getIntereses())
                .visibleEnListados(usuario.getVisibleEnListados())
                .esTutor(usuario.getEsTutor())
                .autenticacionDosFactores(usuario.getAutenticacionDosFactores())
                .notificacionesPush(usuario.getNotificacionesPush())
                .createdAt(usuario.getCreatedAt())
                .googleLinked(usuario.getGoogleId() != null)
                .build();
    }

    /**
     * Mapea {@link Usuario} a {@link UserPublicResponse}.
     *
     * @param usuario Usuario a mapear.
     * @return DTO con datos públicos del usuario.
     */
    private UserPublicResponse mapToPublicResponse(final Usuario usuario) {
        return UserPublicResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .foto(usuario.getFoto())
                .bio(usuario.getBio())
                .universidad(usuario.getUniversidad())
                .grado(usuario.getGrado())
                .ubicacion(
                        usuario.getUbicacion() != null
                                ? UbicacionResponse.builder()
                                        .id(usuario.getUbicacion().getId())
                                        .nombre(usuario.getUbicacion().getNombre())
                                        .direccion(usuario.getUbicacion().getDireccion())
                                        .latitud(usuario.getUbicacion().getLatitud())
                                        .longitud(usuario.getUbicacion().getLongitud())
                                        .tipo(usuario.getUbicacion().getTipo())
                                        .coste(usuario.getUbicacion().getCoste())
                                        .build()
                                : null)
                .intereses(usuario.getIntereses())
                .esTutor(usuario.getEsTutor())
                .tutorId(usuario.getTutor() != null ? usuario.getTutor().getId() : null)
                .build();
    }

    /**
     * Normaliza la foto de perfil recibida desde API.
     *
     * <p>Si llega un nombre de archivo (p.ej. Feliz.png) o una ruta de Renata, la transforma a ruta
     * pública estable. Si llega vacío, deja la foto sin valor (null). Cualquier otra URL/ruta se
     * respeta para no romper compatibilidad con clientes existentes.
     *
     * @param fotoOriginal Valor recibido en UpdateUserRequest.foto.
     * @return Ruta/URL normalizada a persistir.
     */
    private String normalizarFotoPerfil(final String fotoOriginal) {
        if (!StringUtils.hasText(fotoOriginal)) {
            return null;
        }

        String fotoLimpia = fotoOriginal.trim();
        String marker = RENATA_AVATAR_PUBLIC_PREFIX;

        if (fotoLimpia.startsWith(marker)) {
            String fileName = fotoLimpia.substring(marker.length());
            return construirRutaRenataSiExiste(fileName, fotoLimpia);
        }

        int markerIndex = fotoLimpia.indexOf(marker);
        if (markerIndex >= 0) {
            String fileName = fotoLimpia.substring(markerIndex + marker.length());
            return construirRutaRenataSiExiste(fileName, fotoLimpia);
        }

        if (!fotoLimpia.contains("/")) {
            return construirRutaRenataSiExiste(fotoLimpia, fotoLimpia);
        }

        return fotoLimpia;
    }

    /** Intenta parsear coordenadas desde un texto con formato "latitud,longitud". */
    private Double[] parseCoordinatePair(final String value) {
        Matcher matcher = COORDINATE_PAIR_PATTERN.matcher(value);
        if (!matcher.matches()) {
            return null;
        }

        try {
            double lat = Double.parseDouble(matcher.group(1));
            double lon = Double.parseDouble(matcher.group(2));
            if (!Double.isFinite(lat) || !Double.isFinite(lon)) {
                return null;
            }
            if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) {
                return null;
            }
            return new Double[] {lat, lon};
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Construye ruta pública de Renata si el archivo existe en recursos estáticos. */
    private String construirRutaRenataSiExiste(final String fileName, final String fallbackValue) {
        if (!StringUtils.hasText(fileName)) {
            return null;
        }

        Set<String> availableNames = obtenerNombresAvataresRenata();
        if (availableNames.contains(fileName)) {
            return RENATA_AVATAR_PUBLIC_PREFIX + fileName;
        }
        return fallbackValue;
    }

    /** Lee nombres de archivos de avatares Renata desde classpath. */
    private Set<String> obtenerNombresAvataresRenata() {
        try {
            Resource[] resources =
                    resourcePatternResolver.getResources(RENATA_AVATAR_CLASSPATH_PATTERN);

            return Arrays.stream(resources)
                    .map(Resource::getFilename)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toSet());
        } catch (IOException ignored) {
            return Collections.emptySet();
        }
    }
}

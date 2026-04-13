package es.us.meerkat.backend.service.communities;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import es.us.meerkat.backend.dto.communities.CommunityRankingEntryResponse;
import es.us.meerkat.backend.dto.users.UserSimpleResponse;
import es.us.meerkat.backend.entity.communities.Comunidad;
import es.us.meerkat.backend.entity.communities.EstadoComunidad;
import es.us.meerkat.backend.entity.communities.Institution;
import es.us.meerkat.backend.entity.communities.MiembroComunidad;
import es.us.meerkat.backend.entity.communities.RolComunidad;
import es.us.meerkat.backend.entity.communities.TipoGrupo;
import es.us.meerkat.backend.entity.events.Evento;
import es.us.meerkat.backend.entity.subscriptions.Suscripcion;
import es.us.meerkat.backend.entity.subscriptions.TipoPlan;
import es.us.meerkat.backend.entity.subscriptions.TipoPlanComunidad;
import es.us.meerkat.backend.entity.subscriptions.TipoPlanCorporativo;
import es.us.meerkat.backend.entity.tutors.Tutor;
import es.us.meerkat.backend.entity.users.Usuario;
import es.us.meerkat.backend.repository.chats.MensajeComunidadRepository;
import es.us.meerkat.backend.repository.communities.ComunidadRepository;
import es.us.meerkat.backend.repository.communities.InstitutionRepository;
import es.us.meerkat.backend.repository.communities.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.events.EventoRepository;
import es.us.meerkat.backend.repository.tutors.TutorRepository;
import es.us.meerkat.backend.repository.users.UsuarioRepository;
import es.us.meerkat.backend.service.subscriptions.SuscripcionService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CommunityService {

    private final ComunidadRepository comunidadRepository;
    private final MiembroComunidadRepository miembroComunidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final InstitutionRepository institutionRepository;
    private final AuthorizationService authorizationService;
    private final SuscripcionService suscripcionService;
    private final MensajeComunidadRepository mensajeComunidadRepository;
    private final EventoRepository eventoRepository;
    private final TutorRepository tutorRepository;

    private static final int MAX_FREE_COMMUNITIES = 3;
    private static final int MAX_PREMIUM_COMMUNITIES = 10;
    private static final int MAX_PRO_COMMUNITIES = 25;
    private static final int FREE_MAX_MEMBERS = 30;
    private static final int PREMIUM_MAX_MEMBERS = 75;
    private static final int PRO_MAX_MEMBERS = 250;
    private static final int INST_ACADEMY_MAX_COMMUNITIES = 30;
    private static final int INST_SCHOOL_MAX_COMMUNITIES = 100;
    private static final int INST_UNIVERSITY_MAX_COMMUNITIES = Integer.MAX_VALUE;
    private static final int INST_ACADEMY_MAX_MEMBERS = 500;
    private static final int INST_SCHOOL_MAX_MEMBERS = 2000;
    private static final int INST_UNIVERSITY_MAX_MEMBERS = 10000;

    /** Crea una nueva comunidad verificando límites de plan. */
    public Comunidad createCommunity(
            Long userId,
            String nombre,
            String descripcion,
            TipoGrupo tipoGrupo,
            String imagenUrl,
            Long institutionId,
            Integer maxMiembrosSolicitado) {
        // Validar que el usuario exista
        Usuario usuario =
                usuarioRepository
                        .findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Si es una comunidad institucional, no aplicar límites de FREE
        Institution institution = null;
        TipoPlanComunidad tipoPlan = TipoPlanComunidad.FREE;
        Integer maxMiembros;

        if (institutionId != null) {
            institution = obtenerInstitucion(institutionId);
            validarUsuarioPerteneceAInstitucion(usuario, institution);
            validarPlanInstitucionalActivo(institution);

            InstitutionPlanLimits institutionPlanLimits =
                    obtenerLimitesPlanInstitucional(institution);
            long totalInstitutionalCommunities =
                    comunidadRepository.countByInstitutionId(institutionId);

            if (institutionPlanLimits.maxCommunities() != Integer.MAX_VALUE
                    && totalInstitutionalCommunities >= institutionPlanLimits.maxCommunities()) {
                throw new IllegalArgumentException(
                        "La institución ha alcanzado el límite de "
                                + institutionPlanLimits.maxCommunities()
                                + " comunidades para el plan "
                                + institution.getPlanCorporativo().name()
                                + ".");
            }

            if (maxMiembrosSolicitado != null) {
                if (maxMiembrosSolicitado < 1) {
                    throw new IllegalArgumentException(
                            "El máximo de miembros debe ser mayor que 0.");
                }
                if (maxMiembrosSolicitado > institutionPlanLimits.maxMembers()) {
                    throw new IllegalArgumentException(
                            "El máximo de miembros para el plan institucional "
                                    + institution.getPlanCorporativo().name()
                                    + " es "
                                    + institutionPlanLimits.maxMembers()
                                    + ".");
                }
            }

            // Se usa UNLIMITED para distinguir comunidades institucionales.
            tipoPlan = TipoPlanComunidad.UNLIMITED;
            maxMiembros =
                    maxMiembrosSolicitado != null
                            ? maxMiembrosSolicitado
                            : institutionPlanLimits.maxMembers();
            maxMiembros =
                    maxMiembrosSolicitado != null
                            ? maxMiembrosSolicitado
                            : institutionPlanLimits.maxMembers();
        } else {
            Optional<Suscripcion> suscripcionOpt = suscripcionService.obtenerMiSuscripcion(userId);
            TipoPlan userPlan = suscripcionOpt.map(Suscripcion::getPlan).orElse(TipoPlan.FREE);
            long totalCommunities =
                    comunidadRepository.countManagedNonInstitutionCommunities(userId);

            int maxCommunities = getMaxCommunitiesByPlan(userPlan);
            if (totalCommunities >= maxCommunities) {
                throw new IllegalArgumentException(
                        "Has alcanzado el límite de "
                                + maxCommunities
                                + " comunidades para el plan "
                                + userPlan.name()
                                + ".");
            }

            int maxAllowedMembers = getMaxMembersByPlan(userPlan);
            if (maxMiembrosSolicitado != null) {
                if (maxMiembrosSolicitado < 1) {
                    throw new IllegalArgumentException(
                            "El máximo de miembros debe ser mayor que 0.");
                }
                if (maxMiembrosSolicitado > maxAllowedMembers) {
                    throw new IllegalArgumentException(
                            "El máximo de miembros para tu plan "
                                    + userPlan.name()
                                    + " es "
                                    + maxAllowedMembers
                                    + ".");
                }
            }

            maxMiembros = maxMiembrosSolicitado != null ? maxMiembrosSolicitado : maxAllowedMembers;
            tipoPlan =
                    userPlan == TipoPlan.FREE ? TipoPlanComunidad.FREE : TipoPlanComunidad.PREMIUM;
        }

        if (imagenUrl == null || imagenUrl.isBlank()) {
            imagenUrl = "https://i.postimg.cc/Bb8f1dDC/community-default.png";
        }

        // Crear comunidad
        Comunidad comunidad =
                Comunidad.builder()
                        .nombre(nombre)
                        .descripcion(descripcion)
                        .tipoGrupo(tipoGrupo)
                        .imagenUrl(imagenUrl)
                        .creador(usuario)
                        .institution(institution)
                        .tipoPlan(tipoPlan)
                        .estado(EstadoComunidad.ACTIVA)
                        .maxMiembros(maxMiembros)
                        .build();

        Comunidad savedComunidad = comunidadRepository.save(comunidad);

        // Asignar al creador como ADMIN
        MiembroComunidad miembro =
                MiembroComunidad.builder()
                        .usuario(usuario)
                        .comunidad(savedComunidad)
                        .rol(RolComunidad.ADMIN)
                        .build();

        miembroComunidadRepository.save(miembro);

        return savedComunidad;
    }

    /** Crea una nueva comunidad con rol inicial personalizado para el creador. */
    public Comunidad createCommunity(
            Long userId, String nombre, String descripcion, TipoGrupo tipoGrupo, String imagenUrl) {
        return createCommunity(
                userId, nombre, descripcion, tipoGrupo, imagenUrl, (Long) null, (Integer) null);
    }

    public Comunidad createCommunity(
            Long userId,
            String nombre,
            String descripcion,
            TipoGrupo tipoGrupo,
            String imagenUrl,
            Integer maxMiembrosSolicitado) {
        return createCommunity(
                userId, nombre, descripcion, tipoGrupo, imagenUrl, null, maxMiembrosSolicitado);
    }

    public Comunidad createCommunity(
            Long userId,
            String nombre,
            String descripcion,
            TipoGrupo tipoGrupo,
            String imagenUrl,
            Long institutionId) {
        return createCommunity(
                userId, nombre, descripcion, tipoGrupo, imagenUrl, institutionId, (Integer) null);
    }

    public Comunidad createCommunity(
            Long userId,
            String nombre,
            String descripcion,
            TipoGrupo tipoGrupo,
            String imagenUrl,
            Long institutionId,
            RolComunidad rolInicial) {
        return createCommunity(
                userId, nombre, descripcion, tipoGrupo, imagenUrl, institutionId, null, rolInicial);
    }

    public Comunidad createCommunity(
            Long userId,
            String nombre,
            String descripcion,
            TipoGrupo tipoGrupo,
            String imagenUrl,
            Long institutionId,
            Integer maxMiembrosSolicitado,
            RolComunidad rolInicial) {
        // Validar perfil de tutor si el creador quiere rol PROFESOR
        if (rolInicial == RolComunidad.PROFESOR) {
            Usuario usuario =
                    usuarioRepository
                            .findById(userId)
                            .orElseThrow(
                                    () -> new IllegalArgumentException("Usuario no encontrado"));
            if (usuario.getEsTutor() == null || !usuario.getEsTutor()) {
                throw new IllegalArgumentException(
                        "Solo los usuarios tutores pueden crear una comunidad como profesor");
            }
            Tutor tutor =
                    tutorRepository
                            .findByUsuario(usuario)
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "Debes completar tu perfil de tutor antes de"
                                                        + " crear una comunidad como profesor"));
            if (tutor.getEspecialidades() == null
                    || tutor.getEspecialidades().isEmpty()
                    || tutor.getTarifaHora() == null
                    || tutor.getBio() == null
                    || tutor.getBio().isBlank()) {
                throw new IllegalArgumentException(
                        "Debes completar tu perfil de tutor (especialidades, tarifa y bio)"
                                + " antes de crear una comunidad como profesor");
            }
        }

        Comunidad savedComunidad =
                createCommunity(
                        userId,
                        nombre,
                        descripcion,
                        tipoGrupo,
                        imagenUrl,
                        institutionId,
                        maxMiembrosSolicitado);

        // El creador mantiene rol ADMIN; rolDocente modela su rol de contenido.
        if (rolInicial != null && rolInicial != RolComunidad.ADMIN) {
            miembroComunidadRepository
                    .findByUsuarioIdAndComunidadId(userId, savedComunidad.getId())
                    .ifPresent(
                            m -> {
                                m.setRolDocente(rolInicial);
                                miembroComunidadRepository.save(m);
                            });
        }

        return savedComunidad;
    }

    private int getMaxCommunitiesByPlan(TipoPlan plan) {
        if (plan == null) {
            return MAX_FREE_COMMUNITIES;
        }
        return switch (plan) {
            case FREE -> MAX_FREE_COMMUNITIES;
            case PREMIUM -> MAX_PREMIUM_COMMUNITIES;
            case PRO -> MAX_PRO_COMMUNITIES;
        };
    }

    private int getMaxMembersByPlan(TipoPlan plan) {
        if (plan == null) {
            return FREE_MAX_MEMBERS;
        }
        return switch (plan) {
            case FREE -> FREE_MAX_MEMBERS;
            case PREMIUM -> PREMIUM_MAX_MEMBERS;
            case PRO -> PRO_MAX_MEMBERS;
        };
    }

    /** Obtiene la institución del repositorio. */
    private Institution obtenerInstitucion(Long institutionId) {
        return institutionRepository
                .findById(institutionId)
                .orElseThrow(() -> new IllegalArgumentException("Institución no encontrada"));
    }

    private void validarUsuarioPerteneceAInstitucion(Usuario usuario, Institution institution) {
        boolean isLinkedMember =
                usuario.getInstitution() != null
                        && institution.getId().equals(usuario.getInstitution().getId());
        boolean isInstitutionAdmin =
                institution.getUsuarioAdmin() != null
                        && institution.getUsuarioAdmin().getId().equals(usuario.getId());

        if (!isLinkedMember && !isInstitutionAdmin) {
            throw new IllegalArgumentException(
                    "No perteneces ni administras la institución seleccionada para crear esta"
                            + " comunidad.");
        }
    }

    private void validarPlanInstitucionalActivo(Institution institution) {
        boolean planActivo =
                Boolean.TRUE.equals(institution.getPlanActivo())
                        && (institution.getFechaFinPlan() == null
                                || institution.getFechaFinPlan().isAfter(LocalDateTime.now()));
        if (!planActivo || institution.getPlanCorporativo() == null) {
            throw new IllegalArgumentException(
                    "La institución no tiene un plan corporativo activo para crear comunidades"
                            + " institucionales.");
        }
    }

    private InstitutionPlanLimits obtenerLimitesPlanInstitucional(Institution institution) {
        TipoPlanCorporativo tipoPlan = institution.getPlanCorporativo();
        if (tipoPlan == null) {
            throw new IllegalArgumentException(
                    "La institución no tiene plan corporativo asignado.");
        }

        return switch (tipoPlan) {
            case BASICO, REDUCIDO_PUBLICA, REDUCIDO_PRIVADA ->
                    new InstitutionPlanLimits(
                            INST_ACADEMY_MAX_COMMUNITIES, INST_ACADEMY_MAX_MEMBERS);
            case ESTANDAR ->
                    new InstitutionPlanLimits(INST_SCHOOL_MAX_COMMUNITIES, INST_SCHOOL_MAX_MEMBERS);
            case PREMIUM ->
                    new InstitutionPlanLimits(
                            INST_UNIVERSITY_MAX_COMMUNITIES, INST_UNIVERSITY_MAX_MEMBERS);
        };
    }

    private record InstitutionPlanLimits(int maxCommunities, int maxMembers) {}

    /**
     * Indica si una comunidad está vinculada a una institución (es corporativa/institucional).
     *
     * @param communityId ID de la comunidad
     * @return true si la comunidad tiene una institución asociada, false en caso contrario
     */
    @Transactional(readOnly = true)
    public boolean isCommunityCorporate(Long communityId) {
        return comunidadRepository
                .findById(communityId)
                .map(c -> c.getInstitution() != null)
                .orElse(false);
    }

    /** Obtiene una comunidad por ID. Comunidades privadas solo son visibles para miembros. */
    @Transactional(readOnly = true)
    public Comunidad getCommunityById(Long communityId, Long userId) {
        Comunidad comunidad =
                comunidadRepository
                        .findById(communityId)
                        .orElseThrow(() -> new IllegalArgumentException("Comunidad no encontrada"));

        if (comunidad.getTipoGrupo() == TipoGrupo.GRUPO_PRIVADO
                && !authorizationService.isMemberOf(userId, communityId)) {
            throw new IllegalArgumentException("No tienes acceso a esta comunidad privada");
        }

        return comunidad;
    }

    /** Actualiza una comunidad (solo ADMIN). */
    public Comunidad updateCommunity(
            Long userId, Long communityId, String nombre, String descripcion, String imagenUrl) {
        if (!authorizationService.isAdminOf(userId, communityId)) {
            throw new IllegalArgumentException("Solo admins pueden actualizar la comunidad");
        }

        Comunidad comunidad =
                comunidadRepository
                        .findById(communityId)
                        .orElseThrow(() -> new IllegalArgumentException("Comunidad no encontrada"));

        if (nombre != null && !nombre.isBlank()) {
            comunidad.setNombre(nombre);
        }
        if (descripcion != null) {
            comunidad.setDescripcion(descripcion);
        }
        if (imagenUrl != null) {
            comunidad.setImagenUrl(imagenUrl);
        }

        return comunidadRepository.save(comunidad);
    }

    /** Elimina una comunidad (solo ADMIN) - cascada automática. */
    public void deleteCommunity(Long userId, Long communityId) {
        if (!authorizationService.isAdminOf(userId, communityId)) {
            throw new IllegalArgumentException("Solo admins pueden eliminar la comunidad");
        }

        Comunidad comunidad =
                comunidadRepository
                        .findById(communityId)
                        .orElseThrow(() -> new IllegalArgumentException("Comunidad no encontrada"));

        // Desvincular eventos antes de eliminar para evitar violación de FK
        eventoRepository.disassociateFromComunidad(communityId);
        comunidadRepository.delete(comunidad);
    }

    @Transactional(readOnly = true)
    public Page<Comunidad> listActiveCommunities(
            String search,
            List<String> categorias,
            String institucion,
            List<TipoGrupo> tipoGrupos,
            List<TipoPlanComunidad> tipoPlanes,
            Pageable pageable) {
        String normalizedSearch = normalizeFilter(search);
        List<String> normalizedCategorias = normalizeFilters(categorias);
        String normalizedInstitucion = normalizeFilter(institucion);
        List<TipoGrupo> normalizedTipoGrupos = normalizeEnumFilters(tipoGrupos);
        List<TipoPlanComunidad> normalizedTipoPlanes = normalizeEnumFilters(tipoPlanes);

        return comunidadRepository.searchActiveCommunities(
                normalizedSearch,
                normalizedCategorias,
                normalizedInstitucion,
                normalizedTipoGrupos,
                normalizedTipoPlanes,
                EstadoComunidad.ACTIVA,
                pageable);
    }

    private List<String> normalizeFilters(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        List<String> normalized =
                values.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .map(value -> value.length() > 120 ? value.substring(0, 120) : value)
                        .map(String::toLowerCase)
                        .distinct()
                        .toList();

        return normalized.isEmpty() ? null : normalized;
    }

    private <T> List<T> normalizeEnumFilters(List<T> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        List<T> normalized = values.stream().filter(Objects::nonNull).distinct().toList();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        return trimmed.length() > 120 ? trimmed.substring(0, 120) : trimmed;
    }

    /** Cambia la privacidad de una comunidad (solo ADMIN). */
    public Comunidad updatePrivacy(Long userId, Long communityId, TipoGrupo nuevoTipo) {
        if (!authorizationService.isAdminOf(userId, communityId)) {
            throw new IllegalArgumentException("Solo admins pueden cambiar la privacidad");
        }

        Comunidad comunidad =
                comunidadRepository
                        .findById(communityId)
                        .orElseThrow(() -> new IllegalArgumentException("Comunidad no encontrada"));

        comunidad.setTipoGrupo(nuevoTipo);
        return comunidadRepository.save(comunidad);
    }

    /** Mejora una comunidad a Premium (solo ADMIN) - sin procesar pago. */
    public Comunidad upgradeToPremium(Long userId, Long communityId) {
        if (!authorizationService.isAdminOf(userId, communityId)) {
            throw new IllegalArgumentException("Solo admins pueden mejorar la comunidad");
        }

        Comunidad comunidad =
                comunidadRepository
                        .findById(communityId)
                        .orElseThrow(() -> new IllegalArgumentException("Comunidad no encontrada"));

        if (comunidad.getTipoPlan() == TipoPlanComunidad.PREMIUM) {
            throw new IllegalArgumentException("La comunidad ya es Premium");
        }

        comunidad.setTipoPlan(TipoPlanComunidad.PREMIUM);
        comunidad.setMaxMiembros(PREMIUM_MAX_MEMBERS);

        return comunidadRepository.save(comunidad);
    }

    /** Obtiene el número de miembros en una comunidad. */
    @Transactional(readOnly = true)
    public long countMembers(Long communityId) {
        return miembroComunidadRepository.countByComunidadId(communityId);
    }

    /** Obtiene el aforo máximo de una comunidad. Null si es ilimitado. */
    @Transactional(readOnly = true)
    public Integer getMaxMembers(Long communityId) {
        return comunidadRepository
                .findById(communityId)
                .map(Comunidad::getMaxMiembros)
                .orElse(FREE_MAX_MEMBERS);
    }

    /**
     * Verifica si una comunidad puede aceptar más miembros.
     *
     * @param communityId ID de la comunidad
     * @return true si puede aceptar más miembros, false si está al límite
     */
    @Transactional(readOnly = true)
    public boolean canAddMember(Long communityId) {
        Comunidad comunidad =
                comunidadRepository
                        .findById(communityId)
                        .orElseThrow(() -> new IllegalArgumentException("Comunidad no encontrada"));

        // Si maxMiembros es null, es ilimitado
        if (comunidad.getMaxMiembros() == null) {
            return true;
        }

        long miembrosActuales = countMembers(communityId);
        return miembrosActuales < comunidad.getMaxMiembros();
    }

    // ===============================
    // Upload community photo
    // ===============================

    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> ALLOWED_IMAGE_MIME_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    /**
     * Actualiza la imagen/portada de una comunidad a partir de un archivo multipart. Solo admins
     * pueden realizar esta operación.
     */
    public Comunidad actualizarFotoComunidad(Long userId, Long communityId, MultipartFile file) {
        if (!authorizationService.isAdminOf(userId, communityId)) {
            throw new IllegalArgumentException("Solo admins pueden actualizar la comunidad");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Archivo de imagen requerido");
        }

        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("La imagen supera el límite de 5MB");
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_IMAGE_MIME_TYPES.contains(mimeType)) {
            throw new IllegalArgumentException("Formato no permitido. Solo JPG, PNG o WEBP");
        }

        Comunidad comunidad =
                comunidadRepository
                        .findById(communityId)
                        .orElseThrow(() -> new IllegalArgumentException("Comunidad no encontrada"));

        try {
            String base64 = Base64.getEncoder().encodeToString(file.getBytes());
            String dataUri = "data:" + mimeType + ";base64," + base64;
            comunidad.setImagenUrl(dataUri);
            return comunidadRepository.save(comunidad);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo procesar la imagen", e);
        }
    }

    @SuppressWarnings("null")
    @Transactional(readOnly = true)
    public List<CommunityRankingEntryResponse> getCommunityRanking(
            Long communityId, Long requesterId) {
        if (!authorizationService.isMemberOf(requesterId, communityId)) {
            throw new IllegalArgumentException("No eres miembro de esta comunidad");
        }

        List<MiembroComunidad> miembros =
                miembroComunidadRepository
                        .findByComunidadId(communityId, Pageable.unpaged())
                        .getContent();

        Map<Long, Long> mensajesPorUsuario =
                mensajeComunidadRepository.countMensajesByComunidad(communityId).stream()
                        .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        List<Evento> eventos = eventoRepository.findByComunidadId(communityId);
        Map<Long, Long> eventosCreados = new HashMap<>();
        Map<Long, Long> asistentesPorCreador = new HashMap<>();

        for (Evento evento : eventos) {
            if (evento.getCreador() == null) {
                continue;
            }
            Long creadorId = evento.getCreador().getId();
            eventosCreados.merge(creadorId, 1L, Long::sum);

            long asistentes =
                    evento.getAsistentesConfirmados() != null
                            ? evento.getAsistentesConfirmados()
                            : 0L;
            asistentesPorCreador.merge(creadorId, asistentes, Long::sum);
        }

        return miembros.stream()
                .map(
                        miembro -> {
                            var usuario = miembro.getUsuario();
                            long mensajes = mensajesPorUsuario.getOrDefault(usuario.getId(), 0L);
                            long eventosCount = eventosCreados.getOrDefault(usuario.getId(), 0L);
                            long asistentes =
                                    asistentesPorCreador.getOrDefault(usuario.getId(), 0L);

                            long puntos = mensajes + (asistentes * 5);

                            return new CommunityRankingEntryResponse(
                                    toUserSimple(usuario),
                                    mensajes,
                                    eventosCount,
                                    asistentes,
                                    puntos);
                        })
                .sorted(
                        Comparator.comparingLong(CommunityRankingEntryResponse::puntos)
                                .reversed()
                                .thenComparing(
                                        r -> r.usuario().nombre(),
                                        Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    private UserSimpleResponse toUserSimple(Usuario usuario) {
        return new UserSimpleResponse(
                usuario.getId(), usuario.getNombre(), usuario.getEmail(), usuario.getFoto());
    }
}

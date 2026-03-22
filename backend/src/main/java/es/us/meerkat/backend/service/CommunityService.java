package es.us.meerkat.backend.service;

import java.io.IOException;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import es.us.meerkat.backend.dto.CommunityRankingEntryResponse;
import es.us.meerkat.backend.dto.UserSimpleResponse;
import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.EstadoComunidad;
import es.us.meerkat.backend.entity.Evento;
import es.us.meerkat.backend.entity.MiembroComunidad;
import es.us.meerkat.backend.entity.RolComunidad;
import es.us.meerkat.backend.entity.Suscripcion;
import es.us.meerkat.backend.entity.TipoGrupo;
import es.us.meerkat.backend.entity.TipoPlan;
import es.us.meerkat.backend.entity.TipoPlanComunidad;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.ComunidadRepository;
import es.us.meerkat.backend.repository.EventoRepository;
import es.us.meerkat.backend.repository.InstitutionRepository;
import es.us.meerkat.backend.repository.MensajeComunidadRepository;
import es.us.meerkat.backend.repository.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
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

    private static final int MAX_FREE_COMMUNITIES = 3;
    private static final int MAX_PREMIUM_COMMUNITIES = 10;
    private static final int MAX_PRO_COMMUNITIES = 25;
    private static final int FREE_MAX_MEMBERS = 30;
    private static final int PREMIUM_MAX_MEMBERS = 75;
    private static final int PRO_MAX_MEMBERS = 250;

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
        es.us.meerkat.backend.entity.Institution institution = null;
        TipoPlanComunidad tipoPlan = TipoPlanComunidad.FREE;
        Integer maxMiembros;

        if (institutionId != null) {
            institution = obtenerInstitucion(institutionId);

            // Comunidades institucionales obtienen plan UNLIMITED
            tipoPlan = TipoPlanComunidad.UNLIMITED;
            maxMiembros = maxMiembrosSolicitado; // Sin límites de suscripción individual
        } else {
            Optional<Suscripcion> suscripcionOpt = suscripcionService.obtenerMiSuscripcion(userId);
            TipoPlan userPlan = suscripcionOpt.map(Suscripcion::getPlan).orElse(TipoPlan.FREE);
            long totalCommunities = comunidadRepository.countByCreadorIdAndInstitutionIsNull(userId);

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

            maxMiembros =
                    maxMiembrosSolicitado != null ? maxMiembrosSolicitado : maxAllowedMembers;
            tipoPlan = userPlan == TipoPlan.FREE ? TipoPlanComunidad.FREE : TipoPlanComunidad.PREMIUM;
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

    /** Crea una nueva comunidad verificando límites de plan (sin institutionId). */
    public Comunidad createCommunity(
            Long userId, String nombre, String descripcion, TipoGrupo tipoGrupo, String imagenUrl) {
        return createCommunity(userId, nombre, descripcion, tipoGrupo, imagenUrl, null, null);
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
    private es.us.meerkat.backend.entity.Institution obtenerInstitucion(Long institutionId) {
        return institutionRepository
                .findById(institutionId)
                .orElseThrow(() -> new IllegalArgumentException("Institución no encontrada"));
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

        comunidadRepository.delete(comunidad);
    }

    /** Lista comunidades activas (públicas y privadas) con filtros opcionales. */
    @Transactional(readOnly = true)
    public Page<Comunidad> listActiveCommunities(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return comunidadRepository.findByNombreContainingIgnoreCaseAndEstado(
                    search, EstadoComunidad.ACTIVA, pageable);
        } else {
            return comunidadRepository.findByEstado(EstadoComunidad.ACTIVA, pageable);
        }
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

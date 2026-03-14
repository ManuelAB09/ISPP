package es.us.meerkat.backend.service;

import java.io.IOException;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.EstadoComunidad;
import es.us.meerkat.backend.entity.MiembroComunidad;
import es.us.meerkat.backend.entity.RolComunidad;
import es.us.meerkat.backend.entity.Suscripcion;
import es.us.meerkat.backend.entity.TipoGrupo;
import es.us.meerkat.backend.entity.TipoPlan;
import es.us.meerkat.backend.entity.TipoPlanComunidad;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.ComunidadRepository;
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
    private final AuthorizationService authorizationService;
    private final SuscripcionService suscripcionService;

    private static final int MAX_FREE_COMMUNITIES = 3;
    private static final int FREE_MAX_MEMBERS = 50;
    private static final int PREMIUM_MAX_MEMBERS = 200;

    /** Crea una nueva comunidad verificando límites de plan. */
    public Comunidad createCommunity(
            Long userId,
            String nombre,
            String descripcion,
            TipoGrupo tipoGrupo,
            String imagenUrl,
            Long institutionId) {
        // Validar que el usuario exista
        Usuario usuario =
                usuarioRepository
                        .findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Si es una comunidad institucional, no aplicar límites de FREE
        es.us.meerkat.backend.entity.Institution institution = null;
        TipoPlanComunidad tipoPlan = TipoPlanComunidad.FREE;
        Integer maxMiembros = FREE_MAX_MEMBERS;

        if (institutionId != null) {
            institution = obtenerInstitucion(institutionId);

            // Comunidades institucionales obtienen plan UNLIMITED
            tipoPlan = TipoPlanComunidad.UNLIMITED;
            maxMiembros = null; // Sin límite
        } else {
            // Validar límite de comunidades gratuitas para usuarios individuales
            long freeCommunities =
                    comunidadRepository.countByCreadorIdAndTipoPlan(userId, TipoPlanComunidad.FREE);

            Optional<Suscripcion> suscripcionOpt = suscripcionService.obtenerMiSuscripcion(userId);
            TipoPlan userPlan = suscripcionOpt.map(Suscripcion::getPlan).orElse(TipoPlan.FREE);

            if (userPlan == TipoPlan.FREE && freeCommunities >= MAX_FREE_COMMUNITIES) {
                throw new IllegalArgumentException(
                        "Se ha alcanzado el límite de 3 comunidades gratuitas. Actualiza a Premium"
                                + " para crear más.");
            }
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
        return createCommunity(userId, nombre, descripcion, tipoGrupo, imagenUrl, null);
    }

    /** Obtiene la institución del repositorio (requiere que exista InstitutionRepository). */
    private es.us.meerkat.backend.entity.Institution obtenerInstitucion(Long institutionId) {
        // Este método asume que existe un InstitutionRepository
        // Si no existe, será necesario crearlo o inyectarlo
        return new es.us.meerkat.backend.entity.Institution();
        // TODO: Implementar inyección de InstitutionRepository
    }

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
}

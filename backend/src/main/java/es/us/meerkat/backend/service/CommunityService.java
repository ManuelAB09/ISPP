package es.us.meerkat.backend.service;

import java.io.IOException;
import java.util.Base64;
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
            Long userId, String nombre, String descripcion, TipoGrupo tipoGrupo, String imagenUrl) {
        // Validar que el usuario exista
        Usuario usuario =
                usuarioRepository
                        .findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Validar límite de comunidades gratuitas
        if (usuarioRepository.findById(userId).isPresent()) {
            long freeCommunities =
                    comunidadRepository.countByCreadorIdAndTipoPlan(userId, TipoPlanComunidad.FREE);

            TipoPlan userPlan;
            if (suscripcionService.obtenerMiSuscripcion(userId).get() == null) {
                userPlan = TipoPlan.FREE;
            } else {
                userPlan = suscripcionService.obtenerMiSuscripcion(userId).get().getPlan();
            }
            if (userPlan == TipoPlan.FREE && freeCommunities >= MAX_FREE_COMMUNITIES) {
                throw new IllegalArgumentException(
                        "Se ha alcanzado el límite de 3 comunidades gratuitas. Actualiza a Premium"
                                + " para crear más.");
            }
        }

        // Crear comunidad por defecto como FREE
        Comunidad comunidad =
                Comunidad.builder()
                        .nombre(nombre)
                        .descripcion(descripcion)
                        .tipoGrupo(tipoGrupo)
                        .imagenUrl(imagenUrl)
                        .creador(usuario)
                        .tipoPlan(TipoPlanComunidad.FREE)
                        .estado(EstadoComunidad.ACTIVA)
                        .maxMiembros(FREE_MAX_MEMBERS)
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

    /** Obtiene una comunidad por ID, verificando visibilidad según tipo. */
    @Transactional(readOnly = true)
    public Comunidad getCommunityById(Long communityId, Long userId) {
        Comunidad comunidad =
                comunidadRepository
                        .findById(communityId)
                        .orElseThrow(() -> new IllegalArgumentException("Comunidad no encontrada"));

        // Verificar acceso: si es privada, solo miembros pueden ver todos los detalles
        if (comunidad.getTipoGrupo() == TipoGrupo.GRUPO_PRIVADO && userId != null) {
            if (!authorizationService.isMemberOf(userId, communityId)) {
                throw new IllegalArgumentException(
                        "No tienes permiso para acceder a esta comunidad privada");
            }
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

    /** Lista comunidades públicas con filtros opcionales. */
    @Transactional(readOnly = true)
    public Page<Comunidad> listPublicCommunities(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return comunidadRepository.findByTipoGrupoAndNombreContainingIgnoreCaseAndEstado(
                    TipoGrupo.COMUNIDAD_PUBLICA, search, EstadoComunidad.ACTIVA, pageable);
        } else {
            return comunidadRepository.findByTipoGrupoAndEstado(
                    TipoGrupo.COMUNIDAD_PUBLICA, EstadoComunidad.ACTIVA, pageable);
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

    /** Obtiene el aforo máximo de una comunidad. */
    @Transactional(readOnly = true)
    public int getMaxMembers(Long communityId) {
        return comunidadRepository
                .findById(communityId)
                .map(Comunidad::getMaxMiembros)
                .orElse(FREE_MAX_MEMBERS);
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

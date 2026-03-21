package es.us.meerkat.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.dto.AnuncioResponse;
import es.us.meerkat.backend.dto.CreateAnuncioRequest;
import es.us.meerkat.backend.dto.UpdateAnuncioRequest;
import es.us.meerkat.backend.entity.Anuncio;
import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.Notificacion;
import es.us.meerkat.backend.entity.PreferenciasNotificacion;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.AnuncioRepository;
import es.us.meerkat.backend.repository.ComunidadRepository;
import es.us.meerkat.backend.repository.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

/** Servicio para la gestión de anuncios en comunidades. */
@Service
@RequiredArgsConstructor
@Transactional
public class AnuncioService {

    private final AnuncioRepository anuncioRepository;
    private final ComunidadRepository comunidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuthorizationService authorizationService;
    private final MiembroComunidadRepository miembroComunidadRepository;
    private final NotificacionService notificacionService;
    private final PreferenciasNotificacionService preferenciasNotificacionService;
    private final EmailService emailService;

    /**
     * Crea un nuevo anuncio en una comunidad.
     *
     * @param userId ID del usuario administrador
     * @param communityId ID de la comunidad
     * @param request datos del anuncio
     * @return el anuncio creado
     * @throws IllegalArgumentException si el usuario no tiene permisos
     */
    public Anuncio createAnuncio(Long userId, Long communityId, CreateAnuncioRequest request) {
        // Verificar que el usuario es admin de la comunidad
        if (!authorizationService.isAdminOf(userId, communityId)) {
            throw new IllegalArgumentException(
                    "Solo administradores pueden crear anuncios en esta comunidad");
        }

        Usuario usuario =
                usuarioRepository
                        .findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Comunidad comunidad =
                comunidadRepository
                        .findById(communityId)
                        .orElseThrow(() -> new IllegalArgumentException("Comunidad no encontrada"));

        Anuncio anuncio =
                Anuncio.builder()
                        .titulo(request.titulo())
                        .contenido(request.contenido())
                        .usuario(usuario)
                        .comunidad(comunidad)
                        .permitirComentarios(
                                request.permitirComentarios() != null
                                        ? request.permitirComentarios()
                                        : true)
                        .build();

        Anuncio saved = anuncioRepository.save(anuncio);

        // Notificar a todos los miembros de la comunidad excepto el creador
        java.util.List<Usuario> miembros =
                miembroComunidadRepository.findMiembrosMasAntiguosEnComunidad(
                        comunidad.getId(), usuario.getId());
        for (Usuario miembro : miembros) {
            // Usar la misma lógica de imagen que en la pestaña de comunidades
            String imagenUrl = comunidad.getImagenUrl();
            if (imagenUrl != null && (imagenUrl.equalsIgnoreCase("empty") || imagenUrl.isBlank())) {
                imagenUrl = null; // O pon aquí una URL por defecto si tienes una
            }
            Notificacion notif =
                    Notificacion.builder()
                            .usuario(miembro)
                            .titulo("Nuevo anuncio en tu comunidad")
                            .mensaje(saved.getTitulo())
                            .tipo("ANUNCIO")
                            .anuncioId(saved.getId())
                            .comunidadId(comunidad.getId())
                            .comunidadNombre(comunidad.getNombre())
                            .comunidadImagenUrl(imagenUrl)
                            .build();
            notificacionService.crearYNotificar(notif);
            notificarAnuncioPorEmail(saved, comunidad, usuario, miembro);
        }
        return saved;
    }

    private void notificarAnuncioPorEmail(
            final Anuncio anuncio,
            final Comunidad comunidad,
            final Usuario autor,
            final Usuario miembro) {
        if (miembro == null || miembro.getId() == null || miembro.getEmail() == null) {
            return;
        }

        try {
            final PreferenciasNotificacion preferencias =
                    preferenciasNotificacionService.getOrCreate(miembro.getId());
            final boolean puedeRecibir =
                    Boolean.TRUE.equals(preferencias.getEmailsActivados())
                            && Boolean.TRUE.equals(preferencias.getNotificarAnuncios());

            if (!puedeRecibir) {
                return;
            }

            emailService.sendCommunityAnnouncementEmail(miembro, comunidad, autor, anuncio);
        } catch (Exception ignored) {
            // No interrumpir la creación del anuncio si falla el envío de email.
        }
    }

    /**
     * Obtiene los anuncios de una comunidad de forma paginada.
     *
     * @param communityId ID de la comunidad
     * @param pageable paginación
     * @return página de anuncios
     */
    @Transactional(readOnly = true)
    public Page<Anuncio> getAnunciosByCommunity(Long communityId, Pageable pageable) {
        Comunidad comunidad =
                comunidadRepository
                        .findById(communityId)
                        .orElseThrow(() -> new IllegalArgumentException("Comunidad no encontrada"));

        return anuncioRepository.findByComunidadOrderByCreatedAtDesc(comunidad, pageable);
    }

    /**
     * Obtiene un anuncio específico.
     *
     * @param anuncioId ID del anuncio
     * @return el anuncio
     * @throws IllegalArgumentException si no existe
     */
    @Transactional(readOnly = true)
    public Anuncio getAnuncioById(Long anuncioId) {
        return anuncioRepository
                .findById(anuncioId)
                .orElseThrow(() -> new IllegalArgumentException("Anuncio no encontrado"));
    }

    /**
     * Actualiza un anuncio existente.
     *
     * @param userId ID del usuario administrador
     * @param anuncioId ID del anuncio
     * @param request datos a actualizar
     * @return el anuncio actualizado
     * @throws IllegalArgumentException si el usuario no tiene permisos
     */
    public Anuncio updateAnuncio(Long userId, Long anuncioId, UpdateAnuncioRequest request) {
        Anuncio anuncio = getAnuncioById(anuncioId);

        // Verificar que el usuario es admin de la comunidad o creador del anuncio
        if (!authorizationService.isAdminOf(userId, anuncio.getComunidad().getId())
                && !anuncio.getUsuario().getId().equals(userId)) {
            throw new IllegalArgumentException("No tienes permisos para actualizar este anuncio");
        }

        if (request.titulo() != null && !request.titulo().isBlank()) {
            anuncio.setTitulo(request.titulo());
        }

        if (request.contenido() != null && !request.contenido().isBlank()) {
            anuncio.setContenido(request.contenido());
        }

        if (request.permitirComentarios() != null) {
            anuncio.setPermitirComentarios(request.permitirComentarios());
        }

        return anuncioRepository.save(anuncio);
    }

    /**
     * Elimina un anuncio.
     *
     * @param userId ID del usuario administrador
     * @param anuncioId ID del anuncio
     * @throws IllegalArgumentException si el usuario no tiene permisos
     */
    public void deleteAnuncio(Long userId, Long anuncioId) {
        Anuncio anuncio = getAnuncioById(anuncioId);

        // Verificar que el usuario es admin de la comunidad o creador del anuncio
        if (!authorizationService.isAdminOf(userId, anuncio.getComunidad().getId())
                && !anuncio.getUsuario().getId().equals(userId)) {
            throw new IllegalArgumentException("No tienes permisos para eliminar este anuncio");
        }

        anuncioRepository.delete(anuncio);
    }

    /**
     * Convierte una entidad Anuncio a su DTO de respuesta.
     *
     * @param anuncio el anuncio
     * @return el DTO
     */
    public AnuncioResponse toResponse(Anuncio anuncio) {
        return new AnuncioResponse(
                anuncio.getId(),
                anuncio.getTitulo(),
                anuncio.getContenido(),
                new es.us.meerkat.backend.dto.UserSimpleResponse(
                        anuncio.getUsuario().getId(),
                        anuncio.getUsuario().getNombre(),
                        anuncio.getUsuario().getEmail(),
                        anuncio.getUsuario().getFoto()),
                anuncio.getPermitirComentarios(),
                anuncio.getEditado(),
                anuncio.getCreatedAt(),
                anuncio.getUpdatedAt());
    }
}

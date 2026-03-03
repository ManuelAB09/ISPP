package es.us.meerkat.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.dto.EnviarMensajeComunidadRequest;
import es.us.meerkat.backend.dto.MensajeComunidadResponse;
import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.MensajeComunidad;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.ComunidadRepository;
import es.us.meerkat.backend.repository.MensajeComunidadRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

/** Servicio para la gestión de mensajes en chats de comunidades. */
@Service
@RequiredArgsConstructor
public class MensajeComunidadService {

    private final MensajeComunidadRepository mensajeComunidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final ComunidadRepository comunidadRepository;

    /**
     * Envía un mensaje en el chat de una comunidad.
     *
     * @param usuarioId ID del usuario que envía el mensaje.
     * @param request datos del mensaje (comunidadId, contenido).
     * @return respuesta con la información del mensaje guardado.
     * @throws RuntimeException si el usuario o comunidad no existen.
     */
    @Transactional
    public MensajeComunidadResponse enviarMensaje(
            final Long usuarioId, final EnviarMensajeComunidadRequest request) {

        final Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        final Comunidad comunidad =
                comunidadRepository
                        .findById(request.getComunidadId())
                        .orElseThrow(() -> new RuntimeException("Comunidad no encontrada"));

        final MensajeComunidad mensaje =
                MensajeComunidad.builder()
                        .contenido(request.getContenido())
                        .usuario(usuario)
                        .comunidad(comunidad)
                        .editado(false)
                        .build();

        final MensajeComunidad saved = mensajeComunidadRepository.save(mensaje);
        return mapToResponse(saved);
    }

    /**
     * Envía un archivo en el chat de una comunidad.
     *
     * @param usuarioId ID del usuario que envía el archivo.
     * @param comunidadId ID de la comunidad destino.
     * @param contenido texto opcional del mensaje.
     * @param archivoUrl URL del archivo subido.
     * @param archivoNombre nombre original del archivo.
     * @param archivoMimeType tipo MIME del archivo.
     * @param archivoTamano tamaño del archivo en bytes.
     * @return respuesta con el mensaje creado.
     */
    @Transactional
    public MensajeComunidadResponse enviarArchivo(
            final Long usuarioId,
            final Long comunidadId,
            final String contenido,
            final String archivoNombre,
            final String archivoMimeType,
            final Long archivoTamano,
            final byte[] archivoData) {

        if (archivoData == null || archivoData.length == 0) {
            throw new IllegalArgumentException("El contenido del archivo es obligatorio");
        }

        final Usuario usuario =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        final Comunidad comunidad =
                comunidadRepository
                        .findById(comunidadId)
                        .orElseThrow(() -> new RuntimeException("Comunidad no encontrada"));

        final String contenidoFinal =
                (contenido == null || contenido.isBlank())
                        ? "[Adjunto] " + archivoNombre
                        : contenido;

        final MensajeComunidad mensaje =
                MensajeComunidad.builder()
                        .contenido(contenidoFinal)
                        .archivoNombre(archivoNombre)
                        .archivoMimeType(archivoMimeType)
                        .archivoTamano(archivoTamano)
                        .archivoData(archivoData)
                        .usuario(usuario)
                        .comunidad(comunidad)
                        .editado(false)
                        .build();

        final MensajeComunidad saved = mensajeComunidadRepository.save(mensaje);
        saved.setArchivoUrl(
                "/api/v1/comunidades/" + comunidadId + "/mensajes/" + saved.getId() + "/archivo");
        mensajeComunidadRepository.save(saved);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public MensajeComunidadArchivo obtenerArchivo(
            final Long usuarioId, final Long comunidadId, final Long mensajeId) {

        usuarioRepository
                .findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        final MensajeComunidad mensaje =
                mensajeComunidadRepository
                        .findById(mensajeId)
                        .orElseThrow(() -> new RuntimeException("Mensaje no encontrado"));

        if (!mensaje.getComunidad().getId().equals(comunidadId)) {
            throw new IllegalArgumentException("El mensaje no pertenece a la comunidad indicada");
        }

        if (mensaje.getArchivoData() == null || mensaje.getArchivoData().length == 0) {
            throw new IllegalArgumentException("El mensaje no contiene archivo");
        }

        return new MensajeComunidadArchivo(
                mensaje.getArchivoData(), mensaje.getArchivoNombre(), mensaje.getArchivoMimeType());
    }

    /**
     * Obtiene el historial de mensajes de una comunidad.
     *
     * @param comunidadId ID de la comunidad.
     * @return lista de mensajes ordenados por fecha de creación (ascendente).
     */
    @Transactional(readOnly = true)
    public List<MensajeComunidadResponse> obtenerHistorial(final Long comunidadId) {

        final List<MensajeComunidad> mensajes =
                mensajeComunidadRepository.findByComunidadIdOrderByCreatedAtAsc(comunidadId);

        return mensajes.stream().map(this::mapToResponse).toList();
    }

    /**
     * Edita un mensaje de comunidad (solo el autor puede hacerlo).
     *
     * @param usuarioId ID del usuario que intenta editar.
     * @param mensajeId ID del mensaje a editar.
     * @param nuevoContenido nuevo contenido del mensaje.
     * @return respuesta con la información del mensaje actualizado.
     * @throws RuntimeException si el mensaje no existe o el usuario no es el autor.
     */
    @Transactional
    public MensajeComunidadResponse editarMensaje(
            final Long usuarioId, final Long mensajeId, final String nuevoContenido) {

        final MensajeComunidad mensaje =
                mensajeComunidadRepository
                        .findById(mensajeId)
                        .orElseThrow(() -> new RuntimeException("Mensaje no encontrado"));

        if (!mensaje.getUsuario().getId().equals(usuarioId)) {
            throw new RuntimeException("No tienes permiso para editar este mensaje");
        }

        mensaje.editar(nuevoContenido);
        final MensajeComunidad updated = mensajeComunidadRepository.save(mensaje);
        return mapToResponse(updated);
    }

    /**
     * Elimina un mensaje de comunidad (solo el autor puede hacerlo).
     *
     * @param usuarioId ID del usuario que intenta eliminar.
     * @param mensajeId ID del mensaje a eliminar.
     * @throws RuntimeException si el mensaje no existe o el usuario no es el autor.
     */
    @Transactional
    public void eliminarMensaje(final Long usuarioId, final Long mensajeId) {

        final MensajeComunidad mensaje =
                mensajeComunidadRepository
                        .findById(mensajeId)
                        .orElseThrow(() -> new RuntimeException("Mensaje no encontrado"));

        if (!mensaje.getUsuario().getId().equals(usuarioId)) {
            throw new RuntimeException("No tienes permiso para eliminar este mensaje");
        }

        mensajeComunidadRepository.delete(mensaje);
    }

    /**
     * Convierte una entidad MensajeComunidad a su DTO de respuesta.
     *
     * @param mensaje entidad del mensaje.
     * @return DTO de respuesta.
     */
    private MensajeComunidadResponse mapToResponse(final MensajeComunidad mensaje) {
        return MensajeComunidadResponse.builder()
                .id(mensaje.getId())
                .contenido(mensaje.getContenido())
                .editado(mensaje.getEditado())
                .createdAt(mensaje.getCreatedAt())
                .editedAt(mensaje.getEditedAt())
                .usuarioId(mensaje.getUsuario().getId())
                .usuarioNombre(mensaje.getUsuario().getNombre())
                .usuarioFoto(mensaje.getUsuario().getFoto())
                .comunidadId(mensaje.getComunidad().getId())
                .archivoUrl(mensaje.getArchivoUrl())
                .archivoNombre(mensaje.getArchivoNombre())
                .archivoMimeType(mensaje.getArchivoMimeType())
                .archivoTamano(mensaje.getArchivoTamano())
                .build();
    }

    public record MensajeComunidadArchivo(byte[] data, String nombre, String mimeType) {}
}

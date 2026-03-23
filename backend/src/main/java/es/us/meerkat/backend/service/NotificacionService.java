package es.us.meerkat.backend.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.entity.Notificacion;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.NotificacionRepository;

@Service
public class NotificacionService {
    @Autowired private NotificacionRepository notificacionRepository;

    @Autowired private SimpMessagingTemplate messagingTemplate;

    public List<Notificacion> obtenerNotificaciones(Usuario usuario) {
        return notificacionRepository.findByUsuarioOrderByCreatedAtDesc(usuario);
    }

    @Transactional
    public boolean marcarComoLeida(Long id, Long usuarioId) {
        return notificacionRepository
                .findByIdAndUsuarioId(id, usuarioId)
                .map(
                        n -> {
                            n.setLeida(true);
                            notificacionRepository.save(n);
                            return true;
                        })
                .orElse(false);
    }

    @Transactional
    public Notificacion crearYNotificar(Notificacion notificacion) {
        Notificacion guardada = notificacionRepository.save(notificacion);
        // Enviar por WebSocket al usuario
        messagingTemplate.convertAndSendToUser(
                notificacion.getUsuario().getId().toString(), "/queue/notificaciones", guardada);
        return guardada;
    }
}

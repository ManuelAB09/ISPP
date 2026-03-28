package es.us.meerkat.backend.service.notifications;

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
        // Enviar por WebSocket solo si el usuario tiene las notificaciones push activadas
        Boolean pushEnabled = notificacion.getUsuario().getNotificacionesPush();
        if (pushEnabled == null || pushEnabled) {
            messagingTemplate.convertAndSendToUser(
                    notificacion.getUsuario().getId().toString(),
                    "/queue/notificaciones",
                    guardada);
        }
        return guardada;
    }
}

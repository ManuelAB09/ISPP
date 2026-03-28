package es.us.meerkat.backend.repository.notifications;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.Notificacion;
import es.us.meerkat.backend.entity.Usuario;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findByUsuarioOrderByCreatedAtDesc(Usuario usuario);

    List<Notificacion> findByUsuarioAndLeidaFalse(Usuario usuario);

    Optional<Notificacion> findByIdAndUsuarioId(Long id, Long usuarioId);
}

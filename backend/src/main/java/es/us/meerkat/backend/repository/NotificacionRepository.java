package es.us.meerkat.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.Notificacion;
import es.us.meerkat.backend.entity.Usuario;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findByUsuarioOrderByCreatedAtDesc(Usuario usuario);

    List<Notificacion> findByUsuarioAndLeidaFalse(Usuario usuario);
}

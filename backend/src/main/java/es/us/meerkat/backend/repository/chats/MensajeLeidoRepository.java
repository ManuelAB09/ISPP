package es.us.meerkat.backend.repository.chats;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.us.meerkat.backend.entity.Mensaje;
import es.us.meerkat.backend.entity.MensajeLeido;
import es.us.meerkat.backend.entity.Usuario;

public interface MensajeLeidoRepository extends JpaRepository<MensajeLeido, Long> {
    Optional<MensajeLeido> findByMensajeAndUsuario(Mensaje mensaje, Usuario usuario);

    @Query(
            "SELECT ml FROM MensajeLeido ml WHERE ml.usuario.id = :usuarioId AND ml.mensaje.id IN"
                    + " :mensajeIds")
    List<MensajeLeido> findByUsuarioAndMensajeIds(
            @Param("usuarioId") Long usuarioId, @Param("mensajeIds") List<Long> mensajeIds);

    @Query(
            "SELECT ml.mensaje.id FROM MensajeLeido ml "
                    + "WHERE ml.usuario.id = :usuarioId "
                    + "AND ml.mensaje.id IN :mensajeIds")
    List<Long> findMensajeIdsLeidosByUsuario(
            @Param("usuarioId") Long usuarioId, @Param("mensajeIds") List<Long> mensajeIds);

    long countByMensajeAndUsuario(Mensaje mensaje, Usuario usuario);
}

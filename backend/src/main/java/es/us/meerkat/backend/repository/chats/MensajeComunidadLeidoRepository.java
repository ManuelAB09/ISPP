package es.us.meerkat.backend.repository.chats;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.us.meerkat.backend.entity.chats.MensajeComunidad;
import es.us.meerkat.backend.entity.chats.MensajeComunidadLeido;
import es.us.meerkat.backend.entity.users.Usuario;

public interface MensajeComunidadLeidoRepository
        extends JpaRepository<MensajeComunidadLeido, Long> {
    Optional<MensajeComunidadLeido> findByMensajeComunidadAndUsuario(
            MensajeComunidad mensaje, Usuario usuario);

    @Query(
            "SELECT ml FROM MensajeComunidadLeido ml "
                    + "WHERE ml.usuario.id = :usuarioId "
                    + "AND ml.mensajeComunidad.id IN :mensajeIds")
    List<MensajeComunidadLeido> findByUsuarioAndMensajeIds(
            @Param("usuarioId") Long usuarioId, @Param("mensajeIds") List<Long> mensajeIds);

    @Query(
            "SELECT ml.mensajeComunidad.id FROM MensajeComunidadLeido ml "
                    + "WHERE ml.usuario.id = :usuarioId "
                    + "AND ml.mensajeComunidad.id IN :mensajeIds")
    List<Long> findMensajeIdsLeidosByUsuario(
            @Param("usuarioId") Long usuarioId, @Param("mensajeIds") List<Long> mensajeIds);

    @Modifying
    @Query("DELETE FROM MensajeComunidadLeido ml WHERE ml.mensajeComunidad.id = :mensajeId")
    void deleteByMensajeComunidadId(@Param("mensajeId") Long mensajeId);

    long countByMensajeComunidadAndUsuario(MensajeComunidad mensaje, Usuario usuario);
}

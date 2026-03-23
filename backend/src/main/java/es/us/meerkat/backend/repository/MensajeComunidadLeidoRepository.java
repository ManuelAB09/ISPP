package es.us.meerkat.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.us.meerkat.backend.entity.MensajeComunidad;
import es.us.meerkat.backend.entity.MensajeComunidadLeido;
import es.us.meerkat.backend.entity.Usuario;

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

    long countByMensajeComunidadAndUsuario(MensajeComunidad mensaje, Usuario usuario);
}

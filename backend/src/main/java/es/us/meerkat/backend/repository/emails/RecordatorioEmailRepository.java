package es.us.meerkat.backend.repository.emails;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.us.meerkat.backend.entity.RecordatorioEmail;
import es.us.meerkat.backend.entity.TipoRecordatorio;

/**
 * Repositorio JPA para {@link RecordatorioEmail}.
 *
 * <p>Gestiona el log de envíos y la comprobación anti-duplicado.
 */
public interface RecordatorioEmailRepository extends JpaRepository<RecordatorioEmail, Long> {

    /**
     * Comprueba si ya se envió (o intentó enviar) un recordatorio de un tipo concreto para un
     * usuario y evento. Evita envíos duplicados si el scheduler se ejecuta varias veces en la misma
     * ventana de tiempo.
     *
     * @param eventoId ID del evento.
     * @param usuarioId ID del usuario.
     * @param tipo Tipo de recordatorio.
     * @return Optional con el registro si ya existe.
     */
    Optional<RecordatorioEmail> findByEventoIdAndUsuarioIdAndTipo(
            Long eventoId, Long usuarioId, TipoRecordatorio tipo);

    /**
     * Elimina registros de recordatorios con más de 30 días de antigüedad. Llamado por el scheduler
     * de limpieza.
     *
     * @param fechaLimite Registros anteriores a esta fecha serán eliminados.
     */
    @Modifying
    @Query("DELETE FROM RecordatorioEmail r WHERE r.enviadoAt < :fechaLimite")
    void deleteOldRecordatorios(@Param("fechaLimite") LocalDateTime fechaLimite);

    /** Elimina todos los recordatorios de un evento. */
    @Modifying
    @Query("DELETE FROM RecordatorioEmail r WHERE r.evento.id = :eventoId")
    void deleteByEventoId(@Param("eventoId") Long eventoId);
}

package es.us.meerkat.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.EstadoTransaccion;
import es.us.meerkat.backend.entity.TipoTransaccion;
import es.us.meerkat.backend.entity.TransaccionPago;

@Repository
public interface TransaccionPagoRepository extends JpaRepository<TransaccionPago, Long> {

    /** Verifica si existe una transacción pendiente para un tutor de un tipo específico. */
    boolean existsByTutorIdAndTipoAndEstado(
            Long tutorId, TipoTransaccion tipo, EstadoTransaccion estado);

    /**
     * Obtiene la última transacción de un tutor por tipo, ordenada por fecha de inicio descendente.
     */
    Optional<TransaccionPago> findTopByTutorIdAndTipoOrderByIniciadoAtDesc(
            Long tutorId, TipoTransaccion tipo);

    /** Obtiene el historial de pagos de un usuario, ordenado por fecha descendente. */
    Page<TransaccionPago> findByUsuarioIdOrderByIniciadoAtDesc(Long usuarioId, Pageable pageable);

    /** Obtiene una transacción específica del usuario. */
    Optional<TransaccionPago> findByIdAndUsuarioId(Long id, Long usuarioId);

    /** Elimina todas las transacciones de pago de un usuario. */
    void deleteByUsuarioId(Long usuarioId);

    Page<TransaccionPago> findByTutorIdOrderByIniciadoAtDesc(Long tutorId, Pageable pageable);
    /** Obtiene todas las transacciones completadas asociadas a un tutor (para ganancias). */
    List<TransaccionPago> findByTutorIdAndTipoAndEstadoOrderByCompletadoAtDesc(
            Long tutorId, TipoTransaccion tipo, EstadoTransaccion estado);

    /** Obtiene transacciones paginadas de un tutor por tipo y estado. */
    Page<TransaccionPago> findByTutorIdAndTipoAndEstadoOrderByCompletadoAtDesc(
            Long tutorId, TipoTransaccion tipo, EstadoTransaccion estado, Pageable pageable);
}

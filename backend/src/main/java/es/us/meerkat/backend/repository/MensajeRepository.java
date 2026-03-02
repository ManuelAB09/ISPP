package es.us.meerkat.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.us.meerkat.backend.entity.Mensaje;

public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    // Conversación entre dos usuarios sobre un tutor
    @Query(
            """
            SELECT m
            FROM Mensaje m
            WHERE m.tutor.id = :tutorId
              AND ((m.emisor.id = :usuarioId AND m.receptor.id = :tutorUserId)
               OR  (m.emisor.id = :tutorUserId AND m.receptor.id = :usuarioId))
            ORDER BY m.createdAt ASC
            """)
    List<Mensaje> findConversationWithTutor(
            @Param("tutorId") Long tutorId,
            @Param("usuarioId") Long usuarioId,
            @Param("tutorUserId") Long tutorUserId);

    // Todos los mensajes recibidos por un usuario
    List<Mensaje> findByReceptorIdOrderByCreatedAtDesc(Long usuarioId);

    List<Mensaje> findByEmisorIdAndReceptorIdOrEmisorIdAndReceptorIdOrderByCreatedAtAsc(
            Long emisor1, Long receptor1, Long emisor2, Long receptor2);
}

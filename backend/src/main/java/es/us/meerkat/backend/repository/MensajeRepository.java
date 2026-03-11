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

    // Todos los mensajes donde el usuario es emisor o receptor, ordenados por fecha descendente
    @Query(
            """
            SELECT m
            FROM Mensaje m
            LEFT JOIN FETCH m.emisor
            LEFT JOIN FETCH m.receptor
            WHERE m.emisor.id = :usuarioId OR m.receptor.id = :usuarioId
            ORDER BY m.createdAt DESC
            """)
    List<Mensaje> findAllConversations(@Param("usuarioId") Long usuarioId);

    /** Elimina todos los mensajes enviados por un usuario. */
    void deleteByEmisorId(Long usuarioId);

    /** Elimina todos los mensajes recibidos por un usuario. */
    void deleteByReceptorId(Long usuarioId);
}

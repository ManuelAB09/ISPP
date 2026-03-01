package es.us.meerkat.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import es.us.meerkat.backend.entity.Mensaje;

public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    // Conversación entre dos usuarios sobre un tutor
    List<Mensaje>
            findByTutorIdAndEmisorIdAndReceptorIdOrTutorIdAndEmisorIdAndReceptorIdOrderByCreatedAtAsc(
                    Long tutorId1,
                    Long emisor1,
                    Long receptor1,
                    Long tutorId2,
                    Long emisor2,
                    Long receptor2);

    // Todos los mensajes recibidos por un usuario
    List<Mensaje> findByReceptorIdOrderByCreatedAtDesc(Long usuarioId);

    List<Mensaje>
            findByEmisorIdAndReceptorIdOrEmisorIdAndReceptorIdOrderByCreatedAtAsc(
                    Long emisor1,
                    Long receptor1,
                    Long emisor2,
                    Long receptor2);
}

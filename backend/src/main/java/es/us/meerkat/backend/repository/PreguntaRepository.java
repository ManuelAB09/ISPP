package es.us.meerkat.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import es.us.meerkat.backend.entity.Pregunta;

@Repository
public interface PreguntaRepository extends JpaRepository<Pregunta, Long> {}

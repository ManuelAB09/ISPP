package es.us.meerkat.backend.dto;

/** Request DTO para calificar una entrega de tarea. */
public record CalificarEntregaRequest(Integer calificacion, String comentario) {}

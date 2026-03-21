package es.us.meerkat.backend.dto;

/** Request DTO para entregar una tarea. */
public record EntregarTareaRequest(String urlArchivo, String contenido) {}

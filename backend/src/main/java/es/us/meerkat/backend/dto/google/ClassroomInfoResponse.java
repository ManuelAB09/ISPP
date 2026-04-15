package es.us.meerkat.backend.dto.google;

/** Respuesta con la información del curso de Google Classroom vinculado a una comunidad. */
public record ClassroomInfoResponse(Long id, String courseId, String courseName, Boolean activa) {}

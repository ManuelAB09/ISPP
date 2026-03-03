package es.us.meerkat.backend.dto;

/** Respuesta con la información del curso de Google Classroom vinculado a una comunidad. */
public record ClassroomInfoResponse(Long id, String courseId, String courseName, Boolean activa) {}

package es.us.meerkat.backend.dto;

/** Respuesta al crear un usuario en Google Classroom. */
public record ClassroomUserResponse(
        String id, String userId, String fullName, String emailAddress) {}

package es.us.meerkat.backend.dto;

/** Petición para vincular un curso de Google Classroom a una comunidad. */
public record LinkClassroomRequest(
        String courseId,
        String courseName) {}

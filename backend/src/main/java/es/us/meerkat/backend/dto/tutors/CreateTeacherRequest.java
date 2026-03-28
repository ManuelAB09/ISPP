package es.us.meerkat.backend.dto.tutors;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Petición para crear un profesor en un curso de Google Classroom. */
public record CreateTeacherRequest(@JsonProperty("userId") String userId) {}

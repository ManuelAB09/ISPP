package es.us.meerkat.backend.dto.tutors;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateStudentRequest(
        @JsonProperty("userId") String userId,
        @JsonProperty("enrollmentCode") String enrollmentCode) {}

package es.us.meerkat.backend.dto;

/** Franja horaria ocupada de un tutor (para bloquear horas en el selector de reservas). */
public record HorarioOcupadoResponse(String horaInicio, String horaFin) {}

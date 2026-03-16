import { apiClient } from './client';

// ─── Disponibilidad ───────────────────────────────────────────────────────────

export const getDisponibilidadTutor = (tutorId) =>
  apiClient.get(`/api/v1/tutors/${tutorId}/disponibilidad`);

export const getDisponibilidadFecha = (tutorId, fecha) =>
  apiClient.get(`/api/v1/tutors/${tutorId}/disponibilidad-fecha?fecha=${fecha}`);

// ─── Reservas (alumno) ────────────────────────────────────────────────────────

export const crearReserva = (tutorId, data) =>
  apiClient.post(`/api/v1/tutors/${tutorId}/reservas`, data);

export const getMisReservasAlumno = (page = 0, size = 10) =>
  apiClient.get(`/api/v1/reservas/me?page=${page}&size=${size}`);

export const getReservaDetalle = (reservaId) =>
  apiClient.get(`/api/v1/reservas/${reservaId}`);

export const cancelarReserva = (reservaId, motivo = '') =>
  apiClient.delete(`/api/v1/reservas/${reservaId}?motivo=${encodeURIComponent(motivo)}`);

export const calificarReserva = (reservaId, calificacion, comentario) =>
  apiClient.post(`/api/v1/reservas/${reservaId}/calificar`, { calificacion, comentario });

// ─── Reservas (tutor) ─────────────────────────────────────────────────────────

export const getMisReservasTutor = (page = 0, size = 10) =>
  apiClient.get(`/api/v1/tutors/me/reservas?page=${page}&size=${size}`);

export const getProximasReservasTutor = (dias = 7) =>
  apiClient.get(`/api/v1/tutors/me/proximas-reservas?dias=${dias}`);

export const confirmarReserva = (reservaId) =>
  apiClient.put(`/api/v1/reservas/${reservaId}/confirmar`);

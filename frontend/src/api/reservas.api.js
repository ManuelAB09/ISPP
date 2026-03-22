import { apiClient } from './client';

/**
 * POST /api/v1/tutors/{tutorId}/reservas
 * Crear reserva de clase. Devuelve { paymentUrl } si hay pago via Stripe.
 * @param {number|string} tutorId
 * @param {{ fechaHora, duracionMinutos, modalidad, ubicacion, tema, descripcion, aceptarTerminos }} data
 */
export const crearReserva = (tutorId, data) =>
    apiClient.post(`/api/v1/tutors/${tutorId}/reservas`, data);

/**
 * PUT /api/v1/reservas/{reservaId}/confirmar
 * El tutor confirma una reserva pendiente.
 * @param {number|string} reservaId
 */
export const confirmarReserva = (reservaId) =>
    apiClient.put(`/api/v1/reservas/${reservaId}/confirmar`, {});

/**
 * DELETE /api/v1/reservas/{reservaId}
 * Cancelar una reserva (alumno o tutor).
 * @param {number|string} reservaId
 * @param {string} motivo
 */
export const cancelarReserva = (reservaId, motivo) => {
    const query = motivo ? `?motivo=${encodeURIComponent(motivo)}` : '';
    return apiClient.delete(`/api/v1/reservas/${reservaId}${query}`);
};

/**
 * GET /api/v1/reservas/me
 * Reservas del alumno autenticado.
 * @param {{ page, size }} params
 */
export const getMisReservasAlumno = (params = {}) => {
    const query = new URLSearchParams();
    if (params.page !== undefined) query.set('page', String(params.page));
    if (params.size !== undefined) query.set('size', String(params.size));
    const qs = query.toString();
    return apiClient.get(`/api/v1/reservas/me${qs ? '?' + qs : ''}`);
};

/**
 * GET /api/v1/tutors/me/reservas
 * Reservas del tutor autenticado.
 * @param {{ page, size }} params
 */
export const getMisReservasTutor = (params = {}) => {
    const query = new URLSearchParams();
    if (params.page !== undefined) query.set('page', String(params.page));
    if (params.size !== undefined) query.set('size', String(params.size));
    const qs = query.toString();
    return apiClient.get(`/api/v1/tutors/me/reservas${qs ? '?' + qs : ''}`);
};

/**
 * GET /api/v1/reservas/{reservaId}
 * Detalle de una reserva concreta.
 * @param {number|string} reservaId
 */
export const getReservaDetalle = (reservaId) =>
    apiClient.get(`/api/v1/reservas/${reservaId}`);

/**
 * GET /api/v1/tutors/{tutorId}/disponibilidad
 * Franjas de disponibilidad configuradas por el tutor.
 * @param {number|string} tutorId
 */
export const getDisponibilidadTutor = (tutorId) =>
    apiClient.get(`/api/v1/tutors/${tutorId}/disponibilidad`);

/**
 * GET /api/v1/tutors/{tutorId}/disponibilidad-fecha
 * Disponibilidad del tutor para una fecha concreta.
 * @param {number|string} tutorId
 * @param {string} fecha  YYYY-MM-DD
 */
export const getDisponibilidadTutorFecha = (tutorId, fecha) =>
    apiClient.get(`/api/v1/tutors/${tutorId}/disponibilidad-fecha?fecha=${encodeURIComponent(fecha)}`);

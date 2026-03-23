import { apiClient } from './client';

/** GET /api/v1/disponibilidad/tutor/{tutorId} — todas las franjas activas de un tutor */
export const getDisponibilidades = (tutorId) =>
    apiClient.get(`/api/v1/disponibilidad/tutor/${tutorId}`);

/** POST /api/v1/disponibilidad — crear nueva franja (tutor autenticado) */
export const crearDisponibilidad = (data) =>
    apiClient.post('/api/v1/disponibilidad', data);

/** PUT /api/v1/disponibilidad/{id} — actualizar franja */
export const actualizarDisponibilidad = (id, data) =>
    apiClient.put(`/api/v1/disponibilidad/${id}`, data);

/** DELETE /api/v1/disponibilidad/{id} — desactivar franja */
export const eliminarDisponibilidad = (id) =>
    apiClient.delete(`/api/v1/disponibilidad/${id}`);

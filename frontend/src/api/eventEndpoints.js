import axiosInstance from './axiosConfig';
/**
 * Lista eventos visibles en el mapa
 * GET /events/map
 */
export const listMapEvents = async () => {
  const response = await axiosInstance.get('/api/v1/events/map');
  return response.data;
};


// Helper para obtener el userId del usuario autenticado
const getUserId = () => localStorage.getItem('userId');

/**
 * Crea un nuevo evento en una comunidad
 * POST /communities/{communityId}/events
 */
export const createEvent = async (communityId, eventData) => {
  const response = await axiosInstance.post(`/api/v1/communities/${communityId}/events`, eventData);
  return response.data;
};

/**
 * Obtiene el detalle de un evento
 * GET /events/{eventId}
 */
export const getEventById = async (eventId) => {
  const response = await axiosInstance.get(`/api/v1/events/${eventId}`);
  return response.data;
};

/**
 * Actualiza un evento existente
 * PUT /events/{eventId}
 * Backend espera @RequestParam, se envían como query params
 */
export const updateEvent = async (eventId, eventData) => {
  const response = await axiosInstance.put(`/api/v1/events/${eventId}`, null, { params: eventData });
  return response.data;
};

/**
 * Lista eventos de una comunidad
 * GET /communities/{communityId}/events
 */
export const listCommunityEvents = async (communityId, params = {}) => {
  const response = await axiosInstance.get(`/api/v1/communities/${communityId}/events`, { params });
  return response.data;
};

/**
 * Explora eventos públicos
 * GET /events
 */
export const listPublicEvents = async (params = {}) => {
  const response = await axiosInstance.get('/api/v1/events', { params });
  return response.data;
};

/**
 * Cancela un evento
 * POST /events/{eventId}/cancel
 */
export const cancelEvent = async (eventId, motivo = '') => {
  const response = await axiosInstance.post(`/api/v1/events/${eventId}/cancel`, null, { params: { motivo } });
  return response.data;
};

/**
 * Confirma asistencia a un evento (apuntarse)
 * POST /events/{eventId}/attendance
 */
export const attendEvent = async (eventId) => {
  const usuarioId = getUserId();
  const response = await axiosInstance.post(`/api/v1/events/${eventId}/attendance`, null, { params: { usuarioId } });
  return response.data;
};

/**
 * Obtiene mi asistencia a un evento
 * GET /events/{eventId}/attendance/me
 */
export const getMyAttendance = async (eventId) => {
  const usuarioId = getUserId();
  try {
    const response = await axiosInstance.get(`/api/v1/events/${eventId}/attendance/me`, { params: { usuarioId } });
    return response.data;
  } catch (error) {
    if (error?.response?.status === 404) {
      return null;
    }
    throw error;
  }
};

/**
 * Cancela mi asistencia a un evento (desapuntarse)
 * DELETE /events/{eventId}/attendance/me
 */
export const cancelAttendance = async (eventId) => {
  const usuarioId = getUserId();
  const response = await axiosInstance.delete(`/api/v1/events/${eventId}/attendance/me`, { params: { usuarioId } });
  return response.data;
};

/**
 * Lista todos los asistentes de un evento
 * GET /events/{eventId}/attendance
 */
export const listAttendees = async (eventId) => {
  const response = await axiosInstance.get(`/api/v1/events/${eventId}/attendance`);
  return response.data;
};

/**
 * Lista solo los asistentes confirmados
 * GET /events/{eventId}/attendance/confirmed
 */
export const getConfirmedAttendees = async (eventId) => {
  const response = await axiosInstance.get(`/api/v1/events/${eventId}/attendance/confirmed`);
  return response.data;
};

/**
 * Obtiene el conteo de asistentes confirmados
 * GET /events/{eventId}/attendance/count
 */
export const getAttendanceCount = async (eventId) => {
  const response = await axiosInstance.get(`/api/v1/events/${eventId}/attendance/count`);
  return response.data;
};

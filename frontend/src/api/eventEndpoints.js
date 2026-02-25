import axiosInstance from './axiosConfig';

/**
 * Crea un nuevo evento en una comunidad
 * POST /communities/{communityId}/events
 * @param {number} communityId - ID de la comunidad
 * @param {Object} eventData - Datos del evento
 * @param {string} eventData.titulo - Título del evento
 * @param {string} [eventData.descripcion] - Descripción
 * @param {string} eventData.fechaHora - Fecha y hora ISO 8601
 * @param {string} [eventData.fechaFin] - Fecha fin ISO 8601
 * @param {string} [eventData.ubicacion] - Dirección o lugar
 * @param {number} [eventData.latitud] - Latitud
 * @param {number} [eventData.longitud] - Longitud
 * @param {number} eventData.aforo - Aforo máximo
 * @param {string} [eventData.queLlevar] - Qué llevar al evento
 * @param {boolean} [eventData.visibleEnMapa] - Visible en mapa
 * @param {boolean} [eventData.esVirtual] - Si es virtual/online
 * @param {string} [eventData.enlaceVirtual] - Enlace virtual
 * @returns {Promise} - EventDetailResponse
 */
export const createEvent = async (communityId, eventData) => {
  const response = await axiosInstance.post(`/communities/${communityId}/events`, eventData);
  return response.data;
};

/**
 * Obtiene el detalle de un evento
 * GET /events/{eventId}
 * @param {number} eventId - ID del evento
 * @returns {Promise} - EventDetailResponse
 */
export const getEventById = async (eventId) => {
  const response = await axiosInstance.get(`/events/${eventId}`);
  return response.data;
};

/**
 * Actualiza un evento existente
 * PUT /events/{eventId}
 * @param {number} eventId - ID del evento
 * @param {Object} eventData - Datos a actualizar
 * @returns {Promise} - EventDetailResponse
 */
export const updateEvent = async (eventId, eventData) => {
  const response = await axiosInstance.put(`/events/${eventId}`, eventData);
  return response.data;
};

/**
 * Lista eventos de una comunidad
 * GET /communities/{communityId}/events
 * @param {number} communityId - ID de la comunidad
 * @param {Object} [params] - Parámetros de filtrado
 * @param {string} [params.desde] - Fecha desde (ISO 8601)
 * @param {string} [params.hasta] - Fecha hasta (ISO 8601)
 * @param {boolean} [params.cancelados] - Incluir cancelados
 * @param {number} [params.page] - Página
 * @param {number} [params.size] - Tamaño de página
 * @returns {Promise} - EventListResponse
 */
export const listCommunityEvents = async (communityId, params = {}) => {
  const response = await axiosInstance.get(`/communities/${communityId}/events`, { params });
  return response.data;
};

/**
 * Explora eventos públicos
 * GET /events
 * @param {Object} [params] - Parámetros de búsqueda
 * @param {string} [params.search] - Buscar por título o descripción
 * @param {number} [params.lat] - Latitud
 * @param {number} [params.lng] - Longitud
 * @param {number} [params.radio] - Radio en km
 * @param {string} [params.desde] - Fecha desde
 * @param {string} [params.hasta] - Fecha hasta
 * @param {number} [params.page] - Página
 * @param {number} [params.size] - Tamaño de página
 * @returns {Promise} - EventListResponse
 */
export const listPublicEvents = async (params = {}) => {
  const response = await axiosInstance.get('/events', { params });
  return response.data;
};

/**
 * Cancela un evento
 * POST /events/{eventId}/cancel
 * @param {number} eventId - ID del evento
 * @param {Object} [body] - Motivo de cancelación
 * @param {string} [body.motivo] - Motivo
 * @returns {Promise} - EventDetailResponse
 */
export const cancelEvent = async (eventId, body = {}) => {
  const response = await axiosInstance.post(`/events/${eventId}/cancel`, body);
  return response.data;
};

/**
 * Registra asistencia a un evento (apuntarse)
 * POST /events/{eventId}/attendance
 * @param {number} eventId - ID del evento
 * @returns {Promise}
 */
export const attendEvent = async (eventId) => {
  const response = await axiosInstance.post(`/events/${eventId}/attendance`);
  return response.data;
};

/**
 * Cancela asistencia a un evento (desapuntarse)
 * DELETE /events/{eventId}/attendance
 * @param {number} eventId - ID del evento
 * @returns {Promise}
 */
export const cancelAttendance = async (eventId) => {
  const response = await axiosInstance.delete(`/events/${eventId}/attendance`);
  return response.data;
};

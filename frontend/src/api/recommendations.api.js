import { apiClient } from './client';

/**
 * GET /api/recommendations/page
 * Obtener todas las secciones de recomendaciones de una vez:
 * paraTi, profesores, contenidos, cuestionarios, comunidades
 */
export const getRecommendationsPage = () => {
  return apiClient.get('/api/recommendations/page');
};

/**
 * GET /api/recommendations/profesores
 * Obtener recomendaciones de profesores
 * @param {Object} params - { page: number, size: number }
 */
export const getRecommendedTutors = (params = {}) => {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set('page', String(params.page));
  if (params.size !== undefined) query.set('size', String(params.size));
  
  const queryString = query.toString();
  return apiClient.get(`/api/recommendations/profesores${queryString ? '?' + queryString : ''}`);
};

/**
 * GET /api/recommendations/contenido
 * Obtener recomendaciones de contenido
 * @param {Object} params - { page: number, size: number }
 */
export const getRecommendedContent = (params = {}) => {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set('page', String(params.page));
  if (params.size !== undefined) query.set('size', String(params.size));
  
  const queryString = query.toString();
  return apiClient.get(`/api/recommendations/contenido${queryString ? '?' + queryString : ''}`);
};

/**
 * GET /api/recommendations/cuestionarios
 * Obtener recomendaciones de cuestionarios
 * @param {Object} params - { page: number, size: number }
 */
export const getRecommendedQuestions = (params = {}) => {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set('page', String(params.page));
  if (params.size !== undefined) query.set('size', String(params.size));
  
  const queryString = query.toString();
  return apiClient.get(`/api/recommendations/cuestionarios${queryString ? '?' + queryString : ''}`);
};

/**
 * GET /api/recommendations/comunidades
 * Obtener recomendaciones de comunidades
 * @param {Object} params - { page: number, size: number }
 */
export const getRecommendedCommunities = (params = {}) => {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set('page', String(params.page));
  if (params.size !== undefined) query.set('size', String(params.size));
  
  const queryString = query.toString();
  return apiClient.get(`/api/recommendations/comunidades${queryString ? '?' + queryString : ''}`);
};

/**
 * GET /api/recommendations
 * Obtener todas las recomendaciones activas
 * @param {Object} params - { page: number, size: number }
 */
export const getActiveRecommendations = (params = {}) => {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set('page', String(params.page));
  if (params.size !== undefined) query.set('size', String(params.size));
  
  const queryString = query.toString();
  return apiClient.get(`/api/recommendations${queryString ? '?' + queryString : ''}`);
};

/**
 * GET /api/recommendations/no-vistas
 * Obtener recomendaciones no vistas
 * @param {Object} params - { page: number, size: number }
 */
export const getUnseenRecommendations = (params = {}) => {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set('page', String(params.page));
  if (params.size !== undefined) query.set('size', String(params.size));
  
  const queryString = query.toString();
  return apiClient.get(`/api/recommendations/no-vistas${queryString ? '?' + queryString : ''}`);
};

/**
 * POST /api/recommendations/{id}/vista
 * Marcar una recomendación como vista
 * @param {number|string} id - ID de la recomendación
 */
export const markRecommendationAsSeen = (id) => {
  return apiClient.post(`/api/recommendations/${id}/vista`);
};

/**
 * DELETE /api/recommendations/{id}
 * Eliminar/descartar una recomendación
 * @param {number|string} id - ID de la recomendación
 */
export const deleteRecommendation = (id) => {
  return apiClient.delete(`/api/recommendations/${id}`);
};

/**
 * POST /api/recommendations/{id}/feedback
 * Dar feedback sobre una recomendación
 * @param {number|string} id - ID de la recomendación
 * @param {Object} feedback - { esUtil: boolean, comentario: string, satisfaccion: number }
 */
export const giveRecommendationFeedback = (id, feedback) => {
  return apiClient.post(`/api/recommendations/${id}/feedback`, feedback);
};

/**
 * POST /api/recommendations/actividad
 * Registrar actividad del usuario (búsqueda, clic, visualización)
 * @param {Object} activity - { tipoActividad: string, categoriaObjeto: string, terminosBusqueda: string }
 */
export const registerUserActivity = (activity) => {
  return apiClient.post('/api/recommendations/actividad', activity);
};

/**
 * POST /api/recommendations/refresh
 * Forzar regeneración de recomendaciones (llamar tras actualizar perfil o intereses)
 */
export const refreshRecommendations = () => {
  return apiClient.post('/api/recommendations/refresh');
};

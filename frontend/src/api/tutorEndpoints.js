import { apiClient } from './client';

/**
 * GET /api/v1/tutors
 * Listar tutores (por defecto verificados)
 * @param {Object} params - { especialidad, tarifaMin, tarifaMax, page, size }
 */
export const getVerifiedTutors = (params = {}) => {
  const query = new URLSearchParams();
  if (params.especialidad) query.set('especialidad', params.especialidad);
  if (params.tarifaMin) query.set('tarifaMin', String(params.tarifaMin));
  if (params.tarifaMax) query.set('tarifaMax', String(params.tarifaMax));
  if (params.page !== undefined) query.set('page', String(params.page));
  if (params.size !== undefined) query.set('size', String(params.size));

  const queryString = query.toString();
  return apiClient.get(`/api/v1/tutors${queryString ? '?' + queryString : ''}`);
};

/**
 * POST /api/v1/tutors
 * Crear perfil de tutor para el usuario autenticado
 * @param {Object} data - { especialidades, tarifaHora, disponibilidad, bio }
 */
export const createTutorProfile = (data) => {
  return apiClient.post('/api/v1/tutors', data);
};

/**
 * GET /api/v1/tutors/{tutorId}
 * Obtener perfil público de un tutor
 * @param {number|string} id
 */
export const getTutorById = (id) => {
  return apiClient.get(`/api/v1/tutors/${id}`);
};

/**
 * PUT /api/v1/tutors/{tutorId}
 * Actualizar perfil de tutor
 * @param {number|string} tutorId
 * @param {Object} data
 */
export const updateTutorProfile = (tutorId, data) => {
  return apiClient.put('/api/v1/tutors/me', data);
};

/**
 * GET /api/v1/tutors/me/{tutorId}/verification-status
 * Consultar estado de verificación
 * @param {number|string} tutorId
 */
export const getTutorVerificationStatus = (tutorId) => {
  return apiClient.get(`/api/v1/tutors/me/${tutorId}/verification-status`);
};

/**
 * POST /api/v1/tutors/me/{tutorId}/verification
 * Solicitar verificación de tutor
 * @param {number|string} tutorId
 */
export const requestTutorVerification = (tutorId) => {
  return apiClient.post(`/api/v1/tutors/me/${tutorId}/verification`, {});
};

/**
 * POST /api/v1/tutors/me/verificar
 * Verificar tutor directamente (activa verificado=true en BD)
 */
export const verificarTutor = () => {
  return apiClient.post('/api/v1/tutors/me/verificar', {});
};

/**
 * GET /api/v1/tutors/me
 * Obtener perfiles de tutor del usuario autenticado
 */
export const getMyTutorProfiles = () => {
  return apiClient.get('/api/v1/tutors/me');
};

/**
 * GET /api/v1/tutors/me/{tutorId}
 * Obtener un perfil de tutor específico del usuario autenticado
 * @param {number|string} tutorId
 */
export const getMyTutorProfile = (tutorId) => {
  return apiClient.get(`/api/v1/tutors/me/${tutorId}`);
};

// ═══════════════════════════════════════════════════════════
// GESTIÓN DE SOLICITUDES DE CONTRATACIÓN
// ═══════════════════════════════════════════════════════════

/**
 * GET /api/v1/tutors/me/hiring-requests
 * Obtener solicitudes de contratación pendientes para el tutor autenticado
 * @param {Object} params - { page?, size? }
 */
export const getMyHiringRequests = (params = {}) => {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set('page', String(params.page));
  if (params.size !== undefined) query.set('size', String(params.size));

  const queryString = query.toString();
  return apiClient.get(`/api/v1/tutors/me/hiring-requests${queryString ? '?' + queryString : ''}`);
};

/**
 * POST /api/v1/tutors/me/hiring-requests/{requestId}/accept
 * Aceptar una solicitud de contratación
 * @param {number} requestId - ID de la solicitud
 */
export const acceptHiringRequest = (requestId) => {
  return apiClient.post(`/api/v1/tutors/me/hiring-requests/${requestId}/accept`, {});
};

/**
 * POST /api/v1/tutors/me/hiring-requests/{requestId}/reject
 * Rechazar una solicitud de contratación
 * @param {number} requestId - ID de la solicitud
 * @param {string} motivo - Motivo del rechazo
 */
export const rejectHiringRequest = (requestId, motivo) => {
  return apiClient.post(`/api/v1/tutors/me/hiring-requests/${requestId}/reject`, { motivo });
};
/**
 * POST /api/v1/tutors/me/verification
 * Solicitar verificación de tutor (genera URL de pago Stripe)
 */
export const requestVerification = () => {
  return apiClient.post('/api/v1/tutors/me/verification', {});
};

/**
 * POST /api/v1/tutors/me/verify-verification-session
 * Confirmar pago de verificación con sessionId de Stripe
 */
export const verifyVerificationSession = (sessionId) => {
  return apiClient.post('/api/v1/tutors/me/verify-verification-session', { sessionId });
};

/**
 * POST /api/v1/tutors/me/connect
 * Inicia onboarding Stripe Connect
 */
export const connectStripeAccount = () => {
  return apiClient.post('/api/v1/tutors/me/connect', {});
};

/**
 * POST /api/v1/tutors/me/connect/confirm
 * Confirma que el onboarding se completó
 */
export const confirmStripeConnect = () => {
  return apiClient.post('/api/v1/tutors/me/connect/confirm', {});
};

/**
 * GET /api/v1/tutors/me/payments
 * Historial de pagos del tutor
 */
export const getMyTutorPayments = (params = {}) => {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set('page', String(params.page));
  if (params.size !== undefined) query.set('size', String(params.size));
  const qs = query.toString();
  return apiClient.get(`/api/v1/tutors/me/payments${qs ? '?' + qs : ''}`);
};




// Apunta al nuevo controlador
export const verifyHiringSession = (sessionId) => {
  return apiClient.post('/api/v1/contrataciones/verify-hiring-session', { sessionId });
};


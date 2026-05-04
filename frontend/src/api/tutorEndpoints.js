import { apiClient } from './client';

/**
 * GET /api/v1/tutors
 * Listar tutores. Si `verificado` es true/false, filtra exclusivamente por ese estado;
 * si es undefined, devuelve todos (verificados primero por orden).
 * @param {Object} params - { especialidad, tarifaMin, tarifaMax, verificado, page, size }
 */
export const getVerifiedTutors = (params = {}) => {
  const query = new URLSearchParams();
  if (params.especialidad) query.set('especialidad', params.especialidad);
  if (params.tarifaMin) query.set('tarifaMin', String(params.tarifaMin));
  if (params.tarifaMax) query.set('tarifaMax', String(params.tarifaMax));
  if (params.verificado !== undefined) query.set('verificado', String(Boolean(params.verificado)));
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
 * POST /api/v1/tutors/me/create-verification-payment-intent
 * Crear un PaymentIntent para verificación de tutor con Stripe Elements
 * @returns {Promise<Object>} - { clientSecret, paymentIntentId }
 */
export const createVerificationPaymentIntent = () => {
  return apiClient.post('/api/v1/tutors/me/create-verification-payment-intent', {});
};

/**
 * POST /api/v1/tutors/me/confirm-verification-payment
 * Confirmar pago de verificación tras Stripe Elements exitoso
 * @param {string} paymentIntentId
 * @returns {Promise<Object>} - { mensaje }
 */
export const confirmVerificationPayment = (paymentIntentId) => {
  return apiClient.post('/api/v1/tutors/me/confirm-verification-payment', { paymentIntentId });
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

/** POST /api/v1/tutors/me/stripe-connect/onboarding — Inicia onboarding Stripe Connect */
export const iniciarOnboardingStripe = (returnUrl) => {
  return apiClient.post('/api/v1/tutors/me/stripe-connect/onboarding', { returnUrl });
};

/** GET /api/v1/tutors/me/stripe-connect/status — Estado de la cuenta Stripe Connect */
export const obtenerEstadoStripeConnect = () => {
  return apiClient.get('/api/v1/tutors/me/stripe-connect/status');
};

/** GET /api/v1/tutors/me/earnings — Ganancias del tutor */
export const obtenerGananciasTutor = (params = {}) => {
  const query = new URLSearchParams();
  if (params.page !== undefined) query.set('page', String(params.page));
  if (params.size !== undefined) query.set('size', String(params.size));
  const qs = query.toString();
  return apiClient.get(`/api/v1/tutors/me/earnings${qs ? '?' + qs : ''}`);
};

// src/api/institutions.api.js
import { apiClient } from './client';

export const institutionsApi = {
  /**
   * POST /api/v1/institutions
   * Crear institución
   */
  create(data) {
    return apiClient.post('/api/v1/institutions', data);
  },

  /**
   * GET /api/v1/institutions/{id}
   */
  getById(id) {
    return apiClient.get(`/api/v1/institutions/${id}`);
  },

  /**
   * PUT /api/v1/institutions/{id}
   */
  update(id, data) {
    return apiClient.put(`/api/v1/institutions/${id}`, data);
  },

  /**
   * POST /api/v1/institutions/{id}/plan
   * Contratar plan corporativo (CorporatePlanRequest)
   * @param {number} id
   * @param {{ tipoPlan, numUsuarios, duracionMeses, aceptarTerminos, documentacionEligibilidad? }} data
   */
  hirePlan(id, data) {
    return apiClient.post(`/api/v1/institutions/${id}/plan`, data);
  },

  /**
   * DELETE /api/v1/institutions/{id}/plan
   * Cancelar plan corporativo
   */
  cancelPlan(id) {
    return apiClient.delete(`/api/v1/institutions/${id}/plan`);
  },

  /**
   * GET /api/v1/institutions/{id}/plan/status
   * Estado del plan corporativo
   */
  getPlanStatus(id) {
    return apiClient.get(`/api/v1/institutions/${id}/plan/status`);
  },

  verifySession(sessionId) {
    return apiClient.post('/api/v1/institutions/verify-session', { sessionId });
  },

  /**
   * POST /api/v1/institutions/{id}/create-plan-payment-intent
   * Crea un PaymentIntent para plan corporativo (Stripe Elements embebido)
   */
  createPlanPaymentIntent(id, data) {
    return apiClient.post(`/api/v1/institutions/${id}/create-plan-payment-intent`, data);
  },

  /**
   * POST /api/v1/institutions/confirm-plan-payment
   * Confirma el pago del plan corporativo tras Stripe Elements
   */
  confirmPlanPayment(paymentIntentId) {
    return apiClient.post('/api/v1/institutions/confirm-plan-payment', { paymentIntentId });
  },
};

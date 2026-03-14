// src/api/subscriptions.api.js
import { apiClient } from './client';

export const subscriptionsApi = {
  /**
   * GET /api/v1/subscriptions/plans
   * Obtener todos los planes de suscripción disponibles
   * @returns {Promise<Array>} - Array con los tipos de planes
   */
  listPlans() {
    return apiClient.get('/subscriptions/plans');
  },

  /**
   * GET /api/v1/subscriptions/me
   * Obtener la suscripción actual del usuario autenticado
   * @returns {Promise<Object>} - Detalles de la suscripción (SubscriptionResponse)
   */
  getMySubscription() {
    return apiClient.get('/api/v1/subscriptions/me');
  },

  /**
   * POST /api/v1/subscriptions/me
   * Suscribir al usuario a un plan Premium
   * @param {string} planId - ID del plan (PREMIUM)
   * @param {boolean} aceptarTerminos - Aceptación de términos
   * @param {string} periodo - "mensual" o "anual"
   * @returns {Promise<Object>} - PaymentUrlResponse con URL de pago de Stripe
   */
  subscribe({ planId = 'PREMIUM', aceptarTerminos = true, periodo = 'mensual' } = {}) {
    return apiClient.post('/api/v1/subscriptions/me', {
      planId,
      aceptarTerminos,
      periodo,
    });
  },

  /**
   * DELETE /api/v1/subscriptions/me
   * Cancelar la suscripción activa del usuario
   * @returns {Promise<Object>} - Suscripción cancelada
   */
  cancelSubscription() {
    return apiClient.delete('/api/v1/subscriptions/me');
  },

  /**
   * POST /api/v1/subscriptions/me/confirm-payment
   * (Solo desarrollo) Confirma pago sin Stripe
   */
  confirmPayment() {
    return apiClient.post('/api/v1/subscriptions/me/confirm-payment');
  },


  verifySession(sessionId) {
    return apiClient.post('/api/v1/subscriptions/me/verify-session', { sessionId });
  },

  /**
   * POST /api/v1/subscriptions/me/create-payment-intent
   * Crear un PaymentIntent para Stripe Elements embebido
   * @param {Object} params - { planId, aceptarTerminos, periodo }
   * @returns {Promise<Object>} - { clientSecret, paymentIntentId }
   */
  createPaymentIntent({ planId = 'PREMIUM', aceptarTerminos = true, periodo = 'mensual' } = {}) {
    return apiClient.post('/api/v1/subscriptions/me/create-payment-intent', {
      planId,
      aceptarTerminos,
      periodo,
    });
  },

  /**
   * POST /api/v1/subscriptions/me/confirm-embedded-payment
   * Confirmar pago tras Stripe Elements exitoso
   * @param {string} paymentIntentId
   * @returns {Promise<Object>} - { mensaje }
   */
  confirmEmbeddedPayment(paymentIntentId) {
    return apiClient.post('/api/v1/subscriptions/me/confirm-embedded-payment', { paymentIntentId });
  },
};
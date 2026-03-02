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
   * @returns {Promise<Object>} - PaymentUrlResponse con URL de pago de Stripe
   */
  subscribe(planId = 'PREMIUM', aceptarTerminos = true) {
    return apiClient.post('/api/v1/subscriptions/me', { planId, aceptarTerminos });
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
};
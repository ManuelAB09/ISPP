// src/api/payments.api.js
import { apiClient } from './client';

export const paymentsApi = {
  /**
   * GET /api/v1/payments/history
   * Historial de transacciones del usuario autenticado
   * @param {Object} params - { tipo?, desde?, hasta?, page?, size? }
   */
  getHistory(params = {}) {
    const query = new URLSearchParams();
    if (params.tipo) query.set('tipo', params.tipo);
    if (params.desde) query.set('desde', params.desde);
    if (params.hasta) query.set('hasta', params.hasta);
    if (params.page !== undefined) query.set('page', String(params.page));
    if (params.size !== undefined) query.set('size', String(params.size));
    const qs = query.toString();
    return apiClient.get(`/api/v1/payments/history${qs ? '?' + qs : ''}`);
  },

  /**
   * GET /api/v1/payments/{transactionId}
   * Detalle de una transacción
   * @param {number} transactionId
   */
  getTransaction(transactionId) {
    return apiClient.get(`/api/v1/payments/${transactionId}`);
  },
};

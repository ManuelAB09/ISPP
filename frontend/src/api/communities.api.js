// src/api/communities.api.js
import { apiClient } from './client';

export const communitiesApi = {
  /**
   * GET /api/v1/communities
   * Explorar comunidades públicas
   * @param {Object} params - { search?, page?, size? }
   */
  list(params = {}) {
    const query = new URLSearchParams();
    if (params.search) query.set('search', params.search);
    if (params.page !== undefined) query.set('page', String(params.page));
    if (params.size !== undefined) query.set('size', String(params.size));
    
    const queryString = query.toString();
    return apiClient.get(`/api/v1/communities${queryString ? '?' + queryString : ''}`);
  },

  /**
   * POST /api/v1/communities
   * Crear nueva comunidad
   * @param {Object} data - { nombre, descripcion?, tipoGrupo, imagen? }
   */
  create(data) {
    return apiClient.post('/api/v1/communities', data);
  },

  /**
   * GET /api/v1/communities/{id}
   * Obtener detalle de comunidad
   * @param {number} id
   */
  getById(id) {
    return apiClient.get(`/api/v1/communities/${id}`);
  },

  /**
   * POST /api/v1/communities/{id}/members
   * Unirse a comunidad pública
   * @param {number} communityId
   */
  join(communityId) {
    return apiClient.post(`/api/v1/communities/${communityId}/members`, {});
  },

  /**
   * GET /api/v1/communities/{id}/members
   * Listar miembros de una comunidad
   * @param {number} communityId
   * @param {Object} params - { page?, size? }
   */
  getMembers(communityId, params = {}) {
    const query = new URLSearchParams();
    if (params.page !== undefined) query.set('page', String(params.page));
    if (params.size !== undefined) query.set('size', String(params.size));
    
    const queryString = query.toString();
    return apiClient.get(`/api/v1/communities/${communityId}/members${queryString ? '?' + queryString : ''}`);
  },
};
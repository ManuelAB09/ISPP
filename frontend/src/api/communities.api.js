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
   * GET /api/v1/communities/members/me
   * Lista comunidades donde soy miembro
   * @param {Object} params - { page?, size? }
   */
  listMine(params = {}) {
    const query = new URLSearchParams();
    if (params.page !== undefined) query.set('page', String(params.page));
    if (params.size !== undefined) query.set('size', String(params.size));

    const queryString = query.toString();
    return apiClient.get(`/api/v1/communities/members/me${queryString ? '?' + queryString : ''}`);
  },

  /**
   * POST /api/v1/communities
   * Crear nueva comunidad
   * @param {Object} data - { nombre, descripcion?, tipoGrupo, imagenUrl? }
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
   * @param {'ALUMNO'|'PROFESOR'} rol
   */
  join(communityId, rol = 'ALUMNO') {
    return apiClient.post(`/api/v1/communities/${communityId}/members`, { rol });
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

  /**
   * POST /api/v1/communities/{communityId}/upgrade
   * Mejorar comunidad a Premium
   * @param {number} communityId
   */
  upgradeCommunity(communityId) {
    return apiClient.post(`/api/v1/communities/${communityId}/upgrade`, {});
  },

  /**
   * POST /api/v1/communities/{communityId}/tutor/{tutorId}
   * Contratar un tutor para la comunidad (HireTutorRequest)
   * @param {number} communityId
   * @param {number} tutorId
   * @param {{ modalidad, duracion, tarifaAcordada, aceptarTerminos }} data
   */
  hireTutor(communityId, tutorId, data) {
    return apiClient.post(`/api/v1/communities/${communityId}/tutor/${tutorId}`, data);
  },

  /**
   * POST /api/v1/communities/{communityId}/tutor/{tutorId}/create-payment-intent
   * Crea un PaymentIntent para contratación de tutor (Stripe Elements embebido)
   */
  createHiringPaymentIntent(communityId, tutorId, data) {
    return apiClient.post(`/api/v1/communities/${communityId}/tutor/${tutorId}/create-payment-intent`, data);
  },

  /**
   * POST /api/v1/communities/confirm-tutor-payment
   * Confirma el pago de contratación de tutor tras Stripe Elements
   */
  confirmTutorPayment(paymentIntentId) {
    return apiClient.post('/api/v1/communities/confirm-tutor-payment', { paymentIntentId });
  },

  /**
   * GET /api/v1/communities/{communityId}/tutor
   * Obtener tutor actualmente contratado por la comunidad
   * @param {number} communityId
   */
  getHiredTutor(communityId) {
    return apiClient.get(`/api/v1/communities/${communityId}/tutor`);
  },

  /**
   * DELETE /api/v1/communities/{communityId}/tutor
   * Cancelar contratación activa del tutor
   * @param {number} communityId
   * @param {string} motivo
   */
  cancelTutor(communityId, motivo) {
    return apiClient.delete(`/api/v1/communities/${communityId}/tutor?motivo=${encodeURIComponent(motivo)}`);
  },

/**
   * GET /api/v1/communities/{id}/members/me
   * Obtener mi membresía en una comunidad
   * @param {number} communityId
   */
  getMyMembership(communityId) {
    return apiClient
      .get(`/api/v1/communities/${communityId}/members/me`)
      .catch((err) => {
        if (err.status === 404) {
          // no somos miembro, devolver null en vez de propagar el error
          return null;
        }
        return Promise.reject(err);
      });
  },

  /**
   * DELETE /api/v1/communities/{id}/members/me
   * Abandonar una comunidad
   * @param {number} communityId
   */
  leave(communityId) {
    return apiClient.delete(`/api/v1/communities/${communityId}/members/me`);
  },

  /**
   * DELETE /api/v1/communities/{communityId}/members/{userId}
   * Expulsar a un miembro de la comunidad (solo admin)
   * @param {number} communityId
   * @param {number} userId
   */
  expelMember(communityId, userId) {
    return apiClient.delete(`/api/v1/communities/${communityId}/members/${userId}`);
  },

  /**
   * POST /api/v1/communities/{id}/admin/transfer
   * Transferir rol de admin a otro miembro
   * @param {number} communityId
   * @param {number} nuevoAdminId
   */
  transferAdmin(communityId, nuevoAdminId, nuevoRolOrigen) {
    return apiClient.post(`/api/v1/communities/${communityId}/admin/transfer`, { nuevoAdminId, nuevoRolOrigen });
  },

  /**
   * POST /api/v1/communities/{communityId}/admins/{nuevoAdminId}
   * Añadir un nuevo administrador a la comunidad
   * @param {number} communityId
   * @param {number} nuevoAdminId
   */
  addAdmin(communityId, nuevoAdminId) {
    return apiClient.post(`/api/v1/communities/${communityId}/admins/${nuevoAdminId}`, {});
  },

  /**
   * Activar rol PROFESOR para el usuario autenticado dentro de una comunidad.
   * Nota: se prueban varias rutas por compatibilidad hasta fijar el contrato backend.
   * @param {number} communityId
   */
  async activateTeacherRole(communityId) {
    try {
      return await apiClient.put(`/api/v1/communities/${communityId}/members/me/role`, { rol: 'PROFESOR' });
    } catch (err) {
      const status = err?.status || err?.response?.status;
      if (status !== 404 && status !== 405) {
        throw err;
      }

      try {
        return await apiClient.post(`/api/v1/communities/${communityId}/members/me/teacher`, {});
      } catch (err2) {
        const status2 = err2?.status || err2?.response?.status;
        if (status2 !== 404 && status2 !== 405) {
          throw err2;
        }
        return apiClient.post(`/api/v1/communities/${communityId}/members/me/teacher/activate`, {});
      }
    }
  },

  /**
   * POST /api/v1/communities/{communityId}/requests
   * Solicitar acceso a una comunidad privada
   * @param {number} communityId
   * @param {string} mensaje
   */
  requestAccess(communityId, mensaje = '', rolDeseado = 'ALUMNO') {
    return apiClient.post(`/api/v1/communities/${communityId}/requests`, { mensaje, rolDeseado });
  },

  /**
   * GET /api/v1/communities/{communityId}/requests/me
   * Comprobar si tengo solicitud pendiente en una comunidad
   * @param {number} communityId
   * @returns {Promise<{pending: boolean}>}
   */
  getMyRequestStatus(communityId) {
    return apiClient.get(`/api/v1/communities/${communityId}/requests/me`);
  },

  /**
   * GET /api/v1/communities/{communityId}/requests
   * Listar solicitudes de acceso (solo admin)
   * @param {number} communityId
   * @param {Object} params - { estado?, page?, size? }
   */
  listRequests(communityId, params = {}) {
    const query = new URLSearchParams();
    if (params.estado) query.set('estado', params.estado);
    if (params.page !== undefined) query.set('page', String(params.page));
    if (params.size !== undefined) query.set('size', String(params.size));
    const queryString = query.toString();
    return apiClient.get(`/api/v1/communities/${communityId}/requests${queryString ? '?' + queryString : ''}`);
  },

  /**
   * PUT /api/v1/communities/{communityId}/requests/{requestId}
   * Aceptar o rechazar solicitud de acceso (solo admin)
   * @param {number} communityId
   * @param {number} requestId
   * @param {boolean} aceptado
   */
  respondToRequest(communityId, requestId, aceptado) {
    return apiClient.put(`/api/v1/communities/${communityId}/requests/${requestId}`, { aceptado });
  },

  /**
   * DELETE /api/v1/communities/{communityId}
   * Eliminar comunidad (solo admin)
   * @param {number} communityId
   */
  deleteCommunity(communityId) {
    return apiClient.delete(`/api/v1/communities/${communityId}`);
  },

  /**
   * PUT /api/v1/communities/{communityId}
   * Actualizar datos de comunidad (solo admin)
   * @param {number} communityId
   * @param {Object} data - { nombre, descripcion, imagenUrl }
   */
  update(communityId, data) {
    return apiClient.put(`/api/v1/communities/${communityId}`, data);
  },

  // ─── Google Classroom ───

  /**
   * GET /api/v1/communities/{id}/classroom
   * Obtener curso vinculado de Google Classroom
   * @param {number} communityId
   */
  getClassroom(communityId) {
    return apiClient.get(`/api/v1/communities/${communityId}/classroom`);
  },

  /**
   * POST /api/v1/communities/{id}/classroom
   * Vincular curso de Google Classroom a la comunidad
   * @param {number} communityId
   * @param {Object} data - { courseId, courseName }
   */
  linkClassroom(communityId, data) {
    return apiClient.post(`/api/v1/communities/${communityId}/classroom`, data);
  },

  /**
   * DELETE /api/v1/communities/{id}/classroom
   * Desvincular curso de Google Classroom
   * @param {number} communityId
   */
  unlinkClassroom(communityId) {
    return apiClient.delete(`/api/v1/communities/${communityId}/classroom`);
  },

  /**
   * POST /api/v1/communities/{communityId}/photo (multipart/form-data)
   * Subir imagen de portada de comunidad
   * @param {number} communityId
   * @param {FormData} formData - debe contener campo 'file'
   */
  uploadPhoto(communityId, formData) {
    return apiClient.post(`/api/v1/communities/${communityId}/photo`, formData);
  },

  /**
   * GET /api/v1/communities/{communityId}/ranking
   * Obtener ranking de miembros de comunidad
   * @param {number} communityId
   */
  getRanking(communityId) {
    return apiClient.get(`/api/v1/communities/${communityId}/ranking`);
  },
};

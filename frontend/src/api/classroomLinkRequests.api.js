import { apiClient } from './client';

export const classroomLinkRequestsApi = {
    /**
   * GET /api/v1/classroom-link-requests/me
   * Devuelve las solicitudes de vinculación pendientes del usuario actual
   */
  myPending() {
    return apiClient.get("/api/v1/classroom-link-requests/me");
  },

    /**
   * POST /api/v1/classroom-link-requests/{id}/link
   * Completa una solicitud de vinculación de Google Classroom
   * @param {number} requestId
   */
  complete(requestId, data) {
    // data: { cursoId, nombreCurso }
    return apiClient.post(`/api/v1/classroom-link-requests/${requestId}/link`, data);
  },
};
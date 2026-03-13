import { getApiBaseUrl } from './baseUrl';
import { apiClient, ApiError } from './client';

const API_BASE_URL = getApiBaseUrl();

const buildAuthHeaders = () => {
  const token = localStorage.getItem('accessToken');
  return token ? { Authorization: `Bearer ${token}` } : {};
};

export const ZoomApi = {

  /**
   * POST /api/v1/zoom/communities/{communityId}/meeting
   * Crear o reutilizar llamada activa de una comunidad
   * @param {number} communityId
   * @param {{ topic?: string, durationMinutes?: number }} [request]
   */
  createOrGetMeeting(communityId, request = null) {
    return apiClient.post(`/api/v1/zoom/communities/${communityId}/meeting`, request);
  },

  /**
   * GET /api/v1/zoom/communities/{communityId}/meeting
   * Obtener llamada activa de la comunidad
   * @param {number} communityId
   */
  getActiveMeeting(communityId) {
    return apiClient.get(`/api/v1/zoom/communities/${communityId}/meeting`);
  },

  /**
   * POST /api/v1/zoom/communities/{communityId}/meeting/join
   * Entrar en la llamada activa (devuelve link y clave de acceso)
   * @param {number} communityId
   */
  joinMeeting(communityId) {
    return apiClient.post(`/api/v1/zoom/communities/${communityId}/meeting/join`);
  },

  /**
   * GET /api/v1/zoom/communities/{communityId}/meeting/participants
   * Listar quién está en la llamada activa
   * @param {number} communityId
   */
  listParticipants(communityId) {
    return apiClient.get(`/api/v1/zoom/communities/${communityId}/meeting/participants`);
  },

  /**
   * DELETE /api/v1/zoom/communities/{communityId}/meeting
   * Finalizar la llamada activa (solo el creador)
   * @param {number} communityId
   */
  endMeeting(communityId) {
    return apiClient.delete(`/api/v1/zoom/communities/${communityId}/meeting`);
  },

  /**
   * GET /api/v1/zoom/communities/{communityId}/meetings
   * Listar histórico de reuniones de la comunidad
   * @param {number} communityId
   */
  listMeetings(communityId) {
    return apiClient.get(`/api/v1/zoom/communities/${communityId}/meetings`);
  },

  /**
   * GET /api/v1/zoom/communities/{communityId}/recordings
   * Listar grabaciones de la comunidad
   * @param {number} communityId
   */
  listRecordings(communityId) {
    return apiClient.get(`/api/v1/zoom/communities/${communityId}/recordings`);
  },

  /**
   * GET /api/v1/zoom/communities/{communityId}/recordings/{recordingId}
   * Obtener detalle de una grabación
   * @param {number} communityId
   * @param {string} recordingId
   */
  getRecording(communityId, recordingId) {
    return apiClient.get(`/api/v1/zoom/communities/${communityId}/recordings/${recordingId}`);
  },

  /**
   * POST /api/v1/zoom/communities/{communityId}/meetings/{meetingId}/recordings/upload
   * Subir manualmente una grabación para una reunión
   * @param {number} communityId
   * @param {number} meetingId
   * @param {File} file
   */
  uploadRecording(communityId, meetingId, file) {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.post(
      `/api/v1/zoom/communities/${communityId}/meetings/${meetingId}/recordings/upload`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    );
  },

  /**
   * GET /api/v1/zoom/communities/{communityId}/recordings/{recordingId}/download
   * Descargar una grabación
   * @param {number} communityId
   * @param {string} recordingId
   */
  downloadRecording(communityId, recordingId) {
    return fetch(
      `${API_BASE_URL}/api/v1/zoom/communities/${communityId}/recordings/${recordingId}/download`,
      {
        method: 'GET',
        headers: buildAuthHeaders(),
      }
    ).then(async (response) => {
      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new ApiError(response.status, error.message || 'Error desconocido', error);
      }

      const contentDisposition = response.headers.get('content-disposition') || '';
      const match = contentDisposition.match(/filename="?([^";]+)"?/i);

      return {
        blob: await response.blob(),
        fileName: match ? match[1] : `grabacion-${recordingId}`,
        contentType: response.headers.get('content-type') || 'application/octet-stream',
      };
    });
  },

  /**
   * GET /api/v1/zoom/me/calls
   * Saber en qué llamadas activas está el usuario autenticado
   */
  getMyActiveCalls() {
    return apiClient.get('/api/v1/zoom/me/calls');
  },

};
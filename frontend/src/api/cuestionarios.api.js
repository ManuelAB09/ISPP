import axiosInstance from './axiosConfig';

export const cuestionariosApi = {
  createCuestionario: async (data) => {
    const response = await axiosInstance.post('/api/v1/cuestionarios', data);
    return response.data;
  },
  listMine: async () => {
    const response = await axiosInstance.get('/api/v1/cuestionarios/mine');
    return response.data;
  },
  listPublicByUserId: async (userId) => {
    const response = await axiosInstance.get(`/api/v1/cuestionarios/user/${userId}/public`);
    return response.data;
  },
  listByCommunity: async (communityId) => {
    const response = await axiosInstance.get(`/api/v1/cuestionarios/community/${communityId}`);
    return response.data;
  },
  getById: async (id) => {
    const response = await axiosInstance.get(`/api/v1/cuestionarios/${id}`);
    return response.data;
  },
  getPreview: async (id) => {
    const response = await axiosInstance.get(`/api/v1/cuestionarios/${id}/preview`);
    return response.data;
  },
  getResolver: async (id) => {
    const response = await axiosInstance.get(`/api/v1/cuestionarios/${id}/resolver`);
    return response.data;
  },
  publishCuestionario: async (id) => {
    const response = await axiosInstance.put(`/api/v1/cuestionarios/${id}/publish`);
    return response.data;
  },
  draftCuestionario: async (id) => {
    const response = await axiosInstance.put(`/api/v1/cuestionarios/${id}/draft`);
    return response.data;
  },
  submitAttempt: async (id, answers) => {
    const response = await axiosInstance.post(`/api/v1/cuestionarios/${id}/submit`, answers);
    return response.data;
  }
};

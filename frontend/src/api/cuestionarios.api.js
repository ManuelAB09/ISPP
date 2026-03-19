import axiosInstance from './axiosConfig';

export const cuestionariosApi = {
  createCuestionario: async (data) => {
    const response = await axiosInstance.post('/api/v1/cuestionarios', data);
    return response.data;
  },
  getById: async (id) => {
    const response = await axiosInstance.get(`/api/v1/cuestionarios/${id}`);
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

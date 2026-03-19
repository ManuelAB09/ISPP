import axiosInstance from './axiosConfig';

export const feedbackApi = {
  createFeedback: async (communityId, data) => {
    const response = await axiosInstance.post(`/api/v1/communities/${communityId}/feedbacks`, data);
    return response.data;
  },
  listFeedbacks: async (communityId) => {
    const response = await axiosInstance.get(`/api/v1/communities/${communityId}/feedbacks`);
    return response.data;
  }
};

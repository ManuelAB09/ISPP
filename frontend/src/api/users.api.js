import axiosInstance from './axiosConfig';

export const usersApi = {
  getMyActivity: async () => {
    const response = await axiosInstance.get('/api/v1/users/me/activity');
    return response.data;
  }
};

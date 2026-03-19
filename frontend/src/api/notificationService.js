import axiosInstance from './axiosConfig';

export const getAllNotifications = async () => {
  const response = await axiosInstance.get('/api/v1/my-events/alerts/all');
  return response.data;
};

export const markNotificationAsRead = async (alertaId) => {
  const response = await axiosInstance.patch(`/api/v1/my-events/alerts/${alertaId}/read`);
  return response.data;
};

export const markAllNotificationsAsRead = async () => {
  await axiosInstance.patch('/api/v1/my-events/alerts/read-all');
};

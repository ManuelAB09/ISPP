import axiosInstance from './axiosConfig';

// AlertaEvento (eventos)
export const getAllEventAlerts = async () => {
  const response = await axiosInstance.get('/api/v1/my-events/alerts/all');
  return response.data;
};

export const markEventAlertAsRead = async (alertaId) => {
  const response = await axiosInstance.patch(`/api/v1/my-events/alerts/${alertaId}/read`);
  return response.data;
};

export const markAllEventAlertsAsRead = async () => {
  await axiosInstance.patch('/api/v1/my-events/alerts/read-all');
};

// Notificacion (anuncios, push, etc)
export const getAllUserNotifications = async () => {
  const response = await axiosInstance.get('/api/v1/notifications');
  return response.data;
};


export const markUserNotificationAsRead = async (notificacionId) => {
  const response = await axiosInstance.patch(`/api/v1/notifications/${notificacionId}/read`);
  return response.data;
};

export const markAllUserNotificationsAsRead = async () => {
  // TODO: Implementar endpoint PATCH /api/v1/notifications/read-all en backend si se requiere
  // Por ahora, solo simular
  return { success: true };
};

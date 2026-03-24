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
  try {
    const response = await axiosInstance.patch('/api/v1/notifications/read-all');
    return response.data;
  } catch (error) {
    // Fallback: si el endpoint masivo no existe en backend,
    // marcamos una por una las no leidas para persistir el estado.
    const notifications = await getAllUserNotifications();
    const unread = (Array.isArray(notifications) ? notifications : []).filter((n) => !n?.leida && n?.id != null);

    if (unread.length === 0) {
      return { success: true, mode: 'fallback', updated: 0 };
    }

    await Promise.all(unread.map((n) => axiosInstance.patch(`/api/v1/notifications/${n.id}/read`)));
    return { success: true, mode: 'fallback', updated: unread.length };
  }
};

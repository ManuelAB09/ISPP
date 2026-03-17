import axiosInstance from '../api/axiosConfig';

export const getMisEventos = async () => {
  const response = await axiosInstance.get('/api/v1/my-events');
  return response.data;
};

export const getMisEventosHistorial = async (incluirCancelados = false) => {
  const response = await axiosInstance.get('/api/v1/my-events/history', {
    params: { incluirCancelados },
  });
  return response.data;
};

export const getAlertas = async () => {
  const response = await axiosInstance.get('/api/v1/my-events/alerts');
  return response.data;
};

export const getAlertasCount = async () => {
  const response = await axiosInstance.get('/api/v1/my-events/alerts/count');
  return response.data;
};

export const marcarAlertaLeida = async (id) => {
  const response = await axiosInstance.patch(`/api/v1/my-events/alerts/${id}/read`);
  return response.data;
};

export const marcarTodasLeidas = async () => {
  const response = await axiosInstance.patch('/api/v1/my-events/alerts/read-all');
  return response.data;
};

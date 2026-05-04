import axiosInstance from './axiosConfig';

export const getAnnouncementComments = async (anuncioId) => {
  const res = await axiosInstance.get(`/api/v1/announcements/${anuncioId}/comments`);
  return res.data;
};

export const postAnnouncementComment = async (anuncioId, texto) => {
  const res = await axiosInstance.post(`/api/v1/announcements/${anuncioId}/comments`, { texto });
  return res.data;
};

export const deleteAnnouncementComment = async (anuncioId, comentarioId) => {
  await axiosInstance.delete(`/api/v1/announcements/${anuncioId}/comments/${comentarioId}`);
};

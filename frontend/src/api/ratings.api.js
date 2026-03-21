import axios from 'axios';

export const getProfesorStats = async (profesorId) => {
  const res = await axios.get(`/api/valoraciones/profesor/${profesorId}/stats`);
  return res.data;
};

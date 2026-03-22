
import { apiClient } from "./client";

export const getValoracionesStats = async (profesorId) => {
  return await apiClient.get(`/api/valoraciones/profesor/${profesorId}`);
};

export const getValoraciones = async (profesorId) => {
  return await apiClient.get(`/api/valoraciones/profesor/${profesorId}/todas`);
};

export const crearValoracion = async (valoracion) => {
  return await apiClient.post(`/api/valoraciones`, valoracion);
};



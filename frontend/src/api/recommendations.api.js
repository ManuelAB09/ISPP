import { apiClient } from './client';

const BASE = '/api/recommendations';

// ─── Carga de página completa ─────────────────────────────────────────────────

/** Una sola llamada que devuelve todas las secciones. */
export const getRecommendationsPage = () =>
  apiClient.get(`${BASE}/page`);

// ─── Endpoints por sección ────────────────────────────────────────────────────

export const getRecommendaciones = (page = 0, size = 20) =>
  apiClient.get(`${BASE}?page=${page}&size=${size}`);

export const getRecomendacionesProfesores = (page = 0, size = 6) =>
  apiClient.get(`${BASE}/profesores?page=${page}&size=${size}`);

export const getRecomendacionesContenido = (page = 0, size = 8) =>
  apiClient.get(`${BASE}/contenido?page=${page}&size=${size}`);

export const getRecomendacionesCuestionarios = (page = 0, size = 6) =>
  apiClient.get(`${BASE}/cuestionarios?page=${page}&size=${size}`);

export const getRecomendacionesComunidades = (page = 0, size = 4) =>
  apiClient.get(`${BASE}/comunidades?page=${page}&size=${size}`);

export const getNoVistas = (page = 0, size = 10) =>
  apiClient.get(`${BASE}/no-vistas?page=${page}&size=${size}`);

// ─── Acciones sobre una recomendación ────────────────────────────────────────

/** Marca la recomendación como vista (llamar al renderizarla). */
export const marcarVista = (id) =>
  apiClient.post(`${BASE}/${id}/vista`);

/**
 * Envía feedback del usuario.
 * @param {number} id
 * @param {{ esUtil: boolean, comentario?: string, satisfaccion?: number }} data
 */
export const enviarFeedback = (id, data) =>
  apiClient.post(`${BASE}/${id}/feedback`, data);

/** Descarta (elimina) una recomendación. */
export const descartarRecomendacion = (id) =>
  apiClient.delete(`${BASE}/${id}`);

// ─── Valorar tutor ────────────────────────────────────────────────────────────

/**
 * @param {number} tutorId
 * @param {{ puntuacion: number, comentario?: string }} data
 */
export const valorarTutor = (tutorId, data) =>
  apiClient.post(`${BASE}/tutores/${tutorId}/valorar`, data);

// ─── Registrar actividad ──────────────────────────────────────────────────────

/**
 * @param {{ tipoActividad: string, categoriaObjeto?: string, terminosBusqueda?: string,
 *           idObjeto?: number, duracionSegundos?: number, datosAdicionales?: string }} data
 */
export const registrarActividad = (data) =>
  apiClient.post(`${BASE}/actividad`, data);

// ─── Forzar regeneración ──────────────────────────────────────────────────────

/** Dispara regeneración asíncrona (devuelve 202 inmediatamente). */
export const forzarRefresh = () =>
  apiClient.post(`${BASE}/refresh`);

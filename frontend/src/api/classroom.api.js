import { apiClient } from './client';

const B = (id) => `/api/v1/comunidades/${id}`;

// ─── Tareas ───────────────────────────────────────────────────────────────────

export const getTareas = (comunidadId) =>
  apiClient.get(`${B(comunidadId)}/tareas`);

export const crearTarea = (comunidadId, data) =>
  apiClient.post(`${B(comunidadId)}/tareas`, data);

export const getEntregas = (comunidadId, tareaId) =>
  apiClient.get(`${B(comunidadId)}/tareas/${tareaId}/entregas`);

export const entregarTarea = (comunidadId, tareaId, data) =>
  apiClient.post(`${B(comunidadId)}/tareas/${tareaId}/entregas`, data);

export const getMiEntrega = (comunidadId, tareaId) =>
  apiClient.get(`${B(comunidadId)}/tareas/${tareaId}/entregas/mia`);

export const getMisEntregas = (comunidadId) =>
  apiClient.get(`${B(comunidadId)}/tareas/mis-entregas`);

export const calificarEntrega = (comunidadId, entregaId, data) =>
  apiClient.put(`${B(comunidadId)}/tareas/entregas/${entregaId}/calificar`, data);

// ─── Recursos ─────────────────────────────────────────────────────────────────

export const getRecursos = (comunidadId) =>
  apiClient.get(`${B(comunidadId)}/recursos`);

export const agregarRecurso = (comunidadId, data) =>
  apiClient.post(`${B(comunidadId)}/recursos`, data);

export const eliminarRecurso = (comunidadId, recursoId) =>
  apiClient.delete(`${B(comunidadId)}/recursos/${recursoId}`);

// ─── Grabaciones ──────────────────────────────────────────────────────────────

export const getGrabaciones = (comunidadId) =>
  apiClient.get(`${B(comunidadId)}/grabaciones`);

export const agregarGrabacion = (comunidadId, data) =>
  apiClient.post(`${B(comunidadId)}/grabaciones`, data);

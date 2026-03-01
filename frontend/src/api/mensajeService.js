import axios from 'axios';
import { getApiBaseUrl } from './baseUrl';

const API_URL = getApiBaseUrl();
const api = axios.create({
    baseURL: `${API_URL}/api/v1`,
});

/**
 * Interceptor para agregar el token JWT a todos los requests.
 */
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('accessToken');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

/**
 * Envía un mensaje en el chat de una comunidad.
 * @param {number} comunidadId - ID de la comunidad.
 * @param {string} contenido - Contenido del mensaje.
 * @returns {Promise} Respuesta del servidor.
 */
export const enviarMensajeComunidad = (comunidadId, contenido) => {
    return api.post(`/comunidades/${comunidadId}/mensajes`, {
        contenido,
    });
};

/**
 * Obtiene el historial de mensajes de una comunidad.
 * @param {number} comunidadId - ID de la comunidad.
 * @returns {Promise<Array>} Lista de mensajes.
 */
export const obtenerHistorialComunidad = (comunidadId) => {
    return api.get(`/comunidades/${comunidadId}/mensajes`);
};

/**
 * Edita un mensaje de comunidad.
 * @param {number} comunidadId - ID de la comunidad.
 * @param {number} mensajeId - ID del mensaje.
 * @param {string} contenido - Nuevo contenido.
 * @returns {Promise} Respuesta del servidor.
 */
export const editarMensajeComunidad = (comunidadId, mensajeId, contenido) => {
    return api.put(`/comunidades/${comunidadId}/mensajes/${mensajeId}`, {
        contenido,
    });
};

/**
 * Elimina un mensaje de comunidad.
 * @param {number} comunidadId - ID de la comunidad.
 * @param {number} mensajeId - ID del mensaje.
 * @returns {Promise} Respuesta del servidor.
 */
export const eliminarMensajeComunidad = (comunidadId, mensajeId) => {
    return api.delete(`/comunidades/${comunidadId}/mensajes/${mensajeId}`);
};

/**
 * Envía un mensaje privado a otro usuario.
 * @param {number} tutorId - ID del receptor.
 * @param {string} contenido - Contenido del mensaje.
 * @returns {Promise} Respuesta del servidor.
 */
export const enviarMensajePrivado = (tutorId, contenido) => {
    return api.post('/mensajes', {
        tutorId,
        contenido,
    });
};

/**
 * Obtiene el historial de mensajes privados con un usuario.
 * @param {number} tutorId - ID del tutor.
 * @returns {Promise<Array>} Lista de mensajes privados.
 */
export const obtenerHistorialPrivado = (tutorId) => {
    return api.get(`/mensajes/tutor/${tutorId}`);
};

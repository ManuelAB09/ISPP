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
    const payload = { contenido };
    if (tutorId !== undefined && tutorId !== null) {
        payload.userId = tutorId;
    }

    return api.post('/mensajes', payload);
};

/**
 * Envía un archivo en chat privado.
 * @param {number} userId - ID del usuario receptor.
 * @param {File} file - Archivo a enviar.
 * @param {string} contenido - Texto opcional.
 * @returns {Promise} Respuesta del servidor.
 */
export const enviarArchivoPrivado = (userId, file, contenido = '') => {
    const formData = new FormData();
    formData.append('file', file);

    if (userId !== undefined && userId !== null) {
        formData.append('userId', String(userId));
    }

    if (contenido && contenido.trim()) {
        formData.append('contenido', contenido.trim());
    }

    return api.post('/mensajes/upload', formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });
};

/**
 * Obtiene el historial de mensajes privados con un usuario.
 * @param {number} tutorId - ID del tutor.
 * @returns {Promise<Array>} Lista de mensajes privados.
 */
export const obtenerHistorialPrivado = (tutorId) => {
    return api.get(`/mensajes/usuario/${tutorId}`);
};

/**
 * Envía un archivo en chat de comunidad.
 * @param {number} comunidadId - ID de la comunidad.
 * @param {File} file - Archivo a enviar.
 * @param {string} contenido - Texto opcional.
 * @returns {Promise} Respuesta del servidor.
 */
export const enviarArchivoComunidad = (comunidadId, file, contenido = '') => {
    const formData = new FormData();
    formData.append('file', file);

    if (contenido && contenido.trim()) {
        formData.append('contenido', contenido.trim());
    }

    return api.post(`/comunidades/${comunidadId}/mensajes/upload`, formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });
};

/**
 * Descarga/obtiene el blob de un archivo de chat.
 * @param {string} archivoUrl - URL relativa o absoluta del archivo.
 * @returns {Promise} Respuesta axios con blob.
 */
export const obtenerArchivoChatBlob = (archivoUrl) => {
    if (!archivoUrl) {
        return Promise.reject(new Error('archivoUrl es obligatorio'));
    }

    const normalizedUrl = String(archivoUrl).startsWith('/api/v1/')
        ? String(archivoUrl).replace('/api/v1/', '/')
        : archivoUrl;

    return api.get(normalizedUrl, { responseType: 'blob' });
};

/**
 * Obtiene la lista de conversaciones privadas del usuario actual.
 * @returns {Promise<Array>} Lista de conversaciones con usuarios que han escrito.
 */
export const obtenerConversaciones = () => {
    return api.get('/mensajes/conversaciones');
};

export const eliminarMensajePrivado = (mensajeId) => {
    return api.delete(`/mensajes/${mensajeId}`);
};

/**
 * Edita un mensaje privado.
 * @param {number} mensajeId - ID del mensaje.
 * @param {string} contenido - Nuevo contenido.
 * @returns {Promise} Respuesta del servidor.
 */
export const editarMensajePrivado = (mensajeId, contenido) => {
    return api.put(`/mensajes/${mensajeId}`, { contenido });
};

/**
 * Obtiene metadatos de una URL para mostrar su vista previa en el chat.
 * @param {string} url - URL del mensaje a previsualizar.
 * @returns {Promise} Respuesta con título, descripción, imagen y dominio.
 */
export const obtenerPreviewEnlace = (url) => {
    return api.post('/link-preview', { url });
};

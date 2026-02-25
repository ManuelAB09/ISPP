// src/api/ubicaciones.api.js
import { apiClient } from './client';

export const ubicacionesApi = {
    /**
     * POST /api/v1/ubicaciones
     * Crear nueva ubicación
     * @param {Object} data - { nombre, direccion, latitud, longitud }
     */
    create(data) {
        return apiClient.post('/api/ubicaciones', data);
    },
};

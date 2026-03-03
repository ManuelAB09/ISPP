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
    /**
     * GET /api/ubicaciones/buscar-estudio
     * Buscar ubicaciones por lat, lon, radio, tipo
     * @param {Object} params - { lat, lon, radio, tipo }
     */
    buscarEstudio({ lat, lon, radio, tipo }) {
        let url = `/api/ubicaciones/buscar-estudio?lat=${lat}&lon=${lon}&radio=${radio}`;
        if (tipo) url += `&tipo=${encodeURIComponent(tipo)}`;
        return apiClient.get(url);
    },
};

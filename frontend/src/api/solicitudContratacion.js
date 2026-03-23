import axios from 'axios';
import { getApiBaseUrl } from './baseUrl';

const API_URL = getApiBaseUrl();
const api = axios.create({
    baseURL: `${API_URL}/api/v1`,
});

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
 * Crear solicitud de contratación directa a un tutor.
 * @param {number} tutorId
 * @param {{ dia: string, horaInicio: string, horaFin: string, mensaje?: string }} data
 */
export const crearSolicitudContratacion = (tutorId, data) => {
    return api.post(`/solicitudes-contratacion/tutor/${tutorId}`, data);
};

/** Aceptar solicitud (tutor). */
export const aceptarSolicitud = (solicitudId) => {
    return api.post(`/solicitudes-contratacion/${solicitudId}/aceptar`);
};

/** Rechazar solicitud (tutor). */
export const rechazarSolicitud = (solicitudId, motivo) => {
    return api.post(`/solicitudes-contratacion/${solicitudId}/rechazar`, { motivo });
};

/** Marcar solicitud como pagada (alumno). */
export const pagarSolicitud = (solicitudId) => {
    return api.post(`/solicitudes-contratacion/${solicitudId}/pagar`);
};

/** Crear PaymentIntent de Stripe para una solicitud aceptada (alumno). */
export const crearPaymentIntentSolicitud = (solicitudId) => {
    return api.post(`/solicitudes-contratacion/${solicitudId}/create-payment-intent`);
};

/** Confirmar pago de Stripe para una solicitud (alumno). */
export const confirmarPagoSolicitud = (solicitudId, paymentIntentId) => {
    return api.post(`/solicitudes-contratacion/${solicitudId}/confirm-payment`, { paymentIntentId });
};

/** Obtener solicitudes pendientes del tutor autenticado. */
export const obtenerSolicitudesPendientes = () => {
    return api.get('/solicitudes-contratacion/tutor/pendientes');
};

/** Obtener todas las solicitudes del tutor autenticado. */
export const obtenerSolicitudesTutor = () => {
    return api.get('/solicitudes-contratacion/tutor');
};

/** Obtener solicitudes del alumno autenticado. */
export const obtenerSolicitudesAlumno = () => {
    return api.get('/solicitudes-contratacion/alumno');
};

/** Cancelar una reserva (tutor). */
export const cancelarSolicitud = (solicitudId, motivo) => {
    return api.post(`/solicitudes-contratacion/${solicitudId}/cancelar`, { motivo });
};

/** Reprogramar una reserva (tutor). */
export const reprogramarSolicitud = (solicitudId, { dia, horaInicio, horaFin }) => {
    return api.post(`/solicitudes-contratacion/${solicitudId}/reprogramar`, { dia, horaInicio, horaFin });
};

/** Cancelar solicitud como alumno (regla 24h si pagada). */
export const cancelarSolicitudAlumno = (solicitudId, motivo) => {
    return api.post(`/solicitudes-contratacion/${solicitudId}/cancelar-alumno`, { motivo });
};

/** Calificar una clase completada (alumno, 1-5 + comentario). */
export const calificarSolicitud = (solicitudId, calificacion, comentario) => {
    return api.post(`/solicitudes-contratacion/${solicitudId}/calificar`, { calificacion, comentario });
};

/** Alumno aprueba reprogramación propuesta por el tutor. */
export const aprobarReprogramacion = (solicitudId) => {
    return api.post(`/solicitudes-contratacion/${solicitudId}/aprobar-reprogramacion`);
};

/** Alumno rechaza reprogramación propuesta por el tutor. */
export const rechazarReprogramacion = (solicitudId) => {
    return api.post(`/solicitudes-contratacion/${solicitudId}/rechazar-reprogramacion`);
};

/** Obtener horarios ocupados del tutor en una fecha (contrataciones activas). */
export const getHorariosOcupadosContratacion = (tutorId, fecha) => {
    return api.get(`/solicitudes-contratacion/tutor/${tutorId}/horarios-ocupados?fecha=${encodeURIComponent(fecha)}`);
};

/** Obtener disponibilidad del tutor para una fecha. */
export const getDisponibilidadTutorFecha = (tutorId, fecha) => {
    return api.get(`/solicitudes-contratacion/tutor/${tutorId}/disponibilidad?fecha=${encodeURIComponent(fecha)}`);
};

/** Crear reunión Zoom para una clase online pagada (tutor). */
export const crearZoomSolicitud = (solicitudId) => {
    return api.post(`/solicitudes-contratacion/${solicitudId}/crear-zoom`);
};

const DEFAULT_API_BASE_URL = 'http://localhost:8080';

/**
 * Determina la URL base de la API.
 *
 * - Si REACT_APP_API_URL está definida y es válida, la usa.
 * - Si la app NO corre en localhost (despliegue), devuelve '' (mismo origen),
 *   porque frontend y backend se sirven juntos desde Spring Boot.
 * - Si la app corre en localhost (desarrollo), usa http://localhost:8080.
 */
export const getApiBaseUrl = () => {
    const rawUrl = process.env.REACT_APP_API_URL;
    const trimmed = rawUrl?.trim();

    // Si no hay variable de entorno, detectar automáticamente
    if (!trimmed || trimmed === ':8080') {
        // En despliegue (no localhost): usar mismo origen (URL relativa)
        if (typeof window !== 'undefined' && window.location.hostname !== 'localhost') {
            return '';
        }
        // En desarrollo local: apuntar al backend en puerto 8080
        return DEFAULT_API_BASE_URL;
    }

    try {
        const withProtocol = /^https?:\/\//i.test(trimmed) ? trimmed : `http://${trimmed}`;
        const parsedUrl = new URL(withProtocol);

        if (!parsedUrl.hostname) {
            return DEFAULT_API_BASE_URL;
        }

        if (parsedUrl.hostname === ':' || parsedUrl.hostname === '0.0.0.0') {
            return DEFAULT_API_BASE_URL;
        }

        return `${parsedUrl.protocol}//${parsedUrl.host}`;
    } catch {
        return DEFAULT_API_BASE_URL;
    }
};

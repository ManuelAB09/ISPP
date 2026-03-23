const DEFAULT_API_BASE_URL = 'http://localhost:8080';

const isLocalHostname = (hostname) => {
    if (!hostname) return false;
    return hostname === 'localhost' || hostname === '127.0.0.1' || hostname === '::1';
};

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
    const inBrowser = typeof window !== 'undefined';
    const currentHostname = inBrowser ? window.location.hostname : '';
    const isLocalDev = isLocalHostname(currentHostname);

    // Si no hay variable de entorno, detectar automáticamente
    if (!trimmed || trimmed === ':8080') {
        // En despliegue (no local): usar mismo origen (URL relativa)
        if (inBrowser && !isLocalDev) {
            return '';
        }
        // En desarrollo local (localhost/127.0.0.1/::1): backend en 8080
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

        // Guardrail en dev: evitar apuntar accidentalmente al mismo puerto del frontend
        // (ej. 3000/3001), que rompe WebSocket y llamadas API.
        if (
            inBrowser
            && isLocalDev
            && isLocalHostname(parsedUrl.hostname)
            && parsedUrl.port
            && parsedUrl.port === window.location.port
        ) {
            return DEFAULT_API_BASE_URL;
        }

        return `${parsedUrl.protocol}//${parsedUrl.host}`;
    } catch {
        return DEFAULT_API_BASE_URL;
    }
};

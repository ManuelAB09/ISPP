const DEFAULT_API_BASE_URL = 'http://localhost:8080';
const RUNTIME_OVERRIDE_KEYS = ['E2E_API_BASE_URL', 'e2eApiBaseUrl'];

const isLocalHostname = (hostname) => {
    if (!hostname) return false;
    return hostname === 'localhost' || hostname === '127.0.0.1' || hostname === '::1';
};

const getBrowserWindow = () => {
    if (typeof window === 'undefined') {
        return null;
    }
    return window;
};

const normalizeApiBaseCandidate = (candidate) => {
    const trimmed = candidate?.trim();
    if (!trimmed) {
        return '';
    }

    try {
        const withProtocol = /^https?:\/\//i.test(trimmed)
            ? trimmed
            : `http://${trimmed}`;
        const parsedUrl = new URL(withProtocol);

        if (!parsedUrl.hostname || parsedUrl.hostname === ':' || parsedUrl.hostname === '0.0.0.0') {
            return '';
        }

        return `${parsedUrl.protocol}//${parsedUrl.host}`;
    } catch {
        return '';
    }
};

const readRuntimeApiOverride = () => {
    const browserWindow = getBrowserWindow();
    if (!browserWindow) {
        return '';
    }

    const globalOverride = browserWindow.__E2E_API_BASE_URL || browserWindow.__API_BASE_URL_OVERRIDE;
    if (typeof globalOverride === 'string' && globalOverride.trim()) {
        return globalOverride.trim();
    }

    for (const key of RUNTIME_OVERRIDE_KEYS) {
        try {
            const value = browserWindow.localStorage.getItem(key);
            if (typeof value === 'string' && value.trim()) {
                return value.trim();
            }
        } catch {
            // Ignore storage access errors (private mode, restricted context, etc.)
        }
    }

    return '';
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
    const browserWindow = getBrowserWindow();
    const inBrowser = Boolean(browserWindow);
    const currentHostname = inBrowser ? browserWindow.location.hostname : '';
    const isLocalDev = isLocalHostname(currentHostname);
    const runtimeOverride = normalizeApiBaseCandidate(readRuntimeApiOverride());

    if (runtimeOverride) {
        return runtimeOverride;
    }

    // Si no hay variable de entorno, detectar automáticamente
    if (!trimmed || trimmed === ':8080') {
        // En despliegue (no local): usar mismo origen (URL relativa)
        if (inBrowser && !isLocalDev) {
            return '';
        }
        // En desarrollo local (localhost/127.0.0.1/::1): backend en 8080
        return DEFAULT_API_BASE_URL;
    }

    const normalized = normalizeApiBaseCandidate(trimmed);
    if (!normalized) {
        return DEFAULT_API_BASE_URL;
    }

    const parsedUrl = new URL(normalized);

    // Guardrail en dev: evitar apuntar accidentalmente al mismo puerto del frontend
    // (ej. 3000/3001), que rompe WebSocket y llamadas API.
    if (
        inBrowser
        && isLocalDev
        && isLocalHostname(parsedUrl.hostname)
        && parsedUrl.port
        && parsedUrl.port === browserWindow.location.port
    ) {
        return DEFAULT_API_BASE_URL;
    }

    return `${parsedUrl.protocol}//${parsedUrl.host}`;
};

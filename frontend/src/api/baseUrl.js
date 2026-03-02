const DEFAULT_API_BASE_URL = 'http://localhost:8080';

export const getApiBaseUrl = () => {
    const rawUrl = process.env.REACT_APP_API_URL;
    const trimmed = rawUrl?.trim();

    if (!trimmed || trimmed === ':8080') {
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

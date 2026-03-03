const URL_REGEX = /((https?:\/\/|www\.)[^\s<]+)/i;

/**
 * Extrae la primera URL de un texto.
 * @param {string} text - Texto del mensaje.
 * @returns {string|null} URL normalizada o null si no hay ninguna.
 */
export const extractFirstUrl = (text) => {
    if (!text || typeof text !== 'string') {
        return null;
    }

    const match = text.match(URL_REGEX);
    if (!match || !match[0]) {
        return null;
    }

    let candidate = match[0].trim();
    candidate = candidate.replace(/[),.;!?]+$/g, '');

    if (candidate.startsWith('www.')) {
        candidate = `https://${candidate}`;
    }

    return candidate;
};

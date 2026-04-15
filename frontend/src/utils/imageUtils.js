import { getApiBaseUrl } from '../api/baseUrl';

export const DEFAULT_COMMUNITY_IMAGE =
  'https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&w=400&q=80';

export const DEFAULT_PROFILE_AVATAR =
  "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 120 120'%3E%3Ccircle cx='60' cy='60' r='60' fill='%23E6EAF3'/%3E%3Ccircle cx='60' cy='46' r='22' fill='%2395A1BB'/%3E%3Cpath d='M20 106c6-20 22-32 40-32s34 12 40 32' fill='%2395A1BB'/%3E%3C/svg%3E";

/**
 * Resuelve la URL de imagen de una comunidad, manejando valores inválidos y rutas relativas.
 * @param {Object} community - Objeto de comunidad con propiedades imagen/imagenUrl/foto
 * @returns {string} URL válida de imagen o DEFAULT_COMMUNITY_IMAGE
 */
export const resolveCommunityImage = (community) => {
  const raw = community?.imagen || community?.imagenUrl || community?.foto;

  if (!raw || !String(raw).trim()) {
    return DEFAULT_COMMUNITY_IMAGE;
  }

  const value = String(raw).trim();
  const normalizedValue = value.toLowerCase();

  if (normalizedValue === 'empty' || normalizedValue === 'null' || normalizedValue === 'undefined') {
    return DEFAULT_COMMUNITY_IMAGE;
  }

  // URLs absolutas y Data URIs (base64, blob)
  if (/^https?:\/\//i.test(value) || value.startsWith('data:') || value.startsWith('blob:')) {
    return value;
  }

  // Rutas relativas - convertir a URLs absolutas
  const base = getApiBaseUrl();
  if (value.startsWith('/')) {
    return `${base}${value}`;
  }

  return `${base}/${value}`;
};

/**
 * Resuelve la URL de foto de un usuario.
 * @param {string} rawPhoto - URL o path de foto del usuario
 * @returns {string} URL válida de foto o DEFAULT_PROFILE_AVATAR
 */
export const resolveUserImage = (rawPhoto) => {
  if (!rawPhoto || !String(rawPhoto).trim()) {
    return DEFAULT_PROFILE_AVATAR;
  }

  const value = String(rawPhoto).trim();
  const normalizedValue = value.toLowerCase();

  if (normalizedValue === 'empty' || normalizedValue === 'null' || normalizedValue === 'undefined') {
    return DEFAULT_PROFILE_AVATAR;
  }

  if (/^https?:\/\//i.test(value) || value.startsWith('data:') || value.startsWith('blob:')) {
    return value;
  }

  const base = getApiBaseUrl();
  if (value.startsWith('/')) {
    return `${base}${value}`;
  }

  return `${base}/${value}`;
};

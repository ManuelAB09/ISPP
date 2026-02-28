// src/api/auth.api.js
import { apiClient } from './client';

export const authApi = {
  /**
   * POST /api/v1/auth/login
   * Iniciar sesión con email y contraseña
   * @param {Object} data - { email: string, password: string }
   * @returns {Promise<Object>} - { accessToken, refreshToken, expiresIn, user }
   */
  login(data) {
    return apiClient.post('/api/v1/auth/login', data);
  },

  /**
   * POST /api/v1/auth/register
   * Registrar nuevo usuario
   * @param {Object} data - { email, password, nombre }
   * @returns {Promise<Object>} - { accessToken, refreshToken, expiresIn, user }
   */
  register(data) {
    return apiClient.post('/api/v1/auth/register', data);
  },

  /**
   * POST /api/v1/auth/refresh
   * Renovar token de acceso
   * @param {string} refreshToken
   * @returns {Promise<Object>} - { accessToken, refreshToken, expiresIn }
   */
  refresh(refreshToken) {
    return apiClient.post('/api/v1/auth/refresh', { refreshToken });
  },

  /**
   * GET /api/v1/users/me
   * Obtener perfil del usuario autenticado
   * @returns {Promise<Object>} - UserResponse
   */
  getMe() {
    return apiClient.get('/api/v1/users/me');
  },
};
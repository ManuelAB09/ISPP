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
   * @param {Object} data - { email, password, nombre, profileImage? }
   * @returns {Promise<Object>} - { accessToken, refreshToken, expiresIn, user }
   */
  register(data) {
    const { profileImage, ...userData } = data;
    
    // Si hay imagen, usar FormData
    if (profileImage) {
      const formData = new FormData();
      formData.append('email', userData.email);
      formData.append('password', userData.password);
      formData.append('nombre', userData.nombre);
      formData.append('profileImage', profileImage);
      return apiClient.post('/api/v1/auth/register', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
    }
    
    // Sin imagen, enviar JSON normal
    return apiClient.post('/api/v1/auth/register', userData);
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
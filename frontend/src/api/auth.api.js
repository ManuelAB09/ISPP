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

  /**
   * GET /api/v1/users/{userId}
   * Obtener perfil público de un usuario
   * @param {number|string} userId
   * @returns {Promise<Object>} - UserPublicResponse
   */
  getUserById(userId) {
    return apiClient.get(`/api/v1/users/${userId}`);
  },

  /**
   * PUT /api/v1/users/me
   * Actualizar perfil del usuario autenticado
    * @param {Object} data - { nombre, foto, bio, universidad, grado, nivelEstudios, baseFormativa, ubicacion, intereses }
   * @returns {Promise<Object>} - UserResponse actualizado
   */
  updateMe(data) {
    return apiClient.put('/api/v1/users/me', data);
  },

  /**
   * POST /api/v1/users/me/photo
   * Subir foto de perfil personalizada
   * @param {File} file - Imagen de perfil
   * @returns {Promise<Object>} - UserResponse actualizado
   */
  uploadProfilePhoto(file) {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.post('/api/v1/users/me/photo', formData);
  },

  /**
   * GET /api/v1/users/:userId
   * Obtener perfil público de un usuario por ID
   * @param {number} userId
   * @returns {Promise<Object>} - { id, nombre, foto, bio, ... }
   */
  getUserPublicProfile(userId) {
    return apiClient.get(`/api/v1/users/${userId}`);
  },

  /**
   * GET /api/v1/users/profile-avatars
   * Obtener avatares predefinidos para foto de perfil
   * @returns {Promise<string[]>} - Lista de rutas públicas de avatares
   */
  getProfileAvatars() {
    return apiClient.get('/api/v1/users/profile-avatars');
  },

  /**
   * GET /api/v1/auth/verify
   * Verificar email con token
   * @param {string} token - Token de verificación
   * @returns {Promise<Object>} - { accessToken, user }
   */
  verifyEmail(token) {
    return apiClient.get(`/api/v1/auth/verify?token=${encodeURIComponent(token)}`);
  },

  /**
   * POST /api/v1/auth/resend-verification
   * Reenviar email de verificación
   * @param {string} email - Email del usuario
   * @returns {Promise<Object>} - { message }
   */
  resendVerification(email) {
    return apiClient.post('/api/v1/auth/resend-verification', { email });
  },
};
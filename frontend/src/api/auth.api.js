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

  // --- Google Auth ---

  /**
   * POST /api/v1/auth/google
   * Iniciar sesión o registrarse con Google
   * @param {Object} data - { idToken: string, requestClassroomAccess: boolean }
   * @returns {Promise<Object>} - GoogleAuthResponse { authResponse, requestClassroomAccess }
   */
  loginWithGoogle(data) {
    return apiClient.post('/api/v1/auth/google', data);
  },

  /**
   * POST /api/v1/auth/google/link
   * Vincular cuenta de Google al usuario autenticado
   * @param {Object} data - { idToken: string, requestClassroomAccess: boolean }
   * @returns {Promise<Object>} - MessageResponse
   */
  linkGoogle(data) {
    return apiClient.post('/api/v1/auth/google/link', data);
  },

  /**
   * POST /api/v1/auth/google/unlink
   * Desvincular cuenta de Google del usuario autenticado
   * @returns {Promise<Object>} - MessageResponse
   */
  unlinkGoogle() {
    return apiClient.post('/api/v1/auth/google/unlink');
  },

  // --- 2FA ---

  /**
   * POST /api/v1/auth/2fa/login
   * Completar inicio de sesión con código 2FA
   * @param {Object} data - { tempToken: string, code: string }
   * @returns {Promise<Object>} - AuthResponse
   */
  login2fa(data) {
    return apiClient.post('/api/v1/auth/2fa/login', data);
  },

  /**
   * POST /api/v1/auth/2fa/setup
   * Generar clave TOTP y URL para código QR
   * @returns {Promise<Object>} - TotpSetupResponse { secret: string, qrCodeUrl: string }
   */
  setup2fa() {
    return apiClient.post('/api/v1/auth/2fa/setup');
  },

  /**
   * POST /api/v1/auth/2fa/enable
   * Activar 2FA verificando el primer código
   * @param {string} code - Código TOTP de la app autenticadora
   * @returns {Promise<Object>} - MessageResponse
   */
  enable2fa(code) {
    return apiClient.post('/api/v1/auth/2fa/enable', { code });
  },

  /**
   * POST /api/v1/auth/2fa/disable
   * Desactivar 2FA verificando un código actual
   * @param {string} code - Código TOTP de la app autenticadora
   * @returns {Promise<Object>} - MessageResponse
   */
  disable2fa(code) {
    return apiClient.post('/api/v1/auth/2fa/disable', { code });
  },
};
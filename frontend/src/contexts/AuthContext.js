import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { authApi } from '../api/auth.api';
import { apiClient } from '../api/client';

const AuthContext = createContext(null);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth debe usarse dentro de un AuthProvider');
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Inicializar auth desde localStorage
  useEffect(() => {
    const initAuth = async () => {
      const token = localStorage.getItem('accessToken');
      if (token) {
        apiClient.setToken(token);
        try {
          const userData = await authApi.getMe();
          setUser(userData);
        } catch (err) {
          // Token inválido o expirado
          localStorage.removeItem('accessToken');
          apiClient.setToken(null);
        }
      }
      setLoading(false);
    };

    initAuth();
  }, []);

  const login = useCallback(async (email, password) => {
    setError(null);
    try {
      const response = await authApi.login({ email, password });
      const { accessToken, user: userData } = response;
      
      localStorage.setItem('accessToken', accessToken);
      apiClient.setToken(accessToken);
      setUser(userData);
      
      return { success: true };
    } catch (err) {
      const message = err.message || 'Error al iniciar sesión';
      setError(message);
      return { success: false, error: message };
    }
  }, []);

  const register = useCallback(async (email, password, nombre) => {
    setError(null);
    try {
      const response = await authApi.register({ email, password, nombre });
      const { accessToken, user: userData } = response;
      
      localStorage.setItem('accessToken', accessToken);
      apiClient.setToken(accessToken);
      setUser(userData);
      
      return { success: true };
    } catch (err) {
      const message = err.message || 'Error al registrarse';
      setError(message);
      return { success: false, error: message };
    }
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('accessToken');
    apiClient.setToken(null);
    setUser(null);
  }, []);

  const value = {
    user,
    loading,
    error,
    isAuthenticated: !!user,
    login,
    register,
    logout,
    clearError: () => setError(null),
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

export default AuthContext;

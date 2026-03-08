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

  // Función auxiliar para guardar datos del usuario en localStorage
  const saveUserToStorage = (userData, extraData = {}) => {
    // Combinar datos de la API con datos extra (como universidad, grado, ubicacion que la API no devuelve)
    const fullUserData = {
      id: userData.id,
      email: userData.email,
      nombre: userData.nombre,
      foto: userData.foto,
      bio: userData.bio,
      intereses: userData.intereses,
      visibleEnListados: userData.visibleEnListados,
      esTutor: userData.esTutor,
      createdAt: userData.createdAt,
      // Campos que la API acepta en PUT pero no devuelve en GET
      universidad: extraData.universidad ?? userData.universidad ?? '',
      grado: extraData.grado ?? userData.grado ?? '',
      ubicacion: extraData.ubicacion ?? userData.ubicacion ?? '',
    };
    localStorage.setItem('userId', String(userData.id));
    localStorage.setItem('userProfile', JSON.stringify(fullUserData));
    return fullUserData;
  };

  // Función auxiliar para obtener datos guardados del localStorage
  const getStoredUserData = () => {
    try {
      const stored = localStorage.getItem('userProfile');
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  };

  // Función auxiliar para limpiar datos del usuario del localStorage
  const clearUserFromStorage = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('userId');
    localStorage.removeItem('userProfile');
  };

  // Inicializar auth desde localStorage
  useEffect(() => {
    const initAuth = async () => {
      const token = localStorage.getItem('accessToken');
      if (token) {
        apiClient.setToken(token);
        const storedData = getStoredUserData();
        try {
          const userData = await authApi.getMe();
          // Combinar datos de la API con los guardados localmente (para campos que la API no devuelve)
          const combinedUser = {
            ...userData,
            universidad: storedData?.universidad ?? userData.universidad ?? '',
            grado: storedData?.grado ?? userData.grado ?? '',
            ubicacion: userData.ubicacion ?? storedData?.ubicacion ?? '',
          };
          setUser(combinedUser);
          saveUserToStorage(userData, {
            ...(storedData || {}),
            universidad: combinedUser.universidad,
            grado: combinedUser.grado,
            ubicacion: combinedUser.ubicacion,
          });
        } catch (err) {
          const status = err?.status;
          const unauthorized = status === 401 || status === 403;
          if (unauthorized) {
            clearUserFromStorage();
            apiClient.setToken(null);
          } else if (storedData) {
            setUser(storedData);
          }
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
      saveUserToStorage(userData);
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
      saveUserToStorage(userData);
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
    clearUserFromStorage();
    apiClient.setToken(null);
    setUser(null);
  }, []);

  const updateProfile = useCallback(async (profileData, ubicacionSeleccionada = null) => {
    setError(null);
    try {
      const requestPayload = {
        ...profileData,
        ubicacion:
          typeof profileData.ubicacion === 'string'
            ? profileData.ubicacion
            : (profileData.ubicacion?.nombre || ''),
      };

      const updatedUser = await authApi.updateMe(requestPayload);
      // La API no devuelve universidad, grado, ubicacion, así que los mantenemos del profileData enviado
      const combinedUser = {
        ...updatedUser,
        universidad: requestPayload.universidad ?? '',
        grado: requestPayload.grado ?? '',
        ubicacion: ubicacionSeleccionada ?? updatedUser.ubicacion ?? requestPayload.ubicacion ?? '',
      };
      setUser(combinedUser);
      saveUserToStorage(updatedUser, {
        universidad: combinedUser.universidad,
        grado: combinedUser.grado,
        ubicacion: combinedUser.ubicacion,
      });
      return { success: true, user: combinedUser };
    } catch (err) {
      const message = err.message || 'Error al actualizar el perfil';
      setError(message);
      return { success: false, error: message };
    }
  }, []);

  const value = {
    user,
    loading,
    error,
    isAuthenticated: !!user,
    login,
    register,
    logout,
    updateProfile,
    clearError: () => setError(null),
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

export default AuthContext;

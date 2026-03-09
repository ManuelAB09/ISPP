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
    // Combinar datos de la API con datos extra del formulario de perfil.
    const fullUserData = {
      id: userData.id,
      email: userData.email,
      nombre: userData.nombre,
      foto: userData.foto,
      fotoBackgroundColor: extraData.fotoBackgroundColor ?? userData.fotoBackgroundColor ?? '#ffffff',
      bio: userData.bio,
      intereses: userData.intereses,
      visibleEnListados: userData.visibleEnListados,
      esTutor: userData.esTutor,
      autenticacionDosFactores:
        extraData.autenticacionDosFactores ?? userData.autenticacionDosFactores ?? false,
      notificacionesEmail: extraData.notificacionesEmail ?? userData.notificacionesEmail ?? true,
      notificacionesPush: extraData.notificacionesPush ?? userData.notificacionesPush ?? true,
      createdAt: userData.createdAt,
      // Campos que la API acepta en PUT pero no devuelve en GET
      universidad: extraData.universidad ?? userData.universidad ?? '',
      grado: extraData.grado ?? userData.grado ?? '',
      nivelEstudios: extraData.nivelEstudios ?? userData.nivelEstudios ?? '',
      baseFormativa: extraData.baseFormativa ?? userData.baseFormativa ?? '',
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
            universidad: storedData?.universidad ?? '',
            grado: storedData?.grado ?? '',
            nivelEstudios: storedData?.nivelEstudios ?? '',
            baseFormativa: storedData?.baseFormativa ?? '',
            ubicacion: storedData?.ubicacion ?? '',
            fotoBackgroundColor: storedData?.fotoBackgroundColor ?? userData?.fotoBackgroundColor ?? '#ffffff',
            autenticacionDosFactores:
              storedData?.autenticacionDosFactores ?? userData?.autenticacionDosFactores ?? false,
            notificacionesEmail:
              storedData?.notificacionesEmail ?? userData?.notificacionesEmail ?? true,
            notificacionesPush:
              storedData?.notificacionesPush ?? userData?.notificacionesPush ?? false,
          };
          setUser(combinedUser);
          saveUserToStorage(userData, storedData || {});
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
      const fullUser = saveUserToStorage(userData);
      apiClient.setToken(accessToken);
      setUser(fullUser);

      return { success: true };
    } catch (err) {
      const message = err.message || 'Error al iniciar sesión';
      setError(message);
      return { success: false, error: message };
    }
  }, []);

  const register = useCallback(async (email, password, nombre, esTutor = false) => {
    setError(null);
    try {
      const response = await authApi.register({ email, password, nombre, esTutor });
      const { accessToken, user: userData } = response;

      localStorage.setItem('accessToken', accessToken);
      const fullUser = saveUserToStorage(userData);
      apiClient.setToken(accessToken);
      setUser(fullUser);

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

  const updateProfile = useCallback(async (profileData) => {
    setError(null);
    try {
      const updatedUser = await authApi.updateMe(profileData);
      // Combinamos payload parcial + respuesta + estado previo para no perder campos no enviados.
      const combinedUser = {
        ...(user || {}),
        ...updatedUser,
        universidad:
          profileData.universidad ?? updatedUser.universidad ?? user?.universidad ?? '',
        grado: profileData.grado ?? updatedUser.grado ?? user?.grado ?? '',
        nivelEstudios:
          profileData.nivelEstudios ?? updatedUser.nivelEstudios ?? user?.nivelEstudios ?? '',
        baseFormativa:
          profileData.baseFormativa ?? updatedUser.baseFormativa ?? user?.baseFormativa ?? '',
        ubicacion: profileData.ubicacion ?? updatedUser.ubicacion ?? user?.ubicacion ?? '',
        fotoBackgroundColor: profileData.fotoBackgroundColor ?? updatedUser.fotoBackgroundColor ?? '#ffffff',
        visibleEnListados:
          profileData.visibleEnListados
          ?? updatedUser.visibleEnListados
          ?? user?.visibleEnListados
          ?? true,
        autenticacionDosFactores:
          profileData.autenticacionDosFactores
          ?? updatedUser.autenticacionDosFactores
          ?? user?.autenticacionDosFactores
          ?? false,
        notificacionesEmail:
          profileData.notificacionesEmail
          ?? updatedUser.notificacionesEmail
          ?? user?.notificacionesEmail
          ?? true,
        notificacionesPush:
          profileData.notificacionesPush
          ?? updatedUser.notificacionesPush
          ?? user?.notificacionesPush
          ?? false,
      };
      setUser(combinedUser);
      saveUserToStorage(updatedUser, profileData);
      return { success: true, user: combinedUser };
    } catch (err) {
      const message = err.message || 'Error al actualizar el perfil';
      setError(message);
      return { success: false, error: message, validationErrors: err.errors || {} };
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

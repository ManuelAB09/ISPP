import React, { useState } from 'react';
import { getApiBaseUrl } from '../api/baseUrl';

const GoogleAuthButton = ({ onSuccess, onError, text = "Continuar con Google", flowType = "login" }) => {
  const [loading, setLoading] = useState(false);

  const handleGoogleClick = async () => {
    setLoading(true);
    try {
      const API_URL = getApiBaseUrl();
      const endpoint = flowType === 'link'
        ? `${API_URL}/api/v1/auth/google/link/authorize`
        : `${API_URL}/api/v1/auth/google/authorize`;

      const response = await fetch(endpoint, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('accessToken')}` // Para el link necesitamos el token actual
        }
      });
      
      if (!response.ok) throw new Error('Error al contactar con el servidor');
      const data = await response.json();
      
      if (data.url) {
        const width = 500;
        const height = 600;
        const left = window.screen.width / 2 - width / 2;
        const top = window.screen.height / 2 - height / 2;
        window.open(data.url, 'GoogleAuth', `width=${width},height=${height},top=${top},left=${left}`);

        const messageListener = (event) => {
          // Filtrar origines en prod, de momento validamos la estructura
          if (event.data?.type === 'google-auth-success') {
            window.removeEventListener('message', messageListener);
            onSuccess(event.data.payload); // Devuelve JWT, user, isTwoFactor...
          } else if (event.data?.type === 'google-auth-error') {
            window.removeEventListener('message', messageListener);
            onError(event.data.error || 'Autenticación fallida');
          } else if (event.data?.type === 'google-link-success') {
            window.removeEventListener('message', messageListener);
            onSuccess({}); // Success sin body
          }
        };
        window.addEventListener('message', messageListener);
      }
    } catch (err) {
      onError(err.message || 'Error al iniciar sesión con Google');
    } finally {
      setLoading(false);
    }
  };

  return (
    <button
      type="button"
      onClick={handleGoogleClick}
      disabled={loading}
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: '#fff',
        color: '#3c4043',
        border: '1px solid #dadce0',
        borderRadius: '4px',
        padding: '10px 15px',
        fontSize: '14px',
        fontWeight: '500',
        cursor: loading ? 'not-allowed' : 'pointer',
        width: '100%',
        fontFamily: 'Roboto, arial, sans-serif',
        boxShadow: '0 1px 2px 0 rgba(60,64,67,0.3), 0 1px 3px 1px rgba(60,64,67,0.15)',
        transition: 'background-color 0.2s',
        opacity: loading ? 0.7 : 1
      }}
      onMouseOver={(e) => { if (!loading) e.currentTarget.style.backgroundColor = '#f8f9fa'; }}
      onMouseOut={(e) => { if (!loading) e.currentTarget.style.backgroundColor = '#fff'; }}
    >
      <svg version="1.1" xmlns="http://www.w3.org/2000/svg" width="18px" height="18px" viewBox="0 0 48 48" style={{ marginRight: '10px' }}>
        <g>
          <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"></path>
          <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"></path>
          <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"></path>
          <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"></path>
          <path fill="none" d="M0 0h48v48H0z"></path>
        </g>
      </svg>
      {loading ? 'Conectando...' : text}
    </button>
  );
};

export default GoogleAuthButton;

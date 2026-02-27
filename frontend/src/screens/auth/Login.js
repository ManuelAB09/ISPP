import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import './Login.css';
import studyShareLogo from '../../static/images/MeerKatters_logo.png';
import { authApi } from '../../api/auth.api';
import { apiClient } from '../../api/client';

const Login = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    rememberMe: false,
  });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const response = await authApi.login({
        email: formData.email,
        password: formData.password,
      });
      // Guardar token y datos del usuario
      localStorage.setItem('accessToken', response.accessToken);
      if (response.user) {
        localStorage.setItem('userId', response.user.id);
        localStorage.setItem('userName', response.user.nombre || '');
        localStorage.setItem('userEmail', response.user.email || '');
      }
      // Configurar el token en el apiClient para futuras peticiones
      apiClient.setToken(response.accessToken);
      navigate('/');
    } catch (err) {
      console.error('Error al iniciar sesión:', err);
      setError(err.message || 'Credenciales incorrectas. Inténtalo de nuevo.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      {/* Left Panel - Branding */}
      <div className="login-left-panel">
        <div className="login-brand-content">
          <div className="login-logo-wrapper">
            <img src={studyShareLogo} alt="MeerKatters Logo" className="login-logo-img" />
          </div>

          <h1 className="login-brand-title">MeerKatters</h1>
          <p className="login-brand-description">
            La plataforma comunitaria moderna para el aprendizaje colaborativo y el intercambio de recursos.
          </p>

          <div className="login-feature-buttons">
            <button type="button" className="login-feature-btn">
              <span className="login-feature-icon">📚</span>
              <span>Notas compartidas</span>
            </button>
            <button type="button" className="login-feature-btn">
              <span className="login-feature-icon">👥</span>
              <span>Grupos de estudio</span>
            </button>
          </div>
        </div>
      </div>

      {/* Right Panel - Form */}
      <div className="login-right-panel">
        <div className="login-form-container">
          <div className="login-header">
            <h2>Bienvenido</h2>
            <p>Por favor, inserte sus datos para iniciar sesión</p>
          </div>

          <form onSubmit={handleSubmit} className="login-form">
            {error && (
              <div className="login-error" style={{
                background: '#fef2f2',
                color: '#dc2626',
                padding: '10px 14px',
                borderRadius: '8px',
                marginBottom: '16px',
                fontSize: '14px',
                border: '1px solid #fecaca'
              }}>
                {error}
              </div>
            )}
            <div className="form-group">
              <label htmlFor="email">Correo electrónico</label>
              <div className="input-with-icon">
                <svg className="input-icon" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" >
                  <rect x="2" y="4" width="20" height="16" rx="2"/>
                  <path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/>
                </svg>
                <input
                  type="email"
                  id="email"
                  name="email"
                  placeholder="nombre@universidadDeSevilla.mola"
                  value={formData.email}
                  onChange={handleInputChange}
                  required
                />
              </div>
            </div>

            <div className="form-group">
              <div className="label-row">
                <label htmlFor="password">Contraseña</label>
                <Link to="/forgot-password" className="forgot-password-link">
                  Olvidaste la contraseña?
                </Link>
              </div>
              <div className="input-with-icon password-input-container">
                <svg className="input-icon" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                  <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                </svg>
                <input
                  type={showPassword ? 'text' : 'password'}
                  id="password"
                  name="password"
                  placeholder="••••••••"
                  value={formData.password}
                  onChange={handleInputChange}
                  required
                />
                <button
                  type="button"
                  className="password-toggle"
                  onClick={() => setShowPassword(!showPassword)}
                  aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                >
                  {showPassword ? (
                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                      <line x1="1" y1="1" x2="23" y2="23"/>
                    </svg>
                  ) : (
                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                      <circle cx="12" cy="12" r="3"/>
                    </svg>
                  )}
                </button>
              </div>
            </div>

            <div className="form-group checkbox-group">
              <label className="checkbox-label">
                <input
                  type="checkbox"
                  name="rememberMe"
                  checked={formData.rememberMe}
                  onChange={handleInputChange}
                />
                <span className="checkbox-custom"></span>
                <span className="checkbox-text">Recordarme por 30 días</span>
              </label>
            </div>

            <button type="submit" className="login-button" disabled={loading}>
              {loading ? 'Iniciando sesión...' : 'Iniciar sesión'}
            </button>
          </form>

          <div className="register-link">
            <p>
              No tienes cuenta todavía? <Link to="/register">Crear cuenta</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Login;

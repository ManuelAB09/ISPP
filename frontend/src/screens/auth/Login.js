import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import './Login.css';
import studyShareLogo from '../../static/images/studyShare_logo.png';

const Login = () => {
  const navigate = useNavigate();
  const { login, error: authError, clearError, isAuthenticated, loading } = useAuth();
  
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    rememberMe: false,
  });
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  // Si ya está autenticado, mostrar mensaje
  if (!loading && isAuthenticated) {
    return (
      <div className="login-container">
        <div className="login-already-logged">
          <div className="login-already-logged__icon">✓</div>
          <h1 className="login-already-logged__title">Ya has iniciado sesión</h1>
          <p className="login-already-logged__text">
            Ya tienes una sesión activa en la aplicación.
          </p>
          <div className="login-already-logged__buttons">
            <Link to="/" className="btn-home">Ir al inicio</Link>
            <Link to="/perfil" className="btn-profile">Ver mi perfil</Link>
          </div>
        </div>
      </div>
    );
  }

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));
    // Limpiar errores al escribir
    if (error) setError('');
    if (authError) clearError();
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');

    const result = await login(formData.email, formData.password);
    
    if (result.success) {
      navigate('/');
    } else {
      setError(result.error || 'Error al iniciar sesión');
    }
    
    setIsLoading(false);
  };

  return (
    <div className="login-container">
      {/* Left Panel - Branding */}
      <div className="login-left-panel">
        <div className="login-brand-content">
          <Link to="/" className="login-logo-wrapper">
            <img src={studyShareLogo} alt="MeerKatters Logo" className="login-logo-img" />
          </Link>

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

            {error && (
              <div className="login-error-message">
                {error}
              </div>
            )}

            <button type="submit" className="login-button" disabled={isLoading}>
              {isLoading ? 'Iniciando sesión...' : 'Iniciar sesión'}
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

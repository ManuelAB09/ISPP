import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { GoogleLogin } from '@react-oauth/google';
import './Login.css';
import studyShareLogo from '../../static/images/MeerKatters_logo.png';

const Login = () => {
  const navigate = useNavigate();
  const { login, loginWithGoogle, login2fa, resendVerification, error: authError, clearError, isAuthenticated, loading } = useAuth();
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    rememberMe: false,
  });
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [emailNotVerified, setEmailNotVerified] = useState(false);
  const [resendLoading, setResendLoading] = useState(false);
  const [resendMessage, setResendMessage] = useState('');

  // 2FA state
  const [show2FA, setShow2FA] = useState(false);
  const [totpCode, setTotpCode] = useState('');
  const [tempToken, setTempToken] = useState('');

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
    if (emailNotVerified) setEmailNotVerified(false);
    if (resendMessage) setResendMessage('');
    if (authError) clearError();
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    setIsLoading(true);
    setError('');
    setEmailNotVerified(false);
    setResendMessage('');

    // Validaciones frontend
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!formData.email.trim()) {
      setError('El correo electrónico no puede estar vacío');
      setIsLoading(false);
      return;
    }
    if (!emailRegex.test(formData.email)) {
      setError('Introduce un correo electrónico válido (ej: usuario@dominio.com)');
      setIsLoading(false);
      return;
    }
    if (!formData.password) {
      setError('La contraseña no puede estar vacía');
      setIsLoading(false);
      return;
    }

    const result = await login(formData.email, formData.password);

    if (result.success) {
      if (result.isTwoFactor) {
        setTempToken(result.tempToken);
        setShow2FA(true);
      } else {
        navigate('/');
      }
    } else {
      const errorMsg = result.error || 'Error al iniciar sesión';
      // Detectar si el error es por email no verificado
      if (errorMsg.toLowerCase().includes('verificar') || errorMsg.toLowerCase().includes('verificado')) {
        setEmailNotVerified(true);
      }
      setError(errorMsg);
    }

    setIsLoading(false);
  };

  const handle2FASubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');

    const result = await login2fa(tempToken, totpCode);
    if (result.success) {
      navigate('/');
    } else {
      setError(result.error || 'Código 2FA incorrecto');
    }
    setIsLoading(false);
  };

  const handleGoogleSuccess = async (credentialResponse) => {
    setIsLoading(true);
    setError('');
    const idToken = credentialResponse.credential;
    const result = await loginWithGoogle(idToken, true);

    if (result.success) {
      if (result.isTwoFactor) {
        setTempToken(result.tempToken);
        setShow2FA(true);
      } else {
        navigate('/');
      }
    } else {
      setError(result.error || 'Error al iniciar sesión con Google');
    }
    setIsLoading(false);
  };

  const handleResendVerification = async () => {
    setResendLoading(true);
    setResendMessage('');
    
    const result = await resendVerification(formData.email);
    
    if (result.success) {
      setResendMessage('Email de verificación reenviado correctamente. Revisa tu bandeja de entrada.');
    } else {
      setResendMessage(result.error || 'Error al reenviar el email');
    }
    
    setResendLoading(false);
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
            <button type="button" className="login-feature-btn" aria-describedby="tooltip-notas">
              <span className="login-feature-icon">📚</span>
              <span>Notas compartidas</span>
              <span id="tooltip-notas" className="login-feature-tooltip">Comparte y descarga apuntes organizados por asignatura.</span>
            </button>
            <button type="button" className="login-feature-btn" aria-describedby="tooltip-grupos">
              <span className="login-feature-icon">👥</span>
              <span>Grupos de estudio</span>
              <span id="tooltip-grupos" className="login-feature-tooltip">Crea o únete a grupos para estudiar con compañeros.</span>
            </button>
          </div>
        </div>
      </div>

      {/* Right Panel - Form */}
      <div className="login-right-panel">
        <div className="login-form-container">
          
          {show2FA ? (
            <div className="login-2fa-container">
              <div className="login-header">
                <h2>Verificación en dos pasos</h2>
                <p>Introduce el código de tu aplicación de autenticación (ej. Google Authenticator)</p>
              </div>

              <form onSubmit={handle2FASubmit} className="login-form">
                <div className="form-group">
                  <label htmlFor="totpCode">Código 2FA</label>
                  <input
                    type="text"
                    id="totpCode"
                    value={totpCode}
                    onChange={(e) => setTotpCode(e.target.value)}
                    placeholder="000111"
                    maxLength="6"
                    required
                    style={{ letterSpacing: '2px', textAlign: 'center', fontSize: '1.2rem' }}
                  />
                </div>
                {error && <div className="login-error-message">{error}</div>}
                <button type="submit" className="login-button" disabled={isLoading}>
                  {isLoading ? 'Verificando...' : 'Verificar código'}
                </button>
                <button type="button" className="login-button secondary" onClick={() => setShow2FA(false)} style={{ marginTop: '10px', background: 'transparent', border: '1px solid #ccc', color: '#333' }}>
                  Atrás
                </button>
              </form>
            </div>
          ) : (
            <>
              <div className="login-header">
                <h2>Bienvenido</h2>
                <p>Por favor, inserte sus datos para iniciar sesión</p>
              </div>

              <form onSubmit={handleSubmit} className="login-form">
            <div className="form-group">
              <label htmlFor="email">Correo electrónico</label>
              <input
                type="email"
                id="email"
                name="email"
                placeholder="tu@correo.com"
                value={formData.email}
                onChange={handleInputChange}
                required
              />
            </div>

            <div className="form-group">
              <div className="label-row">
                <label htmlFor="password">Contraseña</label>
                <Link to="/forgot-password" className="forgot-password-link">
                  Olvidaste la contraseña?
                </Link>
              </div>
              <div className="password-input-container">
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
                      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
                      <line x1="1" y1="1" x2="23" y2="23" />
                    </svg>
                  ) : (
                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                      <circle cx="12" cy="12" r="3" />
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
                {emailNotVerified && (
                  <div style={{ marginTop: '10px' }}>
                    <button
                      type="button"
                      onClick={handleResendVerification}
                      disabled={resendLoading || !formData.email}
                      style={{
                        background: 'none',
                        border: 'none',
                        color: '#3b82f6',
                        textDecoration: 'underline',
                        cursor: resendLoading ? 'not-allowed' : 'pointer',
                        padding: 0,
                        fontSize: '14px'
                      }}
                    >
                      {resendLoading ? 'Enviando...' : 'Reenviar email de verificación'}
                    </button>
                  </div>
                )}
              </div>
            )}
            
            {resendMessage && (
              <div style={{
                padding: '10px',
                borderRadius: '8px',
                marginBottom: '15px',
                backgroundColor: resendMessage.includes('Error') ? '#fee2e2' : '#d1fae5',
                color: resendMessage.includes('Error') ? '#991b1b' : '#065f46',
                fontSize: '14px'
              }}>
                {resendMessage}
              </div>
            )}

            <button type="submit" className="login-button" disabled={isLoading}>
              {isLoading ? 'Iniciando sesión...' : 'Iniciar sesión'}
            </button>
          </form>

          {/* Import GoogleLogin inside the component */}
          <div className="google-auth-container" style={{ marginTop: '20px', display: 'flex', justifyContent: 'center' }}>
            {/* Usamos require en lugar de import estático para evitar problemas si falla la inicialización */}
            <div style={{ width: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
              <div style={{ display: 'flex', alignItems: 'center', margin: '15px 0', width: '100%' }}>
                <div style={{ flex: 1, height: '1px', backgroundColor: '#e2e8f0' }}></div>
                <span style={{ padding: '0 10px', color: '#94a3b8', fontSize: '0.9rem' }}>o continuar con</span>
                <div style={{ flex: 1, height: '1px', backgroundColor: '#e2e8f0' }}></div>
              </div>
              
              <GoogleLogin
                onSuccess={handleGoogleSuccess}
                onError={() => {
                  setError('Fallo en el inicio de sesión de Google');
                }}
                useOneTap
                theme="outline"
                text="continue_with"
                shape="rectangular"
                width="100%"
              />
            </div>
          </div>

          <div className="register-link" style={{ marginTop: '15px' }}>
            <p>
              No tienes cuenta todavía? <Link to="/register">Crear cuenta</Link>
            </p>
          </div>
        </>
        )}
        </div>
      </div>
    </div>
  );
};

export default Login;

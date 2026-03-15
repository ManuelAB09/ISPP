// filepath: c:\Users\juana\OneDrive\Escritorio\Juan Antonio\Universidad\cuarto año\ISPP\ISPP\frontend\src\screens\auth\Register.js
import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import studyShareLogo from '../../static/images/MeerKatters_logo.png';
import { getPendingInvitationPath } from '../../utils/invitationFlow';
import './Register.css';

const Register = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { register, resendVerification, error: authError, clearError, isAuthenticated, loading } = useAuth();
  const nextParam = searchParams.get('next') || getPendingInvitationPath();
  const loginLink = nextParam ? `/login?next=${encodeURIComponent(nextParam)}` : '/login';
  
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    password: '',
    confirmPassword: '',
    acceptTerms: false,
    esTutor: false,
  });
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [registrationSuccess, setRegistrationSuccess] = useState(false);
  const [registeredEmail, setRegisteredEmail] = useState('');
  const [resendLoading, setResendLoading] = useState(false);
  const [resendMessage, setResendMessage] = useState('');

  // Si ya está autenticado, mostrar mensaje
  if (!loading && isAuthenticated) {
    return (
      <div className="register-container">
        <div className="register-already-logged">
          <div className="register-already-logged__icon">✓</div>
          <h1 className="register-already-logged__title">Ya has iniciado sesión</h1>
          <p className="register-already-logged__text">
            Ya tienes una sesión activa en la aplicación.
          </p>
          <div className="register-already-logged__buttons">
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
    
    // Validación de términos y condiciones
    if (!formData.acceptTerms) {
      setError('Debes aceptar los términos y condiciones para continuar');
      return;
    }

    // Validación de formato de correo
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!formData.email.match(emailRegex)) {
      setError('Introduce un correo electrónico válido (ej: usuario@dominio.com)');
      return;
    }

    // Validación de contraseñas coinciden
    if (formData.password !== formData.confirmPassword) {
      setError('Las contraseñas no coinciden');
      return;
    }

    // Validación de nombre
    if (!formData.fullName.trim()) {
      setError('El nombre no puede estar vacío');
      return;
    }
    if (formData.fullName.trim().length < 2) {
      setError('El nombre debe tener al menos 2 caracteres');
      return;
    }

    // Validación de contraseña
    if (formData.password.length < 8) {
      setError('La contraseña debe tener al menos 8 caracteres');
      return;
    }

    setIsLoading(true);
    setError('');

    const result = await register(formData.email, formData.password, formData.fullName, formData.esTutor);
    
    if (result.success && result.requiresVerification) {
      setRegistrationSuccess(true);
      setRegisteredEmail(formData.email);
    } else if (result.success) {
      navigate('/');
    } else {
      setError(result.error || 'Error al registrarse');
    }
    
    setIsLoading(false);
  };

  const handleResendVerification = async () => {
    setResendLoading(true);
    setResendMessage('');
    
    const result = await resendVerification(registeredEmail);
    
    if (result.success) {
      setResendMessage('Email de verificación reenviado correctamente');
    } else {
      setResendMessage(result.error || 'Error al reenviar el email');
    }
    
    setResendLoading(false);
  };

  // Si el registro fue exitoso, mostrar mensaje de verificación
  if (registrationSuccess) {
    return (
      <div className="register-container">
        <div className="register-already-logged">
          <div className="register-already-logged__icon" style={{ fontSize: '48px' }}>📧</div>
          <h1 className="register-already-logged__title">¡Revisa tu correo!</h1>
          <p className="register-already-logged__text">
            Hemos enviado un email de verificación a <strong>{registeredEmail}</strong>. 
            Por favor, haz clic en el enlace del email para activar tu cuenta.
          </p>
          <p className="register-already-logged__text" style={{ fontSize: '14px', color: '#666', marginTop: '10px' }}>
            El enlace expira en 24 horas. Si no recibes el email, revisa tu carpeta de spam.
          </p>
          {resendMessage && (
            <p style={{ 
              marginTop: '15px', 
              padding: '10px', 
              borderRadius: '8px',
              backgroundColor: resendMessage.includes('Error') ? '#fee2e2' : '#d1fae5',
              color: resendMessage.includes('Error') ? '#991b1b' : '#065f46'
            }}>
              {resendMessage}
            </p>
          )}
          <div className="register-already-logged__buttons" style={{ marginTop: '20px' }}>
            <button 
              onClick={handleResendVerification} 
              className="btn-profile"
              disabled={resendLoading}
            >
              {resendLoading ? 'Enviando...' : 'Reenviar email'}
            </button>
            <Link to={loginLink} className="btn-home">Ir a iniciar sesión</Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="register-container">
      {/* Left Panel - Branding */}
      <div className="register-left-panel">
        <Link to="/" className="register-logo">
          <img src={studyShareLogo} alt="MeerKatters Logo" className="register-logo-img" />
          <span className="logo-text">Meerkatters</span>
        </Link>

        <div className="register-hero">
          <h1>Comienza tu viaje hacia la maestría</h1>
          <p>
            Únete a más de 50,000 estudiantes que comparten apuntes, forman
            grupos de estudio y logran la excelencia académica juntos.
          </p>
        </div>

        <div className="register-features">
          <div className="feature-item">
            <span className="feature-icon">👥</span>
            <span>Grupos de estudio colaborativos</span>
          </div>
          <div className="feature-item">
            <span className="feature-icon">📄</span>
            <span>Biblioteca de recursos</span>
          </div>
        </div>

        <div className="register-footer">
          <p>© 2024 StudYshare. All rights reserved.</p>
        </div>
      </div>

      {/* Right Panel - Form */}
      <div className="register-right-panel">
        <div className="register-form-container">
          <div className="register-header">
            <h2>Crear cuenta</h2>
            <p>Únete a la comunidad en sencillos pasos</p>
          </div>

          <form onSubmit={handleSubmit} className="register-form">
            <div className="form-group">
              <label htmlFor="fullName">Nombre completo</label>
              <input
                type="text"
                id="fullName"
                name="fullName"
                placeholder="Nombre Apellidos"
                value={formData.fullName}
                onChange={handleInputChange}
                required
              />
            </div>

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
              <label htmlFor="password">Contraseña</label>
              <div className="password-input-container">
                <input
                  type={showPassword ? 'text' : 'password'}
                  id="password"
                  name="password"
                  placeholder="(Al menos 8 caracteres)"
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

            <div className="form-group">
              <label htmlFor="confirmPassword">Repetir contraseña</label>
              <div className="password-input-container">
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  id="confirmPassword"
                  name="confirmPassword"
                  placeholder="(Al menos 8 caracteres)"
                  value={formData.confirmPassword}
                  onChange={handleInputChange}
                  required
                />
                <button
                  type="button"
                  className="password-toggle"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  aria-label={showConfirmPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                >
                  {showConfirmPassword ? (
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
                  name="acceptTerms"
                  checked={formData.acceptTerms}
                  onChange={handleInputChange}
                />
                <span className="checkbox-custom"></span>
                <span className="checkbox-text">
                  Acepto los <Link to="/terms">Términos de servicio</Link> y{' '}
                  <Link to="/privacy">Política de privacidad</Link>.
                </span>
              </label>
            </div>

            <div className="form-group tutor-toggle-group">
              <span className="tutor-toggle-label">Quiero registrarme como tutor</span>
              <label className="toggle-switch">
                <input
                  type="checkbox"
                  name="esTutor"
                  checked={formData.esTutor}
                  onChange={handleInputChange}
                />
                <span className="toggle-slider"></span>
              </label>
            </div>

            {error && (
              <div className="register-error-message">
                {error}
              </div>
            )}

            <button type="submit" className="register-button" disabled={isLoading}>
              {isLoading ? 'Registrando...' : 'Registrar cuenta'}
              {!isLoading && (
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <line x1="5" y1="12" x2="19" y2="12"/>
                  <polyline points="12 5 19 12 12 19"/>
                </svg>
              )}
            </button>
          </form>

          <div className="login-link">
            <p>
              Ya tienes una cuenta? <Link to={loginLink}>Iniciar sesión</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Register;

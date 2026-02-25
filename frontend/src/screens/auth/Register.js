// filepath: c:\Users\juana\OneDrive\Escritorio\Juan Antonio\Universidad\cuarto año\ISPP\ISPP\frontend\src\screens\auth\Register.js
import { useState } from 'react';
import { Link } from 'react-router-dom';
import './Register.css';
import studyShareLogo from '../../static/images/studyShare_logo.png';

const ACADEMIC_INTERESTS = [
  'Ingeniería Software',
  'Diseño',
  'Física',
  'Historia',
  'Negocios',
  'Medicina',
];

const Register = () => {
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    password: '',
    interests: [],
    acceptTerms: false,
  });
  const [showPassword, setShowPassword] = useState(false);
  const [showMoreInterests, setShowMoreInterests] = useState(false);

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));
  };

  const toggleInterest = (interest) => {
    setFormData((prev) => ({
      ...prev,
      interests: prev.interests.includes(interest)
        ? prev.interests.filter((i) => i !== interest)
        : [...prev.interests, interest],
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    // TODO: Implement registration logic with API
    console.log('Form submitted:', formData);
    // navigate('/login');
  };

  const handleGoogleRegister = () => {
    // TODO: Implement Google OAuth
    console.log('Google register clicked');
  };

  const handleLinkedInRegister = () => {
    // TODO: Implement LinkedIn OAuth
    console.log('LinkedIn register clicked');
  };

  return (
    <div className="register-container">
      {/* Left Panel - Branding */}
      <div className="register-left-panel">
        <div className="register-logo">
          <img src={studyShareLogo} alt="MeerKatters Logo" className="register-logo-img" />
          <span className="logo-text">Meerkatters</span>
        </div>

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
                placeholder="Universidad de Sevilla"
                value={formData.fullName}
                onChange={handleInputChange}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="email">University Email</label>
              <input
                type="email"
                id="email"
                name="email"
                placeholder="us@universidadDeSevilla.mola"
                value={formData.email}
                onChange={handleInputChange}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="password">Password</label>
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
              <label>Intereses académicos</label>
              <div className="interests-container">
                {ACADEMIC_INTERESTS.map((interest) => (
                  <button
                    key={interest}
                    type="button"
                    className={`interest-chip ${
                      formData.interests.includes(interest) ? 'selected' : ''
                    }`}
                    onClick={() => toggleInterest(interest)}
                  >
                    {interest}
                  </button>
                ))}
                <button
                  type="button"
                  className="interest-chip more-chip"
                  onClick={() => setShowMoreInterests(!showMoreInterests)}
                >
                  + Más
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
                  required
                />
                <span className="checkbox-custom"></span>
                <span className="checkbox-text">
                  Acepto los <Link to="/terms">Términos de servicio</Link> y{' '}
                  <Link to="/privacy">Política de privacidad</Link>.
                </span>
              </label>
            </div>

            <button type="submit" className="register-button">
              Registrar cuenta
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <line x1="5" y1="12" x2="19" y2="12"/>
                <polyline points="12 5 19 12 12 19"/>
              </svg>
            </button>
          </form>

          <div className="login-link">
            <p>
              Ya tienes una cuenta? <Link to="/login">Iniciar sesión</Link>
            </p>
          </div>

          <div className="social-divider">
            <span>O REGÍSTRATE CON</span>
          </div>

          <div className="social-buttons">
            <button
              type="button"
              className="social-button google"
              onClick={handleGoogleRegister}
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24">
                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
              </svg>
              Google
            </button>
            <button
              type="button"
              className="social-button linkedin"
              onClick={handleLinkedInRegister}
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="#0A66C2">
                <path d="M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433c-1.144 0-2.063-.926-2.063-2.065 0-1.138.92-2.063 2.063-2.063 1.14 0 2.064.925 2.064 2.063 0 1.139-.925 2.065-2.064 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z"/>
              </svg>
              LinkedIn
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Register;

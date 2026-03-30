import { useState } from 'react';
import { Link } from 'react-router-dom';
import { apiClient } from '../../api/client';
import studyShareLogo from '../../static/images/MeerKatters_logo.png';
import './Login.css';

const ForgotPassword = () => {
  const [email, setEmail] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!email.trim()) {
      setError('El correo electrónico no puede estar vacío');
      return;
    }
    if (!emailRegex.test(email)) {
      setError('Introduce un correo electrónico válido');
      return;
    }

    setIsLoading(true);

    try {
      const response = await apiClient.post('/api/v1/auth/password/forgot', { email });
      setMessage(response.data?.message || response.message || 'Si el email existe, recibirás instrucciones de recuperación.');
      setSubmitted(true);
    } catch (err) {
      // Always show generic message to prevent email enumeration
      setMessage('Si el email existe en el sistema, recibirás instrucciones de recuperación en tu bandeja de entrada.');
      setSubmitted(true);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-left-panel">
        <div className="login-brand-content">
          <Link to="/" className="login-logo-wrapper">
            <img src={studyShareLogo} alt="MeerKatters Logo" className="login-logo-img" />
          </Link>
          <h1 className="login-brand-title">MeerKatters</h1>
          <p className="login-brand-description">
            Recupera el acceso a tu cuenta de forma segura.
          </p>
        </div>
      </div>

      <div className="login-right-panel">
        <div className="login-form-container">
          <div className="login-header">
            <h2>Recuperar contraseña</h2>
            <p>
              {submitted
                ? 'Revisa tu bandeja de entrada'
                : 'Introduce tu correo electrónico y te enviaremos un enlace para restablecer tu contraseña'}
            </p>
          </div>

          {submitted ? (
            <div>
              {message && (
                <div style={{
                  padding: '15px',
                  borderRadius: '8px',
                  marginBottom: '20px',
                  backgroundColor: '#d1fae5',
                  color: '#065f46',
                  fontSize: '14px',
                  lineHeight: '1.5'
                }}>
                  {message}
                </div>
              )}
              <p style={{ fontSize: '14px', color: '#666', marginBottom: '20px' }}>
                El enlace expirará en 15 minutos. Si no recibes el email, revisa tu carpeta de spam.
              </p>
              <Link to="/login" className="login-button" style={{ display: 'block', textAlign: 'center', textDecoration: 'none' }}>
                Volver al inicio de sesión
              </Link>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="login-form">
              <div className="form-group">
                <label htmlFor="email">Correo electrónico</label>
                <input
                  type="email"
                  id="email"
                  name="email"
                  placeholder="tu@correo.com"
                  value={email}
                  onChange={(e) => { setEmail(e.target.value); if (error) setError(''); }}
                  required
                />
              </div>

              {error && (
                <div className="login-error-message">{error}</div>
              )}

              <button type="submit" className="login-button" disabled={isLoading}>
                {isLoading ? 'Enviando...' : 'Enviar enlace de recuperación'}
              </button>

              <div style={{ textAlign: 'center', marginTop: '20px' }}>
                <Link to="/login" style={{ color: '#3b82f6', fontSize: '14px' }}>
                  ← Volver al inicio de sesión
                </Link>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
};

export default ForgotPassword;

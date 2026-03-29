import { useState, useMemo } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { apiClient } from '../../api/client';
import studyShareLogo from '../../static/images/MeerKatters_logo.png';
import './Login.css';

const MIN_PASSWORD_LENGTH = 8;
const MAX_PASSWORD_LENGTH = 128;

const ResetPassword = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');

  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const passwordRequirements = useMemo(() => ({
    minLength: newPassword.length >= MIN_PASSWORD_LENGTH,
    maxLength: newPassword.length <= MAX_PASSWORD_LENGTH,
    hasUppercase: /[A-Z]/.test(newPassword),
    hasLowercase: /[a-z]/.test(newPassword),
    hasNumber: /[0-9]/.test(newPassword),
  }), [newPassword]);

  const isPasswordValid = passwordRequirements.minLength
    && passwordRequirements.maxLength
    && passwordRequirements.hasUppercase
    && passwordRequirements.hasLowercase
    && passwordRequirements.hasNumber;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!isPasswordValid) {
      setError('La contraseña no cumple todos los requisitos');
      return;
    }

    if (newPassword !== confirmPassword) {
      setError('Las contraseñas no coinciden');
      return;
    }

    setIsLoading(true);

    try {
      await apiClient.post('/api/v1/auth/password/reset', {
        token,
        newPassword
      });
      setSuccess(true);
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Error al restablecer la contraseña';
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  if (!token) {
    return (
      <div className="login-container">
        <div className="login-left-panel">
          <div className="login-brand-content">
            <Link to="/" className="login-logo-wrapper">
              <img src={studyShareLogo} alt="MeerKatters Logo" className="login-logo-img" />
            </Link>
            <h1 className="login-brand-title">MeerKatters</h1>
          </div>
        </div>
        <div className="login-right-panel">
          <div className="login-form-container">
            <div className="login-header">
              <h2>Enlace inválido</h2>
              <p>El enlace de recuperación no es válido. Solicita uno nuevo.</p>
            </div>
            <Link to="/forgot-password" className="login-button" style={{ display: 'block', textAlign: 'center', textDecoration: 'none' }}>
              Solicitar nuevo enlace
            </Link>
          </div>
        </div>
      </div>
    );
  }

  if (success) {
    return (
      <div className="login-container">
        <div className="login-left-panel">
          <div className="login-brand-content">
            <Link to="/" className="login-logo-wrapper">
              <img src={studyShareLogo} alt="MeerKatters Logo" className="login-logo-img" />
            </Link>
            <h1 className="login-brand-title">MeerKatters</h1>
          </div>
        </div>
        <div className="login-right-panel">
          <div className="login-form-container">
            <div className="login-header">
              <h2>¡Contraseña restablecida!</h2>
              <p>Tu contraseña se ha actualizado correctamente.</p>
            </div>
            <Link to="/login" className="login-button" style={{ display: 'block', textAlign: 'center', textDecoration: 'none' }}>
              Iniciar sesión
            </Link>
          </div>
        </div>
      </div>
    );
  }

  const RequirementItem = ({ met, text }) => (
    <li style={{ color: met ? '#065f46' : '#991b1b', fontSize: '13px', marginBottom: '4px' }}>
      {met ? '✓' : '✗'} {text}
    </li>
  );

  return (
    <div className="login-container">
      <div className="login-left-panel">
        <div className="login-brand-content">
          <Link to="/" className="login-logo-wrapper">
            <img src={studyShareLogo} alt="MeerKatters Logo" className="login-logo-img" />
          </Link>
          <h1 className="login-brand-title">MeerKatters</h1>
          <p className="login-brand-description">
            Establece una nueva contraseña segura para tu cuenta.
          </p>
        </div>
      </div>

      <div className="login-right-panel">
        <div className="login-form-container">
          <div className="login-header">
            <h2>Nueva contraseña</h2>
            <p>Introduce tu nueva contraseña</p>
          </div>

          <form onSubmit={handleSubmit} className="login-form">
            <div className="form-group">
              <label htmlFor="newPassword">Nueva contraseña</label>
              <div className="password-input-container">
                <input
                  type={showPassword ? 'text' : 'password'}
                  id="newPassword"
                  placeholder="••••••••"
                  value={newPassword}
                  onChange={(e) => { setNewPassword(e.target.value); if (error) setError(''); }}
                  maxLength={MAX_PASSWORD_LENGTH}
                  required
                />
                <button
                  type="button"
                  className="password-toggle"
                  onClick={() => setShowPassword(!showPassword)}
                  aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                >
                  {showPassword ? '🙈' : '👁️'}
                </button>
              </div>
            </div>

            {newPassword && (
              <ul style={{ listStyle: 'none', padding: '0 0 10px 0', margin: 0 }}>
                <RequirementItem met={passwordRequirements.minLength} text="Mínimo 8 caracteres" />
                <RequirementItem met={passwordRequirements.hasUppercase} text="Al menos una mayúscula" />
                <RequirementItem met={passwordRequirements.hasLowercase} text="Al menos una minúscula" />
                <RequirementItem met={passwordRequirements.hasNumber} text="Al menos un número" />
              </ul>
            )}

            <div className="form-group">
              <label htmlFor="confirmPassword">Confirmar contraseña</label>
              <input
                type={showPassword ? 'text' : 'password'}
                id="confirmPassword"
                placeholder="••••••••"
                value={confirmPassword}
                onChange={(e) => { setConfirmPassword(e.target.value); if (error) setError(''); }}
                maxLength={MAX_PASSWORD_LENGTH}
                required
              />
            </div>

            {error && (
              <div className="login-error-message">{error}</div>
            )}

            <button
              type="submit"
              className="login-button"
              disabled={isLoading || !isPasswordValid || newPassword !== confirmPassword}
            >
              {isLoading ? 'Restableciendo...' : 'Restablecer contraseña'}
            </button>

            <div style={{ textAlign: 'center', marginTop: '20px' }}>
              <Link to="/login" style={{ color: '#3b82f6', fontSize: '14px' }}>
                ← Volver al inicio de sesión
              </Link>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default ResetPassword;

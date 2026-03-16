import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import studyShareLogo from '../../static/images/MeerKatters_logo.png';
import { getPendingInvitationPath } from '../../utils/invitationFlow';
import './Register.css';

const VerifyEmail = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { verifyEmail, resendVerification, isAuthenticated } = useAuth();
  
  const [status, setStatus] = useState('verifying'); // verifying, success, error, no-token
  const [errorMessage, setErrorMessage] = useState('');
  const [resendEmail, setResendEmail] = useState('');
  const [resendLoading, setResendLoading] = useState(false);
  const [resendMessage, setResendMessage] = useState('');

  const token = searchParams.get('token');

  useEffect(() => {
    const verify = async () => {
      if (!token) {
        setStatus('no-token');
        return;
      }

      const result = await verifyEmail(token);
      
      if (result.success) {
        setStatus('success');
        const pendingInvitationPath = getPendingInvitationPath();
        // Redirigir al home después de 3 segundos
        setTimeout(() => {
          navigate(pendingInvitationPath || '/');
        }, 3000);
      } else {
        setStatus('error');
        setErrorMessage(result.error || 'Error al verificar el email');
      }
    };

    if (!isAuthenticated) {
      verify();
    } else {
      // Si ya está autenticado, redirigir al home
      navigate('/');
    }
  }, [token, verifyEmail, navigate, isAuthenticated]);

  const handleResendVerification = async () => {
    if (!resendEmail) {
      setResendMessage('Por favor, introduce tu email');
      return;
    }

    setResendLoading(true);
    setResendMessage('');
    
    const result = await resendVerification(resendEmail);
    
    if (result.success) {
      setResendMessage('Email de verificación reenviado correctamente');
    } else {
      setResendMessage(result.error || 'Error al reenviar el email');
    }
    
    setResendLoading(false);
  };

  const renderContent = () => {
    switch (status) {
      case 'verifying':
        return (
          <>
            <div className="register-already-logged__icon" style={{ fontSize: '48px' }}>⏳</div>
            <h1 className="register-already-logged__title">Verificando tu cuenta...</h1>
            <p className="register-already-logged__text">
              Por favor, espera mientras verificamos tu dirección de email.
            </p>
            <div className="loading-spinner" style={{
              width: '40px',
              height: '40px',
              border: '4px solid #f3f3f3',
              borderTop: '4px solid #3498db',
              borderRadius: '50%',
              animation: 'spin 1s linear infinite',
              margin: '20px auto'
            }} />
          </>
        );

      case 'success':
        return (
          <>
            <div className="register-already-logged__icon" style={{ color: '#10b981' }}>✓</div>
            <h1 className="register-already-logged__title">¡Email verificado!</h1>
            <p className="register-already-logged__text">
              Tu cuenta ha sido activada correctamente. Serás redirigido al inicio en unos segundos...
            </p>
            <div className="register-already-logged__buttons" style={{ marginTop: '20px' }}>
              <Link to="/" className="btn-home">Ir al inicio ahora</Link>
            </div>
          </>
        );

      case 'error':
        return (
          <>
            <div className="register-already-logged__icon" style={{ fontSize: '48px', color: '#ef4444' }}>✗</div>
            <h1 className="register-already-logged__title">Error de verificación</h1>
            <p className="register-already-logged__text">
              {errorMessage}
            </p>
            <p className="register-already-logged__text" style={{ fontSize: '14px', color: '#666', marginTop: '10px' }}>
              El enlace puede haber expirado o ya fue utilizado. Puedes solicitar un nuevo email de verificación.
            </p>
            
            <div style={{ marginTop: '20px', maxWidth: '300px', margin: '20px auto' }}>
              <input
                type="email"
                placeholder="Tu email"
                value={resendEmail}
                onChange={(e) => setResendEmail(e.target.value)}
                style={{
                  width: '100%',
                  padding: '12px 16px',
                  borderRadius: '8px',
                  border: '1px solid #e5e7eb',
                  fontSize: '14px',
                  marginBottom: '10px'
                }}
              />
              {resendMessage && (
                <p style={{ 
                  marginBottom: '10px',
                  padding: '10px', 
                  borderRadius: '8px',
                  backgroundColor: resendMessage.includes('Error') || resendMessage.includes('introduce') ? '#fee2e2' : '#d1fae5',
                  color: resendMessage.includes('Error') || resendMessage.includes('introduce') ? '#991b1b' : '#065f46',
                  fontSize: '14px'
                }}>
                  {resendMessage}
                </p>
              )}
              <button 
                onClick={handleResendVerification} 
                disabled={resendLoading}
                style={{
                  width: '100%',
                  padding: '12px 16px',
                  borderRadius: '8px',
                  border: 'none',
                  backgroundColor: '#3b82f6',
                  color: 'white',
                  fontSize: '14px',
                  fontWeight: '500',
                  cursor: resendLoading ? 'not-allowed' : 'pointer',
                  opacity: resendLoading ? 0.7 : 1
                }}
              >
                {resendLoading ? 'Enviando...' : 'Reenviar email de verificación'}
              </button>
            </div>

            <div className="register-already-logged__buttons" style={{ marginTop: '20px' }}>
              <Link to="/login" className="btn-home">Ir a iniciar sesión</Link>
              <Link to="/register" className="btn-profile">Crear nueva cuenta</Link>
            </div>
          </>
        );

      case 'no-token':
        return (
          <>
            <div className="register-already-logged__icon" style={{ fontSize: '48px', color: '#f59e0b' }}>⚠️</div>
            <h1 className="register-already-logged__title">Token no encontrado</h1>
            <p className="register-already-logged__text">
              No se ha proporcionado un token de verificación válido. 
              Por favor, utiliza el enlace completo que recibiste en tu email.
            </p>
            <div className="register-already-logged__buttons" style={{ marginTop: '20px' }}>
              <Link to="/login" className="btn-home">Ir a iniciar sesión</Link>
              <Link to="/register" className="btn-profile">Crear nueva cuenta</Link>
            </div>
          </>
        );

      default:
        return null;
    }
  };

  return (
    <div className="register-container">
      <style>
        {`
          @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
          }
        `}
      </style>
      
      {/* Left Panel - Branding */}
      <div className="register-left-panel">
        <Link to="/" className="register-logo">
          <img src={studyShareLogo} alt="MeerKatters Logo" className="register-logo-img" />
          <span className="logo-text">Meerkatters</span>
        </Link>

        <div className="register-hero">
          <h1>Verificación de cuenta</h1>
          <p>
            Estamos verificando tu dirección de correo electrónico para activar tu cuenta.
          </p>
        </div>

        <div className="register-footer">
          <p>© 2024 StudYshare. All rights reserved.</p>
        </div>
      </div>

      {/* Right Panel - Status */}
      <div className="register-right-panel">
        <div className="register-already-logged">
          {renderContent()}
        </div>
      </div>
    </div>
  );
};

export default VerifyEmail;

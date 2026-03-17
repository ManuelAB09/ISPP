import React from 'react';
import { useNavigate } from 'react-router-dom';
import './PlanExpiryBanner.css';

export default function PlanExpiryBanner({ planName, fechaFin, onDismiss }) {
  const navigate = useNavigate();

  return (
    <div className="plan-expiry-banner">
      <span className="plan-expiry-banner__text">
        Tu plan <strong>{planName}</strong> expira el <strong>{fechaFin}</strong>.
        Renuévalo para no perder tus beneficios.
      </span>
      <button
        className="plan-expiry-banner__btn"
        onClick={() => navigate('/planes')}
      >
        Renovar plan
      </button>
      <button
        className="plan-expiry-banner__close"
        onClick={onDismiss}
        aria-label="Cerrar aviso"
      >
        ✕
      </button>
    </div>
  );
}

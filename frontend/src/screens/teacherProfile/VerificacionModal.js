import React, { useState } from "react";
import { requestVerification } from "../../api/tutorEndpoints";
import "./TutorModals.css";

const VerificacionModal = ({ tutorId, verificado, onClose, onVerificado }) => {
  const [solicitando, setSolicitando] = useState(false);
  const [errorMsg, setErrorMsg] = useState(null);

  const handleIniciarPago = async () => {
    setSolicitando(true);
    setErrorMsg(null);
    try {
      const data = await requestVerification();
      // Redirigir a Stripe Checkout
      window.location.href = data.paymentUrl;
    } catch (err) {
      console.error("Error al iniciar verificación:", err);
      setErrorMsg("No se pudo conectar con la pasarela de pago. Inténtalo de nuevo.");
      setSolicitando(false);
    }
  };

  return (
    <div className="tm-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="tm-modal" role="dialog" aria-modal="true" aria-label="Verificación de perfil">
        <div className="tm-modal__header">
          <h2 className="tm-modal__title">Promocionarse</h2>
          <button className="tm-modal__close" onClick={onClose} aria-label="Cerrar">✕</button>
        </div>

        <div className="tm-modal__body">
          {verificado ? (
            <div className="tm-verificacion">
              <div className="tm-verificacion__icon tm-verificacion__icon--ok">✓</div>
              <h3 className="tm-verificacion__heading">Perfil verificado</h3>
              <p className="tm-verificacion__text">
                Tu perfil cuenta con la insignia <strong>Verificado</strong> y aparece
                destacado en el listado de profesores.
              </p>
            </div>
          ) : (
            <div className="tm-verificacion">
              <div className="tm-verificacion__icon tm-verificacion__icon--info">🏅</div>
              <h3 className="tm-verificacion__heading">Destaca tu perfil como Verificado</h3>
              <ul className="tm-verificacion__beneficios">
                <li>✓ Insignia <strong>Verificado</strong> visible en tu perfil y en el listado</li>
                <li>✓ Acceso prioritario al contacto directo con alumnos</li>
                <li>✓ Mayor confianza y visibilidad frente al resto de profesores</li>
              </ul>
              <div className="tm-verificacion__precio">
                <span className="tm-verificacion__precio-num">19,99 €</span>
                <span className="tm-verificacion__precio-desc">pago único · sin suscripción</span>
              </div>
              {errorMsg && <p className="tm-error">{errorMsg}</p>}
              <p className="tm-verificacion__nota">
                Serás redirigido a Stripe para completar el pago de forma segura.
                Una vez confirmado, tu perfil quedará verificado automáticamente.
              </p>
              <button
                className="tm-btn tm-btn--primary tm-btn--full"
                onClick={handleIniciarPago}
                disabled={solicitando}
              >
                {solicitando ? (
                  <>
                    <span className="tm-spinner" /> Redirigiendo a Stripe…
                  </>
                ) : (
                  "Pagar 19,99€ y verificarme"
                )}
              </button>
            </div>
          )}
        </div>

        <div className="tm-modal__footer tm-modal__footer--right">
          <button className="tm-btn tm-btn--secondary" onClick={onClose}>
            Cerrar
          </button>
        </div>
      </div>
    </div>
  );
};

export default VerificacionModal;
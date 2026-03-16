import { useState } from 'react';
import { verificarTutor } from '../../api/tutorEndpoints';
import './TutorModals.css';

const VerificacionModal = ({ verificado, onClose, onVerificado }) => {
  const [solicitando, setSolicitando] = useState(false);
  const [errorMsg, setErrorMsg] = useState(null);
  const [mostrarPago, setMostrarPago] = useState(false);
  const [pagoRealizado, setPagoRealizado] = useState(false);
  const [pagoError, setPagoError] = useState(null);
  const [pagoForm, setPagoForm] = useState({
    nombre: '',
    numero: '',
    caducidad: '',
    cvc: '',
  });

  const handlePagoSubmit = async (e) => {
    e.preventDefault();
    setPagoError(null);
    setErrorMsg(null);
    setSolicitando(true);

    try {
      await verificarTutor();
      setPagoRealizado(true);
      if (typeof onVerificado === 'function') {
        onVerificado();
      }
    } catch (err) {
      console.error('Error al verificar tutor:', err);
      setPagoError('No se pudo completar la verificación. Inténtalo de nuevo.');
    } finally {
      setSolicitando(false);
    }
  };

  const renderContenido = () => {
    if (mostrarPago && !pagoRealizado) {
      return (
        <form className="tm-pago-form" onSubmit={handlePagoSubmit} autoComplete="off">
          <div className="tm-pago-form__titulo">Pago de verificación</div>

          <div className="tm-pago-form__row">
            <label>Nombre en la tarjeta</label>
            <input
              type="text"
              value={pagoForm.nombre}
              onChange={(e) => setPagoForm((f) => ({ ...f, nombre: e.target.value }))}
              placeholder="Nombre completo"
              autoFocus
            />
          </div>

          <div className="tm-pago-form__row">
            <label>Número de tarjeta</label>
            <input
              type="text"
              value={pagoForm.numero}
              onChange={(e) => {
                const digits = e.target.value.replace(/\D/g, '').substring(0, 16);
                const formatted = digits.replace(/(\d{4})(?=\d)/g, '$1 ');
                setPagoForm((f) => ({ ...f, numero: formatted }));
              }}
              placeholder="1234 5678 9012 3456"
              maxLength={19}
              inputMode="numeric"
            />
          </div>

          <div className="tm-pago-form__row tm-pago-form__row--split">
            <div>
              <label>Caducidad</label>
              <input
                type="text"
                value={pagoForm.caducidad}
                onChange={(e) => {
                  const digits = e.target.value.replace(/\D/g, '').substring(0, 4);
                  const formatted =
                    digits.length >= 3
                      ? `${digits.substring(0, 2)}/${digits.substring(2)}`
                      : digits;
                  setPagoForm((f) => ({ ...f, caducidad: formatted }));
                }}
                placeholder="MM/AA"
                maxLength={5}
                inputMode="numeric"
              />
            </div>
            <div>
              <label>CVC</label>
              <input
                type="text"
                value={pagoForm.cvc}
                onChange={(e) => {
                  const digits = e.target.value.replace(/\D/g, '').substring(0, 3);
                  setPagoForm((f) => ({ ...f, cvc: digits }));
                }}
                placeholder="123"
                maxLength={3}
                inputMode="numeric"
              />
            </div>
          </div>

          {pagoError && <div className="tm-error tm-pago-form__error">{pagoError}</div>}

          <button className="tm-btn tm-btn--primary tm-btn--full" type="submit" disabled={solicitando}>
            {solicitando ? 'Procesando pago...' : 'Pagar y solicitar verificación'}
          </button>

          <button
            type="button"
            className="tm-btn tm-btn--secondary tm-btn--full"
            style={{ marginTop: 8 }}
            onClick={() => setMostrarPago(false)}
            disabled={solicitando}
          >
            Cancelar
          </button>
        </form>
      );
    }

    if (mostrarPago && pagoRealizado) {
      return (
        <div className="tm-verificacion tm-verificacion--pago-ok">
          <div className="tm-verificacion__icon tm-verificacion__icon--ok">✓</div>
          <h3 className="tm-verificacion__heading">Pago realizado</h3>
        </div>
      );
    }

    if (verificado) {
      return (
        <div className="tm-verificacion">
          <div className="tm-verificacion__icon tm-verificacion__icon--ok">✓</div>
          <h3 className="tm-verificacion__heading">Perfil verificado</h3>
          <p className="tm-verificacion__text">
            Tu perfil cuenta con la insignia <strong>Verificado</strong> y aparece destacado en
            el listado de profesores.
          </p>
        </div>
      );
    }

    return (
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
          Para solicitar la verificación debes realizar el pago mediante tarjeta bancaria.
        </p>
        <button
          className="tm-btn tm-btn--primary tm-btn--full"
          onClick={() => setMostrarPago(true)}
          disabled={solicitando}
        >
          Iniciar pago y solicitud
        </button>
      </div>
    );
  };

  return (
    <div className="tm-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="tm-modal" role="dialog" aria-modal="true" aria-label="Verificación de perfil">
        <div className="tm-modal__header">
          <h2 className="tm-modal__title">Promocionarse</h2>
          <button className="tm-modal__close" onClick={onClose} aria-label="Cerrar">
            ✕
          </button>
        </div>

        <div className="tm-modal__body">{renderContenido()}</div>

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

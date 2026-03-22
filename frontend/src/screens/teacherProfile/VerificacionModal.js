import { Elements, PaymentElement, useElements, useStripe } from "@stripe/react-stripe-js";
import { loadStripe } from "@stripe/stripe-js";
import { useEffect, useState } from "react";
import { confirmVerificationPayment, createVerificationPaymentIntent } from "../../api/tutorEndpoints";
import "./TutorModals.css";

const stripePromise = loadStripe(process.env.REACT_APP_STRIPE_PUBLIC_KEY);

/**
 * Formulario de pago real con Stripe Elements para verificación de tutor.
 */
const VerificacionPaymentForm = ({ onPaymentSuccess, onCancel }) => {
  const stripe = useStripe();
  const elements = useElements();
  const [processing, setProcessing] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!stripe || !elements) return;

    setProcessing(true);
    setError(null);

    try {
      const { error: submitError } = await elements.submit();
      if (submitError) {
        setError(submitError.message);
        setProcessing(false);
        return;
      }

      const { error: confirmError, paymentIntent } = await stripe.confirmPayment({
        elements,
        confirmParams: {
          return_url: window.location.origin + "/success?tipo=verificacion",
        },
        redirect: "if_required",
      });

      if (confirmError) {
        setError(confirmError.message);
        setProcessing(false);
        return;
      }

      if (paymentIntent && paymentIntent.status === "succeeded") {
        await confirmVerificationPayment(paymentIntent.id);
        onPaymentSuccess();
      }
    } catch (err) {
      setError(
        err?.response?.data?.error ||
          err?.data?.error ||
          "Error al procesar el pago. Inténtalo de nuevo."
      );
    } finally {
      setProcessing(false);
    }
  };

  return (
    <form className="tm-pago-form" onSubmit={handleSubmit}>
      <div className="tm-pago-form__titulo">Pago de verificación</div>
      <PaymentElement />
      {error && <div className="tm-error tm-pago-form__error" style={{ marginTop: 12 }}>{error}</div>}
      <button
        className="tm-btn tm-btn--primary tm-btn--full"
        type="submit"
        disabled={processing || !stripe}
        style={{ marginTop: 16 }}
      >
        {processing ? "Procesando..." : "Pagar y solicitar verificación"}
      </button>
      <button
        type="button"
        className="tm-btn tm-btn--secondary tm-btn--full"
        style={{ marginTop: 8 }}
        onClick={onCancel}
        disabled={processing}
      >
        Cancelar
      </button>
    </form>
  );
};

/**
 * Modal de verificación del perfil de tutor.
 * Muestra el estado actual y permite solicitar la verificación (€19,99)
 * con pago real a través de Stripe Elements embebido.
 *
 * Props:
 *   - tutorId: Long
 *   - verificado: Boolean (valor ya conocido del perfil)
 *   - onClose: callback
 *   - onVerificado: callback() → actualiza el tutor en el padre tras solicitar
 */
const VerificacionModal = ({ tutorId, verificado, onClose, onVerificado }) => {
  const [estado, setEstado] = useState(verificado ? "VERIFICADO" : null);
  const [cargando, setCargando] = useState(!verificado);
  const [errorMsg, setErrorMsg] = useState(null);
  const [exito, setExito] = useState(false);
  // Stripe Elements state
  const [mostrarPago, setMostrarPago] = useState(false);
  const [clientSecret, setClientSecret] = useState(null);
  const [loadingIntent, setLoadingIntent] = useState(false);

  // Cargar estado inicial
  useEffect(() => {
    if (verificado) {
      setEstado("VERIFICADO");
      setCargando(false);
    } else {
      setEstado("SIN_SOLICITUD");
      setCargando(false);
    }
  }, [tutorId, verificado]);

  // Crear PaymentIntent cuando el usuario quiere pagar
  const handleIniciarPago = async () => {
    setLoadingIntent(true);
    setErrorMsg(null);

    try {
      const res = await createVerificationPaymentIntent();
      const data = res.data || res;
      setClientSecret(data.clientSecret);
      setMostrarPago(true);
    } catch (err) {
      setErrorMsg(
        err?.response?.data?.error || "Error al conectar con la pasarela de pago. Inténtalo de nuevo."
      );
    } finally {
      setLoadingIntent(false);
    }
  };
  
  const handlePaymentSuccess = () => {
    setMostrarPago(false);
    setEstado("VERIFICADO");
    setExito(true);
    onVerificado && onVerificado();
  };

  const elementsOptions = clientSecret
    ? { clientSecret, appearance: { theme: "stripe" } }
    : undefined;

  /* ─── Contenido según estado ─── */
  const renderContenido = () => {
    // Stripe Elements payment form
    if (mostrarPago && clientSecret && elementsOptions) {
      return (
        <Elements stripe={stripePromise} options={elementsOptions}>
          <VerificacionPaymentForm
            onPaymentSuccess={handlePaymentSuccess}
            onCancel={() => setMostrarPago(false)}
          />
        </Elements>
      );
    }

    if (loadingIntent) {
      return <p className="tm-status tm-status--loading">Cargando pasarela de pago...</p>;
    }

    if (cargando) return <p className="tm-status tm-status--loading">Comprobando estado…</p>;

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
          onClick={handleIniciarPago}
          disabled={loadingIntent}
        >
          {loadingIntent ? "Cargando..." : "Iniciar pago y solicitud"}
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

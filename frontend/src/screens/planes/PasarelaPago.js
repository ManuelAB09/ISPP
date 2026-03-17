import { useState, useMemo, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { loadStripe } from "@stripe/stripe-js";
import { Elements, PaymentElement, useStripe, useElements } from "@stripe/react-stripe-js";
import { subscriptionsApi } from "../../api/subscriptions.api";
import Header from "../../components/Header/Header";
import "./PasarelaPago.css";

const stripePromise = loadStripe(process.env.REACT_APP_STRIPE_PUBLIC_KEY);

/**
 * Formulario de pago real con Stripe Elements.
 */
function CheckoutForm({ selectedPeriod, prices }) {
  const navigate = useNavigate();
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
          return_url: window.location.origin + "/planes/success?embedded=true",
        },
        redirect: "if_required",
      });

      if (confirmError) {
        setError(confirmError.message);
        setProcessing(false);
        return;
      }

      if (paymentIntent && paymentIntent.status === "succeeded") {
        await subscriptionsApi.confirmEmbeddedPayment(paymentIntent.id);
        navigate("/planes/success?embedded=true");
      }
    } catch (err) {
      console.error("Error al procesar el pago:", err);
      setError(
        err?.response?.data?.error ||
          err?.data?.error ||
          "Error al procesar el pago. Intenta de nuevo."
      );
    } finally {
      setProcessing(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="pasarela-form">
      <PaymentElement />

      {error && (
        <div
          className="pasarela-error"
          style={{ padding: "12px", background: "#fef2f2", borderRadius: 8, marginTop: 16 }}
        >
          {error}
        </div>
      )}

      <div className="pasarela-actions">
        <button
          type="button"
          onClick={() => navigate("/planes")}
          className="pasarela-btn pasarela-btn--secondary"
          disabled={processing}
        >
          Cancelar
        </button>
        <button
          type="submit"
          className="pasarela-btn pasarela-btn--primary"
          disabled={processing || !stripe}
        >
          {processing ? (
            <>
              <span className="pasarela-spinner" /> 
            </>
          ) : (
            `Pagar ${(parseFloat(prices[selectedPeriod]) * 1.21).toFixed(2)}€`
          )}
        </button>
      </div>

      <div className="pasarela-security-info">
        <span className="pasarela-security-icon">🔒</span>
        <p>
          Tu informacion esta protegida con encriptacion SSL de 256 bits.
          El pago se procesa de forma segura a traves de Stripe.
        </p>
      </div>
    </form>
  );
}

/**
 * Componente principal de la pasarela de pago de suscripcion PREMIUM
 * con Stripe Elements incrustado.
 */
export default function PasarelaPago() {
  const [selectedPeriod, setSelectedPeriod] = useState("mensual");
  const [clientSecret, setClientSecret] = useState(null);
  const [loadingIntent, setLoadingIntent] = useState(false);
  const [intentError, setIntentError] = useState(null);

  const prices = useMemo(
    () => ({
      mensual: "2.99",
      anual: "25.99",
    }),
    []
  );

  // Crear PaymentIntent al montar y al cambiar de periodo
  useEffect(() => {
    let cancelled = false;
    const fetchIntent = async () => {
      setLoadingIntent(true);
      setIntentError(null);
      setClientSecret(null);
      try {
        const res = await subscriptionsApi.createPaymentIntent({
          planId: "PREMIUM",
          aceptarTerminos: true,
          periodo: selectedPeriod,
        });
        const data = res.data || res;
        if (!cancelled) setClientSecret(data.clientSecret);
      } catch (err) {
        if (!cancelled)
          setIntentError(
            err?.response?.data?.error || "Error al iniciar el pago. Intenta de nuevo."
          );
      } finally {
        if (!cancelled) setLoadingIntent(false);
      }
    };
    fetchIntent();
    return () => { cancelled = true; };
  }, [selectedPeriod]);

  const elementsOptions = clientSecret
    ? { clientSecret, appearance: { theme: "stripe" } }
    : undefined;

  return (
    <>
      <Header page={"planes"} />
      <div className="pasarela-page">
        <div className="pasarela-container">
          {/* Columna izquierda - Resumen del pedido */}
          <div className="pasarela-summary">
            <h2 className="pasarela-summary-title">Resumen del pedido</h2>

            <div className="pasarela-plan-card">
              <div className="pasarela-plan-badge">PREMIUM</div>
              <h3 className="pasarela-plan-name">Plan Premium</h3>
              <p className="pasarela-plan-description">
                Desbloquea todas las funcionalidades avanzadas de MeerKatters
              </p>

              <ul className="pasarela-features-list">
                <li>Mas limites y herramientas</li>
                <li>Mejor experiencia de uso</li>
                <li>Acceso a funcionalidades avanzadas</li>
                <li>Soporte prioritario</li>
                <li>Sin publicidad</li>
              </ul>
            </div>

            {/* Selector de periodo */}
            <div className="pasarela-period-selector">
              <h4 className="pasarela-period-title">Selecciona tu periodo</h4>
              <div className="pasarela-period-options">
                <button
                  type="button"
                  className={`pasarela-period-btn ${
                    selectedPeriod === "mensual"
                      ? "pasarela-period-btn--active"
                      : ""
                  }`}
                  onClick={() => setSelectedPeriod("mensual")}
                >
                  <div className="pasarela-period-label">Mensual</div>
                  <div className="pasarela-period-price">2.99€/mes</div>
                </button>

                <button
                  type="button"
                  className={`pasarela-period-btn ${
                    selectedPeriod === "anual"
                      ? "pasarela-period-btn--active"
                      : ""
                  }`}
                  onClick={() => setSelectedPeriod("anual")}
                >
                  <div className="pasarela-period-label">Anual</div>
                  <div className="pasarela-period-price">25.99€/año</div>
                  <div className="pasarela-period-save">Ahorra 28%</div>
                </button>
              </div>
            </div>

            {/* Total */}
            <div className="pasarela-total">
              <div className="pasarela-total-row">
                <span>Subtotal</span>
                <span>{prices[selectedPeriod]}€</span>
              </div>
              <div className="pasarela-total-row">
                <span>IVA (21%)</span>
                <span>
                  {(parseFloat(prices[selectedPeriod]) * 0.21).toFixed(2)}€
                </span>
              </div>
              <div className="pasarela-total-divider"></div>
              <div className="pasarela-total-row pasarela-total-final">
                <span>Total</span>
                <span>
                  {(parseFloat(prices[selectedPeriod]) * 1.21).toFixed(2)}€
                </span>
              </div>
            </div>
          </div>

          {/* Columna derecha - Formulario de pago con Stripe Elements */}
          <div className="pasarela-form-section">
            <div className="pasarela-header">
              <h1 className="pasarela-title">Informacion de pago</h1>
              <p className="pasarela-subtitle">
                Introduce los datos de tu tarjeta de forma segura. El pago se
                procesa directamente a traves de Stripe.
              </p>
            </div>

            {loadingIntent && (
              <p style={{ textAlign: "center", padding: 20 }}>Cargando pasarela de pago...</p>
            )}
            {intentError && (
              <div className="pasarela-error" style={{ padding: 12, background: "#fef2f2", borderRadius: 8 }}>
                {intentError}
              </div>
            )}
            {clientSecret && elementsOptions && (
              <Elements stripe={stripePromise} options={elementsOptions}>
                <CheckoutForm selectedPeriod={selectedPeriod} prices={prices} />
              </Elements>
            )}
          </div>
        </div>
      </div>
    </>
  );
}

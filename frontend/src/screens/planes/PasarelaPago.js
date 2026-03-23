import { Elements, PaymentElement, useElements, useStripe } from "@stripe/react-stripe-js";
import { loadStripe } from "@stripe/stripe-js";
import { useEffect, useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { subscriptionsApi } from "../../api/subscriptions.api";
import Header from "../../components/Header/Header";
import "./PasarelaPago.css";

const stripePromise = loadStripe(process.env.REACT_APP_STRIPE_PUBLIC_KEY);

const PLAN_CONFIG = {
  PREMIUM: {
    id: "PREMIUM",
    badge: "PREMIUM",
    name: "Plan Premium",
    description: "Más capacidad para crecer en MeerKatters",
    prices: {
      mensual: 4.99,
      anual: 50,
    },
    features: [
      "10 comunidades activas",
      "75 aforo máx",
      "5 profesores por comunidad",
    ],
  },
  PRO: {
    id: "PRO",
    badge: "PRO",
    name: "Plan Pro",
    description: "Capacidad avanzada para equipos con mayor actividad",
    prices: {
      mensual: 19.99,
      anual: 200,
    },
    features: [
      "25 comunidades activas",
      "250 aforo máx",
      "15 profesores por comunidad",
    ],
  },
};

const VALID_PLANS = Object.keys(PLAN_CONFIG);

/**
 * Formulario de pago real con Stripe Elements.
 */
function CheckoutForm({ selectedPlan, selectedPeriod, total }) {
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
        navigate(`/planes/success?embedded=true&plan=${selectedPlan}&periodo=${selectedPeriod}`);
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
            `Pagar ${total.toFixed(2)}€`
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
  const [searchParams, setSearchParams] = useSearchParams();
  const planFromUrl = (searchParams.get("plan") || "PREMIUM").toUpperCase();
  const initialPlan = VALID_PLANS.includes(planFromUrl) ? planFromUrl : "PREMIUM";

  const [selectedPlan, setSelectedPlan] = useState(initialPlan);
  const [selectedPeriod, setSelectedPeriod] = useState("mensual");
  const [clientSecret, setClientSecret] = useState(null);
  const [loadingIntent, setLoadingIntent] = useState(false);
  const [intentError, setIntentError] = useState(null);

  const selectedPlanConfig = useMemo(() => PLAN_CONFIG[selectedPlan], [selectedPlan]);
  const monthlyPrice = selectedPlanConfig.prices.mensual;
  const yearlyPrice = selectedPlanConfig.prices.anual;
  const total = selectedPlanConfig.prices[selectedPeriod];
  const savingPercent = useMemo(() => {
    const fullYearMonthly = monthlyPrice * 12;
    return Math.max(0, Math.round((1 - yearlyPrice / fullYearMonthly) * 100));
  }, [monthlyPrice, yearlyPrice]);

  useEffect(() => {
    const nextParams = new URLSearchParams(searchParams);
    nextParams.set("plan", selectedPlan);
    setSearchParams(nextParams, { replace: true });
  }, [searchParams, selectedPlan, setSearchParams]);

  // Crear PaymentIntent al montar y al cambiar de periodo
  useEffect(() => {
    let cancelled = false;
    const fetchIntent = async () => {
      setLoadingIntent(true);
      setIntentError(null);
      setClientSecret(null);
      try {
        const res = await subscriptionsApi.createPaymentIntent({
          planId: selectedPlan,
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
  }, [selectedPeriod, selectedPlan]);

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
              <div className="pasarela-plan-badge">{selectedPlanConfig.badge}</div>
              <h3 className="pasarela-plan-name">{selectedPlanConfig.name}</h3>
              <p className="pasarela-plan-description">
                {selectedPlanConfig.description}
              </p>

              <ul className="pasarela-features-list">
                {selectedPlanConfig.features.map((feature) => (
                  <li key={feature}>{feature}</li>
                ))}
              </ul>
            </div>

            <div className="pasarela-period-selector" style={{ marginTop: 16 }}>
              <h4 className="pasarela-period-title">Selecciona tu plan</h4>
              <div className="pasarela-period-options">
                {VALID_PLANS.map((planId) => (
                  <button
                    key={planId}
                    type="button"
                    className={`pasarela-period-btn ${selectedPlan === planId ? "pasarela-period-btn--active" : ""}`}
                    onClick={() => setSelectedPlan(planId)}
                  >
                    <div className="pasarela-period-label">{PLAN_CONFIG[planId].name}</div>
                    <div className="pasarela-period-price">{PLAN_CONFIG[planId].prices.mensual.toFixed(2)}€/mes</div>
                  </button>
                ))}
              </div>
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
                  <div className="pasarela-period-price">{monthlyPrice.toFixed(2)}€/mes</div>
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
                  <div className="pasarela-period-price">{yearlyPrice.toFixed(2)}€/año</div>
                  <div className="pasarela-period-save">Ahorra {savingPercent}%</div>
                </button>
              </div>
            </div>

            {/* Total */}
            <div className="pasarela-total">
              <div className="pasarela-total-row">
                <span>Subtotal</span>
                <span>{total.toFixed(2)}€</span>
              </div>
              <div className="pasarela-total-divider"></div>
              <div className="pasarela-total-row pasarela-total-final">
                <span>Total</span>
                <span>{total.toFixed(2)}€</span>
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
                <CheckoutForm selectedPlan={selectedPlan} selectedPeriod={selectedPeriod} total={total} />
              </Elements>
            )}
          </div>
        </div>
      </div>
    </>
  );
}

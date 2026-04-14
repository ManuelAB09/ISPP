import { useState, useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { loadStripe } from "@stripe/stripe-js";
import { Elements, PaymentElement, useStripe, useElements } from "@stripe/react-stripe-js";
import Header from "../../components/Header/Header";
import { communitiesApi } from "../../api/communities.api";
import { getApiBaseUrl } from "../../api/baseUrl";
import "./PasarelaPagoTutor.css";

const stripePromise = loadStripe(process.env.REACT_APP_STRIPE_PUBLIC_KEY);

const toAbsoluteImageUrl = (imageUrl, fallback = '/MeerKatters_logo.png') => {
  const raw = String(imageUrl || '').trim();
  if (!raw) {
    return fallback;
  }
  if (/^https?:\/\//i.test(raw) || raw.startsWith('data:') || raw.startsWith('blob:')) {
    return raw;
  }

  const base = getApiBaseUrl();
  return raw.startsWith('/') ? `${base}${raw}` : `${base}/${raw}`;
};

/**
 * PasarelaPagoTutor - Pantalla de pago para contratar un tutor
 * 
 * IMPORTANTE: Esta pasarela se usa DESPUÉS de que:
 * 1. El usuario envía una solicitud de contratación desde HireTutorModal
 * 2. El profesor ACEPTA la solicitud de contratación
 * 3. El usuario recibe una notificación para proceder con el pago
 * 
 * Esta NO es una acción inmediata. El flujo completo es:
 * Usuario solicita → Profesor acepta → Usuario paga → Contratación activa
 */

function CheckoutForm({ totalPagar, comunidad, tutor, navigate }) {
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
          return_url: window.location.origin + "/profesores?pago=ok",
        },
        redirect: "if_required",
      });

      if (confirmError) {
        setError(confirmError.message);
        setProcessing(false);
        return;
      }

      if (paymentIntent && paymentIntent.status === "succeeded") {
        await communitiesApi.confirmTutorPayment(paymentIntent.id);
        if (comunidad) {
          navigate(`/comunidades/${comunidad.id}?pago=ok`);
        } else {
          navigate("/profesores?pago=ok");
        }
      }
    } catch (err) {
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
    <form onSubmit={handleSubmit} className="ppt-form">
      <PaymentElement />

      {error && (
        <div
          className="ppt-error"
          style={{ padding: "12px", background: "#fef2f2", borderRadius: 8, marginTop: 16 }}
        >
          ⚠️ {error}
        </div>
      )}

      <div className="ppt-security-info">
        <span className="ppt-security-icon">🔒</span>
        <p>
          Tu información está protegida con encriptación SSL de 256 bits.
          El pago se procesa de forma segura a través de Stripe.
        </p>
      </div>

      <div className="ppt-terms">
        <p>
          Al confirmar el pago, aceptas los{" "}
          <a href="/planes" target="_blank" rel="noreferrer">
            términos y condiciones
          </a>{" "}
          de contratación de tutores en MeerKatters.
        </p>
      </div>

      <div className="ppt-actions">
        <button
          type="button"
          onClick={() => navigate(-1)}
          className="ppt-btn ppt-btn--secondary"
          disabled={processing}
        >
          Cancelar
        </button>
        <button
          type="submit"
          className="ppt-btn ppt-btn--primary"
          disabled={processing || !stripe}
        >
          {processing ? (
            <>
              <span className="ppt-spinner"></span>
              Procesando...
            </>
          ) : (
            `Pagar ${totalPagar.toFixed(2)}€`
          )}
        </button>
      </div>
    </form>
  );
}

export default function PasarelaPagoTutor() {
  const navigate = useNavigate();
  const location = useLocation();
  
  const {
    tutor,
    comunidad,
    modalidad,
    duracion,
    tarifa,
  } = location.state || {};

  const [clientSecret, setClientSecret] = useState(null);
  const [loadingIntent, setLoadingIntent] = useState(false);
  const [intentError, setIntentError] = useState(null);

  // Si no hay datos, redirigir
  if (!tutor || !tarifa || !modalidad || !duracion) {
    navigate("/profesores");
    return null;
  }

  const nombreTutor = tutor?.usuario?.nombre || tutor?.nombre || `Tutor #${tutor?.id}`;
  const esContratacionComunidad = Boolean(comunidad);
  const comision = parseFloat(tarifa) * 0.1;
  const tutorRecibe = parseFloat(tarifa) * 0.9;
  const iva = parseFloat(tarifa) * 0.21;
  const totalPagar = parseFloat(tarifa) * 1.21;

  // Create PaymentIntent on mount
  // eslint-disable-next-line react-hooks/rules-of-hooks
  useEffect(() => {
    let cancelled = false;
    const fetchIntent = async () => {
      setLoadingIntent(true);
      setIntentError(null);
      try {
        const communityId = comunidad?.id;
        if (!communityId) {
          setIntentError("Se requiere una comunidad para la contratación.");
          return;
        }
        const res = await communitiesApi.createHiringPaymentIntent(communityId, tutor.id, {
          modalidad,
          duracion,
          tarifaAcordada: parseFloat(tarifa),
          aceptarTerminos: true,
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
  }, [tutor.id, comunidad, modalidad, duracion, tarifa]);

  const elementsOptions = clientSecret
    ? { clientSecret, appearance: { theme: "stripe" } }
    : undefined;

  return (
    <>
      <Header page={"profesores"} />
      <div className="ppt-page">
        <div className="ppt-container">
          {/* Columna izquierda - Resumen */}
          <div className="ppt-summary">
            <h2 className="ppt-summary-title">Resumen de la contratación</h2>

            {/* Información del tutor */}
            <div className="ppt-tutor-card">
              <img
                src={toAbsoluteImageUrl(tutor.usuario?.foto)}
                alt={nombreTutor}
                className="ppt-tutor-avatar"
                onError={e => { e.target.onerror = null; e.target.src = '/MeerKatters_logo.png'; }}
              />
              <div className="ppt-tutor-info">
                <h3 className="ppt-tutor-name">{nombreTutor}</h3>
                {tutor.especialidades && tutor.especialidades.length > 0 && (
                  <p className="ppt-tutor-especialidad">
                    {tutor.especialidades.join(", ")}
                  </p>
                )}
                {tutor.valoracionPromedio > 0 && (
                  <div className="ppt-tutor-rating">
                    ⭐ {tutor.valoracionPromedio.toFixed(1)}
                  </div>
                )}
              </div>
            </div>

            {/* Mensaje informativo sobre aceptación */}
            <div className="ppt-acceptance-notice">
              <div className="ppt-acceptance-icon">✓</div>
              <div className="ppt-acceptance-text">
                <strong>{nombreTutor}</strong> ha aceptado tu solicitud de contratación.
                Completa el pago para activar la contratación.
              </div>
            </div>

            {/* Detalles de la contratación */}
            <div className="ppt-details-box">
              <div className="ppt-detail-row">
                <span className="ppt-detail-label">Tipo de contratación</span>
                <span className="ppt-detail-value">
                  {esContratacionComunidad ? "Para comunidad" : "Personal"}
                </span>
              </div>

              {esContratacionComunidad && (
                <div className="ppt-detail-row">
                  <span className="ppt-detail-label">Comunidad</span>
                  <span className="ppt-detail-value">{comunidad.nombre}</span>
                </div>
              )}

              <div className="ppt-detail-row">
                <span className="ppt-detail-label">Modalidad</span>
                <span className="ppt-detail-value">{modalidad}</span>
              </div>

              <div className="ppt-detail-row">
                <span className="ppt-detail-label">Duración</span>
                <span className="ppt-detail-value">{duracion}</span>
              </div>

              <div className="ppt-detail-row">
                <span className="ppt-detail-label">Tarifa acordada</span>
                <span className="ppt-detail-value">{parseFloat(tarifa).toFixed(2)}€/hora</span>
              </div>
            </div>

            {/* Desglose de pago */}
            <div className="ppt-payment-breakdown">
              <h4 className="ppt-breakdown-title">Desglose del pago</h4>
              
              <div className="ppt-breakdown-row">
                <span>Tarifa del tutor</span>
                <span>{parseFloat(tarifa).toFixed(2)}€</span>
              </div>

              <div className="ppt-breakdown-row ppt-breakdown-row--muted">
                <span>Comisión plataforma (10%)</span>
                <span>+{comision.toFixed(2)}€</span>
              </div>

              <div className="ppt-breakdown-row ppt-breakdown-row--muted">
                <span>IVA (21%)</span>
                <span>+{iva.toFixed(2)}€</span>
              </div>

              <div className="ppt-breakdown-divider"></div>

              <div className="ppt-breakdown-row ppt-breakdown-row--total">
                <span>Total a pagar</span>
                <span>{totalPagar.toFixed(2)}€</span>
              </div>

              <div className="ppt-breakdown-note">
                El tutor recibirá {tutorRecibe.toFixed(2)}€
              </div>
            </div>
          </div>

          {/* Columna derecha - Formulario de pago con Stripe Elements */}
          <div className="ppt-form-section">
            <div className="ppt-header">
              <h1 className="ppt-title">Información de pago</h1>
              <p className="ppt-subtitle">
                Completa el pago para confirmar la contratación
              </p>
            </div>

            {loadingIntent && (
              <div style={{ padding: 40, textAlign: "center" }}>
                <span className="ppt-spinner" style={{ display: "inline-block", marginBottom: 12 }}></span>
                <p>Preparando el formulario de pago...</p>
              </div>
            )}

            {intentError && (
              <div
                className="ppt-error"
                style={{ padding: 16, background: "#fef2f2", borderRadius: 8, marginBottom: 16 }}
              >
                ⚠️ {intentError}
              </div>
            )}

            {clientSecret && elementsOptions && (
              <Elements stripe={stripePromise} options={elementsOptions}>
                <CheckoutForm
                  totalPagar={totalPagar}
                  comunidad={comunidad}
                  tutor={tutor}
                  navigate={navigate}
                />
              </Elements>
            )}
          </div>
        </div>
      </div>
    </>
  );
}

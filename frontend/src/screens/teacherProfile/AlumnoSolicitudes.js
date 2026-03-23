import { useCallback, useEffect, useState } from "react";
import { loadStripe } from "@stripe/stripe-js";
import { Elements, PaymentElement, useStripe, useElements } from "@stripe/react-stripe-js";
import { useSocketContext } from "../../contexts/SocketContext";
import {
  obtenerSolicitudesAlumno,
  crearPaymentIntentSolicitud,
  confirmarPagoSolicitud,
  cancelarSolicitudAlumno,
  calificarSolicitud,
  aprobarReprogramacion,
  rechazarReprogramacion,
} from "../../api/solicitudContratacion";
import "./AlumnoSolicitudes.css";

const stripePromise = loadStripe(process.env.REACT_APP_STRIPE_PUBLIC_KEY);

/** Formulario de pago embebido con Stripe Elements. */
function CheckoutForm({ solicitud, onPaid, onCancel }) {
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
      const { error: submitErr } = await elements.submit();
      if (submitErr) {
        setError(submitErr.message);
        setProcessing(false);
        return;
      }

      const { error: confirmErr, paymentIntent } = await stripe.confirmPayment({
        elements,
        confirmParams: {
          return_url: window.location.href,
        },
        redirect: "if_required",
      });

      if (confirmErr) {
        setError(confirmErr.message);
        setProcessing(false);
        return;
      }

      if (paymentIntent && paymentIntent.status === "succeeded") {
        await confirmarPagoSolicitud(solicitud.id, paymentIntent.id);
        onPaid(solicitud.id);
      }
    } catch (err) {
      setError(
        err?.response?.data?.error || "Error al procesar el pago. Intenta de nuevo."
      );
    } finally {
      setProcessing(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="as-checkout-form">
      <PaymentElement />
      {error && <p className="as-checkout-error">⚠️ {error}</p>}
      <div className="as-checkout-actions">
        <button
          type="button"
          className="as-btn as-btn--secondary"
          onClick={onCancel}
          disabled={processing}
        >
          Cancelar
        </button>
        <button
          type="submit"
          className="as-btn as-btn--pay"
          disabled={processing || !stripe}
        >
          {processing ? "Procesando…" : `Pagar ${solicitud.importeTotal}€`}
        </button>
      </div>
    </form>
  );
}

/**
 * Panel que muestra al alumno sus solicitudes de contratación para un tutor específico.
 * Si una solicitud ha sido aceptada, muestra un botón de pago con Stripe.
 */
const AlumnoSolicitudes = ({ tutorId }) => {
  const { socket, isConnected } = useSocketContext();
  const [solicitudes, setSolicitudes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [payingId, setPayingId] = useState(null);
  const [clientSecret, setClientSecret] = useState(null);
  const [intentError, setIntentError] = useState(null);
  const [cancellingId, setCancellingId] = useState(null);
  const [cancelMotivo, setCancelMotivo] = useState("");
  const [ratingId, setRatingId] = useState(null);
  const [rating, setRating] = useState(0);
  const [ratingComment, setRatingComment] = useState("");
  const [actionLoading, setActionLoading] = useState(false);

  const cargarSolicitudes = useCallback(async () => {
    try {
      setLoading(true);
      const { data } = await obtenerSolicitudesAlumno();
      const filtered = (Array.isArray(data) ? data : []).filter(
        (s) => String(s.tutorId) === String(tutorId)
      );
      setSolicitudes(filtered);
    } catch {
      setSolicitudes([]);
    } finally {
      setLoading(false);
    }
  }, [tutorId]);

  useEffect(() => {
    cargarSolicitudes();
  }, [cargarSolicitudes]);

  // Listen for tutor responses (accept/reject) via WebSocket
  useEffect(() => {
    if (!socket || !isConnected) return;

    const handleRespuesta = (payload) => {
      if (!payload?.id) return;
      setSolicitudes((prev) =>
        prev.map((s) => (s.id === payload.id ? { ...s, ...payload } : s))
      );
    };

    socket.on("solicitud_contratacion_respuesta", handleRespuesta);
    return () => socket.off("solicitud_contratacion_respuesta", handleRespuesta);
  }, [socket, isConnected]);

  const handlePagar = async (solicitud) => {
    setPayingId(solicitud.id);
    setIntentError(null);
    setClientSecret(null);
    try {
      const { data } = await crearPaymentIntentSolicitud(solicitud.id);
      setClientSecret(data.clientSecret || data?.clientSecret);
    } catch (err) {
      setIntentError(
        err?.response?.data?.error || "Error al iniciar el pago"
      );
    }
  };

  const handlePaid = (solicitudId) => {
    setSolicitudes((prev) =>
      prev.map((s) =>
        s.id === solicitudId ? { ...s, estado: "PAGADA" } : s
      )
    );
    setPayingId(null);
    setClientSecret(null);
  };

  const handleCancelPay = () => {
    setPayingId(null);
    setClientSecret(null);
    setIntentError(null);
  };

  const handleCancelarAlumno = async (solicitudId) => {
    setActionLoading(true);
    try {
      const { data } = await cancelarSolicitudAlumno(solicitudId, cancelMotivo);
      setSolicitudes((prev) => prev.map((s) => s.id === solicitudId ? { ...s, ...data } : s));
      setCancellingId(null);
      setCancelMotivo("");
    } catch (err) {
      alert(err?.response?.data?.error || "Error al cancelar.");
    } finally {
      setActionLoading(false);
    }
  };

  const handleCalificar = async (solicitudId) => {
    if (rating < 1 || rating > 5) { alert("Selecciona una calificación (1-5)."); return; }
    setActionLoading(true);
    try {
      const { data } = await calificarSolicitud(solicitudId, rating, ratingComment);
      setSolicitudes((prev) => prev.map((s) => s.id === solicitudId ? { ...s, ...data } : s));
      setRatingId(null);
      setRating(0);
      setRatingComment("");
    } catch (err) {
      alert(err?.response?.data?.error || "Error al calificar.");
    } finally {
      setActionLoading(false);
    }
  };

  const handleAprobarReprog = async (solicitudId) => {
    setActionLoading(true);
    try {
      const { data } = await aprobarReprogramacion(solicitudId);
      setSolicitudes((prev) => prev.map((s) => s.id === solicitudId ? { ...s, ...data } : s));
    } catch (err) {
      alert(err?.response?.data?.error || "Error al aprobar reprogramación.");
    } finally {
      setActionLoading(false);
    }
  };

  const handleRechazarReprog = async (solicitudId) => {
    setActionLoading(true);
    try {
      const { data } = await rechazarReprogramacion(solicitudId);
      setSolicitudes((prev) => prev.map((s) => s.id === solicitudId ? { ...s, ...data } : s));
    } catch (err) {
      alert(err?.response?.data?.error || "Error al rechazar reprogramación.");
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) return null;
  if (solicitudes.length === 0) return null;

  const estadoLabel = {
    PENDIENTE: "⏳ Pendiente",
    ACEPTADA: "✅ Aceptada — pendiente de pago",
    RECHAZADA: "❌ Rechazada",
    PAGADA: "💰 Pagada",
    CANCELADA_ALUMNO: "🚫 Cancelada",
    CANCELADA_TUTOR: "🚫 Cancelada por tutor",
    COMPLETADA: "🏁 Completada",
    NO_ASISTIDA: "❌ No asistida",
    REPROGRAMACION_PENDIENTE: "🔄 Reprogramación pendiente",
  };

  /** Comprueba si la fecha/hora de la clase ya ha pasado. */
  const clasePasada = (s) => {
    if (!s.dia || !s.horaInicio) return false;
    const fechaHora = new Date(`${s.dia}T${s.horaInicio}`);
    return fechaHora < new Date();
  };

  return (
    <section className="as-panel">
      <h2 className="as-panel__title">📋 Mis solicitudes de contratación</h2>
      <div className="as-list">
        {solicitudes.map((s) => (
          <div key={s.id} className={`as-card as-card--${s.estado.toLowerCase()}`}>
            <div className="as-card__header">
              <span className="as-card__estado">{estadoLabel[s.estado] || s.estado}</span>
            </div>
            <div className="as-card__details">
              <div className="as-card__row">
                <span className="as-card__label">📅 Día:</span>
                <span>{s.dia}</span>
              </div>
              <div className="as-card__row">
                <span className="as-card__label">🕐 Horario:</span>
                <span>{s.horaInicio} – {s.horaFin}</span>
              </div>
              <div className="as-card__row">
                <span className="as-card__label">💰 Importe:</span>
                <span><strong>{s.importeTotal}€</strong> ({s.tarifaHora}€/h)</span>
              </div>
              {s.modalidad && (
                <div className="as-card__row">
                  <span className="as-card__label">📍 Modalidad:</span>
                  <span>{s.modalidad === "ONLINE" ? "Online" : s.modalidad === "PRESENCIAL" ? "Presencial" : "Híbrido"}</span>
                </div>
              )}
              {s.motivoRechazo && (
                <div className="as-card__row">
                  <span className="as-card__label">📝 Motivo:</span>
                  <span>{s.motivoRechazo}</span>
                </div>
              )}
              {s.zoomJoinUrl && (
                <div className="as-card__row">
                  <span className="as-card__label">📹 Zoom:</span>
                  <a href={s.zoomJoinUrl} target="_blank" rel="noopener noreferrer" style={{ color: "#2563eb", fontWeight: "bold" }}>
                    Unirse a la reunión
                  </a>
                </div>
              )}
            </div>

            {s.estado === "ACEPTADA" && payingId !== s.id && s.tutorStripeConfigured && !clasePasada(s) && (
              <div className="as-card__actions">
                <button
                  className="as-btn as-btn--pay"
                  onClick={() => handlePagar(s)}
                >
                  💳 Pagar {s.importeTotal}€
                </button>
              </div>
            )}

            {s.estado === "ACEPTADA" && !s.tutorStripeConfigured && !clasePasada(s) && (
              <div className="as-card__actions">
                <span className="as-card__info">
                  ⚠️ El profesor aún no ha configurado su cuenta de Stripe Connect para recibir pagos. Contacta con tu profesor para que configure sus datos bancarios.
                </span>
              </div>
            )}

            {s.estado === "ACEPTADA" && clasePasada(s) && (
              <div className="as-card__actions">
                <span className="as-card__info as-card__info--expired">
                  ⏰ La fecha de esta clase ya ha pasado. No se puede realizar el pago. Si lo deseas, puedes enviar una nueva solicitud.
                </span>
              </div>
            )}

            {payingId === s.id && clientSecret && (
              <div className="as-card__payment">
                <Elements
                  stripe={stripePromise}
                  options={{ clientSecret, appearance: { theme: "stripe" } }}
                >
                  <CheckoutForm
                    solicitud={s}
                    onPaid={handlePaid}
                    onCancel={handleCancelPay}
                  />
                </Elements>
              </div>
            )}

            {payingId === s.id && !clientSecret && intentError && (
              <div className="as-card__payment">
                <p className="as-checkout-error">⚠️ {intentError}</p>
                <div className="as-checkout-actions">
                  <button
                    className="as-btn as-btn--secondary"
                    onClick={handleCancelPay}
                  >
                    Volver
                  </button>
                  <button
                    className="as-btn as-btn--pay"
                    onClick={() => handlePagar(s)}
                  >
                    Reintentar
                  </button>
                </div>
              </div>
            )}

            {payingId === s.id && !clientSecret && !intentError && (
              <p className="as-loading">Preparando pasarela de pago…</p>
            )}

            {/* Calificación existente */}
            {s.calificacion && (
              <div className="as-card__row" style={{ marginTop: "8px" }}>
                <span className="as-card__label">⭐ Calificación:</span>
                <span>{"⭐".repeat(s.calificacion)} {s.comentarioAlumno && <em>— {s.comentarioAlumno}</em>}</span>
              </div>
            )}

            {/* Reprogramación pendiente de aprobación */}
            {s.estado === "REPROGRAMACION_PENDIENTE" && s.reprogramacionDia && (
              <div style={{ background: "#fff8e1", padding: "12px", borderRadius: "8px", marginTop: "8px" }}>
                <p style={{ marginBottom: "8px" }}>
                  El tutor propone cambiar a: <strong>{s.reprogramacionDia}</strong> de{" "}
                  <strong>{s.reprogramacionHoraInicio}</strong> a <strong>{s.reprogramacionHoraFin}</strong>
                </p>
                <div className="as-card__actions">
                  <button className="as-btn as-btn--secondary" onClick={() => handleRechazarReprog(s.id)} disabled={actionLoading}>
                    Rechazar
                  </button>
                  <button className="as-btn as-btn--pay" onClick={() => handleAprobarReprog(s.id)} disabled={actionLoading}>
                    {actionLoading ? "Procesando…" : "✓ Aprobar cambio"}
                  </button>
                </div>
              </div>
            )}

            {/* Botón cancelar alumno */}
            {s.puedeSerCanceladaPorAlumno && cancellingId !== s.id && (
              <div className="as-card__actions">
                <button className="as-btn as-btn--secondary" onClick={() => setCancellingId(s.id)}>
                  🚫 Cancelar clase
                </button>
              </div>
            )}

            {/* Formulario de cancelación */}
            {cancellingId === s.id && (
              <div style={{ background: "#fef2f2", padding: "12px", borderRadius: "8px", marginTop: "8px" }}>
                <input
                  type="text"
                  placeholder="Motivo de cancelación (opcional)"
                  value={cancelMotivo}
                  onChange={(e) => setCancelMotivo(e.target.value)}
                  style={{ width: "100%", padding: "8px", marginBottom: "8px", borderRadius: "4px", border: "1px solid #ccc" }}
                />
                <div className="as-card__actions">
                  <button className="as-btn as-btn--secondary" onClick={() => { setCancellingId(null); setCancelMotivo(""); }} disabled={actionLoading}>Volver</button>
                  <button className="as-btn as-btn--pay" style={{ background: "#dc2626" }} onClick={() => handleCancelarAlumno(s.id)} disabled={actionLoading}>
                    {actionLoading ? "Cancelando…" : "Confirmar cancelación"}
                  </button>
                </div>
              </div>
            )}

            {/* Botón calificar (clase completada o pasada sin calificación) */}
            {(s.estado === "COMPLETADA" || (s.estado === "PAGADA" && clasePasada(s))) && !s.calificacion && ratingId !== s.id && (
              <div className="as-card__actions">
                <button className="as-btn as-btn--pay" onClick={() => setRatingId(s.id)}>
                  ⭐ Calificar clase
                </button>
              </div>
            )}

            {/* Formulario de calificación */}
            {ratingId === s.id && (
              <div style={{ background: "#f0fdf4", padding: "12px", borderRadius: "8px", marginTop: "8px" }}>
                <div style={{ display: "flex", gap: "8px", justifyContent: "center", margin: "8px 0" }}>
                  {[1, 2, 3, 4, 5].map((n) => (
                    <button key={n} onClick={() => setRating(n)} style={{ fontSize: "1.5rem", background: "none", border: "none", cursor: "pointer", opacity: n <= rating ? 1 : 0.3 }}>
                      ⭐
                    </button>
                  ))}
                </div>
                <textarea
                  rows={2}
                  placeholder="Comentario (opcional)…"
                  value={ratingComment}
                  onChange={(e) => setRatingComment(e.target.value)}
                  style={{ width: "100%", padding: "8px", borderRadius: "4px", border: "1px solid #ccc", marginBottom: "8px" }}
                />
                <div className="as-card__actions">
                  <button className="as-btn as-btn--secondary" onClick={() => { setRatingId(null); setRating(0); setRatingComment(""); }} disabled={actionLoading}>Volver</button>
                  <button className="as-btn as-btn--pay" onClick={() => handleCalificar(s.id)} disabled={actionLoading || rating === 0}>
                    {actionLoading ? "Enviando…" : "Enviar calificación"}
                  </button>
                </div>
              </div>
            )}
          </div>
        ))}
      </div>
    </section>
  );
};

export default AlumnoSolicitudes;

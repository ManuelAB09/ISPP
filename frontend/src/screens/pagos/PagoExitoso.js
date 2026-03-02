import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { subscriptionsApi } from "../../api/subscriptions.api";
import Header from "../../components/Header/Header";
import "./PagoExitoso.css";

export default function PagoExitoso() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const sessionId = searchParams.get("session_id");
  const [countdown, setCountdown] = useState(5);
  const [confirmStatus, setConfirmStatus] = useState("loading"); // "loading" | "ok" | "error"

  // Activar la suscripción en el backend al cargar la página.
  // En local, el webhook de Stripe no puede llegar a localhost, por lo que
  // usamos el endpoint de confirmación manual.
  useEffect(() => {
    subscriptionsApi
      .confirmPayment()
      .then(() => setConfirmStatus("ok"))
      .catch((err) => {
        // 400 significa que ya había suscripción activa → también es OK
        if (err?.status === 400 || err?.response?.status === 400) {
          setConfirmStatus("ok");
        } else {
          console.error("Error confirmando suscripción:", err);
          setConfirmStatus("error");
        }
      });
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (confirmStatus !== "ok") return;
    const timer = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          navigate("/planes");
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [confirmStatus, navigate]);

  return (
    <>
      <Header page={"planes"} />
      <div className="pago-exitoso-page">
        <div className="pago-exitoso-card">
          {confirmStatus === "loading" && (
            <>
              <div className="pago-exitoso-icon">⏳</div>
              <h1 className="pago-exitoso-title">Activando tu suscripción…</h1>
              <p className="pago-exitoso-desc">Por favor espera un momento.</p>
            </>
          )}

          {confirmStatus === "ok" && (
            <>
              <div className="pago-exitoso-icon">✅</div>
              <h1 className="pago-exitoso-title">¡Pago realizado con éxito!</h1>
              <p className="pago-exitoso-desc">
                Tu suscripción ha sido activada correctamente. Ya puedes
                disfrutar de todas las funcionalidades premium.
              </p>

              {sessionId && (
                <p className="pago-exitoso-session">
                  Referencia: <code>{sessionId}</code>
                </p>
              )}

              <p className="pago-exitoso-redirect">
                Serás redirigido automáticamente en{" "}
                <strong>{countdown}</strong> segundo{countdown !== 1 ? "s" : ""}…
              </p>
            </>
          )}

          {confirmStatus === "error" && (
            <>
              <div className="pago-exitoso-icon">⚠️</div>
              <h1 className="pago-exitoso-title">Pago recibido</h1>
              <p className="pago-exitoso-desc">
                El pago se ha procesado correctamente, pero no se pudo activar
                la suscripción de forma automática. Contacta con soporte si el
                problema persiste.
              </p>
            </>
          )}

          <div className="pago-exitoso-actions">
            <button
              className="pago-exitoso-btn pago-exitoso-btn--primary"
              onClick={() => navigate("/")}
            >
              Ir al inicio
            </button>
            <button
              className="pago-exitoso-btn pago-exitoso-btn--secondary"
              onClick={() => navigate("/planes")}
            >
              Ver mis planes
            </button>
          </div>
        </div>
      </div>
    </>
  );
}

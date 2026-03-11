import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { subscriptionsApi } from "../../api/subscriptions.api";
import { institutionsApi } from "../../api/institutions.api";
import Header from "../../components/Header/Header";
import "./PagoExitoso.css";

export default function PagoExitoso() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const sessionId = searchParams.get("session_id");
  const tipo = searchParams.get("tipo");
  const [countdown, setCountdown] = useState(5);
  const [confirmStatus, setConfirmStatus] = useState("loading");

  const destino = tipo === "institucional" ? "/planes/instituciones" : "/pagos";

  useEffect(() => {
    const activar = async () => {
      try {
        if (sessionId) {
          if (tipo === "institucional") {
            await institutionsApi.verifySession(sessionId);
          } else {
            await subscriptionsApi.verifySession(sessionId);
          }
        } else {
          await subscriptionsApi.confirmPayment();
        }
        setConfirmStatus("ok");
      } catch (err) {
        if (err?.status === 400 || err?.response?.status === 400) {
          setConfirmStatus("ok");
        } else {
          console.error("Error confirmando pago:", err);
          setConfirmStatus("error");
        }
      }
    };

    activar();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // ✅ navigate fuera del updater de setCountdown
  useEffect(() => {
    if (confirmStatus !== "ok") return;

    let current = 5;
    const timer = setInterval(() => {
      current -= 1;
      setCountdown(current);
      if (current <= 0) {
        clearInterval(timer);
        navigate(destino);
      }
    }, 1000);

    return () => clearInterval(timer);
  }, [confirmStatus]); // eslint-disable-line react-hooks/exhaustive-deps

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
                {tipo === "institucional"
                  ? "Tu plan institucional ha sido activado correctamente. Ya puedes gestionar tu institución."
                  : "Tu suscripción Premium ha sido activada correctamente. Ya puedes disfrutar de todas las funcionalidades."}
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
                El pago se procesó correctamente, pero no se pudo activar
                la suscripción automáticamente. Contacta con soporte si el problema persiste.
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
              onClick={() => navigate(destino)}
            >
              {tipo === "institucional" ? "Ver mi institución" : "Ver mis planes"}
            </button>
          </div>

        </div>
      </div>
    </>
  );
}
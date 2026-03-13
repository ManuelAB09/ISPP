import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { subscriptionsApi } from "../../api/subscriptions.api";
import { institutionsApi } from "../../api/institutions.api";
import { verifyVerificationSession, verifyHiringSession } from "../../api/tutorEndpoints";
import Header from "../../components/Header/Header";
import "./PagoExitoso.css";

export default function PagoExitoso() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const sessionId = searchParams.get("session_id");
  const tipo = searchParams.get("tipo");
  const [countdown, setCountdown] = useState(5);
  const [confirmStatus, setConfirmStatus] = useState("loading");

  const destino =
    tipo === "institucional" ? "/planes/instituciones"
      : tipo === "verificacion" ? "/profesores"
        : tipo === "contratacion" ? "/mis-contrataciones"
          : "/pagos";

  useEffect(() => {
    const activar = async () => {
      try {
        if (sessionId) {
          if (tipo === "institucional") {
            await institutionsApi.verifySession(sessionId);
          } else if (tipo === "verificacion") {
            await verifyVerificationSession(sessionId);
          } else if (tipo === "contratacion") {
            await verifyHiringSession(sessionId);
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

  useEffect(() => {
    if (confirmStatus !== "ok") return;

    let current = 5;
    const timer = setInterval(() => {
      current -= 1;
      setCountdown(current);
      if (current <= 0) {
        clearInterval(timer);
        window.location.href = destino;
      }
    }, 1000);

    return () => clearInterval(timer);
  }, [confirmStatus]); // eslint-disable-line react-hooks/exhaustive-deps

  const getDescripcionExito = () => {
    if (tipo === "institucional") {
      return "Tu plan institucional ha sido activado correctamente. Ya puedes gestionar tu institución.";
    }
    if (tipo === "verificacion") {
      return "¡Tu perfil de tutor ha sido verificado! Ya apareces como tutor verificado en el listado de profesores.";
    }
    if (tipo === "contratacion") {
      return "¡Contratación completada! El tutor ya está activo en tu comunidad.";
    }
    return "Tu suscripción Premium ha sido activada correctamente. Ya puedes disfrutar de todas las funcionalidades.";
  };

  const getBotonSecundario = () => {
    if (tipo === "institucional") return "Ver mi institución";
    if (tipo === "verificacion") return "Ver listado";
    if (tipo === "contratacion") return "Ver mis contrataciones";
    return "Ver mis planes";
  };

  const getTituloActivando = () => {
    if (tipo === "verificacion") return "Activando tu verificación…";
    if (tipo === "contratacion") return "Activando tu contratación…";
    return "Activando tu suscripción…";
  };

  return (
    <>
      <Header page={"planes"} />
      <div className="pago-exitoso-page">
        <div className="pago-exitoso-card">

          {confirmStatus === "loading" && (
            <>
              <div className="pago-exitoso-icon">⏳</div>
              <h1 className="pago-exitoso-title">{getTituloActivando()}</h1>
              <p className="pago-exitoso-desc">Por favor espera un momento.</p>
            </>
          )}

          {confirmStatus === "ok" && (
            <>
              <div className="pago-exitoso-icon">✅</div>
              <h1 className="pago-exitoso-title">¡Pago realizado con éxito!</h1>
              <p className="pago-exitoso-desc">{getDescripcionExito()}</p>
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
                automáticamente. Contacta con soporte si el problema persiste.
              </p>
            </>
          )}

          <div className="pago-exitoso-actions">
            <button
              className="pago-exitoso-btn pago-exitoso-btn--primary"
              onClick={() => { window.location.href = "/"; }}
            >
              Ir al inicio
            </button>
            <button
              className="pago-exitoso-btn pago-exitoso-btn--secondary"
              onClick={() => { window.location.href = destino; }}
            >
              {getBotonSecundario()}
            </button>
          </div>

        </div>
      </div>
    </>
  );
}
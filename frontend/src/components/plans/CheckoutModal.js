import { useState } from "react";
import "./CheckoutModal.css";

export default function CheckoutModal({
  open,
  plan,         
  onClose,
  onConfirm,
  loading,
}) {
  const [period, setPeriod] = useState("mensual");

  if (!open) return null;

  return (
    <div className="checkoutOverlay" onClick={onClose}>
      <div className="checkoutModal" onClick={(e) => e.stopPropagation()}>
        <h2 className="checkoutTitle">Confirmar suscripción</h2>

        <div className="checkoutContent">
          <div className="checkoutPlanSection">
            <span className="checkoutLabel">Plan seleccionado</span>
            <div className="checkoutPlanButtons">
              <button
                type="button"
                className={
                  period === "mensual"
                    ? "checkoutPlanButtonActive"
                    : "checkoutPlanButton"
                }
                onClick={() => setPeriod("mensual")}
                disabled={loading}
              >
                {plan || "PREMIUM"} mensual
              </button>
              <button
                type="button"
                className={
                  period === "anual"
                    ? "checkoutPlanButtonActive"
                    : "checkoutPlanButton"
                }
                onClick={() => setPeriod("anual")}
                disabled={loading}
              >
                {plan || "PREMIUM"} anual
              </button>
            </div>
          </div>
          
          <div className="checkoutInfoBox">
            <p className="checkoutInfoText">
              Al continuar, se activará tu plan <strong>{plan || "PREMIUM"}</strong> con acceso a todas sus funcionalidades.
            </p>
          </div>
        </div>

        <div className="checkoutActions">
          <button className="checkoutBtnSecondary" onClick={onClose} disabled={loading}>
            Cancelar
          </button>
          <button
            className="checkoutBtnPrimary"
            onClick={() => { onConfirm(period); onClose(); }}
            disabled={loading}
          >
            {loading ? "Procesando..." : "Confirmar"}
          </button>
        </div>
      </div>
    </div>
  );
}

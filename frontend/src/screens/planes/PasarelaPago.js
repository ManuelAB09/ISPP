import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { subscriptionsApi } from "../../api/subscriptions.api";
import Header from "../../components/Header/Header";
import "./PasarelaPago.css";

/**
 * Componente de la pasarela de pago de suscripción PREMIUM
 * Funciona con Stripe Checkout: no maneja tarjetas directamente
 */
export default function PasarelaPago() {
  const navigate = useNavigate();
  const [selectedPeriod, setSelectedPeriod] = useState("mensual");
  const [processing, setProcessing] = useState(false);

  const prices = {
    mensual: "2.99",
    anual: "25.99",
  };

  /**
   * Maneja el envío del formulario
   */
  const handleSubscribe = async () => {
    setProcessing(true);
    try {
      // Llamada al backend con plan y periodo
      const paymentResponse = await subscriptionsApi.subscribe({
        planId: "PREMIUM",
        aceptarTerminos: true,
        periodo: selectedPeriod, // mensual o anual
      });

      if (paymentResponse?.paymentUrl) {
        // Redirige al checkout de Stripe
        window.location.href = paymentResponse.paymentUrl;
      } else {
        // En desarrollo o fallback
        await subscriptionsApi.confirmPayment();
        alert("¡Pago procesado exitosamente! Bienvenido a Premium");
        navigate("/pagos");
      }
    } catch (error) {
      console.error("Error al procesar el pago:", error);
      alert(
        error?.response?.data?.error || "Error al procesar el pago. Intenta de nuevo."
      );
    } finally {
      setProcessing(false);
    }
  };

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
                <li>✓ Más límites y herramientas</li>
                <li>✓ Mejor experiencia de uso</li>
                <li>✓ Acceso a funcionalidades avanzadas</li>
                <li>✓ Soporte prioritario</li>
                <li>✓ Sin publicidad</li>
              </ul>
            </div>

            {/* Selector de período */}
            <div className="pasarela-period-selector">
              <h4 className="pasarela-period-title">Selecciona tu período</h4>
              <div className="pasarela-period-options">
                <button
                  type="button"
                  className={`pasarela-period-btn ${selectedPeriod === "mensual"
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
                  className={`pasarela-period-btn ${selectedPeriod === "anual" ? "pasarela-period-btn--active" : ""
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
                <span>{(parseFloat(prices[selectedPeriod]) * 0.21).toFixed(2)}€</span>
              </div>
              <div className="pasarela-total-divider"></div>
              <div className="pasarela-total-row pasarela-total-final">
                <span>Total</span>
                <span>{(parseFloat(prices[selectedPeriod]) * 1.21).toFixed(2)}€</span>
              </div>
            </div>
          </div>

          {/* Columna derecha - Botón de pago */}
          <div className="pasarela-form-section">
            <div className="pasarela-header">
              <h1 className="pasarela-title">Información de pago</h1>
              <p className="pasarela-subtitle">
                Serás redirigido de forma segura a Stripe para completar la suscripción.
              </p>
            </div>

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
                type="button"
                onClick={handleSubscribe}
                className="pasarela-btn pasarela-btn--primary"
                disabled={processing}
              >
                {processing
                  ? "Procesando..."
                  : `Pagar ${(parseFloat(prices[selectedPeriod]) * 1.21).toFixed(2)}€`}
              </button>
            </div>

            <div className="pasarela-security-info">
              <span className="pasarela-security-icon">🔒</span>
              <p>
                Tu información está protegida con encriptación SSL de 256 bits.
                No almacenamos datos de tarjetas de crédito.
              </p>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
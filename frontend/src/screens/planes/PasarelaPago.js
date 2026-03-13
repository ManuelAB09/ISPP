import { useState, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { subscriptionsApi } from "../../api/subscriptions.api";
import Header from "../../components/Header/Header";
import "./PasarelaPago.css";

/**
 * Formulario de pago con inputs manuales filtrados.
 */
function CheckoutForm({ selectedPeriod, prices }) {
  const navigate = useNavigate();
  const [processing, setProcessing] = useState(false);
  const [error, setError] = useState(null);
  const [errors, setErrors] = useState({});

  const [formData, setFormData] = useState({
    cardNumber: "",
    cardName: "",
    expiryDate: "",
    cvc: "",
  });

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    let formattedValue = value;

    if (name === "cardNumber") {
      const digitsOnly = value.replace(/\D/g, "").substring(0, 16);
      formattedValue = digitsOnly.replace(/(\d{4})(?=\d)/g, "$1 ");
    }

    if (name === "expiryDate") {
      const digitsOnly = value.replace(/\D/g, "").substring(0, 4);
      if (digitsOnly.length >= 3) {
        formattedValue = digitsOnly.substring(0, 2) + "/" + digitsOnly.substring(2);
      } else {
        formattedValue = digitsOnly;
      }
    }

    if (name === "cvc") {
      formattedValue = value.replace(/\D/g, "").substring(0, 3);
    }

    setFormData((prev) => ({ ...prev, [name]: formattedValue }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: "" }));
    }
  };

  const validateForm = () => {
    const newErrors = {};
    const cardNumberClean = formData.cardNumber.replace(/\s/g, "");
    if (!cardNumberClean) {
      newErrors.cardNumber = "El numero de tarjeta es requerido";
    } else if (cardNumberClean.length !== 16) {
      newErrors.cardNumber = "El numero de tarjeta debe tener 16 digitos";
    }
    if (!formData.cardName.trim()) {
      newErrors.cardName = "El nombre del titular es requerido";
    }
    if (!formData.expiryDate) {
      newErrors.expiryDate = "La fecha de caducidad es requerida";
    } else if (formData.expiryDate.length !== 5) {
      newErrors.expiryDate = "Formato invalido (MM/YY)";
    } else {
      const [month, year] = formData.expiryDate.split("/");
      const currentYear = new Date().getFullYear() % 100;
      const currentMonth = new Date().getMonth() + 1;
      if (parseInt(month) < 1 || parseInt(month) > 12) {
        newErrors.expiryDate = "Mes invalido";
      } else if (
        parseInt(year) < currentYear ||
        (parseInt(year) === currentYear && parseInt(month) < currentMonth)
      ) {
        newErrors.expiryDate = "Tarjeta expirada";
      }
    }
    if (!formData.cvc) {
      newErrors.cvc = "El CVC es requerido";
    } else if (formData.cvc.length !== 3) {
      newErrors.cvc = "El CVC debe tener 3 digitos";
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validateForm()) return;

    setProcessing(true);
    setError(null);

    try {
      await subscriptionsApi.confirmPayment();
      navigate("/planes/success?embedded=true");
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
      {/* Numero de tarjeta */}
      <div className="pasarela-form-group">
        <label className="pasarela-label">Numero de tarjeta</label>
        <div className="pasarela-input-wrapper">
          <span className="pasarela-card-icon">💳</span>
          <input
            type="text"
            name="cardNumber"
            value={formData.cardNumber}
            onChange={handleInputChange}
            placeholder="1234 5678 9012 3456"
            inputMode="numeric"
            autoComplete="cc-number"
            className={`pasarela-input ${
              errors.cardNumber ? "pasarela-input--error" : ""
            }`}
          />
        </div>
        {errors.cardNumber && (
          <span className="pasarela-error">{errors.cardNumber}</span>
        )}
      </div>

      {/* Nombre del titular */}
      <div className="pasarela-form-group">
        <label className="pasarela-label">Nombre del titular</label>
        <input
          type="text"
          name="cardName"
          value={formData.cardName}
          onChange={handleInputChange}
          placeholder="NOMBRE APELLIDO"
          autoComplete="cc-name"
          className={`pasarela-input ${
            errors.cardName ? "pasarela-input--error" : ""
          }`}
          style={{ textTransform: "uppercase" }}
        />
        {errors.cardName && (
          <span className="pasarela-error">{errors.cardName}</span>
        )}
      </div>

      {/* Fecha y CVC */}
      <div className="pasarela-form-row">
        <div className="pasarela-form-group">
          <label className="pasarela-label">Fecha de caducidad</label>
          <input
            type="text"
            name="expiryDate"
            value={formData.expiryDate}
            onChange={handleInputChange}
            placeholder="MM/YY"
            inputMode="numeric"
            autoComplete="cc-exp"
            className={`pasarela-input ${
              errors.expiryDate ? "pasarela-input--error" : ""
            }`}
          />
          {errors.expiryDate && (
            <span className="pasarela-error">{errors.expiryDate}</span>
          )}
        </div>

        <div className="pasarela-form-group">
          <label className="pasarela-label">CVC</label>
          <input
            type="text"
            name="cvc"
            value={formData.cvc}
            onChange={handleInputChange}
            placeholder="123"
            inputMode="numeric"
            autoComplete="cc-csc"
            className={`pasarela-input ${
              errors.cvc ? "pasarela-input--error" : ""
            }`}
          />
          {errors.cvc && (
            <span className="pasarela-error">{errors.cvc}</span>
          )}
        </div>
      </div>

      {error && (
        <div
          className="pasarela-error"
          style={{ padding: "12px", background: "#fef2f2", borderRadius: 8 }}
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
          disabled={processing}
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
          Tu informacion esta protegida con encriptacion SSL de 256 bits. No
          almacenamos datos de tarjetas de credito.
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

  const prices = useMemo(
    () => ({
      mensual: "2.99",
      anual: "25.99",
    }),
    []
  );

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
                  <div className="pasarela-period-price">25.99€/ano</div>
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

            <CheckoutForm
              selectedPeriod={selectedPeriod}
              prices={prices}
            />
          </div>
        </div>
      </div>
    </>
  );
}

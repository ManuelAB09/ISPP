import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Header from "../../components/Header/Header";
import "./PasarelaPago.css";

export default function PasarelaPago() {
  const navigate = useNavigate();
  const [selectedPeriod, setSelectedPeriod] = useState("mensual");
  const [formData, setFormData] = useState({
    cardNumber: "",
    cardName: "",
    expiryDate: "",
    cvv: "",
  });
  const [processing, setProcessing] = useState(false);
  const [errors, setErrors] = useState({});

  const prices = {
    mensual: "2.99",
    anual: "25.99",
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    let formattedValue = value;

    // Formatear número de tarjeta (espacios cada 4 dígitos)
    if (name === "cardNumber") {
      formattedValue = value
        .replace(/\s/g, "")
        .replace(/(\d{4})/g, "$1 ")
        .trim();
      formattedValue = formattedValue.substring(0, 19); // Máx 16 dígitos + 3 espacios
    }

    // Formatear fecha de expiración (MM/YY)
    if (name === "expiryDate") {
      formattedValue = value
        .replace(/\D/g, "")
        .replace(/(\d{2})(\d)/, "$1/$2")
        .substring(0, 5);
    }

    // Limitar CVV a 3 dígitos
    if (name === "cvv") {
      formattedValue = value.replace(/\D/g, "").substring(0, 3);
    }

    setFormData((prev) => ({ ...prev, [name]: formattedValue }));

    // Limpiar error del campo
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: "" }));
    }
  };

  const validateForm = () => {
    const newErrors = {};

    // Validar número de tarjeta
    const cardNumberClean = formData.cardNumber.replace(/\s/g, "");
    if (!cardNumberClean) {
      newErrors.cardNumber = "El número de tarjeta es requerido";
    } else if (cardNumberClean.length !== 16) {
      newErrors.cardNumber = "El número de tarjeta debe tener 16 dígitos";
    }

    // Validar nombre
    if (!formData.cardName.trim()) {
      newErrors.cardName = "El nombre del titular es requerido";
    }

    // Validar fecha de expiración
    if (!formData.expiryDate) {
      newErrors.expiryDate = "La fecha de expiración es requerida";
    } else if (formData.expiryDate.length !== 5) {
      newErrors.expiryDate = "Formato inválido (MM/YY)";
    } else {
      const [month, year] = formData.expiryDate.split("/");
      const currentYear = new Date().getFullYear() % 100;
      const currentMonth = new Date().getMonth() + 1;

      if (parseInt(month) < 1 || parseInt(month) > 12) {
        newErrors.expiryDate = "Mes inválido";
      } else if (
        parseInt(year) < currentYear ||
        (parseInt(year) === currentYear && parseInt(month) < currentMonth)
      ) {
        newErrors.expiryDate = "Tarjeta expirada";
      }
    }

    // Validar CVV
    if (!formData.cvv) {
      newErrors.cvv = "El CVV es requerido";
    } else if (formData.cvv.length !== 3) {
      newErrors.cvv = "El CVV debe tener 3 dígitos";
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    setProcessing(true);

    try {
      // TODO: Implementar llamada al endpoint del backend
      // const response = await subscriptionsApi.subscribe(selectedPeriod);
      // Se debería enviar el período seleccionado y procesar el pago
      
      // Simulación temporal
      await new Promise((resolve) => setTimeout(resolve, 2000));

      // Redirigir a la página de éxito o planes
      alert("¡Pago procesado exitosamente! Bienvenido a Premium");
      navigate("/planes");
    } catch (error) {
      console.error("Error al procesar el pago:", error);
      alert("Error al procesar el pago. Por favor, intente nuevamente.");
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
                  className={`pasarela-period-btn ${
                    selectedPeriod === "mensual" ? "pasarela-period-btn--active" : ""
                  }`}
                  onClick={() => setSelectedPeriod("mensual")}
                >
                  <div className="pasarela-period-label">Mensual</div>
                  <div className="pasarela-period-price">2.99€/mes</div>
                </button>

                <button
                  type="button"
                  className={`pasarela-period-btn ${
                    selectedPeriod === "anual" ? "pasarela-period-btn--active" : ""
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

          {/* Columna derecha - Formulario de pago */}
          <div className="pasarela-form-section">
            <div className="pasarela-header">
              <h1 className="pasarela-title">Información de pago</h1>
              <p className="pasarela-subtitle">
                Completa los datos de tu tarjeta para finalizar la suscripción
              </p>
            </div>

            <form onSubmit={handleSubmit} className="pasarela-form">
              {/* Número de tarjeta */}
              <div className="pasarela-form-group">
                <label htmlFor="cardNumber" className="pasarela-label">
                  Número de tarjeta
                </label>
                <div className="pasarela-input-wrapper">
                  <span className="pasarela-card-icon">💳</span>
                  <input
                    type="text"
                    id="cardNumber"
                    name="cardNumber"
                    value={formData.cardNumber}
                    onChange={handleInputChange}
                    placeholder="1234 5678 9012 3456"
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
                <label htmlFor="cardName" className="pasarela-label">
                  Nombre del titular
                </label>
                <input
                  type="text"
                  id="cardName"
                  name="cardName"
                  value={formData.cardName}
                  onChange={handleInputChange}
                  placeholder="NOMBRE APELLIDO"
                  className={`pasarela-input ${
                    errors.cardName ? "pasarela-input--error" : ""
                  }`}
                  style={{ textTransform: "uppercase" }}
                />
                {errors.cardName && (
                  <span className="pasarela-error">{errors.cardName}</span>
                )}
              </div>

              {/* Fecha de expiración y CVV */}
              <div className="pasarela-form-row">
                <div className="pasarela-form-group">
                  <label htmlFor="expiryDate" className="pasarela-label">
                    Fecha de expiración
                  </label>
                  <input
                    type="text"
                    id="expiryDate"
                    name="expiryDate"
                    value={formData.expiryDate}
                    onChange={handleInputChange}
                    placeholder="MM/YY"
                    className={`pasarela-input ${
                      errors.expiryDate ? "pasarela-input--error" : ""
                    }`}
                  />
                  {errors.expiryDate && (
                    <span className="pasarela-error">{errors.expiryDate}</span>
                  )}
                </div>

                <div className="pasarela-form-group">
                  <label htmlFor="cvv" className="pasarela-label">
                    CVV
                  </label>
                  <input
                    type="text"
                    id="cvv"
                    name="cvv"
                    value={formData.cvv}
                    onChange={handleInputChange}
                    placeholder="123"
                    className={`pasarela-input ${
                      errors.cvv ? "pasarela-input--error" : ""
                    }`}
                  />
                  {errors.cvv && <span className="pasarela-error">{errors.cvv}</span>}
                </div>
              </div>

              {/* Información de seguridad */}
              <div className="pasarela-security-info">
                <span className="pasarela-security-icon">🔒</span>
                <p>
                  Tu información está protegida con encriptación SSL de 256 bits.
                  No almacenamos datos de tarjetas de crédito.
                </p>
              </div>

              {/* Botones de acción */}
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
                      <span className="pasarela-spinner"></span>
                      Procesando...
                    </>
                  ) : (
                    `Pagar ${(parseFloat(prices[selectedPeriod]) * 1.21).toFixed(2)}€`
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </>
  );
}

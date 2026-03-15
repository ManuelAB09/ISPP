import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import Header from "../../components/Header/Header";
import { getApiBaseUrl } from "../../api/baseUrl";
import "./PasarelaPagoTutor.css";

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
export default function PasarelaPagoTutor() {
  const navigate = useNavigate();
  const location = useLocation();
  
  // Datos pasados desde el modal de contratación
  const {
    tutor,
    comunidad, // puede ser null si es contratación personal
    modalidad,
    duracion,
    tarifa,
  } = location.state || {};

  const [formData, setFormData] = useState({
    cardNumber: "",
    cardName: "",
    expiryDate: "",
    cvv: "",
  });
  const [processing, setProcessing] = useState(false);
  const [errors, setErrors] = useState({});

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

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    let formattedValue = value;

    // Formatear número de tarjeta (espacios cada 4 dígitos)
    if (name === "cardNumber") {
      formattedValue = value
        .replace(/\s/g, "")
        .replace(/(\d{4})/g, "$1 ")
        .trim();
      formattedValue = formattedValue.substring(0, 19);
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

    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: "" }));
    }
  };

  const validateForm = () => {
    const newErrors = {};

    const cardNumberClean = formData.cardNumber.replace(/\s/g, "");
    if (!cardNumberClean) {
      newErrors.cardNumber = "El número de tarjeta es requerido";
    } else if (cardNumberClean.length !== 16) {
      newErrors.cardNumber = "El número de tarjeta debe tener 16 dígitos";
    }

    if (!formData.cardName.trim()) {
      newErrors.cardName = "El nombre del titular es requerido";
    }

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
      if (esContratacionComunidad) {
        // TODO: Implementar llamada al endpoint del backend para contratar tutor en comunidad
        // const response = await communitiesApi.hireTutor(comunidad.id, tutor.id, {
        //   modalidad,
        //   duracion,
        //   tarifaAcordada: parseFloat(tarifa),
        //   aceptarTerminos: true,
        // });
        // Este endpoint debería devolver una PaymentUrlResponse con la URL de Stripe
        
        await new Promise((resolve) => setTimeout(resolve, 2000));
        alert(`¡Pago procesado! Has contratado a ${nombreTutor} para ${comunidad.nombre}`);
        navigate(`/comunidades/${comunidad.id}`);
      } else {
        // TODO: Implementar llamada al endpoint del backend para contratación personal
        // Actualmente no existe este endpoint en el backend
        // Se necesitaría crear un endpoint similar pero sin comunidad
        
        await new Promise((resolve) => setTimeout(resolve, 2000));
        alert(`¡Pago procesado! Has contratado a ${nombreTutor} para uso personal`);
        navigate("/profesores");
      }
    } catch (error) {
      console.error("Error al procesar el pago:", error);
      alert("Error al procesar el pago. Por favor, intente nuevamente.");
    } finally {
      setProcessing(false);
    }
  };

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
              {tutor.usuario?.foto && (
                <img
                  src={toAbsoluteImageUrl(tutor.usuario.foto)}
                  alt={nombreTutor}
                  className="ppt-tutor-avatar"
                />
              )}
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

          {/* Columna derecha - Formulario de pago */}
          <div className="ppt-form-section">
            <div className="ppt-header">
              <h1 className="ppt-title">Información de pago</h1>
              <p className="ppt-subtitle">
                Completa los datos de tu tarjeta para confirmar la contratación
              </p>
            </div>

            <form onSubmit={handleSubmit} className="ppt-form">
              {/* Número de tarjeta */}
              <div className="ppt-form-group">
                <label htmlFor="cardNumber" className="ppt-label">
                  Número de tarjeta
                </label>
                <div className="ppt-input-wrapper">
                  <span className="ppt-card-icon">💳</span>
                  <input
                    type="text"
                    id="cardNumber"
                    name="cardNumber"
                    value={formData.cardNumber}
                    onChange={handleInputChange}
                    placeholder="1234 5678 9012 3456"
                    className={`ppt-input ${
                      errors.cardNumber ? "ppt-input--error" : ""
                    }`}
                  />
                </div>
                {errors.cardNumber && (
                  <span className="ppt-error">{errors.cardNumber}</span>
                )}
              </div>

              {/* Nombre del titular */}
              <div className="ppt-form-group">
                <label htmlFor="cardName" className="ppt-label">
                  Nombre del titular
                </label>
                <input
                  type="text"
                  id="cardName"
                  name="cardName"
                  value={formData.cardName}
                  onChange={handleInputChange}
                  placeholder="NOMBRE APELLIDO"
                  className={`ppt-input ${
                    errors.cardName ? "ppt-input--error" : ""
                  }`}
                  style={{ textTransform: "uppercase" }}
                />
                {errors.cardName && (
                  <span className="ppt-error">{errors.cardName}</span>
                )}
              </div>

              {/* Fecha de expiración y CVV */}
              <div className="ppt-form-row">
                <div className="ppt-form-group">
                  <label htmlFor="expiryDate" className="ppt-label">
                    Fecha de expiración
                  </label>
                  <input
                    type="text"
                    id="expiryDate"
                    name="expiryDate"
                    value={formData.expiryDate}
                    onChange={handleInputChange}
                    placeholder="MM/YY"
                    className={`ppt-input ${
                      errors.expiryDate ? "ppt-input--error" : ""
                    }`}
                  />
                  {errors.expiryDate && (
                    <span className="ppt-error">{errors.expiryDate}</span>
                  )}
                </div>

                <div className="ppt-form-group">
                  <label htmlFor="cvv" className="ppt-label">
                    CVV
                  </label>
                  <input
                    type="text"
                    id="cvv"
                    name="cvv"
                    value={formData.cvv}
                    onChange={handleInputChange}
                    placeholder="123"
                    className={`ppt-input ${
                      errors.cvv ? "ppt-input--error" : ""
                    }`}
                  />
                  {errors.cvv && <span className="ppt-error">{errors.cvv}</span>}
                </div>
              </div>

              {/* Información de seguridad */}
              <div className="ppt-security-info">
                <span className="ppt-security-icon">🔒</span>
                <p>
                  Tu información está protegida con encriptación SSL de 256 bits.
                  No almacenamos datos de tarjetas de crédito.
                </p>
              </div>

              {/* Términos y condiciones */}
              <div className="ppt-terms">
                <p>
                  Al confirmar el pago, aceptas los{" "}
                  <a href="/planes" target="_blank" rel="noreferrer">
                    términos y condiciones
                  </a>{" "}
                  de contratación de tutores en MeerKatters.
                </p>
              </div>

              {/* Botones de acción */}
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
                  disabled={processing}
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
          </div>
        </div>
      </div>
    </>
  );
}

import { useState } from "react";
import { crearSolicitudContratacion } from "../../api/solicitudContratacion";
import "./HireTutorModal.css";

/**
 * Modal para contratar directamente a un tutor.
 * El alumno elige día, rango horario y envía la solicitud.
 * El tutor decide si acepta o rechaza.
 */
const HireDirectModal = ({ tutor, onClose }) => {
  const [paso, setPaso] = useState(1);
  const [dia, setDia] = useState("");
  const [horaInicio, setHoraInicio] = useState("");
  const [horaFin, setHoraFin] = useState("");
  const [modalidad, setModalidad] = useState("ONLINE");
  const [mensaje, setMensaje] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const tarifaHora = tutor?.tarifaHora ?? tutor?.tarifaPorHora ?? 0;
  const nombreTutor = tutor?.usuario?.nombre || `Tutor #${tutor?.id}`;

  const calcularHoras = () => {
    if (!horaInicio || !horaFin) return 0;
    const [h1, m1] = horaInicio.split(":").map(Number);
    const [h2, m2] = horaFin.split(":").map(Number);
    const minutos = (h2 * 60 + m2) - (h1 * 60 + m1);
    return minutos > 0 ? minutos / 60 : 0;
  };

  const horas = calcularHoras();
  const importeTotal = (horas * tarifaHora).toFixed(2);

  // Fecha mínima: mañana
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  const minDate = tomorrow.toISOString().split("T")[0];

  const handleSubmit = async () => {
    setError("");

    if (!dia) {
      setError("Selecciona el día de la clase.");
      return;
    }
    if (!horaInicio || !horaFin) {
      setError("Selecciona el rango horario.");
      return;
    }
    if (horaFin <= horaInicio) {
      setError("La hora de fin debe ser posterior a la hora de inicio.");
      return;
    }
    if (horas <= 0) {
      setError("El rango horario no es válido.");
      return;
    }

    setSubmitting(true);
    try {
      await crearSolicitudContratacion(tutor.id, {
        dia,
        horaInicio,
        horaFin,
        modalidad,
        mensaje: mensaje.trim() || null,
      });
      setPaso(2);
    } catch (err) {
      const msg =
        err?.response?.data?.error ||
        err?.response?.data?.message ||
        err?.message ||
        "No se pudo enviar la solicitud.";
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="htm-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="htm-modal">
        {/* Header */}
        <div className="htm-header">
          <h2 className="htm-title">
            {paso === 1 && "Contratar a " + nombreTutor}
            {paso === 2 && "¡Solicitud enviada!"}
          </h2>
          <button className="htm-close" onClick={onClose}>✕</button>
        </div>

        {/* Paso 1: Elegir día y horario */}
        {paso === 1 && (
          <div className="htm-body">
            <div className="htm-summary-box">
              <span className="htm-summary-label">Profesor</span>
              <span className="htm-summary-value">{nombreTutor}</span>
              <span className="htm-summary-label">Tarifa por hora</span>
              <span className="htm-summary-value">{tarifaHora}€ / h</span>
            </div>

            {/* Día */}
            <div className="htm-field">
              <label className="htm-label" htmlFor="hire-dia">
                Día de la clase
              </label>
              <input
                id="hire-dia"
                className="htm-input"
                type="date"
                min={minDate}
                value={dia}
                onChange={(e) => setDia(e.target.value)}
              />
            </div>

            {/* Hora inicio */}
            <div className="htm-field">
              <label className="htm-label" htmlFor="hire-hora-inicio">
                Hora de inicio
              </label>
              <input
                id="hire-hora-inicio"
                className="htm-input"
                type="time"
                value={horaInicio}
                onChange={(e) => setHoraInicio(e.target.value)}
              />
            </div>

            {/* Hora fin */}
            <div className="htm-field">
              <label className="htm-label" htmlFor="hire-hora-fin">
                Hora de fin
              </label>
              <input
                id="hire-hora-fin"
                className="htm-input"
                type="time"
                value={horaFin}
                onChange={(e) => setHoraFin(e.target.value)}
              />
            </div>

            {/* Modalidad */}
            <div className="htm-field">
              <label className="htm-label">Modalidad</label>
              <div className="htm-radio-group">
                {[
                  { value: "ONLINE", label: "💻 Online" },
                  { value: "PRESENCIAL", label: "🏫 Presencial" },
                  { value: "HIBRIDO", label: "🔄 Híbrido" },
                ].map((opt) => (
                  <label key={opt.value} className="htm-radio">
                    <input
                      type="radio"
                      name="modalidad"
                      value={opt.value}
                      checked={modalidad === opt.value}
                      onChange={(e) => setModalidad(e.target.value)}
                    />
                    <span>{opt.label}</span>
                  </label>
                ))}
              </div>
            </div>

            {/* Mensaje opcional */}
            <div className="htm-field">
              <label className="htm-label" htmlFor="hire-mensaje">
                Mensaje para el profesor (opcional)
              </label>
              <textarea
                id="hire-mensaje"
                className="htm-input"
                rows={3}
                maxLength={500}
                placeholder="Ej: Me gustaría repasar derivadas e integrales..."
                value={mensaje}
                onChange={(e) => setMensaje(e.target.value)}
                style={{ resize: "vertical" }}
              />
            </div>

            {/* Resumen de precio */}
            {horas > 0 && (
              <div className="htm-commission-info">
                <div className="htm-commission-row">
                  <span>Duración</span>
                  <span><strong>{horas.toFixed(1)} h</strong></span>
                </div>
                <div className="htm-commission-row">
                  <span>Tarifa por hora</span>
                  <span><strong>{tarifaHora}€</strong></span>
                </div>
                <div className="htm-commission-row htm-commission-row--net">
                  <span>Importe total</span>
                  <span><strong>{importeTotal}€</strong></span>
                </div>
              </div>
            )}

            {error && <p className="htm-error">⚠️ {error}</p>}

            <div className="htm-footer">
              <button className="htm-btn htm-btn--secondary" onClick={onClose} disabled={submitting}>
                Cancelar
              </button>
              <button
                className="htm-btn htm-btn--primary"
                onClick={handleSubmit}
                disabled={submitting}
              >
                {submitting ? "Enviando..." : "Enviar solicitud"}
              </button>
            </div>
          </div>
        )}

        {/* Paso 2: Confirmación */}
        {paso === 2 && (
          <div className="htm-body htm-body--success">
            <div className="htm-success-icon">✓</div>
            <h3 className="htm-success-title">Solicitud enviada correctamente</h3>
            <p className="htm-success-text">
              Has solicitado una clase con <strong>{nombreTutor}</strong> el día{" "}
              <strong>{dia}</strong> de <strong>{horaInicio}</strong> a{" "}
              <strong>{horaFin}</strong> ({modalidad === "ONLINE" ? "Online" : modalidad === "PRESENCIAL" ? "Presencial" : "Híbrido"}) por un importe de{" "}
              <strong>{importeTotal}€</strong>.
            </p>
            <p className="htm-success-text htm-success-text--muted">
              El profesor recibirá tu solicitud y decidirá si acepta o rechaza.
              Cuando responda, recibirás una notificación. Si acepta, podrás proceder al pago.
            </p>
            <button className="htm-btn htm-btn--primary" onClick={onClose}>
              Cerrar
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default HireDirectModal;

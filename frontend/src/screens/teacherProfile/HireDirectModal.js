import { useState, useEffect, useRef } from "react";
import { crearSolicitudContratacion, getDisponibilidadTutorFecha, getHorariosOcupadosContratacion } from "../../api/solicitudContratacion";
import "./HireTutorModal.css";

/* Leaflet – mapa para ubicación presencial */
import { MapContainer, TileLayer, Marker, useMapEvents } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";

const markerIcon = L.icon({
  iconUrl: "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-red.png",
  shadowUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png",
  iconSize: [25, 41],
  iconAnchor: [12, 41],
});

/** Componente interno: click en el mapa para fijar marcador. */
const ClickMarker = ({ position, setPosition }) => {
  useMapEvents({
    click(e) {
      setPosition([e.latlng.lat, e.latlng.lng]);
    },
  });
  return position ? <Marker position={position} icon={markerIcon} /> : null;
};

/* Normaliza VIRTUAL → ONLINE */
const normMod = (m) => (m === "VIRTUAL" || m === "HIBRIDA" || m === "HIBRIDO" ? "ONLINE" : m || "ONLINE");

/**
 * Modal para contratar directamente a un tutor.
 * El alumno elige día (se muestran franjas disponibles), rango horario y envía la solicitud.
 */
const HireDirectModal = ({ tutor, onClose, initialSelection }) => {
  const [paso, setPaso] = useState(1);
  const [dia, setDia] = useState("");
  const [horaInicio, setHoraInicio] = useState("");
  const [horaFin, setHoraFin] = useState("");
  const [mensaje, setMensaje] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  // Disponibilidad y ocupación
  const [disponibilidades, setDisponibilidades] = useState([]);
  const [ocupados, setOcupados] = useState([]);
  const [loadingDisp, setLoadingDisp] = useState(false);

  // Mapa presencial
  const [mapPos, setMapPos] = useState(null); // [lat, lng]
  const [direccion, setDireccion] = useState("");
  const mapRef = useRef(null);

  const tarifaHora = tutor?.tarifaHora ?? tutor?.tarifaPorHora ?? 0;
  const nombreTutor = tutor?.usuario?.nombre || `Tutor #${tutor?.id}`;

  // Normaliza "HH:mm:ss" → "HH:mm" para comparaciones de hora
  const t5 = (s) => (s || "").substring(0, 5);

  // Modalidad derivada de la franja que contiene el horario seleccionado
  const franjaSeleccionada = disponibilidades.find(
    (d) => horaInicio && horaFin && horaInicio >= t5(d.horaInicio) && horaFin <= t5(d.horaFin)
  );
  const modalidad = franjaSeleccionada ? normMod(franjaSeleccionada.modalidad) : "ONLINE";
  const esPresencial = modalidad === "PRESENCIAL";

  // Cuando cambia el día, cargar disponibilidad y horarios ocupados
  useEffect(() => {
    if (!dia || !tutor?.id) {
      setDisponibilidades([]);
      setOcupados([]);
      return;
    }
    let cancelled = false;
    const fetchDisponibilidad = async () => {
      setLoadingDisp(true);
      setError("");
      try {
        const [dispRes, ocuRes] = await Promise.all([
          getDisponibilidadTutorFecha(tutor.id, dia),
          getHorariosOcupadosContratacion(tutor.id, dia),
        ]);
        if (!cancelled) {
          setDisponibilidades(dispRes.data || []);
          setOcupados(ocuRes.data || []);
        }
      } catch {
        if (!cancelled) {
          setDisponibilidades([]);
          setOcupados([]);
        }
      } finally {
        if (!cancelled) setLoadingDisp(false);
      }
    };
    fetchDisponibilidad();
    return () => { cancelled = true; };
  }, [dia, tutor?.id]);

  useEffect(() => {
    if (!initialSelection) return;
    if (initialSelection.dia) setDia(initialSelection.dia);
    if (initialSelection.horaInicio) setHoraInicio(initialSelection.horaInicio);
    if (initialSelection.horaFin) setHoraFin(initialSelection.horaFin);
  }, [initialSelection]);

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

  // Comprobar si el horario seleccionado cae dentro de alguna franja de disponibilidad
  const horarioDentroDeDisponibilidad = () => {
    if (!horaInicio || !horaFin || disponibilidades.length === 0) return true;
    return disponibilidades.some(d =>
      horaInicio >= t5(d.horaInicio) && horaFin <= t5(d.horaFin)
    );
  };

  // Comprobar si hay conflicto con horarios ya ocupados
  const hayConflictoOcupado = () => {
    if (!horaInicio || !horaFin || ocupados.length === 0) return false;
    return ocupados.some(o =>
      horaInicio < t5(o.horaFin) && horaFin > t5(o.horaInicio)
    );
  };

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
    if (horas > 24) {
      setError("La duración máxima permitida es de 24 horas.");
      return;
    }
    if (disponibilidades.length > 0 && !horarioDentroDeDisponibilidad()) {
      setError("El horario seleccionado no está dentro de la disponibilidad del tutor.");
      return;
    }
    if (hayConflictoOcupado()) {
      setError("El horario seleccionado se solapa con otra clase ya reservada.");
      return;
    }
    if (esPresencial && !mapPos && !direccion.trim()) {
      setError("Indica la ubicación para la clase presencial (selecciona en el mapa o escribe la dirección).");
      return;
    }

    const ubicacionClase = esPresencial
      ? (mapPos ? `${mapPos[0].toFixed(6)},${mapPos[1].toFixed(6)}` + (direccion.trim() ? ` – ${direccion.trim()}` : "") : direccion.trim())
      : null;

    setSubmitting(true);
    try {
      await crearSolicitudContratacion(tutor.id, {
        dia,
        horaInicio,
        horaFin,
        modalidad,
        mensaje: mensaje.trim() || null,
        ubicacionClase,
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
                onChange={(e) => {
                  setDia(e.target.value);
                  setHoraInicio("");
                  setHoraFin("");
                  setMapPos(null);
                  setDireccion("");
                }}
              />
            </div>

            {/* Info de disponibilidad */}
            {dia && loadingDisp && (
              <p style={{ fontSize: "0.9rem", color: "#666" }}>Cargando disponibilidad...</p>
            )}
            {dia && !loadingDisp && disponibilidades.length === 0 && (
              <p style={{ fontSize: "0.9rem", color: "#c00" }}>
                ⚠️ El tutor no tiene disponibilidad configurada para este día.
              </p>
            )}
            {dia && !loadingDisp && disponibilidades.length > 0 && (
              <div style={{ fontSize: "0.9rem", marginBottom: "8px" }}>
                <strong>Franjas disponibles:</strong>
                <ul style={{ margin: "4px 0 0 0", padding: "0 0 0 16px", listStyle: "none" }}>
                  {disponibilidades.map((d, i) => {
                    const mod = normMod(d.modalidad);
                    return (
                      <li key={i} style={{ marginBottom: 2 }}>
                        <span style={{ color: "#3a6", fontWeight: 600 }}>
                          {d.horaInicio?.slice(0, 5)} – {d.horaFin?.slice(0, 5)}
                        </span>
                        {" · "}
                        <span style={{ color: mod === "PRESENCIAL" ? "#b45309" : "#2563eb" }}>
                          {mod === "PRESENCIAL" ? "🏫 Presencial" : "💻 Online"}
                        </span>
                        {d.ubicacionPresencial && (
                          <span style={{ color: "#666", fontSize: "0.85rem" }}>
                            {" "}({d.ubicacionPresencial})
                          </span>
                        )}
                      </li>
                    );
                  })}
                </ul>
                {ocupados.length > 0 && (
                  <div style={{ color: "#c80", marginTop: "4px" }}>
                    <strong>Ya reservados:</strong>
                    {ocupados.map((o, i) => (
                      <span key={i} style={{ marginLeft: "8px" }}>
                        {o.horaInicio} – {o.horaFin}
                      </span>
                    ))}
                  </div>
                )}
              </div>
            )}

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

            {/* Modalidad — derivada de la franja, solo informativa */}
            {franjaSeleccionada && (
              <div className="htm-field">
                <label className="htm-label">Modalidad</label>
                <p style={{
                  margin: 0,
                  padding: "8px 12px",
                  borderRadius: 8,
                  background: esPresencial ? "#fef3c7" : "#dbeafe",
                  fontWeight: 600,
                  color: esPresencial ? "#92400e" : "#1e40af",
                }}>
                  {esPresencial ? "🏫 Presencial" : "💻 Online (Zoom)"}
                </p>
              </div>
            )}

            {/* Mapa de ubicación para clase presencial */}
            {esPresencial && franjaSeleccionada && (
              <div className="htm-field">
                <label className="htm-label">Ubicación de la clase</label>
                {franjaSeleccionada.ubicacionPresencial && (
                  <p style={{ fontSize: "0.85rem", color: "#666", margin: "0 0 4px" }}>
                    Referencia del tutor: <strong>{franjaSeleccionada.ubicacionPresencial}</strong>
                  </p>
                )}
                <input
                  className="htm-input"
                  type="text"
                  placeholder="Dirección o lugar de encuentro"
                  value={direccion}
                  onChange={(e) => setDireccion(e.target.value)}
                  style={{ marginBottom: 8 }}
                />
                <div style={{ height: 220, borderRadius: 8, overflow: "hidden", border: "1px solid #ddd" }}>
                  <MapContainer
                    center={[37.3891, -5.9845]}
                    zoom={13}
                    style={{ width: "100%", height: "100%" }}
                    ref={mapRef}
                  >
                    <TileLayer
                      attribution='&copy; OpenStreetMap'
                      url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                    />
                    <ClickMarker position={mapPos} setPosition={setMapPos} />
                  </MapContainer>
                </div>
                <p style={{ fontSize: "0.8rem", color: "#888", margin: "4px 0 0" }}>
                  Haz clic en el mapa para indicar el punto de encuentro.
                </p>
              </div>
            )}

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
              <strong>{horaFin}</strong> ({modalidad === "PRESENCIAL" ? "Presencial" : "Online"}) por un importe de{" "}
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

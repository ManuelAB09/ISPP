import { useCallback, useEffect, useState } from "react";
import { communitiesApi } from "../../api/communities.api";
import { useAuth } from "../../contexts/AuthContext";
import "./HireTutorModal.css";

const MODALIDADES = ["Online", "Presencial", "Híbrido"];

/**
 * Modal para contratar un tutor desde una de las comunidades del usuario.
 * 
 * FLUJO DE CONTRATACIÓN:
 * 1. Usuario selecciona comunidad y completa detalles
 * 2. Se envía solicitud de contratación al profesor
 * 3. Profesor recibe notificación y puede aceptar/rechazar
 * 4. Si acepta: Usuario recibe notificación y puede proceder al pago
 * 5. Después del pago: Contratación queda activa
 */
const HireTutorModal = ({ tutor, onClose }) => {
  const { user } = useAuth();
  const [paso, setPaso] = useState(1); // 1 = seleccionar comunidad, 2 = detalle contrato, 3 = confirmación

  // Paso 1: comunidad
  const [busqueda, setBusqueda] = useState("");
  const [comunidades, setComunidades] = useState([]);
  const [cargandoComunidades, setCargandoComunidades] = useState(false);
  const [comunidadSeleccionada, setComunidadSeleccionada] = useState(null);

  // Paso 2: detalle
  const [modalidad, setModalidad] = useState("Online");
  const [duracion, setDuracion] = useState("");
  const [tarifa, setTarifa] = useState(tutor?.tarifaHora ?? tutor?.tarifaPorHora ?? "");
  const [aceptarTerminos, setAceptarTerminos] = useState(false);

  // Estado global
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const buscarComunidades = useCallback(async (query) => {
    setCargandoComunidades(true);
    try {
      const res = await communitiesApi.listMine({ page: 0, size: 100 });
      const lista = res?.content ?? (Array.isArray(res) ? res : []);
      // Show communities where the user is admin (creator or has ADMIN role)
      const administradas = lista.filter(
        (c) => c.miRol === 'ADMIN' || c.miRol === 'ADMINISTRADOR' ||
               (c.creador?.id != null && String(c.creador.id) === String(user?.id))
      );
      // Filter by search query locally
      const filtradas = query.trim()
        ? administradas.filter(c => 
            c.nombre?.toLowerCase().includes(query.toLowerCase()) ||
            c.descripcion?.toLowerCase().includes(query.toLowerCase())
          )
        : administradas;
      setComunidades(filtradas);
    } catch {
      setComunidades([]);
    } finally {
      setCargandoComunidades(false);
    }
  }, [user?.id]);

  useEffect(() => {
    buscarComunidades("");
  }, [buscarComunidades]);

  const handleBusqueda = (e) => {
    setBusqueda(e.target.value);
    buscarComunidades(e.target.value);
  };

  const handleSubmit = async () => {
    setError("");
    if (!aceptarTerminos) {
      setError("Debes aceptar los términos para continuar.");
      return;
    }
    if (!duracion.trim()) {
      setError("Indica la duración del servicio.");
      return;
    }

    setSubmitting(true);
    try {
      // Enviar solicitud de contratación al backend
      // El profesor tendrá que aceptarla antes de que se proceda al pago
      await communitiesApi.hireTutor(comunidadSeleccionada.id, tutor.id, {
        modalidad,
        duracion: duracion.trim(),
        tarifaAcordada: parseFloat(tarifa) || 0,
        aceptarTerminos: true,
      });
      setPaso(3); // Mostrar confirmación
    } catch (err) {
      const msg = err?.response?.data?.message || err?.message || "No se pudo enviar la solicitud de contratación.";
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const nombreTutor = tutor?.usuario?.nombre || `Tutor #${tutor?.id}`;

  return (
    <div className="htm-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="htm-modal">
        {/* Header */}
        <div className="htm-header">
          <h2 className="htm-title">
            {paso === 1 && "Selecciona una comunidad"}
            {paso === 2 && "Detalles de la contratación"}
            {paso === 3 && "¡Solicitud enviada!"}
          </h2>
          <button className="htm-close" onClick={onClose}>✕</button>
        </div>

        {/* Progress */}
        {paso < 3 && (
          <div className="htm-steps">
          <div className={`htm-step ${paso >= 1 ? "htm-step--active" : ""}`}>
            <span className="htm-step__num">1</span>
            <span className="htm-step__label">Comunidad</span>
          </div>
          <div className="htm-step__line" />
          <div className={`htm-step ${paso >= 2 ? "htm-step--active" : ""}`}>
            <span className="htm-step__num">2</span>
            <span className="htm-step__label">Contrato</span>
          </div>
        </div>
        )}

        {/* ── Paso 1: Seleccionar comunidad ── */}
        {paso === 1 && (
          <div className="htm-body">
            <p className="htm-subtitle">
              Elige la comunidad que contratará a <strong>{nombreTutor}</strong>. 
              Solo puedes contratar para comunidades de las que seas administrador.
            </p>
            <div className="htm-field">
              <label className="htm-label">Buscar comunidad</label>
              <input
                className="htm-input"
                type="text"
                placeholder="Nombre de la comunidad..."
                value={busqueda}
                onChange={handleBusqueda}
              />
            </div>
            {cargandoComunidades ? (
              <p className="htm-loading">Cargando comunidades…</p>
            ) : (
              <div className="htm-community-list">
                {comunidades.length === 0 && (
                  <p className="htm-empty">
                    {cargandoComunidades
                      ? ""
                      : busqueda
                      ? "No hay resultados para esa búsqueda."
                      : "No eres administrador de ninguna comunidad. Crea una primero."}
                  </p>
                )}
                {comunidades.map((c) => (
                  <button
                    key={c.id}
                    className={`htm-community-item ${comunidadSeleccionada?.id === c.id ? "htm-community-item--selected" : ""}`}
                    onClick={() => setComunidadSeleccionada(c)}
                  >
                    <div className="htm-community-item__name">{c.nombre}</div>
                    {c.descripcion && (
                      <div className="htm-community-item__desc">{c.descripcion}</div>
                    )}
                  </button>
                ))}
              </div>
            )}
            <div className="htm-footer">
              <button className="htm-btn htm-btn--secondary" onClick={onClose}>
                Cancelar
              </button>
              <button
                className="htm-btn htm-btn--primary"
                disabled={!comunidadSeleccionada}
                onClick={() => setPaso(2)}
              >
                Continuar →
              </button>
            </div>
          </div>
        )}

        {/* ── Paso 2: Detalles del contrato ── */}
        {paso === 2 && (
          <div className="htm-body">
            <div className="htm-summary-box">
              <span className="htm-summary-label">Comunidad</span>
              <span className="htm-summary-value">{comunidadSeleccionada?.nombre}</span>
              <span className="htm-summary-label">Tutor</span>
              <span className="htm-summary-value">{nombreTutor}</span>
            </div>

            {/* Modalidad */}
            <div className="htm-field">
              <label className="htm-label">Modalidad</label>
              <div className="htm-radio-group">
                {MODALIDADES.map((m) => (
                  <label key={m} className="htm-radio">
                    <input
                      type="radio"
                      name="modalidad"
                      value={m}
                      checked={modalidad === m}
                      onChange={() => setModalidad(m)}
                    />
                    {m}
                  </label>
                ))}
              </div>
            </div>

            {/* Duración */}
            <div className="htm-field">
              <label className="htm-label" htmlFor="duracion">
                Duración
              </label>
              <input
                id="duracion"
                className="htm-input"
                type="text"
                placeholder="Ej: 1 mes, 10 horas, 4 sesiones..."
                value={duracion}
                onChange={(e) => setDuracion(e.target.value)}
              />
            </div>

            {/* Tarifa */}
            <div className="htm-field">
              <label className="htm-label" htmlFor="tarifa">
                Tarifa acordada (€/hora)
              </label>
              <input
                id="tarifa"
                className="htm-input"
                type="number"
                min="0.01"
                step="0.01"
                value={tarifa}
                onChange={(e) => setTarifa(e.target.value)}
              />
            </div>

            {/* Comisión informativa */}
            {tarifa > 0 && (
              <div className="htm-commission-info">
                <div className="htm-commission-row">
                  <span>Importe total</span>
                  <span><strong>{Number(tarifa).toFixed(2)} €</strong></span>
                </div>
                <div className="htm-commission-row htm-commission-row--muted">
                  <span>Comisión plataforma (10%)</span>
                  <span>−{(Number(tarifa) * 0.1).toFixed(2)} €</span>
                </div>
                <div className="htm-commission-row htm-commission-row--net">
                  <span>El tutor recibirá</span>
                  <span><strong>{(Number(tarifa) * 0.9).toFixed(2)} €</strong></span>
                </div>
              </div>
            )}

            {/* Términos */}
            <label className="htm-terms">
              <input
                type="checkbox"
                checked={aceptarTerminos}
                onChange={(e) => setAceptarTerminos(e.target.checked)}
              />
              Acepto los <a href="/planes" target="_blank" rel="noreferrer">términos y condiciones</a> de contratación de tutores en MeerKat.
            </label>

            {error && <p className="htm-error">⚠️ {error}</p>}

            <div className="htm-footer">
              <button
                className="htm-btn htm-btn--secondary"
                onClick={() => { setPaso(1); setError(""); }}
                disabled={submitting}
              >
                ← Atrás
              </button>
              <button
                className="htm-btn htm-btn--primary"
                onClick={handleSubmit}
                disabled={submitting || !aceptarTerminos}
              >
                {submitting ? "Enviando solicitud..." : "Enviar solicitud"}
              </button>
            </div>
          </div>
        )}

        {/* ── Paso 3: Confirmación ── */}
        {paso === 3 && (
          <div className="htm-body htm-body--success">
            <div className="htm-success-icon">✓</div>
            <h3 className="htm-success-title">Solicitud de contratación enviada</h3>
            <p className="htm-success-text">
              La solicitud para contratar a <strong>{nombreTutor}</strong> en la comunidad{" "}
              <strong>{comunidadSeleccionada?.nombre}</strong> ha sido enviada correctamente.
            </p>
            <p className="htm-success-text htm-success-text--muted">
              El profesor recibirá una notificación y podrá aceptar o rechazar tu solicitud.
              Una vez que el profesor acepte, recibirás una notificación para proceder con el pago.
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

export default HireTutorModal;

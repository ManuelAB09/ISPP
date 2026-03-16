import { useEffect, useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { getDisponibilidadFecha, crearReserva } from '../../api/reservas.api';
import './BookClassModal.css';

const MODALIDADES = ['VIRTUAL', 'PRESENCIAL'];
const DURACIONES = [30, 45, 60, 90, 120];

const formatFecha = (date) => {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
};

const hoyStr = () => formatFecha(new Date());

const BookClassModal = ({ tutor, onClose }) => {
  const { user } = useAuth();
  const [paso, setPaso] = useState(1);

  // Paso 1: fecha y slot
  const [fecha, setFecha] = useState(hoyStr());
  const [slots, setSlots] = useState([]);
  const [cargandoSlots, setCargandoSlots] = useState(false);
  const [slotSeleccionado, setSlotSeleccionado] = useState(null);
  const [duracion, setDuracion] = useState(60);

  // Paso 2: detalles
  const [modalidad, setModalidad] = useState('VIRTUAL');
  const [tema, setTema] = useState('');
  const [descripcion, setDescripcion] = useState('');
  const [ubicacion, setUbicacion] = useState('');

  // Estado global
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const tarifa = tutor?.tarifaHora ?? tutor?.tarifaPorHora ?? 0;
  const precioTotal = tarifa > 0 ? ((tarifa / 60) * duracion).toFixed(2) : null;

  // Cargar slots disponibles al cambiar fecha
  useEffect(() => {
    if (!fecha || !tutor?.id) return;
    setCargandoSlots(true);
    setSlotSeleccionado(null);
    setSlots([]);
    getDisponibilidadFecha(tutor.id, fecha)
      .then((data) => {
        const lista = Array.isArray(data) ? data : (data?.slots ?? []);
        setSlots(lista);
      })
      .catch(() => setSlots([]))
      .finally(() => setCargandoSlots(false));
  }, [fecha, tutor?.id]);

  const handleReservar = async () => {
    setError('');
    if (!tema.trim()) {
      setError('Indica el tema de la clase.');
      return;
    }
    if (modalidad === 'PRESENCIAL' && !ubicacion.trim()) {
      setError('Indica la ubicación para la clase presencial.');
      return;
    }

    setSubmitting(true);
    try {
      const body = {
        fechaHora: `${fecha}T${slotSeleccionado}:00`,
        duracionMinutos: duracion,
        modalidad,
        tema: tema.trim(),
        descripcion: descripcion.trim(),
        ubicacion: modalidad === 'PRESENCIAL' ? ubicacion.trim() : null,
      };
      const resp = await crearReserva(tutor.id, body);
      const url = resp?.paymentUrl;
      if (url) {
        window.location.href = url;
      } else {
        setPaso(3);
      }
    } catch (err) {
      setError(err?.message || 'No se pudo crear la reserva. Inténtalo de nuevo.');
    } finally {
      setSubmitting(false);
    }
  };

  const nombreTutor = tutor?.usuario?.nombre || `Tutor #${tutor?.id}`;

  return (
    <div className="bcm-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="bcm-modal">

        {/* Header */}
        <div className="bcm-header">
          <h2 className="bcm-title">
            {paso === 1 && 'Elige fecha y hora'}
            {paso === 2 && 'Detalles de la clase'}
            {paso === 3 && '¡Reserva completada!'}
          </h2>
          <button className="bcm-close" onClick={onClose}>✕</button>
        </div>

        {/* Stepper */}
        {paso < 3 && (
          <div className="bcm-steps">
            <div className={`bcm-step ${paso >= 1 ? 'bcm-step--active' : ''}`}>
              <span className="bcm-step__num">1</span>
              <span className="bcm-step__label">Horario</span>
            </div>
            <div className="bcm-step__line" />
            <div className={`bcm-step ${paso >= 2 ? 'bcm-step--active' : ''}`}>
              <span className="bcm-step__num">2</span>
              <span className="bcm-step__label">Detalles</span>
            </div>
          </div>
        )}

        {/* ── Paso 1: Fecha y slot ── */}
        {paso === 1 && (
          <div className="bcm-body">
            <p className="bcm-subtitle">
              Selecciona una fecha y hora disponible con <strong>{nombreTutor}</strong>.
            </p>

            <div className="bcm-field">
              <label className="bcm-label">Fecha</label>
              <input
                className="bcm-input"
                type="date"
                min={hoyStr()}
                value={fecha}
                onChange={(e) => setFecha(e.target.value)}
              />
            </div>

            <div className="bcm-field">
              <label className="bcm-label">Duración</label>
              <div className="bcm-duration-group">
                {DURACIONES.map((d) => (
                  <button
                    key={d}
                    type="button"
                    className={`bcm-duration-btn ${duracion === d ? 'bcm-duration-btn--active' : ''}`}
                    onClick={() => setDuracion(d)}
                  >
                    {d} min
                  </button>
                ))}
              </div>
            </div>

            {precioTotal && (
              <div className="bcm-price-hint">
                Precio estimado: <strong>{precioTotal} €</strong>
                <span className="bcm-price-hint__sub"> ({tarifa} €/hora)</span>
              </div>
            )}

            <div className="bcm-field">
              <label className="bcm-label">Hora disponible</label>
              {cargandoSlots ? (
                <p className="bcm-loading">Cargando disponibilidad…</p>
              ) : slots.length === 0 ? (
                <p className="bcm-empty">No hay horarios disponibles para este día. Prueba otra fecha.</p>
              ) : (
                <div className="bcm-slots">
                  {slots.map((s) => (
                    <button
                      key={s}
                      type="button"
                      className={`bcm-slot ${slotSeleccionado === s ? 'bcm-slot--active' : ''}`}
                      onClick={() => setSlotSeleccionado(s)}
                    >
                      {s}
                    </button>
                  ))}
                </div>
              )}
            </div>

            <div className="bcm-footer">
              <button className="bcm-btn bcm-btn--secondary" onClick={onClose}>
                Cancelar
              </button>
              <button
                className="bcm-btn bcm-btn--primary"
                disabled={!slotSeleccionado}
                onClick={() => setPaso(2)}
              >
                Continuar →
              </button>
            </div>
          </div>
        )}

        {/* ── Paso 2: Detalles ── */}
        {paso === 2 && (
          <div className="bcm-body">
            <div className="bcm-summary-box">
              <span className="bcm-summary-label">Tutor</span>
              <span className="bcm-summary-value">{nombreTutor}</span>
              <span className="bcm-summary-label">Fecha y hora</span>
              <span className="bcm-summary-value">{fecha} a las {slotSeleccionado}</span>
              <span className="bcm-summary-label">Duración</span>
              <span className="bcm-summary-value">{duracion} minutos{precioTotal ? ` · ${precioTotal} €` : ''}</span>
            </div>

            <div className="bcm-field">
              <label className="bcm-label">Modalidad</label>
              <div className="bcm-radio-group">
                {MODALIDADES.map((m) => (
                  <label key={m} className="bcm-radio">
                    <input
                      type="radio"
                      name="modalidad"
                      value={m}
                      checked={modalidad === m}
                      onChange={() => setModalidad(m)}
                    />
                    {m === 'VIRTUAL' ? 'Online' : 'Presencial'}
                  </label>
                ))}
              </div>
            </div>

            {modalidad === 'PRESENCIAL' && (
              <div className="bcm-field">
                <label className="bcm-label" htmlFor="ubicacion">Ubicación</label>
                <input
                  id="ubicacion"
                  className="bcm-input"
                  type="text"
                  placeholder="Dirección o lugar de la clase"
                  value={ubicacion}
                  onChange={(e) => setUbicacion(e.target.value)}
                />
              </div>
            )}

            <div className="bcm-field">
              <label className="bcm-label" htmlFor="tema">
                Tema de la clase <span className="bcm-required">*</span>
              </label>
              <input
                id="tema"
                className="bcm-input"
                type="text"
                placeholder="Ej: Derivadas e integrales, Programación en Python…"
                value={tema}
                onChange={(e) => setTema(e.target.value)}
              />
            </div>

            <div className="bcm-field">
              <label className="bcm-label" htmlFor="descripcion">Descripción (opcional)</label>
              <textarea
                id="descripcion"
                className="bcm-textarea"
                rows={3}
                placeholder="Cuéntale al tutor qué necesitas trabajar exactamente…"
                value={descripcion}
                onChange={(e) => setDescripcion(e.target.value)}
              />
            </div>

            {error && <p className="bcm-error">⚠️ {error}</p>}

            <div className="bcm-footer">
              <button
                className="bcm-btn bcm-btn--secondary"
                onClick={() => { setPaso(1); setError(''); }}
                disabled={submitting}
              >
                ← Atrás
              </button>
              <button
                className="bcm-btn bcm-btn--primary"
                onClick={handleReservar}
                disabled={submitting}
              >
                {submitting ? 'Procesando…' : `Reservar y pagar${precioTotal ? ` (${precioTotal} €)` : ''}`}
              </button>
            </div>
          </div>
        )}

        {/* ── Paso 3: Confirmación (sin URL de pago) ── */}
        {paso === 3 && (
          <div className="bcm-body bcm-body--success">
            <div className="bcm-success-icon">✓</div>
            <h3 className="bcm-success-title">Reserva creada</h3>
            <p className="bcm-success-text">
              Tu clase con <strong>{nombreTutor}</strong> el <strong>{fecha}</strong> a las{' '}
              <strong>{slotSeleccionado}</strong> ha sido registrada.
            </p>
            <p className="bcm-success-text bcm-success-text--muted">
              El tutor recibirá una notificación y confirmará la reserva.
              Puedes ver tus reservas en <em>Mis Reservas</em>.
            </p>
            <button className="bcm-btn bcm-btn--primary" onClick={onClose}>
              Cerrar
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default BookClassModal;

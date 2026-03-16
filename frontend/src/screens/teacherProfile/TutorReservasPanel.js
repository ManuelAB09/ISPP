import { useEffect, useState } from 'react';
import {
  getMisReservasTutor,
  confirmarReserva,
  cancelarReserva,
} from '../../api/reservas.api';
import './TutorReservasPanel.css';

const ESTADO_LABEL = {
  PENDIENTE: { label: 'Pendiente', cls: 'trp-badge--pending' },
  CONFIRMADA: { label: 'Confirmada', cls: 'trp-badge--confirmed' },
  COMPLETADA: { label: 'Completada', cls: 'trp-badge--done' },
  CANCELADA_ALUMNO: { label: 'Cancelada (alumno)', cls: 'trp-badge--cancelled' },
  CANCELADA_TUTOR: { label: 'Cancelada (tú)', cls: 'trp-badge--cancelled' },
  NO_ASISTIDA: { label: 'No asistida', cls: 'trp-badge--absent' },
};

const formatDT = (dt) => {
  if (!dt) return '—';
  const d = new Date(dt);
  return d.toLocaleString('es-ES', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
};

const TutorReservasPanel = () => {
  const [reservas, setReservas] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [accionando, setAccionando] = useState(null); // reservaId en proceso
  const [motivoCancelacion, setMotivoCancelacion] = useState('');
  const [cancelando, setCancelando] = useState(null); // reservaId para confirmar cancel

  const cargar = async (p = 0) => {
    setCargando(true);
    try {
      const data = await getMisReservasTutor(p, 10);
      setReservas(data?.content ?? []);
      setTotalPages(data?.totalPages ?? 0);
      setPage(p);
    } catch {
      setReservas([]);
    } finally {
      setCargando(false);
    }
  };

  useEffect(() => { cargar(0); }, []);

  const handleConfirmar = async (id) => {
    setAccionando(id);
    try {
      await confirmarReserva(id);
      await cargar(page);
    } catch (e) {
      alert(e?.message || 'Error al confirmar');
    } finally {
      setAccionando(null);
    }
  };

  const handleCancelar = async () => {
    if (!cancelando) return;
    setAccionando(cancelando);
    try {
      await cancelarReserva(cancelando, motivoCancelacion);
      setCancelando(null);
      setMotivoCancelacion('');
      await cargar(page);
    } catch (e) {
      alert(e?.message || 'Error al cancelar');
    } finally {
      setAccionando(null);
    }
  };

  if (cargando) return <div className="trp-loading">Cargando reservas…</div>;

  return (
    <section className="trp-panel">
      <div className="trp-panel__header">
        <h2 className="trp-panel__title">Mis reservas de clase</h2>
      </div>

      {reservas.length === 0 ? (
        <p className="trp-empty">No tienes reservas todavía.</p>
      ) : (
        <div className="trp-list">
          {reservas.map((r) => {
            const estadoInfo = ESTADO_LABEL[r.estado] ?? { label: r.estado, cls: '' };
            const esPendiente = r.estado === 'PENDIENTE';
            const puedeCancelar = r.puedeSerCancelada &&
              (r.estado === 'PENDIENTE' || r.estado === 'CONFIRMADA');

            return (
              <div key={r.id} className="trp-card">
                <div className="trp-card__top">
                  <div className="trp-card__info">
                    <span className="trp-card__alumno">{r.alumnoNombre}</span>
                    <span className="trp-card__fecha">{formatDT(r.fechaHora)}</span>
                    <span className="trp-card__tema">{r.tema}</span>
                    <span className="trp-card__detalle">
                      {r.duracionMinutos} min · {r.modalidad === 'VIRTUAL' ? 'Online' : 'Presencial'}
                      {r.tarifa ? ` · ${r.tarifa} €/h` : ''}
                    </span>
                  </div>
                  <div className="trp-card__right">
                    <span className={`trp-badge ${estadoInfo.cls}`}>{estadoInfo.label}</span>
                  </div>
                </div>

                {r.descripcion && (
                  <p className="trp-card__desc">{r.descripcion}</p>
                )}

                {(esPendiente || puedeCancelar) && (
                  <div className="trp-card__actions">
                    {esPendiente && (
                      <button
                        className="trp-btn trp-btn--confirm"
                        disabled={accionando === r.id}
                        onClick={() => handleConfirmar(r.id)}
                      >
                        {accionando === r.id ? 'Confirmando…' : '✓ Confirmar'}
                      </button>
                    )}
                    {puedeCancelar && (
                      <button
                        className="trp-btn trp-btn--cancel"
                        disabled={accionando === r.id}
                        onClick={() => { setCancelando(r.id); setMotivoCancelacion(''); }}
                      >
                        Cancelar
                      </button>
                    )}
                  </div>
                )}

                {r.calificacion != null && (
                  <div className="trp-card__rating">
                    {'★'.repeat(r.calificacion)}{'☆'.repeat(5 - r.calificacion)}
                    {r.comentarioAlumno && (
                      <span className="trp-card__comment"> "{r.comentarioAlumno}"</span>
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* Paginación */}
      {totalPages > 1 && (
        <div className="trp-pagination">
          <button
            className="trp-page-btn"
            disabled={page === 0}
            onClick={() => cargar(page - 1)}
          >← Anterior</button>
          <span className="trp-page-info">{page + 1} / {totalPages}</span>
          <button
            className="trp-page-btn"
            disabled={page >= totalPages - 1}
            onClick={() => cargar(page + 1)}
          >Siguiente →</button>
        </div>
      )}

      {/* Modal confirmación cancelación */}
      {cancelando && (
        <div className="trp-overlay" onClick={(e) => e.target === e.currentTarget && setCancelando(null)}>
          <div className="trp-confirm-modal">
            <h3 className="trp-confirm-modal__title">Cancelar reserva</h3>
            <p className="trp-confirm-modal__text">
              ¿Seguro que quieres cancelar esta reserva? Se notificará al alumno.
            </p>
            <div className="trp-field">
              <label className="trp-label">Motivo (opcional)</label>
              <textarea
                className="trp-textarea"
                rows={3}
                placeholder="Indica el motivo de la cancelación…"
                value={motivoCancelacion}
                onChange={(e) => setMotivoCancelacion(e.target.value)}
              />
            </div>
            <div className="trp-confirm-modal__actions">
              <button
                className="trp-btn trp-btn--secondary"
                onClick={() => setCancelando(null)}
              >
                Volver
              </button>
              <button
                className="trp-btn trp-btn--cancel"
                disabled={accionando === cancelando}
                onClick={handleCancelar}
              >
                {accionando === cancelando ? 'Cancelando…' : 'Sí, cancelar'}
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
};

export default TutorReservasPanel;

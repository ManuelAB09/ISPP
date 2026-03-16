import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../../components/Header/Header';
import {
  getMisReservasAlumno,
  cancelarReserva,
  calificarReserva,
} from '../../api/reservas.api';
import './MisReservas.css';

const ESTADO_LABEL = {
  PENDIENTE: { label: 'Pendiente', cls: 'mr-badge--pending' },
  CONFIRMADA: { label: 'Confirmada', cls: 'mr-badge--confirmed' },
  COMPLETADA: { label: 'Completada', cls: 'mr-badge--done' },
  CANCELADA_ALUMNO: { label: 'Cancelada por ti', cls: 'mr-badge--cancelled' },
  CANCELADA_TUTOR: { label: 'Cancelada por el tutor', cls: 'mr-badge--cancelled' },
  NO_ASISTIDA: { label: 'No asistida', cls: 'mr-badge--absent' },
};

const formatDT = (dt) => {
  if (!dt) return '—';
  const d = new Date(dt);
  return d.toLocaleString('es-ES', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
};

const MisReservas = () => {
  const navigate = useNavigate();
  const [reservas, setReservas] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // Cancelar
  const [cancelando, setCancelando] = useState(null);
  const [motivo, setMotivo] = useState('');
  const [accionando, setAccionando] = useState(null);

  // Calificar
  const [calificando, setCalificando] = useState(null);
  const [estrellas, setEstrellas] = useState(5);
  const [comentario, setComentario] = useState('');

  const cargar = async (p = 0) => {
    setCargando(true);
    try {
      const data = await getMisReservasAlumno(p, 10);
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

  const handleCancelar = async () => {
    if (!cancelando) return;
    setAccionando(cancelando);
    try {
      await cancelarReserva(cancelando, motivo);
      setCancelando(null);
      setMotivo('');
      await cargar(page);
    } catch (e) {
      alert(e?.message || 'No se pudo cancelar la reserva.');
    } finally {
      setAccionando(null);
    }
  };

  const handleCalificar = async () => {
    if (!calificando) return;
    setAccionando(calificando);
    try {
      await calificarReserva(calificando, estrellas, comentario);
      setCalificando(null);
      setComentario('');
      setEstrellas(5);
      await cargar(page);
    } catch (e) {
      alert(e?.message || 'No se pudo enviar la valoración.');
    } finally {
      setAccionando(null);
    }
  };

  return (
    <>
      <Header page="mis-reservas" />
      <div className="mr-page">
        <div className="mr-container">
          <div className="mr-top">
            <h1 className="mr-title">Mis reservas</h1>
          </div>

          {cargando ? (
            <p className="mr-loading">Cargando reservas…</p>
          ) : reservas.length === 0 ? (
            <div className="mr-empty">
              <p className="mr-empty__icon">📅</p>
              <p className="mr-empty__text">Todavía no tienes reservas.</p>
              <button
                className="mr-btn mr-btn--primary"
                onClick={() => navigate('/profesores')}
              >
                Buscar profesores
              </button>
            </div>
          ) : (
            <>
              <div className="mr-list">
                {reservas.map((r) => {
                  const estadoInfo = ESTADO_LABEL[r.estado] ?? { label: r.estado, cls: '' };
                  const puedeCancelar = r.puedeSerCancelada &&
                    (r.estado === 'PENDIENTE' || r.estado === 'CONFIRMADA');
                  const puedeCalificar = (r.estado === 'COMPLETADA' || r.estado === 'CONFIRMADA') &&
                    r.calificacion == null;

                  return (
                    <div key={r.id} className="mr-card">
                      <div className="mr-card__top">
                        <div className="mr-card__info">
                          <span className="mr-card__tutor">
                            Con <strong>{r.tutorNombre}</strong>
                          </span>
                          <span className="mr-card__fecha">{formatDT(r.fechaHora)}</span>
                          <span className="mr-card__tema">{r.tema}</span>
                          <span className="mr-card__detalle">
                            {r.duracionMinutos} min ·{' '}
                            {r.modalidad === 'VIRTUAL' ? 'Online' : 'Presencial'}
                            {r.tarifa ? ` · ${r.tarifa} €/h` : ''}
                          </span>
                        </div>
                        <span className={`mr-badge ${estadoInfo.cls}`}>
                          {estadoInfo.label}
                        </span>
                      </div>

                      {r.motivoCancelacion && (
                        <p className="mr-card__cancel-reason">
                          Motivo: {r.motivoCancelacion}
                        </p>
                      )}

                      {r.estado === 'CONFIRMADA' && r.modalidad === 'VIRTUAL' && r.enlaceVirtual && (
                        <a
                          className="mr-link-virtual"
                          href={r.enlaceVirtual}
                          target="_blank"
                          rel="noreferrer"
                        >
                          🔗 Unirse a la clase
                        </a>
                      )}

                      {r.calificacion != null && (
                        <div className="mr-card__rating">
                          {'★'.repeat(r.calificacion)}{'☆'.repeat(5 - r.calificacion)}
                          {r.comentarioAlumno && (
                            <span className="mr-card__comment"> "{r.comentarioAlumno}"</span>
                          )}
                        </div>
                      )}

                      <div className="mr-card__actions">
                        {puedeCancelar && (
                          <button
                            className="mr-btn mr-btn--cancel"
                            onClick={() => { setCancelando(r.id); setMotivo(''); }}
                          >
                            Cancelar reserva
                          </button>
                        )}
                        {puedeCalificar && (
                          <button
                            className="mr-btn mr-btn--rate"
                            onClick={() => { setCalificando(r.id); setEstrellas(5); setComentario(''); }}
                          >
                            ★ Valorar clase
                          </button>
                        )}
                        <button
                          className="mr-btn mr-btn--secondary"
                          onClick={() => navigate(`/profesores/${r.tutorId}`)}
                        >
                          Ver perfil del tutor
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>

              {totalPages > 1 && (
                <div className="mr-pagination">
                  <button
                    className="mr-page-btn"
                    disabled={page === 0}
                    onClick={() => cargar(page - 1)}
                  >← Anterior</button>
                  <span className="mr-page-info">{page + 1} / {totalPages}</span>
                  <button
                    className="mr-page-btn"
                    disabled={page >= totalPages - 1}
                    onClick={() => cargar(page + 1)}
                  >Siguiente →</button>
                </div>
              )}
            </>
          )}
        </div>
      </div>

      {/* Modal cancelar */}
      {cancelando && (
        <div className="mr-overlay" onClick={(e) => e.target === e.currentTarget && setCancelando(null)}>
          <div className="mr-dialog">
            <h3 className="mr-dialog__title">Cancelar reserva</h3>
            <p className="mr-dialog__text">
              ¿Seguro que quieres cancelar esta reserva?
              Recuerda que debes hacerlo con al menos 24 horas de antelación.
            </p>
            <div className="mr-field">
              <label className="mr-label">Motivo (opcional)</label>
              <textarea
                className="mr-textarea"
                rows={3}
                placeholder="¿Por qué cancelas?"
                value={motivo}
                onChange={(e) => setMotivo(e.target.value)}
              />
            </div>
            <div className="mr-dialog__actions">
              <button
                className="mr-btn mr-btn--secondary"
                onClick={() => setCancelando(null)}
              >Volver</button>
              <button
                className="mr-btn mr-btn--cancel"
                disabled={accionando === cancelando}
                onClick={handleCancelar}
              >
                {accionando === cancelando ? 'Cancelando…' : 'Sí, cancelar'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal calificar */}
      {calificando && (
        <div className="mr-overlay" onClick={(e) => e.target === e.currentTarget && setCalificando(null)}>
          <div className="mr-dialog">
            <h3 className="mr-dialog__title">Valorar clase</h3>
            <div className="mr-stars">
              {[1, 2, 3, 4, 5].map((n) => (
                <button
                  key={n}
                  type="button"
                  className={`mr-star-btn ${n <= estrellas ? 'mr-star-btn--active' : ''}`}
                  onClick={() => setEstrellas(n)}
                >
                  ★
                </button>
              ))}
            </div>
            <div className="mr-field">
              <label className="mr-label">Comentario (opcional)</label>
              <textarea
                className="mr-textarea"
                rows={3}
                placeholder="¿Cómo fue la clase?"
                value={comentario}
                onChange={(e) => setComentario(e.target.value)}
              />
            </div>
            <div className="mr-dialog__actions">
              <button
                className="mr-btn mr-btn--secondary"
                onClick={() => setCalificando(null)}
              >Cancelar</button>
              <button
                className="mr-btn mr-btn--primary"
                disabled={accionando === calificando}
                onClick={handleCalificar}
              >
                {accionando === calificando ? 'Enviando…' : 'Enviar valoración'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default MisReservas;

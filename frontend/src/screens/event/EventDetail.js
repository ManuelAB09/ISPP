import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  LuCalendar, LuMapPin, LuLink, LuUsers, LuUser,
  LuPencil, LuX, LuArrowLeft, LuPackage,
  LuEye, LuEyeOff, LuMap, LuClock, LuCheck
} from 'react-icons/lu';
import './EventDetail.css';
import Header from '../../components/Header/Header';
import {
  getEventById, cancelEvent, attendEvent, cancelAttendance,
  getConfirmedAttendees, getMyAttendance
} from '../../api/eventEndpoints';
import { communitiesApi } from '../../api/communities.api';

const EventDetail = () => {
  const { eventId } = useParams();
  const navigate = useNavigate();

  const [event, setEvent] = useState(null);
  const [attendees, setAttendees] = useState([]);
  const [myAttendance, setMyAttendance] = useState(null);
  const [loading, setLoading] = useState(true);
  const [attendanceLoading, setAttendanceLoading] = useState(false);
  const [error, setError] = useState(null);
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [cancelReason, setCancelReason] = useState('');
  const [cancelLoading, setCancelLoading] = useState(false);
  const [isMember, setIsMember] = useState(false);

  const currentUserId = localStorage.getItem('userId');

  const fetchEventData = useCallback(async () => {
    try {
      setLoading(true);
      const [eventData, attendeesData] = await Promise.all([
        getEventById(eventId),
        getConfirmedAttendees(eventId).catch(() => [])
      ]);
      setEvent(eventData);
      setAttendees(Array.isArray(attendeesData) ? attendeesData : (attendeesData?.content || []));

      // Verificar si soy miembro de la comunidad del evento
      if (currentUserId && eventData.comunidadId) {
        try {
          await communitiesApi.getMyMembership(eventData.comunidadId);
          setIsMember(true);
        } catch {
          setIsMember(false);
        }
      }

      // Verificar mi asistencia
      if (currentUserId) {
        try {
          const myAtt = await getMyAttendance(eventId);
          setMyAttendance(myAtt);
        } catch {
          setMyAttendance(null);
        }
      }
    } catch (err) {
      console.error('Error al cargar el evento:', err);
      setError('No se pudo cargar el evento.');
    } finally {
      setLoading(false);
    }
  }, [eventId, currentUserId]);

  useEffect(() => {
    fetchEventData();
  }, [fetchEventData]);

  const isOrganizer = event?.creador?.id?.toString() === currentUserId;
  const isConfirmed = myAttendance?.estado === 'CONFIRMADA';
  const isFull = event && event.aforo && (event.asistentesConfirmados || 0) >= event.aforo;
  const isCancelled = event?.cancelado;

  const handleAttend = async () => {
    try {
      setAttendanceLoading(true);
      await attendEvent(eventId);
      await fetchEventData();
    } catch (err) {
      console.error('Error al confirmar asistencia:', err);
      setError(err.response?.data?.message || 'Error al confirmar asistencia.');
    } finally {
      setAttendanceLoading(false);
    }
  };

  const handleCancelAttendance = async () => {
    try {
      setAttendanceLoading(true);
      await cancelAttendance(eventId);
      setMyAttendance(null);
      await fetchEventData();
    } catch (err) {
      console.error('Error al cancelar asistencia:', err);
      setError(err.response?.data?.message || 'Error al cancelar asistencia.');
    } finally {
      setAttendanceLoading(false);
    }
  };

  const handleCancelEvent = async () => {
    try {
      setCancelLoading(true);
      await cancelEvent(eventId, cancelReason);
      setShowCancelModal(false);
      await fetchEventData();
    } catch (err) {
      console.error('Error al cancelar evento:', err);
      setError(err.response?.data?.message || 'Error al cancelar el evento.');
    } finally {
      setCancelLoading(false);
    }
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleDateString('es-ES', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  const formatTime = (dateStr) => {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleTimeString('es-ES', {
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (loading) {
    return (
      <div className="ed-page">
        <Header page={'eventos'} />
        <div className="ed-container">
          <p className="ed-loading">Cargando evento...</p>
        </div>
      </div>
    );
  }

  if (error && !event) {
    return (
      <div className="ed-page">
        <Header page={'eventos'} />
        <div className="ed-container">
          <div className="ed-error">{error}</div>
          <button className="ed-back-btn" onClick={() => navigate(-1)}>
            <LuArrowLeft /> Volver
          </button>
        </div>
      </div>
    );
  }

  if (!event) return null;

  return (
    <div className="ed-page">
      <Header page={'eventos'} />

      <div className="ed-container">
        {/* Botón volver */}
        <button className="ed-back-btn" onClick={() => navigate(-1)}>
          <LuArrowLeft /> Volver
        </button>

        {/* Cabecera del evento */}
        <div className="ed-header">
          <div className="ed-header-info">
            <div className="ed-badges">
              <span className={`ed-badge ${event.esVirtual ? 'ed-badge-online' : 'ed-badge-presencial'}`}>
                {event.esVirtual ? '🌐 Online' : '📍 Presencial'}
              </span>
              <span className={`ed-badge ${event.privado ? 'ed-badge-private' : 'ed-badge-public'}`}>
                {event.privado ? <><LuEyeOff /> Privado</> : <><LuEye /> Público</>}
              </span>
              {isCancelled && (
                <span className="ed-badge ed-badge-cancelled">❌ Cancelado</span>
              )}
              {event.visibleMapa && !isCancelled && (
                <span className="ed-badge ed-badge-map"><LuMap /> Visible en mapa</span>
              )}
            </div>
            <h1 className="ed-title">{event.titulo}</h1>
            {event.creador && (
              <p className="ed-organizer">
                Organizado por <strong>{event.creador.nombre || event.creador.username || 'Usuario'}</strong>
              </p>
            )}
          </div>

          {/* Acciones del organizador */}
          {isOrganizer && !isCancelled && (
            <div className="ed-organizer-actions">
              <button
                className="ed-btn ed-btn-edit"
                onClick={() => navigate(`/create-event/${eventId}`)}
              >
                <LuPencil /> Editar evento
              </button>
              <button
                className="ed-btn ed-btn-cancel-event"
                onClick={() => setShowCancelModal(true)}
              >
                <LuX /> Cancelar evento
              </button>
            </div>
          )}
        </div>

        {error && <div className="ed-error">{error}</div>}

        {/* Información principal del evento */}
        <div className="ed-content">
          <div className="ed-main">
            {/* Descripción */}
            {event.descripcion && (
              <div className="ed-section">
                <h2 className="ed-section-title">Descripción</h2>
                <p className="ed-description">{event.descripcion}</p>
              </div>
            )}

            {/* Detalles */}
            <div className="ed-section">
              <h2 className="ed-section-title">Detalles del evento</h2>
              <div className="ed-details-grid">
                <div className="ed-detail-item">
                  <LuCalendar className="ed-detail-icon" />
                  <div>
                    <span className="ed-detail-label">Fecha</span>
                    <span className="ed-detail-value">{formatDate(event.fechaHora)}</span>
                  </div>
                </div>

                <div className="ed-detail-item">
                  <LuClock className="ed-detail-icon" />
                  <div>
                    <span className="ed-detail-label">Hora</span>
                    <span className="ed-detail-value">
                      {formatTime(event.fechaHora)}
                      {event.fechaFin && ` — ${formatTime(event.fechaFin)}`}
                    </span>
                  </div>
                </div>

                {event.fechaFin && formatDate(event.fechaHora) !== formatDate(event.fechaFin) && (
                  <div className="ed-detail-item">
                    <LuCalendar className="ed-detail-icon" />
                    <div>
                      <span className="ed-detail-label">Fecha de fin</span>
                      <span className="ed-detail-value">{formatDate(event.fechaFin)}</span>
                    </div>
                  </div>
                )}

                <div className="ed-detail-item">
                  {event.esVirtual ? <LuLink className="ed-detail-icon" /> : <LuMapPin className="ed-detail-icon" />}
                  <div>
                    <span className="ed-detail-label">
                      {event.esVirtual ? 'Enlace virtual' : 'Ubicación'}
                    </span>
                    <span className="ed-detail-value">
                      {event.esVirtual
                        ? (event.enlaceVirtual || 'Por confirmar')
                        : (event.ubicacion?.nombre || event.ubicacion || 'Por confirmar')}
                    </span>
                  </div>
                </div>

                <div className="ed-detail-item">
                  <LuUsers className="ed-detail-icon" />
                  <div>
                    <span className="ed-detail-label">Aforo</span>
                    <span className="ed-detail-value">
                      {event.asistentesConfirmados || 0} / {event.aforo || '∞'} participantes
                    </span>
                  </div>
                </div>
              </div>
            </div>

            {/* Materiales necesarios */}
            {event.queLlevar && (
              <div className="ed-section">
                <h2 className="ed-section-title">
                  <LuPackage className="ed-section-icon" /> Materiales necesarios
                </h2>
                <div className="ed-materials">
                  {event.queLlevar.split(',').map((material, index) => (
                    <span key={index} className="ed-material-tag">
                      {material.trim()}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {/* Motivo de cancelación */}
            {isCancelled && event.motivoCancelacion && (
              <div className="ed-section ed-cancelled-section">
                <h2 className="ed-section-title">Motivo de cancelación</h2>
                <p className="ed-cancel-reason">{event.motivoCancelacion}</p>
              </div>
            )}
          </div>

          {/* Barra lateral: Asistencia y participantes */}
          <div className="ed-sidebar">
            {/* Acción de asistencia */}
            {!isCancelled && (
              <div className="ed-attendance-card">
                <h3 className="ed-card-title">Asistencia</h3>

                <div className="ed-capacity-bar-container">
                  <div className="ed-capacity-bar">
                    <div
                      className="ed-capacity-fill"
                      style={{ width: `${event.aforo ? Math.min(((event.asistentesConfirmados || 0) / event.aforo) * 100, 100) : 0}%` }}
                    ></div>
                  </div>
                  <span className="ed-capacity-text">
                    {event.asistentesConfirmados || 0} / {event.aforo || '∞'}
                  </span>
                </div>

                {isFull && !isConfirmed && (
                  <div className="ed-full-message">
                    <LuUsers /> Aforo completo
                  </div>
                )}

                {!isMember && currentUserId && !isConfirmed && (
                  <div className="ed-full-message">
                    Debes ser miembro de la comunidad para apuntarte
                  </div>
                )}

                {isConfirmed ? (
                  <div className="ed-attendance-actions">
                    <div className="ed-confirmed-badge">
                      <LuCheck /> Asistencia confirmada
                    </div>
                    <button
                      className="ed-btn ed-btn-cancel-attendance"
                      onClick={handleCancelAttendance}
                      disabled={attendanceLoading}
                    >
                      {attendanceLoading ? 'Cancelando...' : 'Cancelar asistencia'}
                    </button>
                  </div>
                ) : (
                  <button
                    className="ed-btn ed-btn-attend"
                    onClick={handleAttend}
                    disabled={attendanceLoading || isFull || !currentUserId || !isMember}
                    title={!currentUserId ? 'Inicia sesión para confirmar asistencia' : !isMember ? 'Debes ser miembro de la comunidad' : ''}
                  >
                    {attendanceLoading ? 'Confirmando...' : 'Confirmar asistencia'}
                  </button>
                )}

                {!currentUserId && (
                  <p className="ed-login-hint">
                    <a href="/login">Inicia sesión</a> para confirmar asistencia
                  </p>
                )}
              </div>
            )}

            {/* Lista de participantes */}
            <div className="ed-participants-card">
              <h3 className="ed-card-title">
                <LuUsers /> Participantes ({attendees.length})
              </h3>
              {attendees.length > 0 ? (
                <ul className="ed-participants-list">
                  {attendees.map((att) => {
                    const user = att.usuario || att;
                    return (
                      <li key={att.id || user.id} className="ed-participant">
                        <div className="ed-participant-avatar">
                          {user.fotoPerfil || user.avatar ? (
                            <img src={user.fotoPerfil || user.avatar} alt={user.nombre || user.username} />
                          ) : (
                            <LuUser />
                          )}
                        </div>
                        <span className="ed-participant-name">
                          {user.nombre || user.username || 'Usuario'}
                        </span>
                      </li>
                    );
                  })}
                </ul>
              ) : (
                <p className="ed-no-participants">Aún no hay participantes confirmados.</p>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Modal de cancelación */}
      {showCancelModal && (
        <div className="ed-modal-overlay" onClick={() => setShowCancelModal(false)}>
          <div className="ed-modal" onClick={(e) => e.stopPropagation()}>
            <h2 className="ed-modal-title">¿Cancelar evento?</h2>
            <p className="ed-modal-text">
              Esta acción no se puede deshacer. Todos los asistentes serán notificados de la cancelación.
            </p>
            <div className="ed-modal-field">
              <label className="ed-modal-label">Motivo de cancelación (opcional)</label>
              <textarea
                className="ed-modal-textarea"
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
                placeholder="Explica por qué se cancela el evento..."
                rows={3}
                maxLength={500}
              />
            </div>
            <div className="ed-modal-actions">
              <button
                className="ed-btn ed-btn-secondary"
                onClick={() => setShowCancelModal(false)}
                disabled={cancelLoading}
              >
                Volver
              </button>
              <button
                className="ed-btn ed-btn-danger"
                onClick={handleCancelEvent}
                disabled={cancelLoading}
              >
                {cancelLoading ? 'Cancelando...' : 'Confirmar cancelación'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default EventDetail;

import React from 'react';
import { useNavigate } from 'react-router-dom';
import { LuUser, LuClock, LuMonitor, LuWifi, LuMapPin, LuCalendar } from 'react-icons/lu';
import './TarjetaEvento.css';

const TarjetaEvento = ({ event, onAttend, onCancelAttendance, attendanceLoading }) => {
  const navigate = useNavigate();

  // Soportar ambos formatos de datos: API real y datos mock
  const isOnline = event.esVirtual || event.type === 'online';
  const title = event.titulo || event.title || '';
  const description = event.descripcion || event.description || '';
  const eventId = event.id;
  const isCancelled = event.cancelado;
  const isConfirmed = event.miAsistencia === 'CONFIRMADA' || event.status === 'enrolled';
  const aforo = event.aforo;
  const asistentes = event.asistentesConfirmados || 0;
  const isFull = aforo && asistentes >= aforo;

  const Icon = isOnline ? LuWifi : LuMonitor;

  // Formatear fecha
  const formatDate = (dateStr) => {
    if (!dateStr) return event.date || '';
    const date = new Date(dateStr);
    return date.toLocaleDateString('es-ES', {
      day: 'numeric',
      month: 'short',
      year: 'numeric'
    });
  };

  const formatTime = (dateStr) => {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' });
  };

  const dateDisplay = formatDate(event.fechaHora);
  const timeDisplay = formatTime(event.fechaHora);

  // Ubicación
  const location = isOnline
    ? 'Online'
    : (event.ubicacion?.nombre || event.ubicacion || '');

  return (
    <div className={`event-list-card ${isCancelled ? 'event-cancelled' : ''}`}>
      <div className="event-main-info">
        <div className={`event-icon-large ${isOnline ? 'online-icon' : 'presencial-icon'}`}>
          <Icon />
        </div>
        <div className="event-details">
          <div className="event-date-row">
            <span className="event-date">
              <LuCalendar style={{ marginRight: '4px', verticalAlign: 'middle' }} />
              {dateDisplay}
              {timeDisplay && ` · ${timeDisplay}`}
            </span>
            <span className={`badge badge-${isOnline ? 'online' : 'presencial'}`}>
              {isOnline ? 'Online' : 'Presencial'}
            </span>
            {isCancelled && <span className="badge badge-cancelled">Cancelado</span>}
          </div>
          <h3 className="event-title">{title}</h3>
          <p className="event-desc">{description}</p>
          {location && !isOnline && (
            <p className="event-location">
              <LuMapPin style={{ marginRight: '4px', verticalAlign: 'middle' }} />
              {location}
            </p>
          )}
        </div>
      </div>

      <div className="event-stats-col">
        <div className="event-stat-item">
          <LuUser className="stat-icon" /> {asistentes}{aforo ? ` / ${aforo}` : ''}
        </div>
        {timeDisplay && (
          <div className="event-stat-item">
            <LuClock className="stat-icon" /> {timeDisplay}
          </div>
        )}
      </div>

      <div className="event-actions-col">
        {isCancelled ? (
          <span className="badge-status badge-cancelled-status">Cancelado</span>
        ) : isConfirmed ? (
          <>
            <span className="badge-status badge-online">Inscrito</span>
            {onCancelAttendance && (
              <button
                className="btn-link"
                onClick={(e) => { e.stopPropagation(); onCancelAttendance(eventId); }}
                disabled={attendanceLoading}
              >
                Cancelar asistencia
              </button>
            )}
          </>
        ) : isFull ? (
          <span className="badge-status badge-full">Aforo completo</span>
        ) : (
          <>
            {onAttend && (
              <button
                className="btn btn-primary btn-sm"
                onClick={(e) => { e.stopPropagation(); onAttend(eventId); }}
                disabled={attendanceLoading}
              >
                Apuntarse
              </button>
            )}
          </>
        )}
        <button
          className="btn-link"
          onClick={() => navigate(`/eventos/${eventId}`)}
        >
          Ver detalles
        </button>
      </div>
    </div>
  );
};

export default TarjetaEvento;

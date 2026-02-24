import React from 'react';
import { LuUser, LuFileText, LuClock, LuMonitor, LuWifi, LuDownload } from 'react-icons/lu';
import './EventCard.css';

const EventCard = ({ event }) => {
  const isOnline = event.type === 'online';
  const Icon = isOnline ? LuWifi : LuMonitor;

  return (
    <div className="event-list-card">
      
      <div className="event-main-info">
        <div className={`event-icon-large ${isOnline ? 'online-icon' : 'presencial-icon'}`}>
          <Icon />
        </div>
        <div className="event-details">
          <div className="event-date-row">
            <span className="event-date">{event.date}</span>
            <span className={`badge badge-${isOnline ? 'online' : 'presencial'}`}>
              {isOnline ? 'Online' : 'Presencial'}
            </span>
          </div>
          <h3 className="event-title">{event.title}</h3>
          <p className="event-desc">{event.description}</p>
        </div>
      </div>

      <div className="event-stats-col">
        <div className="event-stat-item">
          <LuUser className="stat-icon" /> {event.users}
        </div>
        <div className="event-stat-item">
          <LuFileText className="stat-icon" /> {event.files}
        </div>
        <div className="event-stat-item">
          <LuClock className="stat-icon" /> {event.duration}
        </div>
      </div>

      <div className="event-actions-col">
        {event.status === 'enrolled' ? (
          <>
            <span className="badge-status badge-online">Inscrito</span>
            <button className="btn-link">
              Descargar Apuntes <LuDownload />
            </button>
          </>
        ) : (
          <>
            <button className="btn btn-primary btn-sm">Apuntarse</button>
            <button className="btn-link">Ver detalles</button>
          </>
        )}
      </div>

    </div>
  );
};

export default EventCard;
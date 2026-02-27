import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { LuPlus, LuArrowLeft, LuCalendar, LuUsers } from 'react-icons/lu';
import Header from '../../components/Header/Header';
import EventCard from '../../components/EventCard';
import { communitiesApi } from '../../api/communities.api';
import { listCommunityEvents, attendEvent, cancelAttendance } from '../../api/eventEndpoints';
import './CommunityDetail.css';

export default function CommunityDetail() {
  const { communityId } = useParams();
  const navigate = useNavigate();

  const [community, setCommunity] = useState(null);
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [eventsLoading, setEventsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [attendanceLoading, setAttendanceLoading] = useState(false);
  const [filterCancelled, setFilterCancelled] = useState(false);

  const currentUserId = localStorage.getItem('userId');

  const fetchCommunity = useCallback(async () => {
    try {
      setLoading(true);
      const data = await communitiesApi.getById(communityId);
      setCommunity(data);
    } catch (err) {
      console.error('Error al cargar la comunidad:', err);
      setError('No se pudo cargar la comunidad.');
    } finally {
      setLoading(false);
    }
  }, [communityId]);

  const fetchEvents = useCallback(async () => {
    try {
      setEventsLoading(true);
      const data = await listCommunityEvents(communityId, { cancelados: filterCancelled });
      // Soportar tanto array como objeto paginado
      const eventList = Array.isArray(data) ? data : (data?.content || []);
      setEvents(eventList);
    } catch (err) {
      console.error('Error al cargar eventos:', err);
      // Si falla el endpoint de comunidad, intentar no mostrar error fatal
      setEvents([]);
    } finally {
      setEventsLoading(false);
    }
  }, [communityId, filterCancelled]);

  useEffect(() => {
    fetchCommunity();
    fetchEvents();
  }, [fetchCommunity, fetchEvents]);

  const handleAttend = async (eventId) => {
    if (!currentUserId) {
      navigate('/login');
      return;
    }
    try {
      setAttendanceLoading(true);
      await attendEvent(eventId);
      await fetchEvents();
    } catch (err) {
      console.error('Error al confirmar asistencia:', err);
    } finally {
      setAttendanceLoading(false);
    }
  };

  const handleCancelAttendance = async (eventId) => {
    try {
      setAttendanceLoading(true);
      await cancelAttendance(eventId);
      await fetchEvents();
    } catch (err) {
      console.error('Error al cancelar asistencia:', err);
    } finally {
      setAttendanceLoading(false);
    }
  };

  if (loading) {
    return (
      <>
        <Header page={'comunidades'} />
        <div className="cd-container">
          <p className="cd-loading">Cargando comunidad...</p>
        </div>
      </>
    );
  }

  if (error && !community) {
    return (
      <>
        <Header page={'comunidades'} />
        <div className="cd-container">
          <div className="cd-error">{error}</div>
          <button className="cd-back-btn" onClick={() => navigate('/comunidades')}>
            <LuArrowLeft /> Volver a comunidades
          </button>
        </div>
      </>
    );
  }

  return (
    <>
      <Header page={'comunidades'} />
      <div className="cd-container">
        <button className="cd-back-btn" onClick={() => navigate('/comunidades')}>
          <LuArrowLeft /> Volver a comunidades
        </button>

        {/* Cabecera de la comunidad */}
        {community && (
          <div className="cd-header">
            <div className="cd-header-image">
              <img
                src={community.imagen || 'https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&w=400&q=80'}
                alt={community.nombre}
              />
            </div>
            <div className="cd-header-info">
              <h1 className="cd-title">{community.nombre}</h1>
              {community.descripcion && (
                <p className="cd-description">{community.descripcion}</p>
              )}
              <div className="cd-meta">
                {community.categoria && community.categoria.map(cat => (
                  <span key={cat} className="cd-tag">{cat}</span>
                ))}
                <span className="cd-members">
                  <LuUsers /> {community.miembrosActuales || 0} miembros
                </span>
              </div>
            </div>
          </div>
        )}

        {/* Sección de eventos */}
        <div className="cd-events-section">
          <div className="cd-events-header">
            <h2 className="cd-events-title">
              <LuCalendar /> Eventos
            </h2>
            <div className="cd-events-actions">
              <label className="cd-filter-label">
                <input
                  type="checkbox"
                  checked={filterCancelled}
                  onChange={(e) => setFilterCancelled(e.target.checked)}
                />
                Mostrar cancelados
              </label>
              <button
                className="cd-btn cd-btn-create"
                onClick={() => navigate(`/create-event/new?communityId=${communityId}`)}
              >
                <LuPlus /> Crear evento
              </button>
            </div>
          </div>

          {eventsLoading ? (
            <p className="cd-loading">Cargando eventos...</p>
          ) : events.length > 0 ? (
            <div className="cd-events-list">
              {events.map(event => (
                <EventCard
                  key={event.id}
                  event={event}
                  onAttend={currentUserId ? handleAttend : null}
                  onCancelAttendance={currentUserId ? handleCancelAttendance : null}
                  attendanceLoading={attendanceLoading}
                />
              ))}
            </div>
          ) : (
            <div className="cd-empty-events">
              <LuCalendar className="cd-empty-icon" />
              <h3>No hay eventos</h3>
              <p>Sé el primero en crear un evento para esta comunidad.</p>
              <button
                className="cd-btn cd-btn-create"
                onClick={() => navigate(`/create-event/new?communityId=${communityId}`)}
              >
                <LuPlus /> Crear evento
              </button>
            </div>
          )}
        </div>
      </div>
    </>
  );
}

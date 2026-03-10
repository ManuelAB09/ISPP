import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { LuPlus, LuArrowLeft, LuCalendar, LuUsers, LuLogIn, LuLogOut } from 'react-icons/lu';
import Header from '../../components/Header/Header';
import TarjetaEvento from '../../components/Evento/TarjetaEvento';
import CommunityChat from '../chat/CommunityChat';
import GoogleClassroomButton from '../../components/GoogleClassroomButton/GoogleClassroomButton';
import { communitiesApi } from '../../api/communities.api';
import { listCommunityEvents, attendEvent, cancelAttendance, getMyAttendance } from '../../api/eventEndpoints';
import { useAuth } from '../../contexts/AuthContext';
import './CommunityDetail.css';

export default function CommunityDetail() {
  const { communityId } = useParams();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { user } = useAuth();

  const [community, setCommunity] = useState(null);
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [eventsLoading, setEventsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [attendanceLoading, setAttendanceLoading] = useState(false);
  const [filterCancelled, setFilterCancelled] = useState(false);
  const [isMember, setIsMember] = useState(false);
  const [joinLoading, setJoinLoading] = useState(false);
  const [membershipError, setMembershipError] = useState(null);

  const currentUserId = localStorage.getItem('userId');
  const openChatOnLoad = searchParams.get('chat') === 'open';
  const currentUser = {
    id: Number(currentUserId),
    nombre: user?.nombre || 'Usuario',
    foto: user?.foto || null,
    fotoBackgroundColor: user?.fotoBackgroundColor || '#ffffff',
  };
  const communityImage = community?.imagenUrl !== 'empty' ? community?.imagenUrl : 'https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&w=400&q=80';

  const fetchCommunity = useCallback(async () => {
    try {
      setLoading(true);
      const data = await communitiesApi.getById(communityId);
      setCommunity(data);
      // Use esMiembro from backend response if available
      if (data.esMiembro !== undefined) {
        setIsMember(data.esMiembro);
      } else if (currentUserId) {
        // Fallback: check membership via API
        try {
          await communitiesApi.getMyMembership(communityId);
          setIsMember(true);
        } catch {
          setIsMember(false);
        }
      }
    } catch (err) {
      console.error('Error al cargar la comunidad:', err);
      setError('No se pudo cargar la comunidad.');
    } finally {
      setLoading(false);
    }
  }, [communityId, currentUserId]);

  const fetchEvents = useCallback(async () => {
    try {
      setEventsLoading(true);
      const data = await listCommunityEvents(communityId, { cancelados: filterCancelled });
      // Soportar tanto array como objeto paginado
      let eventList = Array.isArray(data) ? data : (data?.content || []);

      // Enriquecer cada evento con el estado de asistencia del usuario actual
      if (currentUserId && eventList.length > 0) {
        const enriched = await Promise.all(
          eventList.map(async (event) => {
            try {
              const att = await getMyAttendance(event.id);
              return { ...event, miAsistencia: att?.estado || null };
            } catch {
              return { ...event, miAsistencia: null };
            }
          })
        );
        eventList = enriched;
      }

      setEvents(eventList);
    } catch (err) {
      console.error('Error al cargar eventos:', err);
      setEvents([]);
    } finally {
      setEventsLoading(false);
    }
  }, [communityId, filterCancelled, currentUserId]);

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
      // Actualizar optimistamente el estado local
      setEvents(prev => prev.map(ev =>
        ev.id === eventId
          ? { ...ev, miAsistencia: 'CONFIRMADA', asistentesConfirmados: (ev.asistentesConfirmados || 0) + 1 }
          : ev
      ));
      // Refrescar desde el servidor
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
      // Actualizar optimistamente el estado local
      setEvents(prev => prev.map(ev =>
        ev.id === eventId
          ? { ...ev, miAsistencia: null, asistentesConfirmados: Math.max((ev.asistentesConfirmados || 1) - 1, 0) }
          : ev
      ));
      // Refrescar desde el servidor
      await fetchEvents();
    } catch (err) {
      console.error('Error al cancelar asistencia:', err);
    } finally {
      setAttendanceLoading(false);
    }
  };

  const handleJoinCommunity = async () => {
    if (!currentUserId) {
      navigate('/login');
      return;
    }
    try {
      setJoinLoading(true);
      setMembershipError(null);
      await communitiesApi.join(communityId);
      setIsMember(true);
      await fetchCommunity(); // Refresh member count
      await fetchEvents(); // Refresh events to show private events
    } catch (err) {
      console.error('Error al unirse a la comunidad:', err);
      if (err.status === 401 || err.message?.includes('401')) {
        navigate('/login');
      } else if (err.status === 409 || err.message?.includes('409')) {
        setIsMember(true); // Already a member
      } else {
        setMembershipError(err.message || 'Error al unirse a la comunidad');
      }
    } finally {
      setJoinLoading(false);
    }
  };

  const handleLeaveCommunity = async () => {
    try {
      setJoinLoading(true);
      setMembershipError(null);
      await communitiesApi.leave(communityId);
      setIsMember(false);
      await fetchCommunity(); // Refresh member count
      await fetchEvents(); // Refresh events to hide private events
    } catch (err) {
      console.error('Error al abandonar la comunidad:', err);
      const status = err.status || err.response?.status;
      if (status === 400) {
        setMembershipError('No puedes abandonar siendo el único admin. Transfiere la administración primero.');
      } else {
        setMembershipError(err.message || 'Error al abandonar la comunidad');
      }
    } finally {
      setJoinLoading(false);
    }
  };

  if (loading) {
    return (
      <>
        <Header page={'comunidades'} user={user} />
        <div className="cd-container">
          <p className="cd-loading">Cargando comunidad...</p>
        </div>
      </>
    );
  }

  if (error && !community) {
    return (
      <>
        <Header page={'comunidades'} user={user} />
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
      <Header page={'comunidades'} user={user} />
      <div className="cd-container">
        <button className="cd-back-btn" onClick={() => navigate('/comunidades')}>
          <LuArrowLeft /> Volver a comunidades
        </button>

        {/* Cabecera de la comunidad */}
        {community && (
          <div className="cd-header">
            <div className="cd-header-image">
              <img
                src={communityImage}
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
              {/* Join / Leave community */}
              <div className="cd-membership-actions">
                {membershipError && (
                  <span className="cd-membership-error">{membershipError}</span>
                )}
                {currentUserId ? (
                  isMember ? (
                    <button
                      className="cd-btn cd-btn-leave"
                      onClick={handleLeaveCommunity}
                      disabled={joinLoading}
                    >
                      <LuLogOut /> {joinLoading ? 'Saliendo...' : 'Abandonar comunidad'}
                    </button>
                  ) : (
                    <button
                      className="cd-btn cd-btn-join"
                      onClick={handleJoinCommunity}
                      disabled={joinLoading}
                    >
                      <LuLogIn /> {joinLoading ? 'Uniéndose...' : 'Unirse a la comunidad'}
                    </button>
                  )
                ) : (
                  <button
                    className="cd-btn cd-btn-join"
                    onClick={() => navigate('/login')}
                  >
                    <LuLogIn /> Inicia sesión para unirte
                  </button>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Sección de Google Classroom */}
        {community && (isMember || community.classroom) && (
          <GoogleClassroomButton
            communityId={Number(communityId)}
            linkedCourse={community.classroom}
            isAdmin={community.miRol === 'ADMIN'}
            onLinked={fetchCommunity}
          />
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
              {isMember ? (
                <button
                  className="cd-btn cd-btn-create"
                  onClick={() => navigate(`/crear-evento/new?communityId=${communityId}`)}
                >
                  <LuPlus /> Crear evento
                </button>
              ) : (
                <span className="cd-member-hint">Únete a la comunidad para crear eventos</span>
              )}
            </div>
          </div>

          {eventsLoading ? (
            <p className="cd-loading">Cargando eventos...</p>
          ) : events.length > 0 ? (
            <div className="cd-events-list">
              {events.map(event => (
                <TarjetaEvento
                  key={event.id}
                  event={event}
                  onAttend={currentUserId && isMember ? handleAttend : null}
                  onCancelAttendance={currentUserId ? handleCancelAttendance : null}
                  attendanceLoading={attendanceLoading}
                />
              ))}
            </div>
          ) : (
            <div className="cd-empty-events">
              <LuCalendar className="cd-empty-icon" />
              <h3>No hay eventos</h3>
              {isMember ? (
                <>
                  <p>Sé el primero en crear un evento para esta comunidad.</p>
                  <button
                    className="cd-btn cd-btn-create"
                    onClick={() => navigate(`/crear-evento/new?communityId=${communityId}`)}
                  >
                    <LuPlus /> Crear evento
                  </button>
                </>
              ) : (
                <p>Únete a la comunidad para poder crear eventos.</p>
              )}
            </div>
          )}
        </div>

        {currentUserId && isMember && user ? (
          <CommunityChat
            comunidadId={Number(communityId)}
            usuarioActual={currentUser}
            comunidadNombre={community?.nombre}
            comunidadImagen={communityImage}
            initiallyOpen={openChatOnLoad}
          />
        ) : null}
      </div>
    </>
  );
}

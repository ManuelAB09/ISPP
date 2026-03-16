import { useCallback, useEffect, useState } from 'react';
import { LuArrowLeft, LuCalendar, LuCheck, LuLogIn, LuLogOut, LuPencil, LuPlus, LuTrash2, LuUserPlus, LuUsers, LuX } from 'react-icons/lu';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { communitiesApi } from '../../api/communities.api';
import { attendEvent, cancelAttendance, getMyAttendance, listCommunityEvents } from '../../api/eventEndpoints';
import EditCommunityModal from '../../components/Comunidad/EditCommunityModal';
import InviteMemberModal from '../../components/Comunidad/InviteMemberModal';
import TransferAdminModal from '../../components/Comunidad/TransferAdminModal';
import TarjetaEvento from '../../components/Evento/TarjetaEvento';
import GoogleClassroomButton from '../../components/GoogleClassroomButton/GoogleClassroomButton';
import Header from '../../components/Header/Header';
import { useAuth } from '../../contexts/AuthContext';
import CommunityChat from '../chat/CommunityChat';
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
  const [requestSent, setRequestSent] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [pendingRequests, setPendingRequests] = useState([]);
  const [requestsLoading, setRequestsLoading] = useState(false);
  const [respondingId, setRespondingId] = useState(null);
  const [showTransferModal, setShowTransferModal] = useState(false);
  const [showInviteModal, setShowInviteModal] = useState(false);

  const isPrivate = community?.tipoGrupo === 'GRUPO_PRIVADO';
  const isAdmin = community?.miRol === 'ADMIN';

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

      // Si es comunidad privada y el usuario no es miembro, comprobar solicitud pendiente
      if (data.tipoGrupo === 'GRUPO_PRIVADO' && !data.esMiembro && currentUserId) {
        try {
          const status = await communitiesApi.getMyRequestStatus(communityId);
          if (status && status.pending) {
            setRequestSent(true);
          }
        } catch {
          // Ignorar error, el usuario simplemente puede solicitar
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

  const fetchPendingRequests = useCallback(async () => {
    try {
      setRequestsLoading(true);
      const data = await communitiesApi.listRequests(communityId, { estado: 'PENDIENTE' });
      const list = data?.content || data?.solicitudes || [];
      setPendingRequests(Array.isArray(list) ? list : []);
    } catch (err) {
      console.error('Error al cargar solicitudes:', err);
      setPendingRequests([]);
    } finally {
      setRequestsLoading(false);
    }
  }, [communityId]);

  useEffect(() => {
    fetchCommunity();
    fetchEvents();
  }, [fetchCommunity, fetchEvents]);

  // Cargar solicitudes pendientes cuando el admin accede a una comunidad privada
  useEffect(() => {
    if (isAdmin && isPrivate) {
      fetchPendingRequests();
    }
  }, [isAdmin, isPrivate, fetchPendingRequests]);

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
      if (isPrivate) {
        await communitiesApi.requestAccess(communityId);
        setRequestSent(true);
      } else {
        await communitiesApi.join(communityId);
        setIsMember(true);
        await fetchCommunity();
        await fetchEvents();
      }
    } catch (err) {
      console.error('Error al unirse/solicitar acceso:', err);
      if (err.status === 401 || err.message?.includes('401')) {
        navigate('/login');
      } else if (err.status === 409 || err.message?.includes('409')) {
        setIsMember(true);
      } else if (err.status === 400) {
        if (isPrivate) {
          setRequestSent(true);
        } else {
          setMembershipError(err.message || 'Error al unirse a la comunidad');
        }
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
      // Si el admin era el único miembro, el backend elimina la comunidad
      // En ese caso redirigimos a la lista
      if (isAdmin) {
        navigate('/comunidades');
        return;
      }
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

  const handleDeleteCommunity = async () => {
    if (!window.confirm('¿Estás seguro de que quieres eliminar esta comunidad? Se expulsará a todos los miembros y no se podrá deshacer.')) {
      return;
    }
    try {
      setDeleteLoading(true);
      setMembershipError(null);
      await communitiesApi.deleteCommunity(communityId);
      navigate('/comunidades');
    } catch (err) {
      console.error('Error al eliminar la comunidad:', err);
      setMembershipError(err.message || 'Error al eliminar la comunidad');
    } finally {
      setDeleteLoading(false);
    }
  };

  const handleRespondRequest = async (requestId, aceptado) => {
    try {
      setRespondingId(requestId);
      await communitiesApi.respondToRequest(communityId, requestId, aceptado);
      setPendingRequests(prev => prev.filter(r => r.id !== requestId));
      if (aceptado) {
        await fetchCommunity();
      }
    } catch (err) {
      console.error('Error al responder solicitud:', err);
    } finally {
      setRespondingId(null);
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
              {/* Join / Leave / Request access */}
              <div className="cd-membership-actions">
                {membershipError && (
                  <span className="cd-membership-error">{membershipError}</span>
                )}
                {isAdmin && (
                  <button
                    className="cd-btn cd-btn-edit"
                    onClick={() => setShowEditModal(true)}
                  >
                    <LuPencil /> Editar comunidad
                  </button>
                )}
                {isAdmin && (
                  <button
                    className="cd-btn cd-btn-invite"
                    onClick={() => setShowInviteModal(true)}
                  >
                    <LuUserPlus /> Invitar por email
                  </button>
                )}
                {isAdmin && (
                  <button
                    className="cd-btn cd-btn-transfer"
                    onClick={() => setShowTransferModal(true)}
                  >
                    <LuUsers /> Transferir administración
                  </button>
                )}
                {isAdmin && (
                  <button
                    className="cd-btn cd-btn-delete"
                    onClick={handleDeleteCommunity}
                    disabled={deleteLoading}
                  >
                    <LuTrash2 /> {deleteLoading ? 'Eliminando...' : 'Eliminar comunidad'}
                  </button>
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
                  ) : requestSent ? (
                    <button className="cd-btn cd-btn-pending" disabled>
                      Solicitud de acceso enviada
                    </button>
                  ) : (
                    <button
                      className="cd-btn cd-btn-join"
                      onClick={handleJoinCommunity}
                      disabled={joinLoading}
                    >
                      <LuLogIn /> {joinLoading
                        ? (isPrivate ? 'Solicitando...' : 'Uniendose...')
                        : (isPrivate ? 'Solicitar acceso' : 'Unirse a la comunidad')}
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

        {/* Seccion de solicitudes pendientes - solo admin de comunidad privada */}
        {isAdmin && isPrivate && (
          <div className="cd-requests-section">
            <h2 className="cd-requests-title">
              <LuUserPlus /> Solicitudes de acceso
              {pendingRequests.length > 0 && (
                <span className="cd-requests-badge">{pendingRequests.length}</span>
              )}
            </h2>
            {requestsLoading ? (
              <p className="cd-loading">Cargando solicitudes...</p>
            ) : pendingRequests.length > 0 ? (
              <div className="cd-requests-list">
                {pendingRequests.map(req => (
                  <div key={req.id} className="cd-request-item">
                    <div className="cd-request-info">
                      <span className="cd-request-name">
                        {req.solicitante?.nombre || 'Usuario'}
                      </span>
                      {req.mensaje && (
                        <span className="cd-request-message">{req.mensaje}</span>
                      )}
                      {req.fechaSolicitud && (
                        <span className="cd-request-date">
                          {new Date(req.fechaSolicitud).toLocaleDateString('es-ES', {
                            day: 'numeric', month: 'short', year: 'numeric'
                          })}
                        </span>
                      )}
                    </div>
                    <div className="cd-request-actions">
                      <button
                        className="cd-btn cd-btn-accept"
                        onClick={() => handleRespondRequest(req.id, true)}
                        disabled={respondingId === req.id}
                      >
                        <LuCheck /> Aceptar
                      </button>
                      <button
                        className="cd-btn cd-btn-reject"
                        onClick={() => handleRespondRequest(req.id, false)}
                        disabled={respondingId === req.id}
                      >
                        <LuX /> Rechazar
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="cd-requests-empty">No hay solicitudes pendientes.</p>
            )}
          </div>
        )}

        {/* Seccion de Google Classroom - si hay vinculacion o es admin */}
        {community && (community.classroom || isAdmin) && (
          <GoogleClassroomButton
            communityId={Number(communityId)}
            linkedCourse={community.classroom}
            isAdmin={isAdmin}
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

        {showEditModal && community && (
          <EditCommunityModal
            community={community}
            onClose={() => setShowEditModal(false)}
            onSaved={() => {
              setShowEditModal(false);
              fetchCommunity();
            }}
          />
        )}

        {showTransferModal && (
          <TransferAdminModal
            communityId={communityId}
            currentUserId={currentUserId}
            onClose={() => setShowTransferModal(false)}
            onTransferred={() => {
              setShowTransferModal(false);
              fetchCommunity();
            }}
          />
        )}

        {showInviteModal && (
          <InviteMemberModal
            communityId={communityId}
            onClose={() => setShowInviteModal(false)}
            onInvited={() => {
              // Refresca estado por si backend ajusta contadores relacionados
              fetchCommunity();
            }}
          />
        )}
      </div>
    </>
  );
}

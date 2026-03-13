import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { LuPlus, LuArrowLeft, LuCalendar, LuUsers, LuLogIn, LuLogOut, LuVideo, LuPlay, LuPencil, LuTrash2, LuCheck, LuX, LuUserPlus } from 'react-icons/lu';
import Header from '../../components/Header/Header';
import TarjetaEvento from '../../components/Evento/TarjetaEvento';
import CommunityChat from '../chat/CommunityChat';
import GoogleClassroomButton from '../../components/GoogleClassroomButton/GoogleClassroomButton';
import EditCommunityModal from '../../components/Comunidad/EditCommunityModal';
import TransferAdminModal from '../../components/Comunidad/TransferAdminModal';
import { communitiesApi } from '../../api/communities.api';
import { ZoomApi } from '../../api/zoom.api';
import { listCommunityEvents, attendEvent, cancelAttendance, getMyAttendance } from '../../api/eventEndpoints';
import { useAuth } from '../../contexts/AuthContext';
import './CommunityDetail.css';

const formatDuration = (milliseconds) => {
  const totalSeconds = Math.max(0, Math.floor(milliseconds / 1000));
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;

  if (hours > 0) {
    return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
  }

  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
};

const formatDateTime = (value) => {
  if (!value) {
    return 'Sin fecha';
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return 'Sin fecha';
  }

  return parsed.toLocaleString('es-ES', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

const formatFileSize = (bytes) => {
  const size = Number(bytes);
  if (!Number.isFinite(size) || size <= 0) {
    return 'Tamano desconocido';
  }
  if (size < 1024 * 1024) {
    return `${Math.round(size / 1024)} KB`;
  }
  return `${(size / (1024 * 1024)).toFixed(1)} MB`;
};

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
  const [activeMeeting, setActiveMeeting] = useState(null);
  const [meetingLoading, setMeetingLoading] = useState(false);
  const [meetingError, setMeetingError] = useState(null);
  const [meetingNow, setMeetingNow] = useState(Date.now());
  const [showMeetingForm, setShowMeetingForm] = useState(false);
  const [meetingTopic, setMeetingTopic] = useState('Reunion de la comunidad');
  const [meetingDurationForm, setMeetingDurationForm] = useState(60);
  const [participantsOpen, setParticipantsOpen] = useState(false);
  const [participantsLoading, setParticipantsLoading] = useState(false);
  const [participantsError, setParticipantsError] = useState(null);
  const [activeParticipants, setActiveParticipants] = useState([]);
  const [meetingsOpen, setMeetingsOpen] = useState(false);
  const [meetingsLoading, setMeetingsLoading] = useState(false);
  const [meetingsError, setMeetingsError] = useState(null);
  const [meetingHistory, setMeetingHistory] = useState([]);
  const [recordingsOpen, setRecordingsOpen] = useState(false);
  const [recordingsLoading, setRecordingsLoading] = useState(false);
  const [recordingsError, setRecordingsError] = useState(null);
  const [recordings, setRecordings] = useState([]);
  const [selectedRecordingMeetingId, setSelectedRecordingMeetingId] = useState(null);
  const [downloadingRecordingId, setDownloadingRecordingId] = useState(null);
  const [uploadingMeetingId, setUploadingMeetingId] = useState(null);
  const [uploadFeedback, setUploadFeedback] = useState(null);
  const [requestSent, setRequestSent] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [pendingRequests, setPendingRequests] = useState([]);
  const [requestsLoading, setRequestsLoading] = useState(false);
  const [respondingId, setRespondingId] = useState(null);
  const [showTransferModal, setShowTransferModal] = useState(false);
  const fileInputRef = useRef(null);
  const activeMeetingRequestInFlightRef = useRef(false);

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

  const fetchActiveMeeting = useCallback(async ({ silent = false } = {}) => {
    if (!currentUserId || !isMember) {
      setActiveMeeting(null);
      return;
    }

    if (activeMeetingRequestInFlightRef.current) {
      return;
    }

    activeMeetingRequestInFlightRef.current = true;

    try {
      if (!silent) {
        setMeetingError(null);
      }

      const meeting = await ZoomApi.getActiveMeeting(communityId);
      if (!meeting) {
        setActiveMeeting(null);
        return;
      }

      setActiveMeeting(meeting);
    } catch (err) {
      if (err?.status === 404) {
        setActiveMeeting(null);
        return;
      }

      console.error('Error al obtener la reunión activa:', err);

      if (!silent) {
        setActiveMeeting(null);
        setMeetingError(err?.message || 'No se pudo comprobar la reunión activa');
      }
    } finally {
      activeMeetingRequestInFlightRef.current = false;
    }
  }, [communityId, currentUserId, isMember]);

  useEffect(() => {
    fetchActiveMeeting();
  }, [fetchActiveMeeting]);

  useEffect(() => {
    if (!currentUserId || !isMember) {
      return undefined;
    }

    const pollId = setInterval(() => {
      fetchActiveMeeting({ silent: true });
    }, 1000);

    return () => clearInterval(pollId);
  }, [fetchActiveMeeting, currentUserId, isMember]);

  useEffect(() => {
    if (!activeMeeting) {
      return undefined;
    }

    const timerId = setInterval(() => {
      setMeetingNow(Date.now());
    }, 1000);

    return () => clearInterval(timerId);
  }, [activeMeeting]);

  const meetingStartRaw = activeMeeting?.startedAt || activeMeeting?.createdAt;
  const meetingStartMs = meetingStartRaw ? new Date(meetingStartRaw).getTime() : null;
  const safeMeetingStartMs = Number.isFinite(meetingStartMs) ? meetingStartMs : null;
  const elapsedMs = safeMeetingStartMs ? Math.max(0, meetingNow - safeMeetingStartMs) : 0;
  const durationMinutes = Number(activeMeeting?.durationMinutes);
  const hasFiniteDuration = Number.isFinite(durationMinutes) && durationMinutes > 0;
  const remainingMs = hasFiniteDuration ? Math.max(0, durationMinutes * 60 * 1000 - elapsedMs) : null;

  const normalizeParticipants = (payload) => {
    if (Array.isArray(payload)) return payload;
    if (Array.isArray(payload?.participants)) return payload.participants;
    if (Array.isArray(payload?.content)) return payload.content;
    if (Array.isArray(payload?.items)) return payload.items;
    return [];
  };

  const normalizeMeetings = (payload) => {
    if (Array.isArray(payload)) return payload;
    if (Array.isArray(payload?.meetings)) return payload.meetings;
    if (Array.isArray(payload?.content)) return payload.content;
    if (Array.isArray(payload?.items)) return payload.items;
    return [];
  };

  const normalizeRecordings = (payload) => {
    if (Array.isArray(payload)) return payload;
    if (Array.isArray(payload?.recordings)) return payload.recordings;
    if (Array.isArray(payload?.content)) return payload.content;
    if (Array.isArray(payload?.items)) return payload.items;
    return [];
  };

  const getParticipantLabel = (participant, index) => {
    return participant?.usuarioNombre
      || participant?.nombre
      || participant?.displayName
      || participant?.name
      || participant?.email
      || `Participante ${index + 1}`;
  };

  const handleJoinMeeting = async () => {
    try {
      setMeetingLoading(true);
      setMeetingError(null);

      const hostUrl = activeMeeting?.startUrl;
      if (hostUrl) {
        window.open(hostUrl, '_blank', 'noopener,noreferrer');
        return;
      }

      const joinData = await ZoomApi.joinMeeting(communityId);
      const joinUrl = joinData?.joinUrl || activeMeeting?.joinUrl;

      if (!joinUrl) {
        setMeetingError('La reunión no tiene enlace de acceso disponible');
        return;
      }

      window.open(joinUrl, '_blank', 'noopener,noreferrer');
    } catch (err) {
      console.error('Error al unirse a la reunión:', err);
      setMeetingError(err?.message || 'No se pudo unir a la reunión');
    } finally {
      setMeetingLoading(false);
    }
  };

  const handleCreateAndJoinMeeting = async () => {
    try {
      setMeetingLoading(true);
      setMeetingError(null);

      const parsedDuration = Number(meetingDurationForm);
      const payload = {
        topic: (meetingTopic || '').trim() || 'Reunion de la comunidad',
        durationMinutes: Number.isFinite(parsedDuration) && parsedDuration > 0 ? parsedDuration : 60,
      };

      const meeting = await ZoomApi.createOrGetMeeting(communityId, payload);
      setActiveMeeting(meeting || null);
      setShowMeetingForm(false);

      const hostUrl = meeting?.startUrl;
      const accessUrl = hostUrl || meeting?.joinUrl;
      if (accessUrl) {
        window.open(accessUrl, '_blank', 'noopener,noreferrer');
        return;
      }

      const joinData = await ZoomApi.joinMeeting(communityId);
      if (joinData?.joinUrl) {
        window.open(joinData.joinUrl, '_blank', 'noopener,noreferrer');
      }
    } catch (err) {
      console.error('Error al crear o unirse a la reunión:', err);
      setMeetingError(err?.message || 'No se pudo crear o unir a la reunión');
    } finally {
      setMeetingLoading(false);
    }
  };

  const handleMeetingMainAction = async () => {
    if (activeMeeting) {
      await handleJoinMeeting();
      return;
    }

    setMeetingError(null);
    setShowMeetingForm((prev) => !prev);
  };

  const handleToggleParticipants = async () => {
    if (participantsOpen) {
      setParticipantsOpen(false);
      return;
    }

    setParticipantsOpen(true);

    try {
      setParticipantsLoading(true);
      setParticipantsError(null);
      const data = await ZoomApi.listParticipants(communityId);
      setActiveParticipants(normalizeParticipants(data));
    } catch (err) {
      console.error('Error al listar participantes activos:', err);
      setParticipantsError(err?.message || 'No se pudieron cargar los participantes');
      setActiveParticipants([]);
    } finally {
      setParticipantsLoading(false);
    }
  };

  const handleToggleMeetings = async () => {
    if (meetingsOpen) {
      setMeetingsOpen(false);
      setRecordingsOpen(false);
      setSelectedRecordingMeetingId(null);
      return;
    }

    try {
      setMeetingsLoading(true);
      setMeetingsError(null);
      const [data, recordingsData] = await Promise.all([
        ZoomApi.listMeetings(communityId),
        ZoomApi.listRecordings(communityId).catch((err) => {
          console.error('Error al precargar grabaciones para el historial:', err);
          return [];
        }),
      ]);
      setMeetingHistory(normalizeMeetings(data));
      setRecordings(normalizeRecordings(recordingsData));
      setMeetingsOpen(true);
    } catch (err) {
      console.error('Error al cargar el historial de reuniones:', err);
      setMeetingsError(err?.message || 'No se pudo cargar el historial de reuniones');
      setMeetingHistory([]);
      setMeetingsOpen(true);
    } finally {
      setMeetingsLoading(false);
    }
  };

  const loadRecordings = async (meetingId = null) => {
    try {
      setRecordingsLoading(true);
      setRecordingsError(null);
      const data = await ZoomApi.listRecordings(communityId);
      const normalized = normalizeRecordings(data);
      setRecordings(normalized);
      setSelectedRecordingMeetingId(meetingId);
      setRecordingsOpen(true);
    } catch (err) {
      console.error('Error al cargar las grabaciones:', err);
      setRecordingsError(err?.message || 'No se pudieron cargar las grabaciones');
      setRecordings([]);
      setSelectedRecordingMeetingId(meetingId);
      setRecordingsOpen(true);
    } finally {
      setRecordingsLoading(false);
    }
  };

  const handleSelectRecordingFile = (meetingId) => {
    setUploadFeedback(null);
    setUploadingMeetingId(meetingId);

    if (fileInputRef.current) {
      fileInputRef.current.value = '';
      fileInputRef.current.click();
    }
  };

  const handleRecordingFileChange = async (event) => {
    const file = event.target.files?.[0];
    const meetingId = uploadingMeetingId;

    if (!file || !meetingId) {
      return;
    }

    try {
      setRecordingsError(null);
      setUploadFeedback(null);
      await ZoomApi.uploadRecording(communityId, meetingId, file);
      setUploadFeedback({
        type: 'success',
        message: 'Grabacion subida correctamente.',
      });

      if (recordingsOpen) {
        await loadRecordings(selectedRecordingMeetingId);
      } else {
        const data = await ZoomApi.listRecordings(communityId);
        setRecordings(normalizeRecordings(data));
      }
    } catch (err) {
      console.error('Error al subir la grabacion:', err);
      setUploadFeedback({
        type: 'error',
        message: err?.message || 'No se pudo subir la grabacion.',
      });
    } finally {
      setUploadingMeetingId(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const handleShowMeetingRecordings = async (meeting) => {
    const meetingId = meeting?.zoomMeetingId || null;

    if (recordingsOpen && selectedRecordingMeetingId === meetingId) {
      setRecordingsOpen(false);
      return;
    }

    await loadRecordings(meetingId);
  };

  const handleDownloadRecording = async (recording) => {
    if (!recording?.zoomRecordingId) {
      return;
    }

    try {
      setDownloadingRecordingId(recording.zoomRecordingId);

      if (recording?.appDownloadUrl) {
        const download = await ZoomApi.downloadRecording(communityId, recording.zoomRecordingId);
        const objectUrl = window.URL.createObjectURL(download.blob);
        const link = document.createElement('a');
        link.href = objectUrl;
        link.download = download.fileName;
        document.body.appendChild(link);
        link.click();
        link.remove();
        window.URL.revokeObjectURL(objectUrl);
        return;
      }

      if (recording?.downloadUrl) {
        window.open(recording.downloadUrl, '_blank', 'noopener,noreferrer');
      }
    } catch (err) {
      console.error('Error al descargar la grabacion:', err);
      setRecordingsError(err?.message || 'No se pudo descargar la grabacion.');
    } finally {
      setDownloadingRecordingId(null);
    }
  };

  useEffect(() => {
    if (activeMeeting) {
      setShowMeetingForm(false);
      return;
    }

    setParticipantsOpen(false);
    setParticipantsError(null);
    setActiveParticipants([]);
  }, [activeMeeting]);

  useEffect(() => {
    setMeetingsOpen(false);
    setMeetingsError(null);
    setMeetingHistory([]);
    setRecordingsOpen(false);
    setRecordingsError(null);
    setRecordings([]);
    setSelectedRecordingMeetingId(null);
  }, [communityId]);

  const visibleRecordings = selectedRecordingMeetingId
    ? recordings.filter((recording) => recording?.zoomMeetingId === selectedRecordingMeetingId)
    : recordings;

  const getMeetingRecordings = (meeting) => {
    const zoomMeetingId = meeting?.zoomMeetingId;
    if (!zoomMeetingId) {
      return [];
    }
    return recordings.filter((recording) => recording?.zoomMeetingId === zoomMeetingId);
  };

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

        <input
          ref={fileInputRef}
          type="file"
          accept=".mp4,.mov,.webm,.m4a,video/mp4,video/quicktime,video/webm,audio/mp4"
          onChange={handleRecordingFileChange}
          style={{ display: 'none' }}
        />

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
extraActions={(
  <div className="cd-floating-tools">
    <div className="cd-floating-toolbar">
      {activeMeeting ? (
        <div className="cd-floating-meeting-timer" title="Tiempo de la reunión activa">
          <span>Activa: {formatDuration(elapsedMs)}</span>
          <span>
            {hasFiniteDuration
              ? `Restante: ${formatDuration(remainingMs)}`
              : 'Restante: 60 min máx.'}
          </span>
        </div>
      ) : null}

      <button
        type="button"
        className="cd-floating-zoom-btn cd-floating-zoom-btn-history"
        onClick={handleToggleMeetings}
        disabled={meetingsLoading}
        title="Ver historial de reuniones"
      >
        <LuCalendar size={18} />
        <span>{meetingsOpen ? 'Ocultar historial' : 'Historial'}</span>
      </button>

      <button
        type="button"
        className={`cd-floating-zoom-btn ${activeMeeting ? 'cd-floating-zoom-btn-join' : ''}`}
        onClick={handleMeetingMainAction}
        disabled={meetingLoading}
        title={activeMeeting ? 'Unirse a la reunion activa' : 'Crear reunion con formulario'}
      >
        {activeMeeting ? <LuPlay size={18} /> : <LuVideo size={18} />}
        <span>
          {meetingLoading
            ? 'Procesando...'
            : activeMeeting
              ? 'Unirse'
              : 'Crear y unirse'}
        </span>
      </button>

      {activeMeeting ? (
        <button
          type="button"
          className="cd-floating-zoom-btn cd-floating-zoom-btn-participants"
          onClick={handleToggleParticipants}
          disabled={participantsLoading}
          title="Ver participantes activos"
        >
          <LuUsers size={18} />
          <span>{participantsOpen ? 'Ocultar participantes' : 'Participantes'}</span>
        </button>
      ) : null}
    </div>

    {!activeMeeting && showMeetingForm ? (
      <div className="cd-floating-popover cd-floating-popover-create">
        <div className="cd-meeting-form-card">
          <div className="cd-meeting-form-row">
            <label htmlFor="meeting-topic">Tema</label>
            <input
              id="meeting-topic"
              type="text"
              value={meetingTopic}
              onChange={(e) => setMeetingTopic(e.target.value)}
              placeholder="Ej. Tutorias semanales"
              maxLength={120}
            />
          </div>
          <div className="cd-meeting-form-row">
            <label htmlFor="meeting-duration">Duracion (min)</label>
            <input
              id="meeting-duration"
              type="number"
              min="5"
              max="180"
              step="5"
              value={meetingDurationForm}
              onChange={(e) => setMeetingDurationForm(e.target.value)}
            />
          </div>
          <div className="cd-meeting-form-actions">
            <button
              type="button"
              className="cd-btn cd-btn-create"
              onClick={handleCreateAndJoinMeeting}
              disabled={meetingLoading}
            >
              <LuVideo /> {meetingLoading ? 'Creando...' : 'Crear reunion'}
            </button>
            <button
              type="button"
              className="cd-btn cd-btn-leave"
              onClick={() => setShowMeetingForm(false)}
              disabled={meetingLoading}
            >
              Cancelar
            </button>
          </div>
        </div>
      </div>
    ) : null}

    {meetingsOpen ? (
      <div className="cd-floating-popover cd-floating-popover-history">
        <div className="cd-floating-meetings-panel">
          {meetingsError ? (
            <p className="cd-floating-meetings-error">{meetingsError}</p>
          ) : meetingHistory.length > 0 ? (
            <>
              <p className="cd-floating-meetings-title">
                Historial de reuniones ({meetingHistory.length})
              </p>
              {uploadFeedback ? (
                <p className={`cd-floating-upload-feedback ${uploadFeedback.type === 'error' ? 'is-error' : 'is-success'}`}>
                  {uploadFeedback.message}
                </p>
              ) : null}
              <ul className="cd-floating-meetings-list">
                {meetingHistory.map((meeting, index) => (
                  <li key={meeting?.id || meeting?.zoomMeetingId || index}>
                    {(() => {
                      const meetingRecordings = getMeetingRecordings(meeting);
                      const hasRecordings = meetingRecordings.length > 0;

                      return (
                        <>
                          <strong>{meeting?.topic || `Reunion ${index + 1}`}</strong>
                          <span>{meeting?.status || 'SIN_ESTADO'}</span>
                          <span>Creada: {formatDateTime(meeting?.createdAt)}</span>
                          {meeting?.startedAt ? <span>Inicio: {formatDateTime(meeting.startedAt)}</span> : null}
                          {meeting?.endedAt ? <span>Fin: {formatDateTime(meeting.endedAt)}</span> : null}
                          <div className="cd-meeting-history-actions">
                            {hasRecordings ? (
                              <button
                                type="button"
                                className="cd-meeting-history-link"
                                onClick={() => handleShowMeetingRecordings(meeting)}
                                disabled={recordingsLoading}
                              >
                                {recordingsLoading && selectedRecordingMeetingId === meeting?.zoomMeetingId
                                  ? 'Cargando grabaciones...'
                                  : `Ver grabaciones (${meetingRecordings.length})`}
                              </button>
                            ) : (
                              <span className="cd-meeting-history-muted">Sin grabaciones</span>
                            )}
                            <button
                              type="button"
                              className="cd-meeting-history-link"
                              onClick={() => handleSelectRecordingFile(meeting?.id)}
                              disabled={uploadingMeetingId === meeting?.id}
                            >
                              {uploadingMeetingId === meeting?.id ? 'Subiendo...' : 'Subir grabacion'}
                            </button>
                          </div>
                        </>
                      );
                    })()}
                  </li>
                ))}
              </ul>
            </>
          ) : (
            <p className="cd-floating-meetings-empty">No hay reuniones registradas todavia.</p>
          )}
        </div>
      </div>
    ) : null}

    {activeMeeting && participantsOpen ? (
      <div className="cd-floating-popover cd-floating-popover-participants">
        <div className="cd-floating-participants-panel">
          {participantsLoading ? (
            <p className="cd-floating-participants-empty">Cargando participantes...</p>
          ) : participantsError ? (
            <p className="cd-floating-participants-error">{participantsError}</p>
          ) : activeParticipants.length > 0 ? (
            <>
              <p className="cd-floating-participants-title">
                Participantes activos ({activeParticipants.length})
              </p>
              <ul className="cd-floating-participants-list">
                {activeParticipants.map((participant, index) => (
                  <li key={participant?.id || participant?.usuarioId || participant?.email || index}>
                    {getParticipantLabel(participant, index)}
                  </li>
                ))}
              </ul>
            </>
          ) : (
            <p className="cd-floating-participants-empty">No hay participantes activos en este momento.</p>
          )}
        </div>
      </div>
    ) : null}

    {recordingsOpen ? (
      <div className="cd-floating-popover cd-floating-popover-recordings">
        <div className="cd-floating-recordings-panel">
          {recordingsError ? (
            <p className="cd-floating-recordings-error">{recordingsError}</p>
          ) : visibleRecordings.length > 0 ? (
            <>
              <p className="cd-floating-recordings-title">
                {selectedRecordingMeetingId
                  ? `Grabaciones de la reunion (${visibleRecordings.length})`
                  : `Grabaciones de la comunidad (${visibleRecordings.length})`}
              </p>
              <ul className="cd-floating-recordings-list">
                {visibleRecordings.map((recording, index) => (
                  <li key={recording?.zoomRecordingId || index}>
                    <strong>{recording?.fileType || 'Grabacion'}</strong>
                    <span>Inicio: {formatDateTime(recording?.recordingStart || recording?.createdAt)}</span>
                    {recording?.recordingEnd ? <span>Fin: {formatDateTime(recording.recordingEnd)}</span> : null}
                    <span>{formatFileSize(recording?.fileSizeBytes)}</span>
                    <div className="cd-floating-recordings-links">
                      {recording?.playUrl ? (
                        <a href={recording.playUrl} target="_blank" rel="noreferrer">Abrir</a>
                      ) : null}
                      {(recording?.appDownloadUrl || recording?.downloadUrl) ? (
                        <button
                          type="button"
                          className="cd-recording-link-button"
                          onClick={() => handleDownloadRecording(recording)}
                          disabled={downloadingRecordingId === recording?.zoomRecordingId}
                        >
                          {downloadingRecordingId === recording?.zoomRecordingId ? 'Descargando...' : 'Descargar'}
                        </button>
                      ) : null}
                    </div>
                  </li>
                ))}
              </ul>
            </>
          ) : (
            <p className="cd-floating-recordings-empty">No hay grabaciones disponibles para esta reunión.</p>
          )}
        </div>
      </div>
    ) : null}

    {meetingError ? (
      <span className="cd-floating-zoom-error">{meetingError}</span>
    ) : null}
  </div>
)}
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
      </div>
    </>
  );
}

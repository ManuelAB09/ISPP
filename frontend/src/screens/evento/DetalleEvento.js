import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  LuCalendar, LuMapPin, LuLink, LuUsers, LuUser,
  LuPencil, LuX, LuArrowLeft, LuPackage,
  LuEye, LuEyeOff, LuMap, LuClock, LuCheck, LuBell, LuTrash2,
  LuVideo, LuPlay, LuBookOpen
} from 'react-icons/lu';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import './DetalleEvento.css';
import Header from '../../components/Header/Header';
import axiosInstance from '../../api/axiosConfig';
import {
  getEventById, cancelEvent, attendEvent, cancelAttendance,
  getConfirmedAttendees, getMyAttendance, linkClassroomTask, unlinkClassroomTask
} from '../../api/eventEndpoints';
import { communitiesApi } from '../../api/communities.api';
import { ZoomApi } from '../../api/zoom.api';
import { checkAlreadyRated } from '../../api/valoraciones.api';
import { getApiBaseUrl } from '../../api/baseUrl';
import { normalizeCommunityRole } from '../../utils/communityRoles';
import { useSocketContext } from '../../contexts/SocketContext';
import RatingForm from '../../components/RatingForm';

const OPCIONES_ANTELACION = [
  { label: '2 días antes', value: 2880 },
  { label: '1 día antes', value: 1440 },
  { label: '2 horas antes', value: 120 },
  { label: '1 hora antes', value: 60 },
  { label: '30 minutos antes', value: 30 },
];

const CANAL_LABELS = { PLATAFORMA: 'Solo en la app', EMAIL: 'Solo por email', AMBOS: 'Ambos' };

const formatAlarmLabel = (minutos) => {
  if (!minutos) return '';
  if (minutos >= 1440 && minutos % 1440 === 0) {
    const dias = minutos / 1440;
    return dias === 1 ? 'en 1 día' : `en ${dias} días`;
  }
  if (minutos >= 60 && minutos % 60 === 0) {
    const horas = minutos / 60;
    return horas === 1 ? 'en 1 hora' : `en ${horas} horas`;
  }
  return `en ${minutos} minutos`;
};

const toAbsoluteImageUrl = (imageUrl, fallback = '') => {
  const raw = String(imageUrl || '').trim();
  if (!raw) {
    return fallback;
  }
  if (/^https?:\/\//i.test(raw) || raw.startsWith('data:') || raw.startsWith('blob:')) {
    return raw;
  }

  const base = getApiBaseUrl();
  return raw.startsWith('/') ? `${base}${raw}` : `${base}/${raw}`;
};

const getUserPhoto = (user) => {
  if (!user || typeof user !== 'object') {
    return '';
  }
  return user.foto || user.fotoPerfil || user.avatar || user.imagen || user.image || '';
};

const eventIconRed = L.icon({
  iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-red.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowUrl: 'https://unpkg.com/leaflet@1.7.1/dist/images/marker-shadow.png',
  shadowSize: [41, 41],
});

// Valoración de profesor (sin validación de permisos)
// Mover fuera de la función para evitar acceder a 'event' antes de su inicialización
const DetalleEvento = () => {
  const { eventId } = useParams();
  // Persist valorado per user+event in localStorage
  const [valorado, setValorado] = useState(() => {
    try {
      const userId = localStorage.getItem('userId');
      const ratedEvents = JSON.parse(localStorage.getItem('ratedEvents') || '{}');
      return !!ratedEvents[`${userId}_${eventId}`];
    } catch {
      return false;
    }
  });
  // When valorado changes to true, persist in localStorage
  useEffect(() => {
    if (valorado) {
      try {
        const userId = localStorage.getItem('userId');
        const ratedEvents = JSON.parse(localStorage.getItem('ratedEvents') || '{}');
        ratedEvents[`${userId}_${eventId}`] = true;
        localStorage.setItem('ratedEvents', JSON.stringify(ratedEvents));
      } catch {}
    }
  }, [valorado, eventId]);
  const navigate = useNavigate();
  const { socket } = useSocketContext();

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
  const [communityRole, setCommunityRole] = useState(null);

  // Alarmas
  const [alarms, setAlarms] = useState([]);
  const [alarmsLoading, setAlarmsLoading] = useState(false);
  const [addAlarmMinutos, setAddAlarmMinutos] = useState(1440);
  const [addAlarmCanal, setAddAlarmCanal] = useState('AMBOS');
  const [addAlarmLoading, setAddAlarmLoading] = useState(false);

  // Modal de confirmación de asistencia con alarmas
  const [showAttendModal, setShowAttendModal] = useState(false);
  const [selectedMinutos, setSelectedMinutos] = useState([]);
  const [selectedCanal, setSelectedCanal] = useState('AMBOS');

  // Zoom meeting state
  const [activeMeeting, setActiveMeeting] = useState(null);
  const [meetingLoading, setMeetingLoading] = useState(false);
  const [meetingError, setMeetingError] = useState(null);
  const [meetingNow, setMeetingNow] = useState(Date.now());
  const [showMeetingForm, setShowMeetingForm] = useState(false);
  const [meetingTopic, setMeetingTopic] = useState('');
  const [meetingDurationForm, setMeetingDurationForm] = useState(60);
  const [zoomParticipants, setZoomParticipants] = useState([]);
  const [participantsOpen, setParticipantsOpen] = useState(false);
  const activeMeetingRequestInFlightRef = useRef(false);

  const [recordingsOpen, setRecordingsOpen] = useState(false);
  const [recordings, setRecordings] = useState([]);
  const [recordingsLoading, setRecordingsLoading] = useState(false);

  // Classroom Task State
  const [showTaskModal, setShowTaskModal] = useState(false);
  const [classroomTasks, setClassroomTasks] = useState([]);
  const [tasksLoading, setTasksLoading] = useState(false);
  const [taskError, setTaskError] = useState(null);
  const [taskLinking, setTaskLinking] = useState(false);

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
          const membership = await communitiesApi.getMyMembership(eventData.comunidadId);
          setIsMember(true);
          setCommunityRole(normalizeCommunityRole(membership?.rol));
        } catch {
          setIsMember(false);
          setCommunityRole(null);
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

        // Cargar alarmas del usuario para este evento
        try {
          const alarmsData = await axiosInstance.get(`/api/v1/events/${eventId}/alarms`);
          setAlarms(Array.isArray(alarmsData.data) ? alarmsData.data : []);
        } catch {
          setAlarms([]);
        }

        // Verificar si ya valoré este evento en el backend
        try {
          const checkResp = await checkAlreadyRated(currentUserId, eventId);
          if (checkResp?.rated || checkResp?.data?.rated) {
            setValorado(true);
          }
        } catch {
          // Si falla, respetar el estado de localStorage
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
  // For community events: only ADMIN/PROFESOR can edit. For non-community: organizer can edit.
  const canEditEvent = communityRole
    ? (communityRole === 'ADMIN' || communityRole === 'PROFESOR')
    : isOrganizer;
  const isConfirmed = myAttendance?.estado === 'CONFIRMADA';
  const isFull = event && event.aforo && (event.asistentesConfirmados || 0) >= event.aforo;
  const isCancelled = event?.cancelado;
  const isStarted = event?.fechaHora ? new Date(event.fechaHora).getTime() <= Date.now() : false;
  const isEnded = event?.fechaFin
    ? new Date(event.fechaFin).getTime() <= Date.now()
    : isStarted && event?.fechaHora
      ? Date.now() - new Date(event.fechaHora).getTime() > 2 * 60 * 60 * 1000
      : false;

  // Abre el modal de confirmación de asistencia
  const handleAttend = () => {
    setSelectedMinutos([]);
    setSelectedCanal('AMBOS');
    setShowAttendModal(true);
  };

  // Confirma asistencia y crea alarmas si se eligieron
  const handleConfirmAttend = async () => {
    try {
      setAttendanceLoading(true);
      await attendEvent(eventId);
      if (selectedMinutos.length > 0) {
        await axiosInstance.post(`/api/v1/events/${eventId}/alarms/batch`, {
          minutosAntesList: selectedMinutos,
          canal: selectedCanal,
        });
      }
      setShowAttendModal(false);
      await fetchEventData();
    } catch (err) {
      setError(err.response?.data?.message || 'Error al confirmar asistencia.');
    } finally {
      setAttendanceLoading(false);
    }
  };

  const handleCancelAttendance = async () => {
    try {
      setAttendanceLoading(true);
      await cancelAttendance(eventId);
      // Eliminar todas las alarmas al cancelar asistencia
      try { await axiosInstance.delete(`/api/v1/events/${eventId}/alarms`); } catch { /* silencioso */ }
      setMyAttendance(null);
      setAlarms([]);
      await fetchEventData();
    } catch (err) {
      setError(err.response?.data?.message || 'Error al cancelar asistencia.');
    } finally {
      setAttendanceLoading(false);
    }
  };

  const handleDeleteAlarm = async (alarmaId) => {
    setAlarmsLoading(true);
    try {
      await axiosInstance.delete(`/api/v1/events/${eventId}/alarms/${alarmaId}`);
      setAlarms(prev => prev.filter(a => a.id !== alarmaId));
    } catch { /* silencioso */ } finally {
      setAlarmsLoading(false);
    }
  };

  const handleAddAlarm = async () => {
    setAddAlarmLoading(true);
    try {
      const res = await axiosInstance.post(`/api/v1/events/${eventId}/alarms`, {
        minutosAntes: addAlarmMinutos,
        canal: addAlarmCanal,
      });
      setAlarms(prev => [...prev, res.data]);
    } catch { /* silencioso */ } finally {
      setAddAlarmLoading(false);
    }
  };

  const toggleMinuto = (value) => {
    setSelectedMinutos(prev =>
      prev.includes(value) ? prev.filter(v => v !== value) : [...prev, value]
    );
  };

  const handleCancelEvent = async () => {
    if (isStarted) {
      setError('No se puede cancelar un evento que ya ha comenzado.');
      setShowCancelModal(false);
      return;
    }

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

  // ============================
  // ZOOM MEETING FUNCTIONS
  // ============================

  const fetchActiveEventMeeting = useCallback(async ({ silent = false } = {}) => {
    if (!currentUserId || !isMember || !event?.esVirtual) {
      setActiveMeeting(null);
      return;
    }
    if (activeMeetingRequestInFlightRef.current) return;
    activeMeetingRequestInFlightRef.current = true;

    try {
      if (!silent) setMeetingError(null);
      const meeting = await ZoomApi.getActiveEventMeeting(eventId);
      if (!meeting) { setActiveMeeting(null); return; }
      setActiveMeeting(meeting);
    } catch (err) {
      if (err?.status === 404) { setActiveMeeting(null); return; }
      if (!silent) {
        setActiveMeeting(null);
        setMeetingError(err?.message || 'No se pudo comprobar la reunión activa');
      }
    } finally {
      activeMeetingRequestInFlightRef.current = false;
    }
  }, [eventId, currentUserId, isMember, event?.esVirtual]);

  useEffect(() => {
    if (event?.esVirtual) fetchActiveEventMeeting();
  }, [fetchActiveEventMeeting, event?.esVirtual]);

  useEffect(() => {
    if (!currentUserId || !isMember || !event?.esVirtual || !eventId) return undefined;

    const topic = `/topic/event.${eventId}.meeting`;
    const handler = (data) => {
      if (!data || data === '') {
        setActiveMeeting(null);
      } else {
        setActiveMeeting(data);
      }
    };
    socket.on(topic, handler);
    return () => socket.off(topic, handler);
  }, [socket, eventId, currentUserId, isMember, event?.esVirtual]);

  useEffect(() => {
    if (!activeMeeting) return undefined;
    const timerId = setInterval(() => setMeetingNow(Date.now()), 1000);
    return () => clearInterval(timerId);
  }, [activeMeeting]);

  const meetingStartRaw = activeMeeting?.startedAt || activeMeeting?.createdAt;
  const meetingStartMs = meetingStartRaw ? new Date(meetingStartRaw).getTime() : null;
  const safeMeetingStartMs = Number.isFinite(meetingStartMs) ? meetingStartMs : null;
  const elapsedMs = safeMeetingStartMs ? Math.max(0, meetingNow - safeMeetingStartMs) : 0;
  const durationMinutes = Number(activeMeeting?.durationMinutes);
  const hasFiniteDuration = Number.isFinite(durationMinutes) && durationMinutes > 0;

  const formatDuration = (ms) => {
    if (ms == null || !Number.isFinite(ms)) return '--:--';
    const totalSecs = Math.floor(ms / 1000);
    const h = Math.floor(totalSecs / 3600);
    const m = Math.floor((totalSecs % 3600) / 60);
    const s = totalSecs % 60;
    return h > 0
      ? `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
      : `${m}:${String(s).padStart(2, '0')}`;
  };

  const handleJoinEventMeeting = async () => {
    try {
      setMeetingLoading(true);
      setMeetingError(null);
      const hostUrl = activeMeeting?.startUrl;
      if (hostUrl) { window.open(hostUrl, '_blank', 'noopener,noreferrer'); return; }
      const joinData = await ZoomApi.joinEventMeeting(eventId);
      const joinUrl = joinData?.joinUrl || activeMeeting?.joinUrl;
      if (!joinUrl) { setMeetingError('La reunión no tiene enlace de acceso disponible'); return; }
      window.open(joinUrl, '_blank', 'noopener,noreferrer');
    } catch (err) {
      setMeetingError(err?.message || 'No se pudo unir a la reunión');
    } finally {
      setMeetingLoading(false);
    }
  };

  const handleCreateAndJoinEventMeeting = async () => {
    try {
      setMeetingLoading(true);
      setMeetingError(null);
      const parsedDuration = Number(meetingDurationForm);
      const payload = {
        topic: (meetingTopic || '').trim() || `Evento: ${event?.titulo || 'Reunión'}`,
        durationMinutes: Number.isFinite(parsedDuration) && parsedDuration > 0 ? parsedDuration : 60,
      };
      const meeting = await ZoomApi.createOrGetEventMeeting(eventId, payload);
      setActiveMeeting(meeting || null);
      setShowMeetingForm(false);
      const hostUrl = meeting?.startUrl;
      const accessUrl = hostUrl || meeting?.joinUrl;
      if (accessUrl) { window.open(accessUrl, '_blank', 'noopener,noreferrer'); return; }
      const joinData = await ZoomApi.joinEventMeeting(eventId);
      if (joinData?.joinUrl) window.open(joinData.joinUrl, '_blank', 'noopener,noreferrer');
    } catch (err) {
      setMeetingError(err?.message || 'No se pudo crear o unir a la reunión');
    } finally {
      setMeetingLoading(false);
    }
  };

  const handleEndEventMeeting = async () => {
    try {
      setMeetingLoading(true);
      setMeetingError(null);
      await ZoomApi.endEventMeeting(eventId);
      setActiveMeeting(null);
    } catch (err) {
      setMeetingError(err?.message || 'No se pudo finalizar la reunión');
    } finally {
      setMeetingLoading(false);
    }
  };

  const handleToggleZoomParticipants = async () => {
    if (participantsOpen) { setParticipantsOpen(false); return; }
    try {
      const data = await ZoomApi.listEventParticipants(eventId);
      setZoomParticipants(Array.isArray(data) ? data : (data?.participants || data?.content || []));
      setParticipantsOpen(true);
    } catch (err) {
      setZoomParticipants([]);
      setParticipantsOpen(true);
    }
  };

  const handleToggleRecordings = async () => {
    if (recordingsOpen) {
      setRecordingsOpen(false);
      return;
    }
    setRecordingsOpen(true);
    setRecordingsLoading(true);
    try {
      const data = await ZoomApi.listRecordings(event.comunidadId);
      let list = Array.isArray(data) ? data : (data?.recordings || data?.content || data?.items || []);
      setRecordings(list);
    } catch(err) {
      setRecordings([]);
    } finally {
      setRecordingsLoading(false);
    }
  };

  const handleDownloadRecording = async (recId) => {
    try {
      const { blob, fileName } = await ZoomApi.downloadRecording(event.comunidadId, recId);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', fileName);
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
    } catch(err) {
      alert('Error descargando la grabación');
    }
  };

  const handleOpenTaskModal = async () => {
    setShowTaskModal(true);
    setTasksLoading(true);
    setTaskError(null);
    try {
      const resp = await axiosInstance.get(`/oauth2/communities/${event.comunidadId}/files`);
      if (resp.data && resp.data.courseWork && resp.data.courseWork.courseWork) {
        setClassroomTasks(resp.data.courseWork.courseWork);
      } else if (resp.data && Array.isArray(resp.data.courseWork)) {
        setClassroomTasks(resp.data.courseWork);
      } else {
        setClassroomTasks([]);
      }
    } catch (err) {
      if (err.response?.status === 403) {
        setTaskError('missing_scopes');
      } else {
        setTaskError(err.response?.data?.error || 'Error al cargar las tareas de Classroom');
      }
    } finally {
      setTasksLoading(false);
    }
  };

  useEffect(() => {
    const handleMessage = (e) => {
      if (e.data && (e.data.courses || e.data.success || e.data.error)) {
        if (showTaskModal && taskError === 'missing_scopes' && !e.data.error) {
          handleOpenTaskModal(); // Re-fetch automatically
        }
      }
    };
    window.addEventListener('message', handleMessage);
    return () => window.removeEventListener('message', handleMessage);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [showTaskModal, taskError]);

  const handleLinkTask = async (task) => {
    try {
      setTaskLinking(true);
      await linkClassroomTask(eventId, {
        taskId: task.id,
        title: task.title,
        url: task.alternateLink
      });
      setShowTaskModal(false);
      await fetchEventData();
    } catch (err) {
      setTaskError(err.response?.data?.message || 'Error al vincular la tarea');
    } finally {
      setTaskLinking(false);
    }
  };

  const handleUnlinkTask = async () => {
    if (!window.confirm('¿Seguro que quieres desvincular esta tarea?')) return;
    try {
      setTaskLinking(true);
      await unlinkClassroomTask(eventId);
      await fetchEventData();
    } catch (err) {
      setError(err.response?.data?.message || 'Error al desvincular la tarea');
    } finally {
      setTaskLinking(false);
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

  // Estado y efecto para obtener el id real de tutor para la valoración
  const [realTutorId, setRealTutorId] = React.useState(null);
  const [buscandoTutor, setBuscandoTutor] = useState(false);
  const [, setTutorError] = useState(null);

  // Determinar si el creador es profesor en la comunidad del evento
  const creadorEsProfesorEnComunidad = event?.creadorRolComunidad === 'PROFESOR';

  React.useEffect(() => {
    let cancelled = false;
    async function fetchTutorId() {
      setBuscandoTutor(false);
      setTutorError(null);
      if (event && creadorEsProfesorEnComunidad && event.creador?.tutorId) {
        setRealTutorId(event.creador.tutorId);
      } else if (event && creadorEsProfesorEnComunidad && event.creador?.id) {
        setBuscandoTutor(true);
        try {
          const resp = await axiosInstance.get(`/api/v1/tutors/user/${event.creador.id}`);
          if (!cancelled && resp.data && resp.data.id) {
            setRealTutorId(resp.data.id);
          } else if (!cancelled) {
            setRealTutorId(null);
            setTutorError('No se encontró tutor para el organizador.');
          }
        } catch (e) {
          if (!cancelled) {
            setRealTutorId(null);
            setTutorError('No se encontró tutor para el organizador.');
          }
        } finally {
          if (!cancelled) setBuscandoTutor(false);
        }
      } else {
        setRealTutorId(null);
      }
    }
    fetchTutorId();
    return () => { cancelled = true; };
  }, [event, creadorEsProfesorEnComunidad]);

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
              {event.visibleMapa && !isCancelled && !event.esVirtual && (
                <span className="ed-badge ed-badge-map"><LuMap /> Visible en mapa</span>
              )}
              {isStarted && !isCancelled && (
                <span className="ed-badge ed-badge-cancelled">Evento iniciado</span>
              )}
            </div>
            <h1 className="ed-title">{event.titulo}</h1>
            {event.creador && (
              <div className="ed-organizer-row">
                <div className="ed-participant-avatar ed-organizer-avatar">
                  {getUserPhoto(event.creador) ? (
                    <img
                      src={toAbsoluteImageUrl(getUserPhoto(event.creador))}
                      alt={event.creador.nombre || event.creador.username || 'Organizador'}
                    />
                  ) : (
                    <LuUser />
                  )}
                </div>
                <p className="ed-organizer">
                  Organizado por <strong>{event.creador.nombre || event.creador.username || 'Usuario'}</strong>
                </p>
              </div>
            )}
          </div>

          {/* Acciones del organizador */}
          {canEditEvent && !isCancelled && !isStarted && (
            <div className="ed-organizer-actions">
              <button
                className="ed-btn ed-btn-edit"
                onClick={() => navigate(`/crear-evento/${eventId}`)}
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

                {event.esVirtual ? (
                  <div className="ed-detail-item" style={{ alignItems: 'flex-start' }}>
                    <LuVideo className="ed-detail-icon" style={{ color: '#1890ff', fontSize: '1.3rem', marginTop: 2 }} />
                    <div style={{ flex: 1 }}>
                      <span className="ed-detail-label">Reunión por Zoom</span>

                      {meetingError && (
                        <p style={{ color: '#ff4d4f', fontSize: '0.85rem', margin: '4px 0' }}>{meetingError}</p>
                      )}

                      {activeMeeting ? (
                        <div style={{ marginTop: 8 }}>
                          <div style={{
                            background: '#f0faf5', border: '1px solid #b7eb8f',
                            borderRadius: 8, padding: '10px 14px', marginBottom: 8
                          }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
                              <span style={{ color: '#52c41a', fontWeight: 700, fontSize: '0.95rem' }}>
                                Llamada activa
                              </span>
                              <span style={{ fontSize: '0.85rem', color: '#888' }}>
                                {formatDuration(elapsedMs)}
                                {hasFiniteDuration ? ` / ${durationMinutes} min` : ''}
                              </span>
                            </div>
                            <p style={{ margin: '2px 0 0', fontSize: '0.85rem', color: '#555' }}>
                              {activeMeeting.topic}
                            </p>
                          </div>

                          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                            <button
                              onClick={handleJoinEventMeeting}
                              disabled={meetingLoading}
                              style={{
                                display: 'inline-flex', alignItems: 'center', gap: 6,
                                padding: '8px 16px', background: '#52c41a', color: '#fff',
                                border: 'none', borderRadius: 6, cursor: 'pointer',
                                fontWeight: 600, fontSize: '0.9rem'
                              }}
                            >
                              <LuPlay size={16} />
                              {meetingLoading ? 'Procesando...' : 'Unirse a la llamada'}
                            </button>

                            <button
                              onClick={handleToggleZoomParticipants}
                              style={{
                                display: 'inline-flex', alignItems: 'center', gap: 6,
                                padding: '8px 16px', background: '#f0f0f0', color: '#333',
                                border: '1px solid #d9d9d9', borderRadius: 6, cursor: 'pointer',
                                fontSize: '0.9rem'
                              }}
                            >
                              <LuUsers size={16} />
                              {participantsOpen ? 'Ocultar' : 'Participantes'}
                            </button>

                            {isOrganizer && (
                              <button
                                onClick={handleEndEventMeeting}
                                disabled={meetingLoading}
                                style={{
                                  display: 'inline-flex', alignItems: 'center', gap: 6,
                                  padding: '8px 16px', background: '#ff4d4f', color: '#fff',
                                  border: 'none', borderRadius: 6, cursor: 'pointer',
                                  fontSize: '0.9rem'
                                }}
                              >
                                <LuX size={16} /> Finalizar
                              </button>
                            )}
                          </div>

                          {participantsOpen && (
                            <div style={{ marginTop: 8, padding: '8px 12px', background: '#fafafa', borderRadius: 6, border: '1px solid #f0f0f0' }}>
                              <p style={{ fontWeight: 600, fontSize: '0.85rem', marginBottom: 4 }}>En la llamada:</p>
                              {zoomParticipants.length > 0 ? (
                                <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
                                  {zoomParticipants.map((p, i) => (
                                    <li key={p.usuarioId || i} style={{ padding: '2px 0', fontSize: '0.85rem', color: '#333' }}>
                                      {p.displayName || p.nombre || p.email || `Participante ${i + 1}`}
                                    </li>
                                  ))}
                                </ul>
                              ) : (
                                <p style={{ fontSize: '0.85rem', color: '#999', margin: 0 }}>Nadie en la llamada ahora mismo</p>
                              )}
                            </div>
                          )}
                        </div>
                      ) : (
                        <div style={{ marginTop: 8 }}>
                          {!showMeetingForm ? (
                            <div>
                              <p style={{ color: '#888', fontSize: '0.9rem', margin: '0 0 8px' }}>
                                {isOrganizer
                                  ? 'No hay llamada activa. Inicia una reunión Zoom para este evento.'
                                  : 'No hay llamada activa. El organizador debe iniciar la reunión.'}
                              </p>
                              {isOrganizer && (
                                <button
                                  onClick={() => {
                                    setMeetingTopic(`Evento: ${event.titulo}`);
                                    setShowMeetingForm(true);
                                  }}
                                  disabled={meetingLoading}
                                  style={{
                                    display: 'inline-flex', alignItems: 'center', gap: 6,
                                    padding: '8px 16px', background: '#1890ff', color: '#fff',
                                    border: 'none', borderRadius: 6, cursor: 'pointer',
                                    fontWeight: 600, fontSize: '0.9rem'
                                  }}
                                >
                                  <LuVideo size={16} /> Crear y unirse
                                </button>
                              )}
                            </div>
                          ) : (
                            <div style={{
                              background: '#f0f7ff', border: '1px solid #91caff',
                              borderRadius: 8, padding: '12px 16px'
                            }}>
                              <div style={{ marginBottom: 8 }}>
                                <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: 4 }}>Tema</label>
                                <input
                                  type="text"
                                  value={meetingTopic}
                                  onChange={(e) => setMeetingTopic(e.target.value)}
                                  placeholder="Ej. Reunión del evento"
                                  maxLength={120}
                                  style={{ width: '100%', padding: '6px 10px', borderRadius: 6, border: '1px solid #d9d9d9', fontSize: '0.9rem' }}
                                />
                              </div>
                              <div style={{ marginBottom: 10 }}>
                                <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: 4 }}>Duración (min)</label>
                                <input
                                  type="number"
                                  min="5"
                                  max="480"
                                  step="5"
                                  value={meetingDurationForm}
                                  onChange={(e) => setMeetingDurationForm(e.target.value)}
                                  style={{ width: '100px', padding: '6px 10px', borderRadius: 6, border: '1px solid #d9d9d9', fontSize: '0.9rem' }}
                                />
                              </div>
                              <div style={{ display: 'flex', gap: 8 }}>
                                <button
                                  onClick={handleCreateAndJoinEventMeeting}
                                  disabled={meetingLoading}
                                  style={{
                                    display: 'inline-flex', alignItems: 'center', gap: 6,
                                    padding: '8px 16px', background: '#1890ff', color: '#fff',
                                    border: 'none', borderRadius: 6, cursor: 'pointer',
                                    fontWeight: 600, fontSize: '0.9rem'
                                  }}
                                >
                                  <LuVideo size={16} /> {meetingLoading ? 'Creando...' : 'Crear reunión'}
                                </button>
                                <button
                                  onClick={() => setShowMeetingForm(false)}
                                  disabled={meetingLoading}
                                  style={{
                                    padding: '8px 16px', background: '#f0f0f0', color: '#333',
                                    border: '1px solid #d9d9d9', borderRadius: 6, cursor: 'pointer',
                                    fontSize: '0.9rem'
                                  }}
                                >
                                  Cancelar
                                </button>
                              </div>
                            </div>
                          )}
                        </div>
                      )}

                      {/* === SECCIÓN DE GRABACIONES === */}
                      <div style={{ marginTop: 16 }}>
                        <button
                          onClick={handleToggleRecordings}
                          style={{
                            display: 'inline-flex', alignItems: 'center', gap: 6,
                            padding: '8px 16px', background: '#9c27b0', color: '#fff',
                            border: 'none', borderRadius: 6, cursor: 'pointer',
                            fontWeight: 600, fontSize: '0.9rem', marginBottom: '8px'
                          }}
                        >
                          <LuVideo size={16} /> {recordingsOpen ? 'Ocultar Grabaciones' : 'Ver Grabaciones en la Comunidad'}
                        </button>
                        {recordingsOpen && (
                          <div style={{ padding: '12px', background: '#fafafa', borderRadius: 8, border: '1px solid #e0e0e0', marginTop: '8px' }}>
                            <p style={{ fontWeight: 600, fontSize: '0.9rem', margin: '0 0 10px 0', display: 'flex', alignItems: 'center', gap: '6px' }}>
                              Grabaciones Disponibles
                            </p>
                            {recordingsLoading ? (
                              <p style={{ fontSize: '0.85rem', color: '#666' }}>Cargando grabaciones...</p>
                            ) : recordings.length > 0 ? (
                              <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                {recordings.map(rec => (
                                  <li key={rec.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px', background: '#fff', border: '1px solid #f0f0f0', borderRadius: '4px' }}>
                                    <div>
                                      <strong style={{ fontSize: '0.85rem', display: 'block' }}>{rec.topic}</strong>
                                      <span style={{ fontSize: '0.8rem', color: '#888' }}>{new Date(rec.startTime).toLocaleString()} • {rec.duration} min</span>
                                    </div>
                                    <button 
                                      onClick={() => handleDownloadRecording(rec.id)} 
                                      style={{ padding: '4px 12px', fontSize: '0.8rem', cursor: 'pointer', background: '#e6f4ff', color: '#1890ff', border: '1px solid #91caff', borderRadius: '4px' }}
                                    >
                                      Descargar
                                    </button>
                                  </li>
                                ))}
                              </ul>
                            ) : (
                              <p style={{ fontSize: '0.85rem', color: '#999', margin: 0 }}>No hay grabaciones disponibles.</p>
                            )}
                          </div>
                        )}
                      </div>

                    </div>
                  </div>
                ) : (
                  event.ubicacion ? (
                    <div className="ed-detail-item" style={{ alignItems: 'flex-start' }}>
                      <LuMapPin className="ed-detail-icon" style={{ color: '#52c41a', fontSize: '1.3rem', marginTop: 2 }} />
                      <div style={{ flex: 1 }}>
                        <span className="ed-detail-label">Ubicación</span>
                        <span className="ed-detail-value" style={{ fontWeight: 600, fontSize: '1rem' }}>
                          {event.ubicacion.nombre || 'Ubicación'}
                        </span>
                        {event.ubicacion.direccion && (
                          <p style={{ margin: '4px 0 8px 0', color: '#555', fontSize: '0.88rem', lineHeight: 1.4 }}>
                            {event.ubicacion.direccion}
                          </p>
                        )}
                        {event.ubicacion.latitud && event.ubicacion.longitud && (
                          <div className="ed-location-map-wrap">
                            <MapContainer
                              center={[Number(event.ubicacion.latitud), Number(event.ubicacion.longitud)]}
                              zoom={15}
                              scrollWheelZoom={true}
                              className="ed-location-map"
                            >
                              <TileLayer
                                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                              />
                              <Marker
                                position={[Number(event.ubicacion.latitud), Number(event.ubicacion.longitud)]}
                                icon={eventIconRed}
                              >
                                <Popup>
                                  <a
                                    href={`https://www.google.com/maps/search/?api=1&query=${Number(event.ubicacion.latitud)},${Number(event.ubicacion.longitud)}`}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    style={{ color: '#1890ff', textDecoration: 'underline', fontWeight: 600 }}
                                  >
                                    Abrir en Google Maps
                                  </a>
                                </Popup>
                              </Marker>
                            </MapContainer>
                          </div>
                        )}
                      </div>
                    </div>
                  ) : (
                    <div className="ed-detail-item">
                      <LuMapPin className="ed-detail-icon" />
                      <div>
                        <span className="ed-detail-label">Ubicación</span>
                        <span className="ed-detail-value">Por confirmar</span>
                      </div>
                    </div>
                  )
                )}

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

            {/* Tarea de Classroom vinculada */}
            {(event.classroomTaskId || (isOrganizer && event.comunidadId)) && (
              <div className="ed-section">
                <h2 className="ed-section-title">
                  <LuBookOpen className="ed-section-icon" /> Tarea de Google Classroom
                </h2>

                {event.classroomTaskId ? (
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', background: '#f8f9fa', padding: '12px 16px', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                      <div style={{ padding: '8px', background: '#e6f4ff', borderRadius: '6px', color: '#1890ff' }}>
                        <LuBookOpen size={20} />
                      </div>
                      <div>
                        <a href={event.classroomTaskUrl} target="_blank" rel="noopener noreferrer" style={{ fontWeight: '600', color: '#1890ff', textDecoration: 'none', display: 'block' }}>
                          {event.classroomTaskTitle}
                        </a>
                        <span style={{ fontSize: '0.85rem', color: '#64748b' }}>Tarea vinculada a este evento</span>
                      </div>
                    </div>
                    {canEditEvent && !isCancelled && !isStarted && (
                      <button
                        onClick={handleUnlinkTask}
                        disabled={taskLinking}
                        style={{ border: 'none', background: 'transparent', color: '#ef4444', cursor: 'pointer', padding: '6px', display: 'flex', alignItems: 'center', borderRadius: '4px' }}
                        title="Desvincular tarea"
                      >
                        <LuTrash2 size={18} />
                      </button>
                    )}
                  </div>
                ) : (
                  <div style={{ textAlign: 'center', padding: '16px', border: '1px dashed #cbd5e1', borderRadius: '8px' }}>
                    <p style={{ margin: '0 0 12px 0', color: '#64748b', fontSize: '0.9rem' }}>
                      No hay ninguna tarea vinculada a este evento.
                    </p>
                    <button
                      className="ed-btn"
                      onClick={handleOpenTaskModal}
                      style={{ background: '#fff', color: '#1890ff', border: '1px solid #1890ff', margin: '0 auto', display: 'inline-flex', alignItems: 'center', gap: '8px', padding: '8px 16px', borderRadius: '6px', fontWeight: '500', cursor: 'pointer' }}
                    >
                      <LuLink size={18} /> Vincular Tarea
                    </button>
                  </div>
                )}
              </div>
            )}

            {/* Materiales necesarios */}
            {event.queLlevar && (
              <div className="ed-section">
                <h2 className="ed-section-title">
                  <LuPackage className="ed-section-icon" /> Materiales necesarios
                </h2>
                <div className="ed-materials">
                  {event.queLlevar.split(',').filter(m => m.trim() !== '').map((material, index) => (
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

                {isFull && !isConfirmed && !isStarted && (
                  <div className="ed-full-message">
                    <LuUsers /> Aforo completo
                  </div>
                )}

                {!isMember && currentUserId && !isConfirmed && !isStarted && (
                  <div className="ed-full-message">
                    Debes ser miembro de la comunidad para apuntarte
                  </div>
                )}

                {isConfirmed ? (
                  <div className="ed-attendance-actions">
                    <div className="ed-confirmed-badge">
                      <LuCheck /> Asistencia confirmada
                    </div>
                    {isOrganizer ? (
                      <span style={{ fontSize: '0.85rem', color: '#888' }}>Eres el organizador de este evento</span>
                    ) : !isEnded ? (
                      <button
                        className="ed-btn ed-btn-cancel-attendance"
                        onClick={handleCancelAttendance}
                        disabled={attendanceLoading}
                      >
                        {attendanceLoading ? 'Cancelando...' : 'Cancelar asistencia'}
                      </button>
                    ) : null}
                  </div>
                ) : !isStarted ? (
                  <button
                    className="ed-btn ed-btn-attend"
                    onClick={handleAttend}
                    disabled={attendanceLoading || isFull || !currentUserId || !isMember}
                    title={!currentUserId ? 'Inicia sesión para confirmar asistencia' : !isMember ? 'Debes ser miembro de la comunidad' : ''}
                  >
                    {attendanceLoading ? 'Confirmando...' : 'Confirmar asistencia'}
                  </button>
                ) : null}

                {/* Alarmas rápidas (solo si confirmado) */}
                {isConfirmed && currentUserId && (
                  <div className="ed-alarms-inline">
                    <div className="ed-alarms-inline__header">
                      <span><LuBell style={{ verticalAlign: 'middle', marginRight: 4 }} />Mis alarmas</span>
                    </div>
                    {alarms.length > 0 ? (
                      <ul className="ed-alarms-list">
                        {alarms.map(alarm => (
                          <li key={alarm.id} className={`ed-alarm-item ${alarm.disparada ? 'ed-alarm-item--fired' : ''}`}>
                            <span className="ed-alarm-label">{formatAlarmLabel(alarm.minutosAntes)}</span>
                            <span className="ed-alarm-canal">{CANAL_LABELS[alarm.canal] || alarm.canal}</span>
                            {!alarm.disparada && (
                              <button
                                className="ed-alarm-delete"
                                onClick={() => handleDeleteAlarm(alarm.id)}
                                disabled={alarmsLoading}
                                title="Eliminar alarma"
                              >
                                <LuTrash2 />
                              </button>
                            )}
                          </li>
                        ))}
                      </ul>
                    ) : (
                      <p className="ed-alarms-empty">Sin alarmas configuradas</p>
                    )}
                    <div className="ed-alarm-add">
                      <select
                        className="ed-alarm-select"
                        value={addAlarmMinutos}
                        onChange={e => setAddAlarmMinutos(Number(e.target.value))}
                      >
                        {OPCIONES_ANTELACION.map(o => (
                          <option key={o.value} value={o.value}>{o.label}</option>
                        ))}
                      </select>
                      <select
                        className="ed-alarm-select"
                        value={addAlarmCanal}
                        onChange={e => setAddAlarmCanal(e.target.value)}
                      >
                        {Object.entries(CANAL_LABELS).map(([k, v]) => (
                          <option key={k} value={k}>{v}</option>
                        ))}
                      </select>
                      <button
                        className="ed-alarm-add-btn"
                        onClick={handleAddAlarm}
                        disabled={addAlarmLoading}
                      >
                        {addAlarmLoading ? '...' : '+ Añadir'}
                      </button>
                    </div>
                  </div>
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
                    const participantPhoto = getUserPhoto(user);
                    return (
                      <li key={att.id || user.id} className="ed-participant">
                        <div className="ed-participant-avatar">
                          {participantPhoto ? (
                            <img src={toAbsoluteImageUrl(participantPhoto)} alt={user.nombre || user.username} />
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

            {/* Formulario de valoración tras evento finalizado (solo si el creador es PROFESOR en la comunidad y soy asistente confirmado) */}
            {!isOrganizer && isConfirmed && creadorEsProfesorEnComunidad && isStarted && !isCancelled && (
              valorado ? (
                <div className="ed-rating-success">¡Gracias por valorar al profesor!</div>
              ) : buscandoTutor ? (
                <div className="ed-rating-card">
                  <h3 className="ed-card-title">Buscando tutor...</h3>
                  <div className="ed-rating-error">Buscando el tutor asociado al organizador del evento...</div>
                </div>
              ) : realTutorId ? (
                <div className="ed-rating-card">
                  <h3 className="ed-card-title">Valora al profesor</h3>
                  <RatingForm
                    profesorId={realTutorId}
                    alumnoId={currentUserId}
                    eventoId={eventId}
                    onValorado={() => setValorado(true)}
                    alreadyRated={valorado}
                  />
                </div>
              ) : null
            )}
          </div>
        </div>
      </div>

      {/* Modal de confirmación de asistencia con alarmas */}
      {showAttendModal && (
        <div className="ed-modal-overlay" onClick={() => setShowAttendModal(false)}>
          <div className="ed-modal" onClick={e => e.stopPropagation()}>
            <h2 className="ed-modal-title">Confirmar asistencia</h2>
            <p className="ed-modal-text">
              ¿Quieres recibir alarmas para recordarte este evento?
            </p>

            <div className="ed-attend-checkboxes">
              {OPCIONES_ANTELACION.map(o => (
                <label key={o.value} className="ed-attend-check-label">
                  <input
                    type="checkbox"
                    checked={selectedMinutos.includes(o.value)}
                    onChange={() => toggleMinuto(o.value)}
                  />
                  {o.label}
                </label>
              ))}
            </div>

            {selectedMinutos.length > 0 && (
              <div className="ed-modal-field" style={{ marginBottom: '1rem' }}>
                <label className="ed-modal-label">Canal de notificación</label>
                <div className="ed-canal-radios">
                  {Object.entries(CANAL_LABELS).map(([k, v]) => (
                    <label key={k} className="ed-canal-radio-label">
                      <input
                        type="radio"
                        name="canal"
                        value={k}
                        checked={selectedCanal === k}
                        onChange={() => setSelectedCanal(k)}
                      />
                      {v}
                    </label>
                  ))}
                </div>
              </div>
            )}

            <div className="ed-modal-actions">
              <button
                className="ed-btn ed-btn-secondary"
                onClick={() => setShowAttendModal(false)}
                disabled={attendanceLoading}
              >
                Cancelar
              </button>
              <button
                className="ed-btn ed-btn-attend"
                style={{ width: 'auto', padding: '0.75rem 1.5rem' }}
                onClick={handleConfirmAttend}
                disabled={attendanceLoading}
              >
                {attendanceLoading
                  ? 'Confirmando...'
                  : selectedMinutos.length > 0
                    ? 'Confirmar con alarmas'
                    : 'Confirmar asistencia'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal de Tareas de Classroom */}
      {showTaskModal && (
        <div className="ed-modal-overlay" onClick={() => setShowTaskModal(false)}>
          <div className="ed-modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '500px' }}>
            <h2 className="ed-modal-title">Vincular tarea de Classroom</h2>
            <p className="ed-modal-text">
              Selecciona una tarea del curso asociado a la comunidad para vincularla al evento.
            </p>

            {taskError === 'missing_scopes' ? (
              <div style={{ textAlign: 'center', padding: '20px', background: '#fef2f2', border: '1px solid #fca5a5', borderRadius: '8px', marginBottom: '20px' }}>
                <p style={{ color: '#b91c1c', marginBottom: '16px', fontWeight: '500', fontSize: '0.95rem' }}>
                  No tienes los permisos necesarios para leer las tareas de Google Classroom.
                </p>
                <button
                  className="ed-btn"
                  onClick={async () => {
                    try {
                      const width = 600, height = 700;
                      const left = window.screenX + (window.innerWidth - width) / 2;
                      const top = window.screenY + (window.innerHeight - height) / 2;
                      const token = localStorage.getItem('accessToken');
                      const resp = await fetch(`${getApiBaseUrl()}/oauth2/authorize/google-classroom-url?communityId=${event.comunidadId}`, {
                        headers: { Authorization: `Bearer ${token}` }
                      });
                      const data = await resp.json();
                      if (resp.ok) {
                        window.open(data.url, 'google_classroom_tasks', `width=${width},height=${height},left=${left},top=${top}`);
                      }
                    } catch (e) {
                      console.error("Error obteniendo URL", e);
                    }
                  }}
                  style={{ background: '#1890ff', color: '#fff', border: 'none', margin: '0 auto', display: 'flex', alignItems: 'center', gap: '8px', padding: '10px 16px', borderRadius: '6px' }}
                >
                  <LuBookOpen size={18} /> Conceder permisos
                </button>
              </div>
            ) : taskError ? (
              <div className="ed-error" style={{ marginBottom: '16px' }}>{taskError}</div>
            ) : null}

            {taskError !== 'missing_scopes' && (
              <div style={{ maxHeight: '300px', overflowY: 'auto', marginBottom: '20px' }}>
                {tasksLoading ? (
                  <p style={{ textAlign: 'center', color: '#888', padding: '20px 0' }}>Cargando tareas...</p>
                ) : classroomTasks.length > 0 ? (
                  <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    {classroomTasks.map(task => (
                      <li key={task.id}>
                        <button
                          onClick={() => handleLinkTask(task)}
                          disabled={taskLinking}
                          style={{
                            width: '100%', textAlign: 'left', padding: '12px', background: '#f8f9fa',
                            border: '1px solid #e9ecef', borderRadius: '6px', cursor: taskLinking ? 'not-allowed' : 'pointer',
                            display: 'flex', alignItems: 'center', gap: '10px', transition: 'background 0.2s',
                            fontSize: '0.95rem'
                          }}
                          onMouseOver={(e) => e.currentTarget.style.background = '#e2e8f0'}
                          onMouseOut={(e) => e.currentTarget.style.background = '#f8f9fa'}
                        >
                          <LuBookOpen style={{ color: '#1890ff', minWidth: '18px' }} size={18} />
                          <span style={{ fontWeight: 500, color: '#334155' }}>{task.title}</span>
                        </button>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p style={{ textAlign: 'center', color: '#888', padding: '20px 0' }}>No se encontraron tareas en este curso.</p>
                )}
              </div>
            )}

            <div className="ed-modal-actions">
              <button
                className="ed-btn ed-btn-secondary"
                onClick={() => setShowTaskModal(false)}
                disabled={taskLinking}
              >
                Cancelar
              </button>
            </div>
          </div>
        </div>
      )}

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

export default DetalleEvento;

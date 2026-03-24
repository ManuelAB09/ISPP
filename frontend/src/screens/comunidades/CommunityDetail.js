import { useCallback, useEffect, useRef, useState } from 'react';
import {
    LuArrowLeft,
    LuCalendar,
    LuCheck,
    LuChevronDown,
    LuChevronUp,
    LuLogIn,
    LuLogOut,
    LuPencil,
    LuPlay,
    LuPlus,
    LuTrash2,
    LuUserPlus,
    LuUsers,
    LuVideo,
    LuX,
} from 'react-icons/lu';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import axiosInstance from '../../api/axiosConfig';
import { getApiBaseUrl } from '../../api/baseUrl';
import { communitiesApi } from '../../api/communities.api';
import { cuestionariosApi } from '../../api/cuestionarios.api';
import { attendEvent, cancelAttendance, getMyAttendance, listCommunityEvents } from '../../api/eventEndpoints';
import { ZoomApi } from '../../api/zoom.api';
import EditCommunityModal from '../../components/Comunidad/EditCommunityModal';
import TransferAdminModal from '../../components/Comunidad/TransferAdminModal';
import TarjetaEvento from '../../components/Evento/TarjetaEvento';
import GoogleClassroomButton from '../../components/GoogleClassroomButton/GoogleClassroomButton';
import Header from '../../components/Header/Header';
import { useAuth } from '../../contexts/AuthContext';
import { useSocketContext } from '../../contexts/SocketContext';
import {
    canCreateCommunityEvent,
    getCommunityRoleCapabilities,
    getCommunityRoleLabel,
    isAdminRole,
    isTeacherRole,
    normalizeCommunityRole,
} from '../../utils/communityRoles';
import CommunityChat from '../chat/CommunityChat';
import CommunityAnnouncementsTab from './CommunityAnnouncementsTab';
import './CommunityDetail.css';

const DEFAULT_MEMBER_AVATAR = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 80 80'%3E%3Ccircle cx='40' cy='40' r='40' fill='%23E6EAF3'/%3E%3Ccircle cx='40' cy='30' r='14' fill='%2395A1BB'/%3E%3Cpath d='M14 68c5-13 15-21 26-21s21 8 26 21' fill='%2395A1BB'/%3E%3C/svg%3E";

const toAbsoluteImageUrl = (imageUrl, fallback = DEFAULT_MEMBER_AVATAR) => {
  if (!imageUrl || !String(imageUrl).trim()) {
    return fallback;
  }

  const value = String(imageUrl).trim();
  if (/^https?:\/\//i.test(value) || value.startsWith('data:') || value.startsWith('blob:')) {
    return value;
  }

  const base = getApiBaseUrl();

  if (value.startsWith('/')) {
    return `${base}${value}`;
  }

  return `${base}/${value}`;
};

const OPCIONES_ANTELACION = [
  { label: '2 días antes', value: 2880 },
  { label: '1 día antes', value: 1440 },
  { label: '2 horas antes', value: 120 },
  { label: '1 hora antes', value: 60 },
  { label: '30 minutos antes', value: 30 },
];

const CANAL_LABELS = { PLATAFORMA: 'Solo en la app', EMAIL: 'Solo por email', AMBOS: 'Ambos' };

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
  // Estado para alternar entre pestaña de eventos y anuncios
  const [searchParams] = useSearchParams();
  const initialTab = searchParams.get('tab');
  const [showAnnouncementsTab, setShowAnnouncementsTab] = useState(initialTab === 'anuncios');
  const { communityId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { socket } = useSocketContext();
  const openChatOnLoad = searchParams.get('chat') === 'open';

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
  const [membersOpen, setMembersOpen] = useState(false);
  const [membersLoading, setMembersLoading] = useState(false);
  const [membersError, setMembersError] = useState(null);
  const [members, setMembers] = useState([]);
  const [promotingMemberId, setPromotingMemberId] = useState(null);
  const [membersActionError, setMembersActionError] = useState(null);
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
  const [communityCuestionarios, setCommunityCuestionarios] = useState([]);
  const [cuestionariosLoading, setCuestionariosLoading] = useState(false);
  const [cuestionariosError, setCuestionariosError] = useState(null);
  const [expellingMemberId, setExpellingMemberId] = useState(null);
  const [memberToast, setMemberToast] = useState(null);
  const [showJoinRoleChooser, setShowJoinRoleChooser] = useState(false);
  const [chatOpen, setChatOpen] = useState(openChatOnLoad);
  const [ranking, setRanking] = useState([]);
  const [rankingLoading, setRankingLoading] = useState(false);
  const [rankingError, setRankingError] = useState(null);
  const fileInputRef = useRef(null);
  const activeMeetingRequestInFlightRef = useRef(false);

  const closeAllOverlays = useCallback(({ keepChat = false } = {}) => {
    setShowMeetingForm(false);
    setParticipantsOpen(false);
    setMeetingsOpen(false);
    setRecordingsOpen(false);
    setSelectedRecordingMeetingId(null);
    setShowEditModal(false);
    setShowTransferModal(false);
    if (!keepChat) {
      setChatOpen(false);
    }
  }, []);

  const isPrivate = community?.tipoGrupo === 'GRUPO_PRIVADO';
  const isCorporateCommunity = String(community?.tipoPlan || '').toUpperCase() === 'UNLIMITED';
  const normalizedRole = normalizeCommunityRole(community?.miRol);
  const normalizedRolDocente = community?.miRolDocente || null;
  const isAdmin = isAdminRole(normalizedRole);
  const isTeacher = isTeacherRole(normalizedRole, normalizedRolDocente);
  const canCreateEvent = canCreateCommunityEvent(normalizedRole, normalizedRolDocente);
  const roleLabel = getCommunityRoleLabel(normalizedRole, normalizedRolDocente);
  const roleCapabilities = getCommunityRoleCapabilities(normalizedRole, normalizedRolDocente);
  const currentUserId = localStorage.getItem('userId');
  const hasTeacherProfile = Boolean(user?.esTutor || user?.esProfesor);
  const currentUser = {
    id: Number(currentUserId),
    nombre: user?.nombre || 'Usuario',
    foto: user?.foto || null,
    fotoBackgroundColor: user?.fotoBackgroundColor || '#ffffff',
  };
  const groupedMembers = members.reduce((acc, member) => {
    const role = normalizeCommunityRole(member?.rol);
    const key = role || 'OTROS';
    if (!acc[key]) acc[key] = [];
    acc[key].push(member);
    return acc;
  }, {});
  const adminMembers = groupedMembers.ADMIN || [];
  const adminProfesores = adminMembers.filter(
    (m) => normalizeCommunityRole(m?.rolDocente) === 'PROFESOR'
  );
  const teacherMembers = [...(groupedMembers.PROFESOR || []), ...adminProfesores];
  const studentMembers = [...(groupedMembers.ALUMNO || []), ...(groupedMembers.MIEMBRO || [])];

  const formatPlanLabel = (plan) => {
    switch (String(plan || '').toUpperCase()) {
      case 'CORPORATIVO':
      case 'INSTITUTIONAL':
      case 'EMPRESARIAL':
        return 'Plan corporativo';
      case 'PREMIUM':
        return 'Plan premium';
      case 'FREE':
      case 'GRATUITO':
        return 'Plan gratuito';
      default:
        return plan ? `Plan ${String(plan).toLowerCase()}` : null;
    }
  };

  const getMemberName = (member) => member?.usuario?.nombre || member?.nombre || 'Usuario';
  const getMemberId = (member) => member?.usuario?.id || member?.id || getMemberName(member);
  const getMemberPhoto = (member) => {
    const userData = member?.usuario || member;

    if (!userData || typeof userData !== 'object') {
      return null;
    }

    return (
      userData.foto
      || userData.avatarUrl
      || userData.fotoUrl
      || userData.fotoPerfil
      || userData.avatar
      || userData.imagen
      || userData.image
      || userData.usuarioFoto
      || null
    );
  };
  const getRankingUserPhoto = (rankingItem) => {
    const userData = rankingItem?.usuario || rankingItem;

    if (!userData || typeof userData !== 'object') {
      return null;
    }

    return (
      userData.avatarUrl
      || userData.foto
      || userData.fotoUrl
      || userData.fotoPerfil
      || userData.avatar
      || userData.imagen
      || userData.image
      || userData.usuarioFoto
      || null
    );
  };
  const getMemberInitial = (member) => getMemberName(member).trim().charAt(0).toUpperCase() || 'U';
  const canExpelMember = (member) => isAdmin && String(getMemberId(member)) !== String(currentUserId);
  const handleOpenMemberProfile = (member) => {
    const memberId = getMemberId(member);
    if (!memberId) return;
    navigate(`/perfil/${memberId}`);
  };

  useEffect(() => {
    if (!memberToast) {
      return undefined;
    }

    const timeoutId = window.setTimeout(() => {
      setMemberToast(null);
    }, 3200);

    return () => window.clearTimeout(timeoutId);
  }, [memberToast]);

  const handleMakeAdmin = async (member) => {
    const memberId = getMemberId(member);
    const memberName = getMemberName(member);
    if (!memberId) return;
    if (!isCorporateCommunity) {
      setMemberToast({
        type: 'error',
        message: 'Solo se pueden añadir administradores en comunidades corporativas.',
      });
      return;
    }
    if (!window.confirm(`¿Seguro que quieres hacer a ${memberName} administrador?`)) {
      return;
    }
    try {
      setMembersLoading(true);
      await communitiesApi.addAdmin(communityId, memberId);
      setMemberToast({ type: 'success', message: `${memberName} es ahora administrador.` });
      await fetchMembers();
    } catch (err) {
      console.error('Error al hacer administrador:', err);
      setMemberToast({ type: 'error', message: err?.message || 'No se pudo hacer administrador al miembro.' });
    } finally {
      setMembersLoading(false);
    }
  };

  const renderMemberPills = (list, emptyMessage) => {
    if (!list.length) {
      return <p className="cd-role-empty">{emptyMessage}</p>;
    }

    return (
      <div className="cd-member-pills">
        {list.map((member) => (
          <div key={getMemberId(member)} className="cd-member-pill">
            <button
              type="button"
              className="cd-member-link"
              onClick={() => handleOpenMemberProfile(member)}
            >
              <span className="cd-member-avatar">
                {getMemberPhoto(member) ? (
                  <img src={toAbsoluteImageUrl(getMemberPhoto(member))} alt={getMemberName(member)} />
                ) : (
                  <span className="cd-member-avatar-fallback">{getMemberInitial(member)}</span>
                )}
              </span>
              <span className="cd-member-info">
                <span className="cd-member-name">{getMemberName(member)}</span>
                <span className="cd-member-role">{getCommunityRoleLabel(member?.rol, member?.rolDocente)}</span>
              </span>
            </button>
            <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
              {isAdmin && isCorporateCommunity && normalizeCommunityRole(member?.rol) !== 'ADMIN' && (
                <button
                  type="button"
                  className="cd-member-remove"
                  style={{ backgroundColor: '#eef2ff', color: '#4f46e5', borderColor: '#eef2ff' }}
                  onClick={() => handleMakeAdmin(member)}
                >
                  Hacer administrador
                </button>
              )}
              {canExpelMember(member) && (
                <button
                  type="button"
                  className="cd-member-remove"
                  onClick={() => handleExpelMember(member)}
                  disabled={expellingMemberId === getMemberId(member)}
                >
                  {expellingMemberId === getMemberId(member) ? 'Expulsando...' : 'Expulsar'}
                </button>
              )}
            </div>
          </div>
        ))}
      </div>
    );
  };
  const communityImage = (() => {
    const raw = community?.imagen || community?.imagenUrl || community?.foto;
    if (!raw || !String(raw).trim() || String(raw).trim().toLowerCase() === 'empty') {
      return 'https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&w=400&q=80';
    }
    const value = String(raw).trim();
    if (/^https?:\/\//i.test(value) || value.startsWith('data:image/')) {
      return value;
    }
    const base = getApiBaseUrl();
    return value.startsWith('/') ? `${base}${value}` : `${base}/${value}`;
  })();

  const fetchCommunity = useCallback(async () => {
    try {
      setLoading(true);
      let data = await communitiesApi.getById(communityId);
      // Use esMiembro from backend response if available
      if (data.esMiembro !== undefined) {
        setIsMember(data.esMiembro);
      } else if (currentUserId) {
        // Fallback: check membership via API
        try {
          const membership = await communitiesApi.getMyMembership(communityId);
          if (membership) {
            data = {
              ...data,
              esMiembro: true,
              miRol: data.miRol || membership.rol,
            };
          }
          setIsMember(true);
        } catch {
          setIsMember(false);
        }
      }

      setCommunity(data);

      // Si es comunidad privada y el usuario no es miembro, comprobar solicitud pendiente
      if (data.tipoGrupo === 'GRUPO_PRIVADO' && !data.esMiembro && currentUserId) {
        try {
          const status = await communitiesApi.getMyRequestStatus(communityId);
          if (status && status.pending) {
            setRequestSent(true);
          } else if (status && !status.pending) {
            setRequestSent(false);
          }
          // Si status es undefined o null, no cambiar estado local (mantener lo que había)
        } catch (err) {
          // Error al verificar: mantener estado local en lugar de asumir false
          console.warn('Aviso: No se pudo verificar solicitud pendiente', err);
        }
      } else if (!data.esMiembro) {
        // No es privada pero tampoco es miembro: seguro no hay solicitud
        setRequestSent(false);
      }
    } catch (err) {
      console.error('Error al cargar la comunidad:', err);
      setError('No se pudo cargar la comunidad.');
    } finally {
      setLoading(false);
    }
  }, [communityId, currentUserId]);

  const fetchMembers = useCallback(async () => {
    try {
      setMembersLoading(true);
      const data = await communitiesApi.getMembers(communityId, { page: 0, size: 100 });
      const list = data?.content || data?.miembros || data || [];
      setMembers(Array.isArray(list) ? list : []);
    } catch (err) {
      console.error('Error al cargar miembros:', err);
      setMembers([]);
    } finally {
      setMembersLoading(false);
    }
  }, [communityId]);

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
      if (!communityId) return;
    
      const load = async () => {
        try {
          setLoading(true);
          const data = await communitiesApi.getById(communityId);
          setCommunity(data);
        
          if (data.esMiembro !== undefined) {
            setIsMember(data.esMiembro);
          } else if (currentUserId) {
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
              setRequestSent(status && status.pending);
            } catch (err) {
              console.warn('No se pudo verificar solicitud pendiente', err);
            }
          } else if (!data.esMiembro) {
            setRequestSent(false);
          }
        } catch (err) {
          console.error('Error al cargar la comunidad:', err);
          setError('No se pudo cargar la comunidad.');
        } finally {
          setLoading(false);
        }
      };
    
      load();
    fetchEvents();
    }, [communityId, currentUserId, fetchEvents]);

  useEffect(() => {
    fetchMembers();
  }, [fetchMembers]);

  useEffect(() => {
    if (!currentUserId) {
      setCommunityCuestionarios([]);
      setCuestionariosLoading(false);
      setCuestionariosError(null);
      return;
    }

    const fetchCommunityCuestionarios = async () => {
      setCuestionariosLoading(true);
      setCuestionariosError(null);
      try {
        const data = await cuestionariosApi.listByCommunity(communityId);
        setCommunityCuestionarios(Array.isArray(data) ? data : []);
      } catch (err) {
        console.error('Error al cargar cuestionarios de la comunidad:', err);
        setCommunityCuestionarios([]);
        setCuestionariosError(err?.response?.data?.message || err?.message || 'No se pudieron cargar los cuestionarios');
      } finally {
        setCuestionariosLoading(false);
      }
    };

    fetchCommunityCuestionarios();
  }, [communityId, currentUserId]);

  const fetchRanking = useCallback(async () => {
    if (!isMember) {
      setRanking([]);
      return;
    }

    try {
      setRankingLoading(true);
      setRankingError(null);
      const data = await communitiesApi.getRanking(communityId);
      setRanking(Array.isArray(data) ? data : (data?.content || []));
    } catch (err) {
      console.error('Error al cargar ranking:', err);
      setRankingError('No se pudo cargar el ranking.');
      setRanking([]);
    } finally {
      setRankingLoading(false);
    }
  }, [communityId, isMember]);

  useEffect(() => {
    if (isMember) {
      fetchRanking();
    }
  }, [fetchRanking, isMember]);

  // Modal de alarmas al confirmar asistencia
  const [showAttendModal, setShowAttendModal] = useState(false);
  const [pendingAttendEventId, setPendingAttendEventId] = useState(null);
  const [selectedMinutos, setSelectedMinutos] = useState([]);
  const [selectedCanal, setSelectedCanal] = useState('AMBOS');

  const toggleMinuto = (value) => {
    setSelectedMinutos(prev =>
      prev.includes(value) ? prev.filter(v => v !== value) : [...prev, value]
    );
  };

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
    if (!currentUserId || !isMember || !communityId) {
      return undefined;
    }

    const topic = `/topic/community.${communityId}.meeting`;
    const handler = (data) => {
      if (!data || data === '') {
        setActiveMeeting(null);
      } else {
        setActiveMeeting(data);
      }
    };
    if (!socket) return undefined;
    socket.on(topic, handler);
    return () => socket.off(topic, handler);
  }, [socket, communityId, currentUserId, isMember]);

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

  const getMemberRoleLabel = (member) => {
    const rawRole = String(member?.rol || member?.role || '').toUpperCase();
    const rawRolDocente = member?.rolDocente ? String(member.rolDocente).toUpperCase() : null;
    if (rawRole === 'ADMIN') {
      if (rawRolDocente === 'PROFESOR') return 'Administrador · Profesor';
      if (rawRolDocente === 'ALUMNO') return 'Administrador · Alumno';
      return 'Administrador';
    }
    if (rawRole === 'MODERADOR' || rawRole === 'MODERATOR') return 'Moderador';
    if (rawRole === 'MIEMBRO' || rawRole === 'MEMBER') return 'Miembro';
    if (rawRole === 'ALUMNO' || rawRole === 'STUDENT') return 'Alumno';
    if (rawRole === 'PROFESOR' || rawRole === 'TEACHER') return 'Profesor';
    return rawRole || 'Miembro';
  };

  const getMemberUserId = (member) => {
    return member?.usuario?.id || member?.user?.id || member?.usuarioId || member?.userId;
  };

  const isMemberAdmin = (member) => {
    const rawRole = String(member?.rol || member?.role || '').toUpperCase();
    return rawRole === 'ADMIN';
  };

  const handleToggleMembers = async () => {
    if (membersOpen) {
      setMembersOpen(false);
      return;
    }

    setMembersOpen(true);
    setMembersActionError(null);
    if (!members.length && !membersLoading) {
      await fetchMembers();
    }
  };

  const handlePromoteMember = async (member) => {
    const memberUserId = getMemberUserId(member);
    if (!memberUserId) {
      setMembersActionError('No se pudo identificar al miembro seleccionado.');
      return;
    }

    try {
      setPromotingMemberId(memberUserId);
      setMembersActionError(null);
      await communitiesApi.promoteMemberToAdmin(communityId, memberUserId);
      setMembers((prev) =>
        prev.map((item) =>
          String(getMemberUserId(item)) === String(memberUserId)
            ? { ...item, rol: 'ADMIN' }
            : item
        )
      );
      await fetchCommunity();
    } catch (err) {
      console.error('Error al promover miembro a admin:', err);
      setMembersActionError(err?.message || 'No se pudo promover al miembro a admin.');
    } finally {
      setPromotingMemberId(null);
    }
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

    if (showMeetingForm) {
      setShowMeetingForm(false);
      return;
    }

    closeAllOverlays();
    setMeetingError(null);
    setShowMeetingForm(true);
  };

  const handleToggleParticipants = async () => {
    if (participantsOpen) {
      setParticipantsOpen(false);
      return;
    }

    closeAllOverlays();
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

    closeAllOverlays();

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
    setMembersOpen(false);
    setMembersError(null);
    setMembers([]);
  }, [communityId]);

  const handleOpenEditModal = () => {
    closeAllOverlays();
    setShowEditModal(true);
  };

  const handleOpenTransferModal = () => {
    closeAllOverlays();
    setShowTransferModal(true);
  };

  const handleChatOpenChange = (nextOpen) => {
    if (nextOpen) {
      closeAllOverlays({ keepChat: true });
    }
    setChatOpen(nextOpen);
  };

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
    setSelectedMinutos([]);
    setSelectedCanal('AMBOS');
    setPendingAttendEventId(eventId);
    setShowAttendModal(true);
  };

  const handleConfirmAttend = async () => {
    const eventId = pendingAttendEventId;
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
      setEvents(prev => prev.map(ev =>
        ev.id === eventId
          ? { ...ev, miAsistencia: 'CONFIRMADA', asistentesConfirmados: (ev.asistentesConfirmados || 0) + 1 }
          : ev
      ));
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
      const status = err?.response?.status;
      if (status === 403) {
        alert('El organizador del evento no puede cancelar su asistencia. Cancela el evento en su lugar.');
      }
    } finally {
      setAttendanceLoading(false);
    }
  };

  const handleJoinCommunity = async () => {
    if (!currentUserId) {
      navigate('/login');
      return;
    }

    if (hasTeacherProfile) {
      setShowJoinRoleChooser(true);
      setMembershipError(null);
      return;
    }

    await handleJoinWithRole('ALUMNO');
  };

  const handleJoinWithRole = async (role) => {
    if (!currentUserId) {
      navigate('/login');
      return;
    }

    try {
      setJoinLoading(true);
      setMembershipError(null);
      if (isPrivate) {
        await communitiesApi.requestAccess(communityId, '', role || 'ALUMNO');
        // Refrescar estado desde servidor para asegurar sincronización
        const status = await communitiesApi.getMyRequestStatus(communityId);
        setRequestSent(status && status.pending);
        setShowJoinRoleChooser(false);
      } else {
        await communitiesApi.join(communityId, role || 'ALUMNO');
        setIsMember(true);
        setShowJoinRoleChooser(false);
        await fetchCommunity();
        await fetchEvents();
      }
    } catch (err) {
      console.error('Error al unirse/solicitar acceso:', err);
      if (err.status === 401 || err.message?.includes('401')) {
        navigate('/login');
      } else if (err.status === 409 || err.message?.includes('409')) {
        setIsMember(true);
        setShowJoinRoleChooser(false);
      } else if (err.status === 400) {
        if (isPrivate) {
          // Aún así, verificar si existe solicitud pendiente en BD
          try {
            const status = await communitiesApi.getMyRequestStatus(communityId);
            setRequestSent(status && status.pending);
          } catch {
            // Si falla, asumir que sí hay solicitud
            setRequestSent(true);
          }
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
      // Cualquier persona que abandone la comunidad será redirigida a la lista de comunidades
      
      navigate('/comunidades');
      
      setIsMember(false);
      await fetchCommunity(); // Refresh member count
      await fetchEvents(); // Refresh events to hide private events
    } catch (err) {
      console.error('Error al abandonar la comunidad:', err);
      const status = err.status || err.response?.status;
      if (status === 409) {
        setMembershipError('No puedes abandonar la comunidad mientras tengas asistencia confirmada a eventos activos. Cancela tu asistencia primero.');
      } else if (status === 400) {
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

  const handleExpelMember = async (member) => {
    const memberId = getMemberId(member);
    const memberName = getMemberName(member);

    if (!memberId) {
      return;
    }

    if (!window.confirm(`¿Seguro que quieres expulsar a ${memberName} de esta comunidad?`)) {
      return;
    }

    try {
      setExpellingMemberId(memberId);
      setMemberToast(null);
      await communitiesApi.expelMember(communityId, memberId);
      setMembers((prev) => prev.filter((currentMember) => String(getMemberId(currentMember)) !== String(memberId)));
      setMemberToast({ type: 'success', message: `${memberName} ha sido expulsado de la comunidad.` });
      await fetchCommunity();
    } catch (err) {
      console.error('Error al expulsar miembro:', err);
      const status = err?.status || err?.response?.status;
      if (status === 403) {
        setMemberToast({ type: 'error', message: 'Solo los administradores pueden expulsar miembros.' });
      } else if (status === 404) {
        setMemberToast({ type: 'error', message: 'No se pudo expulsar al miembro seleccionado.' });
      } else {
        setMemberToast({ type: 'error', message: err?.message || 'No se pudo expulsar al miembro.' });
      }
    } finally {
      setExpellingMemberId(null);
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
        {memberToast && (
          <div
            className={`cd-toast cd-toast--${memberToast.type}`}
            role="status"
            aria-live="polite"
          >
            {memberToast.message}
          </div>
        )}
        <button className="cd-back-btn" onClick={() => navigate('/comunidades')}>
          <LuArrowLeft /> Volver a comunidades
        </button>

        <input
          ref={fileInputRef}
          data-testid="recording-file-input"
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
                {community?.miRol && (
                  <span className="cd-role-chip cd-role-chip--role">
                    Tu rol: {roleLabel}
                  </span>
                )}
                {formatPlanLabel(community?.tipoPlan) && (
                  <span className="cd-role-chip cd-role-chip--plan">
                    {formatPlanLabel(community?.tipoPlan)}
                  </span>
                )}
                <span className="cd-role-chip cd-role-chip--capacity">
                  Aforo {community?.maxMiembros ? `hasta ${community.maxMiembros}` : 'sin límite'}
                </span>
              </div>
              <div className="cd-members-dropdown">
                <button
                  type="button"
                  className="cd-members-toggle"
                  onClick={handleToggleMembers}
                  aria-expanded={membersOpen}
                  aria-controls="cd-members-list-panel"
                >
                  <span className="cd-members-toggle-left">
                    <LuUsers />
                    {membersOpen ? 'Ocultar listado de miembros' : 'Ver listado de miembros'}
                  </span>
                  <span className="cd-members-toggle-right">
                    {membersOpen ? <LuChevronUp /> : <LuChevronDown />}
                  </span>
                </button>

                {membersOpen && (
                  <div id="cd-members-list-panel" className="cd-members-panel">
                    {membersActionError ? (
                      <p className="cd-members-panel-state cd-members-panel-error">{membersActionError}</p>
                    ) : null}
                    {membersLoading ? (
                      <p className="cd-members-panel-state">Cargando miembros...</p>
                    ) : membersError ? (
                      <p className="cd-members-panel-state cd-members-panel-error">{membersError}</p>
                    ) : members.length > 0 ? (
                      <ul className="cd-members-list">
                        {members.map((member, index) => {
                          const memberUserId = getMemberUserId(member);
                          const isCurrentMember = currentUserId && memberUserId && String(memberUserId) === String(currentUserId);
                          const canPromote = isAdmin && !isCurrentMember && !isMemberAdmin(member);

                          return (
                            <li key={member?.id || memberUserId || member?.usuario?.email || member?.email || index} className="cd-members-list-item">
                              <span className="cd-members-list-name">
                                {getMemberName(member, index)}
                                {isCurrentMember ? <strong className="cd-members-you-badge">Tu</strong> : null}
                              </span>
                              <div className="cd-members-list-actions">
                                <span className="cd-members-list-role">{getMemberRoleLabel(member)}</span>
                                {canPromote ? (
                                  <button
                                    type="button"
                                    className="cd-members-promote-btn"
                                    onClick={() => handlePromoteMember(member)}
                                    disabled={promotingMemberId === memberUserId}
                                  >
                                    {promotingMemberId === memberUserId ? 'Promoviendo...' : 'Hacer admin'}
                                  </button>
                                ) : null}
                              </div>
                            </li>
                          );
                        })}
                      </ul>
                    ) : (
                      <p className="cd-members-panel-state">Aún no hay miembros registrados.</p>
                    )}
                  </div>
                )}
              </div>
              {/* Join / Leave / Request access */}
              <div className="cd-membership-actions">
                {membershipError && (
                  <span className="cd-membership-error">{membershipError}</span>
                )}
                {isAdmin && (
                  <button
                    className="cd-btn cd-btn-edit"
                    onClick={handleOpenEditModal}
                  >
                    <LuPencil /> Editar comunidad
                  </button>
                )}
                {isAdmin && (
                  <button
                    className="cd-btn cd-btn-transfer"
                    onClick={handleOpenTransferModal}
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
                    community?.miembrosActuales > 1 && (
                    <button
                      className="cd-btn cd-btn-leave"
                      onClick={handleLeaveCommunity}
                      disabled={joinLoading}
                    >
                      <LuLogOut /> {joinLoading ? 'Saliendo...' : 'Abandonar comunidad'}
                    </button>
                    )
                  ) : requestSent ? (
                    <button className="cd-btn cd-btn-pending" disabled>
                      Solicitud de acceso enviada
                    </button>
                  ) : showJoinRoleChooser && hasTeacherProfile ? (
                    <div className="cd-join-role-picker">
                      <p className="cd-join-role-title">
                        {isPrivate ? 'Elige cómo quieres solicitar acceso:' : 'Elige cómo quieres unirte:'}
                      </p>
                      <div className="cd-join-role-actions">
                        <button
                          className="cd-btn cd-btn-join"
                          onClick={() => handleJoinWithRole('PROFESOR')}
                          disabled={joinLoading}
                        >
                          <LuLogIn /> {joinLoading
                            ? (isPrivate ? 'Solicitando...' : 'Uniendose...')
                            : (isPrivate ? 'Solicitar como profesor' : 'Unirme como profesor')}
                        </button>
                        <button
                          className="cd-btn cd-btn-join"
                          onClick={() => handleJoinWithRole('ALUMNO')}
                          disabled={joinLoading}
                        >
                          <LuLogIn /> {joinLoading
                            ? (isPrivate ? 'Solicitando...' : 'Uniendose...')
                            : (isPrivate ? 'Solicitar como alumno' : 'Unirme como alumno')}
                        </button>
                        <button
                          className="cd-btn cd-btn-leave"
                          onClick={() => setShowJoinRoleChooser(false)}
                          disabled={joinLoading}
                        >
                          Cancelar
                        </button>
                      </div>
                    </div>
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

        {community && (
          <div className="cd-roles-section">
            <div className="cd-role-card">
              <h2 className="cd-role-card-title">Sistema de roles</h2>
              <p className="cd-role-card-subtitle">
                La comunidad distingue entre administración, profesorado y alumnado.
              </p>
              <div className="cd-role-summary-head">
                <span className="cd-role-chip cd-role-chip--role">{roleLabel}</span>
                {isAdmin && <span className="cd-role-summary-note">Acceso completo a gestión</span>}
                {isTeacher && <span className="cd-role-summary-note">Puede crear eventos y coordinar actividades</span>}
              </div>
              <ul className="cd-role-capabilities">
                {roleCapabilities.map((capability) => (
                  <li key={capability}>{capability}</li>
                ))}
              </ul>
            </div>

            <div className="cd-role-card">
              <h2 className="cd-role-card-title">Equipo de la comunidad</h2>
              <p className="cd-role-card-subtitle">
                Listado visible de responsables y miembros por rol. Pulsa sobre cualquier persona para ver su perfil.
              </p>
              {membersLoading ? (
                <p className="cd-loading">Cargando miembros...</p>
              ) : (
                <div className="cd-role-groups">
                  <div className="cd-role-group">
                    <div className="cd-role-group-head">
                      <span>Administradores</span>
                      <strong>{adminMembers.length}</strong>
                    </div>
                    {renderMemberPills(adminMembers, 'Todavía no hay administradores visibles.')}
                  </div>
                  <div className="cd-role-group">
                    <div className="cd-role-group-head">
                      <span>Profesores</span>
                      <strong>{teacherMembers.length}</strong>
                    </div>
                    {renderMemberPills(teacherMembers, 'No hay profesores asignados en este momento.')}
                  </div>
                  <div className="cd-role-group">
                    <div className="cd-role-group-head">
                      <span>Alumnos</span>
                      <strong>{studentMembers.length}</strong>
                    </div>
                    {renderMemberPills(studentMembers, 'No hay alumnos visibles en este momento.')}
                  </div>
                </div>
              )}
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
                        {req.solicitante?.nombre || 'Usuario'} quiere unirse como{' '}
                        <strong>{getCommunityRoleLabel(req.rolDeseado || 'ALUMNO')}</strong>
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

        {/* Tabs de eventos y anuncios */}
        <div className="cd-tabs-section">
          <div className="cd-tabs-header">
            <button
              className={`cd-tab-btn${!showAnnouncementsTab ? ' cd-tab-btn-active' : ''}`}
              onClick={() => {
                setShowAnnouncementsTab(false);
                const params = new URLSearchParams(searchParams);
                params.set('tab', 'eventos');
                navigate({ search: params.toString() }, { replace: true });
              }}
              type="button"
            >
              <LuCalendar /> Eventos
            </button>
            <button
              className={`cd-tab-btn${showAnnouncementsTab ? ' cd-tab-btn-active' : ''}`}
              onClick={() => {
                setShowAnnouncementsTab(true);
                const params = new URLSearchParams(searchParams);
                params.set('tab', 'anuncios');
                navigate({ search: params.toString() }, { replace: true });
              }}
              type="button"
            >
              <span role="img" aria-label="Anuncios">📢</span> Anuncios
            </button>
          </div>
          <div className="cd-tabs-content">
            {!showAnnouncementsTab ? (
              <>
                <div className="cd-questionnaires-section">
                  <div className="cd-events-header">
                    <h2 className="cd-events-title">
                      <LuCalendar /> Cuestionarios de la comunidad
                    </h2>
                  </div>

                  {!currentUserId ? (
                    <div className="cd-questionnaires-empty">
                      <p>Inicia sesión para ver los cuestionarios de esta comunidad.</p>
                    </div>
                  ) : cuestionariosLoading ? (
                    <p className="cd-loading">Cargando cuestionarios...</p>
                  ) : cuestionariosError ? (
                    <div className="cd-error">{cuestionariosError}</div>
                  ) : communityCuestionarios.length > 0 ? (
                    <div className="cd-questionnaires-grid">
                      {communityCuestionarios.map((cuestionario) => (
                        <article
                          key={cuestionario.id}
                          className="cd-questionnaire-card"
                          role="button"
                          tabIndex={0}
                          onClick={() => navigate(`/cuestionarios/${cuestionario.id}`)}
                          onKeyDown={(event) => {
                            if (event.key === 'Enter' || event.key === ' ') {
                              event.preventDefault();
                              navigate(`/cuestionarios/${cuestionario.id}`);
                            }
                          }}
                        >
                          <div className="cd-questionnaire-card__header">
                            <h3>{cuestionario.titulo || 'Cuestionario sin titulo'}</h3>
                            <span className={`cd-questionnaire-state ${cuestionario.publicado ? 'is-published' : 'is-draft'}`}>
                              {cuestionario.publicado ? 'Publicado' : 'Borrador'}
                            </span>
                          </div>
                          <p>Materia: {cuestionario.materia || 'Sin materia'}</p>
                          <p>Preguntas: {cuestionario.numPreguntas || 0}</p>
                          <p>Creado: {formatDateTime(cuestionario.createdAt)}</p>
                        </article>
                      ))}
                    </div>
                  ) : (
                    <div className="cd-questionnaires-empty">
                      <p>Esta comunidad todavía no tiene cuestionarios publicados o asociados.</p>
                    </div>
                  )}
                </div>

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
                        <>
                          <button
                            className="cd-btn cd-btn-create"
                            onClick={() => navigate(`/cuestionarios/crear?communityId=${communityId}`)}
                          >
                            <LuPlus /> Crear cuestionario
                          </button>
                          {canCreateEvent ? (
                            <button
                              className="cd-btn cd-btn-create"
                              onClick={() => navigate(`/crear-evento/new?communityId=${communityId}`)}
                            >
                              <LuPlus /> Crear evento
                            </button>
                          ) : (
                            <span className="cd-member-hint">Solo administradores y profesores pueden crear eventos</span>
                          )}
                        </>
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
                          currentUserId={currentUserId}
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
                            onClick={() => navigate(`/cuestionarios/crear?communityId=${communityId}`)}
                          >
                            <LuPlus /> Crear cuestionario
                          </button>
                          {canCreateEvent && (
                            <button
                              className="cd-btn cd-btn-create"
                              onClick={() => navigate(`/crear-evento/new?communityId=${communityId}`)}
                            >
                              <LuPlus /> Crear evento
                            </button>
                          )}
                          {!canCreateEvent && (
                            <p>Solo administradores y profesores pueden crear eventos en esta comunidad.</p>
                          )}
                        </>
                      ) : (
                        <p>Únete a la comunidad para poder crear eventos.</p>
                      )}
                    </div>
                  )}
                </div>
              </>
            ) : (
              <div className="cd-announcements-section">
                <CommunityAnnouncementsTab communityId={communityId} isAdmin={isAdmin} />
              </div>
            )}
          </div>
        </div>
        
        {isMember && (
          <div className="cd-ranking-section">
            <h2 className="cd-ranking-title">
              <LuUsers /> Ranking de la comunidad
            </h2>

            {rankingLoading ? (
              <p className="cd-loading">Cargando ranking...</p>
            ) : rankingError ? (
              <p className="cd-ranking-error">{rankingError}</p>
            ) : ranking.length > 0 ? (
              <div className="cd-ranking-container">
                <table className="cd-ranking-table">
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>Usuario</th>
                      <th>Puntos</th>
                      <th>Mensajes</th>
                      <th>Eventos</th>
                      <th>Asistencias</th>
                    </tr>
                  </thead>

                  <tbody>
                    {ranking.map((item, index) => {
                      const isMe = item.usuario?.id === Number(currentUserId);
                      const rankingUserPhoto = getRankingUserPhoto(item);

                      return (
                        <tr
                          key={item.usuario?.id || index}
                          className={`
                            cd-ranking-row 
                            ${isMe ? 'is-me' : ''}
                          `}
                        >
                          {/* Columna de Posición / Medalla */}
                          <td>
                            <div className="cd-ranking-position">
                              {index === 0 ? '👑' : index + 1}
                            </div>
                          </td>

                          {/* Columna de Usuario */}
                          <td className="cd-ranking-user-cell">
                            {rankingUserPhoto ? (
                              <img
                                src={toAbsoluteImageUrl(rankingUserPhoto)}
                                alt={item.usuario?.nombre || 'Usuario'}
                                className="cd-ranking-avatar"
                              />
                            ) : (
                              <div className="cd-ranking-avatar cd-ranking-avatar-fallback">
                                {(item.usuario?.nombre || 'U').charAt(0).toUpperCase()}
                              </div>
                            )}

                            <span className="cd-ranking-name">
                              {item.usuario?.nombre || 'Usuario'}
                            </span>

                            {isMe && (
                              <span className="cd-ranking-you-badge">Tú</span>
                            )}
                          </td>

                          {/* Columna de Puntos con Badge */}
                          <td>
                            <span className="cd-ranking-points">
                              {item.puntos}
                            </span>
                          </td>
                          
                          {/* Resto de estadísticas */}
                          <td>{item.mensajes}</td>
                          <td>{item.eventosCreados}</td>
                          <td>{item.asistentesEventos}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            ) : (
              <p className="cd-ranking-empty">
                Aún no hay actividad suficiente.
              </p>
            )}
          </div>
        )}


        {currentUserId && isMember && user ? (
          <CommunityChat
            comunidadId={Number(communityId)}
            usuarioActual={currentUser}
            comunidadNombre={community?.nombre}
            comunidadImagen={communityImage}
            initiallyOpen={openChatOnLoad}
            isOpen={chatOpen}
            onOpenChange={handleChatOpenChange}
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

              {recordingsOpen ? (
                <div className="cd-inline-recordings-section">
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
              ) : null}
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
            hasTeacherProfile={hasTeacherProfile}
            onClose={() => setShowTransferModal(false)}
            onTransferred={() => {
              setShowTransferModal(false);
              fetchCommunity();
            }}
          />
        )}
      </div>

      {/* Modal alarmas al apuntarse */}
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
                        name="canal-cd"
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
    </>
  );
}

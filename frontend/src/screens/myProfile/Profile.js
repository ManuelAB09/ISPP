import { useEffect, useState } from "react"
import { Link, useLocation, useNavigate, useParams } from "react-router-dom"
import { authApi } from "../../api/auth.api"
import { getApiBaseUrl } from "../../api/baseUrl"
import { communitiesApi } from "../../api/communities.api"
import { cuestionariosApi } from "../../api/cuestionarios.api"
import { getMyTutorProfiles, getVerifiedTutors } from "../../api/tutorEndpoints"
import Header from "../../components/Header/Header"
import { useAuth } from "../../contexts/AuthContext"
import BecomeTutorInfoModal from "./BecomeTutorInfoModal";

import CreateProfileModal from "../teacherProfile/CreateProfileModal"

import { apiClient } from "../../api/client"

import EditProfile from "./EditProfile"
import "./MyProfile.css"
import Settings from "./Settings"

const DEFAULT_PROFILE_AVATAR =
    "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 120 120'%3E%3Ccircle cx='60' cy='60' r='60' fill='%23E6EAF3'/%3E%3Ccircle cx='60' cy='46' r='22' fill='%2395A1BB'/%3E%3Cpath d='M20 106c6-20 22-32 40-32s34 12 40 32' fill='%2395A1BB'/%3E%3C/svg%3E";

const DEFAULT_COMMUNITY_IMAGE =
    'https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&w=400&q=80';

const toAbsoluteImageUrl = (imageUrl, fallback = DEFAULT_PROFILE_AVATAR) => {
    if (!imageUrl || !String(imageUrl).trim()) {
        return fallback;
    }

    const value = String(imageUrl).trim();
    if (/^https?:\/\//i.test(value) || value.startsWith('data:image/') || value.startsWith('blob:')) {
        return value;
    }

    const base = getApiBaseUrl();
    if (value.startsWith('/')) {
        return `${base}${value}`;
    }

    return `${base}/${value}`;
};

/**
 * Extrae las iniciales del nombre (máximo 2).
 * Ej: "Juan Pérez" -> "JP", "Maria" -> "M"
 */
const getInitials = (nombre) => {
    if (!nombre || !String(nombre).trim()) {
        return 'U';
    }
    return String(nombre)
        .trim()
        .split(' ')
        .slice(0, 2)
        .map((word) => word[0]?.toUpperCase() ?? '')
        .join('');
};

const MyProfile = () => {
    const { isAuthenticated, loading, user, updateProfile } = useAuth()
    const { userId } = useParams()
    const navigate = useNavigate()
    const location = useLocation()
    const [showSettings, setShowSettings] = useState(false)
    const [showEditProfile, setShowEditProfile] = useState(false)
    const [ubicacionPreseleccionada, setUbicacionPreseleccionada] = useState(null)
    const [becomingTutor, setBecomingTutor] = useState(false)
    const [becomeTutorError, setBecomeTutorError] = useState('')
    const [miPerfilTutor, setMiPerfilTutor] = useState(null)
    const [loadingTutorProfile, setLoadingTutorProfile] = useState(false)
    const [showCreateTutorModal, setShowCreateTutorModal] = useState(false)
    const [misComunidades, setMisComunidades] = useState([])
    const [comunidadesCreadas, setComunidadesCreadas] = useState([])
    const [loadingCommunities, setLoadingCommunities] = useState(true)
    const [misCuestionarios, setMisCuestionarios] = useState([])
    const [loadingMisCuestionarios, setLoadingMisCuestionarios] = useState(true)
    const [stats, setStats] = useState({
        comunidades: 0,
        apuntesSubidos: 0,
        valoracionMedia: 0,
        descargas: 0
    })
    const [publicProfile, setPublicProfile] = useState(null)
    const [publicTutorProfile, setPublicTutorProfile] = useState(null)
    const [loadingPublicProfile, setLoadingPublicProfile] = useState(false)
    const [publicProfileError, setPublicProfileError] = useState('')
    const currentUserId = String(user?.id || localStorage.getItem('userId') || '')
    const isOwner = !userId || currentUserId === String(userId)
    const [unauthorizedMessage, setUnauthorizedMessage] = useState("")
    const [calendarNotification, setCalendarNotification] = useState(null)
    const { logout } = useAuth()
    const [showTutorInfoModal, setShowTutorInfoModal] = useState(false);

    // ── Helpers de tipo de perfil ─────────────────────────────
    const isPublicTutor = !isOwner && publicProfile?.esTutor
    const isPublicStudent = !isOwner && !publicProfile?.esTutor

    useEffect(() => {
        if (isOwner || !userId || !isAuthenticated) {
            setPublicProfile(null)
            setPublicTutorProfile(null)
            setPublicProfileError('')
            setLoadingPublicProfile(false)
            return
        }

        setLoadingPublicProfile(true)
        setPublicProfileError('')

        authApi.getUserById(userId)
            .then((profile) => {
                setPublicProfile(profile)
                if (!profile?.esTutor) {
                    setPublicTutorProfile(null)
                    return null
                }

                return getVerifiedTutors({ page: 0, size: 200 })
                    .then((resp) => {
                        const tutors = resp?.content ?? (Array.isArray(resp) ? resp : [])
                        const matchingTutor = tutors.find((tutor) => String(tutor?.userId) === String(profile.id)) || null
                        setPublicTutorProfile(matchingTutor)
                    })
                    .catch(() => {
                        setPublicTutorProfile(null)
                    })
            })
            .catch((err) => {
                console.error('Error al cargar perfil público:', err)
                setPublicProfile(null)
                setPublicTutorProfile(null)
                setPublicProfileError('No se pudo cargar este perfil.')
            })
            .finally(() => setLoadingPublicProfile(false))
    }, [isAuthenticated, isOwner, userId])

    useEffect(() => {
        if (!isOwner) {
            setMiPerfilTutor(null);
            setLoadingTutorProfile(false);
            return;
        }

        if (!isAuthenticated || loading || !user?.esTutor) {
            setMiPerfilTutor(null);
            return;
        }

        setLoadingTutorProfile(true);
        getMyTutorProfiles()
            .then((perfiles) => {
                const primerPerfil = Array.isArray(perfiles) ? perfiles[0] : perfiles;
                setMiPerfilTutor(primerPerfil?.id ? primerPerfil : null);
            })
            .catch(() => {
                setMiPerfilTutor(null);
            })
            .finally(() => setLoadingTutorProfile(false));
    }, [isAuthenticated, isOwner, loading, user]);

    useEffect(() => {
        if (!isAuthenticated || loading) {
            setMisCuestionarios([])
            setLoadingMisCuestionarios(false)
            return
        }

        const fetchCuestionarios = async () => {
            setLoadingMisCuestionarios(true)
            try {
                if (isOwner) {
                    const data = await cuestionariosApi.listMine()
                    setMisCuestionarios(Array.isArray(data) ? data : [])
                } else if (publicProfile?.id) {
                    const data = await cuestionariosApi.listPublicByUserId(publicProfile.id)
                    setMisCuestionarios(Array.isArray(data) ? data : [])
                } else {
                    setMisCuestionarios([])
                }
            } catch (err) {
                console.error('Error al cargar cuestionarios:', err)
                setMisCuestionarios([])
            } finally {
                setLoadingMisCuestionarios(false)
            }
        }

        if (isOwner || publicProfile) {
            fetchCuestionarios()
        }
    }, [isAuthenticated, loading, isOwner, publicProfile])

    useEffect(() => {
        const ubicacion = location.state?.ubicacion;
        const openEditProfile = Boolean(location.state?.openEditProfile);
        const openSettings = Boolean(location.state?.openSettings);
        const calendarNotif = location.state?.calendarNotification;

        if (!ubicacion && !openEditProfile && !openSettings && !calendarNotif) {
            return;
        }

        if (ubicacion) {
            setUbicacionPreseleccionada(ubicacion);
        }
        if (ubicacion || openEditProfile) {
            setShowEditProfile(true);
        }
        if (openSettings) {
            setShowSettings(true);
        }
        if (calendarNotif) {
            setCalendarNotification(calendarNotif);
        }

        navigate(location.pathname, { replace: true, state: {} });
    }, [location.pathname, location.state, navigate]);

    const handleBecomeTutor = async () => {
        setBecomingTutor(true)
        setBecomeTutorError('')
        const result = await updateProfile({ esTutor: true })
        if (!result.success) {
            setBecomeTutorError(result.error || 'Error al actualizar el rol')
        }
        setBecomingTutor(false)
    }

    useEffect(() => {
        if (!isOwner) {
            setLoadingCommunities(false);
            setMisComunidades([]);
            setComunidadesCreadas([]);
            return;
        }

        if (!isAuthenticated || loading) {
            setLoadingCommunities(false);
            return;
        }

        const fetchCommunities = async () => {
            setLoadingCommunities(true);
            try {
                const response = await communitiesApi.listMine({ page: 0, size: 100 });
                const comunidades = response.content || [];

                const creadas = comunidades.filter(c =>
                    c.miRol === 'ADMIN' || c.miRol === 'ADMINISTRADOR' ||
                    c.creador?.id === user?.id || parseInt(localStorage.getItem('userId')) === c.creador?.id
                );

                const miembro = comunidades.filter(c =>
                    c.miRol !== 'ADMIN' && c.miRol !== 'ADMINISTRADOR' &&
                    c.creador?.id !== user?.id && parseInt(localStorage.getItem('userId')) !== c.creador?.id
                );

                setComunidadesCreadas(creadas);
                setMisComunidades(miembro);

                setStats(prev => ({
                    ...prev,
                    comunidades: comunidades.length
                }));
            } catch (err) {
                console.error('Error al cargar comunidades:', err);
            } finally {
                setLoadingCommunities(false);
            }
        };

        fetchCommunities();
    }, [isAuthenticated, isOwner, loading, user]);

    if (loading) {
        return (
            <>
                <Header page={'inicio'} />
                <main className="my-profile">
                    <div className="profile-loading">Cargando...</div>
                </main>
            </>
        )
    }

    if (!isAuthenticated) {
        return (
            <>
                <Header page={'inicio'} />
                <main className="my-profile">
                    <div className="profile-not-logged">
                        <div className="profile-not-logged__icon">🔒</div>
                        <h1 className="profile-not-logged__title">No has iniciado sesión</h1>
                        <p className="profile-not-logged__text">
                            Para acceder a tu perfil necesitas iniciar sesión o crear una cuenta.
                        </p>
                        <div className="profile-not-logged__buttons">
                            <Link to="/login" className="btn-login">Iniciar sesión</Link>
                            <Link to="/register" className="btn-register">Registrarse</Link>
                        </div>
                    </div>
                </main>
            </>
        )
    }

    if (!isOwner && loadingPublicProfile) {
        return (
            <>
                <Header page={'inicio'} />
                <main className="my-profile">
                    <div className="profile-loading">Cargando perfil...</div>
                </main>
            </>
        )
    }

    if (!isOwner && publicProfileError) {
        return (
            <>
                <Header page={'inicio'} />
                <main className="my-profile">
                    <div className="profile-not-logged">
                        <div className="profile-not-logged__icon">👤</div>
                        <h1 className="profile-not-logged__title">Perfil no disponible</h1>
                        <p className="profile-not-logged__text">{publicProfileError}</p>
                        <div className="profile-not-logged__buttons">
                            <button className="btn-login" onClick={() => navigate(-1)}>Volver</button>
                        </div>
                    </div>
                </main>
            </>
        )
    }

    const profileSource = isOwner ? user : publicProfile

    const userData = {
        nombre: profileSource?.nombre || "Sin nombre",
        descripcion: profileSource?.bio || "",
        rol: profileSource?.esTutor ? 'Estudiante y Profesor' : 'Estudiante',
        email: isOwner ? (profileSource?.email || "Sin email") : "",
        universidad: profileSource?.universidad || "",
        grado: profileSource?.grado || "",
        ubicacion:
            typeof profileSource?.ubicacion === 'string'
                ? profileSource.ubicacion
                : profileSource?.ubicacion?.nombre || '',
        nivelEstudios: isOwner ? (profileSource?.nivelEstudios || "") : "",
        baseFormativa: isOwner ? (profileSource?.baseFormativa || "") : "",
        foto: profileSource?.foto || null,
        fotoBackgroundColor: profileSource?.fotoBackgroundColor || '#ffffff',
        intereses: profileSource?.intereses || []
    }

    const displayValue = (value) => {
        if (typeof value !== 'string') {
            return value ? String(value) : <span className="no-info">Sin información</span>;
        }
        return value.trim() !== '' ? value : <span className="no-info">Sin información</span>
    }

    const getCommunityImageUrl = (comunidad) => {
        const communityImageRaw = comunidad.imagen || comunidad.imagenUrl || comunidad.foto;

        if (!communityImageRaw || !String(communityImageRaw).trim()) {
            return DEFAULT_COMMUNITY_IMAGE;
        }

        const value = String(communityImageRaw).trim();
        const normalizedValue = value.toLowerCase();
        if (normalizedValue === 'empty' || normalizedValue === 'null' || normalizedValue === 'undefined') {
            return DEFAULT_COMMUNITY_IMAGE;
        }

        if (/^https?:\/\//i.test(value) || value.startsWith('data:image/')) {
            return value;
        }

        const base = getApiBaseUrl();
        if (value.startsWith('/')) {
            return `${base}${value}`;
        }

        return `${base}/${value}`;
    }

    const formatCuestionarioDate = (dateValue) => {
        if (!dateValue) {
            return 'Fecha desconocida'
        }

        const parsed = new Date(dateValue)
        if (Number.isNaN(parsed.getTime())) {
            return 'Fecha desconocida'
        }

        return parsed.toLocaleDateString('es-ES', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric'
        })
    }

    const handleLogout = async () => {
        if (!isOwner) {
            setUnauthorizedMessage("No puedes cerrar sesión de una cuenta que no es tuya.")
            setTimeout(() => setUnauthorizedMessage(""), 3000)
            return
        }

        try {
            await apiClient.post('/api/v1/auth/logout')
        } catch (error) {
            console.error('Error al cerrar sesión:', error)
        } finally {
            logout()
            navigate('/')
        }
    }

    // ── Clases y estilos del header según tipo de perfil ─────
    const headerClass = [
        'profile-header',
        isPublicTutor ? 'profile-header--tutor' : '',
        isPublicStudent ? 'profile-header--student' : '',
    ].filter(Boolean).join(' ')

    const headerStyle = isPublicTutor
        ? {
            background: 'linear-gradient(135deg, #2d3250 0%, #424769 60%, #676f9d 100%)',
            border: '1px solid #2d3250',
            color: '#fff',
        }
        : isPublicStudent
            ? {
                background: 'linear-gradient(135deg, #f8faff 0%, #eef2ff 100%)',
                border: '1px solid #c7d2fe',
            }
            : {}

    const avatarClass = [
        'profile-avatar',
        isPublicTutor ? 'profile-avatar--tutor' : '',
        isPublicStudent ? 'profile-avatar--student' : '',
    ].filter(Boolean).join(' ')

    const dataSectionClass = [
        'profile-data',
        isPublicTutor ? 'profile-data--tutor' : '',
        isPublicStudent ? 'profile-data--student' : '',
    ].filter(Boolean).join(' ')

    return (
        <>
            <Header page={'inicio'} />

            <main className="my-profile">

                {/* ── Banner informativo para perfiles públicos (NUEVO) ── */}
                {!isOwner && (
                    <div className={`profile-public-banner profile-public-banner--${isPublicTutor ? 'tutor' : 'student'}`}>
                        <div className="profile-public-banner__icon">
                            {isPublicTutor ? '🎓' : '📚'}
                        </div>
                        <div className="profile-public-banner__text">
                            <h3>
                                {isPublicTutor
                                    ? `Perfil de Profesor${publicTutorProfile?.verificado ? ' · ✓ Verificado' : ''}`
                                    : 'Perfil de Estudiante'}
                            </h3>
                            <p>
                                {isPublicTutor
                                    ? publicTutorProfile?.verificado
                                        ? 'Este profesor ha sido verificado por el equipo de MeerKatters.'
                                        : 'Explora el perfil y contáctale si te interesa aprender con él/ella.'
                                    : 'Este usuario es miembro de la comunidad de estudiantes.'}
                            </p>
                        </div>
                    </div>
                )}

                {/* ══════════════════════════════════════════════════
                    PROFILE HEADER
                ══════════════════════════════════════════════════ */}
                <section className={headerClass} style={headerStyle}>

                    {/* ── Franja de tipo de perfil (solo vistas públicas) ── */}
                    {!isOwner && (
                        <div className={`profile-type-bar profile-type-bar--${isPublicTutor ? 'tutor' : 'student'}`}>
                            <span style={{ fontSize: '14px' }}>{isPublicTutor ? '🎓' : '📚'}</span>
                            {isPublicTutor ? 'Perfil de Profesor' : 'Perfil de Estudiante'}
                            {isPublicTutor && publicTutorProfile?.verificado && (
                                <span className="profile-type-bar__verified">✓ Verificado</span>
                            )}
                        </div>
                    )}

                    <div className="profile-header__left" style={!isOwner ? { marginTop: '36px' } : {}}>
                        {/* Avatar con estilo según tipo */}
                        <div
                            className={avatarClass}
                            style={{ backgroundColor: userData.fotoBackgroundColor }}
                        >
                            {userData.foto ? (
                                <img
                                    src={toAbsoluteImageUrl(userData.foto, DEFAULT_PROFILE_AVATAR)}
                                    alt={userData.nombre}
                                    className="profile-avatar-img"
                                    onError={e => { e.target.onerror = null; e.target.src = DEFAULT_PROFILE_AVATAR; }}
                                />
                            ) : (
                                <div className="profile-avatar__initials">
                                    {getInitials(userData.nombre)}
                                </div>
                            )}
                        </div>

                        <div className="profile-info">
                            <div className="profile-info__headline">
                                <h1 className="profile-info__name">
                                    {userData.nombre}
                                </h1>

                                {/* Badge de tipo (solo perfiles públicos) */}
                                {!isOwner && (
                                    isPublicTutor ? (
                                        publicTutorProfile?.verificado ? (
                                            <span
                                                className="profile-verified-badge"
                                                title="Tutor verificado"
                                            >
                                                ✓ Tutor verificado
                                            </span>
                                        ) : (
                                            <span className="profile-type-badge--tutor">
                                                🎓 Profesor
                                            </span>
                                        )
                                    ) : (
                                        <span className="profile-type-badge--student">
                                            📚 Estudiante
                                        </span>
                                    )
                                )}
                            </div>

                            <p className="profile-info__description">
                                {userData.descripcion}
                            </p>

                            {/* Pill de rol */}
                            {isOwner ? (
                                <span className="profile-info__role">{userData.rol}</span>
                            ) : isPublicTutor ? (
                                <span className="profile-role-pill--tutor">
                                    🎓 Estudiante y Profesor
                                </span>
                            ) : (
                                <span className="profile-role-pill--student">
                                    📚 Estudiante
                                </span>
                            )}

                            {/* Card de tutor público */}
                            {!isOwner && publicTutorProfile && (
                                <div className="profile-public-tutor-card">
                                    <div>
                                        <p className="profile-public-tutor-card__title">Perfil docente disponible</p>
                                        <p className="profile-public-tutor-card__text">
                                            {publicTutorProfile.verificado
                                                ? 'Tutor verificado en la plataforma.'
                                                : 'Tiene perfil de tutor activo.'}
                                        </p>
                                        {publicTutorProfile.especialidades?.length > 0 && (
                                            <div className="profile-public-tutor-card__tags">
                                                {publicTutorProfile.especialidades.slice(0, 3).map((speciality) => (
                                                    <span key={speciality} className="profile-interest-tag">{speciality}</span>
                                                ))}
                                            </div>
                                        )}
                                    </div>
                                    <button
                                        className="btn-edit-profile profile-public-tutor-card__button"
                                        onClick={() => navigate(`/profesores/${publicTutorProfile.id}`)}
                                    >
                                        Ver perfil de profesor
                                    </button>
                                </div>
                            )}

                            {userData.intereses && userData.intereses.length > 0 && (
                                <div className="profile-info__interests">
                                    {userData.intereses.map((interes, index) => (
                                        <span key={index} className="profile-interest-tag">
                                            {interes}
                                        </span>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>

                    {unauthorizedMessage && (
                        <div className="settings-unauthorized-message">
                            ⚠️ {unauthorizedMessage}
                        </div>
                    )}

                    <div className="profile-header__right">
                        {isOwner && (
                            <>
                                <button className="btn-edit-profile" onClick={() => setShowEditProfile(true)}>
                                    Editar Perfil
                                </button>
                                <button className="btn-settings" onClick={() => setShowSettings(true)}>
                                    Configuración
                                </button>
                                <button className="btn-logout" onClick={handleLogout}>
                                    Cerrar Sesión
                                </button>
                            </>
                        )}

                        {/* CTA + stats rápidos para visitante que ve un tutor */}
                        {isPublicTutor && publicTutorProfile && (
                            <>
                                <button
                                    className="profile-tutor-cta"
                                    onClick={() => navigate(`/profesores/${publicTutorProfile.id}`)}
                                >
                                    🎓 Ver como profesor
                                </button>
                                {/* Mini stats debajo del CTA */}
                                {publicTutorProfile.tarifaPorHora != null && (
                                    <div className="profile-tutor-quick-stats">
                                        <div className="profile-tutor-quick-stat">
                                            <span className="profile-tutor-quick-stat__val">
                                                {Number(publicTutorProfile.tarifaPorHora).toFixed(0)}€
                                            </span>
                                            <span className="profile-tutor-quick-stat__lbl">/ hora</span>
                                        </div>
                                        {publicTutorProfile.verificado && (
                                            <div className="profile-tutor-quick-stat">
                                                <span className="profile-tutor-quick-stat__val">✓</span>
                                                <span className="profile-tutor-quick-stat__lbl">Verif.</span>
                                            </div>
                                        )}
                                        {publicTutorProfile.especialidades?.length > 0 && (
                                            <div className="profile-tutor-quick-stat">
                                                <span className="profile-tutor-quick-stat__val">
                                                    {publicTutorProfile.especialidades.length}
                                                </span>
                                                <span className="profile-tutor-quick-stat__lbl">Materias</span>
                                            </div>
                                        )}
                                    </div>
                                )}
                            </>
                        )}
                    </div>
                </section>

                {/* ── El resto del JSX EXACTAMENTE igual que el original ── */}

                <section className="profile-data-section">
                    <div className={dataSectionClass}>
                        <h2 className="section-title">{isOwner ? 'Mis datos' : 'Datos del perfil'}</h2>
                        <div className="profile-data__content">
                            <div className="data-field">
                                <span className="data-field__label">NOMBRE COMPLETO</span>
                                <span className="data-field__value">{displayValue(userData.nombre)}</span>
                            </div>
                            {isOwner && (
                                <div className="data-field">
                                    <span className="data-field__label">EMAIL</span>
                                    <span className="data-field__value">{displayValue(userData.email)}</span>
                                </div>
                            )}
                            <div className="data-field">
                                <span className="data-field__label">UNIVERSIDAD</span>
                                <span className="data-field__value">{displayValue(userData.universidad)}</span>
                            </div>
                            <div className="data-field">
                                <span className="data-field__label">GRADO</span>
                                <span className="data-field__value">{displayValue(userData.grado)}</span>
                            </div>
                            {isOwner && (
                                <div className="data-field">
                                    <span className="data-field__label">NIVEL DE ESTUDIOS</span>
                                    <span className="data-field__value">{displayValue(userData.nivelEstudios)}</span>
                                </div>
                            )}
                            {isOwner && (
                                <div className="data-field">
                                    <span className="data-field__label">BASE FORMATIVA</span>
                                    <span className="data-field__value">{displayValue(userData.baseFormativa)}</span>
                                </div>
                            )}
                            <div className="data-field">
                                <span className="data-field__label">UBICACIÓN</span>
                                <span className="data-field__value">{displayValue(userData.ubicacion)}</span>
                            </div>
                        </div>
                    </div>

                    {isOwner && (
                        <div className="profile-data-section__right">
                            <div className="profile-nav-actions">
                                <button className="profile-nav-btn" onClick={() => navigate('/pagos')}>
                                    💳 Mis pagos
                                </button>
                                <button className="profile-nav-btn" onClick={() => navigate('/ganancias')}>
                                    💰 Mis ganancias
                                </button>
                            </div>

                            <div className="activity-card">
                                <h3 className="activity-card__title">Tu Actividad</h3>
                                <div className="activity-card__grid">
                                    <div className="activity-stat">
                                        <span className="activity-stat__value">{stats.comunidades}</span>
                                        <span className="activity-stat__label">COMUNIDADES</span>
                                    </div>
                                    <div className="activity-stat">
                                        <span className="activity-stat__value">{stats.apuntesSubidos}</span>
                                        <span className="activity-stat__label">APUNTES SUBIDOS</span>
                                    </div>
                                    <div className="activity-stat">
                                        <span className="activity-stat__value">{stats.valoracionMedia}</span>
                                        <span className="activity-stat__label">VALORACIÓN MEDIA</span>
                                    </div>
                                    <div className="activity-stat">
                                        <span className="activity-stat__value">{stats.descargas}</span>
                                        <span className="activity-stat__label">DESCARGAS</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}
                </section>

                {isOwner && <section className="my-communities-section">
                    <h2 className="section-title">Mis comunidades</h2>
                    {
                        loadingCommunities ? (
                            <div className="loading-communities">Cargando comunidades...</div>
                        ) : misComunidades.length > 0 ? (
                            <div className="communities-list">
                                {misComunidades.map((comunidad) => (
                                    <div key={comunidad.id} className="community-card" onClick={() => navigate(`/comunidades/${comunidad.id}`)} style={{ cursor: 'pointer' }}>
                                        <img
                                            src={getCommunityImageUrl(comunidad)}
                                            alt={comunidad.nombre}
                                            className="community-card__image"
                                            onError={(e) => {
                                                e.currentTarget.onerror = null;
                                                e.currentTarget.src = DEFAULT_COMMUNITY_IMAGE;
                                            }}
                                        />
                                        <div className="community-card__info">
                                            <div className="community-card__top">
                                                <h3 className="community-card__name">{comunidad.nombre}</h3>
                                                {comunidad.categoria && comunidad.categoria.length > 0 && (
                                                    <div className="community-card__tags">
                                                        {comunidad.categoria.slice(0, 2).map(cat => (
                                                            <span key={cat} className="community-tag">{cat}</span>
                                                        ))}
                                                    </div>
                                                )}
                                                <p className="community-card__description">{comunidad.descripcion || 'Sin descripción disponible'}</p>
                                            </div>
                                            <div className="community-card__bottom">
                                                <div className="community-card__members">
                                                    <span className="members-icon">👥</span>
                                                    <span className="members-count">{comunidad.miembrosActuales || 0}/ <span className="members-max">{comunidad.maxMiembros || 0}</span></span>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                ))}
                                <div className="view-all-container">
                                    <Link to="/comunidades" className="view-all-link">Ver todas mis comunidades →</Link>
                                </div>
                            </div>
                        ) : (
                            <div className="no-communities">
                                <p>No formas parte de ninguna comunidad todavía.</p>
                                <button className="btn-explore" onClick={() => navigate('/comunidades')}>Explorar comunidades</button>
                            </div>
                        )
                    }
                </section>}

                <section className="my-questionnaires-section">
                    <h2 className="section-title">{isOwner ? 'Mis cuestionarios' : 'Cuestionarios públicos'}</h2>
                    {loadingMisCuestionarios ? (
                        <div className="loading-communities">Cargando cuestionarios...</div>
                    ) : misCuestionarios.length > 0 ? (
                        <div className="my-questionnaires-list">
                            {misCuestionarios.map((cuestionario) => (
                                <article
                                    key={cuestionario.id}
                                    className="my-questionnaire-card"
                                    role="button"
                                    tabIndex={0}
                                    onClick={() => navigate(`/cuestionarios/${cuestionario.id}`)}
                                    onKeyDown={(event) => {
                                        if (event.key === 'Enter' || event.key === ' ') {
                                            event.preventDefault()
                                            navigate(`/cuestionarios/${cuestionario.id}`)
                                        }
                                    }}
                                >
                                    <div className="my-questionnaire-card__header">
                                        <h3>{cuestionario.titulo || 'Cuestionario sin titulo'}</h3>
                                        <span className={`my-questionnaire-status ${cuestionario.publicado ? 'is-published' : 'is-draft'}`}>
                                            {cuestionario.publicado ? 'Publicado' : 'Borrador'}
                                        </span>
                                    </div>
                                    <p className="my-questionnaire-card__meta">
                                        Materia: {cuestionario.materia || 'Sin materia'}
                                    </p>
                                    <p className="my-questionnaire-card__meta">
                                        Preguntas: {cuestionario.numPreguntas || 0}
                                    </p>
                                    <p className="my-questionnaire-card__meta">
                                        Creado: {formatCuestionarioDate(cuestionario.createdAt)}
                                    </p>
                                </article>
                            ))}
                        </div>
                    ) : (
                        <div className="no-communities-created">
                            <p>{isOwner ? 'Todavía no has creado cuestionarios.' : 'Aún no hay cuestionarios públicos.'}</p>
                            {isOwner && (
                                <button className="btn-create-first" onClick={() => navigate('/cuestionarios/crear')}>
                                    Crear mi primer cuestionario
                                </button>
                            )}
                        </div>
                    )}
                </section>

                {isOwner &&
                    !user?.esTutor && (
                        <section className="tutor-profile-section">
                            <div className="created-header">
                                <div>
                                    <h2 className="section-title">¿Quieres ser profesor?</h2>
                                    <p className="section-subtitle">Conviértete en tutor y empieza a enseñar a otros estudiantes.</p>
                                </div>
                                <button
                                    className="btn-become-tutor"
                                    onClick={() => setShowTutorInfoModal(true)}
                                >
                                    <span className="btn-icon">🎓</span>
                                    {becomingTutor ? 'Actualizando...' : 'Convertirme en tutor'}
                                </button>
                            </div>
                            {becomeTutorError && (
                                <span className="become-tutor-error">{becomeTutorError}</span>
                            )}
                        </section>
                    )
                }
                {isOwner &&
                    user?.esTutor && (
                        <section className="tutor-profile-section">
                            <div className="created-header">
                                <div>
                                    <h2 className="section-title">Mi perfil de profesor</h2>
                                    <p className="section-subtitle">Gestiona tu perfil como tutor y consigue alumnos.</p>
                                </div>
                                {!miPerfilTutor && (
                                    <button className="btn-create-new" onClick={() => setShowCreateTutorModal(true)}>
                                        + Crear nuevo perfil de profesor
                                    </button>
                                )}
                            </div>
                            {loadingTutorProfile ? (
                                <div className="loading-communities">Cargando perfil de tutor...</div>
                            ) : miPerfilTutor ? (
                                <div className="tutor-profile-card">
                                    <div className="tutor-profile-card__info">
                                        <div className="tutor-profile-card__header">
                                            <h3 className="tutor-profile-card__name">{userData.nombre}</h3>
                                            {miPerfilTutor.verificado && (
                                                <span className="tutor-profile-card__badge">✓ Verificado</span>
                                            )}
                                        </div>
                                        {miPerfilTutor.especialidades && miPerfilTutor.especialidades.length > 0 && (
                                            <div className="tutor-profile-card__tags">
                                                {miPerfilTutor.especialidades.slice(0, 4).map((esp, i) => (
                                                    <span key={i} className="tutor-profile-card__tag">{esp}</span>
                                                ))}
                                            </div>
                                        )}
                                        {miPerfilTutor.tarifaPorHora != null && (
                                            <p className="tutor-profile-card__tarifa">
                                                <strong>{Number(miPerfilTutor.tarifaPorHora).toFixed(2)} €</strong> / hora
                                            </p>
                                        )}
                                    </div>
                                    <div className="tutor-profile-card__actions">
                                        <button
                                            className="btn-create-new"
                                            onClick={() => navigate(`/profesores/${miPerfilTutor.id}`)}
                                        >
                                            Ver mi perfil
                                        </button>
                                    </div>
                                </div>
                            ) : (
                                <div className="no-communities-created">
                                    <p>Aún no tienes perfil de profesor.</p>
                                    <button className="btn-create-first" onClick={() => setShowCreateTutorModal(true)}>
                                        + Crear nuevo perfil de profesor
                                    </button>
                                </div>
                            )}
                        </section>
                    )
                }

                {isOwner && <section className="created-communities-section">
                    <div className="created-header">
                        <div>
                            <h2 className="section-title">Comunidades creadas</h2>
                            <p className="section-subtitle">Crea comunidades, une a estudiantes y enseña sobre lo que sabes.</p>
                        </div>
                        <button className="btn-create-new" onClick={() => navigate('/crear-comunidad')}>+ Crear Nueva</button>
                    </div>
                    {loadingCommunities ? (
                        <div className="loading-communities">Cargando comunidades...</div>
                    ) : comunidadesCreadas.length > 0 ? (
                        <div className="created-communities-list">
                            {comunidadesCreadas.map((comunidad) => (
                                <div key={comunidad.id} className="created-community-card" onClick={() => navigate(`/comunidades/${comunidad.id}`)} style={{ cursor: 'pointer' }}>
                                    <img
                                        src={getCommunityImageUrl(comunidad)}
                                        alt={comunidad.nombre}
                                        className="created-community-card__image"
                                        onError={(e) => {
                                            e.currentTarget.onerror = null;
                                            e.currentTarget.src = DEFAULT_COMMUNITY_IMAGE;
                                        }}
                                    />
                                    <div className="created-community-card__info">
                                        <div className="created-community-card__top">
                                            <h3 className="created-community-card__name">{comunidad.nombre}</h3>
                                            <div className="created-community-card__tags">
                                                {comunidad.categoria && comunidad.categoria.length > 0 && comunidad.categoria.slice(0, 2).map(cat => (
                                                    <span key={cat} className="created-tag">{cat}</span>
                                                ))}
                                                <span className="created-tag created-tag--plan">{comunidad.tipoPlan || 'FREE'}</span>
                                            </div>
                                            <p className="created-community-card__description">{comunidad.descripcion || 'Sin descripción disponible'}</p>
                                        </div>
                                        <div className="created-community-card__bottom">
                                            <div className="created-community-card__members">
                                                <span className="members-text">Inscritos: <strong>{comunidad.miembrosActuales || 0}</strong>/{comunidad.maxMiembros || 0}</span>
                                            </div>
                                            {isOwner && (
                                                <div className="created-community-card__actions">
                                                    <Link to={`/comunidades/${comunidad.id}/editar`} className="action-link" onClick={(e) => e.stopPropagation()}> Editar</Link>
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div className="no-communities-created">
                            <p>Aún no has creado ninguna comunidad.</p>
                            <button className="btn-create-first" onClick={() => navigate('/crear-comunidad')}>Crear mi primera comunidad</button>
                        </div>
                    )}
                </section>}
            </main>

            {isOwner && showCreateTutorModal && (
                <CreateProfileModal
                    onClose={() => setShowCreateTutorModal(false)}
                    onCreado={(newTutor) => {
                        setMiPerfilTutor(newTutor);
                        setShowCreateTutorModal(false);
                        navigate(`/profesores/${newTutor.id}`);
                    }}
                />
            )}

            {isOwner && showSettings && (
                <Settings
                    onClose={() => setShowSettings(false)}
                    isOwner={isOwner}
                    calendarNotification={calendarNotification}
                    onCalendarNotificationRead={() => setCalendarNotification(null)}
                />
            )}

            {isOwner && showEditProfile && (
                <EditProfile
                    onClose={() => setShowEditProfile(false)}
                    ubicacionPreseleccionada={ubicacionPreseleccionada}
                    onSave={(updatedUser) => {
                        setShowEditProfile(false)
                        setUbicacionPreseleccionada(null)
                    }}
                />
            )}

            {showTutorInfoModal && (
                <BecomeTutorInfoModal
                    onClose={() => setShowTutorInfoModal(false)}
                    onContinue={async () => {
                        setShowTutorInfoModal(false);
                        await handleBecomeTutor();
                    }}
                />
            )}
        </>
    )
}

export default MyProfile
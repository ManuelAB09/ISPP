import { useEffect, useState } from "react"
import { Link, useNavigate } from "react-router-dom"
import { getApiBaseUrl } from "../../api/baseUrl"
import { communitiesApi } from "../../api/communities.api"
import { getMyTutorProfiles } from "../../api/tutorEndpoints"
import Header from "../../components/Header/Header"
import { useAuth } from "../../contexts/AuthContext"
import EditProfile from "./EditProfile"
import "./MyProfile.css"
import Settings from "./Settings"

const toAbsoluteImageUrl = (imageUrl) => {
    if (!imageUrl || !String(imageUrl).trim()) {
        return ''
    }

    const value = String(imageUrl).trim()
    if (/^https?:\/\//i.test(value) || value.startsWith('data:image/')) {
        return value
    }

    const base = getApiBaseUrl()
    if (value.startsWith('/')) {
        return `${base}${value}`
    }

    return `${base}/${value}`
}

const MyProfile = () => {
    const { isAuthenticated, loading, user } = useAuth()
    const navigate = useNavigate()
    const [showSettings, setShowSettings] = useState(false)
    const [showEditProfile, setShowEditProfile] = useState(false)
    const [checkingTutor, setCheckingTutor] = useState(true)
    const [misComunidades, setMisComunidades] = useState([])
    const [comunidadesCreadas, setComunidadesCreadas] = useState([])
    const [loadingCommunities, setLoadingCommunities] = useState(true)
    const [stats, setStats] = useState({
        comunidades: 0,
        apuntesSubidos: 0,
        valoracionMedia: 0,
        descargas: 0
    })
    const isOwner = true // Siempre es el propietario en esta pantalla

    // Si el usuario tiene perfil de tutor, redirigir a su perfil de profesor
    useEffect(() => {
        if (!isAuthenticated || loading) {
            setCheckingTutor(false);
            return;
        }
        getMyTutorProfiles()
            .then((perfiles) => {
                if (perfiles && perfiles.length > 0) {
                    navigate(`/profesores/${perfiles[0].id}`, { replace: true });
                } else {
                    setCheckingTutor(false);
                }
            })
            .catch(() => {
                setCheckingTutor(false);
            });
    }, [isAuthenticated, loading, navigate]);

    // Cargar comunidades del usuario
    useEffect(() => {
        if (!isAuthenticated || loading) {
            setLoadingCommunities(false);
            return;
        }

        const fetchCommunities = async () => {
            setLoadingCommunities(true);
            try {
                const response = await communitiesApi.listMine({ page: 0, size: 100 });
                const comunidades = response.content || [];
                
                // Filtrar comunidades donde soy admin/creador
                const creadas = comunidades.filter(c => 
                    c.miRol === 'ADMIN' || c.miRol === 'ADMINISTRADOR' || 
                    c.creador?.id === user?.id || parseInt(localStorage.getItem('userId')) === c.creador?.id
                );
                
                // Filtrar comunidades donde solo soy miembro
                const miembro = comunidades.filter(c => 
                    c.miRol !== 'ADMIN' && c.miRol !== 'ADMINISTRADOR' && 
                    c.creador?.id !== user?.id && parseInt(localStorage.getItem('userId')) !== c.creador?.id
                );
                
                setComunidadesCreadas(creadas);
                setMisComunidades(miembro);
                
                // Actualizar estadísticas
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
    }, [isAuthenticated, loading, user]);

    if (loading || checkingTutor) {
        return (
            <>
                <Header page={'inicio'} />
                <main className="my-profile">
                    <div className="profile-loading">Cargando...</div>
                </main>
            </>
        )
    }

    // Si no está autenticado y es su propio perfil, mostrar mensaje
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

    // Datos del usuario desde el contexto
    const userData = {
        nombre: user?.nombre || "Sin nombre",
        descripcion: user?.bio || "",
        rol: user?.esTutor ? "Profesor" : "Estudiante",
        email: user?.email || "Sin email",
        universidad: user?.universidad || "",
        grado: user?.grado || "",
        ubicacion: user?.ubicacion || "",
        foto: user?.foto || null,
        fotoBackgroundColor: user?.fotoBackgroundColor || '#ffffff',
        intereses: user?.intereses || []
    }

    // Función para mostrar valor o texto de "sin información"
    const displayValue = (value) => {
        return value && value.trim() !== '' ? value : <span className="no-info">Sin información</span>
    }

    // Función para formatear URL de imagen de comunidad
    const getCommunityImageUrl = (comunidad) => {
        const communityImageRaw = comunidad.imagen || comunidad.imagenUrl || comunidad.foto;
        
        if (!communityImageRaw || !String(communityImageRaw).trim()) {
            return 'https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&w=400&q=80';
        }

        const value = String(communityImageRaw).trim();
        if (/^https?:\/\//i.test(value) || value.startsWith('data:image/')) {
            return value;
        }

        const base = getApiBaseUrl();
        if (value.startsWith('/')) {
            return `${base}${value}`;
        }

        return `${base}/${value}`;
    }

    return (
        <>
            <Header page={'inicio'} />

            <main className="my-profile">
                {/* Sección de perfil principal */}
                <section className="profile-header">
                    <div className="profile-header__left">
                        <div className="profile-avatar" style={{ backgroundColor: userData.fotoBackgroundColor }}>
                            {userData.foto ? (
                                <img src={toAbsoluteImageUrl(userData.foto)} alt={userData.nombre} className="profile-avatar-img" />
                            ) : (
                                <span className="profile-avatar-placeholder">👤</span>
                            )}
                        </div>
                        <div className="profile-info">
                            <h1 className="profile-info__name">{userData.nombre}</h1>
                            <p className="profile-info__description">{userData.descripcion}</p>
                            <span className="profile-info__role">{userData.rol}</span>
                            {userData.intereses && userData.intereses.length > 0 && (
                                <div className="profile-info__interests">
                                    {userData.intereses.map((interes, index) => (
                                        <span key={index} className="profile-interest-tag">{interes}</span>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>
                    <div className="profile-header__right">
                        {isOwner && (
                            <>
                                <button className="btn-edit-profile" onClick={() => setShowEditProfile(true)}>
                                    <span className="btn-icon">✏️</span>
                                    Editar Perfil
                                </button>
                                <button className="btn-settings" onClick={() => setShowSettings(true)}>
                                    <span className="btn-icon">⚙️</span>
                                    Configuración
                                </button>
                            </>
                        )}
                    </div>
                </section>

                {/* Sección Mis datos y Tu Actividad */}
                <section className="profile-data-section">
                    <div className="profile-data">
                        <h2 className="section-title">{isOwner ? 'Mis datos' : 'Datos del perfil'}</h2>
                        <div className="profile-data__content">
                            <div className="data-field">
                                <span className="data-field__label">NOMBRE COMPLETO</span>
                                <span className="data-field__value">{displayValue(userData.nombre)}</span>
                            </div>
                            <div className="data-field">
                                <span className="data-field__label">EMAIL</span>
                                <span className="data-field__value">{displayValue(userData.email)}</span>
                            </div>
                            <div className="data-field">
                                <span className="data-field__label">UNIVERSIDAD</span>
                                <span className="data-field__value">{displayValue(userData.universidad)}</span>
                            </div>
                            <div className="data-field">
                                <span className="data-field__label">GRADO</span>
                                <span className="data-field__value">{displayValue(userData.grado)}</span>
                            </div>
                            <div className="data-field">
                                <span className="data-field__label">UBICACIÓN</span>
                                <span className="data-field__value">{displayValue(userData.ubicacion)}</span>
                            </div>
                        </div>
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
                </section>

                {/* Sección Mis comunidades */}
                <section className="my-communities-section">
                    <h2 className="section-title">Mis comunidades</h2>
                    {loadingCommunities ? (
                        <div className="loading-communities">Cargando comunidades...</div>
                    ) : misComunidades.length > 0 ? (
                        <div className="communities-list">
                            {misComunidades.map((comunidad) => (
                                <div key={comunidad.id} className="community-card" onClick={() => navigate(`/comunidades/${comunidad.id}`)} style={{ cursor: 'pointer' }}>
                                    <img 
                                        src={getCommunityImageUrl(comunidad)} 
                                        alt={comunidad.nombre} 
                                        className="community-card__image" 
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
                    )}
                </section>

                {/* Sección Comunidades creadas */}
                <section className="created-communities-section">
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
                                                <span className="members-icon">👥</span>
                                                <span className="members-text">Inscritos: <strong>{comunidad.miembrosActuales || 0}</strong>/{comunidad.maxMiembros || 0}</span>
                                            </div>
                                            {isOwner && (
                                                <div className="created-community-card__actions">
                                                    <Link to={`/comunidades/${comunidad.id}/editar`} className="action-link" onClick={(e) => e.stopPropagation()}>✏️ Editar</Link>
                                                    <Link to={`/comunidades/${comunidad.id}/apuntes`} className="action-link" onClick={(e) => e.stopPropagation()}>📄 Subir apuntes</Link>
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
                </section>
            </main>

            {/* Modal de configuración */}
            {showSettings && (
                <Settings onClose={() => setShowSettings(false)} isOwner={isOwner} />
            )}

            {/* Modal de editar perfil */}
            {showEditProfile && (
                <EditProfile 
                    onClose={() => setShowEditProfile(false)} 
                    onSave={(updatedUser) => {
                        // Los datos se actualizan automáticamente en el contexto
                        setShowEditProfile(false)
                    }}
                />
            )}
        </>
    )
}

export default MyProfile
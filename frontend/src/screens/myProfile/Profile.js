import { useState, useEffect } from "react"
import { Link, useNavigate } from "react-router-dom"
import { useAuth } from "../../contexts/AuthContext"
import { getMyTutorProfiles } from "../../api/tutorEndpoints"
import Header from "../../components/Header/Header"
import Settings from "./Settings"
import EditProfile from "./EditProfile"
import "./MyProfile.css"

const MyProfile = () => {
    const { isAuthenticated, loading, user } = useAuth()
    const navigate = useNavigate()
    const [showSettings, setShowSettings] = useState(false)
    const [showEditProfile, setShowEditProfile] = useState(false)
    const [checkingTutor, setCheckingTutor] = useState(true)
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
        intereses: user?.intereses || []
    }

    // Función para mostrar valor o texto de "sin información"
    const displayValue = (value) => {
        return value && value.trim() !== '' ? value : <span className="no-info">Sin información</span>
    }

    const stats = {
        comunidades: 12,
        apuntesSubidos: 45,
        valoracionMedia: 4.8,
        descargas: "1.2k"
    }

    const misComunidades = [
        {
            id: 1,
            nombre: "IISSI 2 - Universidad de Sevilla",
            descripcion: "Comunidad sobre la asignatura IISSI 2 para resolver exámenes y ejercicios."
        },
        {
            id: 2,
            nombre: "IISSI 2 - Universidad de Sevilla",
            descripcion: "Comunidad sobre la asignatura IISSI 2 para resolver exámenes y ejercicios."
        }
    ]

    const comunidadesCreadas = [
        {
            id: 1,
            nombre: "Biología 2º Bachillerato",
            descripcion: "Estudio sobre los temas y ejercicios de Biología centrados en la preparación para selectividad.",
            tags: ["Bachillerato", "Biología"],
            inscritos: 20,
            maxInscritos: 30
        },
        {
            id: 2,
            nombre: "Biología 2º Bachillerato",
            descripcion: "Estudio sobre los temas y ejercicios de Biología centrados en la preparación para selectividad.",
            tags: ["Bachillerato", "Biología"],
            inscritos: 20,
            maxInscritos: 30
        }
    ]

    return (
        <>
            <Header page={'inicio'} />
            
            <main className="my-profile">
                {/* Sección de perfil principal */}
                <section className="profile-header">
                    <div className="profile-header__left">
                        <div className="profile-avatar">
                            {userData.foto ? (
                                <img src={userData.foto} alt={userData.nombre} className="profile-avatar-img" />
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
                    <div className="communities-grid">
                        {misComunidades.map((comunidad) => (
                            <div key={comunidad.id} className="community-card">
                                <div className="community-card__image"></div>
                                <div className="community-card__content">
                                    <h3 className="community-card__name">{comunidad.nombre}</h3>
                                    <p className="community-card__description">{comunidad.descripcion}</p>
                                </div>
                            </div>
                        ))}
                        <div className="community-card community-card--explore">
                            <div className="explore-icon">+</div>
                            <h3 className="explore-title">Explorar más comunidades</h3>
                            <p className="explore-text">Busca entre miles de comunidades de estudio adaptadas a tus necesidades</p>
                        </div>
                        <div className="communities-view-all">
                            <a href="/comunidades" className="view-all-link">Ver todas</a>
                        </div>
                    </div>
                </section>

                {/* Sección Comunidades creadas */}
                <section className="created-communities-section">
                    <div className="created-header">
                        <div>
                            <h2 className="section-title">Comunidades creadas</h2>
                            <p className="section-subtitle">Crea comunidades, une a estudiantes y enseña sobre lo que sabes.</p>
                        </div>
                        <button className="btn-create-new">+ Crear Nueva</button>
                    </div>
                    <div className="created-communities-list">
                        {comunidadesCreadas.map((comunidad) => (
                            <div key={comunidad.id} className="created-community-card">
                                <div className="created-community-card__image"></div>
                                <div className="created-community-card__content">
                                    <div className="created-community-card__header">
                                        <h3 className="created-community-card__name">{comunidad.nombre}</h3>
                                        <div className="created-community-card__tags">
                                            {comunidad.tags.map((tag, index) => (
                                                <span key={index} className="tag">{tag}</span>
                                            ))}
                                        </div>
                                    </div>
                                    <p className="created-community-card__description">{comunidad.descripcion}</p>
                                    <p className="created-community-card__members">Personas inscritas: {comunidad.inscritos}/{comunidad.maxInscritos}</p>
                                    {isOwner && (
                                        <div className="created-community-card__actions">
                                            <a href="#editar" className="action-link">Editar</a>
                                            <a href="#subir" className="action-link">Subir apuntes</a>
                                        </div>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
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
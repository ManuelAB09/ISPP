import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useAuth } from "../../contexts/AuthContext";
import { getTutorById } from "../../api/tutorEndpoints";
import Header from "../../components/Header/Header";
import EditProfileModal from "./EditProfileModal";
import CreateProfileModal from "./CreateProfileModal";
import VerificacionModal from "./VerificacionModal";
import Settings from "../myProfile/Settings";
import HireTutorModal from "./HireTutorModal";
import "./TeacherProfile.css";


/* Estrellas (0-5) */
const Estrellas = ({ valor }) => (
  <span className="tp-estrellas">
    {[1, 2, 3, 4, 5].map((i) => (
      <span
        key={i}
        className={i <= Math.round(valor) ? "tp-star tp-star--filled" : "tp-star tp-star--empty"}
      >
        ★
      </span>
    ))}
  </span>
);

const TeacherProfile = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const esNuevo = id === "nuevo";

  const [tutor, setTutor] = useState(null);
  const [cargando, setCargando] = useState(!esNuevo);
  const [error, setError] = useState(null);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showVerificacion, setShowVerificacion] = useState(false);
  const [showSettings, setShowSettings] = useState(false);
  const [showHireModal, setShowHireModal] = useState(false);

  // Callback: actualiza estado local tras editar
  const handlePerfilGuardado = (updatedTutor) => {
    setTutor((prev) => ({
      ...prev,
      ...updatedTutor,
      usuario: {
        ...prev.usuario,
        ...(updatedTutor.usuario || {}),
        foto: updatedTutor.usuario?.foto || prev.usuario?.foto,
      },
    }));
  };

  // Callback: redirige al nuevo perfil tras crearlo
  const handlePerfilCreado = (newTutor) => {
    navigate(`/profesores/${newTutor.id}`, { replace: true });
  };

  useEffect(() => {
    if (esNuevo) return;
    const cargarTutor = async () => {
      try {
        const data = await getTutorById(id);
        setTutor(data);
      } catch (err) {
        console.error("Error al cargar el tutor:", err);
        setError("No se pudo cargar el perfil del tutor.");
      } finally {
        setCargando(false);
      }
    };
    cargarTutor();
  }, [id, esNuevo]);

  if (cargando) return <div className="tp-loading">Cargando perfil…</div>;
  if (error) return <div className="tp-error">{error}</div>;

  // Vista de creación: el usuario aún no tiene perfil de tutor
  if (esNuevo || !tutor) {
    return (
      <>
        <Header page={'profesores'} />
        {showCreateModal && (
          <CreateProfileModal
            onClose={() => setShowCreateModal(false)}
            onCreado={handlePerfilCreado}
          />
        )}
        <div className="tp-page">
          <div className="tp-banner">
            <header className="tp-header">
              <div className="tp-header__left">
                <div className="tp-header__info">
                  <h1 className="tp-header__name">Perfil de Profesor</h1>
                  <p className="tp-header__role">Aún no has creado tu perfil profesional</p>
                </div>
              </div>
              {user?.esTutor && (
              <div className="tp-header__actions">
                <button
                  className="tp-btn tp-btn--edit"
                  onClick={() => setShowCreateModal(true)}
                >
                  + Crear Perfil de Profesor
                </button>
              </div>
              )}
            </header>
          </div>
          <div className="tp-content">
            <div style={{ textAlign: 'center', padding: '60px 20px', color: '#9CA3AF' }}>
              <p style={{ fontSize: '48px', marginBottom: '16px' }}>🎓</p>
              <p style={{ fontSize: '18px', marginBottom: '8px', fontWeight: '600', color: '#374151' }}>
                Crea tu perfil de profesor
              </p>
              <p style={{ maxWidth: '400px', margin: '0 auto 24px' }}>
                Añade tus especialidades, establece tu tarifa y configura tu disponibilidad
                para que los alumnos puedan encontrarte.
              </p>
              {user?.esTutor && (
              <button
                className="tp-btn tp-btn--edit"
                onClick={() => setShowCreateModal(true)}
              >
                + Crear Perfil de Profesor
              </button>
              )}
            </div>
          </div>
        </div>
      </>
    );
  }

  return (
    <>
      <Header page={'profesores'} />
      {showVerificacion && (
        <VerificacionModal
          tutorId={tutor.id}
          verificado={tutor.verificado}
          onClose={() => setShowVerificacion(false)}
          onVerificado={() => {
            // Actualizamos el estado local para que el check verde aparezca al momento
            setTutor(prev => ({ ...prev, verificado: true }));
          }}
        />
      )}
      {showEditModal && (
        <EditProfileModal
          tutor={tutor}
          onClose={() => setShowEditModal(false)}
          onGuardar={handlePerfilGuardado}
        />
      )}
      {showCreateModal && (
        <CreateProfileModal
          onClose={() => setShowCreateModal(false)}
          onCreado={handlePerfilCreado}
        />
      )}
      {showSettings && (
        <Settings onClose={() => setShowSettings(false)} />
      )}
      <div className="tp-page">

      {/* ═══════════════ BANNER MORADO + CABECERA ═══════════════ */}
      <div className="tp-banner">
        <header className="tp-header">
          <div className="tp-header__left">
            <img
              className="tp-header__photo"
              src={tutor.usuario?.foto}
              alt={tutor.usuario?.nombre}
            />
            <div className="tp-header__info">
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <h1 className="tp-header__name">{tutor.usuario?.nombre}</h1>
                {tutor.verificado && (
                  <span style={{ 
                    backgroundColor: '#eafaf1', 
                    color: '#1a7c42', 
                    borderRadius: '50%', 
                    width: '24px', 
                    height: '24px', 
                    display: 'flex', 
                    alignItems: 'center', 
                    justifyContent: 'center',
                    fontSize: '14px',
                    fontWeight: 'bold',
                    border: '1px solid #1a7c42'
                  }} title="Tutor Verificado">✓</span>
                )}
              </div>
              <p className="tp-header__role">
                {tutor.especialidades && tutor.especialidades.length > 0
                  ? `Profesor de ${tutor.especialidades.join(", ")}`
                  : "Profesor"}
              </p>
              {tutor.actividad && (
                <div className="tp-header__rating">
                  <Estrellas valor={tutor.actividad.valoracion} />
                  <span className="tp-header__rating-num">
                    {tutor.actividad.valoracion}
                  </span>
                  <span className="tp-header__rating-count">
                    ({(tutor.opiniones || []).length} reseñas)
                  </span>
                </div>
              )}
              <span className="tp-badge tp-badge--profesor">Profesor</span>
            </div>
          </div>

          {/* Acciones del perfil: sólo visibles para el propietario */}
          {user?.id === tutor.usuario?.id && (
          <div className="tp-header__actions">
            <button
              className="tp-btn tp-btn--edit"
              onClick={() => setShowEditModal(true)}
            >
              Editar Perfil
            </button>
            <button
              className="tp-btn tp-btn--promote"
              onClick={() => setShowVerificacion(true)}
            >
              {tutor.verificado ? "🏅 Verificado" : "Promocionarse"}
            </button>
            <button
              className="tp-btn tp-btn--public"
              onClick={() => setShowSettings(true)}
            >
              ⚙️ Configuración
            </button>
          </div>
          )}

          {/* Acciones para visitantes (otro usuario viendo el perfil) */}
          {user?.id !== tutor.usuario?.id && (
          <div className="tp-header__actions">
            <button className="tp-btn tp-btn--contact">
              💬 Contactar
            </button>
            <button className="tp-btn tp-btn--hire" onClick={() => setShowHireModal(true)}>
              🎓 Contratar
            </button>
          </div>
          )}
        </header>
      </div>

      {/* ═══════════════ CONTENIDO PRINCIPAL (fondo blanco plano) ═══════════════ */}
      <div className="tp-content">

      {/* ═══════════════ FILA: MIS DATOS + ACTIVIDAD ═══════════════ */}
      <div className="tp-row tp-row--datos-actividad">
        {/* — Mis datos — */}
        <section className="tp-datos">
          <h2 className="tp-section-title">Mis datos</h2>

          <div className="tp-dato">
            <span className="tp-dato__label">NOMBRE COMPLETO</span>
            <span className="tp-dato__value">{tutor.usuario?.nombre}</span>
          </div>
          {/* email, universidad, grado y ubicacion no existen en el backend (Usuario entity).
              TODO: Añadir estos campos al entity/DTO cuando el backend los soporte. */}
          <div className="tp-dato">
            <span className="tp-dato__label">BIO</span>
            <span className="tp-dato__value">{tutor.usuario?.bio || tutor.bio || "—"}</span>
          </div>
          <div className="tp-dato">
            <span className="tp-dato__label">ESPECIALIDADES</span>
            <span className="tp-dato__value">
              {tutor.especialidades && tutor.especialidades.length > 0
                ? tutor.especialidades.join(", ")
                : "—"}
            </span>
          </div>
          <div className="tp-dato">
            <span className="tp-dato__label">VERIFICADO</span>
            <span className="tp-dato__value">{tutor.verificado ? "Sí ✓" : "No"}</span>
          </div>
          <div className="tp-dato">
            <span className="tp-dato__label">TARIFA POR HORA</span>
            <span className="tp-dato__value">{tutor.tarifaHora}€ / h</span>
          </div>
        </section>

        {/* — Tu Actividad — */}
        <div className="tp-actividad-col">
          {tutor.actividad ? (
            <section className="tp-actividad">
              <h2 className="tp-actividad__title">Tu Actividad</h2>
              <div className="tp-actividad__grid">
                <div className="tp-actividad__stat">
                  <span className="tp-actividad__num">{tutor.actividad.comunidades ?? "—"}</span>
                  <span className="tp-actividad__label">COMUNIDADES</span>
                </div>
                <div className="tp-actividad__stat">
                  <span className="tp-actividad__num">{tutor.actividad.apuntes ?? "—"}</span>
                  <span className="tp-actividad__label">APUNTES SUBIDOS</span>
                </div>
                <div className="tp-actividad__stat">
                  <span className="tp-actividad__num">{tutor.actividad.valoracion ?? "—"}</span>
                  <span className="tp-actividad__label">VALORACIÓN MEDIA</span>
                </div>
                <div className="tp-actividad__stat">
                  <span className="tp-actividad__num">
                    {tutor.actividad.descargas != null
                      ? tutor.actividad.descargas >= 1000
                        ? `${(tutor.actividad.descargas / 1000).toFixed(1)}k`
                        : tutor.actividad.descargas
                      : "—"}
                  </span>
                  <span className="tp-actividad__label">DESCARGAS</span>
                </div>
              </div>
            </section>
          ) : null}
          <div className="tp-actividad-col__extra">
          </div>
        </div>
      </div>

      {/* ═══════════════ MIS COMUNIDADES (AMPLIADO) ═══════════════ */}
      <section className="tp-comunidades tp-comunidades--ampliada">
        <div className="tp-section-title-row">
          <h2 className="tp-section-title">Mis comunidades</h2>
          <div className="tp-section-title-row__line" />
        </div>
        <div className="tp-comunidades__grid tp-comunidades__grid--xl">
          {(tutor.comunidades || []).map((c, i) => (
            <div key={i} className="tp-comunidades__card tp-comunidades__card--xl">
              <div className="tp-comunidades__img tp-comunidades__img--xl" />
              <div className="tp-comunidades__info tp-comunidades__info--xl">
                <span className="tp-comunidades__name tp-comunidades__name--xl">{c.nombre}</span>
                <span className="tp-comunidades__desc tp-comunidades__desc--xl">{c.descripcion}</span>
              </div>
            </div>
          ))}
          {/* Placeholder "Explorar más comunidades" */}
          <div className="tp-comunidades__card tp-comunidades__card--explore tp-comunidades__card--xl">
            <div className="tp-comunidades__explore-icon tp-comunidades__explore-icon--xl">+</div>
            <span className="tp-comunidades__explore-title tp-comunidades__explore-title--xl">Explorar más comunidades</span>
            <span className="tp-comunidades__explore-text tp-comunidades__explore-text--xl">
              Busca entre miles de comunidades de estudio adaptadas a tus necesidades
            </span>
          </div>
          <span className="tp-comunidades__ver-todas tp-comunidades__ver-todas--xl">Ver todas</span>
        </div>
      </section>

      {/* ═══════════════ COMUNIDADES CREADAS (AMPLIADO) ═══════════════ */}
      <section className="tp-creadas tp-creadas--ampliada">
        <div className="tp-section-title-row">
          <h2 className="tp-section-title">Comunidades creadas</h2>
          <div className="tp-section-title-row__line" />
        </div>
        <div className="tp-creadas__header tp-creadas__header--xl">
          <p className="tp-creadas__subtitle tp-creadas__subtitle--xl">
            Crea comunidades, une a estudiantes y enseña sobre lo que sabes.
          </p>
          {user?.id === tutor.usuario?.id && (
            <button className="tp-btn tp-btn--crear tp-btn--crear-xl">+ Crear Nueva</button>
          )}
        </div>
        <div className="tp-creadas__list tp-creadas__list--xl">
          {(tutor.comunidadesCreadas || []).map((c, i) => (
            <div key={i} className="tp-creadas__item tp-creadas__item--xl">
              <div className="tp-creadas__img tp-creadas__img--xl" />
              <div className="tp-creadas__info tp-creadas__info--xl">
                <div className="tp-creadas__name-row tp-creadas__name-row--xl">
                  <span className="tp-creadas__name tp-creadas__name--xl">{c.nombre}</span>
                  {c.etiquetas.map((e, j) => (
                    <span key={j} className="tp-badge tp-badge--tag tp-badge--tag-xl">{e}</span>
                  ))}
                </div>
                <p className="tp-creadas__desc tp-creadas__desc--xl">{c.descripcion}</p>
                <p className="tp-creadas__inscritos tp-creadas__inscritos--xl">
                  <b>Personas inscritas: {c.inscritos}/{c.total}</b>
                </p>
                {user?.id === tutor.usuario?.id && (
                <div className="tp-creadas__actions tp-creadas__actions--xl">
                  <span className="tp-creadas__action tp-creadas__action--xl">Editar</span>
                  <span className="tp-creadas__action tp-creadas__action--xl">Subir apuntes</span>
                </div>
                )}
              </div>
            </div>
          ))}
        </div>
      </section>

      </div>{/* cierre tp-content */}
    </div>

    {showHireModal && (
      <HireTutorModal tutor={tutor} onClose={() => setShowHireModal(false)} />
    )}
    </>
  );
};

export default TeacherProfile;

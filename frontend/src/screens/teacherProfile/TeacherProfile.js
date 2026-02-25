import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { getTutorById } from "../../api/tutorEndpoints";
import EditProfileModal from "./EditProfileModal";
import VerificacionModal from "./VerificacionModal";
import "./TeacherProfile.css";

// ---------------------------------------------------------------------------
// Mock data — refleja la estructura exacta de TutorProfileResponse del backend.
// Campos de `usuario` coinciden con UsuarioDto:
//   { id, nombre, foto, bio, intereses, esTutor }
// Nota: foto/bio/intereses están comentados en mapToResponse, se devuelven null.
// Los campos email, universidad, grado y ubicacion NO existen en el backend.
// TODO: Eliminar este bloque cuando el endpoint GET /api/tutors/:id esté integrado.
// ---------------------------------------------------------------------------
const MOCK_TUTORES = {
  "1": {
    id: 1,
    userId: 1,
    usuario: {
      id: 1,
      nombre: "Alberto Gómez",
      foto: "https://randomuser.me/api/portraits/men/32.jpg",
      bio: "Apasionado de la tecnología",
      esTutor: true,
    },
    especialidades: ["Programación", "Bachillerato"],
    tarifaHora: 22.5,
    disponibilidad: "Mañanas y tardes",
    bio: "Profesor senior con más de 10 años de experiencia.",
    verificado: true,
    classroomConectado: false,
    actividad: { comunidades: 12, apuntes: 45, valoracion: 4.8, descargas: 1200 },
    trayectoria: [{ titulo: "Profesor", empresa: "UCM", fecha: "2015", descripcion: "..." }],
    opiniones: [{ usuario: "Laura M.", valoracion: 5, texto: "Excelente" }],
    comunidades: [
      { id: 1, nombre: "ISSI 2 - US", descripcion: "Resolución de exámenes y dudas.", imagen: null },
      { id: 2, nombre: "Fundamentos de Programación", descripcion: "Comunidad para principiantes en C++.", imagen: null }
    ],
    comunidadesCreadas: [
      { id: 1, nombre: "Java desde Cero", etiquetas: ["Programación", "Java"], descripcion: "Curso completo de Java para todos los niveles.", inscritos: 45, total: 100, imagen: null },
      { id: 2, nombre: "Algoritmos y Estructuras", etiquetas: ["Ingeniería", "CS"], descripcion: "Preparación para entrevistas técnicas y exámenes.", inscritos: 30, total: 50, imagen: null }
    ]
  },
  "2": {
    id: 2,
    userId: 2,
    usuario: {
      id: 2,
      nombre: "Manuel Nuño",
      foto: "https://randomuser.me/api/portraits/men/44.jpg",
      bio: "Experto en Matemáticas",
      esTutor: true,
    },
    especialidades: ["Matemáticas", "Física"],
    tarifaHora: 18.0,
    disponibilidad: "Fines de semana",
    bio: "Graduado en Matemáticas con pasión por la enseñanza.",
    verificado: false,
    classroomConectado: false,
    actividad: { comunidades: 5, apuntes: 10, valoracion: 4.5, descargas: 300 },
    trayectoria: [{ titulo: "Tutor", empresa: "Academia Plus", fecha: "2020", descripcion: "Clases de refuerzo" }],
    opiniones: [{ usuario: "Carlos R.", valoracion: 4, texto: "Muy buena explicando" }],
    comunidades: [
      { id: 3, nombre: "Álgebra Lineal US", descripcion: "Espacios vectoriales y matrices.", imagen: null },
      { id: 4, nombre: "Cálculo II", descripcion: "Integrales múltiples y series.", imagen: null }
    ],
    comunidadesCreadas: [
      { id: 3, nombre: "Matemáticas 2º Bachillerato", etiquetas: ["Selectividad", "Mates"], descripcion: "Preparación intensiva para la PEvAU.", inscritos: 25, total: 30, imagen: null },
      { id: 4, nombre: "Física Cuántica Básica", etiquetas: ["Física", "Universidad"], descripcion: "Introducción a los conceptos fundamentales.", inscritos: 12, total: 20, imagen: null }
    ]
  }
};

const MOCK_TUTOR = MOCK_TUTORES["1"];

/* Colores para avatares de opiniones */
const AVATAR_COLORS = ["#F2C18E", "#676F9D", "#2D3250", "#9CA3AF"];

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
  const [tutor, setTutor] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showVerificacion, setShowVerificacion] = useState(false);

  // Callback: recibe el TutorProfileResponse actualizado tras editar
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

  useEffect(() => {
    const cargarTutor = async () => {
      try {
        const data = await getTutorById(id);

        // Combinar respuesta real con mock para los campos sin endpoint propio todavía.
        // La API devuelve: id, userId, usuario{id,nombre,foto,bio,intereses,esTutor},
        //   especialidades, tarifaHora, disponibilidad, bio, verificado,
        //   classroomConectado, createdAt.
        // Los demás campos (actividad, trayectoria, opiniones, comunidades, etc.)
        // están pendientes de endpoints propios (ver TODOs en MOCK_TUTOR).
        const mappedTutor = {
          ...MOCK_TUTOR,             // fallback para campos sin endpoint aún
          ...data,                   // sobreescribe con datos reales del backend
          usuario: {
            ...MOCK_TUTOR.usuario,   // foto de fallback si el backend devuelve null
            ...(data.usuario || {}), // datos reales del usuario
            // foto viene null del backend (mapToResponse lo tiene comentado)
            foto: data.usuario?.foto || MOCK_TUTOR.usuario.foto,
          },
        };

        setTutor(mappedTutor);
      } catch (err) {
        console.error("Error al cargar el tutor:", err);
        // Fallback: Si falla la API, usamos los datos Mock según el ID
        const mockSeleccionado = MOCK_TUTORES[id] || MOCK_TUTORES["1"];
        setTutor({ ...mockSeleccionado });
        // No establecemos el estado de error para que la página se renderice
      } finally {
        setCargando(false);
      }
    };
    cargarTutor();
  }, [id]);

  if (cargando) return <div className="tp-loading">Cargando perfil…</div>;
  if (error) return <div className="tp-error">{error}</div>;
  if (!tutor) return null;

  return (
    <>
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
      <div className="tp-page">

      {/* ═══════════════ BANNER MORADO + CABECERA ═══════════════ */}
      <div className="tp-banner">
        <header className="tp-header">
          <div className="tp-header__left">
            <img
              className="tp-header__photo"
              src={tutor.usuario.foto}
              alt={tutor.usuario.nombre}
            />
            <div className="tp-header__info">
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <h1 className="tp-header__name">{tutor.usuario.nombre}</h1>
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
              <div className="tp-header__rating">
                <Estrellas valor={tutor.actividad.valoracion} />
                <span className="tp-header__rating-num">
                  {tutor.actividad.valoracion}
                </span>
                <span className="tp-header__rating-count">
                  ({tutor.opiniones.length} reseñas)
                </span>
              </div>
              <span className="tp-badge tp-badge--profesor">Profesor</span>
            </div>
          </div>

          {/* Acciones del perfil propio */}
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
            <button className="tp-btn tp-btn--public">Ver perfil público</button>
          </div>
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
            <span className="tp-dato__value">{tutor.usuario.nombre}</span>
          </div>
          {/* email, universidad, grado y ubicacion no existen en el backend (Usuario entity).
              TODO: Añadir estos campos al entity/DTO cuando el backend los soporte. */}
          <div className="tp-dato">
            <span className="tp-dato__label">BIO</span>
            <span className="tp-dato__value">{tutor.usuario.bio || tutor.bio || "—"}</span>
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
          <section className="tp-actividad">
            <h2 className="tp-actividad__title">Tu Actividad</h2>
            <div className="tp-actividad__grid">
              <div className="tp-actividad__stat">
                <span className="tp-actividad__num">{tutor.actividad.comunidades}</span>
                <span className="tp-actividad__label">COMUNIDADES</span>
              </div>
              <div className="tp-actividad__stat">
                <span className="tp-actividad__num">{tutor.actividad.apuntes}</span>
                <span className="tp-actividad__label">APUNTES SUBIDOS</span>
              </div>
              <div className="tp-actividad__stat">
                <span className="tp-actividad__num">{tutor.actividad.valoracion}</span>
                <span className="tp-actividad__label">VALORACIÓN MEDIA</span>
              </div>
              <div className="tp-actividad__stat">
                <span className="tp-actividad__num">
                  {tutor.actividad.descargas >= 1000
                    ? `${(tutor.actividad.descargas / 1000).toFixed(1)}k`
                    : tutor.actividad.descargas}
                </span>
                <span className="tp-actividad__label">DESCARGAS</span>
              </div>
            </div>
          </section>
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
          {tutor.comunidades.map((c, i) => (
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
          <button className="tp-btn tp-btn--crear tp-btn--crear-xl">+ Crear Nueva</button>
        </div>
        <div className="tp-creadas__list tp-creadas__list--xl">
          {tutor.comunidadesCreadas.map((c, i) => (
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
                <div className="tp-creadas__actions tp-creadas__actions--xl">
                  <span className="tp-creadas__action tp-creadas__action--xl">Editar</span>
                  <span className="tp-creadas__action tp-creadas__action--xl">Subir apuntes</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </section>

      </div>{/* cierre tp-content */}
    </div>
    </>
  );
};

export default TeacherProfile;

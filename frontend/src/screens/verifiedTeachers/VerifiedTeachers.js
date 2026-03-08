import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { getVerifiedTutors } from "../../api/tutorEndpoints";
import Header from "../../components/Header/Header";
import { useAuth } from "../../contexts/AuthContext";
import "./VerifiedTeachers.css";

/**
 * Pantalla de listado de profesores verificados.
 * Usa GET /api/v1/tutors → Page<TutorProfileResponse> (DTO, evita ciclos de serialización).
 */

const VerifiedTeachers = () => {
  const navigate = useNavigate();
  const { isAuthenticated, user } = useAuth();
  const [profesores, setProfesores] = useState([]);
  const [total, setTotal] = useState(0);
  const [pagina, setPagina] = useState(0);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);

  // Filtros
  const [filtros, setFiltros] = useState({
    especialidad: "",
    tarifaMin: "",
    tarifaMax: "",
  });
  const [filtrosActivos, setFiltrosActivos] = useState({
    especialidad: "",
    tarifaMin: "",
    tarifaMax: "",
  });

  const cargarProfesores = useCallback(
    async (nuevaPagina = 0, filtrosParam = filtrosActivos) => {
      setCargando(true);
      setError(null);
      try {
        const resp = await getVerifiedTutors({
          especialidad: filtrosParam.especialidad || undefined,
          tarifaMin: filtrosParam.tarifaMin || undefined,
          tarifaMax: filtrosParam.tarifaMax || undefined,
          page: nuevaPagina,
          size: 20,
        });
        // La respuesta es Page<TutorProfileResponse>: { content, totalElements, number, ... }
        const contenido = resp?.content ?? (Array.isArray(resp) ? resp : []);
        const totalElem = resp?.totalElements ?? contenido.length;
        setProfesores((prev) =>
          nuevaPagina === 0 ? contenido : [...prev, ...contenido]
        );
        setTotal(totalElem);
        setPagina(nuevaPagina);
      } catch (err) {
        console.error("Error al cargar profesores:", err);
        setError("No se pudieron cargar los profesores.");
      } finally {
        setCargando(false);
      }
    },
    [filtrosActivos]
  );

  useEffect(() => {
    cargarProfesores(0, filtrosActivos);
    // eslint-disable-next-line
  }, [filtrosActivos]);

  const handleFiltroChange = (e) => {
    const { name, value } = e.target;
    setFiltros((prev) => ({ ...prev, [name]: value }));
  };

  const handleBuscar = (e) => {
    e.preventDefault();
    setFiltrosActivos({ ...filtros });
  };

  const handleLimpiar = () => {
    const vacios = { especialidad: "", tarifaMin: "", tarifaMax: "" };
    setFiltros(vacios);
    setFiltrosActivos(vacios);
  };

  const handleCargarMas = () => {
    cargarProfesores(pagina + 1);
  };

  /* ─── Helpers ─── */
  const getNombre = (tutor) =>
    tutor?.usuario?.nombre || tutor?.us?.nombre || "Profesor";

  const getIniciales = (nombre) =>
    nombre
      .split(" ")
      .slice(0, 2)
      .map((w) => w[0]?.toUpperCase() ?? "")
      .join("");

  const AVATAR_COLORS = ["#676F9D", "#F2C18E", "#2D3250", "#9CA3AF", "#22c55e"];

  return (
    <div className="vt-page">
      <Header page={'profesores'} />
      {/* ── Header ──────────────────────────────────────────── */}
      <div className="vt-header">
        <div className="vt-header__inner">
          <div className="headerTitle">
            <p>Profesionales con identidad confirmada, calidad contrastada y acceso directo al contacto</p>
            <span className="line"></span>
            <h1>Profesores Verificados</h1>
          </div>
        </div>
      </div>

      {/* ── Filtros ───────────────────────────────────── */}
      <div className="vt-filtros-bar">
        <form className="vt-filtros" onSubmit={handleBuscar}>
          <input
            className="vt-filtros__input vt-filtros__input--lg"
            name="especialidad"
            type="text"
            placeholder="Buscar por especialidad…"
            value={filtros.especialidad}
            onChange={handleFiltroChange}
          />
          <div className="vt-filtros__tarifa">
            <input
              className="vt-filtros__input vt-filtros__input--sm"
              name="tarifaMin"
              type="number"
              min="0"
              step="1"
              placeholder="€ mín"
              value={filtros.tarifaMin}
              onChange={handleFiltroChange}
            />
            <span className="vt-filtros__sep">—</span>
            <input
              className="vt-filtros__input vt-filtros__input--sm"
              name="tarifaMax"
              type="number"
              min="0"
              step="1"
              placeholder="€ máx"
              value={filtros.tarifaMax}
              onChange={handleFiltroChange}
            />
            <span className="vt-filtros__unit">€/h</span>
          </div>
          <button type="submit" className="vt-btn vt-btn--primary">Buscar</button>
          {(filtrosActivos.especialidad || filtrosActivos.tarifaMin || filtrosActivos.tarifaMax) && (
            <button type="button" className="vt-btn vt-btn--ghost" onClick={handleLimpiar}>
              Limpiar
            </button>
          )}
        </form>
        {!cargando && !error && (
          <span className="vt-total">
            {total} profesor{total !== 1 ? "es" : ""} verificado{total !== 1 ? "s" : ""}
          </span>
        )}
      </div>

      {/* ── Contenido ─────────────────────────────────── */}
      <div className="vt-content">
        {error && (
          <div className="vt-estado vt-estado--error">
            <p>{error}</p>
            <button className="vt-btn vt-btn--primary" onClick={() => cargarProfesores(0)}>
              Reintentar
            </button>
          </div>
        )}

        {!error && profesores.length === 0 && !cargando && (
          <div className="vt-estado">
            <p>No se encontraron profesores con los filtros actuales.</p>
            <button className="vt-btn vt-btn--ghost" onClick={handleLimpiar}>
              Ver todos
            </button>
          </div>
        )}

        {!error && (
          <div className="vt-grid">
            {profesores.map((tutor, i) => {
              const nombre = getNombre(tutor);
              const especialidades = tutor.especialidades ?? [];
              const tarifa = tutor.tarifaHora;

              return (
                <div key={tutor.id ?? i} className="vt-card">
                  {/* Insignia verificado */}
                  <span className="vt-card__badge">Verificado</span>

                  {/* Avatar */}
                  {tutor.usuario?.foto ? (
                    <img
                      className="vt-card__avatar-img"
                      src={tutor.usuario.foto}
                      alt={nombre}
                    />
                  ) : (
                    <div
                      className="vt-card__avatar"
                      style={{ background: AVATAR_COLORS[i % AVATAR_COLORS.length] }}
                    >
                      {getIniciales(nombre)}
                    </div>
                  )}

                  {/* Info */}
                  <h3 className="vt-card__nombre">{nombre}</h3>

                  {especialidades.length > 0 && (
                    <div className="vt-card__tags">
                      {especialidades.slice(0, 3).map((esp, j) => (
                        <span key={j} className="vt-card__tag">{esp}</span>
                      ))}
                      {especialidades.length > 3 && (
                        <span className="vt-card__tag vt-card__tag--more">
                          +{especialidades.length - 3}
                        </span>
                      )}
                    </div>
                  )}

                  {tarifa != null && (
                    <p className="vt-card__tarifa">
                      <strong>{Number(tarifa).toFixed(2)} €</strong> / hora
                    </p>
                  )}

                  {tutor.disponibilidad && (
                    <p className="vt-card__disponibilidad">{tutor.disponibilidad}</p>
                  )}

                  {/* Acciones */}
                  <div className="vt-card__actions">
                    <Link
                      to={`/profesores/${tutor.id}`}
                      className="vt-btn vt-btn--outline"
                    >
                      Ver perfil
                    </Link>
                    {/* Contactar */}
                    <button
                      className="vt-btn vt-btn--primary"
                    >
                      Contactar
                    </button>
                  </div>
                </div>
              );
            })}

            {/* Skeleton cards mientras carga */}
            {cargando &&
              Array.from({ length: 6 }).map((_, i) => (
                <div key={`sk-${i}`} className="vt-card vt-card--skeleton" />
              ))}
          </div>
        )}

        {/* Cargar más */}
        {!cargando &&
          !error &&
          profesores.length < total && (
            <div className="vt-cargar-mas">
              <button className="vt-btn vt-btn--outline" onClick={handleCargarMas}>
                Cargar más profesores
              </button>
            </div>
          )}
      </div>
    </div>
  );
};

export default VerifiedTeachers;

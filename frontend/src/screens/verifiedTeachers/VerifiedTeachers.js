import { useCallback, useEffect, useState } from "react";
import { getValoracionesStats } from "../../api/valoraciones.api";
import { Link, useNavigate } from "react-router-dom";
import { getVerifiedTutors } from "../../api/tutorEndpoints";
import { getApiBaseUrl } from "../../api/baseUrl";
import Header from "../../components/Header/Header";
import { useAuth } from "../../contexts/AuthContext";
import { filterTutorsByDistance, formatDistance, calculateDistance } from "../../utils/geoUtils";
import "./VerifiedTeachers.css";

/**
 * Pantalla de listado de profesores verificados.
 * Usa GET /api/v1/tutors → Page<TutorProfileResponse> (DTO, evita ciclos de serialización).
 */

const VerifiedTeachers = () => {
  const hasValidCoords = (ubicacion) => {
    if (!ubicacion || typeof ubicacion !== 'object') return false;
    const lat = Number(ubicacion.latitud);
    const lon = Number(ubicacion.longitud);
    return Number.isFinite(lat) && Number.isFinite(lon);
  };

  const DEFAULT_PROFILE_AVATAR =
    "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 120 120'%3E%3Ccircle cx='60' cy='60' r='60' fill='%23E6EAF3'/%3E%3Ccircle cx='60' cy='46' r='22' fill='%2395A1BB'/%3E%3Cpath d='M20 106c6-20 22-32 40-32s34 12 40 32' fill='%2395A1BB'/%3E%3C/svg%3E";

  const toAbsoluteImageUrl = (imageUrl) => {
    const raw = String(imageUrl || "").trim();
    if (!raw) return DEFAULT_PROFILE_AVATAR;
    if (/^https?:\/\//i.test(raw) || raw.startsWith("data:") || raw.startsWith("blob:")) {
      return raw;
    }
    const base = getApiBaseUrl();
    return raw.startsWith("/") ? `${base}${raw}` : `${base}/${raw}`;
  };

  const { isAuthenticated, user, refreshUser } = useAuth();
  const navigate = useNavigate();
  const [profesores, setProfesores] = useState([]);
  const [profesoresOriginales, setProfesoresOriginales] = useState([]);
  const [total, setTotal] = useState(0);
  const [pagina, setPagina] = useState(0);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [mostrarModalCercania, setMostrarModalCercania] = useState(false);
  const [busquedaCercaniaActiva, setBusquedaCercaniaActiva] = useState(false);
  const [radioKm, setRadioKm] = useState(10);

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
        let contenido = resp?.content ?? (Array.isArray(resp) ? resp : []);
        contenido = [...contenido].sort(
          (a, b) => Number(Boolean(b?.verificado)) - Number(Boolean(a?.verificado))
        );
        const totalElem = resp?.totalElements ?? contenido.length;
        const userHasCoords = hasValidCoords(user?.ubicacion);
        
        // Calcular distancias si el usuario tiene ubicación
        if (userHasCoords) {
          const userLat = Number(user.ubicacion.latitud);
          const userLon = Number(user.ubicacion.longitud);
          contenido = contenido.map(tutor => {
            if (user?.id != null && tutor?.userId === user.id) {
              return {
                ...tutor,
                distanciaKm: 0,
              };
            }
            if (hasValidCoords(tutor.ubicacion)) {
              return {
                ...tutor,
                distanciaKm: calculateDistance(
                  userLat,
                  userLon,
                  Number(tutor.ubicacion.latitud),
                  Number(tutor.ubicacion.longitud)
                )
              };
            }
            return tutor;
          });
        }
        
        setProfesores((prev) =>
          nuevaPagina === 0 ? contenido : [...prev, ...contenido]
        );
        setProfesoresOriginales((prev) =>
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
    [filtrosActivos, user]
  );

  useEffect(() => {
    if (!isAuthenticated) {
      return;
    }
    refreshUser();
  }, [isAuthenticated, refreshUser]);

  useEffect(() => {
    cargarProfesores(0, filtrosActivos);
    // eslint-disable-next-line
  }, [filtrosActivos]);

  // Recargar profesores cuando cambie la ubicación o foto del usuario
  // para reflejar cambios en el perfil del usuario si es tutor
  const ubicacionStr = JSON.stringify(user?.ubicacion);
  useEffect(() => {
    if (user?.esTutor) {
      cargarProfesores(0, filtrosActivos);
    }
    // eslint-disable-next-line
  }, [
    user?.foto, 
    user?.nombre,
    ubicacionStr
  ]);

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
    setBusquedaCercaniaActiva(false);
    setProfesores(profesoresOriginales);
  };

  const handleBuscarPorCercania = () => {
    if (!user?.ubicacion) {
      alert('No tienes una ubicación configurada en tu perfil. Por favor, edita tu perfil para añadir una ubicación.');
      return;
    }

    if (!hasValidCoords(user.ubicacion)) {
      alert('Tu ubicación no tiene coordenadas válidas. Por favor, actualiza tu ubicación en el perfil.');
      return;
    }

    setMostrarModalCercania(true);
  };

  const aplicarBusquedaCercania = () => {
    // Filtrar solo tutores con ubicación válida
    const tutoresConUbicacion = profesoresOriginales.filter(
      (t) => hasValidCoords(t.ubicacion)
    );

    // Filtrar y calcular distancia para los que tienen ubicación dentro del radio
    const tutoresFiltrados = filterTutorsByDistance(
      tutoresConUbicacion,
      Number(user.ubicacion.latitud),
      Number(user.ubicacion.longitud),
      radioKm
    );

    // Solo mostrar los profesores dentro del radio especificado
    setProfesores(tutoresFiltrados);
    setBusquedaCercaniaActiva(true);
    setMostrarModalCercania(false);
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
  const userHasCoords = hasValidCoords(user?.ubicacion);

  // Estado para stats de valoraciones por tutorId
  const [valoracionesStats, setValoracionesStats] = useState({});

  // Cargar stats de valoraciones para los profesores listados, con cache en estado
  useEffect(() => {
    const fetchStats = async () => {
      // Obtener los IDs de los profesores actualmente visibles
      const idsVisibles = profesores
        .map((tutor) => tutor.id ?? tutor.userId ?? tutor.usuario?.id)
        .filter((id) => !!id);
      // Filtrar solo los IDs que aún no tienen stats en cache
      const idsFaltantes = idsVisibles.filter(
        (id) => valoracionesStats[id] === undefined
      );
      if (idsFaltantes.length === 0) {
        return;
      }
      const nuevosStats = {};
      await Promise.all(
        idsFaltantes.map(async (id) => {
          try {
            const res = await getValoracionesStats(id);
            // Espera que la respuesta tenga { media, total }
            nuevosStats[id] = res;
          } catch (e) {
            // Si falla, ignora
          }
        })
      );
      if (Object.keys(nuevosStats).length > 0) {
        setValoracionesStats((prev) => ({
          ...prev,
          ...nuevosStats,
        }));
      }
    };
    if (profesores.length > 0) {
      fetchStats();
    }
  }, [profesores, valoracionesStats]);

  
  // Función para determinar el nivel
  // Badge compacto: solo letra y color igual que badge de distancia
  const getNivelBadge = (media, total) => {
    if (total < 10 || media < 3) return { label: "P", full: "Principiante", color: "#676F9D" };
    if (total >= 10 && total <= 50 && media >= 3) return { label: "A", full: "Avanzado", color: "#52c41a" };
    if (total > 50 && media >= 4.5) return { label: "E", full: "Experto", color: "#1890ff" };
    return null;
  };

  return (
    <div className="vt-page">
      <Header page={'profesores'} />
      {/* ── Header ──────────────────────────────────────────── */}
      <div className="vt-header">
        <div className="vt-header__inner">

          <div className="headerTitle">
            <p>Profesionales con identidad confirmada</p>
            <span className="line"></span>
            <h1>Profesores</h1>
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
              placeholder="€ min"
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
              placeholder="€ max"
              value={filtros.tarifaMax}
              onChange={handleFiltroChange}
            />
            <span className="vt-filtros__unit">€/h</span>
          </div>
          <button type="submit" className="vt-btn vt-btn--primary">Buscar</button>
          <button 
            type="button" 
            className="vt-btn vt-btn--secondary"
            onClick={handleBuscarPorCercania}
          >
            📍 Buscar por cercanía
          </button>
          {(filtrosActivos.especialidad || filtrosActivos.tarifaMin || filtrosActivos.tarifaMax || busquedaCercaniaActiva) && (
            <button type="button" className="vt-btn vt-btn--ghost" onClick={handleLimpiar}>
              Limpiar
            </button>
          )}
        </form>
        {!cargando && !error && (
          <span className="vt-total">
            {total} profesor{total !== 1 ? "es" : ""}
            {busquedaCercaniaActiva && (
              <span style={{ marginLeft: 8, color: '#676F9D' }}>
                • A menos de {radioKm} km de ti
              </span>
            )}
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

              // Badge de nivel
              const id = tutor.id ?? tutor.userId ?? tutor.usuario?.id;
              const stats = valoracionesStats[id] || {};
              const nivel = getNivelBadge(stats.media, stats.total);
              return (
                <div key={tutor.id ?? i} className="vt-card">
                  {/* Contenedor de badges en esquina superior derecha */}
                  <div style={{ position: 'absolute', top: '1rem', right: '1rem', display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 4, zIndex: 2 }}>
                    {tutor.verificado && (
                      <span className="vt-card__badge">Verificado</span>
                    )}
                    {nivel && (
                      <span className="vt-card__badge vt-card__badge-nivel" style={{ background: nivel.color, color: '#fff', fontWeight: 700, fontSize: '1em', borderRadius: '50%', width: 22, height: 22, display: 'inline-flex', alignItems: 'center', justifyContent: 'center', marginLeft: 0 }} title={nivel.full}>
                        {nivel.label}
                      </span>
                    )}
                  </div>

                  {/* Etiqueta de distancia */}
                  {userHasCoords && (
                    <span className={`vt-card__badge-distancia ${tutor.distanciaKm == null ? 'vt-card__badge-distancia--sin' : ''}`}>
                      {tutor.distanciaKm != null ? (
                        <>📍 {formatDistance(tutor.distanciaKm)}</>
                      ) : (
                        'Sin ubicación'
                      )}
                    </span>
                  )}

                  {/* Avatar */}
                  {tutor.usuario?.foto ? (
                    <img
                      className="vt-card__avatar-img"
                      src={toAbsoluteImageUrl(tutor.usuario.foto)}
                      alt={nombre}
                      onError={e => { e.target.onerror = null; e.target.src = DEFAULT_PROFILE_AVATAR; }}
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

                    {/* Contactar: solo si no es el propio usuario */}
                    {(() => {
                      const targetUserId = tutor.userId ?? tutor.usuario?.id;
                      if (!targetUserId || targetUserId === user?.id) return null;
                      return (
                        <button
                          className="vt-btn vt-btn--primary"
                          onClick={() => {
                            const nombre = getNombre(tutor);
                            const params = new URLSearchParams({ userId: String(targetUserId), userName: nombre, autoStart: 'true' });
                            if (tutor.usuario?.foto) params.set('userPhoto', tutor.usuario.foto);
                            navigate(`/chats?${params.toString()}`);
                          }}
                        >
                          Contactar
                        </button>
                      );
                    })()}
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

      {/* ── Modal Búsqueda por Cercanía ──────────────────── */}
      {mostrarModalCercania && (
        <div className="vt-modal-overlay" onClick={() => setMostrarModalCercania(false)}>
          <div className="vt-modal" onClick={(e) => e.stopPropagation()}>
            <div className="vt-modal__header">
              <h2>Buscar por cercanía</h2>
              <button 
                className="vt-modal__close"
                onClick={() => setMostrarModalCercania(false)}
              >
                ✕
              </button>
            </div>
            <div className="vt-modal__body">
              <p className="vt-modal__descripcion">
                Encuentra profesores cerca de tu ubicación:
                <br />
                <strong>
                  {user?.ubicacion?.nombre || user?.ubicacion?.direccion || 'Tu ubicación'}
                </strong>
              </p>
              <div className="vt-modal__radio-control">
                <label>Radio de búsqueda:</label>
                <div className="vt-modal__radio-slider">
                  <input
                    type="range"
                    min="1"
                    max="50"
                    value={radioKm}
                    onChange={(e) => setRadioKm(parseInt(e.target.value))}
                  />
                  <span className="vt-modal__radio-value">{radioKm} km</span>
                </div>
              </div>
            </div>
            <div className="vt-modal__footer">
              <button 
                className="vt-btn vt-btn--ghost"
                onClick={() => setMostrarModalCercania(false)}
              >
                Cancelar
              </button>
              <button 
                className="vt-btn vt-btn--primary"
                onClick={aplicarBusquedaCercania}
              >
                Buscar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default VerifiedTeachers;

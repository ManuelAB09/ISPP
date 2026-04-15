import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { communitiesApi } from '../../api/communities.api';
import ComunidadCard from '../../components/Comunidad/ComunidadCard';
import Header from "../../components/Header/Header";
import './Home.css';

const Home = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [misComunidades, setMisComunidades] = useState([]);
  const [comunidadesCreadas, setComunidadesCreadas] = useState([]);
  const isAuthenticated = !!localStorage.getItem('accessToken');

  useEffect(() => {
    if (!isAuthenticated) {
      setLoading(false);
      return;
    }

    const fetchCommunities = async () => {
      setLoading(true);
      setError(null);

      try {
        const response = await communitiesApi.listMine({ page: 0, size: 100 });
        const comunidades = response.content || [];

        const creadas = comunidades.filter(c =>
          c.miRol === 'ADMIN' || c.miRol === 'ADMINISTRADOR' || c.creador?.id === localStorage.getItem('userId')
        );

        const miembro = comunidades.filter(c =>
          c.miRol !== 'ADMIN' && c.miRol !== 'ADMINISTRADOR' && c.creador?.id !== localStorage.getItem('userId')
        );

        setComunidadesCreadas(creadas);
        setMisComunidades(miembro);
      } catch (err) {
        console.error('Error al cargar comunidades:', err);
        if (err?.status === 401) {
          localStorage.removeItem('accessToken');
          localStorage.removeItem('userId');
        } else {
          setError('No se pudieron cargar las comunidades. Inténtalo de nuevo más tarde.');
        }
      } finally {
        setLoading(false);
      }
    };

    fetchCommunities();
  }, [isAuthenticated]);

  return (
    <>
      <Header page={'inicio'} />

      {/* ══════════════════════════════════════════════════════
          HERO PRINCIPAL
          ══════════════════════════════════════════════════════ */}
      <div className="home-hero">
        {/* Capas decorativas de fondo */}
        <div className="home-hero__orb home-hero__orb--1" aria-hidden="true"/>
        <div className="home-hero__orb home-hero__orb--2" aria-hidden="true"/>
        <div className="home-hero__orb home-hero__orb--3" aria-hidden="true"/>
        <div className="home-hero__grid"                   aria-hidden="true"/>

        {/* Contenido principal */}
        <div className="home-hero__body">
          <span className="home-hero__badge">
            {isAuthenticated ? 'Tu espacio de aprendizaje' : 'La plataforma de estudio colaborativo'}
          </span>

          <h1 className="home-hero__title">MeerKatters</h1>

          <p className="home-hero__tagline">
            Conecta con estudiantes, aprende con profesores verificados<br/>
            y construye comunidades de conocimiento.
          </p>

          {/* CTAs — no autenticado */}
          {!isAuthenticated && (
            <div className="home-hero__cta">
              <button
                className="home-hero__cta-primary"
                onClick={() => navigate('/comunidades')}
              >
                Explorar comunidades
              </button>
              <button
                className="home-hero__cta-secondary"
                onClick={() => navigate('/login')}
              >
                Iniciar sesión
              </button>
            </div>
          )}

          {/* Acciones rápidas — autenticado */}
          {isAuthenticated && (
            <div className="home-hero__quick">
              <button
                className="home-hero__quick-btn"
                onClick={() => navigate('/eventos-mapa')}
              >
                <span className="home-hero__quick-icon" aria-hidden="true">🗺️</span>
                Mapa de eventos
              </button>
              <button
                className="home-hero__quick-btn"
                onClick={() => navigate('/mis-eventos')}
              >
                <span className="home-hero__quick-icon" aria-hidden="true">📅</span>
                Mis eventos
              </button>
              <button
                className="home-hero__quick-btn"
                onClick={() => navigate('/mis-reservas')}
              >
                <span className="home-hero__quick-icon" aria-hidden="true">📋</span>
                Mis reservas
              </button>
            </div>
          )}
        </div>

        {/* Franja inferior con pilares */}
        <div className="home-hero__pillars">
          <div className="home-hero__pillar">
            <span className="home-hero__pillar-name">Comunidades</span>
            <span className="home-hero__pillar-desc">para cada área de estudio</span>
          </div>
          <div className="home-hero__pillar-sep" aria-hidden="true"/>
          <div className="home-hero__pillar">
            <span className="home-hero__pillar-name">Profesores</span>
            <span className="home-hero__pillar-desc">verificados y expertos</span>
          </div>
          <div className="home-hero__pillar-sep" aria-hidden="true"/>
          <div className="home-hero__pillar">
            <span className="home-hero__pillar-name">Aprendizaje</span>
            <span className="home-hero__pillar-desc">colaborativo en tiempo real</span>
          </div>
        </div>
      </div>

      {/* ══════════════════════════════════════════════════════
          CONTENIDO — secciones de comunidades
          ══════════════════════════════════════════════════════ */}
      <div className="home-container">
        <div className="body">
          <div className="body-content">
            {!isAuthenticated && (
              <div className="not-authenticated">
                <p>Inicia sesión para ver tus comunidades</p>
                <button onClick={() => navigate('/login')} className="login-btn">
                  Iniciar sesión
                </button>
              </div>
            )}

            {isAuthenticated && loading && (
              <div className="loading-state">
                <p>Cargando tus comunidades...</p>
              </div>
            )}

            {isAuthenticated && error && (
              <div className="error-state">
                <p className="error">{error}</p>
              </div>
            )}

            {isAuthenticated && !loading && !error && (
              <>
                <section className="communities-section">
                  <div className="section-header">
                    <h2>Mis comunidades creadas</h2>
                    <button
                      onClick={() => navigate('/crear-comunidad')}
                      className="create-community-btn"
                    >
                      Crear comunidad
                    </button>
                  </div>

                  {comunidadesCreadas.length > 0 ? (
                    <ul className="comunidades-list">
                      {comunidadesCreadas.map(comunidad => (
                        <ComunidadCard key={comunidad.id} comunidad={comunidad} />
                      ))}
                    </ul>
                  ) : (
                    <div className="empty-state">
                      <p>Aún no has creado ninguna comunidad. ¡Crea la primera!</p>
                    </div>
                  )}
                </section>

                <section className="communities-section">
                  <div className="section-header">
                    <h2>Comunidades de las que formo parte</h2>
                    <button
                      onClick={() => navigate('/comunidades')}
                      className="explore-btn"
                    >
                      Explorar más
                    </button>
                  </div>

                  {misComunidades.length > 0 ? (
                    <ul className="comunidades-list">
                      {misComunidades.map(comunidad => (
                        <ComunidadCard key={comunidad.id} comunidad={comunidad} />
                      ))}
                    </ul>
                  ) : (
                    <div className="empty-state">
                      <p>No formas parte de ninguna comunidad todavía.</p>
                      <button
                        onClick={() => navigate('/comunidades')}
                        className="explore-btn-secondary"
                      >
                        Explorar comunidades
                      </button>
                    </div>
                  )}
                </section>
              </>
            )}
          </div>
        </div>
      </div>
    </>
  );
};

export default Home;

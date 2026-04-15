import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { communitiesApi } from '../../api/communities.api';
import ComunidadCard from '../../components/Comunidad/ComunidadCard';
import RecommendationsSection from '../../components/Recomendaciones/RecommendationsSection';
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
        // Obtener todas las comunidades donde soy miembro
        const response = await communitiesApi.listMine({ page: 0, size: 100 });
        const comunidades = response.content || [];
        
        // Filtrar comunidades donde soy admin/creador
        const creadas = comunidades.filter(c => 
          c.miRol === 'ADMIN' || c.miRol === 'ADMINISTRADOR' || c.creador?.id === localStorage.getItem('userId')
        );
        
        // Filtrar comunidades donde solo soy miembro
        const miembro = comunidades.filter(c => 
          c.miRol !== 'ADMIN' && c.miRol !== 'ADMINISTRADOR' && c.creador?.id !== localStorage.getItem('userId')
        );
        
        setComunidadesCreadas(creadas);
        setMisComunidades(miembro);
      } catch (err) {
        console.error('Error al cargar comunidades:', err);
        if (err?.status === 401) {
          // Token inválido, redirigir al login
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
      <div className="home-container">
        <div className="header">
          <div className="headerTitle home">
            <h1 className="title">MeerKatters</h1>
            <h2>Bienvenido a la mayor comunidad de estudio donde estudiantes y universitarios comparten sus estudios</h2>
          </div>
          {isAuthenticated && (
            <div className="home-quick-actions">
              <button onClick={() => navigate('/eventos-mapa')} className="home-quick-btn">
                🗺️ Mapa de eventos
              </button>
              <button onClick={() => navigate('/mis-eventos')} className="home-quick-btn">
                📅 Mis eventos
              </button>
              <button onClick={() => navigate('/mis-reservas')} className="home-quick-btn">
                📋 Mis reservas
              </button>
            </div>
          )}
        </div>
        
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
                {/* Sección de recomendaciones */}
                <RecommendationsSection />

                {/* Comunidades creadas por mí */}
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

                {/* Comunidades a las que pertenezco */}
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
  )
}

export default Home

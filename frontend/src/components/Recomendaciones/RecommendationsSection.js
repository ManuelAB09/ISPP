import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import * as recommendationsApi from '../../api/recommendations.api';
import './RecommendationsSection.css';

const RecommendationsSection = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [recommendations, setRecommendations] = useState({
    paraTi: [],
    profesores: [],
    contenidos: [],
    cuestionarios: [],
    comunidades: []
  });

  useEffect(() => {
    const fetchRecommendations = async () => {
      try {
        setLoading(true);
        const response = await recommendationsApi.getRecommendationsPage();
        setRecommendations(response);
        setError(null);
      } catch (err) {
        console.error('Error al cargar recomendaciones:', err);
        setError('No se pudieron cargar las recomendaciones');
      } finally {
        setLoading(false);
      }
    };

    fetchRecommendations();
  }, []);

  const handleRecommendationSeen = async (id) => {
    try {
      await recommendationsApi.markRecommendationAsSeen(id);
      // Refresh recommendations después de marcar como vista
    } catch (err) {
      console.error('Error al marcar recomendación como vista:', err);
    }
  };

  const handleDeleteRecommendation = async (id) => {
    try {
      await recommendationsApi.deleteRecommendation(id);
      // Refresh recommendations después de eliminar
    } catch (err) {
      console.error('Error al eliminar recomendación:', err);
    }
  };

  if (loading) {
    return (
      <div className="recommendations-section">
        <div className="section-title">
          <h2>Recomendaciones para ti</h2>
        </div>
        <div className="loading-state">
          <p>Cargando recomendaciones...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="recommendations-section">
        <div className="section-title">
          <h2>Recomendaciones para ti</h2>
        </div>
        <div className="error-state">
          <p>{error}</p>
        </div>
      </div>
    );
  }

  const hasAnyRecommendations =
    recommendations.paraTi?.length > 0 ||
    recommendations.profesores?.length > 0 ||
    recommendations.contenidos?.length > 0 ||
    recommendations.cuestionarios?.length > 0 ||
    recommendations.comunidades?.length > 0;

  if (!hasAnyRecommendations) {
    return null;
  }

  return (
    <div className="recommendations-section">
      <div className="section-title">
        <h2>🎯 Recomendaciones para ti</h2>
        <p>Descubre contenido personalizado según tus intereses</p>
      </div>

      {/* Para Ti */}
      {recommendations.paraTi?.length > 0 && (
        <div className="recommendations-subsection">
          <h3>Lo mejor para ti</h3>
          <div className="recommendations-grid">
            {recommendations.paraTi.slice(0, 4).map((rec) => (
              <div key={rec.id} className="recommendation-card">
                {rec.imagenUrl && (
                  <img src={rec.imagenUrl} alt={rec.titulo} className="rec-image" />
                )}
                <div className="rec-content">
                  <h4>{rec.titulo}</h4>
                  <p className="rec-description">{rec.descripcion}</p>
                  <div className="rec-footer">
                    <span className="rec-reason">{rec.razon}</span>
                    {rec.puntuacion && (
                      <span className="rec-score">⭐ {rec.puntuacion.toFixed(1)}</span>
                    )}
                  </div>
                </div>
                <div className="rec-actions">
                  <button
                    onClick={() => handleRecommendationSeen(rec.id)}
                    className="rec-action-btn"
                    title="Marcar como visto"
                  >
                    ✓
                  </button>
                  <button
                    onClick={() => handleDeleteRecommendation(rec.id)}
                    className="rec-action-btn delete"
                    title="Descartar"
                  >
                    ✕
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Profesores */}
      {recommendations.profesores?.length > 0 && (
        <div className="recommendations-subsection">
          <h3>👨‍🏫 Profesores recomendados</h3>
          <div className="recommendations-carousel">
            {recommendations.profesores.slice(0, 6).map((rec) => (
              <div key={rec.id} className="recommendation-card compact">
                {rec.imagenUrl && (
                  <img src={rec.imagenUrl} alt={rec.titulo} className="rec-image" />
                )}
                <h4>{rec.titulo}</h4>
                <p className="rec-reason">{rec.razon}</p>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Contenidos */}
      {recommendations.contenidos?.length > 0 && (
        <div className="recommendations-subsection">
          <h3>📚 Contenidos sugeridos</h3>
          <div className="recommendations-carousel">
            {recommendations.contenidos.slice(0, 6).map((rec) => (
              <div key={rec.id} className="recommendation-card compact">
                {rec.imagenUrl && (
                  <img src={rec.imagenUrl} alt={rec.titulo} className="rec-image" />
                )}
                <h4>{rec.titulo}</h4>
                <p className="rec-reason">{rec.razon}</p>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Cuestionarios */}
      {recommendations.cuestionarios?.length > 0 && (
        <div className="recommendations-subsection">
          <h3>❓ Cuestionarios recomendados</h3>
          <div className="recommendations-carousel">
            {recommendations.cuestionarios.slice(0, 6).map((rec) => (
              <div key={rec.id} className="recommendation-card compact">
                {rec.imagenUrl && (
                  <img src={rec.imagenUrl} alt={rec.titulo} className="rec-image" />
                )}
                <h4>{rec.titulo}</h4>
                <p className="rec-reason">{rec.razon}</p>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Comunidades */}
      {recommendations.comunidades?.length > 0 && (
        <div className="recommendations-subsection">
          <h3>👥 Comunidades que te pueden interesar</h3>
          <div className="recommendations-carousel">
            {recommendations.comunidades.slice(0, 4).map((rec) => (
              <div key={rec.id} className="recommendation-card compact">
                {rec.imagenUrl && (
                  <img src={rec.imagenUrl} alt={rec.titulo} className="rec-image" />
                )}
                <h4>{rec.titulo}</h4>
                <p className="rec-reason">{rec.razon}</p>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default RecommendationsSection;

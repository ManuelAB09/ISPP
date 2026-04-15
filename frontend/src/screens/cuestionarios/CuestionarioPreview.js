import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Header from '../../components/Header/Header';
import { cuestionariosApi } from '../../api/cuestionarios.api';
import './CuestionarioPreview.css';

const formatAttemptDate = (dateValue) => {
  if (!dateValue) {
    return 'Fecha desconocida';
  }

  const parsed = new Date(dateValue);
  if (Number.isNaN(parsed.getTime())) {
    return 'Fecha desconocida';
  }

  return parsed.toLocaleString('es-ES', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
};

const CuestionarioPreview = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [preview, setPreview] = useState(null);

  useEffect(() => {
    if (!id) {
      setError('No se ha indicado el cuestionario.');
      setLoading(false);
      return;
    }

    const loadPreview = async () => {
      setLoading(true);
      setError('');
      try {
        const data = await cuestionariosApi.getPreview(id);
        setPreview(data);
      } catch (err) {
        setPreview(null);
        setError(err?.response?.data?.message || err?.message || 'No se pudo cargar el cuestionario.');
      } finally {
        setLoading(false);
      }
    };

    loadPreview();
  }, [id]);

  const attempts = useMemo(() => (Array.isArray(preview?.intentosPrevios) ? preview.intentosPrevios : []), [preview]);

  const handleBackClick = () => {
    if (preview?.comunidadesIds && preview.comunidadesIds.length > 0) {
      navigate(`/comunidades/${preview.comunidadesIds[0]}`);
    } else {
      navigate('/cuestionarios');
    }
  };

  return (
    <>
      <Header page={'cuestionarios'} />
      <main className="quiz-preview-page">
        <button
          className="quiz-preview-back"
          type="button"
          onClick={handleBackClick}
        >
          Volver
        </button>

        {loading ? (
          <div className="quiz-preview-card">Cargando cuestionario...</div>
        ) : error ? (
          <div className="quiz-preview-error">{error}</div>
        ) : preview ? (
          <section className="quiz-preview-card">
            <div className="quiz-preview-headline">
              <h1>{preview.titulo || 'Cuestionario'}</h1>
              <span className="quiz-preview-chip">{preview.materia || 'Sin materia'}</span>
            </div>

            {preview.descripcion ? (
              <p className="quiz-preview-description">{preview.descripcion}</p>
            ) : null}

            <div className="quiz-preview-meta">
              <span>Preguntas: {preview.numPreguntas || 0}</span>
              <span>Dificultad: {preview.dificultad || 'No definida'}</span>
              <span>Tiempo estimado: {preview.tiempoEstimadoMinutos || '-'} min</span>
            </div>

            <div className="quiz-preview-attempts">
              <h2>Intentos anteriores</h2>
              {attempts.length > 0 ? (
                <div className="quiz-preview-attempts-list">
                  {attempts.map((attempt, index) => (
                    <article key={attempt.id || index} className="quiz-preview-attempt-item">
                      <p>
                        Intento #{attempts.length - index}
                      </p>
                      <p>Puntuación: {Number(attempt.puntuacion || 0).toFixed(1)}%</p>
                      <p>{formatAttemptDate(attempt.createdAt)}</p>
                    </article>
                  ))}
                </div>
              ) : (
                <p className="quiz-preview-empty">Todavía no has realizado este cuestionario.</p>
              )}
            </div>

            <div className="quiz-preview-actions">
              <button
                className="quiz-preview-primary"
                type="button"
                disabled={!preview.puedeResolver}
                onClick={() => navigate(`/cuestionarios/${id}/resolver`)}
              >
                {attempts.length > 0 ? 'Volver a intentarlo' : 'Comenzar'}
              </button>
            </div>
          </section>
        ) : null}
      </main>
    </>
  );
};

export default CuestionarioPreview;

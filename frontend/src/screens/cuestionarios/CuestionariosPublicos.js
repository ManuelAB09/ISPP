import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../../components/Header/Header';
import { cuestionariosApi } from '../../api/cuestionarios.api';
import './CuestionariosPublicos.css';

const DIFFICULTY_LABELS = {
  BASICO: 'Básico',
  INTERMEDIO: 'Intermedio',
  AVANZADO: 'Avanzado',
};

const formatDate = (dateValue) => {
  if (!dateValue) return '';
  const parsed = new Date(dateValue);
  if (Number.isNaN(parsed.getTime())) return '';
  return parsed.toLocaleDateString('es-ES', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
};

export default function CuestionariosPublicos() {
  const navigate = useNavigate();
  const isAuthenticated = Boolean(localStorage.getItem('accessToken'));

  const [quizzes, setQuizzes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    setLoading(true);
    setError('');

    const fetchQuizzes = async () => {
      try {
        const publicData = await cuestionariosApi.listPublic();
        const publicList = Array.isArray(publicData) ? publicData : [];

        let assignedList = [];
        if (isAuthenticated) {
          try {
            const assignedData = await cuestionariosApi.listAssigned();
            assignedList = Array.isArray(assignedData) ? assignedData : [];
          } catch {
            // ignore - user may not have assigned quizzes
          }
        }

        // Merge and deduplicate by id
        const seen = new Set();
        const merged = [];
        for (const q of [...assignedList, ...publicList]) {
          if (!seen.has(q.id)) {
            seen.add(q.id);
            merged.push(q);
          }
        }
        setQuizzes(merged);
      } catch {
        setError('No se pudieron cargar los cuestionarios públicos.');
      } finally {
        setLoading(false);
      }
    };

    fetchQuizzes();
  }, [isAuthenticated]);

  return (
    <>
      <Header page="cuestionarios" />
      <div className="quizzes-public-page">
        <div className="quizzes-public-header">
          <h1>Cuestionarios públicos</h1>
          {isAuthenticated && (
            <button
              className="quizzes-public-btn-create"
              onClick={() => navigate('/cuestionarios/crear?publicOnly=1')}
            >
              ✏️ Crear cuestionario
            </button>
          )}
        </div>

        {loading ? (
          <div className="quizzes-public-loading">Cargando cuestionarios...</div>
        ) : error ? (
          <div className="quizzes-public-error">{error}</div>
        ) : quizzes.length === 0 ? (
          <div className="quizzes-public-empty">No hay cuestionarios públicos disponibles.</div>
        ) : (
          <div className="quizzes-public-grid">
            {quizzes.map((q) => (
              <div
                key={q.id}
                className="quizzes-public-card"
                onClick={() => navigate(`/cuestionarios/${q.id}`)}
              >
                <div className="quizzes-public-card-title">{q.titulo}</div>
                <div className="quizzes-public-card-meta">
                  {q.materia && <span className="quizzes-public-chip">{q.materia}</span>}
                  {q.dificultad && (
                    <span className="quizzes-public-chip quizzes-public-chip--difficulty">
                      {DIFFICULTY_LABELS[q.dificultad] || q.dificultad}
                    </span>
                  )}
                  {q.numPreguntas > 0 && (
                    <span className="quizzes-public-chip">
                      {q.numPreguntas} pregunta{q.numPreguntas !== 1 ? 's' : ''}
                    </span>
                  )}
                </div>
                {q.descripcion && (
                  <div className="quizzes-public-card-desc">{q.descripcion}</div>
                )}
                <div className="quizzes-public-card-footer">
                  <span>{q.createdAt ? formatDate(q.createdAt) : ''}</span>
                  {q.tiempoEstimadoMinutos > 0 && (
                    <span>⏱ {q.tiempoEstimadoMinutos} min</span>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </>
  );
}

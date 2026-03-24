import { useMemo } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import Header from '../../components/Header/Header';
import './CuestionarioResultado.css';

const renderAnswers = (items, fallbackText) => {
  if (!Array.isArray(items) || items.length === 0) {
    return fallbackText;
  }
  return items.join(', ');
};

const CuestionarioResultado = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();

  const resultado = location.state?.resultado || null;
  const cuestionarioTitulo = location.state?.cuestionarioTitulo || 'Cuestionario';

  const preguntas = useMemo(() => (Array.isArray(resultado?.preguntas) ? resultado.preguntas : []), [resultado]);

  return (
    <>
      <Header page={'cuestionarios'} />
      <main className="quiz-result-page">
        {!resultado ? (
          <section className="quiz-result-card">
            <h1>Resultado no disponible</h1>
            <p>Para ver el detalle debes entregar el cuestionario desde la pantalla de resolución.</p>
            <button type="button" className="quiz-result-primary" onClick={() => navigate(`/cuestionarios/${id}`)}>
              Volver al cuestionario
            </button>
          </section>
        ) : (
          <section className="quiz-result-card">
            <header className="quiz-result-header">
              <h1>{cuestionarioTitulo}</h1>
              <p>Puntuación final: <strong>{Number(resultado.puntuacion || 0).toFixed(1)}%</strong></p>
            </header>

            <div className="quiz-result-summary">
              <span>Total: {resultado.totalPreguntas || 0}</span>
              <span>Aciertos: {resultado.preguntasCorrectas || 0}</span>
              <span>Fallos: {resultado.preguntasIncorrectas || 0}</span>
            </div>

            <div className="quiz-result-list">
              {preguntas.map((pregunta, index) => (
                <article
                  key={pregunta.preguntaId || index}
                  className={`quiz-result-question ${pregunta.acertada ? 'is-correct' : 'is-wrong'}`}
                >
                  <h2>{index + 1}. {pregunta.enunciado}</h2>
                  <p>
                    <strong>Estado:</strong> {pregunta.acertada ? 'Correcta' : 'Incorrecta'}
                  </p>
                  <p>
                    <strong>Tu respuesta:</strong> {renderAnswers(pregunta.respuestaUsuario, 'Sin responder')}
                  </p>
                  <p>
                    <strong>Respuesta correcta:</strong> {renderAnswers(pregunta.respuestaCorrecta, 'No disponible')}
                  </p>
                </article>
              ))}
            </div>

            <div className="quiz-result-actions">
              <button type="button" className="quiz-result-secondary" onClick={() => navigate(`/cuestionarios/${id}`)}>
                Volver al preview
              </button>
              <button type="button" className="quiz-result-primary" onClick={() => navigate(`/cuestionarios/${id}/resolver`)}>
                Reintentar
              </button>
            </div>
          </section>
        )}
      </main>
    </>
  );
};

export default CuestionarioResultado;

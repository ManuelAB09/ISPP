import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Header from '../../components/Header/Header';
import { cuestionariosApi } from '../../api/cuestionarios.api';
import './CuestionarioResolver.css';

const isChoiceQuestion = (tipo) => tipo === 'TEST' || tipo === 'VERDADERO_FALSO';

const CuestionarioResolver = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [resolverData, setResolverData] = useState(null);
  const [answers, setAnswers] = useState({});

  useEffect(() => {
    if (!id) {
      setError('No se ha indicado el cuestionario.');
      setLoading(false);
      return;
    }

    const loadResolver = async () => {
      setLoading(true);
      setError('');
      try {
        const data = await cuestionariosApi.getResolver(id);
        setResolverData(data);
      } catch (err) {
        setResolverData(null);
        setError(err?.response?.data?.message || err?.message || 'No se pudo cargar el cuestionario.');
      } finally {
        setLoading(false);
      }
    };

    loadResolver();
  }, [id]);

  const preguntas = useMemo(() => (Array.isArray(resolverData?.preguntas) ? resolverData.preguntas : []), [resolverData]);

  const toggleOption = (preguntaId, opcionId) => {
    setAnswers((prev) => {
      const current = prev[preguntaId] || { opcionIds: [], respuestaTexto: '' };
      const exists = current.opcionIds.includes(opcionId);
      return {
        ...prev,
        [preguntaId]: {
          ...current,
          opcionIds: exists
            ? current.opcionIds.filter((idValue) => idValue !== opcionId)
            : [...current.opcionIds, opcionId]
        }
      };
    });
  };

  const setSingleOption = (preguntaId, opcionId) => {
    setAnswers((prev) => ({
      ...prev,
      [preguntaId]: {
        opcionIds: [opcionId],
        respuestaTexto: ''
      }
    }));
  };

  const onTextAnswerChange = (preguntaId, value) => {
    setAnswers((prev) => ({
      ...prev,
      [preguntaId]: {
        opcionIds: [],
        respuestaTexto: value
      }
    }));
  };

  const submitQuiz = async () => {
    setSubmitting(true);
    setError('');

    const payload = {
      answers: preguntas.map((pregunta) => {
        const current = answers[pregunta.id] || { opcionIds: [], respuestaTexto: '' };
        return {
          preguntaId: pregunta.id,
          opcionIds: current.opcionIds,
          respuestaTexto: current.respuestaTexto
        };
      })
    };

    try {
      const result = await cuestionariosApi.submitAttempt(id, payload);
      navigate(`/cuestionarios/${id}/resultado`, {
        state: {
          resultado: result,
          cuestionarioTitulo: resolverData?.titulo || 'Cuestionario'
        }
      });
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || 'No se pudo enviar el cuestionario.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <Header page={'cuestionarios'} />
      <main className="quiz-resolver-page">
        <button
          className="quiz-resolver-back"
          type="button"
          onClick={() => navigate(`/cuestionarios/${id}`)}
        >
          Volver al resumen
        </button>

        {loading ? (
          <div className="quiz-resolver-card">Cargando cuestionario...</div>
        ) : error ? (
          <div className="quiz-resolver-error">{error}</div>
        ) : resolverData ? (
          <section className="quiz-resolver-card">
            <header className="quiz-resolver-header">
              <h1>{resolverData.titulo || 'Cuestionario'}</h1>
              <p>{preguntas.length} preguntas</p>
            </header>

            <div className="quiz-resolver-questions">
              {preguntas.map((pregunta, index) => {
                const current = answers[pregunta.id] || { opcionIds: [], respuestaTexto: '' };
                const opciones = Array.isArray(pregunta.opciones) ? pregunta.opciones : [];

                return (
                  <article key={pregunta.id} className="quiz-resolver-question-item">
                    <h2>{index + 1}. {pregunta.enunciado}</h2>

                    {isChoiceQuestion(pregunta.tipo) ? (
                      <div className="quiz-resolver-options">
                        {opciones.map((opcion) => {
                          const isSelected = current.opcionIds.includes(opcion.id);
                          const isSingleChoice = pregunta.tipo === 'VERDADERO_FALSO';

                          return (
                            <label key={opcion.id} className="quiz-resolver-option">
                              <input
                                type={isSingleChoice ? 'radio' : 'checkbox'}
                                name={`pregunta-${pregunta.id}`}
                                checked={isSelected}
                                onChange={() => {
                                  if (isSingleChoice) {
                                    setSingleOption(pregunta.id, opcion.id);
                                    return;
                                  }
                                  toggleOption(pregunta.id, opcion.id);
                                }}
                              />
                              <span>{opcion.texto}</span>
                            </label>
                          );
                        })}
                      </div>
                    ) : (
                      <textarea
                        className="quiz-resolver-textarea"
                        placeholder="Escribe tu respuesta"
                        value={current.respuestaTexto}
                        onChange={(event) => onTextAnswerChange(pregunta.id, event.target.value)}
                      />
                    )}
                  </article>
                );
              })}
            </div>

            <div className="quiz-resolver-actions">
              <button
                type="button"
                className="quiz-resolver-submit"
                onClick={submitQuiz}
                disabled={submitting || preguntas.length === 0}
              >
                {submitting ? 'Enviando...' : 'Entregar cuestionario'}
              </button>
            </div>
          </section>
        ) : null}
      </main>
    </>
  );
};

export default CuestionarioResolver;

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Header from '../../components/Header/Header';
import { cuestionariosApi } from '../../api/cuestionarios.api';
import './CuestionarioResolver.css';

const isChoiceQuestion = (tipo) => tipo === 'TEST' || tipo === 'VERDADERO_FALSO';

// Margen (en segundos) que toleramos en el cliente antes de bloquear el envío,
// para absorber el retardo del temporizador y de la petición. El backend aplica
// su propio margen, más amplio, como red de seguridad.
const TIME_LIMIT_GRACE_SECONDS = 5;

const formatRemaining = (totalSeconds) => {
  const safe = Math.max(0, Math.floor(totalSeconds));
  const minutes = Math.floor(safe / 60);
  const seconds = safe % 60;
  return `${minutes}:${String(seconds).padStart(2, '0')}`;
};

const CuestionarioResolver = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [resolverData, setResolverData] = useState(null);
  const [answers, setAnswers] = useState({});
  const [secondsLeft, setSecondsLeft] = useState(null);
  const [timeExpired, setTimeExpired] = useState(false);

  const startedAtRef = useRef(null);
  const autoSubmittedRef = useRef(false);

  const preguntas = useMemo(() => (Array.isArray(resolverData?.preguntas) ? resolverData.preguntas : []), [resolverData]);

  const timeLimitSeconds = useMemo(() => {
    const minutos = Number(resolverData?.tiempoEstimadoMinutos);
    return Number.isFinite(minutos) && minutos > 0 ? Math.round(minutos * 60) : 0;
  }, [resolverData]);

  const elapsedSeconds = useCallback(() => {
    if (!startedAtRef.current) return 0;
    return Math.max(0, Math.round((Date.now() - startedAtRef.current) / 1000));
  }, []);

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
        startedAtRef.current = Date.now();
        autoSubmittedRef.current = false;
        setTimeExpired(false);
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

  const submitQuiz = useCallback(async ({ auto = false } = {}) => {
    if (submitting) return;
    if (preguntas.length === 0) return;

    const empleado = elapsedSeconds();

    // Bloquear el envío si se ha superado el tiempo límite (UC-65).
    if (timeLimitSeconds > 0 && empleado > timeLimitSeconds + TIME_LIMIT_GRACE_SECONDS) {
      setTimeExpired(true);
      setSecondsLeft(0);
      setError('Se ha superado el tiempo límite del cuestionario; ya no se puede enviar.');
      return;
    }

    setSubmitting(true);
    setError('');

    const payload = {
      tiempoEmpleadoSegundos: empleado,
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
          cuestionarioTitulo: resolverData?.titulo || 'Cuestionario',
          enviadoAutomaticamente: auto
        }
      });
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || 'No se pudo enviar el cuestionario.');
    } finally {
      setSubmitting(false);
    }
  }, [submitting, preguntas, elapsedSeconds, timeLimitSeconds, answers, id, navigate, resolverData]);

  // Mantener una referencia estable a la última versión de submitQuiz para usarla
  // desde el temporizador sin reiniciar el intervalo en cada cambio de respuesta.
  const submitQuizRef = useRef(submitQuiz);
  useEffect(() => {
    submitQuizRef.current = submitQuiz;
  }, [submitQuiz]);

  // Cuenta atrás del tiempo límite. Al llegar a 0 se entrega automáticamente lo respondido.
  useEffect(() => {
    if (loading || !resolverData || timeLimitSeconds <= 0 || timeExpired) {
      if (timeLimitSeconds <= 0) setSecondsLeft(null);
      return undefined;
    }

    const tick = () => {
      const remaining = timeLimitSeconds - elapsedSeconds();
      setSecondsLeft(Math.max(0, remaining));
      if (remaining <= 0) {
        setTimeExpired(true);
        if (!autoSubmittedRef.current) {
          autoSubmittedRef.current = true;
          submitQuizRef.current({ auto: true });
        }
      }
    };

    tick();
    const intervalId = setInterval(tick, 1000);
    return () => clearInterval(intervalId);
  }, [loading, resolverData, timeLimitSeconds, timeExpired, elapsedSeconds]);

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

  const timerIsLow = secondsLeft != null && secondsLeft <= 30;
  const answersDisabled = timeExpired || submitting;

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
        ) : error && !resolverData ? (
          <div className="quiz-resolver-error">{error}</div>
        ) : resolverData ? (
          <section className="quiz-resolver-card">
            <header className="quiz-resolver-header">
              <h1>{resolverData.titulo || 'Cuestionario'}</h1>
              <p>{preguntas.length} preguntas</p>
              {timeLimitSeconds > 0 && (
                <p
                  className={`quiz-resolver-timer${timerIsLow ? ' quiz-resolver-timer--low' : ''}${timeExpired ? ' quiz-resolver-timer--expired' : ''}`}
                  role="timer"
                >
                  {timeExpired
                    ? '⏱ Tiempo agotado'
                    : `⏱ Tiempo restante: ${formatRemaining(secondsLeft ?? timeLimitSeconds)}`}
                </p>
              )}
            </header>

            {error && resolverData && <div className="quiz-resolver-error">{error}</div>}

            {timeExpired && (
              <div className="quiz-resolver-error">
                El tiempo para resolver este cuestionario ha terminado.
              </div>
            )}

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
                                disabled={answersDisabled}
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
                        disabled={answersDisabled}
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
                onClick={() => submitQuiz()}
                disabled={submitting || preguntas.length === 0 || timeExpired}
              >
                {submitting ? 'Enviando...' : timeExpired ? 'Tiempo agotado' : 'Entregar cuestionario'}
              </button>
            </div>
          </section>
        ) : null}
      </main>
    </>
  );
};

export default CuestionarioResolver;

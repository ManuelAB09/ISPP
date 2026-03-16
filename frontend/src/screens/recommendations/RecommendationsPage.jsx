import { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../../components/Header/Header';
import { useRecommendations } from '../../hooks/useRecommendations';
import './RecommendationsPage.css';

// ─── Helpers ──────────────────────────────────────────────────────────────────

const NAV_MAP = {
  PROFESOR:     (id) => `/profesores/${id}`,
  CONTENIDO:    (id) => `/contenidos/${id}`,
  CUESTIONARIO: (id) => `/cuestionarios/${id}`,
  COMUNIDAD:    (id) => `/comunidades/${id}`,
  EVENTO:       (id) => `/eventos/${id}`,
};

const getNavUrl = (tipo, communityId) =>
  communityId && NAV_MAP[tipo] ? NAV_MAP[tipo](communityId) : null;

const TIPO_ICON = {
  PROFESOR: '👨‍🏫',
  CONTENIDO: '📄',
  CUESTIONARIO: '📝',
  COMUNIDAD: '🏘️',
  EVENTO: '📅',
};

// ─── RecommendationCard ───────────────────────────────────────────────────────

function RecommendationCard({ rec, onFeedback, onDiscard, onSeen, onNavigate }) {
  const cardRef = useRef(null);
  const seenRef = useRef(false);

  // Intersection Observer para marcar como vista al aparecer en pantalla
  useEffect(() => {
    if (!cardRef.current || seenRef.current || rec.vista) return;
    const obs = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting && !seenRef.current) {
          seenRef.current = true;
          onSeen(rec.id);
          obs.disconnect();
        }
      },
      { threshold: 0.4 }
    );
    obs.observe(cardRef.current);
    return () => obs.disconnect();
  }, [rec.id, rec.vista, onSeen]);

  const navUrl = getNavUrl(rec.tipo, rec.communityId);
  const motivoParts = rec.motivo ? rec.motivo.split(' · ') : [];

  return (
    <div ref={cardRef} className={`rec-card ${!rec.vista ? 'rec-card--new' : ''}`}>
      {!rec.vista && <span className="rec-card__new-dot" title="Nueva" />}

      <button
        className="rec-card__discard"
        title="No me interesa"
        onClick={() => onDiscard(rec.id)}
      >
        ✕
      </button>

      {rec.imagenUrl ? (
        <img className="rec-card__img" src={rec.imagenUrl} alt={rec.nombre} />
      ) : (
        <div className="rec-card__img rec-card__img--placeholder">
          <span>{TIPO_ICON[rec.tipo] ?? '🔖'}</span>
        </div>
      )}

      <div className="rec-card__body">
        <div className="rec-card__tipo">{TIPO_ICON[rec.tipo]} {rec.tipo}</div>
        <h3 className="rec-card__nombre">{rec.nombre}</h3>

        {rec.descripcion && (
          <p className="rec-card__desc">{rec.descripcion}</p>
        )}

        {motivoParts.length > 0 && (
          <div className="rec-card__motivos">
            {motivoParts.map((m, i) => (
              <span key={i} className="rec-card__motivo">{m}</span>
            ))}
          </div>
        )}

        {rec.relevancia != null && (
          <div className="rec-card__relevancia">
            <div
              className="rec-card__relevancia-bar"
              style={{ width: `${Math.min(rec.relevancia, 100)}%` }}
            />
            <span className="rec-card__relevancia-num">{Math.round(rec.relevancia)}%</span>
          </div>
        )}
      </div>

      <div className="rec-card__footer">
        <div className="rec-card__feedback">
          <button
            className={`rec-card__fb-btn ${rec.esFavorable === true ? 'rec-card__fb-btn--active-up' : ''}`}
            title="Útil"
            onClick={() => onFeedback(rec.id, true)}
          >
            👍
          </button>
          <button
            className={`rec-card__fb-btn ${rec.esFavorable === false ? 'rec-card__fb-btn--active-down' : ''}`}
            title="No útil"
            onClick={() => onFeedback(rec.id, false)}
          >
            👎
          </button>
        </div>

        {navUrl && (
          <button
            className="rec-card__ver-btn"
            onClick={() => onNavigate(navUrl, rec)}
          >
            Ver →
          </button>
        )}
      </div>
    </div>
  );
}

// ─── Section ─────────────────────────────────────────────────────────────────

function Section({ title, items, layout = 'scroll', onFeedback, onDiscard, onSeen, onNavigate }) {
  if (items.length === 0) return null;

  return (
    <section className="rec-section">
      <h2 className="rec-section__title">{title}</h2>
      <div className={layout === 'grid' ? 'rec-grid' : 'rec-scroll'}>
        {items.map((rec) => (
          <RecommendationCard
            key={rec.id}
            rec={rec}
            onFeedback={onFeedback}
            onDiscard={onDiscard}
            onSeen={onSeen}
            onNavigate={onNavigate}
          />
        ))}
      </div>
    </section>
  );
}

// ─── Empty state ──────────────────────────────────────────────────────────────

function EmptyState({ onRefresh, refreshing }) {
  return (
    <div className="rec-empty">
      <p className="rec-empty__icon">🔍</p>
      <p className="rec-empty__title">Todavía no tenemos recomendaciones para ti</p>
      <p className="rec-empty__text">
        Explora la plataforma, únete a comunidades y realiza cuestionarios para que
        el sistema aprenda tus intereses.
      </p>
      <button
        className="rec-refresh-btn"
        onClick={onRefresh}
        disabled={refreshing}
      >
        {refreshing ? 'Generando…' : '✨ Generar recomendaciones'}
      </button>
    </div>
  );
}

// ─── RecommendationsPage ──────────────────────────────────────────────────────

export default function RecommendationsPage() {
  const navigate = useNavigate();
  const {
    data,
    loading,
    error,
    refresh,
    submitFeedback,
    discard,
    trackActivity,
    markSeen,
  } = useRecommendations();

  const totalItems =
    data.paraTi.length +
    data.profesores.length +
    data.contenidos.length +
    data.cuestionarios.length +
    data.comunidades.length;

  const handleNavigate = (url, rec) => {
    trackActivity({
      tipoActividad: 'CLIC',
      categoriaObjeto: rec.tipo === 'PROFESOR' ? 'Tutor'
        : rec.tipo === 'COMUNIDAD' ? 'Comunidad'
        : rec.tipo === 'CUESTIONARIO' ? 'Cuestionario'
        : 'Contenido',
      idObjeto: rec.communityId,
    });
    navigate(url);
  };

  return (
    <>
      <Header page="recomendaciones" />
      <div className="rec-page">
        <div className="rec-container">

          {/* Cabecera */}
          <div className="rec-top">
            <div>
              <h1 className="rec-title">✨ Recomendaciones</h1>
              <p className="rec-subtitle">
                Personalizadas para ti en base a tu actividad, nivel y preferencias.
              </p>
            </div>
            <button
              className="rec-refresh-btn"
              onClick={refresh}
              disabled={loading}
            >
              {loading ? 'Actualizando…' : '↻ Actualizar'}
            </button>
          </div>

          {/* Error */}
          {error && !loading && (
            <div className="rec-error">
              ⚠️ {error}
              <button className="rec-refresh-btn" onClick={refresh}>Reintentar</button>
            </div>
          )}

          {/* Loading skeleton */}
          {loading && (
            <div className="rec-skeleton-wrap">
              {[...Array(3)].map((_, i) => (
                <div key={i} className="rec-skeleton-section">
                  <div className="rec-skeleton-title" />
                  <div className="rec-scroll">
                    {[...Array(4)].map((_, j) => (
                      <div key={j} className="rec-skeleton-card" />
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Contenido */}
          {!loading && !error && totalItems === 0 && (
            <EmptyState onRefresh={refresh} refreshing={loading} />
          )}

          {!loading && totalItems > 0 && (
            <>
              <Section
                title="⭐ Recomendado para ti"
                items={data.paraTi}
                layout="grid"
                onFeedback={submitFeedback}
                onDiscard={discard}
                onSeen={markSeen}
                onNavigate={handleNavigate}
              />
              <Section
                title="👨‍🏫 Profesores que te pueden interesar"
                items={data.profesores}
                layout="scroll"
                onFeedback={submitFeedback}
                onDiscard={discard}
                onSeen={markSeen}
                onNavigate={handleNavigate}
              />
              <Section
                title="📝 Cuestionarios sugeridos"
                items={data.cuestionarios}
                layout="scroll"
                onFeedback={submitFeedback}
                onDiscard={discard}
                onSeen={markSeen}
                onNavigate={handleNavigate}
              />
              <Section
                title="📚 Contenidos recomendados"
                items={data.contenidos}
                layout="scroll"
                onFeedback={submitFeedback}
                onDiscard={discard}
                onSeen={markSeen}
                onNavigate={handleNavigate}
              />
              <Section
                title="🏘️ Comunidades sugeridas"
                items={data.comunidades}
                layout="scroll"
                onFeedback={submitFeedback}
                onDiscard={discard}
                onSeen={markSeen}
                onNavigate={handleNavigate}
              />
            </>
          )}
        </div>
      </div>
    </>
  );
}

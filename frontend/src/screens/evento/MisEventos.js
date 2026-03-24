import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../../components/Header/Header';
import { useAuth } from '../../contexts/AuthContext';
import {
  getMisEventos,
  getMisEventosHistorial,
} from '../../utils/myEventsUtils';
import './MisEventos.css';

const TIPO_ICONO = {
  REUNION: '🤝',
  EXAMEN: '📝',
  CUESTIONARIO: '✅',
  TUTORIA: '👨‍🏫',
  CLASE: '📚',
  OTRO: '📌',
};

const TIPOS_FILTRO = ['Todos', 'REUNION', 'EXAMEN', 'CUESTIONARIO', 'TUTORIA', 'CLASE', 'OTRO'];

const TIPO_LABEL = {
  REUNION: 'Reunión',
  EXAMEN: 'Examen',
  CUESTIONARIO: 'Cuestionario',
  TUTORIA: 'Tutoría',
  CLASE: 'Clase',
  OTRO: 'Otro',
};

const formatFecha = (fechaStr) => {
  if (!fechaStr) return '';
  const d = new Date(fechaStr);
  return d.toLocaleDateString('es-ES', { day: 'numeric', month: 'short', year: 'numeric' });
};

const formatHora = (fechaStr) => {
  if (!fechaStr) return '';
  const d = new Date(fechaStr);
  return d.toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' });
};

function EventoCard({ evento, onVerDetalle }) {
  const icono = TIPO_ICONO[evento.tipoEvento] || TIPO_ICONO.OTRO;
  const tipoLabel = TIPO_LABEL[evento.tipoEvento] || 'Otro';
  const fecha = formatFecha(evento.fechaHora);
  const hora = formatHora(evento.fechaHora);

  let urgencyClass = '';
  if (evento.inminenteEn15Min) urgencyClass = 'evento-card--urgente';
  else if (evento.proximaEn24H) urgencyClass = 'evento-card--proximo';

  return (
    <div
      className={`evento-card ${urgencyClass} ${evento.cancelado ? 'evento-card--cancelado' : ''}`}
      onClick={() => onVerDetalle(evento.id)}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => e.key === 'Enter' && onVerDetalle(evento.id)}
    >
      {evento.inminenteEn15Min && (
        <div className="evento-banner-urgente">
          ⚠️ ¡Comienza en menos de 15 minutos!
        </div>
      )}

      <div className="evento-card__body">
        <div className="evento-card__icono" aria-label={tipoLabel}>
          {icono}
        </div>

        <div className="evento-card__info">
          <div className="evento-card__badges">
            <span className={`badge-tipo badge-tipo--${(evento.tipoEvento || 'OTRO').toLowerCase()}`}>
              {tipoLabel}
            </span>
            {evento.proximaEn24H && !evento.inminenteEn15Min && (
              <span className="badge-proximo">Próximamente</span>
            )}
            {evento.cancelado && <span className="badge-cancelado">Cancelado</span>}
            {evento.asistenciaConfirmada && !evento.cancelado && (
              <span className="badge-inscrito">Inscrito</span>
            )}
          </div>

          <h3 className="evento-card__titulo">{evento.titulo}</h3>

          <div className="evento-card__meta">
            <span>📅 {fecha}{hora ? ` · ${hora}` : ''}</span>
            {evento.comunidadNombre && (
              <span>🏠 {evento.comunidadNombre}</span>
            )}
            {!evento.esVirtual && evento.ubicacionNombre && (
              <span>📍 {evento.ubicacionNombre}</span>
            )}
            {evento.esVirtual && <span>💻 Online</span>}
          </div>

          {evento.aforo > 0 && (
            <div className="evento-card__aforo">
              👥 {evento.asistentesConfirmados || 0} / {evento.aforo} asistentes
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default function MisEventos() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [eventos, setEventos] = useState([]);
  const [historial, setHistorial] = useState([]);

  const [vistaHistorial, setVistaHistorial] = useState(false);
  const [filtroTipo, setFiltroTipo] = useState('Todos');

  const [loadingEventos, setLoadingEventos] = useState(true);
  const [error, setError] = useState(null);

  const fetchEventos = useCallback(async () => {
    setLoadingEventos(true);
    setError(null);
    try {
      const data = await getMisEventos();
      setEventos(data);
    } catch (e) {
      setError('No se pudieron cargar los eventos.');
    } finally {
      setLoadingEventos(false);
    }
  }, []);

  const fetchHistorial = useCallback(async () => {
    setLoadingEventos(true);
    setError(null);
    try {
      const data = await getMisEventosHistorial(false);
      setHistorial(data);
    } catch (e) {
      setError('No se pudo cargar el historial.');
    } finally {
      setLoadingEventos(false);
    }
  }, []);

  useEffect(() => {
    if (vistaHistorial) fetchHistorial();
    else fetchEventos();
  }, [vistaHistorial, fetchEventos, fetchHistorial]);

  const listaActual = vistaHistorial ? historial : eventos;
  const eventosFiltrados =
    filtroTipo === 'Todos'
      ? listaActual
      : listaActual.filter((e) => e.tipoEvento === filtroTipo);

  const eventosUrgentesBanner = eventos.filter((e) => e.inminenteEn15Min && !e.cancelado);

  return (
    <div className="mis-eventos-page">
      <Header user={user} page="mis-eventos" />

      <div className="mis-eventos-content">
        {/* Cabecera */}
        <div className="mis-eventos-header">
          <div className="mis-eventos-header__left">
            <h1 className="mis-eventos-titulo">Mis Eventos</h1>
            <p className="mis-eventos-subtitulo">Todos tus próximos eventos en un solo lugar</p>
          </div>
        </div>

        {/* Banner global eventos inminentes */}
        {eventosUrgentesBanner.length > 0 && (
          <div className="banner-inminente">
            ⚠️{' '}
            {eventosUrgentesBanner.length === 1
              ? `"${eventosUrgentesBanner[0].titulo}" comienza en menos de 15 minutos`
              : `${eventosUrgentesBanner.length} eventos comienzan en menos de 15 minutos`}
          </div>
        )}

        {/* Pestañas próximos / historial */}
        <div className="mis-eventos-tabs">
          <button
            className={`tab-btn ${!vistaHistorial ? 'tab-btn--activo' : ''}`}
            onClick={() => setVistaHistorial(false)}
          >
            Próximos
          </button>
          <button
            className={`tab-btn ${vistaHistorial ? 'tab-btn--activo' : ''}`}
            onClick={() => setVistaHistorial(true)}
          >
            Historial
          </button>
        </div>

        {/* Filtros por tipo */}
        <div className="filtros-tipo">
          {TIPOS_FILTRO.map((tipo) => (
            <button
              key={tipo}
              className={`filtro-btn ${filtroTipo === tipo ? 'filtro-btn--activo' : ''}`}
              onClick={() => setFiltroTipo(tipo)}
            >
              {tipo === 'Todos' ? 'Todos' : `${TIPO_ICONO[tipo]} ${TIPO_LABEL[tipo]}`}
            </button>
          ))}
        </div>

        {/* Lista de eventos */}
        {loadingEventos ? (
          <div className="mis-eventos-loading">Cargando eventos...</div>
        ) : error ? (
          <div className="mis-eventos-error">{error}</div>
        ) : eventosFiltrados.length === 0 ? (
          <div className="mis-eventos-empty">
            <span className="mis-eventos-empty__icono">📅</span>
            <p>
              No tienes {vistaHistorial ? 'eventos pasados' : 'próximos eventos'}
              {filtroTipo !== 'Todos' ? ` de tipo ${TIPO_LABEL[filtroTipo]}` : ''}.
            </p>
          </div>
        ) : (
          <div className="eventos-lista">
            {eventosFiltrados.map((evento) => (
              <EventoCard
                key={evento.id}
                evento={evento}
                onVerDetalle={(id) => navigate(`/eventos/${id}`)}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

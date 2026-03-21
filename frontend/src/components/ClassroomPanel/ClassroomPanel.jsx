import { useCallback, useEffect, useState } from 'react';
import {
  getTareas, crearTarea, getEntregas, entregarTarea, getMiEntrega,
  getMisEntregas, calificarEntrega,
  getRecursos, agregarRecurso, eliminarRecurso,
  getGrabaciones, agregarGrabacion,
} from '../../api/classroom.api';
import './ClassroomPanel.css';

// ─── Helpers ──────────────────────────────────────────────────────────────────

const fdt = (dt) => {
  if (!dt) return '—';
  return new Date(dt).toLocaleString('es-ES', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
  });
};

const ESTADO_BADGE = {
  ABIERTA: 'cp-badge--open', CERRADA: 'cp-badge--closed', CANCELADA: 'cp-badge--cancel',
  ENTREGADA: 'cp-badge--delivered', TARDÍA: 'cp-badge--late', CALIFICADA: 'cp-badge--graded',
};

const TIPO_ICON = { DOCUMENTO: '📄', PRESENTACION: '📊', VIDEO: '🎥', ENLACE: '🔗', ARCHIVO: '📁' };

// ─── Sub-components ───────────────────────────────────────────────────────────

function Badge({ estado }) {
  return <span className={`cp-badge ${ESTADO_BADGE[estado] ?? ''}`}>{estado}</span>;
}

function Spinner() {
  return <div className="cp-spinner" />;
}

function EmptyMsg({ msg }) {
  return <p className="cp-empty">{msg}</p>;
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB: TAREAS
// ─────────────────────────────────────────────────────────────────────────────

function TareasTab({ comunidadId, isAdmin, linkedCourse }) {
  const [tareas, setTareas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selTarea, setSelTarea] = useState(null); // task detail view
  const [entregas, setEntregas] = useState([]);
  const [miEntrega, setMiEntrega] = useState(null);
  const [showCreate, setShowCreate] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try { setTareas(await getTareas(comunidadId)); } catch { /* ignore */ }
    finally { setLoading(false); }
  }, [comunidadId]);

  useEffect(() => { load(); }, [load]);

  const openTarea = async (t) => {
    setSelTarea(t);
    try {
      if (isAdmin) {
        setEntregas(await getEntregas(comunidadId, t.id));
      } else {
        const me = await getMiEntrega(comunidadId, t.id).catch(() => null);
        setMiEntrega(me);
      }
    } catch { /* ignore */ }
  };

  if (selTarea) {
    return (
      <TareaDetalle
        tarea={selTarea}
        entregas={entregas}
        miEntrega={miEntrega}
        isAdmin={isAdmin}
        comunidadId={comunidadId}
        onBack={() => { setSelTarea(null); load(); }}
        onGraded={(updated) => setEntregas((prev) => prev.map((e) => e.id === updated.id ? updated : e))}
        onSubmitted={(e) => setMiEntrega(e)}
      />
    );
  }

  return (
    <div className="cp-section">
      <div className="cp-section-top">
        <h3 className="cp-section-h">📋 Tareas</h3>
        {isAdmin && (
          <button className="cp-btn cp-btn--primary" onClick={() => setShowCreate(true)}>
            + Nueva tarea
          </button>
        )}
      </div>
      {loading && <Spinner />}
      {!loading && tareas.length === 0 && <EmptyMsg msg="No hay tareas en esta comunidad." />}
      <div className="cp-list">
        {tareas.map((t) => (
          <div key={t.id} className="cp-card cp-card--clickable" onClick={() => openTarea(t)}>
            <div className="cp-card-top">
              <span className="cp-card-title">{t.titulo}</span>
              <Badge estado={t.estado} />
            </div>
            <div className="cp-card-meta">
              <span>⏰ Vence: {fdt(t.fechaVencimiento)}</span>
              <span>🏆 {t.puntosMaximos} pts</span>
              {isAdmin && <span>📬 {t.totalEntregas} entregas</span>}
              {t.publicada && <span className="cp-sync-badge">✓ Classroom</span>}
            </div>
            {t.descripcion && <p className="cp-card-desc">{t.descripcion}</p>}
          </div>
        ))}
      </div>
      {showCreate && (
        <CrearTareaModal
          comunidadId={comunidadId}
          linkedCourse={linkedCourse}
          onClose={() => setShowCreate(false)}
          onCreated={() => { setShowCreate(false); load(); }}
        />
      )}
    </div>
  );
}

function TareaDetalle({ tarea, entregas, miEntrega, isAdmin, comunidadId, onBack, onGraded, onSubmitted }) {
  const [submitUrl, setSubmitUrl] = useState('');
  const [submitText, setSubmitText] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(null);

  const handleSubmit = async () => {
    if (!submitUrl && !submitText) return;
    setSubmitting(true);
    setSubmitError(null);
    try {
      const res = await entregarTarea(comunidadId, tarea.id, { urlArchivo: submitUrl, contenido: submitText });
      onSubmitted(res);
    } catch (e) {
      setSubmitError(e?.message || 'Error al entregar');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="cp-detail">
      <button className="cp-back-btn" onClick={onBack}>← Volver</button>
      <h3 className="cp-detail-title">{tarea.titulo}</h3>
      <div className="cp-card-meta">
        <Badge estado={tarea.estado} />
        <span>⏰ {fdt(tarea.fechaVencimiento)}</span>
        <span>🏆 {tarea.puntosMaximos} pts</span>
        {tarea.publicada && <span className="cp-sync-badge">✓ Classroom</span>}
      </div>
      {tarea.descripcion && <p className="cp-detail-desc">{tarea.descripcion}</p>}

      {/* Alumno: entregar */}
      {!isAdmin && tarea.estado === 'ABIERTA' && !miEntrega && (
        <div className="cp-submit-box">
          <h4>Entregar tarea</h4>
          <input
            className="cp-input"
            placeholder="URL de tu archivo (Drive, etc.)"
            value={submitUrl}
            onChange={(e) => setSubmitUrl(e.target.value)}
          />
          <textarea
            className="cp-textarea"
            rows={4}
            placeholder="O escribe tu respuesta aquí..."
            value={submitText}
            onChange={(e) => setSubmitText(e.target.value)}
          />
          {submitError && <p className="cp-error">{submitError}</p>}
          <button className="cp-btn cp-btn--primary" onClick={handleSubmit} disabled={submitting}>
            {submitting ? 'Entregando…' : '📤 Entregar'}
          </button>
        </div>
      )}

      {/* Mi entrega */}
      {!isAdmin && miEntrega && (
        <div className="cp-my-submission">
          <h4>Mi entrega</h4>
          <Badge estado={miEntrega.estado} />
          {miEntrega.urlArchivo && <a href={miEntrega.urlArchivo} target="_blank" rel="noreferrer" className="cp-link">Ver archivo</a>}
          {miEntrega.contenido && <p className="cp-card-desc">{miEntrega.contenido}</p>}
          {miEntrega.calificacion != null && (
            <div className="cp-grade">
              <span className="cp-grade-num">{miEntrega.calificacion} / {tarea.puntosMaximos}</span>
              {miEntrega.comentarioRetroalimentacion && <p>{miEntrega.comentarioRetroalimentacion}</p>}
            </div>
          )}
        </div>
      )}

      {/* Admin: lista de entregas */}
      {isAdmin && (
        <div className="cp-entregas-list">
          <h4>Entregas ({entregas.length})</h4>
          {entregas.length === 0 && <EmptyMsg msg="Ningún alumno ha entregado aún." />}
          {entregas.map((e) => (
            <EntregaItem key={e.id} entrega={e} tarea={tarea} comunidadId={comunidadId} onGraded={onGraded} />
          ))}
        </div>
      )}
    </div>
  );
}

function EntregaItem({ entrega, tarea, comunidadId, onGraded }) {
  const [showGrade, setShowGrade] = useState(false);
  const [nota, setNota] = useState(entrega.calificacion ?? '');
  const [comentario, setComentario] = useState(entrega.comentarioRetroalimentacion ?? '');
  const [saving, setSaving] = useState(false);

  const save = async () => {
    setSaving(true);
    try {
      const res = await calificarEntrega(comunidadId, entrega.id, {
        calificacion: Number(nota), comentario,
      });
      onGraded(res);
      setShowGrade(false);
    } catch { /* ignore */ } finally { setSaving(false); }
  };

  return (
    <div className="cp-entrega-item">
      <div className="cp-entrega-top">
        <span className="cp-entrega-alumno">👤 {entrega.alumnoNombre}</span>
        <Badge estado={entrega.estado} />
        <span className="cp-entrega-date">{fdt(entrega.createdAt)}</span>
      </div>
      {entrega.urlArchivo && <a href={entrega.urlArchivo} target="_blank" rel="noreferrer" className="cp-link">Ver archivo</a>}
      {entrega.contenido && <p className="cp-card-desc">{entrega.contenido}</p>}
      {entrega.calificacion != null && (
        <div className="cp-grade">
          <span className="cp-grade-num">{entrega.calificacion} / {tarea.puntosMaximos}</span>
          {entrega.sincronizadoClassroom && <span className="cp-sync-badge">✓ Synced</span>}
        </div>
      )}
      <button className="cp-btn cp-btn--small" onClick={() => setShowGrade(!showGrade)}>
        {entrega.calificacion != null ? 'Editar nota' : 'Calificar'}
      </button>
      {showGrade && (
        <div className="cp-grade-form">
          <input
            className="cp-input cp-input--small"
            type="number"
            min={0}
            max={tarea.puntosMaximos}
            placeholder={`Nota (0-${tarea.puntosMaximos})`}
            value={nota}
            onChange={(e) => setNota(e.target.value)}
          />
          <textarea
            className="cp-textarea"
            rows={2}
            placeholder="Comentario de retroalimentación..."
            value={comentario}
            onChange={(e) => setComentario(e.target.value)}
          />
          <button className="cp-btn cp-btn--primary cp-btn--small" onClick={save} disabled={saving || nota === ''}>
            {saving ? 'Guardando…' : '💾 Guardar'}
          </button>
        </div>
      )}
    </div>
  );
}

function CrearTareaModal({ comunidadId, linkedCourse, onClose, onCreated }) {
  const [titulo, setTitulo] = useState('');
  const [desc, setDesc] = useState('');
  const [vence, setVence] = useState('');
  const [puntos, setPuntos] = useState(100);
  const [publicar, setPublicar] = useState(!!linkedCourse);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const submit = async () => {
    if (!titulo || !vence) { setError('Título y fecha de vencimiento son obligatorios.'); return; }
    setSaving(true); setError(null);
    try {
      await crearTarea(comunidadId, {
        titulo, descripcion: desc, fechaVencimiento: vence,
        puntosMaximos: Number(puntos), publicarEnClassroom: publicar, aceptarTerminos: true,
      });
      onCreated();
    } catch (e) {
      setError(e?.message || 'Error al crear la tarea');
    } finally { setSaving(false); }
  };

  return (
    <div className="cp-overlay" onClick={onClose}>
      <div className="cp-modal" onClick={(e) => e.stopPropagation()}>
        <div className="cp-modal-header">
          <h3>Nueva tarea</h3>
          <button className="cp-close-btn" onClick={onClose}>✕</button>
        </div>
        <div className="cp-modal-body">
          <label className="cp-label">Título *</label>
          <input className="cp-input" value={titulo} onChange={(e) => setTitulo(e.target.value)} placeholder="Título de la tarea" />

          <label className="cp-label">Descripción</label>
          <textarea className="cp-textarea" rows={3} value={desc} onChange={(e) => setDesc(e.target.value)} placeholder="Descripción..." />

          <label className="cp-label">Fecha de vencimiento *</label>
          <input className="cp-input" type="datetime-local" value={vence} onChange={(e) => setVence(e.target.value)} />

          <label className="cp-label">Puntuación máxima</label>
          <input className="cp-input" type="number" min={1} value={puntos} onChange={(e) => setPuntos(e.target.value)} />

          {linkedCourse && (
            <label className="cp-checkbox-label">
              <input type="checkbox" checked={publicar} onChange={(e) => setPublicar(e.target.checked)} />
              Publicar automáticamente en Google Classroom
            </label>
          )}

          {error && <p className="cp-error">{error}</p>}
        </div>
        <div className="cp-modal-footer">
          <button className="cp-btn cp-btn--ghost" onClick={onClose}>Cancelar</button>
          <button className="cp-btn cp-btn--primary" onClick={submit} disabled={saving}>
            {saving ? 'Creando…' : 'Crear tarea'}
          </button>
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB: RECURSOS
// ─────────────────────────────────────────────────────────────────────────────

function RecursosTab({ comunidadId, isAdmin, linkedCourse }) {
  const [recursos, setRecursos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showAdd, setShowAdd] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try { setRecursos(await getRecursos(comunidadId)); } catch { /* ignore */ }
    finally { setLoading(false); }
  }, [comunidadId]);

  useEffect(() => { load(); }, [load]);

  const handleDelete = async (id) => {
    if (!window.confirm('¿Eliminar este recurso?')) return;
    try { await eliminarRecurso(comunidadId, id); load(); } catch { /* ignore */ }
  };

  return (
    <div className="cp-section">
      <div className="cp-section-top">
        <h3 className="cp-section-h">📚 Recursos</h3>
        {isAdmin && (
          <button className="cp-btn cp-btn--primary" onClick={() => setShowAdd(true)}>
            + Añadir recurso
          </button>
        )}
      </div>
      {loading && <Spinner />}
      {!loading && recursos.length === 0 && <EmptyMsg msg="No hay recursos publicados." />}
      <div className="cp-list">
        {recursos.map((r) => (
          <div key={r.id} className="cp-card">
            <div className="cp-card-top">
              <span className="cp-card-title">{TIPO_ICON[r.tipo] ?? '🔖'} {r.titulo}</span>
              <div className="cp-card-actions">
                {r.sincronizadoClassroom && <span className="cp-sync-badge">✓ Classroom</span>}
                <a href={r.url} target="_blank" rel="noreferrer" className="cp-btn cp-btn--small">Abrir</a>
                {isAdmin && (
                  <button className="cp-btn cp-btn--danger cp-btn--small" onClick={() => handleDelete(r.id)}>
                    🗑
                  </button>
                )}
              </div>
            </div>
            {r.descripcion && <p className="cp-card-desc">{r.descripcion}</p>}
            <span className="cp-card-meta-item">Tipo: {r.tipo}</span>
          </div>
        ))}
      </div>
      {showAdd && (
        <AgregarRecursoModal
          comunidadId={comunidadId}
          linkedCourse={linkedCourse}
          onClose={() => setShowAdd(false)}
          onAdded={() => { setShowAdd(false); load(); }}
        />
      )}
    </div>
  );
}

function AgregarRecursoModal({ comunidadId, linkedCourse, onClose, onAdded }) {
  const [tipo, setTipo] = useState('ENLACE');
  const [titulo, setTitulo] = useState('');
  const [desc, setDesc] = useState('');
  const [url, setUrl] = useState('');
  const [driveId, setDriveId] = useState('');
  const [publicar, setPublicar] = useState(!!linkedCourse);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const submit = async () => {
    if (!titulo || !url) { setError('Título y URL son obligatorios.'); return; }
    setSaving(true); setError(null);
    try {
      await agregarRecurso(comunidadId, {
        tipo, titulo, descripcion: desc, url,
        googleDriveFileId: driveId || null,
        publicarEnClassroom: publicar, aceptarTerminos: true,
      });
      onAdded();
    } catch (e) {
      setError(e?.message || 'Error al añadir recurso');
    } finally { setSaving(false); }
  };

  return (
    <div className="cp-overlay" onClick={onClose}>
      <div className="cp-modal" onClick={(e) => e.stopPropagation()}>
        <div className="cp-modal-header">
          <h3>Añadir recurso</h3>
          <button className="cp-close-btn" onClick={onClose}>✕</button>
        </div>
        <div className="cp-modal-body">
          <label className="cp-label">Tipo</label>
          <select className="cp-input" value={tipo} onChange={(e) => setTipo(e.target.value)}>
            {['DOCUMENTO', 'PRESENTACION', 'VIDEO', 'ENLACE', 'ARCHIVO'].map((t) => (
              <option key={t} value={t}>{TIPO_ICON[t]} {t}</option>
            ))}
          </select>

          <label className="cp-label">Título *</label>
          <input className="cp-input" value={titulo} onChange={(e) => setTitulo(e.target.value)} placeholder="Título del recurso" />

          <label className="cp-label">Descripción</label>
          <textarea className="cp-textarea" rows={2} value={desc} onChange={(e) => setDesc(e.target.value)} placeholder="Descripción..." />

          <label className="cp-label">URL *</label>
          <input className="cp-input" value={url} onChange={(e) => setUrl(e.target.value)} placeholder="https://..." />

          <label className="cp-label">ID de Google Drive (opcional)</label>
          <input className="cp-input" value={driveId} onChange={(e) => setDriveId(e.target.value)} placeholder="ID del archivo en Drive" />

          {linkedCourse && (
            <label className="cp-checkbox-label">
              <input type="checkbox" checked={publicar} onChange={(e) => setPublicar(e.target.checked)} />
              Publicar automáticamente en Google Classroom
            </label>
          )}

          {error && <p className="cp-error">{error}</p>}
        </div>
        <div className="cp-modal-footer">
          <button className="cp-btn cp-btn--ghost" onClick={onClose}>Cancelar</button>
          <button className="cp-btn cp-btn--primary" onClick={submit} disabled={saving}>
            {saving ? 'Añadiendo…' : 'Añadir recurso'}
          </button>
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB: GRABACIONES
// ─────────────────────────────────────────────────────────────────────────────

function GrabacionesTab({ comunidadId, isAdmin, linkedCourse }) {
  const [grabaciones, setGrabaciones] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showAdd, setShowAdd] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try { setGrabaciones(await getGrabaciones(comunidadId)); } catch { /* ignore */ }
    finally { setLoading(false); }
  }, [comunidadId]);

  useEffect(() => { load(); }, [load]);

  return (
    <div className="cp-section">
      <div className="cp-section-top">
        <h3 className="cp-section-h">🎥 Grabaciones</h3>
        {isAdmin && (
          <button className="cp-btn cp-btn--primary" onClick={() => setShowAdd(true)}>
            + Añadir grabación
          </button>
        )}
      </div>
      {loading && <Spinner />}
      {!loading && grabaciones.length === 0 && <EmptyMsg msg="No hay grabaciones disponibles." />}
      <div className="cp-list">
        {grabaciones.map((g) => (
          <div key={g.id} className="cp-card">
            <div className="cp-card-top">
              <span className="cp-card-title">🎬 {g.titulo}</span>
              <div className="cp-card-actions">
                {g.sincronizadoClassroom && <span className="cp-sync-badge">✓ Drive</span>}
                <a href={g.urlGrabacion} target="_blank" rel="noreferrer" className="cp-btn cp-btn--small">▶ Ver</a>
              </div>
            </div>
            {g.descripcion && <p className="cp-card-desc">{g.descripcion}</p>}
            <div className="cp-card-meta">
              {g.durationSeconds && <span>⏱ {Math.floor(g.durationSeconds / 60)} min</span>}
              <span>📅 {fdt(g.createdAt)}</span>
              <span className="cp-badge">{g.estado}</span>
            </div>
          </div>
        ))}
      </div>
      {showAdd && (
        <AgregarGrabacionModal
          comunidadId={comunidadId}
          linkedCourse={linkedCourse}
          onClose={() => setShowAdd(false)}
          onAdded={() => { setShowAdd(false); load(); }}
        />
      )}
    </div>
  );
}

function AgregarGrabacionModal({ comunidadId, linkedCourse, onClose, onAdded }) {
  const [titulo, setTitulo] = useState('');
  const [desc, setDesc] = useState('');
  const [url, setUrl] = useState('');
  const [driveId, setDriveId] = useState('');
  const [duracion, setDuracion] = useState('');
  const [compartir, setCompartir] = useState(!!linkedCourse);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const submit = async () => {
    if (!titulo || !url) { setError('Título y URL son obligatorios.'); return; }
    setSaving(true); setError(null);
    try {
      await agregarGrabacion(comunidadId, {
        titulo, descripcion: desc, urlGrabacion: url,
        googleDriveFileId: driveId || null,
        durationSeconds: duracion ? Number(duracion) : null,
        compartirEnClassroom: compartir,
      });
      onAdded();
    } catch (e) {
      setError(e?.message || 'Error al añadir grabación');
    } finally { setSaving(false); }
  };

  return (
    <div className="cp-overlay" onClick={onClose}>
      <div className="cp-modal" onClick={(e) => e.stopPropagation()}>
        <div className="cp-modal-header">
          <h3>Añadir grabación</h3>
          <button className="cp-close-btn" onClick={onClose}>✕</button>
        </div>
        <div className="cp-modal-body">
          <label className="cp-label">Título *</label>
          <input className="cp-input" value={titulo} onChange={(e) => setTitulo(e.target.value)} placeholder="Título de la grabación" />

          <label className="cp-label">Descripción</label>
          <textarea className="cp-textarea" rows={2} value={desc} onChange={(e) => setDesc(e.target.value)} placeholder="Descripción..." />

          <label className="cp-label">URL de la grabación *</label>
          <input className="cp-input" value={url} onChange={(e) => setUrl(e.target.value)} placeholder="https://drive.google.com/..." />

          <label className="cp-label">ID de Google Drive (opcional)</label>
          <input className="cp-input" value={driveId} onChange={(e) => setDriveId(e.target.value)} placeholder="ID del archivo en Drive" />

          <label className="cp-label">Duración (segundos, opcional)</label>
          <input className="cp-input" type="number" min={0} value={duracion} onChange={(e) => setDuracion(e.target.value)} placeholder="Ej: 3600" />

          {linkedCourse && (
            <label className="cp-checkbox-label">
              <input type="checkbox" checked={compartir} onChange={(e) => setCompartir(e.target.checked)} />
              Compartir automáticamente en Google Classroom
            </label>
          )}

          {error && <p className="cp-error">{error}</p>}
        </div>
        <div className="cp-modal-footer">
          <button className="cp-btn cp-btn--ghost" onClick={onClose}>Cancelar</button>
          <button className="cp-btn cp-btn--primary" onClick={submit} disabled={saving}>
            {saving ? 'Añadiendo…' : 'Añadir grabación'}
          </button>
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB: MIS CALIFICACIONES
// ─────────────────────────────────────────────────────────────────────────────

function MisCalificacionesTab({ comunidadId }) {
  const [entregas, setEntregas] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      setLoading(true);
      try { setEntregas(await getMisEntregas(comunidadId)); } catch { /* ignore */ }
      finally { setLoading(false); }
    })();
  }, [comunidadId]);

  if (loading) return <Spinner />;
  if (entregas.length === 0) return <EmptyMsg msg="Aún no tienes entregas en esta comunidad." />;

  return (
    <div className="cp-section">
      <h3 className="cp-section-h">📊 Mis calificaciones</h3>
      <div className="cp-list">
        {entregas.map((e) => (
          <div key={e.id} className="cp-card">
            <div className="cp-card-top">
              <span className="cp-card-title">{e.tareaTitulo}</span>
              <Badge estado={e.estado} />
            </div>
            {e.urlArchivo && <a href={e.urlArchivo} target="_blank" rel="noreferrer" className="cp-link">Ver mi entrega</a>}
            {e.calificacion != null ? (
              <div className="cp-grade">
                <span className="cp-grade-num">🏆 {e.calificacion} pts</span>
                {e.comentarioRetroalimentacion && <p className="cp-card-desc">{e.comentarioRetroalimentacion}</p>}
              </div>
            ) : (
              <p className="cp-card-desc cp-pending">Pendiente de calificación</p>
            )}
            <span className="cp-card-meta-item">📅 Entregado: {fdt(e.createdAt)}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// MAIN PANEL
// ─────────────────────────────────────────────────────────────────────────────

const TABS = [
  { key: 'tareas', label: '📋 Tareas' },
  { key: 'recursos', label: '📚 Recursos' },
  { key: 'grabaciones', label: '🎥 Grabaciones' },
  { key: 'calificaciones', label: '📊 Mis notas' },
];

export default function ClassroomPanel({ comunidadId, isAdmin, linkedCourse }) {
  const [tab, setTab] = useState('tareas');

  return (
    <div className="cp-panel">
      <div className="cp-panel-header">
        <div className="cp-panel-title">
          <img
            src="https://upload.wikimedia.org/wikipedia/commons/thumb/5/59/Google_Classroom_Logo.png/120px-Google_Classroom_Logo.png"
            alt="Classroom"
            className="cp-classroom-logo"
          />
          <div>
            <h2 className="cp-panel-h">Google Classroom</h2>
            {linkedCourse && (
              <p className="cp-panel-course">Curso: {linkedCourse.courseName || linkedCourse.classroomCourseName || 'Vinculado'}</p>
            )}
          </div>
        </div>
      </div>

      <div className="cp-tabs">
        {TABS.map((t) => (
          // Students don't see "Mis notas" tab if they are admin
          (t.key === 'calificaciones' && isAdmin) ? null : (
            <button
              key={t.key}
              className={`cp-tab ${tab === t.key ? 'cp-tab--active' : ''}`}
              onClick={() => setTab(t.key)}
            >
              {t.label}
            </button>
          )
        ))}
        {isAdmin && (
          <button
            className={`cp-tab ${tab === 'todas-entregas' ? 'cp-tab--active' : ''}`}
            onClick={() => setTab('tareas')}
          />
        )}
      </div>

      <div className="cp-tab-content">
        {tab === 'tareas' && (
          <TareasTab comunidadId={comunidadId} isAdmin={isAdmin} linkedCourse={linkedCourse} />
        )}
        {tab === 'recursos' && (
          <RecursosTab comunidadId={comunidadId} isAdmin={isAdmin} linkedCourse={linkedCourse} />
        )}
        {tab === 'grabaciones' && (
          <GrabacionesTab comunidadId={comunidadId} isAdmin={isAdmin} linkedCourse={linkedCourse} />
        )}
        {tab === 'calificaciones' && !isAdmin && (
          <MisCalificacionesTab comunidadId={comunidadId} />
        )}
      </div>
    </div>
  );
}

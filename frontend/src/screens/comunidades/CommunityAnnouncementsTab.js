import React, { useEffect, useState } from 'react';
import axiosInstance from '../../api/axiosConfig';
import { getAnnouncementComments, postAnnouncementComment } from '../../api/announcementComments';
import './CommunityAnnouncementsTab.css';

export default function CommunityAnnouncementsTab({ communityId, isAdmin }) {
  const [announcements, setAnnouncements] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [form, setForm] = useState({ titulo: '', contenido: '', permitirComentarios: true });
  const [creating, setCreating] = useState(false);
  const [formError, setFormError] = useState(null);

  useEffect(() => {
    const fetchAnnouncements = async () => {
      try {
        setLoading(true);
        setError(null);
        const res = await axiosInstance.get(`/api/v1/communities/${communityId}/announcements?page=0&size=50`);
        setAnnouncements(res.data.anuncios || []);
      } catch (err) {
        setError('No se pudieron cargar los anuncios.');
      } finally {
        setLoading(false);
      }
    };
    if (communityId) fetchAnnouncements();
  }, [communityId]);

  const handleOpenModal = () => {
    setForm({ titulo: '', contenido: '', permitirComentarios: true });
    setFormError(null);
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setFormError(null);
  };

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const titulo = form.titulo.trim();
    const contenido = form.contenido.trim();
    if (titulo.length < 5 || titulo.length > 200) {
      setFormError('El título debe tener entre 5 y 200 caracteres.');
      return;
    }
    if (contenido.length < 10 || contenido.length > 5000) {
      setFormError('El contenido debe tener entre 10 y 5000 caracteres.');
      return;
    }
    setCreating(true);
    setFormError(null);
    try {
      await axiosInstance.post(`/api/v1/communities/${communityId}/announcements`, {
        titulo,
        contenido,
        permitirComentarios: form.permitirComentarios,
      });
      setShowModal(false);
      setForm({ titulo: '', contenido: '', permitirComentarios: true });
      // Refrescar anuncios
      setLoading(true);
      const res = await axiosInstance.get(`/api/v1/communities/${communityId}/announcements?page=0&size=50`);
      setAnnouncements(res.data.anuncios || []);
    } catch (err) {
      setFormError('Error al crear el anuncio.');
    } finally {
      setCreating(false);
      setLoading(false);
    }
  };

  return (
    <div className="catab-list">
      {isAdmin && (
        <button className="catab-btn-create" onClick={handleOpenModal}>
          📢 Crear anuncio
        </button>
      )}

      {showModal && (
        <div className="catab-modal-overlay">
          <div className="catab-modal" style={{maxWidth: 420, borderRadius: 12, boxShadow: '0 4px 24px #0002', background: '#fff', padding: 24}}>
            <h2 style={{marginTop: 0, marginBottom: 16, fontWeight: 700, fontSize: 22, color: '#333'}}>Crear anuncio</h2>
            <form onSubmit={handleSubmit}>
              <label style={{display: 'block', fontWeight: 500, marginBottom: 6}}>Título</label>
              <input
                type="text"
                name="titulo"
                placeholder="Título del anuncio"
                value={form.titulo}
                onChange={handleChange}
                minLength={5}
                maxLength={200}
                required
                className="catab-input"
                style={{width: '100%', marginBottom: 12, padding: 8, borderRadius: 6, border: '1px solid #ccc', fontSize: 16}}
                autoFocus
              />
              <label style={{display: 'block', fontWeight: 500, marginBottom: 6}}>Contenido</label>
              <textarea
                name="contenido"
                placeholder="Contenido del anuncio"
                value={form.contenido}
                onChange={handleChange}
                minLength={10}
                maxLength={5000}
                required
                className="catab-textarea"
                style={{width: '100%', marginBottom: 12, padding: 8, borderRadius: 6, border: '1px solid #ccc', fontSize: 15, resize: 'vertical'}} 
                rows={6}
              />
              <label style={{display: 'flex', alignItems: 'center', gap: 8, marginBottom: 16, fontSize: 15}}>
                <input
                  type="checkbox"
                  name="permitirComentarios"
                  checked={form.permitirComentarios}
                  onChange={handleChange}
                  style={{marginRight: 6}}
                />
                Permitir comentarios en este anuncio
              </label>
              {formError && <div className="catab-form-error" style={{color: '#c00', marginBottom: 12}}>{formError}</div>}
              <div className="catab-modal-actions" style={{display: 'flex', justifyContent: 'flex-end', gap: 10}}>
                <button type="button" onClick={handleCloseModal} disabled={creating} className="catab-btn-cancel" style={{padding: '8px 18px', borderRadius: 6, border: 'none', background: '#eee', color: '#333', fontWeight: 500, fontSize: 15, cursor: 'pointer'}}>Cancelar</button>
                <button type="submit" disabled={creating} className="catab-btn-submit" style={{padding: '8px 18px', borderRadius: 6, border: 'none', background: '#2b7cff', color: '#fff', fontWeight: 600, fontSize: 15, cursor: 'pointer'}}>{creating ? 'Creando...' : 'Crear'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {loading ? (
        <div className="catab-loading">Cargando anuncios...</div>
      ) : error ? (
        <div className="catab-error">{error}</div>
      ) : !announcements.length ? (
        <div className="catab-empty">No hay anuncios en esta comunidad.</div>
      ) : (
        announcements.map(anuncio => (
          <div key={anuncio.id} className="catab-item">
            <div className="catab-title">{anuncio.titulo}</div>
            <div className="catab-meta">
              <span>{new Date(anuncio.createdAt).toLocaleString('es-ES')}</span>
              {anuncio.editado && <span className="catab-editado">(editado)</span>}
            </div>
            <div className="catab-content">{anuncio.contenido}</div>
            {anuncio.permitirComentarios && (
              <CommentsSection anuncioId={anuncio.id} />
            )}
          </div>
        ))
      )}
    </div>
  );
}

// --- Componente de comentarios ---
function CommentsSection({ anuncioId }) {
  const [comments, setComments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [input, setInput] = useState("");
  const [posting, setPosting] = useState(false);

  useEffect(() => {
    setLoading(true);
    setError(null);
    getAnnouncementComments(anuncioId)
      .then(setComments)
      .catch(() => setError('Error al cargar comentarios'))
      .finally(() => setLoading(false));
  }, [anuncioId]);

  const handlePost = async (e) => {
    e.preventDefault();
    if (!input.trim()) return;
    setPosting(true);
    try {
      const nuevo = await postAnnouncementComment(anuncioId, input);
      setComments(prev => [nuevo, ...prev]);
      setInput("");
    } catch {
      setError('Error al publicar el comentario');
    } finally {
      setPosting(false);
    }
  };

  return (
    <div className="catab-comments-section">
      <div className="catab-comments-title">Comentarios</div>
      {loading ? (
        <div style={{ color: '#64748b', marginBottom: 8 }}>Cargando comentarios...</div>
      ) : error ? (
        <div style={{ color: '#c00', marginBottom: 8 }}>Error al cargar comentarios</div>
      ) : (
        <>
          <div className="catab-comment-list">
            {comments.length === 0 ? (
              <div style={{ color: '#64748b', fontSize: '0.98rem' }}>Sé el primero en comentar</div>
            ) : (
              comments.map(c => (
                <div key={c.id} className="catab-comment-item">
                  <span style={{ fontWeight: 500 }}>{c.autor}:</span> {c.texto}
                  <span style={{ float: 'right', color: '#94a3b8', fontSize: '0.93em' }}>{new Date(c.fecha).toLocaleString('es-ES')}</span>
                </div>
              ))
            )}
          </div>
          <form className="catab-comment-form" onSubmit={handlePost}>
            <input
              className="catab-comment-input"
              type="text"
              placeholder="Escribe un comentario..."
              value={input}
              onChange={e => setInput(e.target.value)}
              maxLength={500}
              disabled={posting}
            />
            <button className="catab-comment-btn" type="submit" disabled={posting || !input.trim()}>
              Comentar
            </button>
          </form>
        </>
      )}
    </div>
  );
}

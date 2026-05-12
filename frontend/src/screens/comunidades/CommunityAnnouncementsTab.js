import React, { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import axiosInstance from '../../api/axiosConfig';
import { getAnnouncementComments, postAnnouncementComment, deleteAnnouncementComment } from '../../api/announcementComments';
import './CommunityAnnouncementsTab.css';
import { getApiBaseUrl } from '../../api/baseUrl';

const DEFAULT_PROFILE_AVATAR =
  "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 120 120'%3E%3Ccircle cx='60' cy='60' r='60' fill='%23E6EAF3'/%3E%3Ccircle cx='60' cy='46' r='22' fill='%2395A1BB'/%3E%3Cpath d='M20 106c6-20 22-32 40-32s34 12 40 32' fill='%2395A1BB'/%3E%3C/svg%3E";

const toAbsoluteImageUrl = (imageUrl, fallback = DEFAULT_PROFILE_AVATAR) => {
  const raw = String(imageUrl || '').trim();
  if (!raw) {
    return fallback;
  }
  if (/^https?:\/\//i.test(raw) || raw.startsWith('data:') || raw.startsWith('blob:')) {
    return raw;
  }
  const base = getApiBaseUrl();
  return raw.startsWith('/') ? `${base}${raw}` : `${base}/${raw}`;
};

export default function CommunityAnnouncementsTab({ communityId, isAdmin }) {
  const currentUserId = localStorage.getItem('userId');
  const [announcements, setAnnouncements] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [form, setForm] = useState({ titulo: '', contenido: '', permitirComentarios: true });
  const [creating, setCreating] = useState(false);
  const [deletingAnnouncementId, setDeletingAnnouncementId] = useState(null);
  const [formError, setFormError] = useState(null);
  const [editingAnnouncement, setEditingAnnouncement] = useState(null);
  const [editForm, setEditForm] = useState({ titulo: '', contenido: '', permitirComentarios: true });
  const [editFormError, setEditFormError] = useState(null);
  const [editing, setEditing] = useState(false);
  const [searchParams] = useSearchParams();
  const anuncioIdParam = searchParams.get('anuncioId');

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
    setActionError(null);
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

  const handleDeleteAnnouncement = async (announcementId) => {
    const confirmed = window.confirm('¿Seguro que quieres eliminar este anuncio?');
    if (!confirmed) {
      return;
    }

    setActionError(null);
    setDeletingAnnouncementId(announcementId);
    try {
      await axiosInstance.delete(`/api/v1/communities/${communityId}/announcements/${announcementId}`);
      setAnnouncements((prev) => prev.filter((item) => item.id !== announcementId));
    } catch {
      setActionError('No se pudo eliminar el anuncio.');
    } finally {
      setDeletingAnnouncementId(null);
    }
  };


  const handleOpenEdit = (anuncio) => {
    setEditingAnnouncement(anuncio);
    setEditForm({ titulo: anuncio.titulo, contenido: anuncio.contenido, permitirComentarios: anuncio.permitirComentarios });
    setEditFormError(null);
    setActionError(null);
  };

  const handleCloseEdit = () => {
    setEditingAnnouncement(null);
    setEditFormError(null);
  };

  const handleEditChange = (e) => {
    const { name, value, type, checked } = e.target;
    setEditForm((prev) => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
  };

  const handleEditSubmit = async (e) => {
    e.preventDefault();
    const titulo = editForm.titulo.trim();
    const contenido = editForm.contenido.trim();
    if (titulo.length < 5 || titulo.length > 200) {
      setEditFormError('El título debe tener entre 5 y 200 caracteres.');
      return;
    }
    if (contenido.length < 10 || contenido.length > 5000) {
      setEditFormError('El contenido debe tener entre 10 y 5000 caracteres.');
      return;
    }
    setEditing(true);
    setEditFormError(null);
    try {
      const res = await axiosInstance.put(
        `/api/v1/communities/${communityId}/announcements/${editingAnnouncement.id}`,
        { titulo, contenido, permitirComentarios: editForm.permitirComentarios }
      );
      setAnnouncements((prev) =>
        prev.map((a) => (a.id === editingAnnouncement.id ? res.data : a))
      );
      setEditingAnnouncement(null);
    } catch {
      setEditFormError('Error al editar el anuncio.');
    } finally {
      setEditing(false);
    }
  };

  function formatDateTime(dateString) {
      const date = new Date(dateString);
      return date.toLocaleString(undefined, {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false
      });
    }

  return (
    <div className="catab-list">
      {isAdmin && (
        <button className="catab-btn-create" onClick={handleOpenModal}>
          📢 Crear anuncio
        </button>
      )}

      {editingAnnouncement && (
        <div className="catab-modal-overlay">
          <div className="catab-modal">
            <h2>Editar anuncio</h2>
            <form onSubmit={handleEditSubmit}>
              <label className="catab-label">Título</label>
              <input
                type="text"
                name="titulo"
                placeholder="Título del anuncio"
                value={editForm.titulo}
                onChange={handleEditChange}
                minLength={5}
                maxLength={200}
                required
                className="catab-input"
                autoFocus
              />
              <label className="catab-label">Contenido</label>
              <textarea
                name="contenido"
                placeholder="Contenido del anuncio"
                value={editForm.contenido}
                onChange={handleEditChange}
                minLength={10}
                maxLength={5000}
                required
                className="catab-textarea"
                rows={6}
              />
              <label className="catab-label catab-checkbox-label">
                <input
                  type="checkbox"
                  name="permitirComentarios"
                  checked={editForm.permitirComentarios}
                  onChange={handleEditChange}
                />
                Permitir comentarios en este anuncio
              </label>
              {editFormError && <div className="catab-form-error">{editFormError}</div>}
              <div className="catab-modal-actions">
                <button type="button" onClick={handleCloseEdit} disabled={editing} className="catab-btn-cancel">Cancelar</button>
                <button type="submit" disabled={editing} className="catab-btn-submit">{editing ? 'Guardando...' : 'Guardar'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {showModal && (
        <div className="catab-modal-overlay">
          <div className="catab-modal">
            <h2>Crear anuncio</h2>
            <form onSubmit={handleSubmit}>
              <label className="catab-label">Título</label>
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
                autoFocus
              />
              <label className="catab-label">Contenido</label>
              <textarea
                name="contenido"
                placeholder="Contenido del anuncio"
                value={form.contenido}
                onChange={handleChange}
                minLength={10}
                maxLength={5000}
                required
                className="catab-textarea"
                rows={6}
              />
              <label className="catab-label catab-checkbox-label">
                <input
                  type="checkbox"
                  name="permitirComentarios"
                  checked={form.permitirComentarios}
                  onChange={handleChange}
                />
                Permitir comentarios en este anuncio
              </label>
              {formError && <div className="catab-form-error">{formError}</div>}
              <div className="catab-modal-actions">
                <button type="button" onClick={handleCloseModal} disabled={creating} className="catab-btn-cancel">Cancelar</button>
                <button type="submit" disabled={creating} className="catab-btn-submit">{creating ? 'Creando...' : 'Crear'}</button>
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
        announcements.map(anuncio => {
          const isHighlighted = anuncioIdParam && String(anuncio.id) === String(anuncioIdParam);
          return (
            <div
              key={anuncio.id}
              className={`catab-item${isHighlighted ? ' catab-item-highlighted' : ''}`}
              style={isHighlighted ? { border: '2px solid #2b7cff', background: '#eaf2ff' } : {}}
            >
              <div className="catab-item-header">
                <div className="catab-title">{anuncio.titulo}</div>
                {(isAdmin || anuncio.usuario?.id === currentUserId) && (
                  <div className="catab-item-actions">
                    <button
                      type="button"
                      className="catab-btn-edit"
                      onClick={() => handleOpenEdit(anuncio)}
                    >
                      Editar
                    </button>
                    <button
                      type="button"
                      className="catab-btn-delete"
                      onClick={() => handleDeleteAnnouncement(anuncio.id)}
                      disabled={deletingAnnouncementId === anuncio.id}
                    >
                      {deletingAnnouncementId === anuncio.id ? 'Eliminando...' : 'Eliminar'}
                    </button>
                  </div>
                )}
              </div>
              <div className="catab-meta">
                <span>{formatDateTime(anuncio.createdAt)}</span>
                {anuncio.editado && <span className="catab-editado">(editado)</span>}
              </div>
              <div className="catab-content">{anuncio.contenido}</div>
              {anuncio.permitirComentarios && (
                <CommentsSection
                  anuncioId={anuncio.id}
                  isAdmin={isAdmin}
                  currentUserId={currentUserId}
                />
              )}
            </div>
          );
        })
      )}
      {actionError && <div className="catab-error">{actionError}</div>}
    </div>
  );
}

// --- Componente de comentarios ---
function CommentsSection({ anuncioId, isAdmin, currentUserId }) {
  const [comments, setComments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [input, setInput] = useState("");
  const [posting, setPosting] = useState(false);
  const [deletingCommentId, setDeletingCommentId] = useState(null);

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

  const handleDeleteComment = async (comentarioId) => {
    if (!window.confirm('¿Eliminar este comentario?')) return;
    try {
      setDeletingCommentId(comentarioId);
      await deleteAnnouncementComment(anuncioId, comentarioId);
      setComments(prev => prev.filter(c => c.id !== comentarioId));
    } catch {
      setError('No se pudo eliminar el comentario.');
    } finally {
      setDeletingCommentId(null);
    }
  };

  function formatDateTime(dateString) {
    const date = new Date(dateString);
    return date.toLocaleString(undefined, {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    });
  }

  return (
    <div className="catab-comments-section">
      <div className="catab-comments-title">Comentarios</div>
      {loading ? (
        <div style={{ color: '#64748b', marginBottom: 8 }}>Cargando comentarios...</div>
      ) : error ? (
        <div style={{ color: '#c00', marginBottom: 8 }}>{error}</div>
      ) : (
        <>
          <div className="catab-comment-list">
            {comments.length === 0 ? (
              <div style={{ color: '#64748b', fontSize: '0.98rem' }}>Sé el primero en comentar</div>
            ) : (
              comments.map(c => {
                const esAutor = String(c.usuario?.id) === String(currentUserId);
                const puedeEliminar = isAdmin || esAutor;
                return (
                  <div key={c.id} className="catab-comment-item">
                    <img
                      src={toAbsoluteImageUrl(c.usuario?.foto || c.usuario?.avatarUrl, DEFAULT_PROFILE_AVATAR)}
                      alt={c.usuario?.nombre || 'Usuario'}
                      className="catab-comment-avatar"
                    />
                    <div className="catab-comment-body">
                      <span className="catab-comment-author">{c.usuario?.nombre || 'Usuario'}</span>
                      {' '}
                      <span className="catab-comment-texto">{c.texto}</span>
                    </div>
                    <div className="catab-comment-footer">
                      <span style={{ color: '#94a3b8', fontSize: '0.93em' }}>
                        {c.createdAt ? formatDateTime(c.createdAt) : ''}
                      </span>
                      {puedeEliminar && (
                        <button
                          type="button"
                          onClick={() => handleDeleteComment(c.id)}
                          disabled={deletingCommentId === c.id}
                          className="catab-comment-delete-btn"
                        >
                          {deletingCommentId === c.id ? '...' : '🗑️'}
                        </button>
                      )}
                    </div>
                  </div>
                );
              })
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
import { useState } from 'react';
import { communitiesApi } from '../../api/communities.api';
import './EditCommunityModal.css';

export default function EditCommunityModal({ community, onClose, onSaved }) {
  const [nombre, setNombre] = useState(community.nombre || '');
  const [descripcion, setDescripcion] = useState(community.descripcion || '');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!nombre.trim()) {
      setError('El nombre es obligatorio');
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await communitiesApi.update(community.id, {
        nombre: nombre.trim(),
        descripcion: descripcion.trim(),
        imagenUrl: community.imagenUrl,
      });
      onSaved();
    } catch (err) {
      console.error('Error al actualizar comunidad:', err);
      setError(err.message || 'Error al actualizar la comunidad');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="ecm-overlay" data-testid="ecm-overlay" onClick={onClose}>
      <div className="ecm-modal" onClick={(e) => e.stopPropagation()}>
        <div className="ecm-header">
          <h2>Editar comunidad</h2>
          <button className="ecm-close" onClick={onClose}>X</button>
        </div>
        <form onSubmit={handleSubmit} className="ecm-form">
          <div className="ecm-field">
            <label htmlFor="ecm-nombre">Nombre</label>
            <input
              id="ecm-nombre"
              type="text"
              value={nombre}
              onChange={(e) => setNombre(e.target.value)}
              maxLength={100}
              required
            />
          </div>
          <div className="ecm-field">
            <label htmlFor="ecm-descripcion">Descripcion</label>
            <textarea
              id="ecm-descripcion"
              value={descripcion}
              onChange={(e) => setDescripcion(e.target.value)}
              maxLength={1000}
              rows={4}
            />
          </div>
          {error && <p className="ecm-error">{error}</p>}
          <div className="ecm-actions">
            <button type="button" className="ecm-btn ecm-btn--secondary" onClick={onClose} disabled={saving}>
              Cancelar
            </button>
            <button type="submit" className="ecm-btn ecm-btn--primary" disabled={saving}>
              {saving ? 'Guardando...' : 'Guardar cambios'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

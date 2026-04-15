import { useState } from 'react';
import { communitiesApi } from '../../api/communities.api';
import './EditCommunityModal.css';

export default function EditCommunityModal({ community, onClose, onSaved }) {
  const [nombre, setNombre] = useState(community.nombre || '');
  const [descripcion, setDescripcion] = useState(community.descripcion || '');
  const [imagenPreview, setImagenPreview] = useState(community.imagenUrl || null);
  const [nuevaImagen, setNuevaImagen] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const handleImageUpload = (e) => {
    const file = e.target.files[0];
    if (file) {
      // Validar tipo de archivo
      if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
        setError('Solo se permiten imágenes JPG, PNG o WEBP');
        return;
      }
      // Validar tamaño (5MB)
      if (file.size > 5 * 1024 * 1024) {
        setError('La imagen no debe exceder 5MB');
        return;
      }
      setNuevaImagen(file);
      const reader = new FileReader();
      reader.onloadend = () => {
        setImagenPreview(reader.result);
      };
      reader.readAsDataURL(file);
      setError(null);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!nombre.trim()) {
      setError('El nombre es obligatorio');
      return;
    }
    setSaving(true);
    setError(null);
    try {
      // Actualizar datos básicos
      await communitiesApi.update(community.id, {
        nombre: nombre.trim(),
        descripcion: descripcion.trim(),
        imagenUrl: nuevaImagen ? undefined : community.imagenUrl,
      });

      // Si hay nueva imagen, subirla
      if (nuevaImagen) {
        const formData = new FormData();
        formData.append('file', nuevaImagen);
        try {
          await communitiesApi.uploadPhoto(community.id, formData);
          console.log("✅ Foto actualizada correctamente");
        } catch (uploadErr) {
          console.warn("⚠️ Los datos se guardaron pero hubo error al subir la foto:", uploadErr);
          // No fallar completamente si la foto falla
        }
      }

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
          {/* Sección de imagen */}
          <div className="ecm-image-section">
            <label>Imagen de portada</label>
            {imagenPreview && (
              <div className="ecm-image-preview">
                <img src={imagenPreview} alt="Preview" />
              </div>
            )}
            <label htmlFor="ecm-image-input" className="ecm-image-input-label">
              {nuevaImagen || community.imagenUrl ? 'Cambiar imagen' : 'Subir imagen'}
            </label>
            <input
              id="ecm-image-input"
              type="file"
              accept="image/*"
              onChange={handleImageUpload}
              className="ecm-image-input"
              disabled={saving}
            />
            <p className="ecm-image-help">JPG, PNG o WEBP. Máximo 5MB.</p>
          </div>

          {/* Campos de texto */}
          <div className="ecm-field">
            <label htmlFor="ecm-nombre">Nombre</label>
            <input
              id="ecm-nombre"
              type="text"
              value={nombre}
              onChange={(e) => setNombre(e.target.value)}
              maxLength={100}
              required
              disabled={saving}
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
              disabled={saving}
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

import { useEffect, useState } from 'react';
import { communitiesApi } from '../../api/communities.api';
import './EditCommunityModal.css';

export default function EditCommunityModal({ community, onClose, onSaved }) {
  const [nombre, setNombre] = useState(community.nombre || '');
  const [descripcion, setDescripcion] = useState(community.descripcion || '');
  const [imagenPreview, setImagenPreview] = useState(community.imagenUrl || null);
  const [nuevaImagen, setNuevaImagen] = useState(null);
  const [tipoComunidad, setTipoComunidad] = useState(
    community.tipoGrupo === 'GRUPO_PRIVADO' ? 'GRUPO_PRIVADO' : 'COMUNIDAD_PUBLICA'
  );
  const initialTipoGrupo =
    community.tipoGrupo === 'GRUPO_PRIVADO' ? 'GRUPO_PRIVADO' : 'COMUNIDAD_PUBLICA';

  // categorias: lista de objetos { id, nombre } cuando vienen del backend.
  // Las nuevas locales tienen id=null hasta que se guarden.
  const [existingCategorias, setExistingCategorias] = useState([]);
  const [removedCategoryIds, setRemovedCategoryIds] = useState([]);
  const [newCategorias, setNewCategorias] = useState([]); // array de strings
  const [categoriaInput, setCategoriaInput] = useState('');
  const [categoriasLoading, setCategoriasLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const response = await communitiesApi.listCommunityCategories(community.id);
        const items = response?.data?.categorias || response?.categorias || response?.data || [];
        if (!cancelled) {
          setExistingCategorias(Array.isArray(items) ? items : []);
        }
      } catch (err) {
        console.error('Error cargando categorías de la comunidad:', err);
      } finally {
        if (!cancelled) setCategoriasLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [community.id]);

  const handleImageUpload = (e) => {
    const file = e.target.files[0];
    if (file) {
      if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
        setError('Solo se permiten imágenes JPG, PNG o WEBP');
        return;
      }
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

  const visibleExistingCategorias = existingCategorias.filter(
    (cat) => !removedCategoryIds.includes(cat.id)
  );

  const handleAddCategoria = () => {
    const value = categoriaInput.trim();
    if (!value) return;
    const alreadyInExisting = visibleExistingCategorias.some(
      (cat) => (cat.nombre || '').toLowerCase() === value.toLowerCase()
    );
    const alreadyInNew = newCategorias.some((n) => n.toLowerCase() === value.toLowerCase());
    if (alreadyInExisting || alreadyInNew) {
      setCategoriaInput('');
      return;
    }
    setNewCategorias([...newCategorias, value]);
    setCategoriaInput('');
  };

  const handleRemoveExistingCategoria = (categoryId) => {
    setRemovedCategoryIds([...removedCategoryIds, categoryId]);
  };

  const handleRemoveNewCategoria = (value) => {
    setNewCategorias(newCategorias.filter((n) => n !== value));
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
      // Datos básicos (siempre)
      await communitiesApi.update(community.id, {
        nombre: nombre.trim(),
        descripcion: descripcion.trim(),
        imagenUrl: nuevaImagen ? undefined : community.imagenUrl,
      });

      // Privacidad si cambió
      if (tipoComunidad !== initialTipoGrupo) {
        try {
          await communitiesApi.updatePrivacy(community.id, tipoComunidad);
        } catch (privacyErr) {
          console.warn('No se pudo actualizar la privacidad:', privacyErr);
          throw privacyErr;
        }
      }

      // Eliminar categorías marcadas
      for (const removedId of removedCategoryIds) {
        try {
          await communitiesApi.deleteCategory(community.id, removedId);
        } catch (delErr) {
          console.warn('No se pudo eliminar categoría', removedId, delErr);
        }
      }

      // Crear nuevas categorías
      for (const nuevo of newCategorias) {
        try {
          await communitiesApi.createCategory(community.id, { nombre: nuevo });
        } catch (createErr) {
          console.warn('No se pudo crear categoría', nuevo, createErr);
        }
      }

      // Si hay nueva imagen, subirla
      if (nuevaImagen) {
        const formData = new FormData();
        formData.append('file', nuevaImagen);
        try {
          await communitiesApi.uploadPhoto(community.id, formData);
        } catch (uploadErr) {
          console.warn('Los datos se guardaron pero hubo error al subir la foto:', uploadErr);
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

          {/* Tipo de comunidad */}
          <div className="ecm-field">
            <label>Tipo de Comunidad</label>
            <div className="ecm-radio-group">
              <label className="ecm-radio-label">
                <input
                  type="radio"
                  name="tipoComunidad"
                  value="COMUNIDAD_PUBLICA"
                  checked={tipoComunidad === 'COMUNIDAD_PUBLICA'}
                  onChange={(e) => setTipoComunidad(e.target.value)}
                  disabled={saving}
                />
                <span>Pública (acceso libre)</span>
              </label>
              <label className="ecm-radio-label">
                <input
                  type="radio"
                  name="tipoComunidad"
                  value="GRUPO_PRIVADO"
                  checked={tipoComunidad === 'GRUPO_PRIVADO'}
                  onChange={(e) => setTipoComunidad(e.target.value)}
                  disabled={saving}
                />
                <span>Privada (requiere solicitud)</span>
              </label>
            </div>
          </div>

          {/* Categorías */}
          <div className="ecm-field">
            <label htmlFor="ecm-categoria-input">Categorías</label>
            <div className="ecm-categoria-input-row">
              <input
                id="ecm-categoria-input"
                type="text"
                value={categoriaInput}
                onChange={(e) => setCategoriaInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    handleAddCategoria();
                  }
                }}
                placeholder="Agregar categoría"
                disabled={saving}
                maxLength={50}
              />
              <button
                type="button"
                className="ecm-btn ecm-btn--secondary ecm-btn-add-categoria"
                onClick={handleAddCategoria}
                disabled={saving}
              >
                +
              </button>
            </div>
            {categoriasLoading ? (
              <p className="ecm-image-help">Cargando categorías...</p>
            ) : (
              <div className="ecm-categorias-lista">
                {visibleExistingCategorias.map((cat) => (
                  <span key={`exist-${cat.id}`} className="ecm-categoria-chip">
                    {cat.nombre}
                    <button
                      type="button"
                      className="ecm-categoria-remove"
                      onClick={() => handleRemoveExistingCategoria(cat.id)}
                      disabled={saving}
                      aria-label={`Eliminar ${cat.nombre}`}
                    >
                      ×
                    </button>
                  </span>
                ))}
                {newCategorias.map((nuevo) => (
                  <span key={`new-${nuevo}`} className="ecm-categoria-chip ecm-categoria-chip--new">
                    {nuevo}
                    <button
                      type="button"
                      className="ecm-categoria-remove"
                      onClick={() => handleRemoveNewCategoria(nuevo)}
                      disabled={saving}
                      aria-label={`Quitar ${nuevo}`}
                    >
                      ×
                    </button>
                  </span>
                ))}
              </div>
            )}
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

import { useCallback, useEffect, useState } from 'react';
import { LuDownload, LuPlus, LuSearch, LuTrash2, LuX } from 'react-icons/lu';
import { apuntesApi } from '../../api/apuntes.api';
import './CommunityApuntesTab.css';

export default function CommunityApuntesTab({ communityId, isAdmin, isMember }) {
  const [apuntes, setApuntes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showUploadForm, setShowUploadForm] = useState(false);
  const [uploadLoading, setUploadLoading] = useState(false);
  const [uploadError, setUploadError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [searchLoading, setSearchLoading] = useState(false);
  const [page, setPage] = useState(0);
  const pageSize = 10;

  // Formulario de subida
  const [newApunte, setNewApunte] = useState({
    titulo: '',
    descripcion: '',
    file: null,
  });

  // Cargar apuntes
  const fetchApuntes = useCallback(async (pageNum = 0, search = '') => {
    try {
      setLoading(true);
      setError(null);

      let response;
      if (search) {
        response = await apuntesApi.buscarApuntes(communityId, search, pageNum, pageSize);
      } else {
        response = await apuntesApi.obtenerApuntes(communityId, pageNum, pageSize);
      }
      setApuntes(response.data.content || []);
    } catch (err) {
      console.error('Error al cargar apuntes:', err);
      setError('No se pudieron cargar los apuntes');
    } finally {
      setLoading(false);
    }
  }, [communityId]);

  useEffect(() => {
    fetchApuntes();
  }, [fetchApuntes]);

  // Búsqueda
  const handleSearch = (e) => {
    const term = e.target.value;
    setSearchTerm(term);
    setPage(0);
    if (term.trim()) {
      setSearchLoading(true);
      fetchApuntes(0, term);
      setSearchLoading(false);
    } else {
      fetchApuntes(0, '');
    }
  };

  // Subir apunte
  const handleUploadChange = (e) => {
    const { name, value, type, files } = e.target;
    if (type === 'file') {
      setNewApunte({ ...newApunte, file: files[0] });
    } else {
      setNewApunte({ ...newApunte, [name]: value });
    }
  };

  const handleSubmitUpload = async (e) => {
    e.preventDefault();
    if (!newApunte.file) {
      setUploadError('Debes seleccionar un archivo');
      return;
    }

    try {
      setUploadLoading(true);
      setUploadError(null);

      const response = await apuntesApi.subirApunte(
        communityId,
        newApunte.titulo || newApunte.file.name,
        newApunte.descripcion,
        newApunte.file
      );

      setApuntes([response.data, ...apuntes]);
      setNewApunte({ titulo: '', descripcion: '', file: null });
      setShowUploadForm(false);
    } catch (err) {
      console.error('Error al subir apunte:', err);
      setUploadError(err.response?.data?.message || 'Error al subir el apunte');
    } finally {
      setUploadLoading(false);
    }
  };

  // Descargar apunte
  const handleDownload = async (apunte) => {
    try {
      const response = await apuntesApi.descargarApunte(communityId, apunte.id);

      const blob = new Blob([response.data], { type: apunte.tipoMime });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = apunte.nombreArchivo;
      link.click();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Error al descargar apunte:', err);
      alert('Error al descargar el apunte');
    }
  };

  // Eliminar apunte
  const handleDelete = async (apunteId) => {
    if (!window.confirm('¿Estás seguro de que quieres eliminar este apunte?')) {
      return;
    }

    try {
      await apuntesApi.eliminarApunte(communityId, apunteId);
      setApuntes(apuntes.filter((a) => a.id !== apunteId));
    } catch (err) {
      console.error('Error al eliminar apunte:', err);
      alert('Error al eliminar el apunte');
    }
  };

  const formatFileSize = (bytes) => {
    if (!bytes) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('es-ES', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  return (
    <div className="ca-container">
      {/* Encabezado con búsqueda y botón de subida */}
      <div className="ca-header">
        <div className="ca-search-box">
          <LuSearch className="ca-search-icon" />
          <input
            type="text"
            placeholder="Buscar apuntes por título..."
            value={searchTerm}
            onChange={handleSearch}
            className="ca-search-input"
          />
        </div>

        {isMember && (
          <button
            className="ca-btn ca-btn-upload"
            onClick={() => setShowUploadForm(!showUploadForm)}
          >
            <LuPlus /> Subir apunte
          </button>
        )}
      </div>

      {/* Formulario de subida */}
      {showUploadForm && isMember && (
        <div className="ca-upload-form-container">
          <form onSubmit={handleSubmitUpload} className="ca-upload-form">
            <h3>Subir nuevo apunte</h3>

            <div className="ca-form-group">
              <label htmlFor="titulo">Título (opcional)</label>
              <input
                id="titulo"
                type="text"
                name="titulo"
                placeholder="Título del apunte"
                value={newApunte.titulo}
                onChange={handleUploadChange}
                maxLength={200}
              />
            </div>

            <div className="ca-form-group">
              <label htmlFor="descripcion">Descripción (opcional)</label>
              <textarea
                id="descripcion"
                name="descripcion"
                placeholder="Describe brevemente el contenido del apunte"
                value={newApunte.descripcion}
                onChange={handleUploadChange}
                maxLength={1000}
                rows={3}
              />
            </div>

            <div className="ca-form-group">
              <label htmlFor="file">Archivo *</label>
              <input
                id="file"
                type="file"
                name="file"
                onChange={handleUploadChange}
                required
                accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.jpg,.jpeg,.png"
              />
              <p className="ca-form-hint">
                Formatos soportados: PDF, DOC, XLS, PPT, TXT, JPG, PNG (máx 100 MB)
              </p>
            </div>

            {uploadError && <p className="ca-error">{uploadError}</p>}

            <div className="ca-form-actions">
              <button type="submit" className="ca-btn ca-btn-primary" disabled={uploadLoading}>
                {uploadLoading ? 'Subiendo...' : 'Subir apunte'}
              </button>
              <button
                type="button"
                className="ca-btn ca-btn-secondary"
                onClick={() => setShowUploadForm(false)}
              >
                Cancelar
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Lista de apuntes */}
      {loading && searchLoading ? (
        <p className="ca-loading">Cargando apuntes...</p>
      ) : error ? (
        <p className="ca-error">{error}</p>
      ) : apuntes.length > 0 ? (
        <div className="ca-apuntes-list">
          {apuntes.map((apunte) => (
            <div key={apunte.id} className="ca-apunte-card">
              <div className="ca-apunte-header">
                <h4 className="ca-apunte-title">{apunte.titulo}</h4>
                <span className="ca-apunte-type">
                  {apunte.tipoMime?.split('/')[1]?.toUpperCase() || 'FILE'}
                </span>
              </div>

              {apunte.descripcion && (
                <p className="ca-apunte-description">{apunte.descripcion}</p>
              )}

              <div className="ca-apunte-meta">
                <span className="ca-apunte-author">
                  Por: <strong>{apunte.usuarioNombre}</strong>
                </span>
                <span className="ca-apunte-date">{formatDate(apunte.createdAt)}</span>
                <span className="ca-apunte-size">{formatFileSize(apunte.tamanioArchivo)}</span>
                <span className="ca-apunte-downloads">
                  {apunte.descargas || 0} descargas
                </span>
              </div>

              <div className="ca-apunte-actions">
                <button
                  className="ca-btn ca-btn-download"
                  onClick={() => handleDownload(apunte)}
                  title="Descargar apunte"
                >
                  <LuDownload /> Descargar
                </button>

                {isAdmin || apunte.usuarioId === parseInt(localStorage.getItem('userId'), 10) ? (
                  <button
                    className="ca-btn ca-btn-delete"
                    onClick={() => handleDelete(apunte.id)}
                    title="Eliminar apunte"
                  >
                    <LuTrash2 />
                  </button>
                ) : null}
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="ca-empty">
          <p>No hay apuntes disponibles en esta comunidad.</p>
          {isMember && (
            <p className="ca-empty-hint">Sé el primero en subir un apunte.</p>
          )}
        </div>
      )}
    </div>
  );
}

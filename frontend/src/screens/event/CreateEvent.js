import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, useSearchParams, useLocation } from 'react-router-dom';
import { LuCalendar, LuSquareCheck, LuMapPin, LuLink, LuArrowLeft, LuUsers, LuEye, LuEyeOff, LuMap, LuMapPinOff, LuPlus } from 'react-icons/lu';
import './CreateEvent.css';
import Header from '../../components/Header/Header';
import { createEvent, getEventById, updateEvent } from '../../api/eventEndpoints';
import { communitiesApi } from '../../api/communities.api';


const CreateEvent = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const communityId = searchParams.get('communityId');

  const [formData, setFormData] = useState({
    nombre: '',
    descripcion: '',
    comentario: '',
    dia: '',
    mes: '',
    anio: '',
    hora: '',
    minuto: '',
    diaFin: '',
    mesFin: '',
    anioFin: '',
    horaFin: '',
    minutoFin: '',
    tipoLocalizacion: 'Presencial',
    direccion: '',
    aforo: '',
    privado: false,
    visibleEnMapa: true,
    ubicacionId: null,
    ubicacionNombre: '',
    ubicacionDireccion: '',
    ubicacionLatitud: null,
    ubicacionLongitud: null
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [validationErrors, setValidationErrors] = useState({});

  const isEdit = id && id !== 'new';
  const currentUserId = localStorage.getItem('userId');

  // Recibir ubicación seleccionada desde CrearUbicacionScreen
  useEffect(() => {
    // Restaurar borrador del formulario PRIMERO
    if (location.state?.eventFormDraft) {
      setFormData(prev => ({ ...prev, ...location.state.eventFormDraft }));
    }
    // Luego aplicar la ubicación nueva (sobrescribe los campos de ubicación del draft)
    if (location.state?.ubicacion) {
      const ub = location.state.ubicacion;
      setFormData(prev => ({
        ...prev,
        ubicacionId: ub.id,
        ubicacionNombre: ub.nombre || '',
        ubicacionDireccion: ub.direccion || '',
        ubicacionLatitud: ub.latitud || null,
        ubicacionLongitud: ub.longitud || null,
        direccion: ub.direccion || ub.nombre || prev.direccion
      }));
    }
  }, [location.state]);

  // Cargar datos del evento si es edición
  useEffect(() => {
    if (isEdit) {
      const fetchEvent = async () => {
        try {
          setLoading(true);
          const data = await getEventById(id);

          // Verificar que el usuario actual es el creador del evento
          if (data.creador && data.creador.id?.toString() !== currentUserId) {
            setError('No tienes permiso para editar este evento. Solo el creador puede editarlo.');
            setLoading(false);
            return;
          }

          const fecha = new Date(data.fechaHora);
          const fechaFin = data.fechaFin ? new Date(data.fechaFin) : null;
          setFormData({
            nombre: data.titulo || '',
            descripcion: data.descripcion || '',
            comentario: data.queLlevar || '',
            dia: String(fecha.getDate()).padStart(2, '0'),
            mes: String(fecha.getMonth() + 1).padStart(2, '0'),
            anio: String(fecha.getFullYear()),
            hora: String(fecha.getHours()).padStart(2, '0'),
            minuto: String(fecha.getMinutes()).padStart(2, '0'),
            diaFin: fechaFin ? String(fechaFin.getDate()).padStart(2, '0') : '',
            mesFin: fechaFin ? String(fechaFin.getMonth() + 1).padStart(2, '0') : '',
            anioFin: fechaFin ? String(fechaFin.getFullYear()) : '',
            horaFin: fechaFin ? String(fechaFin.getHours()).padStart(2, '0') : '',
            minutoFin: fechaFin ? String(fechaFin.getMinutes()).padStart(2, '0') : '',
            tipoLocalizacion: data.esVirtual ? 'Online' : 'Presencial',
            direccion: data.esVirtual ? (data.enlaceVirtual || '') : (data.ubicacion?.nombre || data.ubicacion || ''),
            aforo: data.aforo ? String(data.aforo) : '',
            privado: data.privado || false,
            visibleEnMapa: data.visibleMapa !== undefined ? data.visibleMapa : (data.visibleEnMapa !== undefined ? data.visibleEnMapa : true),
            ubicacionId: data.ubicacion?.id || null,
            ubicacionNombre: data.ubicacion?.nombre || '',
            ubicacionDireccion: data.ubicacion?.direccion || '',
            ubicacionLatitud: data.ubicacion?.latitud || null,
            ubicacionLongitud: data.ubicacion?.longitud || null
          });
        } catch (err) {
          console.error('Error al cargar el evento:', err);
          setError('No se pudo cargar el evento.');
        } finally {
          setLoading(false);
        }
      };
      fetchEvent();
    }
  }, [id, isEdit, currentUserId, navigate]);

  // Verificar que el usuario es miembro de la comunidad al crear
  useEffect(() => {
    if (!isEdit && communityId && currentUserId) {
      const checkMembership = async () => {
        try {
          await communitiesApi.getMyMembership(communityId);
        } catch {
          setError('No puedes crear eventos en una comunidad a la que no perteneces.');
        }
      };
      checkMembership();
    }
  }, [isEdit, communityId, currentUserId]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
    // Limpiar error de validación al cambiar campo
    const updatedErrors = { ...validationErrors };
    if (updatedErrors[name]) {
      updatedErrors[name] = null;
    }
    // Limpiar error de fechaFin al cambiar cualquier campo de fecha/hora fin
    if (['diaFin', 'mesFin', 'anioFin', 'horaFin', 'minutoFin'].includes(name)) {
      updatedErrors.fechaFin = null;
    }
    setValidationErrors(updatedErrors);
  };

  const validateForm = () => {
    const errors = {};
    if (!formData.nombre.trim()) errors.nombre = 'El título es obligatorio';
    if (formData.nombre.trim().length < 3) errors.nombre = 'El título debe tener al menos 3 caracteres';
    if (formData.nombre.trim().length > 200) errors.nombre = 'El título no puede superar 200 caracteres';
    if (!formData.dia || !formData.mes || !formData.anio) errors.fecha = 'La fecha es obligatoria';
    if (!formData.hora || !formData.minuto) errors.hora = 'La hora es obligatoria';
    if (!formData.aforo || parseInt(formData.aforo) < 1) errors.aforo = 'El aforo debe ser al menos 1';
    if (parseInt(formData.aforo) > 500) errors.aforo = 'El aforo no puede superar 500';

    // Validar que la fecha de inicio no sea posterior a la fecha de fin
    const tieneAlgunCampoFin = formData.diaFin || formData.mesFin || formData.anioFin || formData.horaFin || formData.minutoFin;
    if (tieneAlgunCampoFin) {
      if (!formData.diaFin || !formData.mesFin || !formData.anioFin) {
        errors.fechaFin = 'Completa la fecha de fin (día, mes y año)';
      } else {
        const hFin = formData.horaFin ? parseInt(formData.horaFin) : 0;
        const mFin = formData.minutoFin ? parseInt(formData.minutoFin) : 0;
        const fechaInicio = new Date(
          parseInt(formData.anio), parseInt(formData.mes) - 1, parseInt(formData.dia),
          parseInt(formData.hora), parseInt(formData.minuto)
        );
        const fechaFin = new Date(
          parseInt(formData.anioFin), parseInt(formData.mesFin) - 1, parseInt(formData.diaFin),
          hFin, mFin
        );
        if (fechaInicio >= fechaFin) {
          errors.fechaFin = 'La fecha de inicio debe ser anterior a la fecha de fin';
        }
      }
    }

    if (formData.tipoLocalizacion === 'Online' && !formData.direccion.trim()) {
      errors.direccion = 'El enlace virtual es obligatorio';
    }
    if (formData.tipoLocalizacion === 'Presencial' && !formData.ubicacionId) {
      errors.ubicacion = 'Debes seleccionar una ubicación para el evento presencial';
    }
    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const pad = (val) => String(val).padStart(2, '0');

  const buildEventPayload = () => {
    const fechaHora = `${formData.anio}-${pad(formData.mes)}-${pad(formData.dia)}T${pad(formData.hora)}:${pad(formData.minuto)}:00`;
    const esVirtual = formData.tipoLocalizacion === 'Online';

    const payload = {
      titulo: formData.nombre,
      descripcion: formData.descripcion,
      fechaInicio: fechaHora,
      fechaHora,
      aforo: parseInt(formData.aforo) || 50,
      queLlevar: formData.comentario || undefined,
      esVirtual,
      privado: formData.privado,
      visibleEnMapa: formData.visibleEnMapa
    };

    // Fecha fin si se proporcionó
    if (formData.diaFin && formData.mesFin && formData.anioFin) {
      const hFin = formData.horaFin || '0';
      const mFin = formData.minutoFin || '0';
      payload.fechaFin = `${formData.anioFin}-${pad(formData.mesFin)}-${pad(formData.diaFin)}T${pad(hFin)}:${pad(mFin)}:00`;
    }

    if (!esVirtual) {
      if (formData.ubicacionId) {
        payload.ubicacionId = formData.ubicacionId;
      }
    } else {
      payload.enlaceVirtual = formData.direccion;
    }

    return payload;
  };

  const handleSubmit = async () => {
    if (!validateForm()) return;

    try {
      setLoading(true);
      setError(null);
      const payload = buildEventPayload();

      if (isEdit) {
        await updateEvent(id, payload);
      } else {
        if (!communityId) {
          setError('No se ha especificado la comunidad para crear el evento.');
          return;
        }
        await createEvent(communityId, payload);
      }

      if (communityId) {
        navigate(`/comunidades/${communityId}`);
      } else {
        navigate(-1);
      }
    } catch (err) {
      console.error('Error al guardar el evento:', err);
      setError(err.response?.data?.message || 'Error al guardar el evento. Inténtalo de nuevo.');
    } finally {
      setLoading(false);
    }
  };

  const handleSaveDraft = () => {
    // Guardar borrador en localStorage
    localStorage.setItem('eventDraft', JSON.stringify(formData));
    alert('Borrador guardado correctamente.');
  };


  return (
    <div className="page-container">
      <Header page={'eventos'} />

      <div className="content-wrapper">
        <div className="header-section">
          <p className="header-text">
            Configura tu evento en pocos pasos y aprended junto en cualquier momento
          </p>
          <div className="header-line"></div>
          <h1 className="header-title">
            {isEdit ? 'Editar' : 'Crear'}<br />Evento
          </h1>
        </div>

        {error && (
          <div className="error-message" style={{ color: 'red', padding: '10px', margin: '10px 0', background: '#ffe0e0', borderRadius: '8px' }}>
            {error}
          </div>
        )}

        {/* Si es error de permisos, no mostrar el formulario */}
        {error && (error.includes('No tienes permiso') || error.includes('no perteneces')) ? (
          <div className="actions-container" style={{ marginTop: '2rem' }}>
            <button className="back-link" onClick={() => navigate(-1)}>
              <LuArrowLeft /> Volver
            </button>
          </div>
        ) : (
        <>
        <div className="dotted-divider"></div>

        <div className="form-grid">
          <div className="left-column">
            <div className="community-info">
              <div className="community-image">
                <img src="https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&w=150&q=80" alt="Evento" />
              </div>
              <h3 className="community-title">Evento de comunidad</h3>
            </div>

            <div className="input-group">
              <label className="input-label">Fecha de inicio *</label>
              <div className="input-row">
                <input type="text" name="dia" placeholder="DD" value={formData.dia} onChange={handleChange} className={`input-box input-small ${validationErrors.fecha ? 'input-error' : ''}`} maxLength={2} />
                <input type="text" name="mes" placeholder="MM" value={formData.mes} onChange={handleChange} className={`input-box input-small ${validationErrors.fecha ? 'input-error' : ''}`} maxLength={2} />
                <input type="text" name="anio" placeholder="YYYY" value={formData.anio} onChange={handleChange} className={`input-box input-medium ${validationErrors.fecha ? 'input-error' : ''}`} maxLength={4} />
                <LuCalendar className="input-icon" />
              </div>
              {validationErrors.fecha && <span className="field-error">{validationErrors.fecha}</span>}
            </div>

            <div className="input-group">
              <label className="input-label">Hora de inicio *</label>
              <div className="input-row">
                <input type="text" name="hora" placeholder="HH" value={formData.hora} onChange={handleChange} className={`input-box input-medium ${validationErrors.hora ? 'input-error' : ''}`} maxLength={2} />
                <input type="text" name="minuto" placeholder="mm" value={formData.minuto} onChange={handleChange} className={`input-box input-medium ${validationErrors.hora ? 'input-error' : ''}`} maxLength={2} />
                <LuSquareCheck className="input-icon" />
              </div>
              {validationErrors.hora && <span className="field-error">{validationErrors.hora}</span>}
            </div>

            <div className="input-group">
              <label className="input-label">Fecha de fin (opcional)</label>
              <div className="input-row">
                <input type="text" name="diaFin" placeholder="DD" value={formData.diaFin} onChange={handleChange} className={`input-box input-small ${validationErrors.fechaFin ? 'input-error' : ''}`} maxLength={2} />
                <input type="text" name="mesFin" placeholder="MM" value={formData.mesFin} onChange={handleChange} className={`input-box input-small ${validationErrors.fechaFin ? 'input-error' : ''}`} maxLength={2} />
                <input type="text" name="anioFin" placeholder="YYYY" value={formData.anioFin} onChange={handleChange} className={`input-box input-medium ${validationErrors.fechaFin ? 'input-error' : ''}`} maxLength={4} />
                <LuCalendar className="input-icon" />
              </div>
              {validationErrors.fechaFin && <span className="field-error">{validationErrors.fechaFin}</span>}
            </div>

            <div className="input-group">
              <label className="input-label">Hora de fin (opcional)</label>
              <div className="input-row">
                <input type="text" name="horaFin" placeholder="HH" value={formData.horaFin} onChange={handleChange} className={`input-box input-medium ${validationErrors.fechaFin ? 'input-error' : ''}`} maxLength={2} />
                <input type="text" name="minutoFin" placeholder="mm" value={formData.minutoFin} onChange={handleChange} className={`input-box input-medium ${validationErrors.fechaFin ? 'input-error' : ''}`} maxLength={2} />
                <LuSquareCheck className="input-icon" />
              </div>
            </div>

            <div className="input-group">
              <label className="input-label">Aforo máximo *</label>
              <div className="input-row">
                <input type="number" name="aforo" placeholder="Ej. 30" value={formData.aforo} onChange={handleChange} className={`input-box input-medium ${validationErrors.aforo ? 'input-error' : ''}`} min={1} max={500} />
                <LuUsers className="input-icon" />
              </div>
              {validationErrors.aforo && <span className="field-error">{validationErrors.aforo}</span>}
            </div>
          </div>

          <div className="right-column">
            <div className="input-group">
              <label className="input-label">Nombre del Evento *</label>
              <input type="text" name="nombre" placeholder="Ej. Clase de NodeJS + Sequelize" value={formData.nombre} onChange={handleChange} className={`input-box input-large ${validationErrors.nombre ? 'input-error' : ''}`} />
              {validationErrors.nombre && <span className="field-error">{validationErrors.nombre}</span>}
            </div>

            <div className="input-group">
              <label className="input-label">Descripción</label>
              <textarea name="descripcion" placeholder="¿De qué trata este evento? Comparte los objetivos" value={formData.descripcion} onChange={handleChange} rows="3" className="input-box input-large"></textarea>
            </div>

            <div className="input-group">
              <label className="input-label">Materiales necesarios</label>
              <input type="text" name="comentario" placeholder="Ej. Libro de texto, ordenador portátil, calculadora científica" value={formData.comentario} onChange={handleChange} className="input-box input-large" />
              <span className="field-hint">Indica los materiales que los asistentes deben llevar al evento</span>
            </div>

            <div className="input-group">
              <label className="input-label">Visibilidad del evento</label>
              <div className="toggle-container">
                <button
                  type="button"
                  onClick={() => setFormData({ ...formData, privado: false })}
                  className={`toggle-btn ${!formData.privado ? 'active' : 'inactive'}`}
                >
                  <LuEye className="toggle-icon" /> Público
                </button>
                <button
                  type="button"
                  onClick={() => setFormData({ ...formData, privado: true })}
                  className={`toggle-btn ${formData.privado ? 'active' : 'inactive'}`}
                >
                  <LuEyeOff className="toggle-icon" /> Privado
                </button>
              </div>
              <span className="field-hint">
                {formData.privado 
                  ? 'Solo visible para miembros de la comunidad' 
                  : 'Visible para todos los usuarios de la plataforma'}
              </span>
            </div>

            <div className="input-group">
              <label className="input-label">Mostrar en mapa</label>
              <div className="toggle-container">
                <button
                  type="button"
                  onClick={() => setFormData({ ...formData, visibleEnMapa: true })}
                  className={`toggle-btn ${formData.visibleEnMapa ? 'active' : 'inactive'}`}
                >
                  <LuMap className="toggle-icon" /> Sí
                </button>
                <button
                  type="button"
                  onClick={() => setFormData({ ...formData, visibleEnMapa: false })}
                  className={`toggle-btn ${!formData.visibleEnMapa ? 'active' : 'inactive'}`}
                >
                  <LuMapPinOff className="toggle-icon" /> No
                </button>
              </div>
            </div>
          </div>
        </div>

        <div className="dotted-divider"></div>

        <div className="form-grid">
          <div className="left-column">
            <h3 className="location-title">Localización</h3>
            <p className="location-desc">Define dónde ocurrirá el evento.</p>

            <div className="toggle-container">
              <button
                onClick={() => setFormData({ ...formData, tipoLocalizacion: 'Presencial' })}
                className={`toggle-btn ${formData.tipoLocalizacion === 'Presencial' ? 'active' : 'inactive'}`}
              >
                <LuMapPin className="toggle-icon" /> Presencial
              </button>
              <button
                onClick={() => setFormData({ ...formData, tipoLocalizacion: 'Online' })}
                className={`toggle-btn ${formData.tipoLocalizacion === 'Online' ? 'active' : 'inactive'}`}
              >
                <LuLink className="toggle-icon" /> Online
              </button>
            </div>
          </div>

          <div className="right-column">
            {formData.tipoLocalizacion === 'Online' && (
              <>
                <label className="input-label">Enlace virtual *</label>
                <textarea name="direccion" placeholder="Ej. https://meet.google.com/abc-defg-hij" value={formData.direccion} onChange={handleChange} rows="3" className={`input-box input-large ${validationErrors.direccion ? 'input-error' : ''}`}></textarea>
                {validationErrors.direccion && <span className="field-error">{validationErrors.direccion}</span>}
              </>
            )}

            {formData.tipoLocalizacion === 'Presencial' && (
              <div style={{ marginTop: '12px' }}>
                {formData.ubicacionId ? (
                  <div style={{
                    background: '#f0faf5',
                    border: '1px solid #b7eb8f',
                    borderRadius: 10,
                    padding: '16px',
                    marginBottom: 4
                  }}>
                    <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 12 }}>
                      <div style={{ flex: 1 }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 8 }}>
                          <LuMapPin style={{ color: '#52c41a', fontSize: '1.2rem' }} />
                          <span style={{ fontWeight: 700, fontSize: '1.05rem', color: '#222' }}>
                            {formData.ubicacionNombre || 'Ubicación seleccionada'}
                          </span>
                        </div>
                        {formData.ubicacionDireccion && (
                          <p style={{ margin: '0 0 6px 0', color: '#555', fontSize: '0.9rem', lineHeight: 1.4 }}>
                            {formData.ubicacionDireccion}
                          </p>
                        )}
                        {formData.ubicacionLatitud && formData.ubicacionLongitud && (
                          <p style={{ margin: 0, color: '#888', fontSize: '0.8rem' }}>
                            📍 {Number(formData.ubicacionLatitud).toFixed(5)}, {Number(formData.ubicacionLongitud).toFixed(5)}
                          </p>
                        )}
                      </div>
                      <button
                        type="button"
                        className="btn btn-outline"
                        style={{ fontSize: '0.85rem', padding: '6px 14px', whiteSpace: 'nowrap' }}
                        onClick={() => {
                          const returnPath = isEdit ? `/create-event/${id}` : `/create-event/new`;
                          const returnQuery = communityId ? `?communityId=${communityId}` : '';
                          navigate('/crear-ubicacion?returnTo=' + encodeURIComponent(returnPath + returnQuery), {
                            state: { eventFormDraft: formData }
                          });
                        }}
                      >
                        Cambiar
                      </button>
                    </div>
                  </div>
                ) : (
                  <>
                    <button
                      type="button"
                      className="btn btn-outline"
                      style={{ display: 'flex', alignItems: 'center', gap: 6 }}
                      onClick={() => {
                        const returnPath = isEdit ? `/create-event/${id}` : `/create-event/new`;
                        const returnQuery = communityId ? `?communityId=${communityId}` : '';
                        navigate('/crear-ubicacion?returnTo=' + encodeURIComponent(returnPath + returnQuery), {
                          state: { eventFormDraft: formData }
                        });
                      }}
                    >
                      <LuPlus /> Añadir ubicación del mapa
                    </button>
                    {validationErrors.ubicacion && <span className="field-error">{validationErrors.ubicacion}</span>}
                  </>
                )}
                <span className="field-hint" style={{ marginTop: 4, display: 'block' }}>
                  Selecciona o crea una ubicación para que aparezca en el mapa de eventos
                </span>
              </div>
            )}
          </div>
        </div>

        <div className="actions-container">
          <div className="buttons-row">
            <button className="btn btn-outline" onClick={handleSaveDraft} disabled={loading}>
              Guardar Borrador
            </button>
            <button className="btn btn-primary" onClick={handleSubmit} disabled={loading}>
              {loading ? 'Guardando...' : (isEdit ? 'Actualizar Evento' : 'Crear Evento')}
            </button>
          </div>

          <button className="back-link" onClick={() => navigate(-1)}>
            <LuArrowLeft /> Volver al Dashboard
          </button>
        </div>
        </>
        )}
      </div>
    </div>
  );
};

export default CreateEvent;
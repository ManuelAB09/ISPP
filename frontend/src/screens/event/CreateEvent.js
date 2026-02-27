import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { LuCalendar, LuSquareCheck, LuMapPin, LuLink, LuArrowLeft, LuUsers, LuEye, LuEyeOff, LuMap, LuMapPinOff } from 'react-icons/lu';
import './CreateEvent.css';
import Header from '../../components/Header/Header';
import { createEvent, getEventById, updateEvent } from '../../api/eventEndpoints';
import { communitiesApi } from '../../api/communities.api';


const CreateEvent = () => {
  const { id } = useParams();
  const navigate = useNavigate();
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
    visibleEnMapa: true
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [validationErrors, setValidationErrors] = useState({});

  const isEdit = id && id !== 'new';
  const currentUserId = localStorage.getItem('userId');

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
            visibleEnMapa: data.visibleMapa !== undefined ? data.visibleMapa : (data.visibleEnMapa !== undefined ? data.visibleEnMapa : true)
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
    if (validationErrors[name]) {
      setValidationErrors({ ...validationErrors, [name]: null });
    }
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
    if (!formData.direccion.trim()) {
      errors.direccion = formData.tipoLocalizacion === 'Online' 
        ? 'El enlace virtual es obligatorio' 
        : 'La dirección es obligatoria';
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
    if (formData.diaFin && formData.mesFin && formData.anioFin && formData.horaFin && formData.minutoFin) {
      payload.fechaFin = `${formData.anioFin}-${pad(formData.mesFin)}-${pad(formData.diaFin)}T${pad(formData.horaFin)}:${pad(formData.minutoFin)}:00`;
    }

    if (!esVirtual) {
      payload.ubicacion = formData.direccion;
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
                <input type="text" name="diaFin" placeholder="DD" value={formData.diaFin} onChange={handleChange} className="input-box input-small" maxLength={2} />
                <input type="text" name="mesFin" placeholder="MM" value={formData.mesFin} onChange={handleChange} className="input-box input-small" maxLength={2} />
                <input type="text" name="anioFin" placeholder="YYYY" value={formData.anioFin} onChange={handleChange} className="input-box input-medium" maxLength={4} />
                <LuCalendar className="input-icon" />
              </div>
            </div>

            <div className="input-group">
              <label className="input-label">Hora de fin (opcional)</label>
              <div className="input-row">
                <input type="text" name="horaFin" placeholder="HH" value={formData.horaFin} onChange={handleChange} className="input-box input-medium" maxLength={2} />
                <input type="text" name="minutoFin" placeholder="mm" value={formData.minutoFin} onChange={handleChange} className="input-box input-medium" maxLength={2} />
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
            <label className="input-label">{formData.tipoLocalizacion === 'Online' ? 'Enlace virtual' : 'Dirección o lugar'} *</label>
            <textarea name="direccion" placeholder={formData.tipoLocalizacion === 'Online' ? 'Ej. https://meet.google.com/abc-defg-hij' : '¿Dónde ocurrirá la reunión? Ej. Biblioteca de Facultad de Derecho (Sevilla)'} value={formData.direccion} onChange={handleChange} rows="3" className={`input-box input-large ${validationErrors.direccion ? 'input-error' : ''}`}></textarea>
            {validationErrors.direccion && <span className="field-error">{validationErrors.direccion}</span>}
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
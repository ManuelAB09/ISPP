import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, useSearchParams, useLocation } from 'react-router-dom';
import { LuCalendar, LuSquareCheck, LuMapPin, LuLink, LuArrowLeft, LuUsers, LuEye, LuEyeOff, LuMap, LuMapPinOff, LuPlus, LuVideo } from 'react-icons/lu';
import './CrearEvento.css';
import Header from '../../components/Header/Header';
import { createEvent, getEventById, updateEvent } from '../../api/eventEndpoints';
import { communitiesApi } from '../../api/communities.api';
import { canCreateCommunityEvent, getCommunityRoleLabel, normalizeCommunityRole } from '../../utils/communityRoles';

const DATE_TIME_FIELD_MAX_LENGTH = {
  dia: 2,
  mes: 2,
  anio: 4,
  hora: 2,
  minuto: 2,
  diaFin: 2,
  mesFin: 2,
  anioFin: 4,
  horaFin: 2,
  minutoFin: 2,
};

const DATE_TIME_FIELDS = new Set(Object.keys(DATE_TIME_FIELD_MAX_LENGTH));
const AFORO_MAX_DIGITS = 3;

const parseMaterials = (value) => {
  if (Array.isArray(value)) {
    return value
      .map((m) => String(m || '').trim())
      .filter(Boolean);
  }

  if (typeof value !== 'string') {
    return [];
  }

  return value
    .split(',')
    .map((m) => m.trim())
    .filter(Boolean);
};


const CrearEvento = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const rawCommunityId = searchParams.get('communityId') === 'null' ? null : searchParams.get('communityId');
  const [selectedCommunityId, setSelectedCommunityId] = useState(rawCommunityId || '');
  const [myCommunities, setMyCommunities] = useState([]);

  const [formData, setFormData] = useState({
    nombre: '',
    descripcion: '',
    materiales: [],
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
    zoomDuration: 60,
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
  const [eventStarted, setEventStarted] = useState(false);
  const [validationErrors, setValidationErrors] = useState({});
  const [materialInput, setMaterialInput] = useState('');
  const [selectedCommunityRole, setSelectedCommunityRole] = useState(null);

  const isEdit = id && id !== 'new';
  const currentUserId = localStorage.getItem('userId');

  useEffect(() => {
    if (location.state?.eventFormDraft) {
      setFormData(prev => {
        const draft = { ...prev, ...location.state.eventFormDraft };
        return {
          ...draft,
          materiales: parseMaterials(draft.materiales || draft.comentario || draft.queLlevar)
        };
      });
    }

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

  useEffect(() => {
    if (!isEdit && currentUserId) {
      const fetchMyCommunities = async () => {
        try {
          const data = await communitiesApi.listMine();
          const communitiesList = Array.isArray(data) ? data : (data.content || []);
          const creatableCommunities = communitiesList.filter((community) => {
            if (!community?.miRol) return true;
            return canCreateCommunityEvent(community.miRol);
          });

          setMyCommunities(creatableCommunities);
          if (!selectedCommunityId && creatableCommunities.length === 1) {
            setSelectedCommunityId(creatableCommunities[0].id.toString());
          }

          if (!selectedCommunityId && communitiesList.length > 0 && creatableCommunities.length === 0) {
            setError('Solo puedes crear eventos en comunidades donde seas administrador o profesor.');
          }
        } catch (err) {
          console.error("Error fetching user's communities:", err);
        }
      };
      fetchMyCommunities();
    }
  }, [isEdit, currentUserId, selectedCommunityId]);

  useEffect(() => {
    if (isEdit) {
      const fetchEvent = async () => {
        try {
          setLoading(true);
          const data = await getEventById(id);

          let canEdit = data.creador && data.creador.id?.toString() === currentUserId;

          if (data.comunidad?.id) {
            try {
              const membership = await communitiesApi.getMyMembership(data.comunidad.id);
              const normalizedRole = normalizeCommunityRole(membership?.rol);
              setSelectedCommunityRole(normalizedRole || null);
              canEdit = canCreateCommunityEvent(normalizedRole);
            } catch {
              canEdit = false;
            }
          }

          if (!canEdit) {
            setError(data.comunidad?.id
              ? 'No tienes permiso para editar este evento. Solo un administrador o un profesor de la comunidad pueden hacerlo.'
              : 'No tienes permiso para editar este evento. Solo el creador puede hacerlo.');
            setLoading(false);
            return;
          }

          if (data.comunidad?.id) {
            setSelectedCommunityId(data.comunidad.id.toString());
          }

          const started = data.fechaHora ? new Date(data.fechaHora).getTime() <= Date.now() : false;
          setEventStarted(started);

          const fecha = new Date(data.fechaHora);
          const fechaFin = data.fechaFin ? new Date(data.fechaFin) : null;
          setFormData({
            nombre: data.titulo || '',
            descripcion: data.descripcion || '',
            materiales: parseMaterials(data.queLlevar),
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
            direccion: data.esVirtual ? '' : (data.ubicacion?.nombre || data.ubicacion || ''),
            zoomDuration: 60,
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

  useEffect(() => {
    if (!isEdit && selectedCommunityId && currentUserId) {
      const checkMembership = async () => {
        try {
          const membership = await communitiesApi.getMyMembership(selectedCommunityId);
          const normalizedRole = normalizeCommunityRole(membership?.rol);
          setSelectedCommunityRole(normalizedRole || null);

          if (!canCreateCommunityEvent(normalizedRole)) {
            const roleLabel = getCommunityRoleLabel(normalizedRole);
            setError(`No puedes crear eventos con rol ${roleLabel.toLowerCase()}. Solo administradores y profesores pueden hacerlo.`);
            return;
          }

          setError(null);
        } catch {
          setSelectedCommunityRole(null);
          setError('No puedes crear eventos en una comunidad a la que no perteneces.');
        }
      };
      checkMembership();
    }
  }, [isEdit, selectedCommunityId, currentUserId]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    let parsedValue = value;

    if (DATE_TIME_FIELDS.has(name)) {
      parsedValue = value.replace(/\D/g, '');
      parsedValue = parsedValue.slice(0, DATE_TIME_FIELD_MAX_LENGTH[name]);
    }

    if (name === 'aforo') {
      parsedValue = value.replace(/\D/g, '').slice(0, AFORO_MAX_DIGITS);
    }

    setFormData({ ...formData, [name]: parsedValue });
    const updatedErrors = { ...validationErrors };
    if (updatedErrors[name]) {
      updatedErrors[name] = null;
    }
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
    if (parseInt(formData.aforo) > 999) errors.aforo = 'El aforo no puede superar 999';

    if (!errors.fecha && !errors.hora) {
      const fechaInicio = new Date(
        parseInt(formData.anio),
        parseInt(formData.mes) - 1,
        parseInt(formData.dia),
        parseInt(formData.hora),
        parseInt(formData.minuto)
      );

      if (!Number.isNaN(fechaInicio.getTime()) && fechaInicio.getTime() < Date.now()) {
        errors.hora = 'La fecha y hora de inicio no puede ser anterior al momento actual';
      }
    }

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

    if (formData.tipoLocalizacion === 'Online') {
      const dur = parseInt(formData.zoomDuration);
      if (!dur || dur < 5 || dur > 480) {
        errors.zoomDuration = 'La duración debe ser entre 5 y 480 minutos';
      }
    }
    if (formData.tipoLocalizacion === 'Presencial' && !formData.ubicacionId) {
      errors.ubicacion = 'Debes seleccionar una ubicación para el evento presencial';
    }
    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const pad = (val) => String(val).padStart(2, '0');

  const handleNumericKeyDown = (e) => {
    const allowedKeys = ['Backspace', 'Delete', 'ArrowLeft', 'ArrowRight', 'Tab', 'Home', 'End'];
    if (allowedKeys.includes(e.key)) {
      return;
    }

    if (!/^\d$/.test(e.key)) {
      e.preventDefault();
    }
  };

  const handleAddMaterial = () => {
    const newMaterial = materialInput.trim();
    if (!newMaterial) {
      return;
    }

    const alreadyExists = formData.materiales.some(
      (item) => item.toLowerCase() === newMaterial.toLowerCase()
    );

    if (!alreadyExists) {
      setFormData(prev => ({ ...prev, materiales: [...prev.materiales, newMaterial] }));
    }

    setMaterialInput('');
  };

  const handleRemoveMaterial = (indexToRemove) => {
    setFormData(prev => ({
      ...prev,
      materiales: prev.materiales.filter((_, index) => index !== indexToRemove)
    }));
  };

  const handleMaterialKeyDown = (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleAddMaterial();
    }
  };

  const buildEventPayload = () => {
    const fechaHora = `${formData.anio}-${pad(formData.mes)}-${pad(formData.dia)}T${pad(formData.hora)}:${pad(formData.minuto)}:00`;
    const esVirtual = formData.tipoLocalizacion === 'Online';

    const payload = {
      titulo: formData.nombre,
      descripcion: formData.descripcion,
      fechaInicio: fechaHora,
      fechaHora,
      aforo: parseInt(formData.aforo) || 50,
      queLlevar: formData.materiales.length > 0 ? formData.materiales.join(', ') : undefined,
      esVirtual,
      privado: formData.privado,
      visibleEnMapa: formData.visibleEnMapa
    };

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
      payload.zoomDuration = parseInt(formData.zoomDuration) || 60;
    }

    if (!isEdit && selectedCommunityId) {
      payload.communityId = Number(selectedCommunityId);
    }

    return payload;
  };

  const handleSubmit = async () => {
    if (!validateForm()) return;

    if (isEdit && eventStarted) {
      setError('No puedes actualizar un evento que ya ha comenzado.');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const payload = buildEventPayload();

      if (isEdit) {
        await updateEvent(id, payload);
      } else {
        if (!selectedCommunityId) {
          setError('No se ha especificado la comunidad para crear el evento.');
          return;
        }
        if (!canCreateCommunityEvent(selectedCommunityRole)) {
          setError('Solo administradores y profesores pueden crear eventos en esta comunidad.');
          return;
        }
        await createEvent(selectedCommunityId, payload);
      }

      if (selectedCommunityId) {
        navigate(`/comunidades/${selectedCommunityId}`);
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

        {isEdit && eventStarted && (
          <div className="error-message" style={{ color: '#7a3f00', padding: '10px', margin: '10px 0', background: '#fff2e8', borderRadius: '8px' }}>
            Este evento ya ha comenzado. No se puede actualizar.
          </div>
        )}

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
              <div style={{ display: 'flex', flexDirection: 'column', width: '100%' }}>
                <h3 className="community-title">Evento de comunidad</h3>
                {!isEdit && myCommunities.length > 0 && (
                  <select
                    className="input-box" 
                    style={{ marginTop: '8px', padding: '8px' }}
                    value={selectedCommunityId} 
                    onChange={(e) => setSelectedCommunityId(e.target.value)}
                  >
                    <option value="" disabled>Selecciona una comunidad...</option>
                    {myCommunities.map(c => (
                      <option key={c.id} value={c.id}>
                        {c.nombre}{c.miRol ? ` · ${getCommunityRoleLabel(c.miRol)}` : ''}
                      </option>
                    ))}
                  </select>
                )}
                {!isEdit && selectedCommunityRole && (
                  <span className="field-hint" style={{ marginTop: '8px' }}>
                    Rol detectado en esta comunidad: {getCommunityRoleLabel(selectedCommunityRole)}
                  </span>
                )}
              </div>
            </div>

            <div className="input-group">
              <label className="input-label">Fecha de inicio *</label>
              <div className="input-row">
                <input type="text" name="dia" placeholder="DD" value={formData.dia} onChange={handleChange} onKeyDown={handleNumericKeyDown} inputMode="numeric" pattern="\d*" className={`input-box input-small ${validationErrors.fecha ? 'input-error' : ''}`} maxLength={2} />
                <input type="text" name="mes" placeholder="MM" value={formData.mes} onChange={handleChange} onKeyDown={handleNumericKeyDown} inputMode="numeric" pattern="\d*" className={`input-box input-small ${validationErrors.fecha ? 'input-error' : ''}`} maxLength={2} />
                <input type="text" name="anio" placeholder="YYYY" value={formData.anio} onChange={handleChange} onKeyDown={handleNumericKeyDown} inputMode="numeric" pattern="\d*" className={`input-box input-medium ${validationErrors.fecha ? 'input-error' : ''}`} maxLength={4} />
                <LuCalendar className="input-icon" />
              </div>
              {validationErrors.fecha && <span className="field-error">{validationErrors.fecha}</span>}
            </div>

            <div className="input-group">
              <label className="input-label">Hora de inicio *</label>
              <div className="input-row">
                <input type="text" name="hora" placeholder="HH" value={formData.hora} onChange={handleChange} onKeyDown={handleNumericKeyDown} inputMode="numeric" pattern="\d*" className={`input-box input-medium ${validationErrors.hora ? 'input-error' : ''}`} maxLength={2} />
                <input type="text" name="minuto" placeholder="mm" value={formData.minuto} onChange={handleChange} onKeyDown={handleNumericKeyDown} inputMode="numeric" pattern="\d*" className={`input-box input-medium ${validationErrors.hora ? 'input-error' : ''}`} maxLength={2} />
                <LuSquareCheck className="input-icon" />
              </div>
              {validationErrors.hora && <span className="field-error">{validationErrors.hora}</span>}
            </div>

            <div className="input-group">
              <label className="input-label">Fecha de fin (opcional)</label>
              <div className="input-row">
                <input type="text" name="diaFin" placeholder="DD" value={formData.diaFin} onChange={handleChange} onKeyDown={handleNumericKeyDown} inputMode="numeric" pattern="\d*" className={`input-box input-small ${validationErrors.fechaFin ? 'input-error' : ''}`} maxLength={2} />
                <input type="text" name="mesFin" placeholder="MM" value={formData.mesFin} onChange={handleChange} onKeyDown={handleNumericKeyDown} inputMode="numeric" pattern="\d*" className={`input-box input-small ${validationErrors.fechaFin ? 'input-error' : ''}`} maxLength={2} />
                <input type="text" name="anioFin" placeholder="YYYY" value={formData.anioFin} onChange={handleChange} onKeyDown={handleNumericKeyDown} inputMode="numeric" pattern="\d*" className={`input-box input-medium ${validationErrors.fechaFin ? 'input-error' : ''}`} maxLength={4} />
                <LuCalendar className="input-icon" />
              </div>
              {validationErrors.fechaFin && <span className="field-error">{validationErrors.fechaFin}</span>}
            </div>

            <div className="input-group">
              <label className="input-label">Hora de fin (opcional)</label>
              <div className="input-row">
                <input type="text" name="horaFin" placeholder="HH" value={formData.horaFin} onChange={handleChange} onKeyDown={handleNumericKeyDown} inputMode="numeric" pattern="\d*" className={`input-box input-medium ${validationErrors.fechaFin ? 'input-error' : ''}`} maxLength={2} />
                <input type="text" name="minutoFin" placeholder="mm" value={formData.minutoFin} onChange={handleChange} onKeyDown={handleNumericKeyDown} inputMode="numeric" pattern="\d*" className={`input-box input-medium ${validationErrors.fechaFin ? 'input-error' : ''}`} maxLength={2} />
                <LuSquareCheck className="input-icon" />
              </div>
            </div>

            <div className="input-group">
              <label className="input-label">Aforo máximo *</label>
              <div className="input-row">
                <input type="text" name="aforo" placeholder="Ej. 30" value={formData.aforo} onChange={handleChange} onKeyDown={handleNumericKeyDown} inputMode="numeric" pattern="\d*" className={`input-box input-medium ${validationErrors.aforo ? 'input-error' : ''}`} min={1} max={999} maxLength={AFORO_MAX_DIGITS} />
                <LuUsers className="input-icon" />
              </div>
              {validationErrors.aforo && <span className="field-error">{validationErrors.aforo}</span>}
              <span className="field-hint">Solo números (máximo 3 cifras).</span>
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
              <div className="materials-input-row">
                <input
                  type="text"
                  placeholder="Ej. Libro de texto"
                  value={materialInput}
                  onChange={(e) => setMaterialInput(e.target.value)}
                  onKeyDown={handleMaterialKeyDown}
                  className="input-box input-large"
                />
                <button type="button" className="btn-material-add" onClick={handleAddMaterial}>
                  Añadir
                </button>
              </div>
              {formData.materiales.length > 0 && (
                <div className="materials-list">
                  {formData.materiales.map((material, index) => (
                    <span key={`${material}-${index}`} className="material-chip">
                      {material}
                      <button
                        type="button"
                        className="material-chip-remove"
                        onClick={() => handleRemoveMaterial(index)}
                        aria-label={`Eliminar ${material}`}
                      >
                        ×
                      </button>
                    </span>
                  ))}
                </div>
              )}
              <span className="field-hint">Escribe un material y pulsa "Añadir" o Enter.</span>
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
              <div style={{ marginTop: '12px' }}>
                <div style={{
                  background: '#f0f7ff',
                  border: '1px solid #91caff',
                  borderRadius: 10,
                  padding: '16px',
                  marginBottom: 4
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
                    <LuVideo style={{ color: '#1890ff', fontSize: '1.3rem' }} />
                    <span style={{ fontWeight: 700, fontSize: '1.05rem', color: '#222' }}>
                      Reunión por Zoom
                    </span>
                  </div>
                  <p style={{ margin: '0 0 12px 0', color: '#555', fontSize: '0.9rem', lineHeight: 1.4 }}>
                    Se creará automáticamente una sala de Zoom cuando inicies la reunión desde el detalle del evento.
                  </p>
                  <div className="input-group" style={{ marginBottom: 0 }}>
                    <label className="input-label">Duración máxima (minutos) *</label>
                    <div className="input-row">
                      <input
                        type="number"
                        name="zoomDuration"
                        min="5"
                        max="480"
                        step="5"
                        value={formData.zoomDuration}
                        onChange={handleChange}
                        className={`input-box input-medium ${validationErrors.zoomDuration ? 'input-error' : ''}`}
                      />
                      <LuVideo className="input-icon" />
                    </div>
                    {validationErrors.zoomDuration && <span className="field-error">{validationErrors.zoomDuration}</span>}
                    <span className="field-hint">Entre 5 y 480 minutos.</span>
                  </div>
                </div>
              </div>
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
                          const returnPath = isEdit ? `/crear-evento/${id}` : `/crear-evento/new`;
                          const returnQuery = selectedCommunityId ? `?communityId=${selectedCommunityId}` : '';
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
                        const returnPath = isEdit ? `/crear-evento/${id}` : `/crear-evento/new`;
                        const returnQuery = selectedCommunityId ? `?communityId=${selectedCommunityId}` : '';
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
            <button className="btn btn-primary" onClick={handleSubmit} disabled={loading || (isEdit && eventStarted)}>
              {loading ? 'Guardando...' : (isEdit ? (eventStarted ? 'Evento iniciado' : 'Actualizar Evento') : 'Crear Evento')}
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

export default CrearEvento;
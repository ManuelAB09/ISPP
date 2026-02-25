import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { LuCalendar, LuSquareCheck, LuMapPin, LuLink, LuArrowLeft } from 'react-icons/lu';
import './CreateEvent.css';
import Navbar from '../../components/Navbar';
import { createEvent, getEventById, updateEvent } from '../../api/eventEndpoints';


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
    tipoLocalizacion: 'Presencial',
    direccion: ''
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const isEdit = id && id !== 'new';

  // Cargar datos del evento si es edición
  useEffect(() => {
    if (isEdit) {
      const fetchEvent = async () => {
        try {
          setLoading(true);
          const data = await getEventById(id);
          const fecha = new Date(data.fechaHora);
          setFormData({
            nombre: data.titulo || '',
            descripcion: data.descripcion || '',
            comentario: data.queLlevar || '',
            dia: String(fecha.getDate()).padStart(2, '0'),
            mes: String(fecha.getMonth() + 1).padStart(2, '0'),
            anio: String(fecha.getFullYear()),
            hora: String(fecha.getHours()).padStart(2, '0'),
            minuto: String(fecha.getMinutes()).padStart(2, '0'),
            tipoLocalizacion: data.esVirtual ? 'Online' : 'Presencial',
            direccion: data.esVirtual ? (data.enlaceVirtual || '') : (data.ubicacion || '')
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
  }, [id, isEdit]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  const buildEventPayload = () => {
    const fechaHora = `${formData.anio}-${formData.mes}-${formData.dia}T${formData.hora}:${formData.minuto}:00`;
    const esVirtual = formData.tipoLocalizacion === 'Online';

    return {
      titulo: formData.nombre,
      descripcion: formData.descripcion,
      fechaHora,
      aforo: 50,
      queLlevar: formData.comentario || undefined,
      esVirtual,
      ubicacion: !esVirtual ? formData.direccion : undefined,
      enlaceVirtual: esVirtual ? formData.direccion : undefined,
      visibleEnMapa: true
    };
  };

  const handleSubmit = async () => {
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
        navigate(`/community/${communityId}`);
      } else {
        navigate('/');
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
      <Navbar avatarUrl="https://i.pravatar.cc/150?img=11" />

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

        <div className="dotted-divider"></div>

        <div className="form-grid">
          <div className="left-column">
            <div className="community-info">
              <div className="community-image">
                <img src="https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&w=150&q=80" alt="Evento" />
              </div>
              <h3 className="community-title">IISSI 2 - Universidad<br />de Sevilla</h3>
            </div>

            <div className="input-group">
              <label className="input-label">Fecha</label>
              <div className="input-row">
                <input type="text" name="dia" placeholder="DD" value={formData.dia} onChange={handleChange} className="input-box input-small" />
                <input type="text" name="mes" placeholder="MM" value={formData.mes} onChange={handleChange} className="input-box input-small" />
                <input type="text" name="anio" placeholder="YYYY" value={formData.anio} onChange={handleChange} className="input-box input-medium" />
                <LuCalendar className="input-icon" />
              </div>
            </div>

            <div className="input-group">
              <label className="input-label">Hora</label>
              <div className="input-row">
                <input type="text" name="hora" placeholder="HH" value={formData.hora} onChange={handleChange} className="input-box input-medium" />
                <input type="text" name="minuto" placeholder="mm" value={formData.minuto} onChange={handleChange} className="input-box input-medium" />
                <LuSquareCheck className="input-icon" />
              </div>
            </div>
          </div>

          <div className="right-column">
            <div className="input-group">
              <label className="input-label">Nombre del Evento</label>
              <input type="text" name="nombre" placeholder="Ej. Clase de NodeJS + Sequelize" value={formData.nombre} onChange={handleChange} className="input-box input-large" />
            </div>

            <div className="input-group">
              <label className="input-label">Descripción</label>
              <textarea name="descripcion" placeholder="¿De qué trata este evento? Comparte los objetivos" value={formData.descripcion} onChange={handleChange} rows="3" className="input-box input-large"></textarea>
            </div>

            <div className="input-group">
              <label className="input-label">Comentario adicional</label>
              <input type="text" name="comentario" placeholder="Ej. Materiales necesarios" value={formData.comentario} onChange={handleChange} className="input-box input-large" />
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
            <label className="input-label">Dirección o lugar</label>
            <textarea name="direccion" placeholder="¿Dónde ocurrirá la reunión? Ej. Biblioteca de Facultad de Derecho (Sevilla)" value={formData.direccion} onChange={handleChange} rows="3" className="input-box input-large"></textarea>
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
      </div>
    </div>
  );
};

export default CreateEvent;
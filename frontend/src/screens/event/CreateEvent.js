import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { LuCalendar, LuSquareCheck, LuMapPin, LuLink, LuArrowLeft } from 'react-icons/lu';
import './CreateEvent.css';
import Navbar from '../../components/Navbar';


const CreateEvent = () => {
  const { id } = useParams();

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

  const isEdit = id && id !== 'new';

  // Simulación de fetch de datos si es edición
  useEffect(() => {
    if (isEdit) {
      // Aquí deberías hacer una petición para obtener los datos del evento por id
      // Por ejemplo:
      // fetch(`/api/eventos/${id}`)
      //   .then(res => res.json())
      //   .then(data => setFormData(data));
      // Simulación:
      setFormData({
        nombre: 'Evento de ejemplo',
        descripcion: 'Descripción de ejemplo',
        comentario: 'Comentario de ejemplo',
        dia: '01',
        mes: '01',
        anio: '2026',
        hora: '12',
        minuto: '00',
        tipoLocalizacion: 'Presencial',
        direccion: 'Lugar de ejemplo'
      });
    }
  }, [id, isEdit]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
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
            <button className="btn btn-outline">
              Guardar Borrador
            </button>
            <button className="btn btn-primary">
              {isEdit ? 'Actualizar Evento' : 'Crear Evento'}
            </button>
          </div>

          <button className="back-link">
            <LuArrowLeft /> Volver al Dashboard
          </button>
        </div>
      </div>
    </div>
  );
};

export default CreateEvent;
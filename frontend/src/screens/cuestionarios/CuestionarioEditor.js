import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { LuPlus, LuTrash2, LuSave, LuCheck, LuArrowLeft } from 'react-icons/lu';
import { cuestionariosApi } from '../../api/cuestionarios.api';
import { communitiesApi } from '../../api/communities.api';
import './CuestionarioEditor.css';
import Header from '../../components/Header/Header';

const cloneInitialQuestion = () => ({
  enunciado: '',
  tipo: 'TEST',
  opciones: [
    { texto: '', correcta: true },
    { texto: '', correcta: false }
  ],
  respuestasAceptables: []
});

const CuestionarioEditor = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [loading, setLoading] = useState(false);
  const [myCommunities, setMyCommunities] = useState([]);

  const [formData, setFormData] = useState({
    titulo: '',
    descripcion: '',
    materia: '',
    dificultad: 'INTERMEDIO',
    nivelEducativo: '',
    tiempoEstimadoMinutos: 15,
    publicado: false,
    preguntas: [cloneInitialQuestion()]
  });

  const [scope, setScope] = useState('GENERAL');
  const [selectedCommunityId, setSelectedCommunityId] = useState('');
  const [studentEmailsText, setStudentEmailsText] = useState('');

  useEffect(() => {
    const loadCommunities = async () => {
      try {
        const data = await communitiesApi.listMine();
        const list = Array.isArray(data) ? data : (data?.content || []);
        setMyCommunities(list);

        const requestedCommunityId = Number(searchParams.get('communityId'));
        if (!Number.isInteger(requestedCommunityId) || requestedCommunityId <= 0) {
          return;
        }

        const belongsToRequestedCommunity = list.some((c) => Number(c.id) === requestedCommunityId);
        if (belongsToRequestedCommunity) {
          setScope('COMUNIDAD');
          setSelectedCommunityId(String(requestedCommunityId));
        }
      } catch {
        setMyCommunities([]);
      }
    };
    loadCommunities();
  }, [searchParams]);

  const parsedStudentEmails = useMemo(
    () => studentEmailsText
      .split(',')
      .map((v) => v.trim())
      .filter((v) => v.length > 0 && v.includes('@')),
    [studentEmailsText]
  );

  const handleBasicInfoChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const addQuestion = () => {
    setFormData(prev => ({
      ...prev,
      preguntas: [...prev.preguntas, cloneInitialQuestion()]
    }));
  };

  const removeQuestion = (qIndex) => {
    setFormData(prev => ({
      ...prev,
      preguntas: prev.preguntas.filter((_, i) => i !== qIndex)
    }));
  };

  const updateQuestion = (qIndex, field, value) => {
    const updated = [...formData.preguntas];
    updated[qIndex][field] = value;
    
    // Si cambia el tipo a V/F, resetear opciones
    if (field === 'tipo' && value === 'VERDADERO_FALSO') {
      updated[qIndex].opciones = [
        { texto: 'Verdadero', correcta: true },
        { texto: 'Falso', correcta: false }
      ];
    } else if (field === 'tipo' && value === 'RESPUESTA_CORTA') {
      updated[qIndex].opciones = [{ texto: '', correcta: true }];
    } else if (field === 'tipo' && value === 'TEST') {
      updated[qIndex].opciones = [
        { texto: '', correcta: true },
        { texto: '', correcta: false }
      ];
    }
    
    setFormData(prev => ({ ...prev, preguntas: updated }));
  };

  const addOption = (qIndex) => {
    const updated = [...formData.preguntas];
    updated[qIndex].opciones.push({ texto: '', correcta: false });
    setFormData(prev => ({ ...prev, preguntas: updated }));
  };

  const removeOption = (qIndex, oIndex) => {
    const updated = [...formData.preguntas];
    updated[qIndex].opciones = updated[qIndex].opciones.filter((_, i) => i !== oIndex);
    setFormData(prev => ({ ...prev, preguntas: updated }));
  };

  const updateOption = (qIndex, oIndex, field, value) => {
    const updated = [...formData.preguntas];
    
    if (field === 'correcta' && (updated[qIndex].tipo === 'TEST' || updated[qIndex].tipo === 'VERDADERO_FALSO')) {
      // Solo una correcta por ahora
      updated[qIndex].opciones.forEach(opt => opt.correcta = false);
    }
    
    updated[qIndex].opciones[oIndex][field] = value;
    setFormData(prev => ({ ...prev, preguntas: updated }));
  };

  const handleSubmit = async (publicar) => {
    if (!formData.titulo.trim() || !formData.materia.trim()) {
      alert('Completa al menos título y materia del cuestionario');
      return;
    }

    const tiempo = Number(formData.tiempoEstimadoMinutos);
    if (!Number.isFinite(tiempo) || tiempo <= 0) {
      alert('El tiempo estimado debe ser un número mayor que 0');
      return;
    }

    if (!Array.isArray(formData.preguntas) || formData.preguntas.length === 0) {
      alert('Debes añadir al menos una pregunta');
      return;
    }

    for (let i = 0; i < formData.preguntas.length; i += 1) {
      const q = formData.preguntas[i];
      if (!q.enunciado || !q.enunciado.trim()) {
        alert(`La pregunta ${i + 1} no tiene enunciado`);
        return;
      }

      if (q.tipo === 'TEST') {
        const opcionesConTexto = (q.opciones || []).filter((o) => (o?.texto || '').trim() !== '');
        const numCorrectas = (q.opciones || []).filter((o) => Boolean(o?.correcta)).length;
        if (opcionesConTexto.length < 2) {
          alert(`La pregunta ${i + 1} tipo test necesita al menos 2 opciones con texto`);
          return;
        }
        if (numCorrectas !== 1) {
          alert(`La pregunta ${i + 1} tipo test debe tener exactamente 1 opción correcta`);
          return;
        }
      }

      if (q.tipo === 'RESPUESTA_CORTA') {
        const respuestas = (q.opciones || [])
          .map((o) => (o?.texto || '').trim())
          .filter(Boolean);
        if (respuestas.length === 0) {
          alert(`La pregunta ${i + 1} de respuesta corta necesita una respuesta esperada`);
          return;
        }
      }
    }

    if (scope === 'COMUNIDAD' && !selectedCommunityId) {
      alert('Selecciona una comunidad para publicar el cuestionario');
      return;
    }

    if (scope === 'PERSONA' && parsedStudentEmails.length === 0) {
      alert('Introduce al menos un email de alumno para publicar el cuestionario');
      return;
    }

    try {
      setLoading(true);
      const isPrivate = scope === 'PRIVADO';
      const payload = {
        ...formData,
        publicado: publicar && !isPrivate,
        numPreguntas: formData.preguntas.length,
        activo: true,
        titulo: formData.titulo.trim(),
        materia: formData.materia.trim(),
        descripcion: (formData.descripcion || '').trim(),
        nivelEducativo: (formData.nivelEducativo || '').trim(),
        tiempoEstimadoMinutos: tiempo,
        dificultad: formData.dificultad || 'INTERMEDIO',
        comunidadesIds: scope === 'COMUNIDAD' ? [Number(selectedCommunityId)] : [],
        alumnosEmails: scope === 'PERSONA' ? parsedStudentEmails : [],
        // Limpiamos los arrays según el tipo
        preguntas: formData.preguntas.map(q => ({
          enunciado: q.enunciado.trim(),
          tipo: q.tipo,
          opciones: q.tipo !== 'RESPUESTA_CORTA'
            ? q.opciones
              .filter((o) => (o?.texto || '').trim() !== '')
              .map((o, idx) => ({ ...o, texto: o.texto.trim(), orden: idx }))
            : [],
          respuestasAceptables: q.tipo === 'RESPUESTA_CORTA'
            ? q.opciones
              .map((o) => (o?.texto || '').trim())
              .filter(Boolean)
            : []
        }))
      };

      await cuestionariosApi.createCuestionario(payload);

      if (!publicar) {
        alert('Borrador guardado');
      } else if (isPrivate) {
        alert('Cuestionario guardado como privado');
      } else if (scope === 'GENERAL') {
        alert('Cuestionario publicado para cualquier usuario');
      } else if (scope === 'COMUNIDAD') {
        alert('Cuestionario publicado para la comunidad seleccionada');
      } else {
        alert('Cuestionario publicado para los alumnos indicados');
      }

      navigate(-1);
    } catch (error) {
      console.error(error);
      const apiMessage =
        error?.response?.data?.message
        || error?.response?.data?.error
        || error?.response?.data?.details
        || (typeof error?.response?.data === 'string' ? error.response.data : null)
        || error?.message;
      alert(apiMessage ? `Error al guardar el cuestionario: ${apiMessage}` : 'Error al guardar el cuestionario');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <Header />
      <div className="cuestionario-editor-container">
        <div className="cuestionario-editor-header">
          <button className="btn-secondary" onClick={() => navigate(-1)} style={{ padding: '0.5rem' }}>
            <LuArrowLeft /> Volver
          </button>
          <h1>Editor de Cuestionario</h1>
          <span className="status-badge draft">Borrador</span>
        </div>

        <div className="form-section">
          <h2>Información General</h2>
          <div className="form-group-row">
            <div className="form-group" style={{ flex: 2 }}>
              <label>Título del cuestionario</label>
              <input 
                type="text" 
                className="form-control" 
                name="titulo" 
                value={formData.titulo} 
                onChange={handleBasicInfoChange} 
                placeholder="Ej: Autoevaluación Tema 1"
              />
            </div>
            <div className="form-group">
              <label>Materia</label>
              <input 
                type="text" 
                className="form-control" 
                name="materia" 
                value={formData.materia} 
                onChange={handleBasicInfoChange} 
                placeholder="Ej: Matemáticas"
              />
            </div>
          </div>

          <div className="form-group-row">
            <div className="form-group">
              <label>Descripción (Opcional)</label>
              <textarea 
                className="form-control" 
                name="descripcion" 
                value={formData.descripcion} 
                onChange={handleBasicInfoChange} 
                rows="2"
              />
            </div>
          </div>

          <div className="form-group-row">
            <div className="form-group">
              <label>Dificultad</label>
              <select className="form-control" name="dificultad" value={formData.dificultad} onChange={handleBasicInfoChange}>
                <option value="BASICO">Básico</option>
                <option value="INTERMEDIO">Intermedio</option>
                <option value="AVANZADO">Avanzado</option>
              </select>
            </div>
            <div className="form-group">
              <label>Tiempo estimado (min)</label>
              <input 
                type="number" 
                className="form-control" 
                name="tiempoEstimadoMinutos" 
                value={formData.tiempoEstimadoMinutos} 
                onChange={handleBasicInfoChange} 
              />
            </div>
          </div>

          <div className="form-group-row">
            <div className="form-group">
              <label>Modo de publicación</label>
              <select className="form-control" value={scope} onChange={(e) => setScope(e.target.value)}>
                <option value="GENERAL">Publicado para cualquier usuario</option>
                <option value="COMUNIDAD">Publicado para una comunidad</option>
                <option value="PERSONA">Publicado para personas concretas</option>
                <option value="PRIVADO">Privado (solo tú)</option>
              </select>
            </div>
          </div>

          {scope === 'COMUNIDAD' && (
            <div className="form-group-row">
              <div className="form-group">
                <label>Comunidad destino</label>
                <select
                  className="form-control"
                  value={selectedCommunityId}
                  onChange={(e) => setSelectedCommunityId(e.target.value)}
                >
                  <option value="">Selecciona una comunidad</option>
                  {myCommunities.map((c) => (
                    <option key={c.id} value={c.id}>{c.nombre}</option>
                  ))}
                </select>
              </div>
            </div>
          )}

          {scope === 'PERSONA' && (
            <div className="form-group-row">
              <div className="form-group">
                <label>Emails de alumnos (separados por coma)</label>
                <input
                  type="text"
                  className="form-control"
                  value={studentEmailsText}
                  onChange={(e) => setStudentEmailsText(e.target.value)}
                  placeholder="Ej: alumno1@email.com, alumno2@email.com"
                />
              </div>
            </div>
          )}
        </div>

        <div className="form-section">
          <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem'}}>
            <h2 style={{margin: 0, border: 'none'}}>Preguntas</h2>
            <button className="btn-add" onClick={addQuestion}>
              <LuPlus /> Añadir Pregunta
            </button>
          </div>

          {formData.preguntas.map((pregunta, qIndex) => (
            <div key={qIndex} className="pregunta-card">
              <div className="pregunta-header">
                <h3>Pregunta {qIndex + 1}</h3>
                <button className="btn-icon" onClick={() => removeQuestion(qIndex)} disabled={formData.preguntas.length === 1}>
                  <LuTrash2 size={20} />
                </button>
              </div>

              <div className="form-group-row">
                <div className="form-group" style={{ flex: 3 }}>
                  <label>Enunciado</label>
                  <input 
                    type="text" 
                    className="form-control" 
                    value={pregunta.enunciado} 
                    onChange={(e) => updateQuestion(qIndex, 'enunciado', e.target.value)} 
                    placeholder="Escribe la pregunta aquí..."
                  />
                </div>
                <div className="form-group" style={{ flex: 1 }}>
                  <label>Tipo</label>
                  <select 
                    className="form-control" 
                    value={pregunta.tipo} 
                    onChange={(e) => updateQuestion(qIndex, 'tipo', e.target.value)}
                  >
                    <option value="TEST">Tipo Test</option>
                    <option value="VERDADERO_FALSO">Verdadero/Falso</option>
                    <option value="RESPUESTA_CORTA">Respuesta Corta</option>
                  </select>
                </div>
              </div>

              {/* OPCIONES TEST */}
              {pregunta.tipo === 'TEST' && (
                <div className="opciones-list">
                  <label>Opciones (marca la correcta)</label>
                  {pregunta.opciones.map((opcion, oIndex) => (
                    <div key={oIndex} className="opcion-item">
                      <input 
                        type="radio" 
                        name={`correcta-${qIndex}`}
                        className="checkbox-correcta"
                        checked={opcion.correcta}
                        onChange={() => updateOption(qIndex, oIndex, 'correcta', true)}
                      />
                      <input 
                        type="text" 
                        className="form-control" 
                        value={opcion.texto}
                        onChange={(e) => updateOption(qIndex, oIndex, 'texto', e.target.value)}
                        placeholder={`Opción ${oIndex + 1}`}
                      />
                      <button className="btn-icon" onClick={() => removeOption(qIndex, oIndex)} disabled={pregunta.opciones.length <= 2}>
                        <LuTrash2 />
                      </button>
                    </div>
                  ))}
                  <button className="btn-add-option" onClick={() => addOption(qIndex)}>
                    + Añadir opción
                  </button>
                </div>
              )}

              {/* OPCIONES V/F */}
              {pregunta.tipo === 'VERDADERO_FALSO' && (
                <div className="opciones-list">
                  <label>Selecciona cuál es la respuesta correcta</label>
                  {pregunta.opciones.map((opcion, oIndex) => (
                    <div key={oIndex} className="opcion-item">
                      <input 
                        type="radio" 
                        name={`correcta-${qIndex}`}
                        className="checkbox-correcta"
                        checked={opcion.correcta}
                        onChange={() => updateOption(qIndex, oIndex, 'correcta', true)}
                      />
                      <span style={{marginLeft: '10px'}}>{opcion.texto}</span>
                    </div>
                  ))}
                </div>
              )}

              {/* RESPUESTA CORTA */}
              {pregunta.tipo === 'RESPUESTA_CORTA' && (
                <div className="opciones-list">
                  <label>Respuesta esperada (para corrección automática)</label>
                  <input 
                     type="text" 
                     className="form-control" 
                     value={pregunta.opciones[0]?.texto || ''}
                     onChange={(e) => updateQuestion(qIndex, 'opciones', [{texto: e.target.value, correcta: true}])}
                     placeholder="Ej: Paris"
                  />
                </div>
              )}
            </div>
          ))}
        </div>

        <div className="actions-footer">
          <button className="btn-secondary" onClick={() => handleSubmit(false)} disabled={loading}>
            <LuSave /> Guardar Borrador
          </button>
          <button className="btn-success" onClick={() => handleSubmit(true)} disabled={loading}>
            <LuCheck /> Publicar Cuestionario
          </button>
        </div>
      </div>
    </>
  );
};

export default CuestionarioEditor;

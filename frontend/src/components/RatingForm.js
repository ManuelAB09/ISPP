
import React, { useState } from 'react';
import axios from 'axios';
import './RatingForm.css';

const RatingForm = ({ profesorId, eventoId, onSuccess }) => {
  const [puntuacion, setPuntuacion] = useState(0);
  const [comentario, setComentario] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setSuccess(false);
    try {
      await axios.post('/api/valoraciones', {
        profesor: { id: profesorId },
        evento: { id: eventoId },
        puntuacion,
        comentario,
        fecha: new Date().toISOString(),
      });
      setPuntuacion(0);
      setComentario('');
      setSuccess(true);
      if (onSuccess) onSuccess();
    } catch (err) {
      setError('Error al enviar la valoración');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form className="rating-form" onSubmit={handleSubmit}>
      <div className="rating-form-stars">
        <label>Puntuación:</label>
        <div className="rating-stars">
          {[1,2,3,4,5].map(n => (
            <span
              key={n}
              className={`star ${puntuacion >= n ? 'selected' : ''}`}
              onClick={() => setPuntuacion(n)}
              role="button"
              aria-label={`Valorar con ${n} estrellas`}
            >&#9733;</span>
          ))}
        </div>
      </div>
      <div className="rating-form-comment">
        <textarea
          placeholder="Comentario (opcional)"
          value={comentario}
          onChange={e => setComentario(e.target.value)}
          rows={3}
        />
      </div>
      <button className="rating-form-btn" type="submit" disabled={loading || puntuacion === 0}>
        {loading ? 'Enviando...' : 'Valorar'}
      </button>
      {error && <div className="rating-form-error">{error}</div>}
      {success && <div className="rating-form-success">¡Valoración enviada!</div>}
    </form>
  );
};

export default RatingForm;

import React, { useState } from "react";
import { crearValoracion } from "../api/valoraciones.api";
import "./RatingForm.css";

const RatingForm = ({ profesorId, alumnoId, eventoId, onValorado, alreadyRated }) => {
  const [puntuacion, setPuntuacion] = useState(0);
  const [comentario, setComentario] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState(null);
  const [exito, setExito] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setEnviando(true);
    setError(null);
    try {
      await crearValoracion({
        profesor: { id: profesorId },
        alumno: { id: alumnoId },
        puntuacion,
        comentario,
        evento: { id: eventoId },
        fecha: new Date().toISOString(),
      });
      setExito(true);
      if (onValorado) onValorado();
    } catch (err) {
      let msg = "Error al enviar la valoración";
      if (err?.response?.data?.message) {
        msg = err.response.data.message;
      } else if (err?.message) {
        msg = err.message;
      }
      setError(msg);
    } finally {
      setEnviando(false);
    }
  };

  if (alreadyRated) {
    return <div className="rating-form__success">¡Gracias por tu valoración!</div>;
  }
  return (
    <form className="rating-form" onSubmit={handleSubmit}>
      <div className="rating-form__stars">
        {[1, 2, 3, 4, 5].map((star) => (
          <span
            key={star}
            className={star <= puntuacion ? "star filled" : "star"}
            onClick={() => setPuntuacion(star)}
          >
            ★
          </span>
        ))}
      </div>
      <textarea
        className="rating-form__comment"
        placeholder="Escribe un comentario (opcional)"
        value={comentario}
        onChange={(e) => setComentario(e.target.value)}
        rows={3}
      />
      <button className="rating-form__submit" type="submit" disabled={enviando || puntuacion === 0}>
        {enviando ? "Enviando..." : "Enviar valoración"}
      </button>
      {error && <div className="rating-form__error">{error}</div>}
      {exito && <div className="rating-form__success">¡Gracias por tu valoración!</div>}
    </form>
  );
};

export default RatingForm;

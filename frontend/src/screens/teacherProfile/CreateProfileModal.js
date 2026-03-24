import React, { useState } from "react";
import { createTutorProfile } from "../../api/tutorEndpoints";
import "./TutorModals.css";

/**
 * Modal para crear un perfil de tutor nuevo.
 * Llama a POST /api/v1/tutors
 *
 * Props:
 *   - onClose: callback para cerrar el modal
 *   - onCreado: callback(newTutor) cuando se crea correctamente
 */
const CreateProfileModal = ({ onClose, onCreado }) => {
  const [form, setForm] = useState({
    especialidades: "",
    tarifaHora: "",
    disponibilidad: "",
    bio: "",
  });
  const [creando, setCreando] = useState(false);
  const [error, setError] = useState(null);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setCreando(true);
    setError(null);

    if (!form.bio.trim()) {
      setError("La biografia es obligatoria. Por favor, escribe una breve descripcion profesional.");
      setCreando(false);
      return;
    }

    const payload = {
      especialidades: form.especialidades
        .split(",")
        .map((s) => s.trim())
        .filter(Boolean),
      tarifaPorHora: parseInt(form.tarifaHora, 10) || 0,
      disponibilidad: form.disponibilidad.trim(),
      biografia: form.bio.trim(),
    };

    try {
      const newTutor = await createTutorProfile(payload);
      onCreado(newTutor);
      onClose();
    } catch (err) {
      console.error("Error al crear perfil de tutor:", err);
      const backendMsg = err?.response?.data?.message
        || err?.response?.data?.error
        || err?.data?.message
        || err?.data?.error;
      if (backendMsg) {
        setError(backendMsg);
      } else {
        setError("No se pudo crear el perfil. Intentalo de nuevo.");
      }
    } finally {
      setCreando(false);
    }
  };

  return (
    <div className="tm-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="tm-modal" role="dialog" aria-modal="true" aria-label="Crear perfil de profesor">
        <div className="tm-modal__header">
          <h2 className="tm-modal__title">🎓 Crear Perfil de Profesor</h2>
          <button className="tm-modal__close" onClick={onClose} aria-label="Cerrar">✕</button>
        </div>

        <form className="tm-form" onSubmit={handleSubmit}>
          {/* Especialidades */}
          <div className="tm-field">
            <label className="tm-field__label" htmlFor="especialidades">
              Especialidades
              <span className="tm-field__hint"> (separadas por coma)</span>
            </label>
            <input
              id="especialidades"
              name="especialidades"
              type="text"
              className="tm-field__input"
              value={form.especialidades}
              onChange={handleChange}
              placeholder="Ej: Programación, Matemáticas, Bachillerato"
              required
            />
          </div>

          {/* Tarifa por hora */}
          <div className="tm-field">
            <label className="tm-field__label" htmlFor="tarifaHora">
              Tarifa por hora (€)
            </label>
            <input
              id="tarifaHora"
              name="tarifaHora"
              type="number"
              min="1"
              step="1"
              className="tm-field__input tm-field__input--sm"
              value={form.tarifaHora}
              onChange={handleChange}
              placeholder="Ej: 25"
              required
            />
          </div>

          {/* Disponibilidad */}
          <div className="tm-field">
            <label className="tm-field__label" htmlFor="disponibilidad">
              Disponibilidad horaria
            </label>
            <input
              id="disponibilidad"
              name="disponibilidad"
              type="text"
              className="tm-field__input"
              value={form.disponibilidad}
              onChange={handleChange}
              placeholder="Ej: Tardes y fines de semana"
            />
          </div>

          {/* Bio */}
          <div className="tm-field">
            <label className="tm-field__label" htmlFor="bio">
              Biografía profesional
            </label>
            <textarea
              id="bio"
              name="bio"
              className="tm-field__textarea"
              rows={4}
              value={form.bio}
              onChange={handleChange}
              placeholder="Cuéntales a los alumnos quién eres y qué experiencia tienes…"
            />
          </div>

          {error && <p className="tm-error">{error}</p>}

          <div className="tm-modal__footer">
            <button type="button" className="tm-btn tm-btn--secondary" onClick={onClose}>
              Cancelar
            </button>
            <button type="submit" className="tm-btn tm-btn--primary" disabled={creando}>
              {creando ? "Creando…" : "Crear perfil"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CreateProfileModal;

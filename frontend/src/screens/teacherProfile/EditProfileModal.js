import React, { useState } from "react";
import { updateTutorProfile } from "../../api/tutorEndpoints";
import "./TutorModals.css";

/**
 * Modal para editar el perfil de tutor.
 * Permite modificar especialidades, tarifa por hora, disponibilidad y bio.
 * Llama a PUT /api/v1/tutors/{tutorId}
 *
 * Props:
  *   - tutor: objeto TutorProfileResponse actual
  *   - onClose: callback para cerrar el modal
  *   - onGuardar: callback(updatedTutor) cuando se guarda correctamente
  */
 const EditProfileModal = ({ tutor, onClose, onGuardar }) => {
   const [form, setForm] = useState({
     especialidades: (tutor.especialidades || []).join(", "),
     tarifaHora: tutor.tarifaHora ?? "",
     disponibilidad: tutor.disponibilidad ?? "",
     bio: tutor.bio ?? "",
   });
   const [guardando, setGuardando] = useState(false);
   const [error, setError] = useState(null);
 
   const handleChange = (e) => {
     const { name, value } = e.target;
     setForm((prev) => ({ ...prev, [name]: value }));
   };
 
   const handleSubmit = async (e) => {
     e.preventDefault();
     setGuardando(true);
     setError(null);
 
     // Convertir especialidades de string CSV a array
     const payload = {
       especialidades: form.especialidades
         .split(",")
         .map((s) => s.trim())
         .filter(Boolean),
       tarifaHora: parseFloat(form.tarifaHora) || 0,
       disponibilidad: form.disponibilidad.trim(),
       bio: form.bio.trim(),
     };
 
     try {
       const updated = await updateTutorProfile(tutor.id, payload);
       onGuardar(updated);
       onClose();
     } catch (err) {
       console.error("Error al guardar perfil:", err);
       setError("No se pudieron guardar los cambios. Inténtalo de nuevo.");
     } finally {
       setGuardando(false);
     }
   };

  return (
    <div className="tm-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="tm-modal" role="dialog" aria-modal="true" aria-label="Editar perfil">
        <div className="tm-modal__header">
          <h2 className="tm-modal__title">✏ Editar Perfil</h2>
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
              min="0"
              step="0.5"
              className="tm-field__input tm-field__input--sm"
              value={form.tarifaHora}
              onChange={handleChange}
              placeholder="Ej: 25"
            />
          </div>

          {/* Disponibilidad */}
          <div className="tm-field">
            <label className="tm-field__label" htmlFor="disponibilidad">
              Disponibilidad
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
            <button type="submit" className="tm-btn tm-btn--primary" disabled={guardando}>
              {guardando ? "Guardando…" : "Guardar cambios"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default EditProfileModal;

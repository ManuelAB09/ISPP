import { useState } from 'react';
import { communitiesApi } from '../../api/communities.api';
import './InviteMemberModal.css';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function InviteMemberModal({ communityId, onClose, onInvited }) {
  const [email, setEmail] = useState('');
  const [rol, setRol] = useState('ALUMNO');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError(null);
    setSuccess(null);

    const normalizedEmail = email.trim().toLowerCase();
    if (!normalizedEmail) {
      setError('El email es obligatorio.');
      return;
    }

    if (!EMAIL_REGEX.test(normalizedEmail)) {
      setError('Introduce un email valido.');
      return;
    }

    try {
      setSubmitting(true);
      await communitiesApi.createInvitation(communityId, {
        email: normalizedEmail,
        rol,
      });
      setSuccess('Invitacion enviada por email correctamente.');
      setEmail('');
      setRol('ALUMNO');
      if (onInvited) onInvited();
    } catch (err) {
      setError(err?.message || 'No se pudo enviar la invitacion.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="imm-overlay" onClick={onClose}>
      <div className="imm-modal" onClick={(e) => e.stopPropagation()}>
        <div className="imm-header">
          <h2>Invitar miembro por email</h2>
          <button className="imm-close" onClick={onClose} type="button">
            x
          </button>
        </div>

        <p className="imm-description">
          Envia una invitacion para unirse a la comunidad. El destinatario recibira un email con el
          enlace de acceso.
        </p>

        <form className="imm-form" onSubmit={handleSubmit}>
          <label htmlFor="invite-email">Email del invitado</label>
          <input
            id="invite-email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="usuario@dominio.com"
            disabled={submitting}
            required
          />

          <label htmlFor="invite-role">Rol en la comunidad</label>
          <select
            id="invite-role"
            value={rol}
            onChange={(e) => setRol(e.target.value)}
            disabled={submitting}
          >
            <option value="ALUMNO">Alumno</option>
            <option value="PROFESOR">Profesor</option>
            <option value="ADMIN">Administrador</option>
          </select>

          {error && <p className="imm-error">{error}</p>}
          {success && <p className="imm-success">{success}</p>}

          <div className="imm-actions">
            <button type="button" className="imm-btn imm-btn--secondary" onClick={onClose}>
              Cerrar
            </button>
            <button type="submit" className="imm-btn imm-btn--primary" disabled={submitting}>
              {submitting ? 'Enviando...' : 'Enviar invitacion'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

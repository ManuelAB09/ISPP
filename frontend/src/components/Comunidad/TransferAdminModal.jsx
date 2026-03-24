import { useState, useEffect } from 'react';
import { communitiesApi } from '../../api/communities.api';
import './TransferAdminModal.css';

export default function TransferAdminModal({ communityId, currentUserId, hasTeacherProfile, onClose, onTransferred }) {
  const [members, setMembers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedUserId, setSelectedUserId] = useState(null);
  const [transferring, setTransferring] = useState(false);
  const [error, setError] = useState(null);
  const [confirmStep, setConfirmStep] = useState(false);
  const [selectedRole, setSelectedRole] = useState(hasTeacherProfile ? 'PROFESOR' : 'ALUMNO');

  useEffect(() => {
    const fetchMembers = async () => {
      try {
        setLoading(true);
        const data = await communitiesApi.getMembers(communityId, { size: 100 });
        const list = data?.content || data || [];
        // Excluir al admin actual de la lista
        const filtered = list.filter(
          (m) => m.usuario?.id !== Number(currentUserId) && m.rol !== 'ADMIN'
        );
        setMembers(filtered);
      } catch (err) {
        console.error('Error al cargar miembros:', err);
        setError('No se pudieron cargar los miembros');
      } finally {
        setLoading(false);
      }
    };
    fetchMembers();
  }, [communityId, currentUserId]);

  const selectedMember = members.find((m) => m.usuario?.id === selectedUserId);

  const handleTransfer = async () => {
    if (!selectedUserId) return;

    if (!confirmStep) {
      setConfirmStep(true);
      return;
    }

    try {
      setTransferring(true);
      setError(null);
      await communitiesApi.transferAdmin(communityId, selectedUserId, selectedRole);
      onTransferred();
    } catch (err) {
      console.error('Error al transferir administración:', err);
      setError(err.message || 'Error al transferir la administración');
      setConfirmStep(false);
    } finally {
      setTransferring(false);
    }
  };

  return (
    <div className="tam-overlay" onClick={onClose}>
      <div className="tam-modal" onClick={(e) => e.stopPropagation()}>
        <div className="tam-header">
          <h2>Transferir administración</h2>
          <button className="tam-close" onClick={onClose}>✕</button>
        </div>

        {!confirmStep ? (
          <>
            <p className="tam-description">
              Selecciona un miembro para convertirlo en el nuevo administrador de la comunidad.
              Tú pasarás a ser un miembro normal.
            </p>

            {loading ? (
              <p className="tam-loading">Cargando miembros...</p>
            ) : members.length === 0 ? (
              <p className="tam-empty">No hay otros miembros en la comunidad a los que transferir.</p>
            ) : (
              <div className="tam-member-list">
                {members.map((m) => (
                  <button
                    key={m.usuario.id}
                    className={`tam-member-item ${selectedUserId === m.usuario.id ? 'tam-member-selected' : ''}`}
                    onClick={() => setSelectedUserId(m.usuario.id)}
                    type="button"
                  >
                    <div className="tam-member-avatar">
                      {m.usuario.avatarUrl ? (
                        <img src={m.usuario.avatarUrl} alt={m.usuario.nombre} />
                      ) : (
                        <span className="tam-member-initials">
                          {m.usuario.nombre?.charAt(0)?.toUpperCase() || '?'}
                        </span>
                      )}
                    </div>
                    <div className="tam-member-info">
                      <span className="tam-member-name">{m.usuario.nombre}</span>
                      <span className="tam-member-role">{m.rol === 'PROFESOR' ? 'Profesor' : 'Alumno'}</span>
                    </div>
                    {selectedUserId === m.usuario.id && (
                      <span className="tam-member-check">✓</span>
                    )}
                  </button>
                ))}
              </div>
            )}
          </>
        ) : (
          <div className="tam-confirm">
            <p className="tam-confirm-text">
              ¿Estás seguro de que quieres transferir la administración a{' '}
              <strong>{selectedMember?.usuario?.nombre}</strong>?
            </p>
            {hasTeacherProfile && (
              <div className="tam-role-selection" style={{ marginTop: '15px' }}>
                <p>Elige tu nuevo rol en la comunidad:</p>
                <label style={{ display: 'block', marginTop: '10px' }}>
                  <input
                    type="radio"
                    name="newRole"
                    value="PROFESOR"
                    checked={selectedRole === 'PROFESOR'}
                    onChange={(e) => setSelectedRole(e.target.value)}
                  />
                  {' '}Profesor
                </label>
                <label style={{ display: 'block', marginTop: '10px' }}>
                  <input
                    type="radio"
                    name="newRole"
                    value="ALUMNO"
                    checked={selectedRole === 'ALUMNO'}
                    onChange={(e) => setSelectedRole(e.target.value)}
                  />
                  {' '}Alumno
                </label>
              </div>
            )}
            <p className="tam-confirm-warning" style={{ marginTop: '15px' }}>
              Perderás tus privilegios de administrador y pasarás a ser un miembro normal.
              Esta acción no se puede deshacer directamente.
            </p>
          </div>
        )}

        {error && <p className="tam-error">{error}</p>}

        <div className="tam-actions">
          <button
            type="button"
            className="tam-btn tam-btn--secondary"
            onClick={confirmStep ? () => setConfirmStep(false) : onClose}
            disabled={transferring}
          >
            {confirmStep ? 'Volver' : 'Cancelar'}
          </button>
          {members.length > 0 && (
            <button
              type="button"
              className="tam-btn tam-btn--primary"
              onClick={handleTransfer}
              disabled={!selectedUserId || transferring}
            >
              {transferring
                ? 'Transfiriendo...'
                : confirmStep
                  ? 'Confirmar transferencia'
                  : 'Transferir'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

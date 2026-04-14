// src/pages/borradores/MisBorradores.jsx
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../../components/Header/Header';
import { useAuth } from '../../contexts/AuthContext';
import './MisBorradores.css';

const formatFecha = (fechaStr) => {
    if (!fechaStr) return '';
    const d = new Date(fechaStr);
    return d.toLocaleDateString('es-ES', {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    });
};

function ConfirmModal({ message, onConfirm, onCancel }) {
    return (
        <div className="borradores-modal-overlay" onClick={onCancel}>
            <div className="borradores-modal" onClick={(e) => e.stopPropagation()}>
                <p className="borradores-modal__message">{message}</p>
                <div className="borradores-modal__actions">
                    <button className="borradores-modal__btn borradores-modal__btn--cancel" onClick={onCancel}>
                        Cancelar
                    </button>
                    <button className="borradores-modal__btn borradores-modal__btn--confirm" onClick={onConfirm}>
                        Eliminar
                    </button>
                </div>
            </div>
        </div>
    );
}

export default function MisBorradores() {
    const { user } = useAuth();
    const navigate = useNavigate();
    const [eventDrafts, setEventDrafts] = useState([]);
    const [communityDrafts, setCommunityDrafts] = useState([]);
    const [activeTab, setActiveTab] = useState('eventos');

    // Estado del modal de confirmación
    const [confirmModal, setConfirmModal] = useState({
        open: false,
        message: '',
        onConfirm: null,
    });

    useEffect(() => {
        try {
            const evDrafts = JSON.parse(localStorage.getItem('eventDrafts') || '[]');
            setEventDrafts(Array.isArray(evDrafts) ? evDrafts : []);
        } catch {
            setEventDrafts([]);
        }
        try {
            const raw = localStorage.getItem('crearComunidadDraft');
            if (raw) {
                const draft = JSON.parse(raw);
                setCommunityDrafts([{ ...draft, savedAt: draft.savedAt || null }]);
            } else {
                setCommunityDrafts([]);
            }
        } catch {
            setCommunityDrafts([]);
        }
    }, []);

    const openConfirm = (message, onConfirm) => {
        setConfirmModal({ open: true, message, onConfirm });
    };

    const closeConfirm = () => {
        setConfirmModal({ open: false, message: '', onConfirm: null });
    };

    const handleDeleteEventDraft = (idx) => {
        openConfirm('¿Quieres eliminar este borrador de evento? Esta acción no se puede deshacer.', () => {
            const updated = eventDrafts.filter((_, i) => i !== idx);
            setEventDrafts(updated);
            localStorage.setItem('eventDrafts', JSON.stringify(updated));
            closeConfirm();
        });
    };

    const handleEditEventDraft = (draft, idx) => {
        const communityId = draft.selectedCommunityId || '';
        localStorage.setItem('eventDraftIndex', String(idx));
        navigate(`/crear-evento/new${communityId ? `?communityId=${communityId}` : ''}`, {
            state: { eventFormDraft: draft },
        });
    };

    const handleDeleteCommunityDraft = () => {
        openConfirm('¿Quieres eliminar el borrador de comunidad? Esta acción no se puede deshacer.', () => {
            localStorage.removeItem('crearComunidadDraft');
            setCommunityDrafts([]);
            closeConfirm();
        });
    };

    const handleEditCommunityDraft = () => {
        navigate('/crear-comunidad');
    };

    return (
        <div className="borradores-page">
            <Header user={user} page="borradores" />

            {confirmModal.open && (
                <ConfirmModal
                    message={confirmModal.message}
                    onConfirm={confirmModal.onConfirm}
                    onCancel={closeConfirm}
                />
            )}

            <div className="borradores-content">
                <div className="borradores-header">
                    <h1>Tus borradores</h1>
                    <p>Retoma donde lo dejaste</p>
                </div>

                <div className="borradores-tabs">
                    <button
                        className={`borradores-tab ${activeTab === 'eventos' ? 'borradores-tab--active' : ''}`}
                        onClick={() => setActiveTab('eventos')}
                    >
                        📅 Eventos ({eventDrafts.length})
                    </button>
                    <button
                        className={`borradores-tab ${activeTab === 'comunidades' ? 'borradores-tab--active' : ''}`}
                        onClick={() => setActiveTab('comunidades')}
                    >
                        🏠 Comunidades ({communityDrafts.length})
                    </button>
                </div>

                {activeTab === 'eventos' && (
                    <div className="borradores-list">
                        {eventDrafts.length === 0 ? (
                            <div className="borradores-empty">
                                <span>📝</span>
                                <p>No tienes borradores de eventos guardados.</p>
                                <button
                                    className="borradores-btn-create"
                                    onClick={() => navigate('/crear-evento/new')}
                                >
                                    Crear evento
                                </button>
                            </div>
                        ) : (
                            eventDrafts.map((draft, idx) => (
                                <div key={idx} className="borrador-card">
                                    <div className="borrador-card__icon">📅</div>
                                    <div className="borrador-card__info">
                                        <h3 className="borrador-card__title">
                                            {draft.nombre || 'Sin título'}
                                        </h3>
                                        {draft.descripcion && (
                                            <p className="borrador-card__desc">
                                                {draft.descripcion.slice(0, 100)}
                                                {draft.descripcion.length > 100 ? '...' : ''}
                                            </p>
                                        )}
                                        <div className="borrador-card__meta">
                                            {draft.dia && draft.mes && draft.anio && (
                                                <span>
                                                    📆 {draft.dia}/{draft.mes}/{draft.anio}
                                                    {draft.hora && draft.minuto ? ` ${draft.hora}:${draft.minuto}` : ''}
                                                </span>
                                            )}
                                            {draft.tipoLocalizacion && (
                                                <span>
                                                    {draft.tipoLocalizacion === 'Online' ? '💻 Online' : '📍 Presencial'}
                                                </span>
                                            )}
                                            {draft.aforo && <span>👥 Aforo: {draft.aforo}</span>}
                                            {draft.savedAt && (
                                                <span>💾 Guardado: {formatFecha(draft.savedAt)}</span>
                                            )}
                                        </div>
                                    </div>
                                    <div className="borrador-card__actions">
                                        <button
                                            className="borrador-btn borrador-btn--edit"
                                            onClick={() => handleEditEventDraft(draft, idx)}
                                        >
                                            Continuar
                                        </button>
                                        <button
                                            className="borrador-btn borrador-btn--delete"
                                            onClick={() => handleDeleteEventDraft(idx)}
                                        >
                                            Eliminar
                                        </button>
                                    </div>
                                </div>
                            ))
                        )}
                    </div>
                )}

                {activeTab === 'comunidades' && (
                    <div className="borradores-list">
                        {communityDrafts.length === 0 ? (
                            <div className="borradores-empty">
                                <span>🏠</span>
                                <p>No tienes borradores de comunidades guardados.</p>
                                <button
                                    className="borradores-btn-create"
                                    onClick={() => navigate('/crear-comunidad')}
                                >
                                    Crear comunidad
                                </button>
                            </div>
                        ) : (
                            communityDrafts.map((draft, idx) => (
                                <div key={idx} className="borrador-card">
                                    <div className="borrador-card__icon">🏠</div>
                                    <div className="borrador-card__info">
                                        <h3 className="borrador-card__title">
                                            {draft.nombre || 'Sin nombre'}
                                        </h3>
                                        {draft.descripcion && (
                                            <p className="borrador-card__desc">
                                                {draft.descripcion.slice(0, 100)}
                                                {draft.descripcion.length > 100 ? '...' : ''}
                                            </p>
                                        )}
                                        <div className="borrador-card__meta">
                                            {draft.tipoComunidad && (
                                                <span>
                                                    {draft.tipoComunidad === 'GRUPO_PRIVADO' ? '🔒 Privada' : '🌐 Pública'}
                                                </span>
                                            )}
                                            {draft.maxMiembros && (
                                                <span>👥 Máx. {draft.maxMiembros} miembros</span>
                                            )}
                                            {draft.categorias?.length > 0 && (
                                                <span>🏷️ {draft.categorias.join(', ')}</span>
                                            )}
                                            {draft.savedAt && (
                                                <span>💾 Guardado: {formatFecha(draft.savedAt)}</span>
                                            )}
                                        </div>
                                    </div>
                                    <div className="borrador-card__actions">
                                        <button
                                            className="borrador-btn borrador-btn--edit"
                                            onClick={handleEditCommunityDraft}
                                        >
                                            Continuar
                                        </button>
                                        <button
                                            className="borrador-btn borrador-btn--delete"
                                            onClick={handleDeleteCommunityDraft}
                                        >
                                            Eliminar
                                        </button>
                                    </div>
                                </div>
                            ))
                        )}
                    </div>
                )}
            </div>
        </div>
    );
}
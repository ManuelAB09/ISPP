import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PersonIcon from '../icons/Person';
import { communitiesApi } from '../../api/communities.api';
import { getApiBaseUrl } from '../../api/baseUrl';
import './ComunidadCard.css';

export default function ComunidadCard({ comunidad, onJoined }) {
    const navigate = useNavigate();
    const [joining, setJoining] = useState(false);
    const [joined, setJoined] = useState(comunidad.esMiembro || false);
    const [requestSent, setRequestSent] = useState(false);
    const [showRolePicker, setShowRolePicker] = useState(false);
    const [error, setError] = useState(null);
    const currentUserId = localStorage.getItem('userId');
    const isPrivate = comunidad.tipoGrupo === 'GRUPO_PRIVADO';
    const userProfileRaw = localStorage.getItem('userProfile');
    let userProfile = null;
    try {
        userProfile = userProfileRaw ? JSON.parse(userProfileRaw) : null;
    } catch {
        userProfile = null;
    }
    const hasTeacherProfile = Boolean(userProfile?.esTutor || userProfile?.esProfesor);
    const communityImageRaw = comunidad.imagen || comunidad.imagenUrl || comunidad.foto;
    const communityImage = (() => {
        if (!communityImageRaw || !String(communityImageRaw).trim() || String(communityImageRaw).trim().toLowerCase() === 'empty') {
            return 'https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&w=400&q=80';
        }

        const value = String(communityImageRaw).trim();
        if (/^https?:\/\//i.test(value) || value.startsWith('data:image/')) {
            return value;
        }

        const base = getApiBaseUrl();
        if (value.startsWith('/')) {
            return `${base}${value}`;
        }

        return `${base}/${value}`;
    })();

    const performJoin = async (role) => {
        setJoining(true);
        setError(null);
        try {
            await communitiesApi.join(comunidad.id, role);
            setJoined(true);
            setShowRolePicker(false);
            if (onJoined) onJoined(comunidad.id);
        } catch (err) {
            if (err.message?.includes('401') || err.status === 401) {
                navigate('/login');
            } else if (err.message?.includes('409') || err.status === 409) {
                setJoined(true);
                setShowRolePicker(false);
            } else {
                setError(err?.message || 'Error al unirse');
            }
        } finally {
            setJoining(false);
        }
    };

    const handleJoin = async (e) => {
        e.stopPropagation();
        const token = localStorage.getItem('accessToken');
        if (!token) {
            navigate('/login');
            return;
        }

        if (!isPrivate && hasTeacherProfile) {
            setShowRolePicker(true);
            return;
        }

        setJoining(true);
        setError(null);
        try {
            if (isPrivate) {
                await communitiesApi.requestAccess(comunidad.id);
                setRequestSent(true);
            } else {
                await communitiesApi.join(comunidad.id, 'ALUMNO');
                setJoined(true);
                setShowRolePicker(false);
                if (onJoined) onJoined(comunidad.id);
            }
        } catch (err) {
            if (err.message?.includes('401') || err.status === 401) {
                navigate('/login');
            } else if (err.message?.includes('409') || err.status === 409) {
                setJoined(true);
            } else if (err.status === 400) {
                if (isPrivate) {
                    setRequestSent(true);
                } else {
                    setError(err?.message || 'Error al unirse');
                }
            } else {
                setError(isPrivate ? 'Error al solicitar acceso' : 'Error al unirse');
            }
        } finally {
            setJoining(false);
        }
    };

    return (
        <div
            key={comunidad.id}
            className="comunidad-card"
            onClick={() => navigate(`/comunidades/${comunidad.id}`)}
            style={{ cursor: 'pointer' }}
        >
            <img src={communityImage} alt={comunidad.nombre} className="comunidad-image" />
            <div className="comunidad-info">
                <div className='top-info'>
                    <h2>
                        {comunidad.nombre}
                        {isPrivate && <span className="comunidad-private-badge">Privada</span>}
                    </h2>
                    {comunidad?.categoria?.map(cat => (
                        <span key={cat} className="comunidad-tag">{cat}</span>
                    ))}
                    <p>{comunidad.descripcion || 'Sin descripción disponible'}</p>
                </div>
                <div className='bottom-info'>
                    <div className="members-info">
                        <PersonIcon width={20} height={20} />
                        <p>{comunidad.miembrosActuales || 0}/ <span>{comunidad.maxMiembros || 0}</span></p>
                    </div>
                    {error && <span style={{ color: 'red', fontSize: '0.8rem' }}>{error}</span>}
                    {currentUserId && !joined && !requestSent && (
                        showRolePicker ? (
                            <div className="join-role-picker" onClick={(e) => e.stopPropagation()}>
                                <p className="join-role-picker__title">Elige cómo quieres unirte</p>
                                <div className="join-role-picker__actions">
                                    <button
                                        className="join-button"
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            performJoin('PROFESOR');
                                        }}
                                        disabled={joining}
                                    >
                                        {joining ? 'Uniéndose...' : 'Unirme como profesor'}
                                    </button>
                                    <button
                                        className="join-button join-button--request"
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            performJoin('ALUMNO');
                                        }}
                                        disabled={joining}
                                    >
                                        {joining ? 'Uniéndose...' : 'Unirme como alumno'}
                                    </button>
                                </div>
                            </div>
                        ) : (
                            <button
                                className={`join-button${isPrivate ? ' join-button--request' : ''}`}
                                onClick={handleJoin}
                                disabled={joining}
                            >
                                {joining
                                    ? (isPrivate ? 'Solicitando...' : 'Uniéndose...')
                                    : (isPrivate ? 'Solicitar acceso' : 'Unirse')}
                            </button>
                        )
                    )}
                    {joined && (
                        <button className="join-button joined" disabled>
                            Unido
                        </button>
                    )}
                    {requestSent && !joined && (
                        <button className="join-button join-button--pending" disabled>
                            Solicitud enviada
                        </button>
                    )}
                    {!currentUserId && (
                        <button
                            className="join-button"
                            onClick={(e) => { e.stopPropagation(); navigate('/login'); }}
                        >
                            {isPrivate ? 'Solicitar acceso' : 'Unirse'}
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
}

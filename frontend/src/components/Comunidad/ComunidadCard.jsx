import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PersonIcon from '../icons/Person';
import { communitiesApi } from '../../api/communities.api';
import { subscriptionsApi } from '../../api/subscriptions.api';
import { getApiBaseUrl } from '../../api/baseUrl';
import { resolveCommunityImage } from '../../utils/imageUtils';
import './ComunidadCard.css';

const DEFAULT_COMMUNITY_IMAGE =
    'https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&w=400&q=80';

const PLAN_MAX_ACTIVE_COMMUNITIES = {
    FREE: 3,
    PREMIUM: 10,
    PRO: 25,
};

const normalizeSubscriptionPayload = (payload) => {
    if (!payload || typeof payload !== 'object') return {};
    if (payload.subscription && typeof payload.subscription === 'object') {
        return payload.subscription;
    }
    if (payload.data && typeof payload.data === 'object') {
        if (payload.data.subscription && typeof payload.data.subscription === 'object') {
            return payload.data.subscription;
        }
        return payload.data;
    }
    return payload;
};

const extractCommunitiesFromResponse = (response) => {
    if (Array.isArray(response)) return response;
    if (Array.isArray(response?.content)) return response.content;
    return [];
};

const resolveMaxActiveCommunitiesByPlan = (subscriptionRaw) => {
    const subscription = normalizeSubscriptionPayload(subscriptionRaw);
    const planKey = String(subscription?.plan || 'FREE').toUpperCase();
    return PLAN_MAX_ACTIVE_COMMUNITIES[planKey] ?? PLAN_MAX_ACTIVE_COMMUNITIES.FREE;
};

export default function ComunidadCard({ comunidad, onJoined }) {
    const navigate = useNavigate();
    const [joining, setJoining] = useState(false);
    const [joined, setJoined] = useState(comunidad.esMiembro || false);
    const [justJoined, setJustJoined] = useState(false);
    const [requestSent, setRequestSent] = useState(Boolean(comunidad.solicitudPendiente));
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
    const isMember = comunidad.esMiembro || false;
    const communityImage = resolveCommunityImage(comunidad);
    const displayedMemberCount = (comunidad.miembrosActuales || 0) + (justJoined ? 1 : 0);

    useEffect(() => {
        let cancelled = false;

        const loadPendingStatus = async () => {
            if (!currentUserId || joined || !isPrivate) {
                if (!cancelled) {
                    setRequestSent(false);
                }
                return;
            }

            try {
                const status = await communitiesApi.getMyRequestStatus(comunidad.id);
                if (!cancelled) {
                    setRequestSent(Boolean(status?.pending));
                }
            } catch {
                if (!cancelled) {
                    setRequestSent(false);
                }
            }
        };

        loadPendingStatus();

        return () => {
            cancelled = true;
        };
    }, [comunidad.id, currentUserId, isPrivate, joined]);

    const performJoin = async (role) => {
        setJoining(true);
        setError(null);
        try {
            if (isPrivate) {
                await communitiesApi.requestAccess(comunidad.id, '', role);
                setRequestSent(true);
                setShowRolePicker(false);
            } else {
                await communitiesApi.join(comunidad.id, role);
                setJoined(true);
                setJustJoined(true);
                setShowRolePicker(false);
                if (onJoined) onJoined(comunidad.id);
            }
        } catch (err) {
            if (err.message?.includes('401') || err.status === 401) {
                navigate('/login');
            } else if (err.message?.includes('409') || err.status === 409) {
                if (isPrivate) setRequestSent(true); else setJoined(true);
                setShowRolePicker(false);
            } else if (err.status === 400) {
                if (isPrivate) setRequestSent(true);
                else setError(err?.message || 'Error al unirse');
            } else {
                setError(isPrivate ? 'Error al solicitar acceso' : 'Error al unirse');
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

        try {
            const [subscriptionRaw, myCommunitiesRaw] = await Promise.all([
                subscriptionsApi.getMySubscription(),
                communitiesApi.listMine({ page: 0, size: 200 }),
            ]);

            const maxActiveCommunities = resolveMaxActiveCommunitiesByPlan(subscriptionRaw);
            const myActiveCommunities = extractCommunitiesFromResponse(myCommunitiesRaw).length;

            if (myActiveCommunities >= maxActiveCommunities) {
                setError(
                    `Has alcanzado el límite de ${maxActiveCommunities} comunidades activas para tu plan.`
                );
                return;
            }
        } catch {
            setError('No se pudo validar tu límite de comunidades activas. Inténtalo de nuevo.');
            return;
        }

        if (hasTeacherProfile) {
            setShowRolePicker(true);
            return;
        }

        // Sin perfil de tutor: siempre ALUMNO
        await performJoin('ALUMNO');
    };

    return (
        <div
            key={comunidad.id}
            className="comunidad-card"
            onClick={() => {
                if (isPrivate && !isMember) {
                    setError('No puedes ver una comunidad privada a la que no estás unido.');
                    return;
                }
                navigate(`/comunidades/${comunidad.id}`);
            }}
            style={{ cursor: 'pointer' }}
        >
            <img
                src={communityImage}
                alt={comunidad.nombre}
                className="comunidad-image"
                onError={(e) => {
                    e.currentTarget.onerror = null;
                    e.currentTarget.src = DEFAULT_COMMUNITY_IMAGE;
                }}
            />
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
                        <p>{displayedMemberCount}/ <span>{comunidad.maxMiembros || 0}</span></p>
                    </div>
                    {error && <span style={{ color: 'red', fontSize: '0.8rem' }}>{error}</span>}
                    {currentUserId && !joined && !requestSent && (
                        showRolePicker ? (
                            <div className="join-role-picker" onClick={(e) => e.stopPropagation()}>
                                <p className="join-role-picker__title">
                                    {isPrivate ? 'Elige cómo quieres solicitar acceso' : 'Elige cómo quieres unirte'}
                                </p>
                                <div className="join-role-picker__actions">
                                    <button
                                        className="join-button"
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            performJoin('PROFESOR');
                                        }}
                                        disabled={joining}
                                    >
                                        {joining
                                            ? (isPrivate ? 'Solicitando...' : 'Uniéndose...')
                                            : (isPrivate ? 'Solicitar como profesor' : 'Unirme como profesor')}
                                    </button>
                                    <button
                                        className="join-button join-button--request"
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            performJoin('ALUMNO');
                                        }}
                                        disabled={joining}
                                    >
                                        {joining
                                            ? (isPrivate ? 'Solicitando...' : 'Uniéndose...')
                                            : (isPrivate ? 'Solicitar como alumno' : 'Unirme como alumno')}
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

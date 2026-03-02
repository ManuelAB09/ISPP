import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import Header from '../../components/Header/Header';
import { communitiesApi } from '../../api/communities.api';
import { getApiBaseUrl } from '../../api/baseUrl';
import { useAuth } from '../../contexts/AuthContext';
import CommunityChat from './CommunityChat';
import PrivateChat from './PrivateChat';
import { obtenerConversaciones } from '../../api/mensajeService';
import './Chats.css';

const DEFAULT_COMMUNITY_IMAGE = 'https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&w=400&q=80';

const resolveCommunityImage = (community) => {
    const raw = community?.imagen || community?.imagenUrl || community?.foto;

    if (!raw || !String(raw).trim()) {
        return DEFAULT_COMMUNITY_IMAGE;
    }

    const value = String(raw).trim();
    if (/^https?:\/\//i.test(value) || value.startsWith('data:image/')) {
        return value;
    }

    const base = getApiBaseUrl();
    if (value.startsWith('/')) {
        return `${base}${value}`;
    }

    return `${base}/${value}`;
};

export default function Chats() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const { user } = useAuth();
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [communities, setCommunities] = useState([]);
    const [conversaciones, setConversaciones] = useState([]);
    const [selectedCommunityId, setSelectedCommunityId] = useState(null);
    const [privateTarget, setPrivateTarget] = useState(null);
    const [activeTab, setActiveTab] = useState('communities'); // 'communities' o 'private'
    const communityIdFromQuery = Number(searchParams.get('communityId'));
    const privateUserIdFromQuery = Number(searchParams.get('userId'));
    const privateUserNameFromQuery = searchParams.get('userName');
    const privateUserPhotoFromQuery = searchParams.get('userPhoto');

    const currentUser = {
        id: Number(localStorage.getItem('userId')),
        nombre: user?.nombre || 'Usuario',
        foto: user?.foto || null,
    };

    useEffect(() => {
        const token = localStorage.getItem('accessToken');
        if (!token) {
            navigate('/login');
            return;
        }

        const fetchData = async () => {
            try {
                setLoading(true);
                setError(null);

                // Cargar comunidades
                let page = 0;
                let totalPages = 1;
                const collected = [];

                while (page < totalPages) {
                    const response = await communitiesApi.listMine({ page, size: 50 });
                    const content = response?.content || [];
                    const pageInfo = response?.page;

                    collected.push(...content);
                    totalPages = pageInfo?.totalPages ?? 1;
                    page += 1;
                }

                setCommunities(collected);
                if (collected.length > 0) {
                    const existsInList = collected.some((community) => community.id === communityIdFromQuery);
                    setSelectedCommunityId(existsInList ? communityIdFromQuery : collected[0].id);
                }

                if (privateUserIdFromQuery) {
                    setPrivateTarget({
                        id: privateUserIdFromQuery,
                        nombre: privateUserNameFromQuery || `Usuario ${privateUserIdFromQuery}`,
                        foto: privateUserPhotoFromQuery || null,
                    });
                    setActiveTab('private');
                }

                // Cargar conversaciones
                try {
                    const { data } = await obtenerConversaciones();
                    setConversaciones(Array.isArray(data) ? data : []);
                } catch (err) {
                    console.error('Error al cargar conversaciones:', err);
                    setConversaciones([]);
                }
            } catch (err) {
                console.error('Error al cargar tus comunidades:', err);
                setError('No se pudieron cargar tus chats de comunidades.');
                setCommunities([]);
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, [
        navigate,
        communityIdFromQuery,
        privateUserIdFromQuery,
        privateUserNameFromQuery,
        privateUserPhotoFromQuery,
    ]);

    // Recargar conversaciones cuando se abre la pestaña de privados
    useEffect(() => {
        if (activeTab === 'private') {
            const fetchConversaciones = async () => {
                try {
                    const { data } = await obtenerConversaciones();
                    setConversaciones(Array.isArray(data) ? data : []);
                } catch (err) {
                    console.error('Error al cargar conversaciones:', err);
                    setConversaciones([]);
                }
            };
            fetchConversaciones();
        }
    }, [activeTab]);

    return (
        <>
            <Header page={'chats'} user={user} />
            <div className="chats-container">
                <div className="chats-header">
                    <h1>Chats</h1>
                    <p>Gestiona tus conversaciones de comunidades y chats privados.</p>
                </div>

                {/* Pestañas */}
                <div className="chats-tabs">
                    <button
                        className={`chats-tab ${activeTab === 'communities' ? 'active' : ''}`}
                        onClick={() => setActiveTab('communities')}
                    >
                        Comunidades
                    </button>
                    <button
                        className={`chats-tab ${activeTab === 'private' ? 'active' : ''}`}
                        onClick={() => setActiveTab('private')}
                    >
                        Privados
                    </button>
                </div>

                {loading && <p className="chats-loading">Cargando chats...</p>}
                {!loading && error && <p className="chats-error">{error}</p>}

                {/* Pestaña de Comunidades */}
                {activeTab === 'communities' && (
                    <>
                        {!loading && !error && communities.length === 0 && (
                            <div className="chats-empty">
                                <h3>No tienes chats de comunidades</h3>
                                <p>Únete a una comunidad para empezar a chatear.</p>
                                <button onClick={() => navigate('/comunidades')}>Explorar comunidades</button>
                            </div>
                        )}

                        {!loading && !error && communities.length > 0 && (
                            <div className="chats-layout">
                                <aside className="chats-sidebar">
                                    {communities.map((community) => {
                                        const isSelected = community.id === selectedCommunityId;
                                        return (
                                            <button
                                                key={community.id}
                                                type="button"
                                                className={`chat-list-item ${isSelected ? 'active' : ''}`}
                                                onClick={() => {
                                                    setSelectedCommunityId(community.id);
                                                    setPrivateTarget(null);
                                                }}
                                            >
                                                <img
                                                    src={resolveCommunityImage(community)}
                                                    alt={community.nombre}
                                                    className="chat-list-image"
                                                />
                                                <div className="chat-list-content">
                                                    <h3>{community.nombre}</h3>
                                                    <p>{community.descripcion || 'Sin descripción disponible.'}</p>
                                                </div>
                                            </button>
                                        );
                                    })}
                                </aside>

                                <section className="chats-main">
                                    {selectedCommunityId && communities.length > 0 ? (
                                        <CommunityChat
                                            comunidadId={selectedCommunityId}
                                            usuarioActual={currentUser}
                                            comunidadNombre={communities.find((c) => c.id === selectedCommunityId)?.nombre}
                                            comunidadImagen={resolveCommunityImage(
                                                communities.find((c) => c.id === selectedCommunityId)
                                            )}
                                            mode="embedded"
                                            initiallyOpen={true}
                                            onOpenPrivateChat={(target) => {
                                                setPrivateTarget({
                                                    id: Number(target.userId),
                                                    nombre: target.userName,
                                                    foto: target.userPhoto || null,
                                                });
                                                setActiveTab('private');
                                            }}
                                        />
                                    ) : (
                                        <div className="chats-main-empty">
                                            <h3>Selecciona una comunidad</h3>
                                            <p>Elige una comunidad de la izquierda para abrir su chat.</p>
                                        </div>
                                    )}
                                </section>
                            </div>
                        )}
                    </>
                )}

                {/* Pestaña de Privados */}
                {activeTab === 'private' && (
                    <>
                        {!loading && (privateTarget || conversaciones.length > 0) && (
                            <div className="chats-layout">
                                {conversaciones.length > 0 && (
                                    <aside className="chats-sidebar">
                                        {conversaciones.map((conv, idx) => {
                                            const isSelected = privateTarget?.id === conv.usuarioId;
                                            return (
                                                <button
                                                    key={idx}
                                                    type="button"
                                                    className={`chat-list-item ${isSelected ? 'active' : ''}`}
                                                    onClick={() =>
                                                        setPrivateTarget({
                                                            id: conv.usuarioId,
                                                            nombre: conv.usuarioNombre,
                                                            foto: conv.usuarioFoto || null,
                                                        })
                                                    }
                                                >
                                                    <img
                                                        src={conv.usuarioFoto || '/MeerKatters_logo.png'}
                                                        alt={conv.usuarioNombre}
                                                        className="chat-list-image"
                                                    />
                                                    <div className="chat-list-content">
                                                        <h3>{conv.usuarioNombre}</h3>
                                                        <p className="last-message">{conv.ultimoMensaje}</p>
                                                    </div>
                                                </button>
                                            );
                                        })}
                                    </aside>
                                )}

                                <section className="chats-main">
                                    {privateTarget ? (
                                        <PrivateChat
                                            tutorId={privateTarget.id}
                                            tutorNombre={privateTarget.nombre}
                                            usuarioActual={currentUser}
                                            onClose={() => setPrivateTarget(null)}
                                        />
                                    ) : (
                                        <div className="chats-main-empty">
                                            <h3>Selecciona una conversación</h3>
                                            <p>Elige una conversación de la izquierda para abrir el chat.</p>
                                        </div>
                                    )}
                                </section>
                            </div>
                        )}

                        {!loading && !privateTarget && conversaciones.length === 0 && (
                            <div className="chats-empty">
                                <h3>No tienes chats privados</h3>
                                <p>Cuando otros usuarios te escriban, aparecerán aquí.</p>
                            </div>
                        )}
                    </>
                )}
            </div>
        </>
    );
}

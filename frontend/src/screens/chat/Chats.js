import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { authApi } from '../../api/auth.api';
import { getApiBaseUrl } from '../../api/baseUrl';
import { communitiesApi } from '../../api/communities.api';
import { obtenerConversaciones } from '../../api/mensajeService';
import Header from '../../components/Header/Header';
import PageHeader from '../../components/PageHeader';
import { useAuth } from '../../contexts/AuthContext';
import { useNotificationContext } from '../../contexts/NotificationContext';
import './Chats.css';
import CommunityChat from './CommunityChat';
import PrivateChat from './PrivateChat';

const DEFAULT_COMMUNITY_IMAGE = 'https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&w=400&q=80';
const DEFAULT_PROFILE_AVATAR =
    "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 120 120'%3E%3Ccircle cx='60' cy='60' r='60' fill='%23E6EAF3'/%3E%3Ccircle cx='60' cy='46' r='22' fill='%2395A1BB'/%3E%3Cpath d='M20 106c6-20 22-32 40-32s34 12 40 32' fill='%2395A1BB'/%3E%3C/svg%3E";

export const resolveCommunityImage = (community) => {
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

const resolveUserImage = (rawPhoto) => {
    const fallback = DEFAULT_PROFILE_AVATAR;
    if (!rawPhoto || !String(rawPhoto).trim()) {
        return fallback;
    }

    const value = String(rawPhoto).trim();
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
    const { isChatMuted, toggleChatMuted, communityUnreadById, clearCommunityUnread } = useNotificationContext();
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [communities, setCommunities] = useState([]);
    const [conversaciones, setConversaciones] = useState([]);
    const [selectedCommunityId, setSelectedCommunityId] = useState(null);
    const [privateTarget, setPrivateTarget] = useState(null);
    const [activeTab, setActiveTab] = useState(
        searchParams.get('userId') ? 'private' : 'communities'
    ); // 'communities' o 'private'
    const [isMobileDropdownOpen, setIsMobileDropdownOpen] = useState(false);

    // lista que se mostrará en la barra lateral de privados; incluye el target cuando
    // no hay conversaciones serializadas para que siempre aparezca al menos ese usuario.
    const sidebarConversations =
        conversaciones.length > 0
            ? conversaciones
            : privateTarget
            ? [
                  {
                      usuarioId: privateTarget.id,
                      usuarioNombre: privateTarget.nombre,
                      usuarioFoto: privateTarget.foto || null,
                      usuarioFotoBackgroundColor: privateTarget.fotoBackgroundColor || '#ffffff',
                      ultimoMensaje: '',
                  },
              ]
            : [];
    const hasSidebar = sidebarConversations.length > 0;
    const communityIdFromQuery = Number(searchParams.get('communityId'));
    const privateUserIdFromQuery = Number(searchParams.get('userId'));

    const currentUser = {
        id: Number(localStorage.getItem('userId')),
        nombre: user?.nombre || 'Usuario',
        foto: user?.foto || null,
        fotoBackgroundColor: user?.fotoBackgroundColor || '#ffffff',
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
                    const nextSelectedId = existsInList ? communityIdFromQuery : collected[0].id;
                    setSelectedCommunityId(nextSelectedId);
                    clearCommunityUnread(nextSelectedId);
                }

                if (privateUserIdFromQuery) {
                    try {
                        const { data: profile } = await authApi.getUserPublicProfile(privateUserIdFromQuery);
                        const targetObj = {
                            id: profile.id,
                            nombre: profile.nombre || `Usuario ${privateUserIdFromQuery}`,
                            foto: profile.foto || null,
                            fotoBackgroundColor: '#ffffff',
                        };
                        setPrivateTarget(targetObj);
                        setActiveTab('private');
                        setConversaciones((prev) => {
                            if (prev.some((c) => c.usuarioId === profile.id)) {
                                return prev;
                            }
                            return [
                                ...prev,
                                {
                                    usuarioId: profile.id,
                                    usuarioNombre: targetObj.nombre,
                                    usuarioFoto: targetObj.foto,
                                    usuarioFotoBackgroundColor: targetObj.fotoBackgroundColor,
                                    ultimoMensaje: '',
                                },
                            ];
                        });
                    } catch (err) {
                        console.error('Error al cargar perfil del usuario:', err);
                    }
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
    }, [navigate, communityIdFromQuery, privateUserIdFromQuery, clearCommunityUnread]);

    // Recargar conversaciones cuando se abre la pestaña de privados
    useEffect(() => {
        if (activeTab === 'private') {
            const fetchConversaciones = async () => {
                try {
                    const { data } = await obtenerConversaciones();
                    const serverList = Array.isArray(data) ? data : [];
                    setConversaciones((prev) => {
                        // si ya tenemos un target seleccionado que no está en el servidor,
                        // agréguelo para que la barra lateral muestre al usuario clicado
                        if (
                            privateTarget &&
                            !serverList.some((c) => c.usuarioId === privateTarget.id)
                        ) {
                            return [
                                ...serverList,
                                {
                                    usuarioId: privateTarget.id,
                                    usuarioNombre: privateTarget.nombre,
                                    usuarioFoto: privateTarget.foto || null,
                                    usuarioFotoBackgroundColor: privateTarget.fotoBackgroundColor || '#ffffff',
                                    ultimoMensaje: '',
                                },
                            ];
                        }
                        return serverList;
                    });
                } catch (err) {
                    console.error('Error al cargar conversaciones:', err);
                    setConversaciones((prev) => {
                        // keep any existing manual conversation when fetch fails
                        if (
                            privateTarget &&
                            !prev.some((c) => c.usuarioId === privateTarget.id)
                        ) {
                            return [
                                ...prev,
                                {
                                    usuarioId: privateTarget.id,
                                    usuarioNombre: privateTarget.nombre,
                                    usuarioFoto: privateTarget.foto || null,
                                    usuarioFotoBackgroundColor: privateTarget.fotoBackgroundColor || '#ffffff',
                                    ultimoMensaje: '',
                                },
                            ];
                        }
                        return prev;
                    });
                }
            };
            fetchConversaciones();
        }
    }, [activeTab, privateTarget]);

    useEffect(() => {
        if (selectedCommunityId) {
            clearCommunityUnread(selectedCommunityId);
        }
    }, [selectedCommunityId, clearCommunityUnread]);

    const renderMuteButton = (chatType, chatId) => {
        if (chatId === null || chatId === undefined) return null;

        const muted = isChatMuted(chatType, chatId);
        const label = muted ? 'Activar notificaciones' : 'Silenciar chat';

        return (
            <button
                type="button"
                className={`chat-mute-toggle ${muted ? 'muted' : ''}`}
                onClick={() => toggleChatMuted(chatType, chatId)}
                title={muted ? 'Reactivar notificaciones del chat' : 'Silenciar notificaciones del chat'}
            >
                {label}
            </button>
        );
    };

    return (
        <>
            <Header page={'chats'} user={user} />
            <div className="chats-container">
                <div className="chats-header">
                    <PageHeader 
                        title="Chats"
                        subtitle="Accede a los chats de todas las comunidades donde eres miembro"
                    />
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
                                {/* Selector móvil */}
                                <div className="chats-mobile-selector">
                                    {isMobileDropdownOpen && (
                                        <div 
                                            className="mobile-selector-overlay"
                                            onClick={() => setIsMobileDropdownOpen(false)}
                                        />
                                    )}
                                    <button 
                                        className="mobile-selector-button"
                                        onClick={() => setIsMobileDropdownOpen(!isMobileDropdownOpen)}
                                    >
                                        <img
                                            src={resolveCommunityImage(communities.find((c) => c.id === selectedCommunityId))}
                                            alt="Comunidad actual"
                                            className="mobile-selector-image"
                                        />
                                        <span className="mobile-selector-text">
                                            {communities.find((c) => c.id === selectedCommunityId)?.nombre || 'Selecciona una comunidad'}
                                        </span>
                                        <svg 
                                            width="20" 
                                            height="20" 
                                            viewBox="0 0 20 20" 
                                            fill="none" 
                                            className={`mobile-selector-arrow ${isMobileDropdownOpen ? 'open' : ''}`}
                                        >
                                            <path d="M5 7.5L10 12.5L15 7.5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                                        </svg>
                                    </button>
                                    {isMobileDropdownOpen && (
                                        <div className="mobile-selector-dropdown">
                                            {communities.map((community) => (
                                                <button
                                                    key={community.id}
                                                    type="button"
                                                    className={`mobile-dropdown-item ${community.id === selectedCommunityId ? 'active' : ''}`}
                                                    onClick={() => {
                                                        setSelectedCommunityId(community.id);
                                                        clearCommunityUnread(community.id);
                                                        setPrivateTarget(null);
                                                        setIsMobileDropdownOpen(false);
                                                    }}
                                                >
                                                    <div className="community-icon-with-badge">
                                                        <img
                                                            src={resolveCommunityImage(community)}
                                                            alt={community.nombre}
                                                            className="mobile-dropdown-image"
                                                        />
                                                        {communityUnreadById[String(community.id)] > 0 && (
                                                            <span className="community-unread-badge">
                                                                {communityUnreadById[String(community.id)]}
                                                            </span>
                                                        )}
                                                    </div>
                                                    <span>{community.nombre}</span>
                                                </button>
                                            ))}
                                        </div>
                                    )}
                                </div>

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
                                                    clearCommunityUnread(community.id);
                                                    setPrivateTarget(null);
                                                }}
                                            >
                                                <div className="community-icon-with-badge">
                                                    <img
                                                        src={resolveCommunityImage(community)}
                                                        alt={community.nombre}
                                                        className="chat-list-image"
                                                    />
                                                    {communityUnreadById[String(community.id)] > 0 && (
                                                        <span className="community-unread-badge">
                                                            {communityUnreadById[String(community.id)]}
                                                        </span>
                                                    )}
                                                </div>
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
                                            headerActions={renderMuteButton('community', selectedCommunityId)}
                                            onOpenPrivateChat={(target) => {
                                                const id = Number(target.userId);
                                                const nombre = target.userName;
                                                const foto = target.userPhoto || null;
                                                const fotoBackgroundColor = target.userPhotoBg || '#ffffff';
                                                setPrivateTarget({
                                                    id,
                                                    nombre,
                                                    foto,
                                                    fotoBackgroundColor,
                                                });
                                                setActiveTab('private');
                                                setConversaciones((prev) => {
                                                    if (prev.some((c) => c.usuarioId === id)) {
                                                        return prev;
                                                    }
                                                    return [
                                                        ...prev,
                                                        {
                                                            usuarioId: id,
                                                            usuarioNombre: nombre,
                                                            usuarioFoto: foto,
                                                            usuarioFotoBackgroundColor: fotoBackgroundColor,
                                                            ultimoMensaje: '',
                                                        },
                                                    ];
                                                });
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
                            <div className={`chats-layout ${hasSidebar ? '' : 'no-sidebar'}`}>
                                {/* Selector móvil para privados */}
                                {hasSidebar && (
                                    <div className="chats-mobile-selector">
                                        {isMobileDropdownOpen && (
                                            <div 
                                                className="mobile-selector-overlay"
                                                onClick={() => setIsMobileDropdownOpen(false)}
                                            />
                                        )}
                                        <button 
                                            className="mobile-selector-button"
                                            onClick={() => setIsMobileDropdownOpen(!isMobileDropdownOpen)}
                                        >
                                            {privateTarget ? (
                                                <>
                                                    <img
                                                        src={resolveUserImage(privateTarget.foto)}
                                                        alt={privateTarget.nombre}
                                                        className="mobile-selector-image"
                                                        style={{ backgroundColor: privateTarget.fotoBackgroundColor || '#ffffff' }}
                                                    />
                                                    <span className="mobile-selector-text">
                                                        {privateTarget.nombre}
                                                    </span>
                                                </>
                                            ) : (
                                                <span className="mobile-selector-text">Selecciona una conversación</span>
                                            )}
                                            <svg 
                                                width="20" 
                                                height="20" 
                                                viewBox="0 0 20 20" 
                                                fill="none" 
                                                className={`mobile-selector-arrow ${isMobileDropdownOpen ? 'open' : ''}`}
                                            >
                                                <path d="M5 7.5L10 12.5L15 7.5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                                            </svg>
                                        </button>
                                        {isMobileDropdownOpen && (
                                            <div className="mobile-selector-dropdown">
                                                {sidebarConversations.map((conv, idx) => (
                                                    <button
                                                        key={idx}
                                                        type="button"
                                                        className={`mobile-dropdown-item ${privateTarget?.id === conv.usuarioId ? 'active' : ''}`}
                                                        onClick={() => {
                                                            setPrivateTarget({
                                                                id: conv.usuarioId,
                                                                nombre: conv.usuarioNombre,
                                                                foto: conv.usuarioFoto || null,
                                                                fotoBackgroundColor: conv.usuarioFotoBackgroundColor || '#ffffff',
                                                            });
                                                            setIsMobileDropdownOpen(false);
                                                        }}
                                                    >
                                                        <img
                                                            src={resolveUserImage(conv.usuarioFoto)}
                                                            alt={conv.usuarioNombre}
                                                            className="mobile-dropdown-image"
                                                            style={{ backgroundColor: conv.usuarioFotoBackgroundColor || '#ffffff' }}
                                                        />
                                                        <span>{conv.usuarioNombre}</span>
                                                    </button>
                                                ))}
                                            </div>
                                        )}
                                    </div>
                                )}

                                {hasSidebar && (
                                    <aside className="chats-sidebar">
                                        {sidebarConversations.map((conv, idx) => {
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
                                                            fotoBackgroundColor: conv.usuarioFotoBackgroundColor || '#ffffff',
                                                        })
                                                    }
                                                >
                                                    <img
                                                        src={resolveUserImage(conv.usuarioFoto)}
                                                        alt={conv.usuarioNombre}
                                                        className="chat-list-image"
                                                        style={{
                                                            backgroundColor:
                                                                conv.usuarioFotoBackgroundColor ||
                                                                (privateTarget?.id === conv.usuarioId
                                                                    ? privateTarget.fotoBackgroundColor
                                                                    : '#ffffff'),
                                                        }}
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
                                            headerActions={renderMuteButton('private', privateTarget.id)}
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

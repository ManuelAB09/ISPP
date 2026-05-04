import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { authApi } from '../api/auth.api';
import { getApiBaseUrl } from '../api/baseUrl';
import { communitiesApi } from '../api/communities.api';
import { obtenerConversaciones, obtenerHistorialComunidad } from '../api/mensajeService';
import { getAllEventAlerts, getAllUserNotifications } from '../api/notificationService';
import { useNotifications } from '../hooks/useNotifications';
import { resolveCommunityImage } from '../screens/chat/Chats';
import { useAuth } from './AuthContext';
import { useSocketContext } from './SocketContext';

const CHAT_MUTE_STORAGE_KEY = 'mutedChatNotificationsByUser';

const DEFAULT_PROFILE_AVATAR =
    "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 120 120'%3E%3Ccircle cx='60' cy='60' r='60' fill='%23E6EAF3'/%3E%3Ccircle cx='60' cy='46' r='22' fill='%2395A1BB'/%3E%3Cpath d='M20 106c6-20 22-32 40-32s34 12 40 32' fill='%2395A1BB'/%3E%3C/svg%3E";

const normalizeMutedChats = (value) => {
    if (!value || typeof value !== 'object') {
        return { private: {}, community: {} };
    }

    const privateChats = value.private && typeof value.private === 'object' ? value.private : {};
    const communityChats = value.community && typeof value.community === 'object' ? value.community : {};

    return {
        private: privateChats,
        community: communityChats,
    };
};

const escapeRegex = (value) => String(value || '').replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

const NotificationContext = createContext(null);

export const NotificationProvider = ({ children }) => {
    const location = useLocation();
    const navigate = useNavigate();
    const { isAuthenticated, user } = useAuth();
    const { socket, isConnected } = useSocketContext();
    const { permission, requestPermission, showNotification, isSupported } = useNotifications();
    const knownConversationsRef = useRef(new Map());
    const conversationUsersRef = useRef(new Map());
    const shownNotificationsRef = useRef(new Set());
    const [hasRequestedPermission, setHasRequestedPermission] = useState(false);
    const [notificationsEnabled, setNotificationsEnabled] = useState(() => {
        const saved = localStorage.getItem('notificationsEnabled');
        return saved !== null ? JSON.parse(saved) : false;
    });
    const [mutedChats, setMutedChats] = useState({ private: {}, community: {} });
    const [panelUnreadCount, setPanelUnreadCount] = useState(0);
    const [communityUnreadById, setCommunityUnreadById] = useState({});
    const initializedRef = useRef(false);

    const toAbsoluteImageUrl = (imageUrl, fallback = DEFAULT_PROFILE_AVATAR) => {
        const raw = imageUrl || fallback;
        if (!raw) return fallback;
        if (raw.startsWith('http://') || raw.startsWith('https://') || raw.startsWith('data:') || raw.startsWith('blob:')) {
            return raw;
        }
        try {
            return new URL(raw, getApiBaseUrl() || window.location.origin).toString();
        } catch {
            return fallback;
        }
    };

    const sanitizeNotificationText = (text) => {
        return String(text || '')
            .replace(/https?:\/\/localhost:\d+\S*/gi, 'enlace')
            .replace(/https?:\/\/\S+/gi, 'enlace');
    };

    const truncate = (text, max = 72) => {
        const value = String(text || '');
        return value.length > max ? `${value.substring(0, max)}...` : value;
    };

    const showPrettyNotification = useCallback((title, body, avatarUrl, tag, onClick) => {
        const cleanTitle = truncate(sanitizeNotificationText(title), 42);
        const cleanBody = truncate(sanitizeNotificationText(body), 72);

        showNotificationRef.current(
            cleanTitle,
            {
                body: cleanBody,
                icon: toAbsoluteImageUrl(avatarUrl, DEFAULT_PROFILE_AVATAR),
                badge: '/favicon.ico',
                tag,
                requireInteraction: false,
            },
            onClick
        );
    }, []);

    const hasUserMention = useCallback((content) => {
        const userName = String(user?.nombre || '').trim();
        if (!content || !userName) return false;
        const mentionRegex = new RegExp(`@${escapeRegex(userName)}(?![\\w-])`, 'i');
        return mentionRegex.test(String(content));
    }, [user?.nombre]);

    const isChatMuted = useCallback((chatType, chatId) => {
        if (!chatType || chatId === null || chatId === undefined) return false;
        const chatMap = mutedChats[chatType] || {};
        return Boolean(chatMap[String(chatId)]);
    }, [mutedChats]);

    const toggleChatMuted = useCallback((chatType, chatId) => {
        if (!chatType || chatId === null || chatId === undefined) return;

        setMutedChats((prev) => {
            const current = normalizeMutedChats(prev);
            const chatMap = current[chatType] || {};
            const key = String(chatId);

            return {
                ...current,
                [chatType]: {
                    ...chatMap,
                    [key]: !Boolean(chatMap[key]),
                },
            };
        });
    }, []);

    const incrementCommunityUnread = useCallback((communityId) => {
        if (communityId === null || communityId === undefined) return;
        const key = String(communityId);
        setCommunityUnreadById((prev) => ({
            ...prev,
            [key]: (prev[key] || 0) + 1,
        }));
    }, []);

    const clearCommunityUnread = useCallback((communityId) => {
        if (communityId === null || communityId === undefined) return;
        const key = String(communityId);
        setCommunityUnreadById((prev) => {
            if (!prev[key]) return prev;
            return {
                ...prev,
                [key]: 0,
            };
        });
    }, []);

    // NUEVO: inicializa el mapa de no leídos desde el servidor sin pisar
    // los incrementos que ya haya acumulado el tiempo real
    const initCommunityUnread = useCallback((unreadMap) => {
        if (!unreadMap || typeof unreadMap !== 'object') return;
        setCommunityUnreadById((prev) => {
            const next = { ...prev };
            Object.entries(unreadMap).forEach(([id, count]) => {
                const key = String(id);
                const serverCount = Number(count) || 0;
                // Usar el mayor entre lo que ya tenemos (tiempo real) y el servidor
                next[key] = Math.max(prev[key] || 0, serverCount);
            });
            return next;
        });
    }, []);

    const markOnePanelNotificationRead = useCallback(() => {
        setPanelUnreadCount((prev) => Math.max(0, prev - 1));
    }, []);

    const clearPanelNotificationsUnread = useCallback(() => {
        setPanelUnreadCount(0);
    }, []);

    const setPanelNotificationsUnreadCount = useCallback((count) => {
        const numeric = Number(count);
        setPanelUnreadCount(Number.isFinite(numeric) && numeric > 0 ? Math.floor(numeric) : 0);
    }, []);

    const refreshPanelUnreadCount = useCallback(() => {
        if (!isAuthenticated || !notificationsEnabled) {
            setPanelUnreadCount(0);
            return;
        }

        Promise.all([getAllEventAlerts(), getAllUserNotifications()])
            .then(([eventAlerts, userNotifications]) => {
                const eventUnread = (Array.isArray(eventAlerts) ? eventAlerts : []).filter((n) => !n?.leida).length;
                const notifUnread = (Array.isArray(userNotifications) ? userNotifications : []).filter((n) => !n?.leida).length;
                setPanelUnreadCount(eventUnread + notifUnread);
            })
            .catch((error) => {
                console.error('Error al refrescar el contador de notificaciones:', error);
            });
    }, [isAuthenticated, notificationsEnabled]);

    const getUserData = useCallback((userId) => {
        const cacheKey = String(userId);
        const cached = conversationUsersRef.current.get(cacheKey);

        if (cached && cached.nombre) {
            return Promise.resolve(cached);
        }

        return authApi.getUserPublicProfile(userId)
            ?.then((response) => {
                const userData = {
                    nombre: response?.data?.nombre || response?.data?.name,
                    foto: response?.data?.foto || response?.data?.avatarUrl,
                };
                conversationUsersRef.current.set(cacheKey, userData);
                return userData;
            })
            .catch(() => {
                return { nombre: null, foto: null };
            });
    }, []);

    const shouldSkipNotification = useCallback((senderId) => {
        if (permission !== 'granted' || !notificationsEnabled) return true;
        if (senderId != null && Number(senderId) === Number(user?.id)) return true;
        return false;
    }, [permission, notificationsEnabled, user?.id]);

    const isInChatRoute = location.pathname === '/chats';

    const isInChatRouteRef = useRef(isInChatRoute);
    isInChatRouteRef.current = isInChatRoute;
    const navigateRef = useRef(navigate);
    navigateRef.current = navigate;
    const showNotificationRef = useRef(showNotification);
    showNotificationRef.current = showNotification;

    useEffect(() => {
        if (isAuthenticated && isSupported && !hasRequestedPermission && permission === 'default') {
            const timer = setTimeout(() => {
                requestPermission().then(() => {
                    setHasRequestedPermission(true);
                });
            }, 2000);

            return () => clearTimeout(timer);
        }
    }, [isAuthenticated, isSupported, hasRequestedPermission, permission, requestPermission]);

    useEffect(() => {
        if (!isAuthenticated || initializedRef.current) return;
        initializedRef.current = true;

        const seedKnown = async () => {
            try {
                const { data: privateData } = await obtenerConversaciones();
                const conversations = Array.isArray(privateData) ? privateData : [];
                conversations.forEach((conv) => {
                    knownConversationsRef.current.set(
                        `user-${conv.usuarioId}`,
                        conv.ultimoMensaje || ''
                    );
                    conversationUsersRef.current.set(String(conv.usuarioId), {
                        nombre: conv.usuarioNombre,
                        foto: conv.usuarioFoto,
                    });
                });
            } catch (error) { console.error('Error al obtener conversaciones:', error); }

            try {
                const { content: communities } = await communitiesApi.listMine({ page: 0, size: 100 });
                for (const community of (communities || [])) {
                    try {
                        const { data: messages } = await obtenerHistorialComunidad(community.id);
                        const lastMessage = messages && messages.length > 0 ? messages[messages.length - 1] : null;
                        if (lastMessage) {
                            knownConversationsRef.current.set(
                                `community-${community.id}`,
                                lastMessage.contenido || ''
                            );
                        }
                    } catch (error) { console.error('Error al obtener historial de comunidad:', error); }
                }
            } catch (error) { console.error('Error al obtener comunidades:', error); }
        };

        seedKnown();
    }, [isAuthenticated]);

    useEffect(() => {
        if (!isAuthenticated) {
            setPanelUnreadCount(0);
            setCommunityUnreadById({});
            return;
        }

        if (!notificationsEnabled) {
            setPanelUnreadCount(0);
            return;
        }

        refreshPanelUnreadCount();

        const intervalId = window.setInterval(() => {
            refreshPanelUnreadCount();
        }, 30000);

        const handleFocus = () => refreshPanelUnreadCount();
        const handleVisibility = () => {
            if (!document.hidden) refreshPanelUnreadCount();
        };

        window.addEventListener('focus', handleFocus);
        document.addEventListener('visibilitychange', handleVisibility);

        return () => {
            window.clearInterval(intervalId);
            window.removeEventListener('focus', handleFocus);
            document.removeEventListener('visibilitychange', handleVisibility);
        };
    }, [isAuthenticated, notificationsEnabled, refreshPanelUnreadCount]);

    useEffect(() => {
        if (!socket || !isConnected || !isAuthenticated) return;

        const handleDM = (msg) => {
            if (!msg || !msg.id) return;
            if (shouldSkipNotification(msg.emisorId)) return;
            if (isChatMuted('private', msg.emisorId)) return;

            if (msg.contenido === "¡Hola! Me gustaría contactar contigo.") return;
            const notificationId = `dm-${msg.id}`;
            const alreadyShown = shownNotificationsRef.current.has(notificationId);

            if (alreadyShown) return;
            shownNotificationsRef.current.add(notificationId);

            const userKey = String(msg.emisorId);
            const cachedUser = conversationUsersRef.current.get(userKey);
            const senderName = msg.emisorNombre?.trim() || cachedUser?.nombre;
            const senderPhoto = msg.emisorFoto || cachedUser?.foto;

            if (senderName) {
                showPrettyNotification(
                    `Mensaje de ${senderName}`,
                    msg.contenido || 'Tienes un nuevo mensaje',
                    senderPhoto,
                    `msg-${msg.emisorId}-${msg.id}`,
                    () => { navigateRef.current(`/chats?userId=${msg.emisorId}`); }
                );
            } else {
                getUserData(msg.emisorId)
                    .then((userData) => {
                        showPrettyNotification(
                            `Mensaje de ${userData?.nombre || 'Alguien'}`,
                            msg.contenido || 'Tienes un nuevo mensaje',
                            userData?.foto || senderPhoto,
                            `msg-${msg.emisorId}-${msg.id}`,
                            () => { navigateRef.current(`/chats?userId=${msg.emisorId}`); }
                        );
                    })
                    .catch(() => {
                        showPrettyNotification(
                            'Mensaje de Alguien',
                            msg.contenido || 'Tienes un nuevo mensaje',
                            senderPhoto,
                            `msg-${msg.emisorId}-${msg.id}`,
                            () => { navigateRef.current(`/chats?userId=${msg.emisorId}`); }
                        );
                    });
            }
        };

        const handleSolicitudContratacion = (solicitud) => {
            if (!solicitud) return;
            refreshPanelUnreadCount();
            if (shouldSkipNotification(solicitud.alumnoId)) return;

            showPrettyNotification(
                '📋 Nueva solicitud de contratación',
                `${solicitud.alumnoNombre || 'Un alumno'} te ha solicitado una clase`,
                solicitud.alumnoFoto,
                `solicitud-${solicitud.id}`,
                () => { navigateRef.current('/mis-solicitudes'); }
            );
        };

        const handleSolicitudRespuesta = (solicitud) => {
            if (!solicitud) return;
            refreshPanelUnreadCount();
            if (shouldSkipNotification(solicitud.tutorId)) return;

            const estado = solicitud.estado || 'ACEPTADA';
            let title, body;

            if (estado === 'ACEPTADA') {
                title = '✅ Solicitud aceptada';
                body = `${solicitud.tutorNombre || 'El tutor'} ha aceptado tu solicitud`;
            } else if (estado === 'RECHAZADA') {
                title = '❌ Solicitud rechazada';
                body = `${solicitud.tutorNombre || 'El tutor'} ha rechazado tu solicitud`;
            } else {
                title = '📋 Actualización de solicitud';
                body = `Tu solicitud ha sido ${estado.toLowerCase()}`;
            }

            showPrettyNotification(title, body, solicitud.tutorFoto, `solicitud-respuesta-${solicitud.id}`,
                () => { navigateRef.current('/mis-solicitudes'); }
            );
        };

        const handleSolicitudPagada = (solicitud) => {
            if (!solicitud) return;
            refreshPanelUnreadCount();
            if (shouldSkipNotification(solicitud.alumnoId)) return;

            showPrettyNotification(
                '💰 Clase pagada y confirmada',
                `${solicitud.alumnoNombre || 'El alumno'} ha completado el pago. Clase confirmada.`,
                solicitud.alumnoFoto,
                `solicitud-pagada-${solicitud.id}`,
                () => { navigateRef.current('/mis-solicitudes'); }
            );
        };

        const handleAlertsCount = (data) => {
            if (!data) return;

            if (typeof data === 'number') {
                setPanelNotificationsUnreadCount(data);
                return;
            }

            const candidates = [data.total, data.count, data.unread, data.unreadCount, data.alertsCount];
            const numeric = candidates.find((value) => Number.isFinite(Number(value)));
            if (numeric !== undefined) {
                setPanelNotificationsUnreadCount(Number(numeric));
                return;
            }

            refreshPanelUnreadCount();
        };

        const handleUserNotification = (notification) => {
            if (!notification) return;

            if (notification?.tipo === 'EVENT_ALERT') {
                if (permission === 'granted' && notificationsEnabled) {
                    showPrettyNotification(
                        notification?.comunidadNombre || 'Recordatorio de evento',
                        notification?.mensaje || `Tienes un evento próximo: ${notification?.eventoTitulo || ''}`,
                        DEFAULT_PROFILE_AVATAR,
                        `event-alert-${notification?.eventoId || Date.now()}`,
                        () => {
                            if (notification?.eventoId) {
                                navigateRef.current(`/eventos/${notification.eventoId}`);
                            } else {
                                navigateRef.current('/notifications');
                            }
                        }
                    );
                }
            }

            if (!notification.leida) {
                setPanelUnreadCount((prev) => prev + 1);
            }

            refreshPanelUnreadCount();
        };

        const handleCommunityMessage = (msg) => {
            if (!msg || !msg.comunidadId) return;

            if (Number(msg.usuarioId) !== Number(user?.id)) {
                incrementCommunityUnread(msg.comunidadId);
            }

            const isMentioned = hasUserMention(msg.contenido) && Number(msg.usuarioId) !== Number(user?.id);
            if (shouldSkipNotification(msg.usuarioId)) return;
            if (isChatMuted('community', msg.comunidadId) && !isMentioned) return;

            const key = `community-${msg.comunidadId}`;
            const known = knownConversationsRef.current;
            const current = msg.contenido || msg.archivoNombre || '';
            const prev = known.get(key);

            if (prev !== undefined && prev === current) return;
            known.set(key, current);

            const fakeCommunity = { imagen: msg.comunidadImagenUrl, imagenUrl: msg.comunidadImagenUrl, foto: msg.comunidadImagenUrl };
            const communityImage = resolveCommunityImage(fakeCommunity);

            if (isMentioned) {
                showPrettyNotification(
                    `Mención en ${msg.comunidadNombre || 'Comunidad'}`,
                    `${msg.usuarioNombre || 'Alguien'} te mencionó: ${msg.contenido}`,
                    communityImage,
                    `mention-${msg.comunidadId}-${msg.id || Date.now()}`,
                    () => { navigateRef.current(`/chats?communityId=${msg.comunidadId}`); }
                );
                return;
            }

            showPrettyNotification(
                `${msg.comunidadNombre || 'Comunidad'}`,
                msg.contenido
                    ? `${msg.usuarioNombre || 'Alguien'}: ${msg.contenido}`
                    : `${msg.usuarioNombre || 'Alguien'} ha enviado un archivo en la comunidad`,
                communityImage,
                `community-msg-${msg.comunidadId}-${msg.id || Date.now()}`,
                () => { navigateRef.current(`/chats?communityId=${msg.comunidadId}`); }
            );
        };

        socket.on('dm_message', handleDM);
        socket.on('solicitud_contratacion', handleSolicitudContratacion);
        socket.on('solicitud_contratacion_respuesta', handleSolicitudRespuesta);
        socket.on('solicitud_contratacion_pagada', handleSolicitudPagada);
        socket.on('alerts_count', handleAlertsCount);
        socket.on('notificaciones', handleUserNotification);
        socket.on('community_message', handleCommunityMessage);

        return () => {
            socket.off('dm_message', handleDM);
            socket.off('solicitud_contratacion', handleSolicitudContratacion);
            socket.off('solicitud_contratacion_respuesta', handleSolicitudRespuesta);
            socket.off('solicitud_contratacion_pagada', handleSolicitudPagada);
            socket.off('alerts_count', handleAlertsCount);
            socket.off('notificaciones', handleUserNotification);
            socket.off('community_message', handleCommunityMessage);
        };
    }, [
        socket,
        isConnected,
        isAuthenticated,
        permission,
        notificationsEnabled,
        user?.id,
        user?.nombre,
        mutedChats,
        hasUserMention,
        isChatMuted,
        shouldSkipNotification,
        showPrettyNotification,
        incrementCommunityUnread,
        setPanelNotificationsUnreadCount,
        refreshPanelUnreadCount,
        getUserData,
    ]);

    useEffect(() => {
        if ((isInChatRoute || location.pathname.startsWith('/comunidades/')) && isAuthenticated) {
            obtenerConversaciones()
                .then(({ data }) => {
                    const conversations = Array.isArray(data) ? data : [];
                    const newMap = knownConversationsRef.current;
                    conversations.forEach((conv) => {
                        newMap.set(`user-${conv.usuarioId}`, conv.ultimoMensaje || '');
                        conversationUsersRef.current.set(String(conv.usuarioId), {
                            nombre: conv.usuarioNombre,
                            foto: conv.usuarioFoto,
                        });
                    });
                })
                .catch((error) => { console.error('Error al obtener conversaciones (ruta chats/comunidades):', error); });
        }
    }, [isInChatRoute, location.pathname, isAuthenticated]);

    useEffect(() => {
        if (user?.notificacionesPush != null) {
            setNotificationsEnabled(user.notificacionesPush);
        }
    }, [user?.notificacionesPush]);

    useEffect(() => {
        localStorage.setItem('notificationsEnabled', JSON.stringify(notificationsEnabled));
    }, [notificationsEnabled]);

    useEffect(() => {
        if (!user?.id) {
            setMutedChats({ private: {}, community: {} });
            return;
        }

        const key = `${CHAT_MUTE_STORAGE_KEY}:${user.id}`;
        const saved = localStorage.getItem(key);

        if (!saved) {
            setMutedChats({ private: {}, community: {} });
            return;
        }

        try {
            setMutedChats(normalizeMutedChats(JSON.parse(saved)));
        } catch {
            setMutedChats({ private: {}, community: {} });
        }
    }, [user?.id]);

    useEffect(() => {
        if (!user?.id) return;
        const key = `${CHAT_MUTE_STORAGE_KEY}:${user.id}`;
        localStorage.setItem(key, JSON.stringify(normalizeMutedChats(mutedChats)));
    }, [mutedChats, user?.id]);

    const toggleNotifications = () => {
        setNotificationsEnabled(prev => !prev);
    };

    const value = {
        permission,
        requestPermission,
        showNotification,
        isSupported,
        notificationsEnabled,
        toggleNotifications,
        panelUnreadCount,
        markOnePanelNotificationRead,
        clearPanelNotificationsUnread,
        setPanelNotificationsUnreadCount,
        communityUnreadById,
        clearCommunityUnread,
        initCommunityUnread,   // NUEVO
        mutedChats,
        isChatMuted,
        toggleChatMuted,
    };

    return (
        <NotificationContext.Provider value={value}>
            {children}
        </NotificationContext.Provider>
    );
};

export const useNotificationContext = () => {
    return useContext(NotificationContext);
};
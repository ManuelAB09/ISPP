import React, { createContext, useContext, useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useNotifications } from '../hooks/useNotifications';
import { obtenerConversaciones, obtenerHistorialComunidad } from '../api/mensajeService';
import { communitiesApi } from '../api/communities.api';
import { useAuth } from './AuthContext';
import { useSocketContext } from './SocketContext';

/**
 * Contexto para manejar notificaciones push de mensajes en toda la aplicación.
 */
const NotificationContext = createContext(null);

/**
 * Provider de notificaciones push que funciona en toda la aplicación.
 * - Solicita permisos de notificaciones al usuario
 * - Usa WebSocket para detectar mensajes nuevos en tiempo real
 * - Muestra notificaciones push cuando llegan mensajes mientras el usuario está fuera de /chats
 */
export const NotificationProvider = ({ children }) => {
    const location = useLocation();
    const navigate = useNavigate();
    const { isAuthenticated } = useAuth();
    const { socket, isConnected } = useSocketContext();
    const { permission, requestPermission, showNotification, isSupported } = useNotifications();
    const knownConversationsRef = useRef(new Map());
    const [hasRequestedPermission, setHasRequestedPermission] = useState(false);
    const initializedRef = useRef(false);
    const DEFAULT_COMMUNITY_IMAGE = 'https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&w=400&q=80';

    const toAbsoluteImageUrl = (imageUrl, fallback = '/favicon.ico') => {
        const raw = imageUrl || fallback;
        if (!raw) return fallback;
        if (raw.startsWith('http://') || raw.startsWith('https://') || raw.startsWith('data:') || raw.startsWith('blob:')) {
            return raw;
        }
        try {
            return new URL(raw, window.location.origin).toString();
        } catch {
            return fallback;
        }
    };

    // Determinar si estamos en la ruta de chats
    const isInChatRoute = location.pathname === '/chats';

    // Refs for volatile values used inside the WS handler — avoids
    // unsubscribing/resubscribing every time these change.
    const isInChatRouteRef = useRef(isInChatRoute);
    isInChatRouteRef.current = isInChatRoute;
    const navigateRef = useRef(navigate);
    navigateRef.current = navigate;
    const showNotificationRef = useRef(showNotification);
    showNotificationRef.current = showNotification;

    // Solicitar permisos de notificación cuando el usuario esté autenticado
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

    // Seed the known-conversations map on mount so the first real-time message
    // doesn't trigger a spurious notification
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
                });
            } catch { /* silent */ }

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
                    } catch { /* silent */ }
                }
            } catch { /* silent */ }
        };

        seedKnown();
    }, [isAuthenticated]);

    // Listen for real-time private messages via WebSocket
    useEffect(() => {
        if (!socket || !isConnected || !isAuthenticated || permission !== 'granted') return;

        const handleDM = (msg) => {
            if (isInChatRouteRef.current || !msg) return;

            const key = `user-${msg.emisorId}`;
            const known = knownConversationsRef.current;
            const prev = known.get(key);
            const current = msg.contenido || '';

            if (prev !== undefined && prev === current) return;
            known.set(key, current);

            // Don't notify for first-time seed
            if (prev === undefined && known.size <= 1) return;

            const title = msg.emisorNombre || 'Nuevo mensaje';
            const body = current || 'Tienes un nuevo mensaje';

            showNotificationRef.current(
                title,
                {
                    body: body.length > 100 ? body.substring(0, 100) + '...' : body,
                    icon: toAbsoluteImageUrl(msg.emisorFoto, '/favicon.ico'),
                    badge: '/favicon.ico',
                    tag: `msg-${msg.emisorId}-${Date.now()}`,
                    requireInteraction: true,
                },
                () => {
                    navigateRef.current(`/chats?userId=${msg.emisorId}`);
                }
            );
        };

        socket.on('dm_message', handleDM);

        return () => {
            socket.off('dm_message', handleDM);
        };
    }, [socket, isConnected, isAuthenticated, permission]);

    // Resetear el estado conocido cuando entramos a /chats o /comunidades
    useEffect(() => {
        if ((isInChatRoute || location.pathname.startsWith('/comunidades/')) && isAuthenticated) {
            obtenerConversaciones()
                .then(({ data }) => {
                    const conversations = Array.isArray(data) ? data : [];
                    const newMap = knownConversationsRef.current;
                    conversations.forEach((conv) => {
                        newMap.set(`user-${conv.usuarioId}`, conv.ultimoMensaje || '');
                    });
                })
                .catch(() => {});
        }
    }, [isInChatRoute, location.pathname, isAuthenticated]);

    const value = {
        permission,
        requestPermission,
        showNotification,
        isSupported,
    };

    return (
        <NotificationContext.Provider value={value}>
            {children}
        </NotificationContext.Provider>
    );
};

/**
 * Hook para acceder al contexto de notificaciones.
 */
export const useNotificationContext = () => {
    const context = useContext(NotificationContext);
    if (!context) {
        throw new Error('useNotificationContext debe usarse dentro de <NotificationProvider>');
    }
    return context;
};

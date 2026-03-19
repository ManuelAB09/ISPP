import React, { createContext, useContext, useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useNotifications } from '../hooks/useNotifications';
import { obtenerConversaciones, obtenerHistorialComunidad } from '../api/mensajeService';
import { communitiesApi } from '../api/communities.api';
import { getApiBaseUrl } from '../api/baseUrl';
import { resolveCommunityImage } from '../screens/chat/Chats';
import { useAuth } from './AuthContext';
import { useSocketContext } from './SocketContext';

/**
 * Contexto para manejar notificaciones push de mensajes y eventos en toda la aplicación.
 */
const NotificationContext = createContext(null);

/**
 * Provider de notificaciones push que funciona en toda la aplicación.
 * - Solicita permisos de notificaciones al usuario
 * - Usa WebSocket para detectar mensajes nuevos, solicitudes y eventos en tiempo real
 * - Muestra notificaciones push cuando llegan eventos mientras el usuario está en la app
 */
export const NotificationProvider = ({ children }) => {
    const location = useLocation();
    const navigate = useNavigate();
    const { isAuthenticated, user } = useAuth();
    const { socket, isConnected } = useSocketContext();
    const { permission, requestPermission, showNotification, isSupported } = useNotifications();
    const knownConversationsRef = useRef(new Map());
    const [hasRequestedPermission, setHasRequestedPermission] = useState(false);
    const [notificationsEnabled, setNotificationsEnabled] = useState(true);
    const initializedRef = useRef(false);

    const toAbsoluteImageUrl = (imageUrl, fallback = '/favicon.ico') => {
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

    const showPrettyNotification = (title, body, avatarUrl, tag, onClick) => {
        const cleanTitle = truncate(sanitizeNotificationText(title), 42);
        const cleanBody = truncate(sanitizeNotificationText(body), 72);

        showNotificationRef.current(
            cleanTitle,
            {
                body: cleanBody,
                icon: toAbsoluteImageUrl(avatarUrl, '/favicon.ico'),
                badge: '/favicon.ico',
                tag,
                requireInteraction: false,
            },
            onClick
        );
    };

    const shouldSkipNotification = (senderId) => {
        if (permission !== 'granted' || !notificationsEnabled) return true;
        if (isInChatRouteRef.current) return true;
        if (senderId != null && Number(senderId) === Number(user?.id)) return true;
        return false;
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
        if (!socket || !isConnected || !isAuthenticated) return;

        const handleDM = (msg) => {
            if (!msg) return;
            if (shouldSkipNotification(msg.emisorId)) return;

            const key = `user-${msg.emisorId}`;
            const known = knownConversationsRef.current;
            const prev = known.get(key);
            const current = msg.contenido || '';

            if (prev !== undefined && prev === current) return;
            known.set(key, current);

            // Don't notify for first-time seed
            if (prev === undefined && known.size <= 1) return;

            const title = `${msg.emisorNombre || 'Nuevo mensaje'}`;
            const body = current || 'Tienes un nuevo mensaje';

            showPrettyNotification(
                title,
                body,
                msg.emisorFoto,
                `msg-${msg.emisorId}-${Date.now()}`,
                () => {
                    navigateRef.current(`/chats?userId=${msg.emisorId}`);
                }
            );
        };

        // Handler para nuevas solicitudes de contratación
        const handleSolicitudContratacion = (solicitud) => {
            if (!solicitud) return;
            if (shouldSkipNotification(solicitud.alumnoId)) return;

            const title = '📋 Nueva solicitud de contratación';
            const body = `${solicitud.alumnoNombre || 'Un alumno'} te ha solicitado una clase`;

            showPrettyNotification(
                title,
                body,
                solicitud.alumnoFoto,
                `solicitud-${solicitud.id}`,
                () => {
                    navigateRef.current('/mis-solicitudes');
                }
            );
        };

        // Handler para respuestas a solicitudes (aceptada/rechazada)
        const handleSolicitudRespuesta = (solicitud) => {
            if (!solicitud) return;
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

            showPrettyNotification(
                title,
                body,
                solicitud.tutorFoto,
                `solicitud-respuesta-${solicitud.id}`,
                () => {
                    navigateRef.current('/mis-solicitudes');
                }
            );
        };

        // Handler para pagos completados
        const handleSolicitudPagada = (solicitud) => {
            if (!solicitud) return;
            if (shouldSkipNotification(solicitud.alumnoId)) return;

            const title = '💰 Clase pagada y confirmada';
            const body = `${solicitud.alumnoNombre || 'El alumno'} ha completado el pago. Clase confirmada.`;

            showPrettyNotification(
                title,
                body,
                solicitud.alumnoFoto,
                `solicitud-pagada-${solicitud.id}`,
                () => {
                    navigateRef.current('/mis-solicitudes');
                }
            );
        };

        // Handler para actualizaciones de alertas/contador
        const handleAlertsCount = (data) => {
            if (!data) return;
            // Opcionalmente mostrar notificación si el contador cambió significativamente
            // Por ahora solo emitimos localmente para UI updates
        };

        // Handler para mensajes de comunidades
        const handleCommunityHistory = (data) => {
            if (!data) return;
            // Los mensajes de comunidades pueden procesarse aquí si es necesario
        };

        const handleCommunityMessage = (msg) => {
            if (!msg || !msg.comunidadId) return;
            if (shouldSkipNotification(msg.usuarioId)) return;

            const key = `community-${msg.comunidadId}`;
            const known = knownConversationsRef.current;
            const current = msg.contenido || msg.archivoNombre || '';
            const prev = known.get(key);

            if (prev !== undefined && prev === current) return;
            known.set(key, current);

            const title = `${msg.comunidadNombre || 'Comunidad'}`;
            const body = msg.contenido
                ? `${msg.usuarioNombre || 'Alguien'}: ${msg.contenido}`
                : `${msg.usuarioNombre || 'Alguien'} ha enviado un archivo en la comunidad`;

            // Usar la misma lógica de Chats para la imagen de comunidad
            const fakeCommunity = { imagen: msg.comunidadImagenUrl, imagenUrl: msg.comunidadImagenUrl, foto: msg.comunidadImagenUrl };
            const communityImage = resolveCommunityImage(fakeCommunity);

            showPrettyNotification(
                title,
                body,
                communityImage,
                `community-msg-${msg.comunidadId}-${msg.id || Date.now()}`,
                () => {
                    navigateRef.current(`/chats?communityId=${msg.comunidadId}`);
                }
            );
        };

        socket.on('dm_message', handleDM);
        socket.on('solicitud_contratacion', handleSolicitudContratacion);
        socket.on('solicitud_contratacion_respuesta', handleSolicitudRespuesta);
        socket.on('solicitud_contratacion_pagada', handleSolicitudPagada);
        socket.on('alerts_count', handleAlertsCount);
        socket.on('community_history', handleCommunityHistory);
        socket.on('community_message', handleCommunityMessage);

        return () => {
            socket.off('dm_message', handleDM);
            socket.off('solicitud_contratacion', handleSolicitudContratacion);
            socket.off('solicitud_contratacion_respuesta', handleSolicitudRespuesta);
            socket.off('solicitud_contratacion_pagada', handleSolicitudPagada);
            socket.off('alerts_count', handleAlertsCount);
            socket.off('community_history', handleCommunityHistory);
            socket.off('community_message', handleCommunityMessage);
        };
    }, [socket, isConnected, isAuthenticated, permission, notificationsEnabled, user?.id]);

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

    // Load notifications preference from localStorage on mount
    useEffect(() => {
        const saved = localStorage.getItem('notificationsEnabled');
        if (saved !== null) {
            setNotificationsEnabled(JSON.parse(saved));
        }
    }, []);

    // Save notifications preference to localStorage when it changes
    useEffect(() => {
        localStorage.setItem('notificationsEnabled', JSON.stringify(notificationsEnabled));
    }, [notificationsEnabled]);

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

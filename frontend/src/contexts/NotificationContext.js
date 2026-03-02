import React, { createContext, useContext, useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useNotifications } from '../hooks/useNotifications';
import { obtenerConversaciones, obtenerHistorialComunidad } from '../api/mensajeService';
import { communitiesApi } from '../api/communities.api';
import { useAuth } from './AuthContext';

/**
 * Contexto para manejar notificaciones push de mensajes en toda la aplicación.
 */
const NotificationContext = createContext(null);

/**
 * Provider de notificaciones push que funciona en toda la aplicación.
 * - Solicita permisos de notificaciones al usuario
 * - Hace polling de mensajes cuando el usuario está fuera de /chats
 * - Muestra notificaciones push cuando llegan mensajes nuevos
 */
export const NotificationProvider = ({ children }) => {
    const location = useLocation();
    const navigate = useNavigate();
    const { isAuthenticated } = useAuth();
    const { permission, requestPermission, showNotification, isSupported } = useNotifications();
    const knownConversationsRef = useRef(new Map());
    const [hasRequestedPermission, setHasRequestedPermission] = useState(false);
    const pollingIntervalRef = useRef(null);
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

    // Solicitar permisos de notificación cuando el usuario esté autenticado
    useEffect(() => {
        if (isAuthenticated && isSupported && !hasRequestedPermission && permission === 'default') {
            // Esperar 2 segundos después de la autenticación para solicitar permisos
            const timer = setTimeout(() => {
                requestPermission().then(() => {
                    setHasRequestedPermission(true);
                });
            }, 2000);

            return () => clearTimeout(timer);
        }
    }, [isAuthenticated, isSupported, hasRequestedPermission, permission, requestPermission]);

    // Polling de mensajes cuando NO estamos en la ruta de chats
    useEffect(() => {
        if (!isAuthenticated || isInChatRoute || permission !== 'granted') {
            // Limpiar polling si existe
            if (pollingIntervalRef.current) {
                clearInterval(pollingIntervalRef.current);
                pollingIntervalRef.current = null;
            }
            return;
        }

        console.log('📡 Iniciando polling de mensajes...');

        // Función para verificar nuevos mensajes (privados + comunidades)
        const checkNewMessages = async () => {
            try {
                // Obtener conversaciones privadas
                const { data: privateData } = await obtenerConversaciones();
                const privateConversations = Array.isArray(privateData) ? privateData : [];
                
                // Obtener comunidades del usuario
                let communityConversations = [];
                try {
                    const { content: communities } = await communitiesApi.listMine({ page: 0, size: 100 });
                    
                    // Para cada comunidad, obtener el último mensaje
                    const communityPromises = (communities || []).map(async (community) => {
                        try {
                            const { data: messages } = await obtenerHistorialComunidad(community.id);
                            const lastMessage = messages && messages.length > 0 ? messages[messages.length - 1] : null;
                            
                            if (lastMessage) {
                                return {
                                    isCommunity: true,
                                    comunidadId: community.id,
                                    comunidadNombre: community.nombre || 'Comunidad',
                                    comunidadFoto: toAbsoluteImageUrl(
                                        community.imagenUrl
                                        || community.imagen
                                        || community.foto,
                                        DEFAULT_COMMUNITY_IMAGE
                                    ),
                                    ultimoMensaje: lastMessage.contenido,
                                    emisorNombre:
                                        lastMessage.usuarioNombre
                                        || lastMessage.emisorNombre
                                        || 'Usuario',
                                    ultimaFecha: lastMessage.createdAt
                                };
                            }
                            return null;
                        } catch (err) {
                            console.warn(`⚠️ Error obteniendo mensajes de comunidad ${community.id}:`, err);
                            return null;
                        }
                    });
                    
                    const results = await Promise.all(communityPromises);
                    communityConversations = results.filter(c => c !== null);
                } catch (err) {
                    console.warn('⚠️ Error obteniendo comunidades:', err);
                }
                
                // Marcar conversaciones privadas
                const markedPrivate = privateConversations.map(conv => ({ ...conv, isCommunity: false }));
                
                // Combinar ambos tipos
                const allConversations = [...markedPrivate, ...communityConversations];
                
                console.log('📬 Conversaciones obtenidas:', allConversations.length, '(', privateConversations.length, 'privadas,', communityConversations.length, 'comunidades)');
                
                // Detectar conversaciones nuevas o actualizadas
                const newMessages = [];
                const knownConversations = knownConversationsRef.current;
                
                console.log('🔍 Revisando', allConversations.length, 'conversaciones (caché tiene', knownConversations.size, 'entradas)');
                
                allConversations.forEach((conv) => {
                    // Crear key única: privado usa usuarioId, comunidad usa comunidadId con prefijo
                    const key = conv.isCommunity ? `community-${conv.comunidadId}` : `user-${conv.usuarioId}`;
                    const previousState = knownConversations.get(key);
                    
                    // Nueva conversación o mensaje actualizado
                    const currentMessage = conv.ultimoMensaje || '';
                    const displayName = conv.isCommunity ? `[Comunidad] ${conv.comunidadNombre}` : conv.usuarioNombre;
                    
                    console.log(`${conv.isCommunity ? '👥' : '👤'} ${displayName}: anterior="${previousState || 'NULL'}" actual="${currentMessage}"`);
                    
                    if (!previousState) {
                        // Conversación nueva - solo notificar si ya habíamos inicializado (no en primera carga)
                        if (knownConversations.size > 0 && currentMessage) {
                            console.log(`🆕 Nueva ${conv.isCommunity ? 'conversación de comunidad' : 'conversación privada'} detectada:`, displayName);
                            newMessages.push(conv);
                        } else {
                            console.log('⏭️ Primera carga, no notificar para:', displayName);
                        }
                    } else if (previousState !== currentMessage && currentMessage) {
                        // Mensaje actualizado
                        console.log(`✉️ Mensaje actualizado ${conv.isCommunity ? 'en comunidad' : 'de'}:`, displayName);
                        newMessages.push(conv);
                    } else if (previousState === currentMessage) {
                        console.log('➖ Sin cambios para:', displayName);
                    }
                });

                // Actualizar el mapa de conversaciones conocidas
                const newMap = new Map();
                allConversations.forEach((conv) => {
                    const key = conv.isCommunity ? `community-${conv.comunidadId}` : `user-${conv.usuarioId}`;
                    newMap.set(key, conv.ultimoMensaje || '');
                });
                knownConversationsRef.current = newMap;

                // Mostrar notificaciones para mensajes nuevos
                if (newMessages.length > 0) {
                    console.log('🔔 Mostrando', newMessages.length, 'notificaciones');
                }
                
                newMessages.forEach((conv) => {
                    let title, body, icon, tag, clickHandler;
                    
                    if (conv.isCommunity) {
                        // Notificación de mensaje de comunidad
                        const communityName = conv.comunidadNombre || 'Comunidad';
                        const senderName = conv.emisorNombre || conv.usuarioNombre || 'Usuario';
                        const messageText = conv.ultimoMensaje || 'Nuevo mensaje';
                        title = `💬 ${communityName}`;
                        body = `${senderName}: ${messageText}`;
                        icon = toAbsoluteImageUrl(conv.comunidadFoto, DEFAULT_COMMUNITY_IMAGE);
                        tag = `community-${conv.comunidadId}-${Date.now()}`;
                        clickHandler = () => {
                            // Navegar al detalle de la comunidad
                            navigate(`/comunidades/${conv.comunidadId}`);
                        };
                    } else {
                        // Notificación de mensaje privado
                        title = conv.usuarioNombre || 'Nuevo mensaje';
                        body = conv.ultimoMensaje || 'Tienes un nuevo mensaje';
                        icon = conv.usuarioFoto || '/favicon.ico';
                        tag = `msg-${conv.usuarioId}-${Date.now()}`;
                        clickHandler = () => {
                            navigate(`/chats?userId=${conv.usuarioId}&userName=${encodeURIComponent(conv.usuarioNombre)}`);
                        };
                    }
                    
                    showNotification(
                        title,
                        {
                            body: body.length > 100 ? body.substring(0, 100) + '...' : body,
                            icon: icon,
                            badge: icon,
                            tag: tag,
                            requireInteraction: true,
                        },
                        clickHandler
                    );
                });
            } catch (error) {
                console.error('❌ Error al verificar nuevos mensajes:', error);
            }
        };

        // Cargar el estado inicial inmediatamente
        checkNewMessages();

        // Hacer polling cada 5 segundos (más frecuente para mejor detección)
        pollingIntervalRef.current = setInterval(checkNewMessages, 5000);

        return () => {
            if (pollingIntervalRef.current) {
                clearInterval(pollingIntervalRef.current);
                pollingIntervalRef.current = null;
                console.log('🛑 Polling detenido');
            }
        };
    }, [isAuthenticated, isInChatRoute, permission]);

    // Resetear el estado conocido cuando entramos a /chats o /comunidades
    useEffect(() => {
        if ((isInChatRoute || location.pathname.startsWith('/comunidades/')) && isAuthenticated) {
            // Actualizar el mapa de conversaciones conocidas para evitar notificaciones duplicadas
            console.log('💬 Entrando a /chats o comunidad - actualizando estado de conversaciones');
            
            // Actualizar conversaciones privadas
            obtenerConversaciones()
                .then(({ data }) => {
                    const conversations = Array.isArray(data) ? data : [];
                    const newMap = knownConversationsRef.current;
                    conversations.forEach((conv) => {
                        newMap.set(`user-${conv.usuarioId}`, conv.ultimoMensaje || '');
                    });
                    console.log('✅ Estado de conversaciones privadas actualizado');
                })
                .catch((err) => console.error('❌ Error al actualizar estado de mensajes privados:', err));
            
            // Actualizar mensajes de comunidades
            communitiesApi.listMine({ page: 0, size: 100 })
                .then(async ({ content: communities }) => {
                    const newMap = knownConversationsRef.current;
                    for (const community of (communities || [])) {
                        try {
                            const { data: messages } = await obtenerHistorialComunidad(community.id);
                            const lastMessage = messages && messages.length > 0 ? messages[messages.length - 1] : null;
                            if (lastMessage) {
                                newMap.set(`community-${community.id}`, lastMessage.contenido || '');
                            }
                        } catch (err) {
                            console.warn(`⚠️ Error actualizando comunidad ${community.id}:`, err);
                        }
                    }
                    console.log('✅ Estado de conversaciones de comunidades actualizado');
                })
                .catch((err) => console.error('❌ Error al actualizar estado de mensajes de comunidades:', err));
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

import { useEffect, useRef, useState } from 'react';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import { getApiBaseUrl } from '../api/baseUrl';

/**
 * Hook personalizado para manejar la conexión WebSocket (Socket.IO).
 * @param {string} token - Token JWT para autenticación.
 * @returns {object} Socket.IO instance y estado de conexión.
 */
export const useSocket = (token) => {
    const socketRef = useRef(null);
    const stompClientRef = useRef(null);
    const subscriptionsRef = useRef(new Map());
    const localListenersRef = useRef(new Map());
    const [isConnected, setIsConnected] = useState(false);

    useEffect(() => {
        if (!token) {
            setIsConnected(false);
            socketRef.current = null;
            return;
        }

        const SOCKET_SERVER = getApiBaseUrl();
        const WS_URL = `${SOCKET_SERVER}/ws`;
        const subscriptions = subscriptionsRef.current;
        const localListeners = localListenersRef.current;

        const emitLocal = (event, payload) => {
            const listeners = localListeners.get(event) || new Set();
            listeners.forEach((cb) => cb(payload));
        };

        const eventToDestination = (event) => {
            const emitMap = {
                'community.message.send': '/app/community.message.send',
                'community.message.edit': '/app/community.message.edit',
                'community.message.delete': '/app/community.message.delete',
                'community.history': '/app/community.history',
                'dm.send': '/app/dm.send',
                'dm.delete': '/app/dm.delete',
                'dm.history': '/app/dm.history',
                'conversations.get': '/app/conversations.get',
            };
            return emitMap[event] || event;
        };

        const eventToSubscription = (event) => {
            if (event.startsWith('/topic/') || event.startsWith('/queue/') || event.startsWith('/user/')) {
                return event;
            }
            const subscribeMap = {
                dm_message: '/user/queue/dm',
                dm_delete_success: '/user/queue/dm_delete_success',
                dm_history: '/user/queue/dm_history',
                conversations: '/user/queue/conversations',
                community_history: '/user/queue/community_history',
                error: '/user/queue/error',
            };
            return subscribeMap[event] || event;
        };

        const socketAdapter = {
            on: (event, callback) => {
                if (!stompClientRef.current || !stompClientRef.current.connected) {
                    const listeners = localListeners.get(event) || new Set();
                    listeners.add(callback);
                    localListeners.set(event, listeners);
                    return;
                }

                if (event === 'connect' || event === 'disconnect' || event === 'connect_error') {
                    const listeners = localListeners.get(event) || new Set();
                    listeners.add(callback);
                    localListeners.set(event, listeners);
                    return;
                }

                const destination = eventToSubscription(event);
                const key = `${event}:${callback.toString()}:${Math.random()}`;
                const subscription = stompClientRef.current.subscribe(destination, (frame) => {
                    try {
                        const body = frame.body ? JSON.parse(frame.body) : null;
                        callback(body);
                    } catch {
                        callback(frame.body);
                    }
                });
                subscriptions.set(key, { event, callback, subscription });
            },

            off: (event, callback) => {
                for (const [key, item] of subscriptions.entries()) {
                    if (item.event === event && (!callback || item.callback === callback)) {
                        item.subscription.unsubscribe();
                        subscriptions.delete(key);
                    }
                }

                if (localListeners.has(event)) {
                    if (!callback) {
                        localListeners.delete(event);
                    } else {
                        const listeners = localListeners.get(event);
                        listeners.delete(callback);
                        if (listeners.size === 0) {
                            localListeners.delete(event);
                        }
                    }
                }
            },

            emit: (event, payload = {}) => {
                if (!stompClientRef.current || !stompClientRef.current.connected) {
                    emitLocal('error', { message: 'Socket no conectado' });
                    return;
                }

                const destination = eventToDestination(event);
                stompClientRef.current.publish({
                    destination,
                    body: JSON.stringify(payload),
                });
            },
        };

        socketRef.current = socketAdapter;

        const client = new Client({
            webSocketFactory: () => new SockJS(WS_URL),
            connectHeaders: {
                Authorization: `Bearer ${token}`,
            },
            reconnectDelay: 3000,
            heartbeatIncoming: 10000,
            heartbeatOutgoing: 10000,
            onConnect: () => {
                setIsConnected(true);
                emitLocal('connect', {});
                console.log('✓ Conectado al servidor WebSocket (STOMP)');
            },
            onStompError: (frame) => {
                setIsConnected(false);
                emitLocal('error', { message: frame?.headers?.message || 'STOMP error' });
                console.error('✗ STOMP error:', frame?.body);
            },
            onWebSocketError: (error) => {
                setIsConnected(false);
                emitLocal('connect_error', error);
                console.error('✗ Error de conexión WebSocket:', error);
            },
            onWebSocketClose: () => {
                setIsConnected(false);
                emitLocal('disconnect', {});
                console.log('✗ Desconectado del servidor WebSocket');
            },
        });

        stompClientRef.current = client;
        client.activate();

        // Cleanup al desmontar
        return () => {
            for (const [, item] of subscriptions.entries()) {
                item.subscription.unsubscribe();
            }
            subscriptions.clear();
            localListeners.clear();

            if (stompClientRef.current) {
                stompClientRef.current.deactivate();
                stompClientRef.current = null;
            }

            socketRef.current = null;
            setIsConnected(false);
        };
    }, [token]);

    return {
        socket: socketRef.current,
        isConnected,
    };
};

import { useEffect, useState, useCallback } from 'react';

/**
 * Hook para manejar notificaciones push del navegador.
 * Solicita permisos y proporciona funciones para mostrar notificaciones.
 * @returns {object} Estado de permiso y función para mostrar notificaciones.
 */
export const useNotifications = () => {
    const [permission, setPermission] = useState('default');
    const [isSupported, setIsSupported] = useState(false);

    useEffect(() => {
        // Verificar si el navegador soporta notificaciones
        if (!('Notification' in window)) {
            setIsSupported(false);
            return;
        }

        setIsSupported(true);
        setPermission(Notification.permission);
    }, []);

    /**
     * Solicita permiso al usuario para mostrar notificaciones.
     */
    const requestPermission = useCallback(async () => {
        if (!isSupported) {
            return 'denied';
        }

        if (Notification.permission === 'granted') {
            setPermission('granted');
            return 'granted';
        }

        if (Notification.permission !== 'denied') {
            const result = await Notification.requestPermission();
            setPermission(result);
            return result;
        }

        return 'denied';
    }, [isSupported]);

    /**
     * Muestra una notificación push del navegador.
     * @param {string} title - Título de la notificación.
     * @param {object} options - Opciones de la notificación (body, icon, tag, etc.).
     * @param {function} onClick - Callback cuando se hace clic en la notificación.
     */
    const showNotification = useCallback(
        (title, options = {}, onClick = null) => {
            if (!isSupported || Notification.permission !== 'granted') {
                return null;
            }

            const defaultOptions = {
                icon: '/favicon.ico',
                badge: '/favicon.ico',
                tag: 'default-tag',
                requireInteraction: false,
                renotify: true, // Permite notificaciones con el mismo tag
                ...options,
            };

            try {
                const notification = new Notification(title, defaultOptions);

                if (onClick) {
                    notification.onclick = (event) => {
                        event.preventDefault();
                        window.focus();
                        onClick();
                        notification.close();
                    };
                } else {
                    // Por defecto, enfocar la ventana al hacer clic
                    notification.onclick = (event) => {
                        event.preventDefault();
                        window.focus();
                        notification.close();
                    };
                }

                notification.onerror = () => {};

                // Auto-cerrar después de 8 segundos
                setTimeout(() => {
                    notification.close();
                }, 8000);

                return notification;
            } catch {
                return null;
            }
        },
        [isSupported]
    );

    return {
        permission,
        isSupported,
        requestPermission,
        showNotification,
    };
};

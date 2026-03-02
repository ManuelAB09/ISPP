import React from 'react';
import { useNotificationContext } from '../../contexts/NotificationContext';
import './NotificationToggle.css';

/**
 * Componente para gestionar el estado de las notificaciones push.
 * Permite al usuario habilitar/deshabilitar notificaciones del navegador.
 */
export default function NotificationToggle() {
    const { permission, requestPermission, isSupported } = useNotificationContext();

    if (!isSupported) {
        return null; // No mostrar si el navegador no soporta notificaciones
    }

    const handleToggle = async () => {
        if (permission === 'default') {
            await requestPermission();
        } else if (permission === 'denied') {
            alert('Has bloqueado las notificaciones. Por favor, permite las notificaciones desde la configuración de tu navegador.');
        }
    };

    const getIcon = () => {
        switch (permission) {
            case 'granted':
                return '🔔';
            case 'denied':
                return '🔕';
            default:
                return '🔕';
        }
    };

    const getLabel = () => {
        switch (permission) {
            case 'granted':
                return 'Notificaciones activadas';
            case 'denied':
                return 'Notificaciones bloqueadas';
            default:
                return 'Activar notificaciones';
        }
    };

    return (
        <button
            className={`notification-toggle ${permission}`}
            onClick={handleToggle}
            title={getLabel()}
            disabled={permission === 'granted'}
        >
            <span className="notification-icon">{getIcon()}</span>
            <span className="notification-label">{getLabel()}</span>
        </button>
    );
}

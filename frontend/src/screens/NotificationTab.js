import React, { useEffect, useState } from 'react';
import { useNotificationContext } from '../contexts/NotificationContext';
import './NotificationTab.css';
import { getAllNotifications, markNotificationAsRead, markAllNotificationsAsRead } from '../api/notificationService';

// Simulación: en el futuro, esto debe venir del backend
const mockNotifications = [
  // Ejemplo de notificaciones
  // { id: 1, type: 'EVENTO_NUEVO', title: 'Nuevo evento en tu comunidad', message: 'Se ha creado el evento "Examen final" en Matemáticas', read: false, createdAt: '2026-03-19T10:00:00' },
];

export default function NotificationTab() {
  const { notificationsEnabled } = useNotificationContext();
  const [notifications, setNotifications] = useState([]);

  useEffect(() => {
    getAllNotifications()
      .then((data) => {
        setNotifications(
          (data || []).map((n) => ({
            id: n.id,
            type: n.tipo,
            title: n.eventoTitulo || n.tipo || 'Notificación',
            message: n.mensaje,
            read: n.leida,
            createdAt: n.createdAt,
            eventoId: n.eventoId,
            comunidadNombre: n.comunidadNombre,
            icono: n.icono,
          }))
        );
      })
      .catch(() => setNotifications([]));
  }, []);

  const handleMarkAsRead = async (id) => {
    await markNotificationAsRead(id);
    setNotifications((prev) => prev.map((n) => n.id === id ? { ...n, read: true } : n));
  };

  const handleMarkAllAsRead = async () => {
    await markAllNotificationsAsRead();
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
  };

  return (
    <div className="notification-tab-container">
      <h2>Notificaciones</h2>
      {!notificationsEnabled && (
        <div className="notification-warning">Las notificaciones están desactivadas.</div>
      )}
      {notifications.length === 0 ? (
        <div className="notification-empty">No tienes notificaciones recientes.</div>
      ) : (
        <ul className="notification-list">
          {notifications.map((n) => (
            <li key={n.id} className={`notification-item${n.read ? '' : ' unread'}`} onClick={() => !n.read && handleMarkAsRead(n.id)} style={{ cursor: n.read ? 'default' : 'pointer' }}>
              <div className="notification-title">{n.title}</div>
              <div className="notification-message">{n.message}</div>
              <div className="notification-date">{new Date(n.createdAt).toLocaleString()}</div>
              {!n.read && <span className="notification-unread-dot" title="No leída"></span>}
            </li>
          ))}
        </ul>
      )}
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 12 }}>
        <button onClick={handleMarkAllAsRead} className="notification-markall-btn">
          Marcar todo como leído
        </button>
      </div>
    </div>
  );
}

import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useNotificationContext } from '../contexts/NotificationContext';
import './NotificationTab.css';
import {
  getAllEventAlerts,
  getAllUserNotifications,
  markEventAlertAsRead,
  markAllEventAlertsAsRead,
  markUserNotificationAsRead,
  markAllUserNotificationsAsRead,
} from '../api/notificationService';
import Header from '../components/Header/Header';

export default function NotificationTab() {
  const { notificationsEnabled } = useNotificationContext();
  const [notifications, setNotifications] = useState([]);
  const navigate = useNavigate();

  // Lógica para obtener la URL base de la API 
  function getApiBaseUrl() {
    if (process.env.REACT_APP_API_BASE_URL) return process.env.REACT_APP_API_BASE_URL;
    if (window && window.location && window.location.origin) return window.location.origin + '/api';
    return '/api';
  }

  function resolveCommunityImage(raw) {
    if (!raw || !String(raw).trim() || String(raw).trim().toLowerCase() === 'empty') {
      return 'https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&w=400&q=80';
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
  }

  useEffect(() => {
    Promise.all([
      getAllEventAlerts(),
      getAllUserNotifications(),
    ])
      .then(([eventAlerts, userNotifications]) => {
        const alerts = (eventAlerts || []).map((n) => ({
          id: `alerta-${n.id}`,
          type: n.tipo,
          title: n.eventoTitulo || n.tipo || 'Notificación',
          message: n.mensaje,
          read: n.leida,
          createdAt: n.createdAt,
          source: 'alerta',
          eventoId: n.eventoId,
          comunidadNombre: n.comunidadNombre,
          icono: n.icono,
        }));
        const notifs = (userNotifications || []).map((n) => {
          // Unificar lógica de imagen
          const communityImageRaw = n.comunidadImagenUrl || n.comunidadImagen || n.imagenUrl || n.foto;
          return {
            id: `notif-${n.id}`,
            type: n.tipo,
            title: n.titulo || n.tipo || 'Notificación',
            message: n.mensaje,
            read: n.leida,
            createdAt: n.createdAt,
            source: 'notificacion',
            anuncioId: n.anuncioId,
            comunidadId: n.comunidadId,
            comunidadNombre: n.comunidadNombre,
            comunidadImagenUrl: resolveCommunityImage(communityImageRaw),
          };
        });
        const merged = [...alerts, ...notifs].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
        setNotifications(merged);
      })
      .catch(() => setNotifications([]));
  }, []);

  const handleMarkAsRead = async (id, source) => {
    if (source === 'alerta') {
      await markEventAlertAsRead(id.replace('alerta-', ''));
    } else if (source === 'notificacion') {
      await markUserNotificationAsRead(id.replace('notif-', ''));
    }
    setNotifications((prev) => prev.map((n) => n.id === id ? { ...n, read: true } : n));
  };

  // Navegar al anuncio si es notificación de anuncio
  const handleNotificationClick = async (n) => {
    if (!n.read) {
      await handleMarkAsRead(n.id, n.source);
      await new Promise(res => setTimeout(res, 100));
    }
    if (n.source === 'notificacion' && n.type === 'ANUNCIO' && n.anuncioId && n.comunidadId) {
      navigate(`/comunidades/${n.comunidadId}?tab=anuncios&anuncioId=${n.anuncioId}`);
      return;
    }
    if (n.source === 'alerta' && n.eventoId) {
      navigate(`/eventos/${n.eventoId}`);
    }
  };

  const handleMarkAllAsRead = async () => {
    await Promise.all([
      markAllEventAlertsAsRead(),
      markAllUserNotificationsAsRead(),
    ]);
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
  };

  return (
    <>
      <Header page="notificaciones" />
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
              <li
                key={n.id}
                className={`notification-item${n.read ? '' : ' unread'}`}
                onClick={() => handleNotificationClick(n)}
                style={{ cursor: n.read ? 'default' : 'pointer' }}
              >
                <div className="notification-title">
                  {n.title}
                  {n.source === 'notificacion' && <span className="notification-type">[Anuncio]</span>}
                  {n.source === 'alerta' && <span className="notification-type">[Evento]</span>}
                </div>
                {n.comunidadNombre && (
                  <div className="notification-community">
                    <img
                      src={n.comunidadImagenUrl || 'https://ui-avatars.com/api/?name=Comunidad&background=eee&color=888&size=64'}
                      alt="Comunidad"
                      className="notification-community-img"
                      onError={e => { e.target.onerror = null; e.target.src = 'https://ui-avatars.com/api/?name=Comunidad&background=eee&color=888&size=64'; }}
                    />
                    Comunidad: {n.comunidadNombre}
                  </div>
                )}
                <div className="notification-message">{n.message}</div>
                <div className="notification-date">{new Date(n.createdAt).toLocaleString()}</div>
                {!n.read && <span className="notification-unread-dot" title="No leída"></span>}
              </li>
            ))}
          </ul>
        )}
        <div className="notification-actions">
          <button onClick={handleMarkAllAsRead} className="notification-markall-btn">
            Marcar todo como leído
          </button>
        </div>
      </div>
    </>
  );
}

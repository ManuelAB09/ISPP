import React, { useEffect, useState } from 'react';
import Modal from '../../components/Modal/Modal';
import '../../components/Modal/ModalChanges.css';
import { useNavigate } from 'react-router-dom';
import { useNotificationContext } from '../../contexts/NotificationContext';
import './NotificationTab.css';
import {
  getAllEventAlerts,
  getAllUserNotifications,
  markEventAlertAsRead,
  markAllEventAlertsAsRead,
  markUserNotificationAsRead,
  markAllUserNotificationsAsRead,
} from '../../api/notificationService';
import Header from '../../components/Header/Header';

export default function NotificationTab() {
  const {
    notificationsEnabled,
    markOnePanelNotificationRead,
    clearPanelNotificationsUnread,
    setPanelNotificationsUnreadCount,
  } = useNotificationContext();
  const [notifications, setNotifications] = useState([]);
  const [showChangesModal, setShowChangesModal] = useState(false);
  const [changesContent, setChangesContent] = useState('');
  const [pendingEventId, setPendingEventId] = useState(null);
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
            eventoId: n.eventoId,
            comunidadNombre: n.comunidadNombre,
            comunidadImagenUrl: resolveCommunityImage(communityImageRaw),
          };
        });
        const merged = [...alerts, ...notifs].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
        setNotifications(merged);
        setPanelNotificationsUnreadCount(merged.filter((n) => !n.read).length);
      })
      .catch(() => {
        setNotifications([]);
        setPanelNotificationsUnreadCount(0);
      });
  }, [setPanelNotificationsUnreadCount]);

  const handleMarkAsRead = async (id, source) => {
    const current = notifications.find((n) => n.id === id);
    if (source === 'alerta') {
      await markEventAlertAsRead(id.replace('alerta-', ''));
    } else if (source === 'notificacion') {
      await markUserNotificationAsRead(id.replace('notif-', ''));
    }
    setNotifications((prev) => prev.map((n) => n.id === id ? { ...n, read: true } : n));
    if (current && !current.read) {
      markOnePanelNotificationRead();
    }
  };

  // Navegar al anuncio si es notificación de anuncio
  const handleNotificationClick = async (n) => {
    if (!n.read) {
      await handleMarkAsRead(n.id, n.source);
      await new Promise(res => setTimeout(res, 100));
    }
    // Redirigir a la pestaña de solicitudes si es notificación de solicitud de acceso
    if (n.source === 'notificacion' && n.type === 'SOLICITUD_ACCESO' && n.comunidadId) {
      navigate(`/comunidades/${n.comunidadId}?tab=solicitudes`);
      return;
    }
    // Redirigir a la comunidad solo si la solicitud fue aprobada
    if (
      n.source === 'notificacion' &&
      n.type === 'RESPUESTA_SOLICITUD_ACCESO' &&
      n.comunidadId &&
      n.message &&
      n.message.toLowerCase().includes('aprobada')
    ) {
      navigate(`/comunidades/${n.comunidadId}`);
      return;
    }
    // Si es notificación de anuncio
    if (n.source === 'notificacion' && n.type === 'ANUNCIO' && n.anuncioId && n.comunidadId) {
      navigate(`/comunidades/${n.comunidadId}?tab=anuncios&anuncioId=${n.anuncioId}`);
      return;
    }
    // Si es notificación de evento (tipo EVENTO)
    if (n.source === 'notificacion' && n.type === 'EVENTO' && n.eventoId && n.message) {
      // Buscar si el mensaje contiene cambios
      const cambiosIdx = n.message.indexOf('Cambios:');
      if (cambiosIdx !== -1) {
        const cambios = n.message.substring(cambiosIdx + 8).trim();
        setChangesContent(cambios);
        setPendingEventId(n.eventoId);
        setShowChangesModal(true);
        return;
      }
      // Si no hay cambios, navegar directamente
      if (n.eventoId) {
        navigate(`/eventos/${n.eventoId}`);
        return;
      }
    }
    if (n.source === 'alerta' && n.eventoId) {
      navigate(`/eventos/${n.eventoId}`);
    }
  };

  const handleGoToEvent = () => {
    if (pendingEventId) {
      navigate(`/eventos/${pendingEventId}`);
      setShowChangesModal(false);
      setChangesContent('');
      setPendingEventId(null);
    }
  };

  const handleMarkAllAsRead = async () => {
    await Promise.all([
      markAllEventAlertsAsRead(),
      markAllUserNotificationsAsRead(),
    ]);
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
    clearPanelNotificationsUnread();
  };

     
    function renderChangesList(changesText) {
      if (!changesText) return <em>No hay cambios detectados.</em>;
      const items = changesText
        .split(/\r?\n/)
        .map(line => line.trim())
        .filter(line => line.length > 0);
      if (items.length === 0) return <em>No hay cambios detectados.</em>;
      return (
        <ul>
          {items.map((line, idx) => (
            <li key={idx}>{line}</li>
          ))}
        </ul>
      );
    }

    function formatDateTime(dateString) {
      const date = new Date(dateString);
      return date.toLocaleString(undefined, {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false
      });
    }

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
                  {n.source === 'notificacion' && n.type === 'ANUNCIO' && (
                    <span className="notification-type">[Anuncio]</span>
                  )}
                  {((n.source === 'notificacion' && n.type === 'EVENTO') || (n.source === 'alerta' && n.type === 'EVENTO')) && (
                    <span className="notification-type">[Evento]</span>
                  )}
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
                <div className="notification-date">{formatDateTime(n.createdAt)}</div>
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
      {showChangesModal && (
        <Modal isOpen={showChangesModal} onClose={() => setShowChangesModal(false)}>
          <div className="modal-changes-title">Cambios en el evento</div>
          <div className="modal-changes-list">
            {renderChangesList(changesContent)}
          </div>
          <button className="modal-changes-btn" onClick={handleGoToEvent}>
            Ir al evento
          </button>
        </Modal>
      )}
    </>
  );
}

import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useSocketContext } from "../../contexts/SocketContext";
import { obtenerConversaciones } from "../../api/mensajeService";
import { getApiBaseUrl } from "../../api/baseUrl";
import PrivateChat from "../chat/PrivateChat";
import "./TutorConversaciones.css";

const DEFAULT_AVATAR =
  "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 120 120'%3E%3Ccircle cx='60' cy='60' r='60' fill='%23E6EAF3'/%3E%3Ccircle cx='60' cy='46' r='22' fill='%2395A1BB'/%3E%3Cpath d='M20 106c6-20 22-32 40-32s34 12 40 32' fill='%2395A1BB'/%3E%3C/svg%3E";

const resolvePhoto = (raw) => {
  if (!raw || !String(raw).trim()) return DEFAULT_AVATAR;
  const v = String(raw).trim();
  if (/^https?:\/\//i.test(v) || v.startsWith("data:")) return v;
  const base = getApiBaseUrl();
  return v.startsWith("/") ? `${base}${v}` : `${base}/${v}`;
};

/**
 * Panel de conversaciones privadas del tutor en su propio perfil.
 * Muestra la lista de alumnos que le han contactado y permite abrir el chat.
 */
const TutorConversaciones = ({ usuarioActual }) => {
  const navigate = useNavigate();
  const { socket, isConnected } = useSocketContext();
  const [conversaciones, setConversaciones] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedUser, setSelectedUser] = useState(null);

  const cargarConversaciones = useCallback(async () => {
    try {
      setLoading(true);
      const { data } = await obtenerConversaciones();
      setConversaciones(Array.isArray(data) ? data : []);
    } catch {
      setConversaciones([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    cargarConversaciones();
  }, [cargarConversaciones]);

  // Refrescar conversaciones cuando llega un nuevo DM
  useEffect(() => {
    if (!socket || !isConnected) return;

    const handleNewDm = () => {
      cargarConversaciones();
    };

    socket.on("dm_message", handleNewDm);
    return () => socket.off("dm_message", handleNewDm);
  }, [socket, isConnected, cargarConversaciones]);

  if (loading) return <p className="tc-loading">Cargando conversaciones…</p>;

  return (
    <section className="tc-panel">
      <h2 className="tc-panel__title">💬 Mis conversaciones</h2>

      {selectedUser ? (
        <div className="tc-chat-wrapper">
          <button
            className="tc-back-btn"
            onClick={() => setSelectedUser(null)}
          >
            ← Volver a conversaciones
          </button>
          <PrivateChat
            tutorId={selectedUser.id}
            tutorNombre={selectedUser.nombre}
            usuarioActual={usuarioActual}
            headerActions={(
              <button
                type="button"
                className="private-chat-close"
                onClick={() => navigate(`/perfil/${selectedUser.id}`)}
              >
                Ver perfil
              </button>
            )}
            onClose={() => setSelectedUser(null)}
          />
        </div>
      ) : conversaciones.length === 0 ? (
        <p className="tc-empty">
          Aún no tienes conversaciones. Cuando un alumno te contacte, aparecerá aquí.
        </p>
      ) : (
        <div className="tc-list">
          {conversaciones.map((c) => (
            <button
              key={c.usuarioId}
              className="tc-item"
              onClick={() =>
                setSelectedUser({
                  id: c.usuarioId,
                  nombre: c.usuarioNombre || `Usuario ${c.usuarioId}`,
                  foto: c.usuarioFoto,
                })
              }
            >
              <img
                className="tc-item__avatar"
                src={resolvePhoto(c.usuarioFoto)}
                alt={c.usuarioNombre}
                onError={(e) => { e.target.src = DEFAULT_AVATAR; }}
              />
              <div className="tc-item__info">
                <span className="tc-item__name">
                  {c.usuarioNombre || `Usuario ${c.usuarioId}`}
                </span>
                {c.ultimoMensaje && (
                  <span className="tc-item__last">
                    {c.ultimoMensaje.length > 60
                      ? c.ultimoMensaje.slice(0, 60) + "…"
                      : c.ultimoMensaje}
                  </span>
                )}
              </div>
            </button>
          ))}
        </div>
      )}
    </section>
  );
};

export default TutorConversaciones;

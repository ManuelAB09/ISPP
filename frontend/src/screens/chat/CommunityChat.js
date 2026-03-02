import React, { useEffect, useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSocketContext } from '../../contexts/SocketContext';
import {
    enviarMensajeComunidad,
    obtenerHistorialComunidad,
    editarMensajeComunidad,
    eliminarMensajeComunidad,
} from '../../api/mensajeService';
import { LuMessageCircle, LuX } from 'react-icons/lu';
import './CommunityChat.css';

/**
 * Componente de chat en tiempo real para comunidades.
 * @param {object} props - Props del componente.
 * @param {number} props.comunidadId - ID de la comunidad.
 * @param {object} props.usuarioActual - Información del usuario autenticado.
 */
const CommunityChat = ({
    comunidadId,
    usuarioActual,
    comunidadNombre,
    comunidadImagen,
    initiallyOpen = false,
    mode = 'floating',
    onOpenPrivateChat,
}) => {
    const isEmbedded = mode === 'embedded';
    const navigate = useNavigate();
    const { socket, isConnected } = useSocketContext();
    const [mensajes, setMensajes] = useState([]);
    const [contenido, setContenido] = useState('');
    const [cargandoHistorial, setCargandoHistorial] = useState(false);
    const [enviando, setEnviando] = useState(false);
    const [error, setError] = useState(null);
    const [chatAbierto, setChatAbierto] = useState(isEmbedded ? true : initiallyOpen);
    const [editandoId, setEditandoId] = useState(null);
    const [contenidoEditado, setContenidoEditado] = useState('');
    const [procesandoId, setProcesandoId] = useState(null);
    const messagesEndRef = useRef(null);

    const isOwnMessage = (msg) => Number(msg?.usuarioId) === Number(usuarioActual?.id);

    /**
     * Carga el historial de mensajes al montar el componente.
     */
    useEffect(() => {
        const cargarHistorial = async () => {
            try {
                setCargandoHistorial(true);
                const { data } = await obtenerHistorialComunidad(comunidadId);
                setMensajes(data);
            } catch (err) {
                setError('Error al cargar el historial de mensajes');
                console.error(err);
            } finally {
                setCargandoHistorial(false);
            }
        };

        cargarHistorial();
    }, [comunidadId]);

    /**
     * Suscribe al socket para recibir mensajes en tiempo real.
     */
    useEffect(() => {
        if (!socket || !isConnected) return;

        // Escuchar nuevos mensajes en la comunidad
        const handleNewMessage = (payload) => {
            if (payload?.type === 'message_deleted' && payload?.messageId) {
                setMensajes((prev) => prev.filter((msg) => msg.id !== payload.messageId));
                return;
            }

            setMensajes((prev) => {
                if (!payload?.id) {
                    return prev;
                }

                const index = prev.findIndex((msg) => msg.id === payload.id);
                if (index >= 0) {
                    const updated = [...prev];
                    updated[index] = { ...updated[index], ...payload };
                    return updated;
                }

                return [...prev, payload];
            });
        };

        // Escuchar errores del socket
        const handleSocketError = (error) => {
            setError(`Error: ${error.message}`);
        };

        socket.on(`/topic/community.${comunidadId}`, handleNewMessage);
        socket.on('error', handleSocketError);

        return () => {
            socket.off(`/topic/community.${comunidadId}`, handleNewMessage);
            socket.off('error', handleSocketError);
        };
    }, [socket, isConnected, comunidadId]);

    useEffect(() => {
        if (chatAbierto) {
            scrollToBottom();
        }
    }, [mensajes, chatAbierto]);

    useEffect(() => {
        if (isEmbedded) {
            setChatAbierto(true);
            return;
        }
        setChatAbierto(initiallyOpen);
    }, [initiallyOpen, isEmbedded]);

    /**
     * Desplaza la vista al último mensaje.
     */
    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    /**
     * Envía un mensaje a través del socket.
     */
    const handleEnviarMensaje = async (e) => {
        e.preventDefault();

        if (!contenido.trim() || enviando) return;

        try {
            setEnviando(true);
            setError(null);

            const messageContent = contenido.trim();
            const { data } = await enviarMensajeComunidad(comunidadId, messageContent);
            setMensajes((prev) => {
                if (!data?.id) {
                    return prev;
                }
                if (prev.some((msg) => msg.id === data.id)) {
                    return prev;
                }
                return [...prev, data];
            });

            setContenido('');
            scrollToBottom();
        } catch (err) {
            setError('Error al enviar el mensaje');
            console.error(err);
        } finally {
            setEnviando(false);
        }
    };

    /**
     * Edita un mensaje.
     */
    const startEditarMensaje = (mensajeId, contenidoActual) => {
        setEditandoId(mensajeId);
        setContenidoEditado(contenidoActual);
    };

    const cancelEditarMensaje = () => {
        setEditandoId(null);
        setContenidoEditado('');
    };

    const handleEditarMensaje = async (mensajeId) => {
        const nuevoContenido = contenidoEditado.trim();
        if (!nuevoContenido) return;

        try {
            setError(null);
            setProcesandoId(mensajeId);
            const { data } = await editarMensajeComunidad(comunidadId, mensajeId, nuevoContenido);
            setMensajes((prev) => prev.map((msg) => (msg.id === mensajeId ? { ...msg, ...data } : msg)));

            cancelEditarMensaje();
        } catch (err) {
            setError('Error al editar el mensaje');
            console.error(err);
        } finally {
            setProcesandoId(null);
        }
    };

    /**
     * Elimina un mensaje.
     */
    const handleEliminarMensaje = async (mensajeId) => {
        try {
            setError(null);
            setProcesandoId(mensajeId);
            await eliminarMensajeComunidad(comunidadId, mensajeId);
            setMensajes((prev) => prev.filter((msg) => msg.id !== mensajeId));
        } catch (err) {
            setError('Error al eliminar el mensaje');
            console.error(err);
        } finally {
            setProcesandoId(null);
        }
    };

    const handleOpenLargeChat = () => {
        navigate(`/chats?communityId=${comunidadId}`);
    };

    const handleOpenPrivateChat = (msg) => {
        const targetId = Number(msg?.usuarioId);
        if (!targetId || Number(usuarioActual?.id) === targetId) {
            return;
        }

        const payload = {
            userId: targetId,
            userName: msg?.usuarioNombre || `Usuario ${targetId}`,
            userPhoto: msg?.usuarioFoto || '',
        };

        if (onOpenPrivateChat) {
            onOpenPrivateChat(payload);
            return;
        }

        const params = new URLSearchParams({
            communityId: String(comunidadId),
            userId: String(targetId),
            userName: payload.userName,
        });
        if (payload.userPhoto) {
            params.set('userPhoto', payload.userPhoto);
        }
        navigate(`/chats?${params.toString()}`);
    };

    return (
        <div className={isEmbedded ? 'community-chat-embedded' : 'community-chat-floating'}>
            {!isEmbedded && (
                <button
                    type="button"
                    className="chat-toggle-button"
                    onClick={() => setChatAbierto((prev) => !prev)}
                    aria-expanded={chatAbierto}
                    aria-label="Abrir chat de comunidad"
                >
                    <LuMessageCircle size={20} />
                    <span>Chat</span>
                </button>
            )}

            {!chatAbierto && !isEmbedded ? null : (
                <aside className={`community-chat-panel ${isEmbedded ? 'embedded' : ''}`}>
                    <div className="chat-header">
                        <div className="chat-header-left">
                            <img
                                className="chat-header-avatar"
                                src={comunidadImagen || '/MeerKatters_logo.png'}
                                alt={comunidadNombre || `Comunidad ${comunidadId}`}
                            />
                            <div>
                                <h2>Chat de comunidad</h2>
                                <p>{comunidadNombre || `Comunidad #${comunidadId}`}</p>
                            </div>
                        </div>
                        <div className="chat-header-right">
                            <div className={`status ${isConnected ? 'conectado' : 'desconectado'}`}>
                                {isConnected ? 'En línea' : 'Fuera de línea'}
                            </div>
                            {!isEmbedded && (
                                <button
                                    type="button"
                                    className="chat-open-large-button"
                                    onClick={handleOpenLargeChat}
                                >
                                    Abrir grande
                                </button>
                            )}
                            {!isEmbedded && (
                                <button
                                    type="button"
                                    className="chat-close-button"
                                    onClick={() => setChatAbierto(false)}
                                    aria-label="Cerrar chat"
                                >
                                    <LuX size={18} />
                                </button>
                            )}
                        </div>
                    </div>

                    {error && <div className="error-message">{error}</div>}

                    <div className="messages-container">
                        {cargandoHistorial ? (
                            <div className="loading">Cargando mensajes...</div>
                        ) : mensajes.length === 0 ? (
                            <div className="empty-state">No hay mensajes aún. ¡Sé el primero en escribir!</div>
                        ) : (
                            mensajes.map((msg) => (
                                <div
                                    key={msg.id}
                                    className={`message ${isOwnMessage(msg) ? 'propio' : 'otro'}`}
                                >
                                    <div className="message-header">
                                        <button
                                            type="button"
                                            className={`open-private-trigger ${isOwnMessage(msg) ? 'disabled' : ''}`}
                                            onClick={() => handleOpenPrivateChat(msg)}
                                            disabled={isOwnMessage(msg)}
                                            title={isOwnMessage(msg) ? 'Tu mensaje' : 'Hablar por privado'}
                                        >
                                            <img
                                                className="usuario-foto"
                                                src={msg.usuarioFoto || '/MeerKatters_logo.png'}
                                                alt={msg.usuarioNombre || 'Usuario'}
                                            />
                                            <span className="usuario-nombre">{msg.usuarioNombre}</span>
                                        </button>
                                        {msg.editado && <span className="editado-badge">(editado)</span>}
                                        <span className="timestamp">
                                            {new Date(msg.createdAt).toLocaleTimeString()}
                                        </span>
                                    </div>

                                    {editandoId === msg.id ? (
                                        <div className="edit-form">
                                            <input
                                                type="text"
                                                value={contenidoEditado}
                                                onChange={(e) => setContenidoEditado(e.target.value)}
                                                maxLength={1000}
                                            />
                                            <div className="edit-actions">
                                                <button
                                                    type="button"
                                                    className="btn-save"
                                                    onClick={() => handleEditarMensaje(msg.id)}
                                                    disabled={!contenidoEditado.trim() || procesandoId === msg.id}
                                                >
                                                    {procesandoId === msg.id ? 'Guardando...' : 'Guardar'}
                                                </button>
                                                <button
                                                    type="button"
                                                    className="btn-cancel"
                                                    onClick={cancelEditarMensaje}
                                                    disabled={procesandoId === msg.id}
                                                >
                                                    Cancelar
                                                </button>
                                            </div>
                                        </div>
                                    ) : (
                                        <div className="message-content">{msg.contenido}</div>
                                    )}

                                    {isOwnMessage(msg) && editandoId !== msg.id && (
                                        <div className="message-actions">
                                            <button
                                                className="btn-edit"
                                                onClick={() => startEditarMensaje(msg.id, msg.contenido)}
                                                disabled={procesandoId === msg.id}
                                            >
                                                Editar
                                            </button>
                                            <button
                                                className="btn-delete"
                                                onClick={() => handleEliminarMensaje(msg.id)}
                                                disabled={procesandoId === msg.id}
                                            >
                                                {procesandoId === msg.id ? 'Eliminando...' : 'Eliminar'}
                                            </button>
                                        </div>
                                    )}
                                </div>
                            ))
                        )}
                        <div ref={messagesEndRef} />
                    </div>

                    <form onSubmit={handleEnviarMensaje} className="message-input-form">
                        <input
                            type="text"
                            placeholder="Escribe un mensaje..."
                            value={contenido}
                            onChange={(e) => setContenido(e.target.value)}
                            disabled={enviando}
                            maxLength={1000}
                        />
                        <button type="submit" disabled={enviando || !contenido.trim()}>
                            {enviando ? 'Enviando...' : 'Enviar'}
                        </button>
                    </form>
                </aside>
            )}
        </div>
    );
};

export default CommunityChat;

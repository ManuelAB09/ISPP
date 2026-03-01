import React, { useEffect, useState, useRef } from 'react';
import { useSocketContext } from '../contexts/SocketContext';
import {
    enviarMensajeComunidad,
    obtenerHistorialComunidad,
    eliminarMensajeComunidad,
} from '../api/mensajeService';
import './CommunityChat.css';

/**
 * Componente de chat en tiempo real para comunidades.
 * @param {object} props - Props del componente.
 * @param {number} props.comunidadId - ID de la comunidad.
 * @param {object} props.usuarioActual - Información del usuario autenticado.
 */
const CommunityChat = ({ comunidadId, usuarioActual }) => {
    const { socket, isConnected } = useSocketContext();
    const [mensajes, setMensajes] = useState([]);
    const [contenido, setContenido] = useState('');
    const [cargando, setCargando] = useState(false);
    const [error, setError] = useState(null);
    const messagesEndRef = useRef(null);

    /**
     * Carga el historial de mensajes al montar el componente.
     */
    useEffect(() => {
        const cargarHistorial = async () => {
            try {
                setCargando(true);
                const { data } = await obtenerHistorialComunidad(comunidadId);
                setMensajes(data);
            } catch (err) {
                setError('Error al cargar el historial de mensajes');
                console.error(err);
            } finally {
                setCargando(false);
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
                if (prev.some((msg) => msg.id === payload.id)) {
                    return prev;
                }
                return [...prev, payload];
            });
            scrollToBottom();
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

        if (!contenido.trim() || cargando) return;

        try {
            setCargando(true);
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
            setCargando(false);
        }
    };

    /**
     * Edita un mensaje.
     */
    const handleEditarMensaje = async (mensajeId, nuevoContenido) => {
        try {
            setError(null);
            // Aquí iría la lógica para editar via HTTP o socket
            console.log('Editar mensaje:', mensajeId, nuevoContenido);
        } catch (err) {
            setError('Error al editar el mensaje');
            console.error(err);
        }
    };

    /**
     * Elimina un mensaje.
     */
    const handleEliminarMensaje = async (mensajeId) => {
        try {
            setError(null);

            if (isConnected && socket) {
                socket.emit('community.message.delete', {
                    messageId: mensajeId,
                    comunidadId,
                });
            } else {
                await eliminarMensajeComunidad(comunidadId, mensajeId);
                setMensajes((prev) => prev.filter((msg) => msg.id !== mensajeId));
            }
        } catch (err) {
            setError('Error al eliminar el mensaje');
            console.error(err);
        }
    };

    return (
        <div className="community-chat">
            <div className="chat-header">
                <h2>Chat - Comunidad #{comunidadId}</h2>
                <div className={`status ${isConnected ? 'conectado' : 'desconectado'}`}>
                    {isConnected ? '⚪ En línea' : '⚫ Fuera de línea'}
                </div>
            </div>

            {error && <div className="error-message">{error}</div>}

            <div className="messages-container">
                {cargando ? (
                    <div className="loading">Cargando mensajes...</div>
                ) : mensajes.length === 0 ? (
                    <div className="empty-state">No hay mensajes aún. ¡Sé el primero en escribir!</div>
                ) : (
                    mensajes.map((msg) => (
                        <div
                            key={msg.id}
                            className={`message ${msg.usuarioId === usuarioActual.id ? 'propio' : 'otro'}`}
                        >
                            <div className="message-header">
                                <span className="usuario-nombre">{msg.usuarioNombre}</span>
                                {msg.editado && <span className="editado-badge">(editado)</span>}
                                <span className="timestamp">
                                    {new Date(msg.createdAt).toLocaleTimeString()}
                                </span>
                            </div>
                            <div className="message-content">{msg.contenido}</div>
                            {msg.usuarioId === usuarioActual.id && (
                                <div className="message-actions">
                                    <button
                                        className="btn-edit"
                                        onClick={() => handleEditarMensaje(msg.id, msg.contenido)}
                                    >
                                        Editar
                                    </button>
                                    <button
                                        className="btn-delete"
                                        onClick={() => handleEliminarMensaje(msg.id)}
                                    >
                                        Eliminar
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
                    disabled={cargando}
                    maxLength={1000}
                />
                <button type="submit" disabled={cargando || !contenido.trim()}>
                    {cargando ? 'Enviando...' : 'Enviar'}
                </button>
            </form>
        </div>
    );
};

export default CommunityChat;

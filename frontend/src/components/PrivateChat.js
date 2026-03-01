import React, { useEffect, useState, useRef } from 'react';
import { useSocketContext } from '../contexts/SocketContext';
import { enviarMensajePrivado, obtenerHistorialPrivado } from '../api/mensajeService';
import './PrivateChat.css';

/**
 * Componente de chat privado en tiempo real con otro usuario.
 * @param {object} props - Props del componente.
 * @param {number} props.tutorId - ID del tutor/usuario destino.
 * @param {string} props.tutorNombre - Nombre del tutor.
 * @param {object} props.usuarioActual - Información del usuario autenticado.
 */
const PrivateChat = ({ tutorId, tutorNombre, usuarioActual }) => {
    const { socket, isConnected } = useSocketContext();
    const [mensajes, setMensajes] = useState([]);
    const [contenido, setContenido] = useState('');
    const [cargando, setCargando] = useState(false);
    const [error, setError] = useState(null);
    const messagesEndRef = useRef(null);

    /**
     * Carga el historial de mensajes privados al montar.
     */
    useEffect(() => {
        const cargarHistorial = async () => {
            try {
                setCargando(true);
                const { data } = await obtenerHistorialPrivado(tutorId);
                setMensajes(data);
            } catch (err) {
                setError('Error al cargar el historial de mensajes');
                console.error(err);
            } finally {
                setCargando(false);
            }
        };

        cargarHistorial();
    }, [tutorId]);

    /**
     * Suscribe al socket para recibir mensajes privados en tiempo real.
     */
    useEffect(() => {
        if (!socket || !isConnected) return;

        // Escuchar nuevos mensajes privados
        const handleNewDM = (nuevoMensaje) => {
            const currentUserNumericId = Number(usuarioActual?.id);
            const isCurrentConversation =
                (nuevoMensaje?.emisorId === tutorId && nuevoMensaje?.receptorId === currentUserNumericId) ||
                (nuevoMensaje?.emisorId === currentUserNumericId && nuevoMensaje?.receptorId === tutorId);

            if (!isCurrentConversation) {
                return;
            }

            setMensajes((prev) => [...prev, nuevoMensaje]);
            scrollToBottom();
        };

        // Escuchar eliminación de mensajes
        const handleDMDeleted = (messageId) => {
            setMensajes((prev) => prev.filter((msg) => msg.id !== messageId));
        };

        // Escuchar errores
        const handleSocketError = (error) => {
            setError(`Error: ${error.message}`);
        };

        socket.on('dm_message', handleNewDM);
        socket.on('dm_delete_success', handleDMDeleted);
        socket.on('error', handleSocketError);

        return () => {
            socket.off('dm_message', handleNewDM);
            socket.off('dm_delete_success', handleDMDeleted);
            socket.off('error', handleSocketError);
        };
    }, [socket, isConnected, tutorId, usuarioActual?.id]);

    /**
     * Desplaza la vista al último mensaje.
     */
    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    /**
     * Envía un mensaje privado.
     */
    const handleEnviarMensaje = async (e) => {
        e.preventDefault();

        if (!contenido.trim() || cargando) return;

        try {
            setCargando(true);
            setError(null);

            const { data } = await enviarMensajePrivado(tutorId, contenido.trim());
            setMensajes((prev) => [...prev, data]);
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
     * Elimina un mensaje privado.
     */
    const handleEliminarMensaje = async (mensajeId) => {
        try {
            socket?.emit('dm.delete', { messageId: mensajeId });
        } catch (err) {
            setError('Error al eliminar el mensaje');
            console.error(err);
        }
    };

    return (
        <div className="private-chat">
            <div className="chat-header">
                <h2>Chat con {tutorNombre}</h2>
                <div className={`status ${isConnected ? 'conectado' : 'desconectado'}`}>
                    {isConnected ? '⚪ En línea' : '⚫ Fuera de línea'}
                </div>
            </div>

            {error && <div className="error-message">{error}</div>}

            <div className="messages-container">
                {cargando ? (
                    <div className="loading">Cargando historial...</div>
                ) : mensajes.length === 0 ? (
                    <div className="empty-state">
                        Sin historial de mensajes. ¡Inicia la conversación!
                    </div>
                ) : (
                    mensajes.map((msg) => (
                        <div
                            key={msg.id}
                            className={`message ${msg.emisorId === usuarioActual.id ? 'propio' : 'otro'}`}
                        >
                            <div className="message-content">{msg.contenido}</div>
                            <div className="message-footer">
                                <span className="timestamp">
                                    {new Date(msg.createdAt).toLocaleTimeString()}
                                </span>
                                {msg.editado && <span className="editado-badge">(editado)</span>}
                            </div>
                            {msg.emisorId === usuarioActual.id && (
                                <div className="message-actions">
                                    <button
                                        className="btn-delete"
                                        onClick={() => handleEliminarMensaje(msg.id)}
                                        title="Eliminar mensaje"
                                    >
                                        ✕
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
                    {cargando ? '...' : '→'}
                </button>
            </form>
        </div>
    );
};

export default PrivateChat;

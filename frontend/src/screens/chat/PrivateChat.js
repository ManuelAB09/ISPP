import React, { useEffect, useState, useRef } from 'react';
import { useSocketContext } from '../../contexts/SocketContext';
import { enviarMensajePrivado, obtenerHistorialPrivado, eliminarMensajePrivado } from '../../api/mensajeService';
import './PrivateChat.css';

/**
 * Componente de chat privado en tiempo real con otro usuario.
 * @param {object} props - Props del componente.
 * @param {number} props.tutorId - ID del usuario destino.
 * @param {string} props.tutorNombre - Nombre del usuario destino.
 * @param {object} props.usuarioActual - Información del usuario autenticado.
 */
const PrivateChat = ({ tutorId, tutorNombre, usuarioActual, onClose }) => {
    const { socket, isConnected } = useSocketContext();
    const [mensajes, setMensajes] = useState([]);
    const [contenido, setContenido] = useState('');
    const [cargandoHistorial, setCargandoHistorial] = useState(false);
    const [enviando, setEnviando] = useState(false);
    const [procesandoId, setProcesandoId] = useState(null);
    const [error, setError] = useState(null);
    const messagesEndRef = useRef(null);

    /**
     * Carga el historial de mensajes privados al montar.
     */
    useEffect(() => {
        const cargarHistorial = async () => {
            try {
                setCargandoHistorial(true);
                const { data } = await obtenerHistorialPrivado(tutorId);
                setMensajes(data);
            } catch (err) {
                setError('Error al cargar el historial de mensajes');
                console.error(err);
            } finally {
                setCargandoHistorial(false);
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
            const otherUserNumericId = Number(tutorId);
            const isCurrentConversation =
                (Number(nuevoMensaje?.emisorId) === otherUserNumericId && Number(nuevoMensaje?.receptorId) === currentUserNumericId) ||
                (Number(nuevoMensaje?.emisorId) === currentUserNumericId && Number(nuevoMensaje?.receptorId) === otherUserNumericId);

            if (!isCurrentConversation) {
                return;
            }

            setMensajes((prev) => {
                if (!nuevoMensaje?.id) {
                    return prev;
                }
                if (prev.some((msg) => msg.id === nuevoMensaje.id)) {
                    return prev;
                }
                return [...prev, nuevoMensaje];
            });
            scrollToBottom();
        };

        // Escuchar eliminación de mensajes
        const handleDMDeleted = (payload) => {
            const deletedId = typeof payload === 'number' ? payload : payload?.messageId;
            if (!deletedId) return;
            setMensajes((prev) => prev.filter((msg) => msg.id !== deletedId));
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

        if (!contenido.trim() || enviando) return;

        try {
            setEnviando(true);
            setError(null);

            const { data } = await enviarMensajePrivado(tutorId, contenido.trim());
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
     * Elimina un mensaje privado.
     */
    const handleEliminarMensaje = async (mensajeId) => {
        try {
            setProcesandoId(mensajeId);
            await eliminarMensajePrivado(mensajeId);
            setMensajes((prev) => prev.filter((msg) => msg.id !== mensajeId));
        } catch (err) {
            setError('Error al eliminar el mensaje');
            console.error(err);
        } finally {
            setProcesandoId(null);
        }
    };

    return (
        <div className="private-chat">
            <div className="chat-header">
                <h2>Chat con {tutorNombre}</h2>
                <div className="private-chat-header-actions">
                    <div className={`status ${isConnected ? 'conectado' : 'desconectado'}`}>
                        {isConnected ? 'En línea' : 'Fuera de línea'}
                    </div>
                    {onClose && (
                        <button type="button" className="private-chat-close" onClick={onClose}>
                            Volver
                        </button>
                    )}
                </div>
            </div>

            {error && <div className="error-message">{error}</div>}

            <div className="messages-container">
                {cargandoHistorial ? (
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
                                        disabled={procesandoId === msg.id}
                                    >
                                        {procesandoId === msg.id ? '...' : '✕'}
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
                    {enviando ? '...' : '→'}
                </button>
            </form>
        </div>
    );
};

export default PrivateChat;

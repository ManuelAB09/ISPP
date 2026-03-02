import React, { useEffect, useState, useRef } from 'react';
import { useSocketContext } from '../../contexts/SocketContext';
import { enviarMensajePrivado, obtenerHistorialPrivado, eliminarMensajePrivado, editarMensajePrivado } from '../../api/mensajeService';
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
    const [editandoId, setEditandoId] = useState(null);
    const [contenidoEditado, setContenidoEditado] = useState('');
    const messagesEndRef = useRef(null);

    // helper para determinar si un mensaje pertenece al usuario actual
    const isOwnMessage = (msg) =>
        Number(msg?.emisorId) === Number(usuarioActual?.id);

    /**
     * Carga el historial de mensajes privados al montar.
     */
    useEffect(() => {
        const cargarHistorial = async () => {
            try {
                setCargandoHistorial(true);
                const { data } = await obtenerHistorialPrivado(tutorId);
                // normalizar IDs numéricos para evitar discrepancias de tipo
                setMensajes(
                    Array.isArray(data)
                        ? data.map((m) => ({
                              ...m,
                              emisorId: Number(m.emisorId),
                              receptorId: Number(m.receptorId),
                          }))
                        : []
                );
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

        // Escuchar edición de mensajes
        const handleDMUpdated = (payload) => {
            if (!payload?.id) return;
            setMensajes((prev) =>
                prev.map((msg) => (msg.id === payload.id ? { ...msg, ...payload } : msg))
            );
        };

        // Escuchar errores
        const handleSocketError = (error) => {
            setError(`Error: ${error.message}`);
        };

        socket.on('dm_message', (nuevoMensaje) => {
            // normalizar antes de procesar
            const normalized = nuevoMensaje
                ? {
                      ...nuevoMensaje,
                      emisorId: Number(nuevoMensaje.emisorId),
                      receptorId: Number(nuevoMensaje.receptorId),
                  }
                : nuevoMensaje;
            handleNewDM(normalized);
        });
        socket.on('dm_delete_success', handleDMDeleted);
        socket.on('dm_update_success', handleDMUpdated);
        socket.on('error', handleSocketError);

        return () => {
            socket.off('dm_message', handleNewDM);
            socket.off('dm_delete_success', handleDMDeleted);
            socket.off('dm_update_success', handleDMUpdated);
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
            // normalizar los campos numéricos
            const msg = data
                ? {
                      ...data,
                      emisorId: Number(data.emisorId),
                      receptorId: Number(data.receptorId),
                  }
                : data;
            setMensajes((prev) => {
                if (!msg?.id) {
                    return prev;
                }
                if (prev.some((m) => m.id === msg.id)) {
                    return prev;
                }
                return [...prev, msg];
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
            setError(null);
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

    /**
     * Edita un mensaje privado.
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
            const { data } = await editarMensajePrivado(mensajeId, nuevoContenido);
            setMensajes((prev) => prev.map((msg) => (msg.id === mensajeId ? { ...msg, ...data } : msg)));

            cancelEditarMensaje();
        } catch (err) {
            setError('Error al editar el mensaje');
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
                    mensajes.map((msg) => {
                        const own = isOwnMessage(msg);
                        return (
                            <div
                                key={msg.id}
                                className={`message ${own ? 'propio' : 'otro'}`}
                            >
                            <div className="message-header">
                                <span className="timestamp">
                                    {new Date(msg.createdAt).toLocaleTimeString([], {
                                        hour: '2-digit',
                                        minute: '2-digit'
                                        })}
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

                            <div className="message-footer">
                                {msg.editado && <span className="editado-badge">(editado)</span>}
                            </div>

                            {own && editandoId !== msg.id && (
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
                        );
                    })
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

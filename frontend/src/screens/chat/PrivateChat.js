import React, { useEffect, useState, useRef } from 'react';
import { useSocketContext } from '../../contexts/SocketContext';
import {
    enviarMensajePrivado,
    enviarArchivoPrivado,
    obtenerHistorialPrivado,
    eliminarMensajePrivado,
    editarMensajePrivado,
    obtenerPreviewEnlace,
    obtenerArchivoChatBlob,
} from '../../api/mensajeService';
import { extractFirstUrl } from '../../utils/linkPreview';
import LinkPreviewCard from './LinkPreviewCard';
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
    const [previewsByMessageId, setPreviewsByMessageId] = useState({});
    const [archivoSeleccionado, setArchivoSeleccionado] = useState(null);
    const [archivoSeleccionadoPreviewUrl, setArchivoSeleccionadoPreviewUrl] = useState('');
    const [descargandoId, setDescargandoId] = useState(null);
    const [abriendoId, setAbriendoId] = useState(null);
    const [attachmentPreviewByMessageId, setAttachmentPreviewByMessageId] = useState({});
    const messagesEndRef = useRef(null);
    const fileInputRef = useRef(null);
    const previewCacheByUrlRef = useRef(new Map());
    const pendingPreviewKeysRef = useRef(new Set());
    const previewsByMessageIdRef = useRef({});
    const attachmentObjectUrlsRef = useRef(new Map());
    const pendingAttachmentPreviewRef = useRef(new Set());

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

    useEffect(() => {
        previewsByMessageIdRef.current = previewsByMessageId;
    }, [previewsByMessageId]);

    useEffect(() => {
        const objectUrls = attachmentObjectUrlsRef.current;
        return () => {
            objectUrls.forEach((url) => URL.revokeObjectURL(url));
            objectUrls.clear();
        };
    }, []);

    useEffect(() => {
        if (!archivoSeleccionado) {
            setArchivoSeleccionadoPreviewUrl('');
            return;
        }

        const mimeType = String(archivoSeleccionado.type || '').toLowerCase();
        if (!mimeType.startsWith('image/')) {
            setArchivoSeleccionadoPreviewUrl('');
            return;
        }

        const objectUrl = URL.createObjectURL(archivoSeleccionado);
        setArchivoSeleccionadoPreviewUrl(objectUrl);

        return () => {
            URL.revokeObjectURL(objectUrl);
        };
    }, [archivoSeleccionado]);

    useEffect(() => {
        const activeIds = new Set(
            mensajes
                .map((msg) => msg?.id)
                .filter((id) => id !== null && id !== undefined)
                .map((id) => String(id))
        );

        setPreviewsByMessageId((prev) => {
            let changed = false;
            const next = { ...prev };

            Object.keys(next).forEach((messageId) => {
                if (!activeIds.has(messageId)) {
                    delete next[messageId];
                    changed = true;
                }
            });

            return changed ? next : prev;
        });

        mensajes.forEach((msg) => {
            const messageId = msg?.id;
            if (messageId === null || messageId === undefined) {
                return;
            }

            const messageKey = String(messageId);
            const extractedUrl = extractFirstUrl(msg?.contenido);

            if (!extractedUrl) {
                setPreviewsByMessageId((prev) => {
                    if (!prev[messageKey]) {
                        return prev;
                    }
                    const next = { ...prev };
                    delete next[messageKey];
                    return next;
                });
                return;
            }

            const currentEntry = previewsByMessageIdRef.current[messageKey];
            if (currentEntry?.url === extractedUrl) {
                return;
            }

            if (previewCacheByUrlRef.current.has(extractedUrl)) {
                const cachedPreview = previewCacheByUrlRef.current.get(extractedUrl);
                setPreviewsByMessageId((prev) => ({
                    ...prev,
                    [messageKey]: { url: extractedUrl, preview: cachedPreview },
                }));
                return;
            }

            const pendingKey = `${messageKey}:${extractedUrl}`;
            if (pendingPreviewKeysRef.current.has(pendingKey)) {
                return;
            }

            pendingPreviewKeysRef.current.add(pendingKey);

            obtenerPreviewEnlace(extractedUrl)
                .then(({ data }) => {
                    previewCacheByUrlRef.current.set(extractedUrl, data || null);
                    setPreviewsByMessageId((prev) => ({
                        ...prev,
                        [messageKey]: { url: extractedUrl, preview: data || null },
                    }));
                })
                .catch(() => {
                    previewCacheByUrlRef.current.set(extractedUrl, null);
                });
        });
    }, [mensajes]);

    useEffect(() => {
        const activeIds = new Set(
            mensajes
                .map((msg) => msg?.id)
                .filter((id) => id !== null && id !== undefined)
                .map((id) => String(id))
        );

        Object.keys(attachmentPreviewByMessageId).forEach((messageId) => {
            if (!activeIds.has(messageId)) {
                const objectUrl = attachmentObjectUrlsRef.current.get(messageId);
                if (objectUrl) {
                    URL.revokeObjectURL(objectUrl);
                    attachmentObjectUrlsRef.current.delete(messageId);
                }
                pendingAttachmentPreviewRef.current.delete(messageId);
            }
        });

        setAttachmentPreviewByMessageId((prev) => {
            const next = { ...prev };
            let changed = false;
            Object.keys(next).forEach((messageId) => {
                if (!activeIds.has(messageId)) {
                    delete next[messageId];
                    changed = true;
                }
            });
            return changed ? next : prev;
        });

        mensajes.forEach((msg) => {
            const messageId = msg?.id;
            if (messageId === null || messageId === undefined) {
                return;
            }

            const mimeType = String(msg?.archivoMimeType || '').toLowerCase();
            const isImage = mimeType.startsWith('image/');
            const hasFile = Boolean(msg?.archivoUrl || msg?.archivoNombre);
            const key = String(messageId);

            if (!hasFile || !isImage || !msg?.archivoUrl || attachmentPreviewByMessageId[key]) {
                return;
            }

            if (pendingAttachmentPreviewRef.current.has(key)) {
                return;
            }

            pendingAttachmentPreviewRef.current.add(key);

            obtenerArchivoChatBlob(msg.archivoUrl)
                .then(({ data }) => {
                    const objectUrl = URL.createObjectURL(data);

                    const prevUrl = attachmentObjectUrlsRef.current.get(key);
                    if (prevUrl) {
                        URL.revokeObjectURL(prevUrl);
                    }

                    attachmentObjectUrlsRef.current.set(key, objectUrl);
                    setAttachmentPreviewByMessageId((prev) => ({
                        ...prev,
                        [key]: objectUrl,
                    }));
                })
                .catch(() => {
                    setAttachmentPreviewByMessageId((prev) => ({
                        ...prev,
                        [key]: null,
                    }));
                })
                .finally(() => {
                    pendingAttachmentPreviewRef.current.delete(key);
                });
        });
    }, [mensajes, attachmentPreviewByMessageId]);

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

        if ((!contenido.trim() && !archivoSeleccionado) || enviando) return;

        try {
            setEnviando(true);
            setError(null);

            let data;
            if (archivoSeleccionado) {
                const response = await enviarArchivoPrivado(
                    tutorId,
                    archivoSeleccionado,
                    contenido.trim()
                );
                data = response.data;
            } else {
                const response = await enviarMensajePrivado(tutorId, contenido.trim());
                data = response.data;
            }

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
            clearArchivoSeleccionado();
            scrollToBottom();
        } catch (err) {
            setError('Error al enviar el mensaje o archivo');
            console.error(err);
        } finally {
            setEnviando(false);
        }
    };

    const formatearTamanoArchivo = (bytes) => {
        if (!bytes && bytes !== 0) return '';
        if (bytes < 1024) return `${bytes} B`;
        if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
        return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
    };

    const clearArchivoSeleccionado = () => {
        setArchivoSeleccionado(null);
        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
    };

    const handleDescargarArchivo = async (msg) => {
        if (!msg?.archivoUrl || !msg?.id) return;

        try {
            setDescargandoId(msg.id);
            const { data } = await obtenerArchivoChatBlob(msg.archivoUrl);
            const blobUrl = URL.createObjectURL(data);
            const anchor = document.createElement('a');
            anchor.href = blobUrl;
            anchor.download = msg.archivoNombre || 'adjunto';
            document.body.appendChild(anchor);
            anchor.click();
            anchor.remove();
            URL.revokeObjectURL(blobUrl);
        } catch (err) {
            setError('No se pudo descargar el archivo');
            console.error(err);
        } finally {
            setDescargandoId(null);
        }
    };

    const handleAbrirArchivo = async (msg) => {
        if (!msg?.archivoUrl || !msg?.id) return;

        try {
            setAbriendoId(msg.id);
            const { data } = await obtenerArchivoChatBlob(msg.archivoUrl);
            const blobUrl = URL.createObjectURL(data);
            window.open(blobUrl, '_blank', 'noopener,noreferrer');
            setTimeout(() => URL.revokeObjectURL(blobUrl), 60_000);
        } catch (err) {
            setError('No se pudo abrir el archivo');
            console.error(err);
        } finally {
            setAbriendoId(null);
        }
    };

    const renderAdjunto = (msg) => {
        if (!msg?.archivoNombre && !msg?.archivoUrl) {
            return null;
        }

        const mimeType = String(msg?.archivoMimeType || '').toLowerCase();
        const isImage = mimeType.startsWith('image/');
        const previewSrc = attachmentPreviewByMessageId[String(msg.id)];

        if (isImage && previewSrc) {
            return (
                <div className="chat-attachment-image-block">
                    <button
                        type="button"
                        className="chat-attachment-image-trigger"
                        onClick={() => handleAbrirArchivo(msg)}
                        disabled={abriendoId === msg.id}
                        aria-label="Abrir imagen adjunta"
                    >
                        <img
                            src={previewSrc}
                            alt={msg.archivoNombre || 'Imagen adjunta'}
                            className="chat-attachment-image-large"
                            loading="lazy"
                        />
                    </button>

                    <div className="chat-attachment-image-footer">
                        <span className="chat-attachment-name">{msg.archivoNombre || 'Imagen'}</span>
                        <div className="chat-attachment-actions row">
                            <button
                                type="button"
                                className="chat-attachment-open"
                                onClick={() => handleAbrirArchivo(msg)}
                                disabled={abriendoId === msg.id}
                            >
                                {abriendoId === msg.id ? 'Abriendo...' : 'Abrir'}
                            </button>
                            <button
                                type="button"
                                className="chat-attachment-download"
                                onClick={() => handleDescargarArchivo(msg)}
                                disabled={descargandoId === msg.id}
                            >
                                {descargandoId === msg.id ? 'Descargando...' : 'Descargar'}
                            </button>
                        </div>
                    </div>
                </div>
            );
        }

        return (
            <div className="chat-attachment-card">
                <div className="chat-attachment-file-icon">📎</div>

                <div className="chat-attachment-meta">
                    <span className="chat-attachment-name">{msg.archivoNombre || 'Adjunto'}</span>
                    <span className="chat-attachment-extra">
                        {msg.archivoMimeType || 'Archivo'}
                        {msg.archivoTamano ? ` · ${formatearTamanoArchivo(msg.archivoTamano)}` : ''}
                    </span>
                </div>

                <div className="chat-attachment-actions">
                    <button
                        type="button"
                        className="chat-attachment-open"
                        onClick={() => handleAbrirArchivo(msg)}
                        disabled={abriendoId === msg.id}
                    >
                        {abriendoId === msg.id ? 'Abriendo...' : 'Abrir'}
                    </button>
                    <button
                        type="button"
                        className="chat-attachment-download"
                        onClick={() => handleDescargarArchivo(msg)}
                        disabled={descargandoId === msg.id}
                    >
                        {descargandoId === msg.id ? 'Descargando...' : 'Descargar'}
                    </button>
                </div>
            </div>
        );
    };

    const getContenidoVisible = (msg) => {
        const content = String(msg?.contenido || '');
        const hasAttachment = Boolean(msg?.archivoNombre || msg?.archivoUrl);

        if (!hasAttachment) {
            return content;
        }

        const cleaned = content.replace(/^\[Adjunto\]\s*/i, '').trim();
        if (cleaned !== content.trim()) {
            if (!cleaned) {
                return '';
            }
            if (msg?.archivoNombre && cleaned === msg.archivoNombre) {
                return '';
            }
            return cleaned;
        }

        return content;
    };

    const renderAdjuntoPendiente = () => {
        if (!archivoSeleccionado) {
            return null;
        }

        return (
            <div className="chat-pending-attachment">
                {archivoSeleccionadoPreviewUrl ? (
                    <img
                        src={archivoSeleccionadoPreviewUrl}
                        alt={archivoSeleccionado.name || 'Adjunto seleccionado'}
                        className="chat-pending-attachment-image"
                    />
                ) : (
                    <div className="chat-pending-attachment-icon">📎</div>
                )}

                <div className="chat-pending-attachment-meta">
                    <span className="chat-pending-attachment-name">{archivoSeleccionado.name}</span>
                    <span className="chat-pending-attachment-extra">
                        {archivoSeleccionado.type || 'Archivo'} · {formatearTamanoArchivo(archivoSeleccionado.size)}
                    </span>
                </div>

                <button type="button" className="chat-pending-attachment-remove" onClick={clearArchivoSeleccionado}>
                    Quitar
                </button>
            </div>
        );
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
                                <>
                                    {getContenidoVisible(msg) ? (
                                        <div className="message-content">{getContenidoVisible(msg)}</div>
                                    ) : null}
                                    {renderAdjunto(msg)}
                                    <LinkPreviewCard
                                        preview={
                                            previewsByMessageId[String(msg.id)]?.url
                                                === extractFirstUrl(msg.contenido)
                                                ? previewsByMessageId[String(msg.id)]?.preview
                                                : null
                                        }
                                    />
                                </>
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

            {renderAdjuntoPendiente()}

            <form onSubmit={handleEnviarMensaje} className="message-input-form">
                <input
                    ref={fileInputRef}
                    type="file"
                    id="private-chat-file"
                    accept="image/jpeg,image/png,image/webp,application/pdf"
                    onChange={(e) => setArchivoSeleccionado(e.target.files?.[0] || null)}
                    style={{ display: 'none' }}
                    disabled={enviando}
                />
                <label htmlFor="private-chat-file" className="chat-file-label">
                    {archivoSeleccionado ? 'Archivo listo' : 'Adjuntar'}
                </label>
                <input
                    type="text"
                    placeholder="Escribe un mensaje..."
                    value={contenido}
                    onChange={(e) => setContenido(e.target.value)}
                    disabled={enviando}
                    maxLength={1000}
                />
                <button type="submit" disabled={enviando || (!contenido.trim() && !archivoSeleccionado)}>
                    {enviando ? '...' : '→'}
                </button>
            </form>
        </div>
    );
};

export default PrivateChat;

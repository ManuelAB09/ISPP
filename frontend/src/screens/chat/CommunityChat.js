import React, { useEffect, useState, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useSocketContext } from '../../contexts/SocketContext';
import {
    enviarMensajeComunidad,
    enviarArchivoComunidad,
    obtenerHistorialComunidad,
    editarMensajeComunidad,
    eliminarMensajeComunidad,
    obtenerPreviewEnlace,
    obtenerArchivoChatBlob,
} from '../../api/mensajeService';
import { getApiBaseUrl } from '../../api/baseUrl';
import { extractFirstUrl } from '../../utils/linkPreview';
import LinkPreviewCard from './LinkPreviewCard';
import { LuExpand, LuMessageCircle, LuX } from 'react-icons/lu';
import './CommunityChat.css';
import { communitiesApi } from '../../api/communities.api';

const DEFAULT_PROFILE_AVATAR =
    "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 120 120'%3E%3Ccircle cx='60' cy='60' r='60' fill='%23E6EAF3'/%3E%3Ccircle cx='60' cy='46' r='22' fill='%2395A1BB'/%3E%3Cpath d='M20 106c6-20 22-32 40-32s34 12 40 32' fill='%2395A1BB'/%3E%3C/svg%3E";

const toAbsoluteImageUrl = (imageUrl, fallback = DEFAULT_PROFILE_AVATAR) => {
    const raw = String(imageUrl || '').trim();
    if (!raw) {
        return fallback;
    }

    const normalizedValue = raw.toLowerCase();
    if (normalizedValue === 'empty' || normalizedValue === 'null' || normalizedValue === 'undefined') {
        return fallback;
    }

    if (/^https?:\/\//i.test(raw) || raw.startsWith('data:') || raw.startsWith('blob:')) {
        return raw;
    }

    const base = getApiBaseUrl();
    return raw.startsWith('/') ? `${base}${raw}` : `${base}/${raw}`;
};

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
    isOpen,
    mode = 'floating',
    onOpenPrivateChat,
    onOpenChange,
    extraActions,
    headerActions,
}) => {
    const isEmbedded = mode === 'embedded';
    const navigate = useNavigate();
    const location = useLocation();
    const { socket, isConnected } = useSocketContext();
    const [mensajes, setMensajes] = useState([]);
    const [contenido, setContenido] = useState('');
    const [cargandoHistorial, setCargandoHistorial] = useState(false);
    const [enviando, setEnviando] = useState(false);
    const [error, setError] = useState(null);
    const [chatAbierto, setChatAbierto] = useState(
        isEmbedded ? true : (typeof isOpen === 'boolean' ? isOpen : initiallyOpen)
    );
    const [editandoId, setEditandoId] = useState(null);
    const [contenidoEditado, setContenidoEditado] = useState('');
    const [procesandoId, setProcesandoId] = useState(null);
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

    const handleCommunityAvatarError = (e) => {
        e.currentTarget.onerror = null;
        e.currentTarget.src = '/MeerKatters_logo.png';
    };

    const handleProfileAvatarError = (e) => {
        e.currentTarget.onerror = null;
        e.currentTarget.src = DEFAULT_PROFILE_AVATAR;
    };

    const isOwnMessage = (msg) => Number(msg?.usuarioId) === Number(usuarioActual?.id);

    const resolveAvatarBackgroundColor = (msg) => {
        if (msg?.usuarioFotoBackgroundColor) {
            return msg.usuarioFotoBackgroundColor;
        }
        if (isOwnMessage(msg) && usuarioActual?.fotoBackgroundColor) {
            return usuarioActual.fotoBackgroundColor;
        }
        return '#ffffff';
    };

    /**
     * Carga el historial de mensajes al montar el componente.
     */
    useEffect(() => {
        const cargarHistorial = async () => {
            try {
                setCargandoHistorial(true);
                const response = await obtenerHistorialComunidad(comunidadId);
                const data = Array.isArray(response?.data) ? response.data : [];
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
    }, [socket, isConnected, comunidadId, comunidadNombre, navigate, location.pathname]);

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
        if (typeof isOpen === 'boolean') {
            setChatAbierto(isOpen);
            return;
        }
        setChatAbierto(initiallyOpen);
    }, [initiallyOpen, isEmbedded, isOpen]);

    const updateOpenState = (nextOpen) => {
        if (isEmbedded) {
            return;
        }
        setChatAbierto(nextOpen);
        if (onOpenChange) {
            onOpenChange(nextOpen);
        }
    };

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
     * Envía un mensaje a través del socket.
     */
    const handleEnviarMensaje = async (e) => {
        e.preventDefault();

        if ((!contenido.trim() && !archivoSeleccionado) || enviando) return;

        try {
            setEnviando(true);
            setError(null);

            let data;
            if (archivoSeleccionado) {
                const response = await enviarArchivoComunidad(
                    comunidadId,
                    archivoSeleccionado,
                    contenido.trim()
                );
                data = response.data;
            } else {
                const messageContent = contenido.trim();
                const response = await enviarMensajeComunidad(comunidadId, messageContent);
                data = response.data;
            }

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
            userPhotoBg: msg?.usuarioFotoBackgroundColor || '#ffffff',
        };

        if (onOpenPrivateChat) {
            onOpenPrivateChat(payload);
            return;
        }

        navigate(`/chats?communityId=${comunidadId}&userId=${targetId}`);
    };

    const [miembros, setMiembros] = useState([]);
    const [showMentionMenu, setShowMentionMenu] = useState(false);
    const [mentionQuery, setMentionQuery] = useState('');
    const [mentionSelectedIdx, setMentionSelectedIdx] = useState(0);

    useEffect(() => {
        communitiesApi.getMembers(comunidadId).then(res => {
            setMiembros(Array.isArray(res?.content) ? res.content : []);
        }).catch((err) => {
            setMiembros([]);
        });
    }, [comunidadId]);

    const filteredMiembros = miembros.filter(m =>
        m.usuario && m.usuario.nombre && m.usuario.nombre.toLowerCase().includes(mentionQuery.toLowerCase())
    );

    const handleMentionSelect = (nombre) => {
        if (!nombre) return;
        const input = document.getElementById('community-chat-input');
        const cursor = input ? input.selectionStart : contenido.length;
        const before = contenido.slice(0, cursor);
        const after = contenido.slice(cursor);
        const newBefore = before.replace(/@([\w]*)$/, `@${nombre} `);
        setContenido(newBefore + after);
        setShowMentionMenu(false);
        setMentionQuery('');
        setMentionSelectedIdx(0);
        setTimeout(() => input && input.focus(), 0);
    };

    const handleMentionKeyDown = (e) => {
        if (!showMentionMenu) return;
        if (e.key === 'ArrowDown') {
            setMentionSelectedIdx(idx => Math.min(idx + 1, filteredMiembros.length - 1));
            e.preventDefault();
        } else if (e.key === 'ArrowUp') {
            setMentionSelectedIdx(idx => Math.max(idx - 1, 0));
            e.preventDefault();
        } else if (e.key === 'Enter') {
            if (filteredMiembros[mentionSelectedIdx]) {
                handleMentionSelect(filteredMiembros[mentionSelectedIdx].nombre);
                e.preventDefault();
            }
        } else if (e.key === 'Escape') {
            setShowMentionMenu(false);
        }
    };

    const handleInputChange = (e) => {
        const value = e.target.value;
        setContenido(value);

        const cursorPosition = e.target.selectionStart;
        const textBeforeCursor = value.slice(0, cursorPosition);
        const mentionMatch = textBeforeCursor.match(/@([\w]*)$/); 

        if (mentionMatch) {
            setShowMentionMenu(true);
            setMentionQuery(mentionMatch[1]); 
            setMentionSelectedIdx(0);
        } else {
            setShowMentionMenu(false);
            setMentionQuery('');
        }
    };

    return (
        <div className={isEmbedded ? 'community-chat-embedded' : 'community-chat-floating'}>
            {!isEmbedded && (
                <div className="chat-floating-actions-row">
                    {extraActions ? <div className="chat-floating-extra-actions">{extraActions}</div> : null}
                    <button
                        type="button"
                        className="chat-toggle-button"
                        onClick={() => updateOpenState(!chatAbierto)}
                        aria-expanded={chatAbierto}
                        aria-label="Abrir chat de comunidad"
                    >
                        <LuMessageCircle size={20} />
                        <span>Chat</span>
                    </button>
                </div>
            )}

            {!chatAbierto && !isEmbedded ? null : (
                <aside className={`community-chat-panel ${isEmbedded ? 'embedded' : ''}`}>
                    <div className="chat-header">
                        <div className="chat-header-left">
                            <img
                                className="chat-header-avatar"
                                src={toAbsoluteImageUrl(comunidadImagen, '/MeerKatters_logo.png')}
                                alt={comunidadNombre || `Comunidad ${comunidadId}`}
                                onError={handleCommunityAvatarError}
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
                            {headerActions ? <div className="chat-header-extra-actions">{headerActions}</div> : null}
                            {!isEmbedded && (
                                <button
                                    type="button"
                                    className="chat-open-large-button"
                                    onClick={handleOpenLargeChat}
                                    aria-label="Abrir chat en vista grande"
                                    title="Abrir chat en vista grande"
                                >
                                    <LuExpand size={16} />
                                </button>
                            )}
                            {!isEmbedded && (
                                <button
                                    type="button"
                                    className="chat-close-button"
                                    onClick={() => updateOpenState(false)}
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
                                                src={toAbsoluteImageUrl(msg.usuarioFoto, DEFAULT_PROFILE_AVATAR)}
                                                alt={msg.usuarioNombre || 'Usuario'}
                                                onError={handleProfileAvatarError}
                                                style={{ backgroundColor: resolveAvatarBackgroundColor(msg) }}
                                            />
                                            <span className="usuario-nombre">{msg.usuarioNombre}</span>
                                        </button>
                                        {msg.editado && <span className="editado-badge">(editado)</span>}
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

                    {renderAdjuntoPendiente()}

                    <form onSubmit={handleEnviarMensaje} className="message-input-form">
                        <input
                            ref={fileInputRef}
                            type="file"
                            id={`community-chat-file-${comunidadId}`}
                            accept="image/jpeg,image/png,image/webp,application/pdf"
                            onChange={(e) => setArchivoSeleccionado(e.target.files?.[0] || null)}
                            style={{ display: 'none' }}
                            disabled={enviando}
                        />
                        <label htmlFor={`community-chat-file-${comunidadId}`} className="chat-file-label">
                            {archivoSeleccionado ? 'Archivo listo' : 'Adjuntar'}
                        </label>
                        <div style={{ position: 'relative', width: '100%' }}>
                            <input
                                id="community-chat-input"
                                type="text"
                                placeholder="Escribe un mensaje..."
                                value={contenido}
                                onChange={handleInputChange}
                                onKeyDown={handleMentionKeyDown}
                                disabled={enviando}
                                maxLength={1000}
                                autoComplete="off"
                                style={{ zIndex: 11 }}
                            />
                            {showMentionMenu && filteredMiembros.length > 0 && (
                                <div style={{ position: 'absolute', left: 0, bottom: 40, zIndex: 100, width: '100%' }}>
                                    <ul className="mention-menu" style={{ background: '#fff', border: '1px solid #ccc', width: '100%', maxHeight: 200, overflowY: 'auto', margin: 0, padding: 0 }}>
                                        {filteredMiembros.map((m, idx) => (
                                            <li
                                                key={m.id}
                                                style={{ padding: '8px', cursor: 'pointer', background: idx === mentionSelectedIdx ? '#eee' : '#fff', zIndex: 101 }}
                                                onMouseDown={() => handleMentionSelect(m.usuario.nombre)}
                                            >
                                                @{m.usuario.nombre}
                                            </li>
                                        ))}
                                    </ul>
                                </div>
                            )}
                        </div>
                        <button type="submit" disabled={enviando || (!contenido.trim() && !archivoSeleccionado)}>
                            {enviando ? '...' : '→'}
                        </button>
                    </form>
                </aside>
            )}
        </div>
    );
};

export default CommunityChat;

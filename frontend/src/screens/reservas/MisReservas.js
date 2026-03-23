import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Header from "../../components/Header/Header";
import { obtenerSolicitudesAlumno, cancelarSolicitudAlumno, calificarSolicitud, aprobarReprogramacion, rechazarReprogramacion } from "../../api/solicitudContratacion";
import "./MisReservas.css";

const ESTADO_LABEL = {
    PENDIENTE: "Pendiente",
    ACEPTADA: "Aceptada",
    RECHAZADA: "Rechazada",
    PAGADA: "Pagada",
    CANCELADA_ALUMNO: "Cancelada",
    CANCELADA_TUTOR: "Cancelada por tutor",
    COMPLETADA: "Completada",
    NO_ASISTIDA: "No asistida",
    REPROGRAMACION_PENDIENTE: "Reprogramación pendiente",
};

const ESTADO_COLOR = {
    PENDIENTE: "mr-badge--pending",
    ACEPTADA: "mr-badge--confirmed",
    RECHAZADA: "mr-badge--cancelled",
    PAGADA: "mr-badge--confirmed",
    CANCELADA_ALUMNO: "mr-badge--cancelled",
    CANCELADA_TUTOR: "mr-badge--cancelled",
    COMPLETADA: "mr-badge--done",
    NO_ASISTIDA: "mr-badge--cancelled",
    REPROGRAMACION_PENDIENTE: "mr-badge--pending",
};

const MODALIDAD_LABEL = {
    ONLINE: "💻 Online",
    PRESENCIAL: "🏫 Presencial",
    HIBRIDO: "🔄 Híbrido",
};

const CancelDialog = ({ onConfirm, onClose, loading }) => {
    const [motivo, setMotivo] = useState("");
    return (
        <div className="mr-dialog-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
            <div className="mr-dialog">
                <h3 className="mr-dialog__title">Cancelar clase</h3>
                <p className="mr-dialog__text">
                    Indica el motivo de cancelación (opcional). El tutor será notificado.
                    Las clases pagadas solo se pueden cancelar con al menos 24h de antelación.
                </p>
                <textarea
                    className="mr-dialog__input"
                    rows={3}
                    placeholder="Motivo de cancelación…"
                    value={motivo}
                    onChange={(e) => setMotivo(e.target.value)}
                />
                <div className="mr-dialog__footer">
                    <button className="mr-btn mr-btn--secondary" onClick={onClose} disabled={loading}>
                        Volver
                    </button>
                    <button className="mr-btn mr-btn--danger" onClick={() => onConfirm(motivo)} disabled={loading}>
                        {loading ? "Cancelando…" : "Confirmar cancelación"}
                    </button>
                </div>
            </div>
        </div>
    );
};

const RatingDialog = ({ onConfirm, onClose, loading }) => {
    const [calificacion, setCalificacion] = useState(0);
    const [comentario, setComentario] = useState("");
    return (
        <div className="mr-dialog-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
            <div className="mr-dialog">
                <h3 className="mr-dialog__title">Calificar clase</h3>
                <p className="mr-dialog__text">¿Cómo fue tu experiencia?</p>
                <div style={{ display: "flex", gap: "8px", justifyContent: "center", margin: "12px 0" }}>
                    {[1, 2, 3, 4, 5].map((n) => (
                        <button
                            key={n}
                            onClick={() => setCalificacion(n)}
                            style={{
                                fontSize: "1.5rem",
                                background: "none",
                                border: "none",
                                cursor: "pointer",
                                opacity: n <= calificacion ? 1 : 0.3,
                            }}
                        >
                            ⭐
                        </button>
                    ))}
                </div>
                <textarea
                    className="mr-dialog__input"
                    rows={3}
                    placeholder="Comentario (opcional)…"
                    value={comentario}
                    onChange={(e) => setComentario(e.target.value)}
                />
                <div className="mr-dialog__footer">
                    <button className="mr-btn mr-btn--secondary" onClick={onClose} disabled={loading}>
                        Volver
                    </button>
                    <button
                        className="mr-btn mr-btn--primary"
                        onClick={() => onConfirm(calificacion, comentario)}
                        disabled={loading || calificacion === 0}
                    >
                        {loading ? "Enviando…" : "Enviar calificación"}
                    </button>
                </div>
            </div>
        </div>
    );
};

const MisReservas = () => {
    const navigate = useNavigate();
    const [solicitudes, setSolicitudes] = useState([]);
    const [cargando, setCargando] = useState(true);
    const [error, setError] = useState("");
    const [successMsg, setSuccessMsg] = useState("");
    const [cancelTarget, setCancelTarget] = useState(null);
    const [cancelLoading, setCancelLoading] = useState(false);
    const [ratingTarget, setRatingTarget] = useState(null);
    const [ratingLoading, setRatingLoading] = useState(false);
    const [reprogLoading, setReprogLoading] = useState(null);

    const cargar = async () => {
        setCargando(true);
        try {
            const { data } = await obtenerSolicitudesAlumno();
            const lista = Array.isArray(data) ? data : [];
            lista.sort((a, b) => {
                if (a.dia && b.dia) return a.dia.localeCompare(b.dia);
                return 0;
            });
            setSolicitudes(lista);
        } catch {
            setSolicitudes([]);
        } finally {
            setCargando(false);
        }
    };

    useEffect(() => { cargar(); }, []);

    const handleCancelar = async (motivo) => {
        setCancelLoading(true);
        setError("");
        setSuccessMsg("");
        try {
            await cancelarSolicitudAlumno(cancelTarget, motivo);
            setCancelTarget(null);
            setSuccessMsg("Clase cancelada. El profesor ha sido notificado.");
            cargar();
        } catch (err) {
            setError(err?.response?.data?.error || err?.message || "No se pudo cancelar.");
        } finally {
            setCancelLoading(false);
        }
    };

    const handleCalificar = async (calificacion, comentario) => {
        setRatingLoading(true);
        setError("");
        setSuccessMsg("");
        try {
            await calificarSolicitud(ratingTarget, calificacion, comentario);
            setRatingTarget(null);
            setSuccessMsg("¡Calificación enviada!");
            cargar();
        } catch (err) {
            setError(err?.response?.data?.error || err?.message || "No se pudo calificar.");
        } finally {
            setRatingLoading(false);
        }
    };

    const handleAprobarReprog = async (solicitudId) => {
        setReprogLoading(solicitudId);
        setError("");
        try {
            await aprobarReprogramacion(solicitudId);
            setSuccessMsg("Reprogramación aprobada.");
            cargar();
        } catch (err) {
            setError(err?.response?.data?.error || err?.message || "Error al aprobar.");
        } finally {
            setReprogLoading(null);
        }
    };

    const handleRechazarReprog = async (solicitudId) => {
        setReprogLoading(solicitudId);
        setError("");
        try {
            await rechazarReprogramacion(solicitudId);
            setSuccessMsg("Reprogramación rechazada. Se mantiene el horario original.");
            cargar();
        } catch (err) {
            setError(err?.response?.data?.error || err?.message || "Error al rechazar.");
        } finally {
            setReprogLoading(null);
        }
    };

    const activas = solicitudes.filter((s) =>
        ["PENDIENTE", "ACEPTADA", "PAGADA", "REPROGRAMACION_PENDIENTE"].includes(s.estado)
    );
    const pasadas = solicitudes.filter((s) =>
        !["PENDIENTE", "ACEPTADA", "PAGADA", "REPROGRAMACION_PENDIENTE"].includes(s.estado)
    );

    return (
        <>
            <Header page="mis-reservas" />
            <div className="mr-page">
                <div className="mr-container">
                    <div className="mr-hero">
                        <h1 className="mr-hero__title">Mis clases</h1>
                        <p className="mr-hero__sub">Consulta y gestiona tus clases contratadas con tutores.</p>
                        <button className="mr-btn mr-btn--primary" onClick={() => navigate("/profesores")}>
                            + Contratar nueva clase
                        </button>
                    </div>

                    {error && <p className="mr-error">⚠️ {error}</p>}
                    {successMsg && <p className="mr-success">✓ {successMsg}</p>}

                    {cargando ? (
                        <p className="mr-loading">Cargando tus clases…</p>
                    ) : solicitudes.length === 0 ? (
                        <div className="mr-empty">
                            <span className="mr-empty__icon">📅</span>
                            <p className="mr-empty__title">Aún no tienes clases</p>
                            <p className="mr-empty__sub">Busca un tutor y contrata tu primera clase.</p>
                            <button className="mr-btn mr-btn--primary" onClick={() => navigate("/profesores")}>
                                Explorar tutores
                            </button>
                        </div>
                    ) : (
                        <>
                            {activas.length > 0 && (
                                <section className="mr-section">
                                    <h2 className="mr-section__title">Próximas clases</h2>
                                    <div className="mr-list">
                                        {activas.map((s) => (
                                            <SolicitudCard
                                                key={s.id}
                                                solicitud={s}
                                                onCancelar={s.puedeSerCanceladaPorAlumno ? () => setCancelTarget(s.id) : null}
                                                onCalificar={null}
                                                onAprobarReprog={() => handleAprobarReprog(s.id)}
                                                onRechazarReprog={() => handleRechazarReprog(s.id)}
                                                reprogLoading={reprogLoading === s.id}
                                            />
                                        ))}
                                    </div>
                                </section>
                            )}

                            {pasadas.length > 0 && (
                                <section className="mr-section">
                                    <h2 className="mr-section__title">Historial</h2>
                                    <div className="mr-list">
                                        {pasadas.map((s) => (
                                            <SolicitudCard
                                                key={s.id}
                                                solicitud={s}
                                                onCalificar={
                                                    (s.estado === "COMPLETADA" || s.estado === "PAGADA") && !s.calificacion
                                                        ? () => setRatingTarget(s.id)
                                                        : null
                                                }
                                            />
                                        ))}
                                    </div>
                                </section>
                            )}
                        </>
                    )}
                </div>
            </div>

            {cancelTarget && (
                <CancelDialog
                    onConfirm={handleCancelar}
                    onClose={() => setCancelTarget(null)}
                    loading={cancelLoading}
                />
            )}

            {ratingTarget && (
                <RatingDialog
                    onConfirm={handleCalificar}
                    onClose={() => setRatingTarget(null)}
                    loading={ratingLoading}
                />
            )}
        </>
    );
};

const SolicitudCard = ({ solicitud: s, onCancelar, onCalificar, onAprobarReprog, onRechazarReprog, reprogLoading }) => (
    <div className="mr-card">
        <div className="mr-card__header">
            <div className="mr-card__tutor">
                <span className="mr-card__tutor-name">{s.tutorNombre || `Tutor #${s.tutorId}`}</span>
                <span className={`mr-badge ${ESTADO_COLOR[s.estado] || ""}`}>
                    {ESTADO_LABEL[s.estado] || s.estado}
                </span>
            </div>
            <span className="mr-card__fecha">{s.dia} · {s.horaInicio} – {s.horaFin}</span>
        </div>

        <div className="mr-card__body">
            <p className="mr-card__meta">
                {MODALIDAD_LABEL[s.modalidad] || s.modalidad} · <strong>{s.importeTotal}€</strong> ({s.tarifaHora}€/h)
            </p>
            {s.mensaje && <p className="mr-card__desc">{s.mensaje}</p>}
            {s.motivoRechazo && (
                <p className="mr-card__motivo">
                    Motivo: <em>{s.motivoRechazo}</em>
                </p>
            )}
            {s.calificacion && (
                <p className="mr-card__rating">
                    {"⭐".repeat(s.calificacion)} {s.comentarioAlumno && <em>— {s.comentarioAlumno}</em>}
                </p>
            )}
        </div>

        {/* Reprogramación pendiente */}
        {s.estado === "REPROGRAMACION_PENDIENTE" && s.reprogramacionDia && (
            <div className="mr-card__reprog">
                <p style={{ marginBottom: "8px" }}>
                    El tutor propone cambiar a: <strong>{s.reprogramacionDia}</strong> de{" "}
                    <strong>{s.reprogramacionHoraInicio}</strong> a <strong>{s.reprogramacionHoraFin}</strong>
                </p>
                <div className="mr-card__actions">
                    <button className="mr-btn mr-btn--danger-outline" onClick={onRechazarReprog} disabled={reprogLoading}>
                        Rechazar
                    </button>
                    <button className="mr-btn mr-btn--primary" onClick={onAprobarReprog} disabled={reprogLoading}>
                        {reprogLoading ? "Procesando…" : "Aprobar cambio"}
                    </button>
                </div>
            </div>
        )}

        <div className="mr-card__actions">
            {onCancelar && (
                <button className="mr-btn mr-btn--danger-outline" onClick={onCancelar}>
                    Cancelar clase
                </button>
            )}
            {onCalificar && (
                <button className="mr-btn mr-btn--primary" onClick={onCalificar}>
                    ⭐ Calificar
                </button>
            )}
        </div>
    </div>
);

export default MisReservas;

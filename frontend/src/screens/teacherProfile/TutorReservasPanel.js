import { useEffect, useState } from "react";
import { cancelarReserva, confirmarReserva, getMisReservasTutor } from "../../api/reservas.api";
import "./TutorReservasPanel.css";

const ESTADO_LABEL = {
    PENDIENTE: "Pendiente",
    CONFIRMADA: "Confirmada",
    COMPLETADA: "Completada",
    CANCELADA_ALUMNO: "Cancelada (alumno)",
    CANCELADA_TUTOR: "Cancelada (tutor)",
    NO_ASISTIDA: "No asistida",
};

const ESTADO_COLOR = {
    PENDIENTE: "trp-badge--pending",
    CONFIRMADA: "trp-badge--confirmed",
    COMPLETADA: "trp-badge--done",
    CANCELADA_ALUMNO: "trp-badge--cancelled",
    CANCELADA_TUTOR: "trp-badge--cancelled",
    NO_ASISTIDA: "trp-badge--cancelled",
};

const TABS = ["Pendientes", "Confirmadas", "Historial"];

const formatFecha = (fechaHora) => {
    if (!fechaHora) return "—";
    const d = new Date(fechaHora);
    return d.toLocaleString("es-ES", {
        weekday: "short",
        day: "2-digit",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
    });
};

const CancelDialog = ({ onConfirm, onClose, loading }) => {
    const [motivo, setMotivo] = useState("");
    return (
        <div className="trp-dialog-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
            <div className="trp-dialog">
                <h3 className="trp-dialog__title">Cancelar reserva</h3>
                <p className="trp-dialog__text">Indica el motivo de cancelación (opcional). El alumno será notificado.</p>
                <textarea
                    className="trp-dialog__input"
                    rows={3}
                    placeholder="Motivo de cancelación…"
                    value={motivo}
                    onChange={(e) => setMotivo(e.target.value)}
                />
                <div className="trp-dialog__footer">
                    <button className="trp-btn trp-btn--secondary" onClick={onClose} disabled={loading}>
                        Volver
                    </button>
                    <button className="trp-btn trp-btn--danger" onClick={() => onConfirm(motivo)} disabled={loading}>
                        {loading ? "Cancelando…" : "Confirmar cancelación"}
                    </button>
                </div>
            </div>
        </div>
    );
};

const TutorReservasPanel = ({ tutorId }) => {
    const [reservas, setReservas] = useState([]);
    const [cargando, setCargando] = useState(true);
    const [esTutor, setEsTutor] = useState(true);
    const [tab, setTab] = useState(0);
    const [actionLoading, setActionLoading] = useState(null);
    const [cancelTarget, setCancelTarget] = useState(null);
    const [error, setError] = useState("");
    const [successMsg, setSuccessMsg] = useState("");

    const cargar = () => {
        setCargando(true);
        getMisReservasTutor({ size: 100 })
            .then((data) => {
                const lista = Array.isArray(data) ? data : data?.content ?? [];
                setReservas(lista);
                setEsTutor(true);
            })
            .catch((err) => {
                const status = err?.status || err?.response?.status;
                if (status === 403) setEsTutor(false);
                setReservas([]);
            })
            .finally(() => setCargando(false));
    };

    useEffect(() => {
        cargar();
    }, [tutorId]);

    const handleConfirmar = async (reservaId) => {
        setError("");
        setSuccessMsg("");
        setActionLoading(reservaId + "_confirm");
        try {
            await confirmarReserva(reservaId);
            setSuccessMsg("Reserva confirmada. El alumno ha recibido un email de confirmación.");
            cargar();
        } catch (err) {
            setError(err?.message || "Error al confirmar.");
        } finally {
            setActionLoading(null);
        }
    };

    const handleCancelar = async (motivo) => {
        setError("");
        setSuccessMsg("");
        setActionLoading(cancelTarget + "_cancel");
        try {
            await cancelarReserva(cancelTarget, motivo);
            setCancelTarget(null);
            setSuccessMsg("Reserva cancelada. El alumno ha sido notificado por email.");
            cargar();
        } catch (err) {
            setError(err?.message || "Error al cancelar.");
        } finally {
            setActionLoading(null);
        }
    };

    const pendientes = reservas.filter((r) => r.estado === "PENDIENTE");
    const confirmadas = reservas.filter((r) => r.estado === "CONFIRMADA");
    const historial = reservas.filter((r) =>
        ["COMPLETADA", "CANCELADA_ALUMNO", "CANCELADA_TUTOR", "NO_ASISTIDA"].includes(r.estado)
    );

    const grupos = [pendientes, confirmadas, historial];
    const lista = grupos[tab];

    if (!esTutor) return null;

    return (
        <section className="trp-panel">
            <div className="tp-section-title-row">
                <h2 className="tp-section-title">Mis reservas de clase</h2>
                <div className="tp-section-title-row__line" />
            </div>

            <div className="trp-tabs">
                {TABS.map((t, i) => (
                    <button
                        key={t}
                        className={`trp-tab ${tab === i ? "trp-tab--active" : ""}`}
                        onClick={() => setTab(i)}
                    >
                        {t}
                        {grupos[i].length > 0 && (
                            <span className="trp-tab__count">{grupos[i].length}</span>
                        )}
                    </button>
                ))}
            </div>

            {error && <p className="trp-error">⚠️ {error}</p>}
            {successMsg && <p className="trp-success">✓ {successMsg}</p>}

            {cargando ? (
                <p className="trp-loading">Cargando reservas…</p>
            ) : lista.length === 0 ? (
                <p className="trp-empty">No hay reservas en esta sección.</p>
            ) : (
                <div className="trp-list">
                    {lista.map((r) => (
                        <div key={r.id} className="trp-card">
                            <div className="trp-card__top">
                                <div>
                                    <span className="trp-card__alumno">{r.alumnoNombre || `Alumno #${r.alumnoId}`}</span>
                                    <span className={`trp-badge ${ESTADO_COLOR[r.estado] || ""}`}>
                                        {ESTADO_LABEL[r.estado] || r.estado}
                                    </span>
                                </div>
                                <span className="trp-card__fecha">{formatFecha(r.fechaHora)}</span>
                            </div>

                            <div className="trp-card__details">
                                <span className="trp-card__tema">{r.tema}</span>
                                <span className="trp-card__meta">
                                    {r.modalidad} · {r.duracionMinutos} min
                                    {r.tarifa != null && <> · {r.tarifa}€</>}
                                </span>
                                {r.descripcion && (
                                    <p className="trp-card__desc">{r.descripcion}</p>
                                )}
                            </div>

                            {r.estado === "PENDIENTE" && (
                                <div className="trp-card__actions">
                                    <button
                                        className="trp-btn trp-btn--confirm"
                                        disabled={!!actionLoading}
                                        onClick={() => handleConfirmar(r.id)}
                                    >
                                        {actionLoading === r.id + "_confirm" ? "Confirmando…" : "Confirmar"}
                                    </button>
                                    <button
                                        className="trp-btn trp-btn--danger"
                                        disabled={!!actionLoading}
                                        onClick={() => setCancelTarget(r.id)}
                                    >
                                        Rechazar
                                    </button>
                                </div>
                            )}

                            {r.estado === "CONFIRMADA" && (
                                <div className="trp-card__actions">
                                    <button
                                        className="trp-btn trp-btn--secondary"
                                        disabled={!!actionLoading}
                                        onClick={() => setCancelTarget(r.id)}
                                    >
                                        Cancelar
                                    </button>
                                </div>
                            )}
                        </div>
                    ))}
                </div>
            )}

            {cancelTarget && (
                <CancelDialog
                    onConfirm={handleCancelar}
                    onClose={() => setCancelTarget(null)}
                    loading={actionLoading === cancelTarget + "_cancel"}
                />
            )}
        </section>
    );
};

export default TutorReservasPanel;

import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Header from "../../components/Header/Header";
import { cancelarReserva, getMisReservasAlumno } from "../../api/reservas.api";
import "./MisReservas.css";

const ESTADO_LABEL = {
    PENDIENTE: "Pendiente",
    CONFIRMADA: "Confirmada",
    COMPLETADA: "Completada",
    CANCELADA_ALUMNO: "Cancelada",
    CANCELADA_TUTOR: "Cancelada por tutor",
    NO_ASISTIDA: "No asistida",
};

const ESTADO_COLOR = {
    PENDIENTE: "mr-badge--pending",
    CONFIRMADA: "mr-badge--confirmed",
    COMPLETADA: "mr-badge--done",
    CANCELADA_ALUMNO: "mr-badge--cancelled",
    CANCELADA_TUTOR: "mr-badge--cancelled",
    NO_ASISTIDA: "mr-badge--cancelled",
};

const formatFecha = (fechaHora) => {
    if (!fechaHora) return "—";
    const d = new Date(fechaHora);
    return d.toLocaleString("es-ES", {
        weekday: "long",
        day: "2-digit",
        month: "long",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
    });
};

const CancelDialog = ({ onConfirm, onClose, loading }) => {
    const [motivo, setMotivo] = useState("");
    return (
        <div className="mr-dialog-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
            <div className="mr-dialog">
                <h3 className="mr-dialog__title">Cancelar reserva</h3>
                <p className="mr-dialog__text">
                    Indica el motivo de cancelación (opcional). El tutor será notificado.
                    Recuerda que las cancelaciones con menos de 24h de antelación pueden tener penalizaciones.
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

const MisReservas = () => {
    const navigate = useNavigate();
    const [reservas, setReservas] = useState([]);
    const [cargando, setCargando] = useState(true);
    const [error, setError] = useState("");
    const [cancelTarget, setCancelTarget] = useState(null);
    const [cancelLoading, setCancelLoading] = useState(false);

    const cargar = () => {
        setCargando(true);
        getMisReservasAlumno({ size: 100 })
            .then((data) => {
                const lista = Array.isArray(data) ? data : data?.content ?? [];
                // Ordenar: primeras las más próximas
                lista.sort((a, b) => new Date(a.fechaHora) - new Date(b.fechaHora));
                setReservas(lista);
            })
            .catch(() => setReservas([]))
            .finally(() => setCargando(false));
    };

    useEffect(() => { cargar(); }, []);

    const handleCancelar = async (motivo) => {
        setCancelLoading(true);
        try {
            await cancelarReserva(cancelTarget, motivo);
            setCancelTarget(null);
            cargar();
        } catch (err) {
            setError(err?.response?.data?.message || err?.message || "No se pudo cancelar la reserva.");
        } finally {
            setCancelLoading(false);
        }
    };

    const proximas = reservas.filter((r) => ["PENDIENTE", "CONFIRMADA"].includes(r.estado));
    const pasadas = reservas.filter((r) => !["PENDIENTE", "CONFIRMADA"].includes(r.estado));

    return (
        <>
            <Header page="mis-reservas" />
            <div className="mr-page">
                <div className="mr-container">
                    <div className="mr-hero">
                        <h1 className="mr-hero__title">Mis reservas de clase</h1>
                        <p className="mr-hero__sub">Consulta y gestiona tus clases reservadas con tutores.</p>
                        <button className="mr-btn mr-btn--primary" onClick={() => navigate("/profesores")}>
                            + Reservar nueva clase
                        </button>
                    </div>

                    {error && <p className="mr-error">⚠️ {error}</p>}

                    {cargando ? (
                        <p className="mr-loading">Cargando tus reservas…</p>
                    ) : reservas.length === 0 ? (
                        <div className="mr-empty">
                            <span className="mr-empty__icon">📅</span>
                            <p className="mr-empty__title">Aún no tienes reservas</p>
                            <p className="mr-empty__sub">Busca un tutor y reserva tu primera clase.</p>
                            <button className="mr-btn mr-btn--primary" onClick={() => navigate("/profesores")}>
                                Explorar tutores
                            </button>
                        </div>
                    ) : (
                        <>
                            {proximas.length > 0 && (
                                <section className="mr-section">
                                    <h2 className="mr-section__title">Próximas clases</h2>
                                    <div className="mr-list">
                                        {proximas.map((r) => (
                                            <ReservaCard
                                                key={r.id}
                                                reserva={r}
                                                onCancelar={() => setCancelTarget(r.id)}
                                            />
                                        ))}
                                    </div>
                                </section>
                            )}

                            {pasadas.length > 0 && (
                                <section className="mr-section">
                                    <h2 className="mr-section__title">Historial</h2>
                                    <div className="mr-list">
                                        {pasadas.map((r) => (
                                            <ReservaCard key={r.id} reserva={r} />
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
        </>
    );
};

const ReservaCard = ({ reserva: r, onCancelar }) => (
    <div className="mr-card">
        <div className="mr-card__header">
            <div className="mr-card__tutor">
                <span className="mr-card__tutor-name">{r.tutorNombre || `Tutor #${r.tutorId}`}</span>
                <span className={`mr-badge ${ESTADO_COLOR[r.estado] || ""}`}>
                    {ESTADO_LABEL[r.estado] || r.estado}
                </span>
            </div>
            <span className="mr-card__fecha">{formatFecha(r.fechaHora)}</span>
        </div>

        <div className="mr-card__body">
            <p className="mr-card__tema">{r.tema}</p>
            <p className="mr-card__meta">
                {r.modalidad} · {r.duracionMinutos} min
                {r.tarifa != null && <> · <strong>{r.tarifa}€</strong></>}
            </p>
            {r.descripcion && <p className="mr-card__desc">{r.descripcion}</p>}
            {r.enlaceVirtual && (
                <a
                    className="mr-card__link"
                    href={r.enlaceVirtual}
                    target="_blank"
                    rel="noreferrer"
                >
                    🔗 Enlace a la clase virtual
                </a>
            )}
            {r.motivoCancelacion && (
                <p className="mr-card__motivo">
                    Motivo de cancelación: <em>{r.motivoCancelacion}</em>
                </p>
            )}
        </div>

        {r.estado === "PENDIENTE" && onCancelar && (
            <div className="mr-card__actions">
                <button className="mr-btn mr-btn--danger-outline" onClick={onCancelar}>
                    Cancelar reserva
                </button>
            </div>
        )}
    </div>
);

export default MisReservas;

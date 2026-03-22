import { useEffect, useState } from "react";
import {
    crearReserva,
    getDisponibilidadTutorFecha,
    getHorariosOcupados,
} from "../../api/reservas.api";
import "./BookClassModal.css";

const MODALIDADES = [
    { value: "VIRTUAL", label: "Virtual" },
    { value: "PRESENCIAL", label: "Presencial" },
    { value: "HIBRIDA", label: "Híbrida" },
];

const DURACIONES = [30, 60, 90, 120];

const hoy = () => {
    const d = new Date();
    return d.toISOString().split("T")[0];
};

const BookClassModal = ({ tutor, onClose }) => {
    const [paso, setPaso] = useState(1);

    // Paso 1
    const [fecha, setFecha] = useState("");
    const [hora, setHora] = useState("");
    const [franjas, setFranjas] = useState([]);
    const [ocupados, setOcupados] = useState([]);
    const [cargandoFranjas, setCargandoFranjas] = useState(false);
    const [franjaSeleccionada, setFranjaSeleccionada] = useState(null);

    // Paso 2
    const [modalidad, setModalidad] = useState("VIRTUAL");
    const [duracion, setDuracion] = useState(60);
    const [tema, setTema] = useState("");
    const [descripcion, setDescripcion] = useState("");
    const [ubicacion, setUbicacion] = useState("");
    const [aceptarTerminos, setAceptarTerminos] = useState(false);

    // Global
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState("");

    const nombreTutor = tutor?.usuario?.nombre || `Tutor #${tutor?.id}`;
    const tarifa = tutor?.tarifaHora ?? tutor?.tarifaPorHora ?? 0;

    useEffect(() => {
        if (!fecha) {
            setFranjas([]);
            setOcupados([]);
            return;
        }
        setCargandoFranjas(true);
        setFranjaSeleccionada(null);
        setHora("");
        Promise.all([
            getDisponibilidadTutorFecha(tutor.id, fecha).catch(() => []),
            getHorariosOcupados(tutor.id, fecha).catch(() => []),
        ]).then(([dispData, ocupData]) => {
            const lista = Array.isArray(dispData) ? dispData : dispData?.content ?? [];
            setFranjas(lista.filter((f) => f.activa !== false));
            setOcupados(Array.isArray(ocupData) ? ocupData : []);
        }).finally(() => setCargandoFranjas(false));
    }, [fecha, tutor.id]);

    const toMinutes = (hhmm) => {
        const [h, m] = (hhmm || "00:00").split(":").map(Number);
        return h * 60 + (m || 0);
    };

    const horaEsOcupada = (h) => {
        if (!h || ocupados.length === 0) return false;
        const hMin = toMinutes(h);
        return ocupados.some((o) => {
            const oStart = toMinutes(o.horaInicio);
            const oEnd = toMinutes(o.horaFin);
            return hMin >= oStart && hMin < oEnd;
        });
    };

    const franjaEsOcupada = (franja) => {
        if (ocupados.length === 0) return false;
        const fStart = toMinutes(franja.horaInicio);
        const fEnd = toMinutes(franja.horaFin);
        return ocupados.some((o) => {
            const oStart = toMinutes(o.horaInicio);
            const oEnd = toMinutes(o.horaFin);
            return fStart < oEnd && fEnd > oStart;
        });
    };

    const horaFinal = franjaSeleccionada ? franjaSeleccionada.horaInicio : hora;

    const handleSubmit = async () => {
        setError("");
        if (!aceptarTerminos) {
            setError("Debes aceptar los términos para continuar.");
            return;
        }
        if (!tema.trim()) {
            setError("El tema de la clase es obligatorio.");
            return;
        }
        if (!horaFinal) {
            setError("Selecciona una hora para la clase.");
            return;
        }

        const fechaHora = `${fecha}T${horaFinal}:00`;

        setSubmitting(true);
        try {
            const payload = {
                fechaHora,
                duracionMinutos: duracion,
                modalidad,
                tema: tema.trim(),
                descripcion: descripcion.trim() || undefined,
                aceptarTerminos: true,
            };
            if (modalidad === "PRESENCIAL" && ubicacion.trim()) {
                payload.ubicacion = ubicacion.trim();
            }

            const res = await crearReserva(tutor.id, payload);

            // El backend devuelve sessionId real de Stripe (cs_test_xxx o cs_live_xxx)
            // o simulado ("session_<id>"). Solo redirigir si es una sesión real.
            const isRealStripeSession = res?.sessionId &&
                (res.sessionId.startsWith("cs_test_") || res.sessionId.startsWith("cs_live_"));
            if (isRealStripeSession && res.paymentUrl) {
                window.location.href = res.paymentUrl;
            } else {
                setPaso(3);
            }
        } catch (err) {
            const msg =
                err?.response?.data?.message ||
                err?.message ||
                "No se pudo crear la reserva. Inténtalo de nuevo.";
            setError(msg);
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="bcm-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
            <div className="bcm-modal">
                {/* Header */}
                <div className="bcm-header">
                    <h2 className="bcm-title">
                        {paso === 1 && "Selecciona fecha y hora"}
                        {paso === 2 && "Detalles de la clase"}
                        {paso === 3 && "¡Reserva enviada!"}
                    </h2>
                    <button className="bcm-close" onClick={onClose}>✕</button>
                </div>

                {/* Pasos */}
                {paso < 3 && (
                    <div className="bcm-steps">
                        <div className={`bcm-step ${paso >= 1 ? "bcm-step--active" : ""}`}>
                            <span className="bcm-step__num">1</span>
                            <span className="bcm-step__label">Fecha y hora</span>
                        </div>
                        <div className="bcm-step__line" />
                        <div className={`bcm-step ${paso >= 2 ? "bcm-step--active" : ""}`}>
                            <span className="bcm-step__num">2</span>
                            <span className="bcm-step__label">Detalles</span>
                        </div>
                    </div>
                )}

                {/* ── Paso 1: Fecha y hora ── */}
                {paso === 1 && (
                    <div className="bcm-body">
                        <p className="bcm-subtitle">
                            Reserva una clase con <strong>{nombreTutor}</strong>
                            {tarifa > 0 && <> · <strong>{tarifa}€/h</strong></>}
                        </p>

                        <div className="bcm-field">
                            <label className="bcm-label" htmlFor="bcm-fecha">Fecha</label>
                            <input
                                id="bcm-fecha"
                                className="bcm-input"
                                type="date"
                                min={hoy()}
                                value={fecha}
                                onChange={(e) => setFecha(e.target.value)}
                            />
                        </div>

                        {fecha && (
                            <>
                                {cargandoFranjas ? (
                                    <p className="bcm-loading">Cargando disponibilidad…</p>
                                ) : franjas.length > 0 ? (
                                    <div className="bcm-field">
                                        <label className="bcm-label">Franjas disponibles</label>
                                        <div className="bcm-slots">
                                            {franjas.map((f) => {
                                                const ocupada = franjaEsOcupada(f);
                                                return (
                                                    <button
                                                        key={f.id}
                                                        className={`bcm-slot ${franjaSeleccionada?.id === f.id ? "bcm-slot--selected" : ""} ${ocupada ? "bcm-slot--ocupada" : ""}`}
                                                        disabled={ocupada}
                                                        onClick={() => { setFranjaSeleccionada(f); setHora(""); }}
                                                    >
                                                        {f.horaInicio} – {f.horaFin}
                                                        {f.modalidad && <span className="bcm-slot__mode"> ({f.modalidad})</span>}
                                                        {ocupada && <span className="bcm-slot__tag"> · Ocupado</span>}
                                                    </button>
                                                );
                                            })}
                                        </div>
                                    </div>
                                ) : (
                                    <div className="bcm-field">
                                        <label className="bcm-label" htmlFor="bcm-hora">
                                            Hora (el tutor no tiene franjas configuradas ese día)
                                        </label>
                                        <input
                                            id="bcm-hora"
                                            className="bcm-input"
                                            type="time"
                                            value={hora}
                                            onChange={(e) => { setHora(e.target.value); setFranjaSeleccionada(null); }}
                                        />
                                        {hora && horaEsOcupada(hora) && (
                                            <p className="bcm-slot-warning">
                                                Esta hora ya está reservada. Elige otro horario.
                                            </p>
                                        )}
                                    </div>
                                )}
                            </>
                        )}

                        <div className="bcm-footer">
                            <button className="bcm-btn bcm-btn--secondary" onClick={onClose}>
                                Cancelar
                            </button>
                            <button
                                className="bcm-btn bcm-btn--primary"
                                disabled={!fecha || (!franjaSeleccionada && !hora) || (hora && horaEsOcupada(hora))}
                                onClick={() => setPaso(2)}
                            >
                                Continuar →
                            </button>
                        </div>
                    </div>
                )}

                {/* ── Paso 2: Detalles ── */}
                {paso === 2 && (
                    <div className="bcm-body">
                        <div className="bcm-summary-box">
                            <span className="bcm-summary-label">Tutor</span>
                            <span className="bcm-summary-value">{nombreTutor}</span>
                            <span className="bcm-summary-label">Fecha y hora</span>
                            <span className="bcm-summary-value">
                                {fecha} a las {horaFinal}
                            </span>
                        </div>

                        <div className="bcm-field">
                            <label className="bcm-label">Modalidad</label>
                            <div className="bcm-radio-group">
                                {MODALIDADES.map((m) => (
                                    <label key={m.value} className="bcm-radio">
                                        <input
                                            type="radio"
                                            name="modalidad"
                                            value={m.value}
                                            checked={modalidad === m.value}
                                            onChange={() => setModalidad(m.value)}
                                        />
                                        {m.label}
                                    </label>
                                ))}
                            </div>
                        </div>

                        {modalidad === "PRESENCIAL" && (
                            <div className="bcm-field">
                                <label className="bcm-label" htmlFor="bcm-ubicacion">
                                    Ubicación (opcional)
                                </label>
                                <input
                                    id="bcm-ubicacion"
                                    className="bcm-input"
                                    type="text"
                                    placeholder="Dirección o lugar de encuentro"
                                    value={ubicacion}
                                    onChange={(e) => setUbicacion(e.target.value)}
                                />
                            </div>
                        )}

                        <div className="bcm-field">
                            <label className="bcm-label" htmlFor="bcm-duracion">Duración</label>
                            <select
                                id="bcm-duracion"
                                className="bcm-input"
                                value={duracion}
                                onChange={(e) => setDuracion(Number(e.target.value))}
                            >
                                {DURACIONES.map((d) => (
                                    <option key={d} value={d}>{d} minutos</option>
                                ))}
                            </select>
                        </div>

                        <div className="bcm-field">
                            <label className="bcm-label" htmlFor="bcm-tema">Tema *</label>
                            <input
                                id="bcm-tema"
                                className="bcm-input"
                                type="text"
                                placeholder="¿Qué quieres trabajar en la clase?"
                                value={tema}
                                onChange={(e) => setTema(e.target.value)}
                                maxLength={200}
                            />
                        </div>

                        <div className="bcm-field">
                            <label className="bcm-label" htmlFor="bcm-desc">Descripción (opcional)</label>
                            <textarea
                                id="bcm-desc"
                                className="bcm-input bcm-textarea"
                                placeholder="Detalles adicionales para el tutor…"
                                value={descripcion}
                                onChange={(e) => setDescripcion(e.target.value)}
                                rows={3}
                                maxLength={500}
                            />
                        </div>

                        {tarifa > 0 && (
                            <div className="bcm-price-box">
                                <div className="bcm-price-row">
                                    <span>Tarifa tutor ({duracion} min)</span>
                                    <strong>{((tarifa / 60) * duracion).toFixed(2)} €</strong>
                                </div>
                            </div>
                        )}

                        <label className="bcm-terms">
                            <input
                                type="checkbox"
                                checked={aceptarTerminos}
                                onChange={(e) => setAceptarTerminos(e.target.checked)}
                            />
                            Acepto los <a href="/planes" target="_blank" rel="noreferrer">términos y condiciones</a> de reserva de clases en MeerKat.
                        </label>

                        {error && <p className="bcm-error">⚠️ {error}</p>}

                        <div className="bcm-footer">
                            <button
                                className="bcm-btn bcm-btn--secondary"
                                onClick={() => { setPaso(1); setError(""); }}
                                disabled={submitting}
                            >
                                ← Atrás
                            </button>
                            <button
                                className="bcm-btn bcm-btn--primary"
                                onClick={handleSubmit}
                                disabled={submitting || !aceptarTerminos}
                            >
                                {submitting ? "Procesando…" : tarifa > 0 ? "Ir al pago →" : "Reservar clase"}
                            </button>
                        </div>
                    </div>
                )}

                {/* ── Paso 3: Confirmación (sin pago) ── */}
                {paso === 3 && (
                    <div className="bcm-body bcm-body--success">
                        <div className="bcm-success-icon">✓</div>
                        <h3 className="bcm-success-title">¡Reserva enviada!</h3>
                        <p className="bcm-success-text">
                            Tu solicitud de clase con <strong>{nombreTutor}</strong> ha sido enviada.
                            El tutor la revisará y recibirás una confirmación por email.
                        </p>
                        <button className="bcm-btn bcm-btn--primary" onClick={onClose}>
                            Cerrar
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
};

export default BookClassModal;

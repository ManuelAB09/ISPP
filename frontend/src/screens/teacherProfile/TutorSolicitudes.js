import { useCallback, useEffect, useState } from "react";
import { useSocketContext } from "../../contexts/SocketContext";
import {
  obtenerSolicitudesTutor,
  aceptarSolicitud,
  rechazarSolicitud,
  cancelarSolicitud,
  reprogramarSolicitud,
} from "../../api/solicitudContratacion";
import { cancelarReserva, confirmarReserva, getMisReservasTutor } from "../../api/reservas.api";
import "./TutorSolicitudes.css";

const MODALIDAD_LABEL = {
  ONLINE: "💻 Online",
  PRESENCIAL: "🏫 Presencial",
  HIBRIDO: "🔄 Híbrido",
};

const ESTADO_LABEL = {
  PENDIENTE: "⏳ Pendiente",
  ACEPTADA: "✅ Aceptada",
  RECHAZADA: "❌ Rechazada",
  PAGADA: "💰 Pagada",
  CANCELADA: "🚫 Cancelada",
};

const RESERVA_ESTADO_LABEL = {
  PENDIENTE: "⏳ Pendiente",
  CONFIRMADA: "✅ Confirmada",
  COMPLETADA: "🏁 Completada",
  CANCELADA_ALUMNO: "🚫 Cancelada (alumno)",
  CANCELADA_TUTOR: "🚫 Cancelada (tutor)",
  NO_ASISTIDA: "❌ No asistida",
};

const RESERVA_ESTADO_COLOR = {
  PENDIENTE: "ts-card--pendiente",
  CONFIRMADA: "ts-card--aceptada",
  COMPLETADA: "ts-card--pagada",
  CANCELADA_ALUMNO: "ts-card--cancelada",
  CANCELADA_TUTOR: "ts-card--cancelada",
  NO_ASISTIDA: "ts-card--rechazada",
};

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

/**
 * Panel de gestión de reservas del tutor.
 * Muestra solicitudes de contratación y reservas de clase directa.
 */
const TutorSolicitudes = () => {
  const { socket, isConnected } = useSocketContext();

  // ── Solicitudes de contratación ──────────────────────────
  const [solicitudes, setSolicitudes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [procesandoId, setProcesandoId] = useState(null);
  const [motivoRechazo, setMotivoRechazo] = useState("");
  const [rechazandoId, setRechazandoId] = useState(null);
  const [cancelandoId, setCancelandoId] = useState(null);
  const [motivoCancelacion, setMotivoCancelacion] = useState("");
  const [reprogramandoId, setReprogramandoId] = useState(null);
  const [nuevoDia, setNuevoDia] = useState("");
  const [nuevaHoraInicio, setNuevaHoraInicio] = useState("");
  const [nuevaHoraFin, setNuevaHoraFin] = useState("");
  const [tab, setTab] = useState("PENDIENTE");

  // ── Reservas de clase directa ─────────────────────────────
  const [reservas, setReservas] = useState([]);
  const [reservasLoading, setReservasLoading] = useState(true);
  const [reservaActionLoading, setReservaActionLoading] = useState(null);
  const [reservaCancelTarget, setReservaCancelTarget] = useState(null);
  const [reservaCancelMotivo, setReservaCancelMotivo] = useState("");
  const [reservasError, setReservasError] = useState("");
  const [reservasTab, setReservasTab] = useState("PENDIENTE");

  // ── Sección activa ────────────────────────────────────────
  const [seccion, setSeccion] = useState("contrataciones");

  // ── Carga solicitudes ─────────────────────────────────────
  const cargarSolicitudes = useCallback(async () => {
    try {
      setLoading(true);
      const { data } = await obtenerSolicitudesTutor();
      setSolicitudes(Array.isArray(data) ? data : []);
    } catch {
      setSolicitudes([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { cargarSolicitudes(); }, [cargarSolicitudes]);

  // ── Carga reservas directas ───────────────────────────────
  const cargarReservas = useCallback(() => {
    setReservasLoading(true);
    getMisReservasTutor({ size: 100 })
      .then((data) => {
        const lista = Array.isArray(data) ? data : data?.content ?? [];
        setReservas(lista);
      })
      .catch(() => setReservas([]))
      .finally(() => setReservasLoading(false));
  }, []);

  useEffect(() => { cargarReservas(); }, [cargarReservas]);

  // ── WebSocket ─────────────────────────────────────────────
  useEffect(() => {
    if (!socket || !isConnected) return;
    const handleNuevaSolicitud = (solicitud) => {
      if (solicitud?.id) {
        setSolicitudes((prev) => {
          if (prev.some((s) => s.id === solicitud.id)) return prev;
          return [solicitud, ...prev];
        });
      }
    };
    const handlePagada = (solicitud) => {
      if (solicitud?.id) {
        setSolicitudes((prev) =>
          prev.map((s) => (s.id === solicitud.id ? { ...s, ...solicitud } : s))
        );
      }
    };
    socket.on("solicitud_contratacion", handleNuevaSolicitud);
    socket.on("solicitud_contratacion_pagada", handlePagada);
    return () => {
      socket.off("solicitud_contratacion", handleNuevaSolicitud);
      socket.off("solicitud_contratacion_pagada", handlePagada);
    };
  }, [socket, isConnected]);

  // ── Acciones solicitudes ──────────────────────────────────
  const handleAceptar = async (solicitudId) => {
    setProcesandoId(solicitudId);
    try {
      const { data } = await aceptarSolicitud(solicitudId);
      setSolicitudes((prev) => prev.map((s) => (s.id === solicitudId ? { ...s, ...data } : s)));
    } catch (err) {
      alert(err?.response?.data?.error || "Error al aceptar la solicitud");
    } finally {
      setProcesandoId(null);
    }
  };

  const handleRechazar = async (solicitudId) => {
    setProcesandoId(solicitudId);
    try {
      const { data } = await rechazarSolicitud(solicitudId, motivoRechazo);
      setSolicitudes((prev) => prev.map((s) => (s.id === solicitudId ? { ...s, ...data } : s)));
      setRechazandoId(null);
      setMotivoRechazo("");
    } catch (err) {
      alert(err?.response?.data?.error || "Error al rechazar la solicitud");
    } finally {
      setProcesandoId(null);
    }
  };

  const handleCancelar = async (solicitudId) => {
    setProcesandoId(solicitudId);
    try {
      const { data } = await cancelarSolicitud(solicitudId, motivoCancelacion);
      setSolicitudes((prev) => prev.map((s) => (s.id === solicitudId ? { ...s, ...data } : s)));
      setCancelandoId(null);
      setMotivoCancelacion("");
    } catch (err) {
      alert(err?.response?.data?.error || "Error al cancelar la reserva");
    } finally {
      setProcesandoId(null);
    }
  };

  const handleReprogramar = async (solicitudId) => {
    if (!nuevoDia || !nuevaHoraInicio || !nuevaHoraFin) {
      alert("Completa todos los campos de la nueva fecha y horario.");
      return;
    }
    setProcesandoId(solicitudId);
    try {
      const { data } = await reprogramarSolicitud(solicitudId, {
        dia: nuevoDia,
        horaInicio: nuevaHoraInicio,
        horaFin: nuevaHoraFin,
      });
      setSolicitudes((prev) => prev.map((s) => (s.id === solicitudId ? { ...s, ...data } : s)));
      setReprogramandoId(null);
      setNuevoDia("");
      setNuevaHoraInicio("");
      setNuevaHoraFin("");
    } catch (err) {
      alert(err?.response?.data?.error || "Error al reprogramar la reserva");
    } finally {
      setProcesandoId(null);
    }
  };

  // ── Acciones reservas directas ────────────────────────────
  const handleConfirmarReserva = async (reservaId) => {
    setReservaActionLoading(reservaId + "_confirm");
    setReservasError("");
    try {
      await confirmarReserva(reservaId);
      cargarReservas();
    } catch (err) {
      setReservasError(err?.message || "Error al confirmar.");
    } finally {
      setReservaActionLoading(null);
    }
  };

  const handleCancelarReserva = async () => {
    setReservaActionLoading(reservaCancelTarget + "_cancel");
    setReservasError("");
    try {
      await cancelarReserva(reservaCancelTarget, reservaCancelMotivo);
      setReservaCancelTarget(null);
      setReservaCancelMotivo("");
      cargarReservas();
    } catch (err) {
      setReservasError(err?.message || "Error al cancelar.");
    } finally {
      setReservaActionLoading(null);
    }
  };

  // ── Datos derivados ───────────────────────────────────────
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  const minDate = tomorrow.toISOString().split("T")[0];

  const pendientes = solicitudes.filter((s) => s.estado === "PENDIENTE");
  const activas = solicitudes.filter((s) => s.estado === "ACEPTADA" || s.estado === "PAGADA");
  const historialSol = solicitudes.filter((s) => s.estado === "RECHAZADA" || s.estado === "CANCELADA");
  const currentList = tab === "PENDIENTE" ? pendientes : tab === "ACTIVAS" ? activas : historialSol;

  const reservasPendientes = reservas.filter((r) => r.estado === "PENDIENTE");
  const reservasConfirmadas = reservas.filter((r) => r.estado === "CONFIRMADA");
  const reservasHistorial = reservas.filter((r) =>
    ["COMPLETADA", "CANCELADA_ALUMNO", "CANCELADA_TUTOR", "NO_ASISTIDA"].includes(r.estado)
  );
  const reservasGrupos = { PENDIENTE: reservasPendientes, CONFIRMADA: reservasConfirmadas, HISTORIAL: reservasHistorial };
  const currentReservas = reservasGrupos[reservasTab] || [];

  return (
    <section className="ts-panel">
      <h2 className="ts-panel__title">📋 Gestión de reservas</h2>

      {/* Selector de sección */}
      <div className="ts-seccion-tabs">
        <button
          className={`ts-seccion-tab ${seccion === "contrataciones" ? "ts-seccion-tab--active" : ""}`}
          onClick={() => setSeccion("contrataciones")}
        >
          Solicitudes de contratación
          {pendientes.length > 0 && <span className="ts-seccion-tab__badge">{pendientes.length}</span>}
        </button>
        <button
          className={`ts-seccion-tab ${seccion === "clases" ? "ts-seccion-tab--active" : ""}`}
          onClick={() => setSeccion("clases")}
        >
          Reservas de clase directa
          {reservasPendientes.length > 0 && <span className="ts-seccion-tab__badge">{reservasPendientes.length}</span>}
        </button>
      </div>

      {/* ── Sección: Solicitudes de contratación ── */}
      {seccion === "contrataciones" && (
        <>
          <div className="ts-tabs">
            <button className={`ts-tab ${tab === "PENDIENTE" ? "ts-tab--active" : ""}`} onClick={() => setTab("PENDIENTE")}>
              Pendientes {pendientes.length > 0 && `(${pendientes.length})`}
            </button>
            <button className={`ts-tab ${tab === "ACTIVAS" ? "ts-tab--active" : ""}`} onClick={() => setTab("ACTIVAS")}>
              Confirmadas {activas.length > 0 && `(${activas.length})`}
            </button>
            <button className={`ts-tab ${tab === "HISTORIAL" ? "ts-tab--active" : ""}`} onClick={() => setTab("HISTORIAL")}>
              Historial
            </button>
          </div>

          {loading ? (
            <p className="ts-loading">Cargando reservas…</p>
          ) : currentList.length === 0 ? (
            <p className="ts-empty">
              {tab === "PENDIENTE" && "No tienes solicitudes pendientes."}
              {tab === "ACTIVAS" && "No tienes reservas confirmadas."}
              {tab === "HISTORIAL" && "Sin historial de reservas pasadas."}
            </p>
          ) : (
            <div className="ts-list">
              {currentList.map((s) => (
                <div key={s.id} className={`ts-card ts-card--${s.estado.toLowerCase()}`}>
                  <div className="ts-card__header">
                    <strong>{s.alumnoNombre}</strong>
                    <span className="ts-card__badge">{ESTADO_LABEL[s.estado] || s.estado}</span>
                  </div>
                  <div className="ts-card__details">
                    <div className="ts-card__row">
                      <span className="ts-card__label">📅 Día:</span>
                      <span>{s.dia}</span>
                    </div>
                    <div className="ts-card__row">
                      <span className="ts-card__label">🕐 Horario:</span>
                      <span>{s.horaInicio} – {s.horaFin}</span>
                    </div>
                    <div className="ts-card__row">
                      <span className="ts-card__label">💰 Importe:</span>
                      <span><strong>{s.importeTotal}€</strong> ({s.tarifaHora}€/h)</span>
                    </div>
                    {s.modalidad && (
                      <div className="ts-card__row">
                        <span className="ts-card__label">📍 Modalidad:</span>
                        <span>{MODALIDAD_LABEL[s.modalidad] || s.modalidad}</span>
                      </div>
                    )}
                    {s.mensaje && (
                      <div className="ts-card__row">
                        <span className="ts-card__label">💬 Mensaje:</span>
                        <span>{s.mensaje}</span>
                      </div>
                    )}
                    {s.motivoRechazo && (
                      <div className="ts-card__row">
                        <span className="ts-card__label">📝 Motivo:</span>
                        <span>{s.motivoRechazo}</span>
                      </div>
                    )}
                  </div>

                  {s.estado === "PENDIENTE" && rechazandoId !== s.id && (
                    <div className="ts-card__actions">
                      <button className="ts-btn ts-btn--reject" onClick={() => setRechazandoId(s.id)} disabled={procesandoId === s.id}>Rechazar</button>
                      <button className="ts-btn ts-btn--accept" onClick={() => handleAceptar(s.id)} disabled={procesandoId === s.id}>
                        {procesandoId === s.id ? "Aceptando…" : "✓ Aceptar"}
                      </button>
                    </div>
                  )}

                  {s.estado === "PENDIENTE" && rechazandoId === s.id && (
                    <div className="ts-card__rechazo">
                      <input type="text" className="ts-card__rechazo-input" placeholder="Motivo del rechazo (opcional)" value={motivoRechazo} onChange={(e) => setMotivoRechazo(e.target.value)} />
                      <div className="ts-card__actions">
                        <button className="ts-btn ts-btn--cancel" onClick={() => { setRechazandoId(null); setMotivoRechazo(""); }} disabled={procesandoId === s.id}>Volver</button>
                        <button className="ts-btn ts-btn--reject" onClick={() => handleRechazar(s.id)} disabled={procesandoId === s.id}>
                          {procesandoId === s.id ? "Rechazando…" : "Confirmar rechazo"}
                        </button>
                      </div>
                    </div>
                  )}

                  {(s.estado === "ACEPTADA" || s.estado === "PAGADA") && cancelandoId !== s.id && reprogramandoId !== s.id && (
                    <div className="ts-card__actions">
                      <button className="ts-btn ts-btn--cancel" onClick={() => setCancelandoId(s.id)} disabled={procesandoId === s.id}>🚫 Cancelar</button>
                      <button className="ts-btn ts-btn--reschedule" onClick={() => { setReprogramandoId(s.id); setNuevoDia(s.dia); setNuevaHoraInicio(s.horaInicio); setNuevaHoraFin(s.horaFin); }} disabled={procesandoId === s.id}>📅 Reprogramar</button>
                    </div>
                  )}

                  {cancelandoId === s.id && (
                    <div className="ts-card__rechazo">
                      <input type="text" className="ts-card__rechazo-input" placeholder="Motivo de la cancelación (opcional)" value={motivoCancelacion} onChange={(e) => setMotivoCancelacion(e.target.value)} />
                      <div className="ts-card__actions">
                        <button className="ts-btn ts-btn--cancel" onClick={() => { setCancelandoId(null); setMotivoCancelacion(""); }} disabled={procesandoId === s.id}>Volver</button>
                        <button className="ts-btn ts-btn--reject" onClick={() => handleCancelar(s.id)} disabled={procesandoId === s.id}>
                          {procesandoId === s.id ? "Cancelando…" : "Confirmar cancelación"}
                        </button>
                      </div>
                    </div>
                  )}

                  {reprogramandoId === s.id && (
                    <div className="ts-card__reschedule">
                      <p className="ts-card__reschedule-title">Nueva fecha y horario:</p>
                      <p className="ts-card__reschedule-hint">
                        Solo puedes mover la clase hasta 2 días después de la fecha original ({s.diaOriginal || s.dia}).
                      </p>
                      <div className="ts-card__reschedule-fields">
                        <input type="date" className="ts-card__reschedule-input" min={minDate} max={(() => { const d = new Date((s.diaOriginal || s.dia) + "T00:00:00"); d.setDate(d.getDate() + 2); return d.toISOString().split("T")[0]; })()} value={nuevoDia} onChange={(e) => setNuevoDia(e.target.value)} />
                        <input type="time" className="ts-card__reschedule-input" value={nuevaHoraInicio} onChange={(e) => setNuevaHoraInicio(e.target.value)} />
                        <span>–</span>
                        <input type="time" className="ts-card__reschedule-input" value={nuevaHoraFin} onChange={(e) => setNuevaHoraFin(e.target.value)} />
                      </div>
                      <div className="ts-card__actions">
                        <button className="ts-btn ts-btn--cancel" onClick={() => setReprogramandoId(null)} disabled={procesandoId === s.id}>Volver</button>
                        <button className="ts-btn ts-btn--accept" onClick={() => handleReprogramar(s.id)} disabled={procesandoId === s.id}>
                          {procesandoId === s.id ? "Guardando…" : "Confirmar nueva fecha"}
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {/* ── Sección: Reservas de clase directa ── */}
      {seccion === "clases" && (
        <>
          <div className="ts-tabs">
            {[["PENDIENTE", "Pendientes", reservasPendientes], ["CONFIRMADA", "Confirmadas", reservasConfirmadas], ["HISTORIAL", "Historial", reservasHistorial]].map(([key, label, grupo]) => (
              <button key={key} className={`ts-tab ${reservasTab === key ? "ts-tab--active" : ""}`} onClick={() => setReservasTab(key)}>
                {label} {grupo.length > 0 && `(${grupo.length})`}
              </button>
            ))}
          </div>

          {reservasError && <p className="ts-error">⚠️ {reservasError}</p>}

          {reservasLoading ? (
            <p className="ts-loading">Cargando reservas…</p>
          ) : currentReservas.length === 0 ? (
            <p className="ts-empty">No hay reservas en esta sección.</p>
          ) : (
            <div className="ts-list">
              {currentReservas.map((r) => (
                <div key={r.id} className={`ts-card ${RESERVA_ESTADO_COLOR[r.estado] || ""}`}>
                  <div className="ts-card__header">
                    <strong>{r.alumnoNombre || `Alumno #${r.alumnoId}`}</strong>
                    <span className="ts-card__badge">{RESERVA_ESTADO_LABEL[r.estado] || r.estado}</span>
                  </div>
                  <div className="ts-card__details">
                    <div className="ts-card__row">
                      <span className="ts-card__label">📅 Fecha:</span>
                      <span>{formatFecha(r.fechaHora)}</span>
                    </div>
                    <div className="ts-card__row">
                      <span className="ts-card__label">📖 Tema:</span>
                      <span>{r.tema}</span>
                    </div>
                    <div className="ts-card__row">
                      <span className="ts-card__label">📍 Modalidad:</span>
                      <span>{r.modalidad} · {r.duracionMinutos} min{r.tarifa != null ? ` · ${r.tarifa}€` : ""}</span>
                    </div>
                    {r.descripcion && (
                      <div className="ts-card__row">
                        <span className="ts-card__label">💬 Descripción:</span>
                        <span>{r.descripcion}</span>
                      </div>
                    )}
                  </div>

                  {r.estado === "PENDIENTE" && reservaCancelTarget !== r.id && (
                    <div className="ts-card__actions">
                      <button className="ts-btn ts-btn--reject" onClick={() => setReservaCancelTarget(r.id)} disabled={!!reservaActionLoading}>Rechazar</button>
                      <button className="ts-btn ts-btn--accept" onClick={() => handleConfirmarReserva(r.id)} disabled={!!reservaActionLoading}>
                        {reservaActionLoading === r.id + "_confirm" ? "Confirmando…" : "✓ Confirmar"}
                      </button>
                    </div>
                  )}

                  {r.estado === "CONFIRMADA" && reservaCancelTarget !== r.id && (
                    <div className="ts-card__actions">
                      <button className="ts-btn ts-btn--cancel" onClick={() => setReservaCancelTarget(r.id)} disabled={!!reservaActionLoading}>🚫 Cancelar</button>
                    </div>
                  )}

                  {reservaCancelTarget === r.id && (
                    <div className="ts-card__rechazo">
                      <input type="text" className="ts-card__rechazo-input" placeholder="Motivo de cancelación (opcional)" value={reservaCancelMotivo} onChange={(e) => setReservaCancelMotivo(e.target.value)} />
                      <div className="ts-card__actions">
                        <button className="ts-btn ts-btn--cancel" onClick={() => { setReservaCancelTarget(null); setReservaCancelMotivo(""); }} disabled={!!reservaActionLoading}>Volver</button>
                        <button className="ts-btn ts-btn--reject" onClick={handleCancelarReserva} disabled={!!reservaActionLoading}>
                          {reservaActionLoading === reservaCancelTarget + "_cancel" ? "Cancelando…" : "Confirmar cancelación"}
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </section>
  );
};

export default TutorSolicitudes;

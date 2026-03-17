import { useCallback, useEffect, useState } from "react";
import { useSocketContext } from "../../contexts/SocketContext";
import {
  obtenerSolicitudesTutor,
  aceptarSolicitud,
  rechazarSolicitud,
  cancelarSolicitud,
  reprogramarSolicitud,
} from "../../api/solicitudContratacion";
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

/**
 * Panel de gestión de reservas del tutor.
 * Muestra todas las solicitudes (pendientes, aceptadas, pagadas, etc.)
 * con acciones de aceptar, rechazar, cancelar y reprogramar.
 */
const TutorSolicitudes = () => {
  const { socket, isConnected } = useSocketContext();
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

  useEffect(() => {
    cargarSolicitudes();
  }, [cargarSolicitudes]);

  // Escuchar nuevas solicitudes por WebSocket
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

  const handleAceptar = async (solicitudId) => {
    setProcesandoId(solicitudId);
    try {
      const { data } = await aceptarSolicitud(solicitudId);
      setSolicitudes((prev) =>
        prev.map((s) => (s.id === solicitudId ? { ...s, ...data } : s))
      );
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
      setSolicitudes((prev) =>
        prev.map((s) => (s.id === solicitudId ? { ...s, ...data } : s))
      );
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
      setSolicitudes((prev) =>
        prev.map((s) => (s.id === solicitudId ? { ...s, ...data } : s))
      );
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
      setSolicitudes((prev) =>
        prev.map((s) => (s.id === solicitudId ? { ...s, ...data } : s))
      );
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

  // Fecha mínima para reprogramar: mañana
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  const minDate = tomorrow.toISOString().split("T")[0];

  const pendientes = solicitudes.filter((s) => s.estado === "PENDIENTE");
  const activas = solicitudes.filter((s) => s.estado === "ACEPTADA" || s.estado === "PAGADA");
  const historial = solicitudes.filter((s) => s.estado === "RECHAZADA" || s.estado === "CANCELADA");

  const currentList =
    tab === "PENDIENTE" ? pendientes : tab === "ACTIVAS" ? activas : historial;

  if (loading) return <p className="ts-loading">Cargando reservas…</p>;

  return (
    <section className="ts-panel">
      <h2 className="ts-panel__title">📋 Gestión de reservas</h2>

      {/* Tabs */}
      <div className="ts-tabs">
        <button
          className={`ts-tab ${tab === "PENDIENTE" ? "ts-tab--active" : ""}`}
          onClick={() => setTab("PENDIENTE")}
        >
          Pendientes {pendientes.length > 0 && `(${pendientes.length})`}
        </button>
        <button
          className={`ts-tab ${tab === "ACTIVAS" ? "ts-tab--active" : ""}`}
          onClick={() => setTab("ACTIVAS")}
        >
          Confirmadas {activas.length > 0 && `(${activas.length})`}
        </button>
        <button
          className={`ts-tab ${tab === "HISTORIAL" ? "ts-tab--active" : ""}`}
          onClick={() => setTab("HISTORIAL")}
        >
          Historial
        </button>
      </div>

      {currentList.length === 0 ? (
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

              {/* ═══ Actions for PENDIENTE ═══ */}
              {s.estado === "PENDIENTE" && rechazandoId !== s.id && (
                <div className="ts-card__actions">
                  <button
                    className="ts-btn ts-btn--reject"
                    onClick={() => setRechazandoId(s.id)}
                    disabled={procesandoId === s.id}
                  >
                    Rechazar
                  </button>
                  <button
                    className="ts-btn ts-btn--accept"
                    onClick={() => handleAceptar(s.id)}
                    disabled={procesandoId === s.id}
                  >
                    {procesandoId === s.id ? "Aceptando…" : "✓ Aceptar"}
                  </button>
                </div>
              )}

              {s.estado === "PENDIENTE" && rechazandoId === s.id && (
                <div className="ts-card__rechazo">
                  <input
                    type="text"
                    className="ts-card__rechazo-input"
                    placeholder="Motivo del rechazo (opcional)"
                    value={motivoRechazo}
                    onChange={(e) => setMotivoRechazo(e.target.value)}
                  />
                  <div className="ts-card__actions">
                    <button
                      className="ts-btn ts-btn--cancel"
                      onClick={() => { setRechazandoId(null); setMotivoRechazo(""); }}
                      disabled={procesandoId === s.id}
                    >
                      Volver
                    </button>
                    <button
                      className="ts-btn ts-btn--reject"
                      onClick={() => handleRechazar(s.id)}
                      disabled={procesandoId === s.id}
                    >
                      {procesandoId === s.id ? "Rechazando…" : "Confirmar rechazo"}
                    </button>
                  </div>
                </div>
              )}

              {/* ═══ Actions for ACTIVAS (ACEPTADA/PAGADA) ═══ */}
              {(s.estado === "ACEPTADA" || s.estado === "PAGADA")
                && cancelandoId !== s.id && reprogramandoId !== s.id && (
                <div className="ts-card__actions">
                  <button
                    className="ts-btn ts-btn--cancel"
                    onClick={() => setCancelandoId(s.id)}
                    disabled={procesandoId === s.id}
                  >
                    🚫 Cancelar
                  </button>
                  <button
                    className="ts-btn ts-btn--reschedule"
                    onClick={() => {
                      setReprogramandoId(s.id);
                      setNuevoDia(s.dia);
                      setNuevaHoraInicio(s.horaInicio);
                      setNuevaHoraFin(s.horaFin);
                    }}
                    disabled={procesandoId === s.id}
                  >
                    📅 Reprogramar
                  </button>
                </div>
              )}

              {/* Cancel form */}
              {cancelandoId === s.id && (
                <div className="ts-card__rechazo">
                  <input
                    type="text"
                    className="ts-card__rechazo-input"
                    placeholder="Motivo de la cancelación (opcional)"
                    value={motivoCancelacion}
                    onChange={(e) => setMotivoCancelacion(e.target.value)}
                  />
                  <div className="ts-card__actions">
                    <button
                      className="ts-btn ts-btn--cancel"
                      onClick={() => { setCancelandoId(null); setMotivoCancelacion(""); }}
                      disabled={procesandoId === s.id}
                    >
                      Volver
                    </button>
                    <button
                      className="ts-btn ts-btn--reject"
                      onClick={() => handleCancelar(s.id)}
                      disabled={procesandoId === s.id}
                    >
                      {procesandoId === s.id ? "Cancelando…" : "Confirmar cancelación"}
                    </button>
                  </div>
                </div>
              )}

              {/* Reschedule form */}
              {reprogramandoId === s.id && (
                <div className="ts-card__reschedule">
                  <p className="ts-card__reschedule-title">Nueva fecha y horario:</p>
                  <div className="ts-card__reschedule-fields">
                    <input
                      type="date"
                      className="ts-card__reschedule-input"
                      min={minDate}
                      value={nuevoDia}
                      onChange={(e) => setNuevoDia(e.target.value)}
                    />
                    <input
                      type="time"
                      className="ts-card__reschedule-input"
                      value={nuevaHoraInicio}
                      onChange={(e) => setNuevaHoraInicio(e.target.value)}
                    />
                    <span>–</span>
                    <input
                      type="time"
                      className="ts-card__reschedule-input"
                      value={nuevaHoraFin}
                      onChange={(e) => setNuevaHoraFin(e.target.value)}
                    />
                  </div>
                  <div className="ts-card__actions">
                    <button
                      className="ts-btn ts-btn--cancel"
                      onClick={() => { setReprogramandoId(null); }}
                      disabled={procesandoId === s.id}
                    >
                      Volver
                    </button>
                    <button
                      className="ts-btn ts-btn--accept"
                      onClick={() => handleReprogramar(s.id)}
                      disabled={procesandoId === s.id}
                    >
                      {procesandoId === s.id ? "Guardando…" : "Confirmar nueva fecha"}
                    </button>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </section>
  );
};

export default TutorSolicitudes;

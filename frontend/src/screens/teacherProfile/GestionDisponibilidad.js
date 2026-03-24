import React, { useEffect, useState } from "react";
import {
  getDisponibilidades,
  crearDisponibilidad,
  eliminarDisponibilidad,
} from "../../api/disponibilidad";

const DIAS = [
  { value: "MONDAY", label: "Lunes" },
  { value: "TUESDAY", label: "Martes" },
  { value: "WEDNESDAY", label: "Miércoles" },
  { value: "THURSDAY", label: "Jueves" },
  { value: "FRIDAY", label: "Viernes" },
  { value: "SATURDAY", label: "Sábado" },
  { value: "SUNDAY", label: "Domingo" },
];

const DIA_LABEL = {
  MONDAY: "Lunes",
  TUESDAY: "Martes",
  WEDNESDAY: "Miércoles",
  THURSDAY: "Jueves",
  FRIDAY: "Viernes",
  SATURDAY: "Sábado",
  SUNDAY: "Domingo",
};

const MODALIDADES = [
  { value: "ONLINE", label: "Online" },
  { value: "PRESENCIAL", label: "Presencial" },
];

const EMPTY_FORM = {
  esRecurrente: true,
  diaSemana: "MONDAY",
  fechaPuntual: "",
  horaInicio: "",
  horaFin: "",
  modalidad: "ONLINE",
  ubicacionPresencial: "",
};

const GestionDisponibilidad = ({ tutorId, onClose }) => {
  const [franjas, setFranjas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [showForm, setShowForm] = useState(false);

  const cargar = async () => {
    setLoading(true);
    try {
      const res = await getDisponibilidades(tutorId);
      setFranjas(Array.isArray(res) ? res : res?.data || []);
    } catch {
      setError("No se pudo cargar la disponibilidad.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    cargar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tutorId]);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const abrirNuevo = () => {
    setForm(EMPTY_FORM);
    setError("");
    setShowForm(true);
  };

  const cancelarForm = () => {
    setShowForm(false);
    setError("");
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    if (!form.horaInicio || !form.horaFin) {
      setError("Las horas de inicio y fin son obligatorias.");
      return;
    }
    if (form.horaInicio >= form.horaFin) {
      setError("La hora de inicio debe ser anterior a la de fin.");
      return;
    }
    if (!form.esRecurrente && !form.fechaPuntual) {
      setError("Debes indicar la fecha para una disponibilidad puntual.");
      return;
    }
    setSaving(true);
    try {
      const payload = {
        esRecurrente: form.esRecurrente,
        diaSemana: form.esRecurrente ? form.diaSemana : null,
        fechaPuntual: !form.esRecurrente
          ? form.fechaPuntual + "T" + form.horaInicio + ":00"
          : null,
        horaInicio: form.horaInicio + ":00",
        horaFin: form.horaFin + ":00",
        modalidad: form.modalidad,
        ubicacionPresencial:
          form.modalidad === "PRESENCIAL" ? form.ubicacionPresencial : null,
      };
      await crearDisponibilidad(payload);
      setShowForm(false);
      await cargar();
    } catch (err) {
      const status = err?.status ?? err?.response?.status;
      const backendMsg =
        err?.message ||
        err?.details?.message ||
        err?.details?.error ||
        err?.response?.data?.message ||
        err?.response?.data?.error;
      let msg = backendMsg;
      if (!msg) {
        if (status === 409) {
          msg = "La franja se solapa con otra disponibilidad existente.";
        } else if (status >= 500) {
          msg = "Error interno al guardar. Inténtalo de nuevo en unos segundos.";
        } else {
          msg = "No se pudo guardar la disponibilidad.";
        }
      }
      setError(msg);
    } finally {
      setSaving(false);
    }
  };

  const handleEliminar = async (id) => {
    if (!window.confirm("¿Eliminar esta franja horaria?")) return;
    try {
      await eliminarDisponibilidad(id);
      await cargar();
    } catch {
      setError("No se pudo eliminar la franja.");
    }
  };

  return (
    <div
      className="tm-overlay"
      onClick={(e) => e.target === e.currentTarget && onClose()}
    >
      <div
        className="tm-modal"
        role="dialog"
        aria-modal="true"
        aria-label="Gestionar disponibilidad"
        style={{ maxWidth: 560, maxHeight: "90vh", overflowY: "auto" }}
      >
        <div className="tm-modal__header">
          <h2 className="tm-modal__title">📅 Mi disponibilidad</h2>
          <button
            className="tm-modal__close"
            onClick={onClose}
            aria-label="Cerrar"
          >
            ✕
          </button>
        </div>

        {error && (
          <p style={{ color: "#c00", padding: "0 24px", margin: "8px 0" }}>
            {error}
          </p>
        )}

        {/* Lista de franjas existentes */}
        <div style={{ padding: "0 24px" }}>
          {loading ? (
            <p style={{ color: "#666" }}>Cargando…</p>
          ) : franjas.length === 0 ? (
            <p style={{ color: "#888", fontStyle: "italic" }}>
              Todavía no tienes franjas horarias configuradas.
            </p>
          ) : (
            <ul style={{ listStyle: "none", padding: 0, margin: "12px 0" }}>
              {franjas.map((f) => (
                <li
                  key={f.id}
                  style={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "space-between",
                    padding: "10px 12px",
                    marginBottom: 8,
                    borderRadius: 8,
                    background: "#f4f0ff",
                    gap: 8,
                  }}
                >
                  <div style={{ fontSize: "0.95rem" }}>
                    <strong>
                      {f.esRecurrente
                        ? `Todos los ${DIA_LABEL[f.diaSemana] || f.diaSemana}`
                        : `Puntual: ${
                            f.fechaPuntual
                              ? new Date(f.fechaPuntual).toLocaleDateString(
                                  "es-ES"
                                )
                              : "—"
                          }`}
                    </strong>{" "}
                    · {f.horaInicio?.slice(0, 5)} – {f.horaFin?.slice(0, 5)} ·{" "}
                    {f.modalidad === "PRESENCIAL" ? "🏫 Presencial" : "💻 Online"}
                    {f.ubicacionPresencial && (
                      <span style={{ color: "#555" }}>
                        {" "}
                        ({f.ubicacionPresencial})
                      </span>
                    )}
                  </div>
                  <div style={{ display: "flex", gap: 6, flexShrink: 0 }}>
                    <button
                      className="tp-btn"
                      style={{
                        padding: "4px 10px",
                        fontSize: "0.8rem",
                        background: "#e53e3e",
                        color: "#fff",
                        border: "none",
                        borderRadius: 6,
                        cursor: "pointer",
                      }}
                      onClick={() => handleEliminar(f.id)}
                    >
                      Eliminar
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          )}

          {!showForm && (
            <button
              className="tp-btn tp-btn--hire"
              style={{ marginBottom: 16 }}
              onClick={abrirNuevo}
            >
              + Añadir franja horaria
            </button>
          )}
        </div>

        {/* Formulario añadir / editar */}
        {showForm && (
          <form
            className="tm-form"
            onSubmit={handleSubmit}
            style={{ padding: "0 24px 24px" }}
          >
            <hr style={{ margin: "0 0 16px" }} />
            <h3 style={{ margin: "0 0 12px", fontSize: "1rem" }}>
              Nueva franja horaria
            </h3>

            {/* Tipo: recurrente o puntual */}
            <div className="tm-field" style={{ flexDirection: "row", gap: 24, alignItems: "center" }}>
              <label style={{ display: "flex", alignItems: "center", gap: 6, cursor: "pointer" }}>
                <input
                  type="radio"
                  name="esRecurrente"
                  checked={form.esRecurrente === true}
                  onChange={() => setForm((p) => ({ ...p, esRecurrente: true }))}
                />
                Semanal (recurrente)
              </label>
              <label style={{ display: "flex", alignItems: "center", gap: 6, cursor: "pointer" }}>
                <input
                  type="radio"
                  name="esRecurrente"
                  checked={form.esRecurrente === false}
                  onChange={() => setForm((p) => ({ ...p, esRecurrente: false }))}
                />
                Puntual (una fecha)
              </label>
            </div>

            {/* Día de semana o fecha puntual */}
            {form.esRecurrente ? (
              <div className="tm-field">
                <label className="tm-field__label" htmlFor="diaSemana">
                  Día de la semana
                </label>
                <select
                  id="diaSemana"
                  name="diaSemana"
                  className="tm-field__input"
                  value={form.diaSemana}
                  onChange={handleChange}
                >
                  {DIAS.map((d) => (
                    <option key={d.value} value={d.value}>
                      {d.label}
                    </option>
                  ))}
                </select>
              </div>
            ) : (
              <div className="tm-field">
                <label className="tm-field__label" htmlFor="fechaPuntual">
                  Fecha de inicio del evento
                </label>
                <input
                  id="fechaPuntual"
                  name="fechaPuntual"
                  type="date"
                  className="tm-field__input"
                  value={form.fechaPuntual}
                  onChange={handleChange}
                  min={new Date().toISOString().slice(0, 10)}
                />
              </div>
            )}

            {/* Horas */}
            <div style={{ display: "flex", gap: 16 }}>
              <div className="tm-field" style={{ flex: 1 }}>
                <label className="tm-field__label" htmlFor="horaInicio">
                  Hora inicio
                </label>
                <input
                  id="horaInicio"
                  name="horaInicio"
                  type="time"
                  className="tm-field__input"
                  value={form.horaInicio}
                  onChange={handleChange}
                  required
                />
              </div>
              <div className="tm-field" style={{ flex: 1 }}>
                <label className="tm-field__label" htmlFor="horaFin">
                  Hora fin
                </label>
                <input
                  id="horaFin"
                  name="horaFin"
                  type="time"
                  className="tm-field__input"
                  value={form.horaFin}
                  onChange={handleChange}
                  required
                />
              </div>
            </div>

            {/* Modalidad */}
            <div className="tm-field">
              <label className="tm-field__label" htmlFor="modalidad">
                Modalidad
              </label>
              <select
                id="modalidad"
                name="modalidad"
                className="tm-field__input"
                value={form.modalidad}
                onChange={handleChange}
              >
                {MODALIDADES.map((m) => (
                  <option key={m.value} value={m.value}>
                    {m.label}
                  </option>
                ))}
              </select>
            </div>

            {/* Ubicación (solo presencial/híbrida) */}
            {form.modalidad === "PRESENCIAL" && (
              <div className="tm-field">
                <label
                  className="tm-field__label"
                  htmlFor="ubicacionPresencial"
                >
                  Ubicación presencial
                </label>
                <input
                  id="ubicacionPresencial"
                  name="ubicacionPresencial"
                  type="text"
                  className="tm-field__input"
                  value={form.ubicacionPresencial}
                  onChange={handleChange}
                  placeholder="Ej: Biblioteca de la Facultad, Calle Mayor 3…"
                />
              </div>
            )}

            {error && (
              <p style={{ color: "#c00", marginTop: 4 }}>{error}</p>
            )}

            <div style={{ display: "flex", gap: 10, marginTop: 8 }}>
              <button
                type="submit"
                className="tp-btn tp-btn--hire"
                disabled={saving}
              >
                {saving ? "Guardando…" : "Añadir"}
              </button>
              <button
                type="button"
                className="tp-btn tp-btn--edit"
                onClick={cancelarForm}
              >
                Cancelar
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};

export default GestionDisponibilidad;

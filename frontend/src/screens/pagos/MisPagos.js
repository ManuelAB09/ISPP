import { useCallback, useEffect, useState } from "react";
import { paymentsApi } from "../../api/payments.api";
import Header from "../../components/Header/Header";
import PageHeader from "../../components/PageHeader";
import "./MisPagos.css";

/* ─── Helpers ─────────────────────────────────────────── */
const TIPO_LABEL = {
  PAGO_VERIFICACION: "Verificación de tutor",
  PAGO_TUTOR: "Contratación de tutor",
  SUSCRIPCION: "Suscripción Premium",
  COMISION: "Comisión de plataforma",
};

const ESTADO_CLASS = {
  COMPLETADO: "mp-badge mp-badge--success",
  PENDIENTE: "mp-badge mp-badge--warning",
  FALLIDO: "mp-badge mp-badge--error",
  REEMBOLSADO: "mp-badge mp-badge--info",
};

const ESTADO_LABEL = {
  COMPLETADO: "Completado",
  PENDIENTE: "Pendiente",
  FALLIDO: "Fallido",
  REEMBOLSADO: "Reembolsado",
};

const fmt = (amount) =>
  new Intl.NumberFormat("es-ES", { style: "currency", currency: "EUR" }).format(amount ?? 0);

const fmtDate = (iso) => {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString("es-ES", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
};

/* ─── Component ──────────────────────────────────────── */
const MisPagos = () => {
  const [transacciones, setTransacciones] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const PAGE_SIZE = 15;

  const cargarPagos = useCallback(async (p) => {
    setCargando(true);
    setError(null);
    try {
      const data = await paymentsApi.getHistory({ page: p, size: PAGE_SIZE });
      if (data?.content) {
        setTransacciones(data.content);
        setTotalPages(data.totalPages ?? 1);
      } else if (Array.isArray(data)) {
        setTransacciones(data);
        setTotalPages(1);
      } else {
        setTransacciones([]);
      }
    } catch (err) {
      setError(err?.response?.data?.message || "No se pudo cargar el historial de pagos.");
    } finally {
      setCargando(false);
    }
  }, []);

  useEffect(() => {
    cargarPagos(page);
  }, [cargarPagos, page]);

  const totalGastado = transacciones
    .filter((t) => t.estado === "COMPLETADO")
    .reduce((acc, t) => acc + (t.monto ?? 0), 0);
  const totalComisiones = transacciones
    .filter((t) => t.tipo === "COMISION" && t.estado === "COMPLETADO")
    .reduce((acc, t) => acc + (t.monto ?? 0), 0);

  return (
    <>
      <Header page={"pagos"} />
      <div className="mp-page">
        <div className="mp-container">
          {/* Page title */}
          <div className="mp-page-header">
            <PageHeader
              title="Mis pagos"
              subtitle="Historial de transacciones de tu cuenta y resumen de actividad financiera"
            />
          </div>

          {/* Summary cards */}
          {!cargando && !error && transacciones.length > 0 && (
            <div className="mp-summary">
              <div className="mp-summary-card">
                <span className="mp-summary-card__label">Total transacciones</span>
                <span className="mp-summary-card__value">{transacciones.length}</span>
              </div>
              <div className="mp-summary-card">
                <span className="mp-summary-card__label">Total abonado</span>
                <span className="mp-summary-card__value">{fmt(totalGastado)}</span>
              </div>
              <div className="mp-summary-card mp-summary-card--muted">
                <span className="mp-summary-card__label">Comisiones plataforma</span>
                <span className="mp-summary-card__value">{fmt(totalComisiones)}</span>
              </div>
            </div>
          )}

          {/* Loading */}
          {cargando && (
            <div className="mp-state">
              <div className="mp-spinner" />
              <p>Cargando historial…</p>
            </div>
          )}

          {/* Error */}
          {error && (
            <div className="mp-state mp-state--error">
              <p>⚠️ {error}</p>
              <button className="mp-retry" onClick={() => cargarPagos(page)}>
                Reintentar
              </button>
            </div>
          )}

          {/* Empty */}
          {!cargando && !error && transacciones.length === 0 && (
            <div className="mp-state mp-state--empty">
              <div className="mp-empty-icon">💳</div>
              <h3>Sin transacciones</h3>
              <p>Todavía no has realizado ningún pago en MeerKat.</p>
            </div>
          )}

          {/* Table */}
          {!cargando && !error && transacciones.length > 0 && (
            <>
              <div className="mp-table-wrapper">
                <table className="mp-table">
                  <thead>
                    <tr>
                      <th>Tipo</th>
                      <th>Importe</th>
                      <th>Comisión (10%)</th>
                      <th>Neto tutor</th>
                      <th>Estado</th>
                      <th>Descripción</th>
                      <th>Fecha</th>
                    </tr>
                  </thead>
                  <tbody>
                    {transacciones.map((t) => {
                      const comision = t.comision ?? (t.monto ?? 0) * 0.1;
                      const neto = t.montoNeto ?? (t.monto ?? 0) - comision;
                      const esPagoTutor = t.tipo === "PAGO_TUTOR";

                      return (
                        <tr key={t.id} className="mp-row">
                          <td>
                            <span className="mp-tipo">
                              {TIPO_LABEL[t.tipo] ?? t.tipo}
                            </span>
                          </td>
                          <td className="mp-amount">{fmt(t.monto)}</td>
                          <td className="mp-amount mp-amount--muted">
                            {esPagoTutor ? `− ${fmt(comision)}` : "—"}
                          </td>
                          <td className="mp-amount mp-amount--net">
                            {esPagoTutor ? fmt(neto) : "—"}
                          </td>
                          <td>
                            <span className={ESTADO_CLASS[t.estado] ?? "mp-badge"}>
                              {ESTADO_LABEL[t.estado] ?? t.estado}
                            </span>
                          </td>
                          <td className="mp-desc">{t.descripcion || "—"}</td>
                          <td className="mp-date">
                            {fmtDate(t.fechaCreacion ?? t.fecha)}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>

              {/* Pagination */}
              {totalPages > 1 && (
                <div className="mp-pagination">
                  <button
                    className="mp-page-btn"
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={page === 0}
                  >
                    ← Anterior
                  </button>
                  <span className="mp-page-indicator">
                    Página {page + 1} de {totalPages}
                  </span>
                  <button
                    className="mp-page-btn"
                    onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                    disabled={page >= totalPages - 1}
                  >
                    Siguiente →
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </>
  );
};

export default MisPagos;

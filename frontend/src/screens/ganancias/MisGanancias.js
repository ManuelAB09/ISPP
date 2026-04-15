import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { obtenerGananciasTutor } from "../../api/tutorEndpoints";
import Header from "../../components/Header/Header";
import PageHeader from "../../components/PageHeader";
import "./MisGanancias.css";

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

const extraerMensajeError = (err) => {
  const payload = err?.response?.data;
  if (typeof payload === "string" && payload.trim()) return payload;
  if (payload && typeof payload === "object") {
    if (typeof payload.error === "string" && payload.error.trim()) return payload.error;
    if (typeof payload.message === "string" && payload.message.trim()) return payload.message;
  }
  return "No se pudieron cargar las ganancias.";
};

const esErrorSinPerfilTutor = (err, mensaje) => {
  const status = err?.response?.status;
  if (status !== 400 && status !== 404) return false;

  const texto = String(mensaje || "").toLowerCase();
  return (
    texto.includes("no tienes un perfil de tutor") ||
    texto.includes("perfil de tutor") ||
    texto.includes("perfil tutor")
  );
};

const MisGanancias = () => {
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [sinPerfilTutor, setSinPerfilTutor] = useState(false);
  const [page, setPage] = useState(0);
  const PAGE_SIZE = 15;

  const cargar = useCallback(async (p) => {
    setCargando(true);
    setError(null);
    setSinPerfilTutor(false);
    try {
      const res = await obtenerGananciasTutor({ page: p, size: PAGE_SIZE });
      setData(res);
    } catch (err) {
      const mensaje = extraerMensajeError(err);
      const perfilNoExiste = esErrorSinPerfilTutor(err, mensaje);

      setSinPerfilTutor(perfilNoExiste);
      setError(
        perfilNoExiste
          ? "No puedes ver tus ganancias porque todavía no has creado tu perfil de tutor."
          : mensaje
      );
    } finally {
      setCargando(false);
    }
  }, []);

  useEffect(() => {
    cargar(page);
  }, [cargar, page]);

  const transacciones = data?.content || [];
  const totalPages = data?.totalPages ?? 1;

  return (
    <>
      <Header page={"ganancias"} />
      <div className="mg-page">
        <div className="mg-container">
          <div className="mg-page-header">
            <PageHeader
              title="Mis ganancias"
              subtitle="Resumen de los pagos recibidos por tus clases como tutor"
            />
          </div>

          {/* Summary cards */}
          {!cargando && !error && data && (
            <div className="mg-summary">
              <div className="mg-summary-card mg-summary-card--primary">
                <span className="mg-summary-card__label">Total neto recibido</span>
                <span className="mg-summary-card__value">{fmt(data.totalNeto)}</span>
              </div>
              <div className="mg-summary-card">
                <span className="mg-summary-card__label">Total bruto</span>
                <span className="mg-summary-card__value">{fmt(data.totalBruto)}</span>
              </div>
              <div className="mg-summary-card mg-summary-card--muted">
                <span className="mg-summary-card__label">Comisiones (10%)</span>
                <span className="mg-summary-card__value">{fmt(data.totalComisiones)}</span>
              </div>
              <div className="mg-summary-card">
                <span className="mg-summary-card__label">Clases pagadas</span>
                <span className="mg-summary-card__value">{data.totalTransacciones ?? 0}</span>
              </div>
            </div>
          )}

          {cargando && (
            <div className="mg-state">
              <div className="mg-spinner" />
              <p>Cargando ganancias…</p>
            </div>
          )}

          {error && (
            <div className="mg-state mg-state--error">
              <p>⚠️ {error}</p>
              <div className="mg-actions">
                {sinPerfilTutor ? (
                  <button className="mg-retry" onClick={() => navigate("/profesores/nuevo")}>
                    Crear perfil de tutor
                  </button>
                ) : (
                  <button className="mg-retry" onClick={() => cargar(page)}>
                    Reintentar
                  </button>
                )}
              </div>
            </div>
          )}

          {!cargando && !error && transacciones.length === 0 && (
            <div className="mg-state mg-state--empty">
              <div className="mg-empty-icon">💰</div>
              <h3>Sin ganancias aún</h3>
              <p>Cuando tus alumnos paguen por tus clases, aparecerán aquí.</p>
            </div>
          )}

          {!cargando && !error && transacciones.length > 0 && (
            <>
              <div className="mg-table-wrapper">
                <table className="mg-table">
                  <thead>
                    <tr>
                      <th>Importe bruto</th>
                      <th>Comisión (10%)</th>
                      <th>Neto recibido</th>
                      <th>Estado</th>
                      <th>Fecha</th>
                    </tr>
                  </thead>
                  <tbody>
                    {transacciones.map((t) => (
                      <tr key={t.id} className="mg-row">
                        <td className="mg-amount">{fmt(t.monto)}</td>
                        <td className="mg-amount mg-amount--muted">
                          − {fmt(t.comision)}
                        </td>
                        <td className="mg-amount mg-amount--net">{fmt(t.montoNeto)}</td>
                        <td>
                          <span className="mg-badge mg-badge--success">Completado</span>
                        </td>
                        <td className="mg-date">{fmtDate(t.fecha)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {totalPages > 1 && (
                <div className="mg-pagination">
                  <button
                    className="mg-page-btn"
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={page === 0}
                  >
                    ← Anterior
                  </button>
                  <span className="mg-page-info">
                    Página {page + 1} de {totalPages}
                  </span>
                  <button
                    className="mg-page-btn"
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

export default MisGanancias;

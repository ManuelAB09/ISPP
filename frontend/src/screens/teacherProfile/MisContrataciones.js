import { useEffect, useState } from "react";
import { paymentsApi } from "../../api/payments.api";
import Header from "../../components/Header/Header";
import { useNavigate } from "react-router-dom";

const estadoLabel = {
    PENDIENTE_APROBACION: { label: "Esperando al tutor", color: "#f59e0b", bg: "#fffbeb" },
    APROBADA: { label: "Pendiente de pago", color: "#2563eb", bg: "#eff6ff" },
    PENDIENTE_PAGO: { label: "Pendiente de pago", color: "#2563eb", bg: "#eff6ff" },
    ACTIVA: { label: "Activa", color: "#15803d", bg: "#f0fdf4" },
    COMPLETADA: { label: "Completada", color: "#6b7280", bg: "#f3f4f6" },
    RECHAZADA: { label: "Rechazada", color: "#b91c1c", bg: "#fef2f2" },
    CANCELADA: { label: "Cancelada", color: "#b91c1c", bg: "#fef2f2" },
};

export default function MisContrataciones() {
    const navigate = useNavigate();
    const [contrataciones, setContrataciones] = useState([]);
    const [cargando, setCargando] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        paymentsApi.getMisContrataciones({ page: 0, size: 20 })
            .then(data => {
                const lista = Array.isArray(data) ? data : (data?.content ?? []);
                setContrataciones(lista);
            })
            .catch(() => setError("No se pudieron cargar las contrataciones."))
            .finally(() => setCargando(false));
    }, []);

    if (cargando) {
        return (
            <>
                <Header />
                <div style={styles.page}>
                    <div style={styles.centerBox}>
                        <div style={styles.spinner} />
                        <p style={{ color: "#6b7280", marginTop: 16 }}>Cargando contrataciones…</p>
                    </div>
                </div>
            </>
        );
    }

    return (
        <>
            <Header />
            <div style={styles.page}>
                <div style={styles.container}>

                    <div style={styles.pageHeader}>
                        <h1 style={styles.title}>Mis contrataciones</h1>
                        <p style={styles.subtitle}>
                            Gestiona las contrataciones de tutores para tus comunidades
                        </p>
                    </div>

                    {error && (
                        <div style={styles.errorBox}>⚠️ {error}</div>
                    )}

                    {!error && contrataciones.length === 0 && (
                        <div style={styles.emptyBox}>
                            <p style={{ fontSize: 48, marginBottom: 12 }}>🎓</p>
                            <p style={{ fontWeight: 700, fontSize: 18, color: "#374151", marginBottom: 8 }}>
                                Aún no tienes contrataciones
                            </p>
                            <p style={{ color: "#9ca3af", fontSize: 14, marginBottom: 24 }}>
                                Contrata un tutor desde el perfil de un profesor verificado.
                            </p>
                            <button style={styles.btnPrimary} onClick={() => navigate("/profesores")}>
                                Buscar profesores
                            </button>
                        </div>
                    )}

                    <div style={styles.list}>
                        {contrataciones.map((c) => {
                            const est = estadoLabel[c.estado] ?? { label: c.estado, color: "#6b7280", bg: "#f3f4f6" };
                            const nombreTutor = c.tutor?.usuario?.nombre ?? `Tutor #${c.tutor?.id}`;
                            const necesitaPago = (c.estado === "APROBADA" || c.estado === "PENDIENTE_PAGO") && c.paymentUrl;

                            return (
                                <div key={c.id} style={styles.card}>
                                    <div style={styles.cardTop}>
                                        <div style={styles.tutorInfo}>
                                            <div style={styles.avatar}>
                                                {nombreTutor.charAt(0).toUpperCase()}
                                            </div>
                                            <div>
                                                <p style={styles.tutorNombre}>{nombreTutor}</p>
                                                <p style={styles.comunidadNombre}>
                                                    📚 {c.comunidad?.nombre ?? "—"}
                                                </p>
                                            </div>
                                        </div>
                                        <span style={{ ...styles.badge, background: est.bg, color: est.color }}>
                                            {est.label}
                                        </span>
                                    </div>

                                    <div style={styles.detalles}>
                                        <div style={styles.detalleItem}>
                                            <span style={styles.detalleLabel}>Modalidad</span>
                                            <span style={styles.detalleValor}>{c.modalidad ?? "—"}</span>
                                        </div>
                                        <div style={styles.detalleItem}>
                                            <span style={styles.detalleLabel}>Duración</span>
                                            <span style={styles.detalleValor}>{c.duracion ?? "—"}</span>
                                        </div>
                                        <div style={styles.detalleItem}>
                                            <span style={styles.detalleLabel}>Tarifa</span>
                                            <span style={styles.detalleValor}>
                                                {c.tarifaAcordada ? `${Number(c.tarifaAcordada).toFixed(2)}€/h` : "—"}
                                            </span>
                                        </div>
                                        <div style={styles.detalleItem}>
                                            <span style={styles.detalleLabel}>Fecha inicio</span>
                                            <span style={styles.detalleValor}>
                                                {c.fechaInicio
                                                    ? new Date(c.fechaInicio).toLocaleDateString("es-ES")
                                                    : "—"}
                                            </span>
                                        </div>
                                    </div>

                                    <div style={styles.cardBottom}>
                                        {necesitaPago && (
                                            <button
                                                style={styles.btnPagar}
                                                onClick={() => window.location.href = c.paymentUrl}
                                            >
                                                💳 Pagar ahora
                                            </button>
                                        )}
                                        {c.estado === "PENDIENTE_APROBACION" && (
                                            <p style={styles.esperandoMsg}>
                                                ⏳ El tutor aún no ha respondido a tu solicitud.
                                            </p>
                                        )}
                                        {c.estado === "ACTIVA" && (
                                            <p style={styles.activaMsg}>✅ Contratación activa</p>
                                        )}
                                        {(c.estado === "RECHAZADA" || c.estado === "CANCELADA") && c.motivoCancelacion && (
                                            <p style={styles.motivoMsg}>Motivo: {c.motivoCancelacion}</p>
                                        )}
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </div>
            </div>
            <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
        </>
    );
}

const styles = {
    page: { background: "#f5f6fa", minHeight: "100vh", padding: "40px 20px 80px" },
    container: { maxWidth: 760, margin: "0 auto" },
    pageHeader: { marginBottom: 32 },
    title: { fontSize: 28, fontWeight: 800, color: "#1f2a4a", margin: "0 0 6px" },
    subtitle: { fontSize: 14, color: "#6b7280", margin: 0 },
    centerBox: { display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", minHeight: "50vh" },
    spinner: { width: 40, height: 40, border: "4px solid #eef2f7", borderTop: "4px solid #F2C18E", borderRadius: "50%", animation: "spin 0.9s linear infinite" },
    errorBox: { background: "#fef2f2", border: "1px solid #fecaca", borderRadius: 12, padding: "14px 18px", color: "#b91c1c", fontSize: 14, marginBottom: 24 },
    emptyBox: { background: "#fff", borderRadius: 20, padding: "60px 40px", textAlign: "center", boxShadow: "0 4px 16px rgba(0,0,0,0.06)" },
    list: { display: "flex", flexDirection: "column", gap: 16 },
    card: { background: "#fff", borderRadius: 18, padding: "22px 24px", boxShadow: "0 4px 16px rgba(0,0,0,0.06)", border: "1px solid #eef2f7" },
    cardTop: { display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 18 },
    tutorInfo: { display: "flex", alignItems: "center", gap: 12 },
    avatar: { width: 44, height: 44, borderRadius: "50%", background: "linear-gradient(135deg, #F2C18E, #e8a96e)", color: "#fff", display: "flex", alignItems: "center", justifyContent: "center", fontWeight: 800, fontSize: 18, flexShrink: 0 },
    tutorNombre: { fontWeight: 800, fontSize: 15, color: "#1f2a4a", margin: "0 0 2px" },
    comunidadNombre: { fontSize: 13, color: "#6b7280", margin: 0 },
    badge: { borderRadius: 999, padding: "5px 14px", fontSize: 12, fontWeight: 700, whiteSpace: "nowrap" },
    detalles: { display: "grid", gridTemplateColumns: "1fr 1fr", gap: "10px 24px", background: "#f9fafb", borderRadius: 12, padding: "14px 16px", marginBottom: 16 },
    detalleItem: { display: "flex", flexDirection: "column", gap: 2 },
    detalleLabel: { fontSize: 11, color: "#9ca3af", textTransform: "uppercase", letterSpacing: "0.04em" },
    detalleValor: { fontSize: 13, fontWeight: 700, color: "#374151" },
    cardBottom: { display: "flex", alignItems: "center" },
    btnPagar: { background: "#F2C18E", color: "#fff", border: "none", borderRadius: 12, padding: "10px 24px", fontWeight: 800, fontSize: 14, cursor: "pointer" },
    btnPrimary: { background: "#676F9D", color: "#fff", border: "none", borderRadius: 12, padding: "12px 28px", fontWeight: 800, fontSize: 14, cursor: "pointer" },
    esperandoMsg: { fontSize: 13, color: "#f59e0b", margin: 0, fontWeight: 600 },
    activaMsg: { fontSize: 13, color: "#15803d", margin: 0, fontWeight: 600 },
    motivoMsg: { fontSize: 13, color: "#b91c1c", margin: 0 },
};
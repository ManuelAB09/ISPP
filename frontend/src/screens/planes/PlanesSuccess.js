import { useEffect, useState } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import { subscriptionsApi } from "../../api/subscriptions.api";
import Header from "../../components/Header/Header";

const benefits = [
    { icon: "⚡", title: "Más límites y herramientas", desc: "Accede a funcionalidades sin restricciones" },
    { icon: "✨", title: "Mejor experiencia de uso", desc: "Interfaz optimizada y sin interrupciones" },
    { icon: "🚀", title: "Funcionalidades avanzadas", desc: "Todo el potencial de MeerKatters desbloqueado" },
    { icon: "🎯", title: "Soporte prioritario", desc: "Atención preferente cuando lo necesites" },
    { icon: "🚫", title: "Sin publicidad", desc: "Disfruta sin distracciones" },
];

export default function PlanesSuccess() {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const [estado, setEstado] = useState("verificando");

    useEffect(() => {
        const sessionId = searchParams.get("session_id");
        if (!sessionId) {
            navigate("/planes");
            return;
        }
        subscriptionsApi.verifySession(sessionId)
            .then(() => setEstado("ok"))
            .catch(() => setEstado("error"));
    }, []);

    return (
        <>
            <Header page={"planes"} />
            <div style={styles.page}>

                {/* ── Verificando ── */}
                {estado === "verificando" && (
                    <div style={styles.centerBox}>
                        <div style={styles.spinnerWrap}>
                            <div style={styles.spinner} />
                        </div>
                        <h2 style={styles.verifyTitle}>Verificando tu pago...</h2>
                        <p style={styles.verifySubtitle}>Estamos confirmando tu suscripción con Stripe</p>
                    </div>
                )}

                {/* ── Error ── */}
                {estado === "error" && (
                    <div style={styles.centerBox}>
                        <div style={{ ...styles.iconCircle, background: "#fee2e2" }}>
                            <span style={{ fontSize: 40 }}>❌</span>
                        </div>
                        <h2 style={{ ...styles.verifyTitle, color: "#b91c1c" }}>Algo salió mal</h2>
                        <p style={styles.verifySubtitle}>
                            No pudimos activar tu suscripción. Por favor contacta a soporte o inténtalo de nuevo.
                        </p>
                        <div style={styles.errorBtns}>
                            <button style={styles.btnSecondary} onClick={() => navigate("/planes/pasarela")}>
                                Reintentar
                            </button>
                            <button style={styles.btnMuted} onClick={() => navigate("/planes")}>
                                Volver a planes
                            </button>
                        </div>
                    </div>
                )}

                {/* ── Éxito ── */}
                {estado === "ok" && (
                    <div style={styles.successWrap}>

                        {/* Hero */}
                        <div style={styles.hero}>
                            <div style={styles.confettiRow}>
                                {["🎉", "⭐", "✨", "🎊", "💎"].map((e, i) => (
                                    <span key={i} style={{ ...styles.confetti, animationDelay: `${i * 0.12}s` }}>{e}</span>
                                ))}
                            </div>
                            <div style={styles.premiumBadge}>PREMIUM</div>
                            <h1 style={styles.heroTitle}>¡Bienvenido a Premium!</h1>
                            <p style={styles.heroSub}>
                                Tu suscripción está activa. Ahora tienes acceso completo a todas las funcionalidades avanzadas de MeerKatters.
                            </p>
                        </div>

                        {/* Content grid */}
                        <div style={styles.grid}>

                            {/* Resumen de pago */}
                            <div style={styles.card}>
                                <div style={styles.cardHeader}>
                                    <span style={styles.cardIcon}>🧾</span>
                                    <h3 style={styles.cardTitle}>Resumen del pago</h3>
                                </div>
                                <div style={styles.summaryRow}>
                                    <span style={styles.summaryLabel}>Plan</span>
                                    <span style={styles.summaryValue}>Premium</span>
                                </div>
                                <div style={styles.divider} />
                                <div style={styles.summaryRow}>
                                    <span style={styles.summaryLabel}>Estado</span>
                                    <span style={{ ...styles.summaryValue, ...styles.statusBadge }}>✓ Activo</span>
                                </div>
                                <div style={styles.divider} />
                                <div style={styles.summaryRow}>
                                    <span style={styles.summaryLabel}>Procesado por</span>
                                    <span style={styles.summaryValue}>Stripe 🔒</span>
                                </div>
                                <div style={styles.secureNote}>
                                    <span>🔐</span>
                                    <span>Pago procesado con encriptación SSL de 256 bits</span>
                                </div>
                            </div>

                            {/* Beneficios */}
                            <div style={styles.card}>
                                <div style={styles.cardHeader}>
                                    <span style={styles.cardIcon}>💎</span>
                                    <h3 style={styles.cardTitle}>Tus beneficios Premium</h3>
                                </div>
                                <ul style={styles.benefitsList}>
                                    {benefits.map((b, i) => (
                                        <li key={i} style={styles.benefitItem}>
                                            <div style={styles.benefitIconWrap}>
                                                <span style={styles.benefitIcon}>{b.icon}</span>
                                            </div>
                                            <div>
                                                <div style={styles.benefitTitle}>{b.title}</div>
                                                <div style={styles.benefitDesc}>{b.desc}</div>
                                            </div>
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        </div>

                        {/* CTA */}
                        <div style={styles.ctaRow}>
                            <button style={styles.btnPrimary} onClick={() => navigate("/")}>
                                Ir al inicio →
                            </button>
                            <button style={styles.btnSecondary} onClick={() => navigate("/planes")}>
                                Ver mi plan
                            </button>
                        </div>

                    </div>
                )}
            </div>

            <style>{`
        @keyframes spin {
          to { transform: rotate(360deg); }
        }
        @keyframes popIn {
          0%   { opacity: 0; transform: scale(0.5) translateY(10px); }
          70%  { transform: scale(1.15) translateY(-2px); }
          100% { opacity: 1; transform: scale(1) translateY(0); }
        }
        @keyframes fadeUp {
          from { opacity: 0; transform: translateY(24px); }
          to   { opacity: 1; transform: translateY(0); }
        }
      `}</style>
        </>
    );
}

const styles = {
    page: {
        background: "#f5f6fa",
        minHeight: "100vh",
        display: "flex",
        alignItems: "flex-start",
        justifyContent: "center",
        padding: "48px 20px 80px",
    },
    centerBox: {
        background: "#fff",
        borderRadius: 20,
        padding: "56px 40px",
        maxWidth: 480,
        width: "100%",
        textAlign: "center",
        boxShadow: "0 10px 40px rgba(17,24,39,0.08)",
        animation: "fadeUp 0.4s ease both",
    },
    spinnerWrap: {
        display: "flex",
        justifyContent: "center",
        marginBottom: 24,
    },
    spinner: {
        width: 48,
        height: 48,
        border: "4px solid #eef2f7",
        borderTop: "4px solid #F2C18E",
        borderRadius: "50%",
        animation: "spin 0.9s linear infinite",
    },
    iconCircle: {
        width: 80,
        height: 80,
        borderRadius: "50%",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        margin: "0 auto 20px",
    },
    verifyTitle: {
        fontFamily: "'spacegrotesk', sans-serif",
        fontSize: 22,
        fontWeight: 800,
        color: "#1f2a4a",
        margin: "0 0 10px",
    },
    verifySubtitle: {
        fontSize: 14,
        color: "#6b7280",
        margin: "0 0 24px",
        lineHeight: 1.6,
    },
    errorBtns: {
        display: "flex",
        gap: 10,
        justifyContent: "center",
        flexWrap: "wrap",
    },

    /* Success layout */
    successWrap: {
        maxWidth: 860,
        width: "100%",
        animation: "fadeUp 0.5s ease both",
    },
    hero: {
        textAlign: "center",
        marginBottom: 32,
    },
    confettiRow: {
        display: "flex",
        justifyContent: "center",
        gap: 10,
        marginBottom: 16,
    },
    confetti: {
        fontSize: 28,
        display: "inline-block",
        animation: "popIn 0.5s ease both",
    },
    premiumBadge: {
        display: "inline-block",
        background: "linear-gradient(135deg, #F2C18E, #e8a96e)",
        color: "#fff",
        fontWeight: 800,
        fontSize: 11,
        letterSpacing: "0.08em",
        padding: "5px 16px",
        borderRadius: 999,
        marginBottom: 14,
    },
    heroTitle: {
        fontFamily: "'spacegrotesk', sans-serif",
        fontSize: 42,
        fontWeight: 800,
        color: "#1f2a4a",
        margin: "0 0 12px",
    },
    heroSub: {
        fontSize: 15,
        color: "#6b7280",
        maxWidth: 540,
        margin: "0 auto",
        lineHeight: 1.7,
    },

    /* Grid */
    grid: {
        display: "grid",
        gridTemplateColumns: "1fr 1fr",
        gap: 18,
        marginBottom: 28,
    },
    card: {
        background: "#fff",
        borderRadius: 18,
        padding: "22px 22px 18px",
        boxShadow: "0 10px 30px rgba(17,24,39,0.07)",
        border: "1px solid #eef2f7",
    },
    cardHeader: {
        display: "flex",
        alignItems: "center",
        gap: 10,
        marginBottom: 18,
    },
    cardIcon: {
        fontSize: 22,
    },
    cardTitle: {
        fontFamily: "'spacegrotesk', sans-serif",
        fontWeight: 800,
        fontSize: 15,
        color: "#1f2a4a",
        margin: 0,
    },

    /* Summary */
    summaryRow: {
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
        padding: "10px 0",
    },
    summaryLabel: {
        fontSize: 13,
        color: "#6b7280",
    },
    summaryValue: {
        fontSize: 13,
        fontWeight: 700,
        color: "#111827",
    },
    statusBadge: {
        background: "#f0fdf4",
        color: "#15803d",
        border: "1px solid #dcfce7",
        borderRadius: 999,
        padding: "3px 10px",
        fontSize: 12,
    },
    divider: {
        height: 1,
        background: "#f3f4f6",
    },
    secureNote: {
        display: "flex",
        alignItems: "center",
        gap: 6,
        marginTop: 16,
        background: "#f9fafb",
        borderRadius: 10,
        padding: "8px 12px",
        fontSize: 11,
        color: "#9ca3af",
    },

    /* Benefits */
    benefitsList: {
        listStyle: "none",
        margin: 0,
        padding: 0,
        display: "flex",
        flexDirection: "column",
        gap: 12,
    },
    benefitItem: {
        display: "flex",
        alignItems: "flex-start",
        gap: 12,
    },
    benefitIconWrap: {
        width: 36,
        height: 36,
        borderRadius: 10,
        background: "#fff7ed",
        border: "1px solid #fed7aa",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        flexShrink: 0,
    },
    benefitIcon: {
        fontSize: 16,
    },
    benefitTitle: {
        fontSize: 13,
        fontWeight: 700,
        color: "#111827",
        marginBottom: 2,
    },
    benefitDesc: {
        fontSize: 11,
        color: "#9ca3af",
        lineHeight: 1.4,
    },

    /* CTA */
    ctaRow: {
        display: "flex",
        gap: 12,
        justifyContent: "center",
        flexWrap: "wrap",
    },
    btnPrimary: {
        background: "#F2C18E",
        color: "#fff",
        border: "none",
        borderRadius: 12,
        padding: "12px 28px",
        fontWeight: 800,
        fontSize: 14,
        cursor: "pointer",
    },
    btnSecondary: {
        background: "#676F9D",
        color: "#fff",
        border: "none",
        borderRadius: 12,
        padding: "12px 28px",
        fontWeight: 800,
        fontSize: 14,
        cursor: "pointer",
    },
    btnMuted: {
        background: "#eef2f7",
        color: "#6b7280",
        border: "none",
        borderRadius: 12,
        padding: "12px 28px",
        fontWeight: 800,
        fontSize: 14,
        cursor: "pointer",
    },
};
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { subscriptionsApi } from "../../api/subscriptions.api";
import Header from "../../components/Header/Header";
import PageHeader from "../../components/PageHeader";
import CheckoutModal from "../../components/plans/CheckoutModal";
import "./PlansScreen.css";

const DEFAULT_PLANS = [
  {
    id: 1,
    nombre: "Gratuito",
    descripcion: "Acceso básico",
    caracteristicas: ["Comunidades y eventos", "Funcionalidades esenciales", "Límites estándar"]
  },
  {
    id: 2,
    nombre: "PREMIUM",
    descripcion: "Funciones avanzadas desbloqueadas",
    caracteristicas: ["Más límites y herramientas", "Mejor experiencia de uso", "Acceso a funcionalidades avanzadas", "Soporte prioritario", "Sin publicidad"]
  }
];

export default function PlansScreen() {
  const navigate = useNavigate();
  const [plans, setPlans] = useState(DEFAULT_PLANS);
  const [myPlan, setMyPlan] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [successMessage] = useState("");
  const [showCheckout, setShowCheckout] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const isPremium = myPlan?.activa && myPlan?.plan === "PREMIUM";
  const hasInstitutionPlan = myPlan?.planCorporativoActivo && myPlan?.institutionNombre;

  useEffect(() => {
    const loadData = async () => {
      try {
        setLoading(true);

        // Intentar cargar planes del backend
        try {
          const plansResponse = await subscriptionsApi.listPlans();
          const plansData = Array.isArray(plansResponse) ? plansResponse : (plansResponse?.data || DEFAULT_PLANS);
          setPlans(plansData);
        } catch (err) {
          console.log("Usando planes por defecto");
          setPlans(DEFAULT_PLANS);
        }

        // Intentar cargar suscripción actual
        try {
          const myPlanResponse = await subscriptionsApi.getMySubscription();
          setMyPlan(myPlanResponse || null);
        } catch (err) {
          if (err?.status === 404) {
            console.log("Sin suscripción activa");
          } else {
            console.log("No se pudo cargar suscripción");
          }
          setMyPlan(null);
        }

        setLoading(false);
      } catch (err) {
        console.error("Error crítico:", err);
        setLoading(false);
      }
    };

    loadData();
  }, []);

  const handleSubscribe = async (period) => {
    setSubmitting(true);
    setError("");

    try {
      const res = await subscriptionsApi.subscribe(period);
      setMyPlan(res || null);
      setShowCheckout(false);
      alert("¡Suscripción Premium activada!");
    } catch (e) {
      if (e?.status === 403) {
        setError("Por favor, inicia sesión para continuar");
      } else {
        setError(e?.message || "No se pudo iniciar la suscripción");
      }
    } finally {
      setSubmitting(false);
    }
  };

  //MOCK: simular suscripción exitosa 
  /*
    const handleSubscribe = async (period) => {
    setSubmitting(true);
    setError("");
    setSuccessMessage("");
  
      try {
        const isYearly = period === "anual";
        const days = isYearly ? 365 : 30;
  
        // Mock: simular suscripción exitosa
        const mockPlan = {
          plan: "PREMIUM",
          activa: true,
          periodo: isYearly ? "ANUAL" : "MENSUAL",
          fechaFin: new Date(Date.now() + days * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
        };
        
        // Simular delay de red
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        setMyPlan(mockPlan);
        setShowCheckout(false);
        setSuccessMessage(
          isYearly
            ? "¡Suscripción Premium anual activada exitosamente!"
            : "¡Suscripción Premium mensual activada exitosamente!"
        );
        
        // Desaparecer mensaje después de 3 segundos
        setTimeout(() => setSuccessMessage(""), 5000);
      } catch (e) {
        setError(e?.message || "No se pudo iniciar la suscripción");
      } finally {
        setSubmitting(false);
      }
    };
  */

  //MOCK: simular cancelación exitosa
  /*  
    const handleCancel = async () => {
      setSubmitting(true);
      setError("");
      setSuccessMessage("");
  
      try {
        // Mock: simular cancelación exitosa
        // Simular delay de red
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        setMyPlan(null);
        setSuccessMessage("Suscripción cancelada exitosamente");
        
        // Desaparecer mensaje después de 5 segundos
        setTimeout(() => setSuccessMessage(""), 5000);
      } catch (e) {
        setError(e?.message || "No se pudo cancelar la suscripción");
      } finally {
        setSubmitting(false);
      }
    };
  
    if (loading) {
      return (
        <>
          <Header page={'planes'} />
          <div className="plansPage">
            <p style={{ textAlign: 'center', padding: '40px' }}>Cargando...</p>
          </div>
        </>
      );
    }
  */
  return (
    <>
      <Header page={'planes'} />
      <div className="plansPage">
        <div className="header">
          <PageHeader 
            title="Planes de Suscripción"
            subtitle="Elige el plan perfecto para desbloquear todas las funcionalidades y sacar el máximo provecho de MeerKatters"
          />
        </div>

        <main className="plansContent">
          <section className="plansSection">
            <div className="sectionHead">
              <h2>Pásate a Premium</h2>
              <p>Desbloquea funcionalidades avanzadas y mejora tu experiencia en MeerKatters.</p>
            </div>

            {error && <div className="plansError">⚠️ {error}</div>}

            {successMessage && <div className="plansSuccess">✓ {successMessage}</div>}

            {hasInstitutionPlan && (
              <div className="plansInfo" style={{
                background: '#e8f4fd',
                border: '1px solid #b3d7f2',
                borderRadius: '8px',
                padding: '12px 16px',
                marginBottom: '16px',
                color: '#1a5276',
                fontSize: '14px'
              }}>
                ℹ️ Tienes un plan institucional activo a través de <b>{myPlan.institutionNombre}</b>.
                No puedes suscribirte a un plan individual Premium mientras tu plan institucional esté vigente.
              </div>
            )}

            <div className="cardsGrid">
              {plans && plans.length > 0 ? (
                plans.map((plan) => {
                  const isPremiumPlan = plan.nombre === "PREMIUM" || plan.tipo === "PREMIUM";
                  const isCurrentPlan = myPlan?.plan === plan.nombre;

                  return (
                    <article
                      key={plan.id || plan.nombre}
                      className={`planCard ${isPremiumPlan ? "planCard--premium" : "planCard--free"}`}
                    >
                      {isPremiumPlan && <div className="badgeRecommended">RECOMENDADO</div>}

                      <div className="planCardTop">
                        <div>
                          <div className="planName">{plan.nombre}</div>
                          <div className="planSub">{plan.descripcion || "Acceso básico"}</div>
                        </div>
                        <div className={isPremiumPlan ? "planPricePremium" : "planPrice"}>
                          {plan.precio === 0 ? (
                            "GRATIS"
                          ) : plan.precio ? (
                            `$${plan.precio}`
                          ) : isPremiumPlan ? (
                            <span className="planPriceStack">
                              <span className="planPriceMain">
                                2.99€/mes + IVA: {(parseFloat(2.99) * 1.21).toFixed(2)}€
                              </span> <span className="planPriceSub">25.99€/año + IVA: {(parseFloat(25.99) * 1.21).toFixed(2)}€</span>
                            </span>
                          ) : (
                            "GRATIS"
                          )}
                        </div>
                      </div>

                      <ul className={`planFeatures ${isPremiumPlan ? "planFeatures--checks" : ""}`}>
                        {Array.isArray(plan.caracteristicas) ? (
                          plan.caracteristicas.map((item, idx) => <li key={idx}>{item}</li>)
                        ) : typeof plan.caracteristicas === "string" ? (
                          plan.caracteristicas.split(",").map((item, idx) => <li key={idx}>{item.trim()}</li>)
                        ) : (
                          <li>Características disponibles</li>
                        )}
                      </ul>

                      {isCurrentPlan ? (
                        <button className="btn btn--muted" disabled>
                          Plan actual
                        </button>
                      ) : isPremiumPlan ? (
                        <button
                          className="btn btn--primary"
                          disabled={isPremium || hasInstitutionPlan || submitting}
                          onClick={() => navigate("/planes/pasarela")}
                          title={hasInstitutionPlan ? "No disponible con plan institucional activo" : ""}
                        >
                          {isPremium ? "Ya eres Premium" : hasInstitutionPlan ? "No disponible" : "Mejorar a Premium"}
                        </button>
                      ) : (
                        <button className="btn btn--muted" disabled>
                          {isPremium ? "Incluido" : "Plan actual"}
                        </button>
                      )}
                    </article>
                  );
                })
              ) : (
                <div>No hay planes disponibles</div>
              )}
            </div>
          </section>

          {/* ── Institutional plan active ─────────────── */}
          {hasInstitutionPlan && (
            <section className="instBannerSection">
              <div className="instBannerContent">
                <div className="instBannerLeft">
                  <div className="instBannerIcon">🏛️</div>
                  <div>
                    <h2 className="instBannerTitle">Plan Institucional activo</h2>
                    <p className="instBannerDesc">
                      Tienes un plan institucional{myPlan.planCorporativo ? <> <b>{myPlan.planCorporativo}</b></> : ''} a través de <b>{myPlan.institutionNombre}</b>.
                      Disfruta de todas las ventajas incluidas en tu plan corporativo.
                    </p>
                    <div className="instBannerFeatures">
                      <span>✅ Plan activo</span>
                      <span>🏫 {myPlan.institutionNombre}</span>
                      {myPlan.planCorporativo && <span>📋 {myPlan.planCorporativo}</span>}
                    </div>
                  </div>
                </div>
              </div>
            </section>
          )}

          {/* ── Institutional plans banner ─────────────── */}
          {!hasInstitutionPlan && (
            <section className="instBannerSection">
              <div className="instBannerContent">
                <div className="instBannerLeft">
                  <div className="instBannerIcon">🏛️</div>
                  <div>
                    <h2 className="instBannerTitle">¿Eres una institución educativa?</h2>
                    <p className="instBannerDesc">
                      Ofrecemos planes especiales para academias, universidades y centros
                      educativos, con gestión de grupos, múltiples administradores y
                      estadísticas avanzadas. También disponemos de precios reducidos para
                      centros públicos y concertados.
                    </p>
                    <div className="instBannerFeatures">
                      <span>👥 Múltiples admins</span>
                      <span>📊 Estadísticas avanzadas</span>
                      <span>🏫 Gestión de grupos</span>
                      <span>💜 Precios especiales</span>
                    </div>
                  </div>
                </div>
                <button
                  className="instBannerBtn"
                  onClick={() => navigate("/planes/instituciones")}
                >
                  Ver planes institucionales →
                </button>
              </div>
            </section>
          )}

          {!loading && (
            <section className="plansStatus">
              <h3>Tu suscripción</h3>

              {hasInstitutionPlan ? (
                <div className="statusBox">
                  <div>
                    <b>Plan:</b> Institucional{myPlan.planCorporativo ? ` ${myPlan.planCorporativo}` : ''}
                  </div>
                  <div>
                    <b>Institución:</b> {myPlan.institutionNombre}
                  </div>
                  <div>
                    <b>Estado:</b> Activo
                  </div>
                </div>
              ) : isPremium ? (
                <div className="statusBox">
                  <div>
                    <b>Plan:</b> {myPlan?.plan} {myPlan?.periodo === "ANUAL" ? "(anual)" : "(mensual)"}
                  </div>
                  <div>
                    <b>Activa:</b> {String(myPlan?.activa) ? "Sí" : "No"}
                  </div>
                  {myPlan?.fechaFin && (
                    <div>
                      <b>Fecha de fin de la suscripción:</b> {myPlan.fechaFin}
                    </div>
                  )}
                </div>
              ) : (
                <div className="statusBox">No tienes suscripción activa.</div>
              )}
            </section>
          )}
        </main>

        <CheckoutModal
          open={showCheckout}
          plan="PREMIUM"
          onClose={() => !submitting && setShowCheckout(false)}
          onConfirm={(period) => handleSubscribe(period)}
          loading={submitting}
        />
      </div>
    </>
  );
}
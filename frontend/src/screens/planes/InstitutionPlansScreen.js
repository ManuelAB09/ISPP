import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Header from "../../components/Header/Header";
import PageHeader from "../../components/PageHeader";
import InstitutionPlanModal from "./InstitutionPlanModal";
import "./InstitutionPlansScreen.css";

const INSTITUTION_PLANS = [
  {
    id: "BASICO",
    nombre: "Instituciones Academias",
    descripcion: "Para academias y centros de tamaño medio",
    precio: "120€/mes",
    precioAnual: "1200€/año",
    maxUsuarios: 500,
    features: [
      "30 comunidades activas",
      "500 aforo máx",
      "20 profesores por comunidad",
    ],
    destacado: false,
    requiereEligibilidad: false,
  },
  {
    id: "ESTANDAR",
    nombre: "Instituciones Colegios",
    descripcion: "Pensado para colegios con alta actividad",
    precio: "340€/mes",
    precioAnual: "3400€/año",
    maxUsuarios: 2000,
    features: [
      "100 comunidades activas",
      "2000 aforo máx",
      "75 profesores por comunidad",
    ],
    destacado: true,
    requiereEligibilidad: false,
  },
  {
    id: "PREMIUM",
    nombre: "Instituciones Universidades",
    descripcion: "Máxima capacidad para universidades",
    precio: "950€/mes",
    precioAnual: "9500€/año",
    maxUsuarios: 10000,
    features: [
      "Comunidades ilimitadas",
      "10000 aforo máx",
      "300 profesores por comunidad",
    ],
    destacado: false,
    requiereEligibilidad: false,
  },
];

export default function InstitutionPlansScreen() {
  const navigate = useNavigate();
  const [selectedPlan, setSelectedPlan] = useState(null);
  const [showModal, setShowModal] = useState(false);

  const handleSelectPlan = (plan) => {
    setSelectedPlan(plan);
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setSelectedPlan(null);
  };

  return (
    <>
      <Header page={"planes"} />
      <div className="instPlansPage">
        {/* ── Hero Header ─────────────────────────────── */}
        <div className="instPlansHeader">
          <PageHeader 
            title="Planes para Instituciones"
            subtitle="Escala desde academias hasta universidades con límites de comunidades, aforo y profesorado adaptados"
          />
          <button
            className="instBtnBack"
            onClick={() => navigate("/planes")}
          >
            ← Volver a planes personales
          </button>
        </div>

        <main className="instPlansContent">
          {/* ── Feature highlights ───────────────────── */}
          <section className="instFeaturesHighlight">
            <div className="instFeatureItem">
              <div className="instFeatureIcon">👥</div>
              <div>
                <strong>Más profesorado</strong>
                <p>Amplía docentes por comunidad según tu plan</p>
              </div>
            </div>
            <div className="instFeatureItem">
              <div className="instFeatureIcon">📊</div>
              <div>
                <strong>Mayor aforo</strong>
                <p>Desde 500 hasta 10000 plazas según plan</p>
              </div>
            </div>
            <div className="instFeatureItem">
              <div className="instFeatureIcon">🏫</div>
              <div>
                <strong>Comunidades escalables</strong>
                <p>Desde 30 comunidades hasta ilimitadas</p>
              </div>
            </div>
            <div className="instFeatureItem">
              <div className="instFeatureIcon">✅</div>
              <div>
                <strong>Plan anual disponible</strong>
                <p>Ahorra con facturación anual en cada categoría</p>
              </div>
            </div>
          </section>

          {/* ── Standard institutional plans ─────────── */}
          <section className="instPlansSection">
            <div className="instSectionHead">
              <h2>Planes Institucionales</h2>
              <p>
                Elige el plan según el tamaño de tu institución y las necesidades
                de capacidad de tus comunidades.
              </p>
            </div>

            <div className="instCardsGrid instCardsGrid--3">
              {INSTITUTION_PLANS.map((plan) => (
                <PlanCard
                  key={plan.id}
                  plan={plan}
                  onSelect={handleSelectPlan}
                />
              ))}
            </div>
          </section>
        </main>

        {/* ── Modal ────────────────────────────────────── */}
        {showModal && selectedPlan && (
          <InstitutionPlanModal
            plan={selectedPlan}
            onClose={handleCloseModal}
          />
        )}
      </div>
    </>
  );
}

/* ── Plan Card ──────────────────────────────────────── */
function PlanCard({ plan, onSelect, special = false }) {
  return (
    <article
      className={[
        "instPlanCard",
        plan.destacado ? "instPlanCard--destacado" : "",
        special ? "instPlanCard--special" : "",
      ]
        .filter(Boolean)
        .join(" ")}
    >
      {plan.destacado && (
        <div className="instBadgeRecomendado">RECOMENDADO</div>
      )}
      {plan.descuento && (
        <div className="instBadgeDescuento">{plan.descuento}</div>
      )}

      <div className="instPlanCardTop">
        <div>
          <div className="instPlanName">{plan.nombre}</div>
          <div className="instPlanSub">{plan.descripcion}</div>
        </div>
        <div className={plan.destacado || special ? "instPlanPriceAccent" : "instPlanPrice"}>
          <span className="instPlanPriceMain">{plan.precio}</span>
          <span className="instPlanPriceSub">{plan.precioAnual}</span>
        </div>
      </div>

      <div className="instPlanCapacity">
        {plan.maxUsuarios
          ? `Aforo máximo ${plan.maxUsuarios}`
          : "Aforo ilimitado"}
      </div>

      <ul className="instPlanFeatures">
        {plan.features.map((f, i) => (
          <li key={i}>{f}</li>
        ))}
      </ul>

      {plan.eligibilidadInfo && (
        <div className="instEligibilityBox">
          <span className="instEligibilityBoxIcon">🔒</span>
          <span>{plan.eligibilidadInfo}</span>
        </div>
      )}

      <button
        className={`instBtn ${plan.destacado ? "instBtn--primary" : plan.requiereEligibilidad ? "instBtn--special" : "instBtn--secondary"}`}
        onClick={() => onSelect(plan)}
      >
        {plan.requiereEligibilidad ? "Verificar elegibilidad" : "Contratar plan"}
      </button>
    </article>
  );
}

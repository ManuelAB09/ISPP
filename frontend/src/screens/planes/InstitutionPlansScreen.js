import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Header from "../../components/Header/Header";
import InstitutionPlanModal from "./InstitutionPlanModal";
import "./InstitutionPlansScreen.css";

const INSTITUTION_PLANS = [
  {
    id: "BASICO",
    nombre: "Institucional Básico",
    descripcion: "Para academias e instituciones pequeñas",
    precio: "49,99€/mes",
    precioAnual: "499,99€/año",
    maxUsuarios: 50,
    features: [
      "Hasta 50 usuarios activos",
      "Gestión de grupos de estudio",
      "Perfil institucional verificado",
      "Estadísticas básicas de uso",
      "Soporte por email",
    ],
    destacado: false,
    requiereEligibilidad: false,
  },
  {
    id: "ESTANDAR",
    nombre: "Institucional Estándar",
    descripcion: "Para academias en pleno crecimiento",
    precio: "99,99€/mes",
    precioAnual: "999,99€/año",
    maxUsuarios: 200,
    features: [
      "Hasta 200 usuarios activos",
      "Múltiples administradores",
      "Gestión avanzada de grupos",
      "Estadísticas detalladas de uso",
      "Eventos y comunidades sin límite",
      "Soporte prioritario",
    ],
    destacado: true,
    requiereEligibilidad: false,
  },
  {
    id: "PREMIUM",
    nombre: "Institucional Premium",
    descripcion: "La solución completa para grandes instituciones",
    precio: "199,99€/mes",
    precioAnual: "1999,99€/año",
    maxUsuarios: null,
    features: [
      "Usuarios ilimitados",
      "Administradores ilimitados",
      "Estadísticas avanzadas y exportación",
      "Personalización de marca",
      "Gestor de cuenta dedicado",
      "SLA garantizado 99,9%",
      "API de integración",
    ],
    destacado: false,
    requiereEligibilidad: false,
  },
];

const SPECIAL_PLANS = [
  {
    id: "REDUCIDO_PUBLICA",
    nombre: "Educación Pública",
    descripcion: "Precio especial para centros educativos públicos",
    precio: "29,99€/mes",
    precioAnual: "299,99€/año",
    maxUsuarios: 300,
    descuento: "50% DESC.",
    features: [
      "Hasta 300 usuarios activos",
      "Múltiples administradores",
      "Gestión de grupos de estudio",
      "Estadísticas de uso detalladas",
      "Soporte prioritario",
    ],
    eligibilidadInfo:
      "Disponible para universidades, institutos y colegios públicos. La validación se realiza a través del dominio de email institucional.",
    requiereEligibilidad: true,
    tipoEligibilidad: "publica",
  },
  {
    id: "REDUCIDO_PRIVADA",
    nombre: "Centro Concertado",
    descripcion: "Condiciones especiales para centros privados concertados",
    precio: "69,99€/mes",
    precioAnual: "699,99€/año",
    maxUsuarios: 200,
    descuento: "30% DESC.",
    features: [
      "Hasta 200 usuarios activos",
      "Múltiples administradores",
      "Gestión avanzada de grupos",
      "Estadísticas de uso detalladas",
      "Soporte prioritario",
    ],
    eligibilidadInfo:
      "Disponible para centros privados concertados con financiación pública. Requiere documentación acreditativa.",
    requiereEligibilidad: true,
    tipoEligibilidad: "privada",
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
          <div className="instPlansHeaderTitle">
            <span className="instLine" />
            <h1>Planes para Instituciones</h1>
            <span className="instLine" />
          </div>
          <p className="instPlansHeaderDesc">
            Soluciones adaptadas para academias, universidades y centros
            educativos. Gestión avanzada de grupos, múltiples administradores y
            estadísticas detalladas para sacar el máximo partido a MeerKatters.
          </p>
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
                <strong>Múltiples administradores</strong>
                <p>Delegar la gestión entre varios responsables</p>
              </div>
            </div>
            <div className="instFeatureItem">
              <div className="instFeatureIcon">📊</div>
              <div>
                <strong>Estadísticas avanzadas</strong>
                <p>Seguimiento de actividad de todos los usuarios</p>
              </div>
            </div>
            <div className="instFeatureItem">
              <div className="instFeatureIcon">🏫</div>
              <div>
                <strong>Gestión de grupos</strong>
                <p>Organiza usuarios en grupos y clases</p>
              </div>
            </div>
            <div className="instFeatureItem">
              <div className="instFeatureIcon">✅</div>
              <div>
                <strong>Perfil verificado</strong>
                <p>Distintivo oficial para tu institución</p>
              </div>
            </div>
          </section>

          {/* ── Standard institutional plans ─────────── */}
          <section className="instPlansSection">
            <div className="instSectionHead">
              <h2>Planes Institucionales</h2>
              <p>
                Elige el plan que mejor se adapte al tamaño y necesidades de tu
                institución.
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

          {/* ── Special / reduced plans ───────────────── */}
          <section className="instPlansSection instSpecialSection">
            <div className="instSectionHead">
              <h2>Planes con Condiciones Especiales</h2>
              <p>
                Precios reducidos para centros educativos públicos y privados
                concertados. Sujetos a validación de elegibilidad.
              </p>
            </div>

            <div className="instEligibilityNotice">
              <span className="instEligibilityIcon">ℹ️</span>
              <span>
                Estos planes requieren validación previa. Comprobaremos la
                elegibilidad a través del dominio de email institucional o
                documentación acreditativa antes de activar el plan.
              </span>
            </div>

            <div className="instCardsGrid instCardsGrid--2">
              {SPECIAL_PLANS.map((plan) => (
                <PlanCard
                  key={plan.id}
                  plan={plan}
                  onSelect={handleSelectPlan}
                  special
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
          ? `Hasta ${plan.maxUsuarios} usuarios`
          : "Usuarios ilimitados"}
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

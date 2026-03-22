import { Elements, PaymentElement, useElements, useStripe } from "@stripe/react-stripe-js";
import { loadStripe } from "@stripe/stripe-js";
import { useState } from "react";
import { institutionsApi } from "../../api/institutions.api";
import "./InstitutionPlanModal.css";

const stripePromise = loadStripe(process.env.REACT_APP_STRIPE_PUBLIC_KEY);

/* ── Helpers ──────────────────────────────────────── */
const getUserEmail = () => {
  try {
    const profile = JSON.parse(localStorage.getItem("userProfile") || "{}");
    return profile.email || "";
  } catch {
    return "";
  }
};

const extractDomain = (email) => {
  const parts = (email || "").split("@");
  return parts.length === 2 ? parts[1].toLowerCase().trim() : "";
};

const STEPS_WITH_ELIGIBILITY = ["eligibility", "details", "config", "confirm", "payment"];
const STEPS_WITHOUT_ELIGIBILITY = ["details", "config", "confirm", "payment"];

const STEP_LABELS = {
  eligibility: "Elegibilidad",
  details: "Institución",
  config: "Configuración",
  confirm: "Confirmar",
  payment: "Pago",
};

const DURACION_OPTIONS = [
  { value: 1, label: "1 mes" },
  { value: 3, label: "3 meses" },
  { value: 6, label: "6 meses" },
  { value: 12, label: "1 año (recomendado)" },
];

const persistManagedInstitution = (institution) => {
  if (!institution?.id) return;

  try {
    const raw = localStorage.getItem("managedInstitutions");
    const current = raw ? JSON.parse(raw) : [];
    const safeCurrent = Array.isArray(current) ? current : [];

    const sanitized = {
      id: institution.id,
      nombre: institution.nombre || "Institución",
      dominioEmail: institution.dominioEmail || "",
    };

    const withoutCurrent = safeCurrent.filter((item) => String(item?.id) !== String(institution.id));
    localStorage.setItem("managedInstitutions", JSON.stringify([sanitized, ...withoutCurrent]));
  } catch (error) {
    console.warn("No se pudo guardar la institución administrada en localStorage", error);
  }
};

/* ── Component ────────────────────────────────────── */
export default function InstitutionPlanModal({ plan, onClose }) {
  const steps = plan.requiereEligibilidad
    ? STEPS_WITH_ELIGIBILITY
    : STEPS_WITHOUT_ELIGIBILITY;

  const [stepIndex, setStepIndex] = useState(0);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  /* Eligibility state */
  const userEmail = getUserEmail();
  const inferredDomain = extractDomain(userEmail);
  const [dominioElegibilidad, setDominioElegibilidad] = useState(inferredDomain);
  const [documentacion, setDocumentacion] = useState("");

  /* Institution details state */
  const [nombre, setNombre] = useState("");
  const [descripcion, setDescripcion] = useState("");
  const [emailContacto, setEmailContacto] = useState(userEmail);
  const [telefono, setTelefono] = useState("");
  const [dominioEmail, setDominioEmail] = useState(inferredDomain);
  const [ubicacion, setUbicacion] = useState("");
  const [sitioweb, setSitioweb] = useState("");

  /* Plan config state */
  const maxUsers = plan.maxUsuarios || 500;
  const [numUsuarios, setNumUsuarios] = useState(Math.min(50, maxUsers));
  const [duracionMeses, setDuracionMeses] = useState(12);
  const [aceptarTerminos, setAceptarTerminos] = useState(false);

  /* Stripe Elements state */
  const [clientSecret, setClientSecret] = useState(null);
  const [institutionId, setInstitutionId] = useState(null);

  const currentStep = steps[stepIndex];
  const isPaymentStep = currentStep === "payment";
  const isConfirmStep = currentStep === "confirm";

  /* ── Validation ─────────────────────────── */
  const validateStep = () => {
    setError("");
    if (currentStep === "eligibility") {
      if (!documentacion.trim()) {
        setError("Debes adjuntar documentación para acreditar tu elegibilidad.");
        return false;
      }
      if (!dominioElegibilidad.trim()) {
        setError("Introduce el dominio de email de tu centro.");
        return false;
      }
    }
    if (currentStep === "details") {
      if (!nombre.trim()) {
        setError("El nombre de la institución es obligatorio.");
        return false;
      }
      if (!emailContacto.trim()) {
        setError("El email de contacto es obligatorio.");
        return false;
      }
      if (!dominioEmail.trim()) {
        setError("El dominio de email institucional es obligatorio.");
        return false;
      }
    }
    if (currentStep === "confirm") {
      if (!aceptarTerminos) {
        setError("Debes aceptar los términos y condiciones para continuar.");
        return false;
      }
    }
    return true;
  };

  /* ── Navigation ──────────────────────────── */
  const handleNext = async () => {
    if (!validateStep()) return;
    setError("");

    // When leaving confirm step, create institution + get PaymentIntent
    if (isConfirmStep) {
      setLoading(true);
      try {
        // 1. Create institution
        const institution = await institutionsApi.create({
          nombre: nombre.trim(),
          descripcion: descripcion.trim() || undefined,
          emailContacto: emailContacto.trim(),
          telefonoContacto: telefono.trim() || undefined,
          dominioEmail: dominioEmail.trim(),
          ubicacion: ubicacion.trim() || undefined,
          sitioweb: sitioweb.trim() || undefined,
        });

        setInstitutionId(institution.id);

        // 2. Create PaymentIntent for the corporate plan
        const intentRes = await institutionsApi.createPlanPaymentIntent(institution.id, {
          tipoPlan: plan.id,
          numUsuarios,
          duracionMeses,
          aceptarTerminos: true,
          periodo: duracionMeses >= 12 ? "anual" : "mensual",
          documentacionEligibilidad:
            plan.requiereEligibilidad && documentacion.trim()
              ? documentacion.trim()
              : undefined,
        });

        const data = intentRes.data || intentRes;
        setClientSecret(data.clientSecret);
        setStepIndex((i) => i + 1);
      } catch (e) {
        if (e?.status === 403) {
          setError("Debes iniciar sesión para contratar un plan institucional.");
        } else if (e?.status === 409) {
          setError(
            "Ya existe una institución con ese dominio de email. Si crees que es un error, contacta con soporte."
          );
        } else {
          setError(e?.message || "Error al procesar la solicitud. Inténtalo de nuevo.");
        }
      } finally {
        setLoading(false);
      }
      return;
    }

    setStepIndex((i) => i + 1);
  };

  const handleBack = () => {
    setError("");
    setStepIndex((i) => i - 1);
  };

  /* ── Submit ──────────────────────────────── */
  const handleSubmit = async () => {
    if (!validateStep()) return;
    setLoading(true);
    setError("");

    try {
      // 1. Create institution
      const institution = await institutionsApi.create({
        nombre: nombre.trim(),
        descripcion: descripcion.trim() || undefined,
        emailContacto: emailContacto.trim(),
        telefonoContacto: telefono.trim() || undefined,
        dominioEmail: dominioEmail.trim(),
        ubicacion: ubicacion.trim() || undefined,
        sitioweb: sitioweb.trim() || undefined,
      });
      persistManagedInstitution(institution);

      // 2. Contract the corporate plan
      const planResponse = await institutionsApi.hirePlan(institution.id, {
        tipoPlan: plan.id,
        numUsuarios,
        duracionMeses,
        aceptarTerminos: true,
        periodo: duracionMeses >= 12 ? "anual" : "mensual",
        documentacionEligibilidad:
          plan.requiereEligibilidad && documentacion.trim()
            ? documentacion.trim()
            : undefined,
      });

      // 3. Redirect to Stripe checkout
      if (planResponse?.paymentUrl) {
        window.location.href = planResponse.paymentUrl;
      } else {
        setError("No se recibió la URL de pago. Contacta con soporte.");
      }
    } catch (e) {
      if (e?.status === 403) {
        setError("Debes iniciar sesión para contratar un plan institucional.");
      } else if (e?.status === 409) {
        setError(
          "Ya existe una institución con ese dominio de email. Si crees que es un error, contacta con soporte."
        );
      } else {
        setError(e?.message || "Error al procesar la solicitud. Inténtalo de nuevo.");
      }
    } finally {
      setLoading(false);
    }
  };
  
  /* ── Submit (no longer used — payment is handled by Stripe Elements) ── */
  const handlePaymentSuccess = () => {
    onClose();
    window.location.href = "/planes/success?tipo=institucional";
  };

  /* ── Render ──────────────────────────────── */
  return (
    <div className="instModalOverlay" onClick={() => !loading && onClose()}>
      <div
        className="instModal"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
      >
        {/* Header */}
        <div className="instModalHeader">
          <div>
            <h2 className="instModalTitle">Contratar: {plan.nombre}</h2>
            <div className="instModalPrice">{plan.precio} · {plan.precioAnual}</div>
          </div>
          <button
            className="instModalClose"
            onClick={() => !loading && onClose()}
            disabled={loading}
          >
            ✕
          </button>
        </div>

        {/* Step indicator */}
        <div className="instStepIndicator">
          {steps.map((s, i) => (
            <div key={s} className="instStepItem">
              <div
                className={[
                  "instStepDot",
                  i < stepIndex ? "instStepDot--done" : "",
                  i === stepIndex ? "instStepDot--active" : "",
                ]
                  .filter(Boolean)
                  .join(" ")}
              >
                {i < stepIndex ? "✓" : i + 1}
              </div>
              <span
                className={i === stepIndex ? "instStepLabel--active" : "instStepLabel"}
              >
                {STEP_LABELS[s]}
              </span>
              {i < steps.length - 1 && <div className="instStepLine" />}
            </div>
          ))}
        </div>

        {/* Step content */}
        <div className="instModalBody">
          {currentStep === "eligibility" && (
            <StepEligibility
              plan={plan}
              userEmail={userEmail}
              dominioElegibilidad={dominioElegibilidad}
              setDominioElegibilidad={(v) => {
                setDominioElegibilidad(v);
                setDominioEmail(v);
              }}
              documentacion={documentacion}
              setDocumentacion={setDocumentacion}
            />
          )}
          {currentStep === "details" && (
            <StepDetails
              nombre={nombre}
              setNombre={setNombre}
              descripcion={descripcion}
              setDescripcion={setDescripcion}
              emailContacto={emailContacto}
              setEmailContacto={(v) => {
                setEmailContacto(v);
                const d = extractDomain(v);
                if (d) setDominioEmail(d);
              }}
              telefono={telefono}
              setTelefono={setTelefono}
              dominioEmail={dominioEmail}
              setDominioEmail={setDominioEmail}
              ubicacion={ubicacion}
              setUbicacion={setUbicacion}
              sitioweb={sitioweb}
              setSitioweb={setSitioweb}
            />
          )}
          {currentStep === "config" && (
            <StepConfig
              plan={plan}
              numUsuarios={numUsuarios}
              setNumUsuarios={setNumUsuarios}
              duracionMeses={duracionMeses}
              setDuracionMeses={setDuracionMeses}
              maxUsers={maxUsers}
            />
          )}
          {currentStep === "confirm" && (
            <StepConfirm
              plan={plan}
              nombre={nombre}
              emailContacto={emailContacto}
              dominioEmail={dominioEmail}
              numUsuarios={numUsuarios}
              duracionMeses={duracionMeses}
              aceptarTerminos={aceptarTerminos}
              setAceptarTerminos={setAceptarTerminos}
            />
          )}
          {currentStep === "payment" && clientSecret && (
            <Elements stripe={stripePromise} options={{ clientSecret, appearance: { theme: "stripe" } }}>
              <StepPayment
                plan={plan}
                duracionMeses={duracionMeses}
                institutionId={institutionId}
                onSuccess={handlePaymentSuccess}
                onError={setError}
              />
            </Elements>
          )}
          {currentStep === "payment" && !clientSecret && (
            <div className="instStepContent">
              <p>Cargando formulario de pago...</p>
            </div>
          )}
        </div>

        {/* Error */}
        {error && <div className="instModalError">⚠️ {error}</div>}

        {/* Actions */}
        {!isPaymentStep && (
        <div className="instModalActions">
          {stepIndex > 0 && (
            <button
              className="instModalBtnBack"
              onClick={handleBack}
              disabled={loading}
            >
              Atrás
            </button>
          )}
          <button
            className="instModalBtnCancel"
            onClick={() => !loading && onClose()}
            disabled={loading}
          >
            Cancelar
          </button>
          <button
            className="instModalBtnNext"
            onClick={handleNext}
            disabled={loading}
          >
            {loading ? "Procesando..." : (isConfirmStep ? "Continuar al pago →" : "Siguiente →")}
          </button>
        </div>
        )}
      </div>
    </div>
  );
}

/* ── Step: Eligibility ────────────────────────────── */
function StepEligibility({
  plan,
  userEmail,
  dominioElegibilidad,
  setDominioElegibilidad,
  documentacion,
  setDocumentacion,
}) {
  const isPublica = plan.tipoEligibilidad === "publica";

  return (
    <div className="instStepContent">
      <div className="instStepIcon">{isPublica ? "🏛️" : "🏫"}</div>
      <h3>Verificación de elegibilidad</h3>
      <p className="instStepDesc">
        {isPublica
          ? "Este plan está reservado para centros educativos de titularidad pública (colegios, institutos y academias públicas). Nuestro equipo revisará la solicitud antes de activar el plan."
          : "Este plan está reservado para centros privados concertados con financiación pública. Debes acreditar tu condición con documentación oficial."}
      </p>

      <div className="instFormGroup">
        <label>Dominio de email del centro *</label>
        <input
          type="text"
          className="instInput"
          placeholder="ej. micentro.es, academiasanjose.com"
          value={dominioElegibilidad}
          onChange={(e) => setDominioElegibilidad(e.target.value)}
        />
        {userEmail && (
          <span className="instFormHint">
            Detectado de tu cuenta: <strong>{extractDomain(userEmail)}</strong>
          </span>
        )}
      </div>

      <div className="instFormGroup">
        <label>Documentación acreditativa *</label>
        <textarea
          className="instTextarea"
          rows={3}
          placeholder={
            isPublica
              ? "Describe brevemente tu centro o adjunta un enlace oficial que acredite su titularidad pública (ej. página web del Ayuntamiento, Consejería de Educación...)"
              : "URL del Registro de Centros Docentes de tu Comunidad Autónoma u otro documento que acredite tu condición de centro concertado"
          }
          value={documentacion}
          onChange={(e) => setDocumentacion(e.target.value)}
        />
        {!isPublica && (
          <span className="instFormHint">
            Ejemplo: URL del Registro de Centros Docentes de tu Comunidad Autónoma.
          </span>
        )}
      </div>
    </div>
  );
}

/* ── Step: Institution details ────────────────────── */
function StepDetails({
  nombre, setNombre,
  descripcion, setDescripcion,
  emailContacto, setEmailContacto,
  telefono, setTelefono,
  dominioEmail, setDominioEmail,
  ubicacion, setUbicacion,
  sitioweb, setSitioweb,
}) {
  return (
    <div className="instStepContent">
      <h3>Datos de la institución</h3>
      <p className="instStepDesc">
        Esta información identificará a tu institución en MeerKatters. El dominio
        de email se utilizará para verificar que los usuarios pertenecen a tu centro.
      </p>

      <div className="instFormRow">
        <div className="instFormGroup">
          <label>Nombre de la institución *</label>
          <input
            type="text"
            className="instInput"
            placeholder="ej. Universidad de Sevilla"
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
          />
        </div>
        <div className="instFormGroup">
          <label>Email de contacto *</label>
          <input
            type="email"
            className="instInput"
            placeholder="contacto@institución.es"
            value={emailContacto}
            onChange={(e) => setEmailContacto(e.target.value)}
          />
        </div>
      </div>

      <div className="instFormRow">
        <div className="instFormGroup">
          <label>Dominio de email institucional *</label>
          <input
            type="text"
            className="instInput"
            placeholder="ej. us.es"
            value={dominioEmail}
            onChange={(e) => setDominioEmail(e.target.value)}
          />
          <span className="instFormHint">
            Los usuarios con este dominio podrán vincularse a tu institución.
          </span>
        </div>
        <div className="instFormGroup">
          <label>Teléfono de contacto</label>
          <input
            type="tel"
            className="instInput"
            placeholder="+34 954 000 000"
            value={telefono}
            onChange={(e) => setTelefono(e.target.value)}
          />
        </div>
      </div>

      <div className="instFormRow">
        <div className="instFormGroup">
          <label>Ubicación</label>
          <input
            type="text"
            className="instInput"
            placeholder="ej. Sevilla, España"
            value={ubicacion}
            onChange={(e) => setUbicacion(e.target.value)}
          />
        </div>
        <div className="instFormGroup">
          <label>Sitio web</label>
          <input
            type="url"
            className="instInput"
            placeholder="https://www.institución.es"
            value={sitioweb}
            onChange={(e) => setSitioweb(e.target.value)}
          />
        </div>
      </div>

      <div className="instFormGroup">
        <label>Descripción</label>
        <textarea
          className="instTextarea"
          rows={2}
          placeholder="Breve descripción de tu institución (opcional)"
          value={descripcion}
          onChange={(e) => setDescripcion(e.target.value)}
        />
      </div>
    </div>
  );
}

/* ── Step: Plan config ────────────────────────────── */
function StepConfig({ plan, numUsuarios, setNumUsuarios, duracionMeses, setDuracionMeses, maxUsers }) {
  const isUnlimited = !plan.maxUsuarios;

  return (
    <div className="instStepContent">
      <h3>Configuración del plan</h3>
      <p className="instStepDesc">
        Ajusta el número de usuarios y la duración de tu plan institucional.
      </p>

      {!isUnlimited && (
        <div className="instFormGroup">
          <label>
            Número de usuarios: <strong>{numUsuarios}</strong>
          </label>
          <input
            type="range"
            className="instRange"
            min={10}
            max={maxUsers}
            step={10}
            value={numUsuarios}
            onChange={(e) => setNumUsuarios(Number(e.target.value))}
          />
          <div className="instRangeLabels">
            <span>10 usuarios</span>
            <span>{maxUsers} usuarios</span>
          </div>
        </div>
      )}

      {isUnlimited && (
        <div className="instInfoBox">
          <strong>Usuarios ilimitados</strong> — Este plan incluye usuarios sin restricción.
        </div>
      )}

      <div className="instFormGroup">
        <label>Duración del plan</label>
        <div className="instDuracionGrid">
          {DURACION_OPTIONS.map((opt) => (
            <button
              key={opt.value}
              type="button"
              className={`instDuracionBtn ${duracionMeses === opt.value ? "instDuracionBtn--active" : ""}`}
              onClick={() => setDuracionMeses(opt.value)}
            >
              {opt.label}
            </button>
          ))}
        </div>
      </div>

      <div className="instConfigSummary">
        <div className="instConfigRow">
          <span>Plan</span>
          <strong>{plan.nombre}</strong>
        </div>
        {!isUnlimited && (
          <div className="instConfigRow">
            <span>Usuarios incluidos</span>
            <strong>{numUsuarios}</strong>
          </div>
        )}
        <div className="instConfigRow">
          <span>Duración</span>
          <strong>
            {DURACION_OPTIONS.find((o) => o.value === duracionMeses)?.label}
          </strong>
        </div>
        <div className="instConfigRow instConfigRow--price">
          <span>Precio estimado</span>
          <strong>{duracionMeses >= 12 ? plan.precioAnual : plan.precio}</strong>
        </div>
      </div>
    </div>
  );
}

/* ── Step: Confirm ────────────────────────────────── */
function StepConfirm({
  plan,
  nombre,
  emailContacto,
  dominioEmail,
  numUsuarios,
  duracionMeses,
  aceptarTerminos,
  setAceptarTerminos,
}) {
  return (
    <div className="instStepContent">
      <h3>Resumen y confirmación</h3>
      <p className="instStepDesc">
        Revisa los datos antes de proceder al pago.
      </p>

      <div className="instSummaryCard">
        <div className="instSummaryRow">
          <span>Institución</span>
          <strong>{nombre || "—"}</strong>
        </div>
        <div className="instSummaryRow">
          <span>Email de contacto</span>
          <strong>{emailContacto || "—"}</strong>
        </div>
        <div className="instSummaryRow">
          <span>Dominio institucional</span>
          <strong>{dominioEmail || "—"}</strong>
        </div>
        <div className="instSummaryRow">
          <span>Plan contratado</span>
          <strong>{plan.nombre}</strong>
        </div>
        {plan.maxUsuarios && (
          <div className="instSummaryRow">
            <span>Usuarios</span>
            <strong>{numUsuarios}</strong>
          </div>
        )}
        <div className="instSummaryRow">
          <span>Duración</span>
          <strong>
            {DURACION_OPTIONS.find((o) => o.value === duracionMeses)?.label}
          </strong>
        </div>
        <div className="instSummaryRow instSummaryRow--price">
          <span>Importe</span>
          <strong>
            {duracionMeses >= 12 ? plan.precioAnual : plan.precio}
          </strong>
        </div>
      </div>

      {plan.requiereEligibilidad && (
        <div className="instEligibilityReminder">
          ℹ️ Este plan con condiciones especiales está sujeto a validación de
          elegibilidad por parte del equipo de MeerKatters. El plan se activará
          una vez completada la verificación.
        </div>
      )}

      <label className="instTermsLabel">
        <input
          type="checkbox"
          checked={aceptarTerminos}
          onChange={(e) => setAceptarTerminos(e.target.checked)}
        />
        <span>
          Acepto los{" "}
          <a href="/terminos" target="_blank" rel="noreferrer">
            términos y condiciones
          </a>{" "}
          de los planes institucionales de MeerKatters.
        </span>
      </label>
    </div>
  );
}

/* ── Step: Payment (Stripe Elements) ──────────────── */
function StepPayment({ plan, duracionMeses, institutionId, onSuccess, onError }) {
  const stripe = useStripe();
  const elements = useElements();
  const [processing, setProcessing] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!stripe || !elements) return;

    setProcessing(true);
    onError("");

    try {
      const { error: submitError } = await elements.submit();
      if (submitError) {
        onError(submitError.message);
        setProcessing(false);
        return;
      }

      const { error: confirmError, paymentIntent } = await stripe.confirmPayment({
        elements,
        confirmParams: {
          return_url: window.location.origin + "/planes/success?tipo=institucional",
        },
        redirect: "if_required",
      });

      if (confirmError) {
        onError(confirmError.message);
        setProcessing(false);
        return;
      }

      if (paymentIntent && paymentIntent.status === "succeeded") {
        await institutionsApi.confirmPlanPayment(paymentIntent.id);
        onSuccess();
      }
    } catch (err) {
      onError(
        err?.response?.data?.error ||
          err?.data?.error ||
          "Error al procesar el pago. Intenta de nuevo."
      );
    } finally {
      setProcessing(false);
    }
  };

  return (
    <div className="instStepContent">
      <h3>Pago seguro</h3>
      <p className="instStepDesc">
        Introduce los datos de tu tarjeta para completar la contratación del plan{" "}
        <strong>{plan.nombre}</strong>.
      </p>

      <form onSubmit={handleSubmit}>
        <PaymentElement />

        <div className="instModalActions" style={{ marginTop: 20 }}>
          <button
            type="submit"
            className="instModalBtnSubmit"
            disabled={processing || !stripe}
          >
            {processing
              ? "Procesando..."
              : `Pagar ${duracionMeses >= 12 ? plan.precioAnual : plan.precio}`}
          </button>
        </div>
      </form>

      <div style={{ marginTop: 12, fontSize: "0.85rem", color: "#666", display: "flex", alignItems: "center", gap: 6 }}>
        <span>🔒</span>
        <span>Tu información está protegida. El pago se procesa de forma segura a través de Stripe.</span>
      </div>
    </div>
  );
}

import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { communitiesApi } from "../../api/communities.api";
import { subscriptionsApi } from "../../api/subscriptions.api";
import { getMyTutorProfiles } from "../../api/tutorEndpoints";
import Header from "../../components/Header/Header";
import PageHeader from "../../components/PageHeader";
import { useAuth } from "../../contexts/AuthContext";
import "./CrearComunidad.css";

const PLAN_LIMITS = {
    FREE: { planLabel: "Gratuito", maxCommunities: 3, maxMembers: 30 },
    PREMIUM: { planLabel: "Premium", maxCommunities: 10, maxMembers: 75 },
    PRO: { planLabel: "Pro", maxCommunities: 25, maxMembers: 250 },
};

const INSTITUTION_PLAN_LIMITS = {
    BASICO: { planLabel: "Academias", maxCommunities: 30, maxMembers: 500 },
    REDUCIDO_PUBLICA: { planLabel: "Academias", maxCommunities: 30, maxMembers: 500 },
    REDUCIDO_PRIVADA: { planLabel: "Academias", maxCommunities: 30, maxMembers: 500 },
    ESTANDAR: { planLabel: "Colegios", maxCommunities: 100, maxMembers: 2000 },
    PREMIUM: {
        planLabel: "Universidades",
        maxCommunities: Number.MAX_SAFE_INTEGER,
        maxMembers: 10000,
    },
};

const normalizeSubscriptionPayload = (payload) => {
    if (!payload || typeof payload !== "object") return {};
    if (payload.subscription && typeof payload.subscription === "object") {
        return payload.subscription;
    }
    if (payload.data && typeof payload.data === "object") {
        if (payload.data.subscription && typeof payload.data.subscription === "object") {
            return payload.data.subscription;
        }
        return payload.data;
    }
    return payload;
};

const normalizeBoolean = (value) => {
    if (typeof value === "boolean") return value;
    if (typeof value === "string") return value.trim().toLowerCase() === "true";
    if (typeof value === "number") return value === 1;
    return false;
};

const resolveInstitutionPlanCode = (rawPlan) => {
    const normalized = (rawPlan || "").toString().trim().toUpperCase();
    if (!normalized) return null;

    if (["BASICO", "ACADEMIA", "ACADEMIAS", "REDUCIDO_PUBLICA", "REDUCIDO_PRIVADA"].includes(normalized)) {
        return "BASICO";
    }
    if (["ESTANDAR", "COLEGIO", "COLEGIOS"].includes(normalized)) {
        return "ESTANDAR";
    }
    if (["PREMIUM", "UNIVERSIDAD", "UNIVERSIDADES"].includes(normalized)) {
        return "PREMIUM";
    }

    return normalized;
};

export default function CrearComunidad() {
    const navigate = useNavigate();
    const { user } = useAuth();
    const hasTeacherProfile = Boolean(user?.esTutor || user?.esProfesor);
    const [nombre, setNombre] = useState("");
    const [descripcion, setDescripcion] = useState("");
    const [imagenPortada, setImagenPortada] = useState(null);
    const [imagenPreview, setImagenPreview] = useState(null);
    const [categoriaInput, setCategoriaInput] = useState("");
    const [categorias, setCategorias] = useState([]);
    const [tipoComunidad, setTipoComunidad] = useState("COMUNIDAD_PUBLICA"); // Debe ser enum del backend
    const [maxMiembros, setMaxMiembros] = useState(30);
    const [planLimits, setPlanLimits] = useState(PLAN_LIMITS.FREE);
    const [institutionContext, setInstitutionContext] = useState({
        hasActivePlan: false,
        institutionId: null,
        institutionName: null,
        planCode: null,
    });
    const [createAsInstitutional, setCreateAsInstitutional] = useState(false);
    const [myCommunitiesCount, setMyCommunitiesCount] = useState(0);
    const [rolInicial, setRolInicial] = useState("ALUMNO");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(null);
    const [perfilTutorRequerido, setPerfilTutorRequerido] = useState(false);

    const institutionalLimits =
        institutionContext.hasActivePlan && institutionContext.planCode
            ? INSTITUTION_PLAN_LIMITS[institutionContext.planCode] || null
            : null;
    const effectiveLimits =
        createAsInstitutional && institutionalLimits ? institutionalLimits : planLimits;
    const maxMembersAllowed = effectiveLimits.maxMembers;
    const maxCommunitiesAllowed = effectiveLimits.maxCommunities;
    const communitiesRemaining = Math.max(0, maxCommunitiesAllowed - myCommunitiesCount);
    const reachedCommunityLimit = myCommunitiesCount >= maxCommunitiesAllowed;
    const isUnlimitedCommunities = maxCommunitiesAllowed >= Number.MAX_SAFE_INTEGER;
    const currentLimitsLabel =
        createAsInstitutional && institutionalLimits
            ? `Institucional ${institutionalLimits.planLabel}`
            : planLimits.planLabel;

    const handleImageUpload = (e) => {
        const file = e.target.files[0];
        if (file) {
            setImagenPortada(file);
            const reader = new FileReader();
            reader.onloadend = () => {
                setImagenPreview(reader.result);
            };
            reader.readAsDataURL(file);
        }
    };

    const agregarCategoria = () => {
        if (categoriaInput.trim() !== "" && !categorias.includes(categoriaInput.trim())) {
            setCategorias([...categorias, categoriaInput.trim()]);
            setCategoriaInput("");
        }
    };

    const eliminarCategoria = (categoriaAEliminar) => {
        setCategorias(categorias.filter(cat => cat !== categoriaAEliminar));
    };

    const handleGuardarBorrador = () => {
        const draft = {
            nombre: nombre.trim(),
            descripcion: descripcion.trim(),
            tipoComunidad,
            maxMiembros,
            createAsInstitutional,
            categorias,
            imagenPreview // store preview data URL, file cannot be stored
        };

        try {
            localStorage.setItem('crearComunidadDraft', JSON.stringify(draft));
            setSuccess('Borrador guardado');
            setTimeout(() => {
                navigate(`/comunidades`);
            }, 1000);
        } catch (err) {
            console.error('Error guardando borrador:', err);
            setError('No se pudo guardar el borrador en el navegador.');
        }
    };

    // Load draft from localStorage on mount
    useEffect(() => {
        try {
            const saved = localStorage.getItem('crearComunidadDraft');
            if (saved) {
                const draft = JSON.parse(saved);
                if (draft.nombre) setNombre(draft.nombre);
                if (draft.descripcion) setDescripcion(draft.descripcion);
                if (draft.tipoComunidad) setTipoComunidad(draft.tipoComunidad);
                if (draft.maxMiembros) setMaxMiembros(draft.maxMiembros);
                if (typeof draft.createAsInstitutional === 'boolean') {
                    setCreateAsInstitutional(draft.createAsInstitutional);
                }
                if (Array.isArray(draft.categorias)) setCategorias(draft.categorias);
                if (draft.imagenPreview) setImagenPreview(draft.imagenPreview);
            }
        } catch (err) {
            console.error('Error cargando borrador:', err);
        }
    }, []);

    useEffect(() => {
        const resolveLimits = (subscription) => {
            const planKey = (subscription?.plan || "FREE").toUpperCase();
            if (PLAN_LIMITS[planKey]) {
                return PLAN_LIMITS[planKey];
            }
            return PLAN_LIMITS.FREE;
        };

        const loadLimitsAndCount = async () => {
            try {
                const [subscriptionRaw, myCommunities] = await Promise.all([
                    subscriptionsApi.getMySubscription(),
                    communitiesApi.listMine({ page: 0, size: 1 }),
                ]);

                const subscription = normalizeSubscriptionPayload(subscriptionRaw);

                const limits = resolveLimits(subscription);
                setPlanLimits(limits);

                const institutionPlanCode = resolveInstitutionPlanCode(subscription?.planCorporativo);
                const institutionPlanLimits = INSTITUTION_PLAN_LIMITS[institutionPlanCode] || null;
                const institutionPlanIsActive = normalizeBoolean(subscription?.planCorporativoActivo);
                const hasInstitutionSignals = Boolean(
                    subscription?.institutionId ||
                    subscription?.institutionNombre ||
                    subscription?.planCorporativo
                );
                const hasActiveInstitutionPlan = Boolean(
                    (institutionPlanIsActive || hasInstitutionSignals) && institutionPlanLimits
                );

                setInstitutionContext({
                    hasActivePlan: hasActiveInstitutionPlan,
                    institutionId: hasActiveInstitutionPlan ? subscription.institutionId : null,
                    institutionName: subscription?.institutionNombre || null,
                    planCode: hasActiveInstitutionPlan ? institutionPlanCode : null,
                });
                if (!hasActiveInstitutionPlan) {
                    setCreateAsInstitutional(false);
                }

                const total =
                    typeof myCommunities?.page?.totalElements === "number"
                        ? myCommunities.page.totalElements
                        : Array.isArray(myCommunities?.content)
                        ? myCommunities.content.length
                        : 0;
                setMyCommunitiesCount(total);

                setMaxMiembros((prev) => {
                    const parsed = Number(prev);
                    if (!Number.isFinite(parsed) || parsed < 1) return limits.maxMembers;
                    return Math.min(parsed, limits.maxMembers);
                });
            } catch (err) {
                console.error("Error cargando límites de plan/comunidades:", err);
                setPlanLimits(PLAN_LIMITS.FREE);
                setInstitutionContext({
                    hasActivePlan: false,
                    institutionId: null,
                    institutionName: null,
                    planCode: null,
                });
                setCreateAsInstitutional(false);
                setMyCommunitiesCount(0);
                setMaxMiembros((prev) => {
                    const parsed = Number(prev);
                    if (!Number.isFinite(parsed) || parsed < 1) return PLAN_LIMITS.FREE.maxMembers;
                    return Math.min(parsed, PLAN_LIMITS.FREE.maxMembers);
                });
            }
        };

        loadLimitsAndCount();
    }, []);

    useEffect(() => {
        setMaxMiembros((prev) => {
            const parsed = Number(prev);
            if (!Number.isFinite(parsed) || parsed < 1) return maxMembersAllowed;
            return Math.min(parsed, maxMembersAllowed);
        });
    }, [maxMembersAllowed]);

    const handleCrearComunidad = async () => {
        // Validación básica
        if (!nombre.trim()) {
            setError("El nombre de la comunidad es requerido.");
            return;
        }

        if (nombre.length < 3) {
            setError("El nombre debe tener al menos 3 caracteres.");
            return;
        }

        if (nombre.length > 100) {
            setError("El nombre no puede exceder 100 caracteres.");
            return;
        }

        if (reachedCommunityLimit) {
            const maxCommunitiesText = isUnlimitedCommunities ? "ilimitadas" : maxCommunitiesAllowed;
            setError(
                `Has alcanzado el límite de ${maxCommunitiesText} comunidades para tu plan ${currentLimitsLabel}.`
            );
            return;
        }

        if (createAsInstitutional && !institutionContext.institutionId) {
            setError("No se pudo identificar tu institución activa para crear una comunidad institucional.");
            return;
        }

        // Si el usuario elige rol PROFESOR, verificar que tenga el perfil de tutor configurado
        if (rolInicial === 'PROFESOR') {
            try {
                const perfilTutor = await getMyTutorProfiles();
                const perfilCompleto = perfilTutor &&
                    perfilTutor.especialidades?.length > 0 &&
                    perfilTutor.tarifaPorHora != null &&
                    perfilTutor.biografia?.trim();
                if (!perfilCompleto) {
                    setError(
                        'Para crear una comunidad como profesor necesitas tener tu perfil de tutor configurado ' +
                        '(especialidades, tarifa y bio). '
                    );
                    setPerfilTutorRequerido(true);
                    return;
                }
            } catch {
                setError(
                    'Para crear una comunidad como profesor necesitas tener tu perfil de tutor configurado ' +
                    '(especialidades, tarifa y bio). '
                );
                setPerfilTutorRequerido(true);
                return;
            }
        }
        setError(null);
        setSuccess(null);
        setLoading(true);

        try {
            // Preparar datos para el API (no enviar imagen como base64)
            const data = {
                nombre: nombre.trim(),
                descripcion: descripcion.trim(),
                tipoGrupo: tipoComunidad,
                imagenUrl: imagenPortada ? undefined : null,
                maxMiembros,
                rolInicial,
            };
            if (createAsInstitutional) {
                data.institutionId = institutionContext.institutionId;
            }

            // Llamar API para crear comunidad
            const response = await communitiesApi.create(data);
            console.log("✅ Comunidad creada:", response);

            // Si seleccionaron imagen, subirla como multipart/form-data
            if (imagenPortada) {
                const formData = new FormData();
                formData.append('file', imagenPortada);
                await communitiesApi.uploadPhoto(response.id, formData);
            }
            
            setSuccess('¡Comunidad creada con éxito!');
            // Clear saved draft on successful creation
            try { localStorage.removeItem('crearComunidadDraft'); } catch (e) { console.warn(e); }
            // Navegar a la comunidad creada
            setTimeout(() => {
                navigate(`/comunidades/${response.id}`);
            }, 1000);

        } catch (err) {
            console.error("❌ Error al crear comunidad:", err);
            setError(err.details?.message || "No se pudo crear la comunidad. Intenta de nuevo.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="crear-comunidad-container">
            <Header page={'comunidades'} />
            <div className="header">
                <PageHeader 
                    title="Crear Comunidad"
                    subtitle="Explora las comunidades que mejor se adaptan a tus necesidades y ganas de aprender"
                />
            </div>
            <div className="body">
               <div className="first-section">
                    <div className="image-upload">
                        <label htmlFor="file-input" className="upload-label">
                            {imagenPreview ? (
                                <img src={imagenPreview} alt="Preview" className="image-preview" />
                            ) : (
                                <div className="upload-placeholder">
                                    <span className="upload-icon">📷</span>
                                    <p>Subir imagen de portada</p>
                                </div>
                            )}
                        </label>
                        <input
                            id="file-input"
                            type="file"
                            accept="image/*"
                            onChange={handleImageUpload}
                            className="file-input"
                        />
                    </div>
                    <div className="form">
                        <div className="form-group">
                            <label htmlFor="nombre">Nombre de la Comunidad</label>
                            <input
                                id="nombre"
                                type="text"
                                value={nombre}
                                onChange={(e) => setNombre(e.target.value)}
                                placeholder="Ingresa el nombre de la comunidad"
                                className="form-input"
                            />
                        </div>
                        <div className="form-group">
                            <label htmlFor="descripcion">Descripción</label>
                            <textarea
                                id="descripcion"
                                value={descripcion}
                                onChange={(e) => setDescripcion(e.target.value)}
                                placeholder="Describe tu comunidad"
                                className="form-textarea"
                                rows="5"
                            />
                        </div>
                    </div>
               </div>
               <div className="second-section">
                    <h3>Categorías</h3>
                    <div className="categoria-input-container">
                        <input
                            type="text"
                            value={categoriaInput}
                            onChange={(e) => setCategoriaInput(e.target.value)}
                            onKeyPress={(e) => e.key === 'Enter' && agregarCategoria()}
                            placeholder="Agregar categoría"
                            className="categoria-input"
                        />
                        <button onClick={agregarCategoria} className="btn-agregar">
                            +
                        </button>
                    </div>
                    <div className="categorias-lista">
                        {categorias.map((categoria, index) => (
                            <div key={index} className="categoria-item">
                                <span>{categoria}</span>
                                <button
                                    onClick={() => eliminarCategoria(categoria)}
                                    className="btn-eliminar"
                                >
                                    ×
                                </button>
                            </div>
                        ))}
                    </div>
               </div>
               <div className="third-section">
                    <h3>Configuración de la Comunidad</h3>
                    <div className="config-group">
                        <label>Tipo de Comunidad</label>
                        <div className="radio-group">
                            <label className="radio-label">
                                <input
                                    type="radio"
                                    value="COMUNIDAD_PUBLICA"
                                    checked={tipoComunidad === "COMUNIDAD_PUBLICA"}
                                    onChange={(e) => setTipoComunidad(e.target.value)}
                                />
                                <span>Pública (acceso libre)</span>
                            </label>
                            <label className="radio-label">
                                <input
                                    type="radio"
                                    value="GRUPO_PRIVADO"
                                    checked={tipoComunidad === "GRUPO_PRIVADO"}
                                    onChange={(e) => setTipoComunidad(e.target.value)}
                                />
                                <span>Privada (requiere solicitud)</span>
                            </label>
                        </div>
                        <div className="plan-limit-box">
                            {institutionContext.hasActivePlan && (
                                <div className="institution-toggle-box">
                                    <label className="institution-toggle-label" htmlFor="institutional-community-toggle">
                                        <input
                                            id="institutional-community-toggle"
                                            type="checkbox"
                                            checked={createAsInstitutional}
                                            onChange={(e) => setCreateAsInstitutional(e.target.checked)}
                                        />
                                        <span>Crear como comunidad institucional</span>
                                    </label>
                                    <p className="institution-toggle-help">
                                        {institutionContext.institutionId
                                            ? (institutionContext.institutionName
                                                ? `Se asociará a ${institutionContext.institutionName}.`
                                                : "Se asociará a tu institución activa.")
                                            : "Tienes plan institucional activo, pero tu cuenta no está vinculada a una institución. Usa el email de contacto institucional o solicita vinculación."}
                                    </p>
                                </div>
                            )}

                            <p><strong>Plan aplicado:</strong> {currentLimitsLabel}</p>
                            <p>
                                <strong>Comunidades creadas:</strong>{' '}
                                {isUnlimitedCommunities
                                    ? `${myCommunitiesCount} / Ilimitadas`
                                    : `${myCommunitiesCount} / ${maxCommunitiesAllowed}`}
                            </p>
                            <p>
                                <strong>Comunidades restantes:</strong>{' '}
                                {isUnlimitedCommunities ? 'Ilimitadas' : communitiesRemaining}
                            </p>
                        </div>

                        <div className="capacity-slider-group">
                            <label htmlFor="max-miembros-slider">Máx. miembros de la comunidad</label>
                            <input
                                id="max-miembros-slider"
                                type="range"
                                min="1"
                                max={maxMembersAllowed}
                                value={maxMiembros}
                                onChange={(e) => setMaxMiembros(Number(e.target.value))}
                                className="capacity-slider"
                            />
                            <div className="capacity-slider-values">
                                <span>1</span>
                                <strong>{maxMiembros} miembros</strong>
                                <span>{maxMembersAllowed}</span>
                            </div>
                            <p className="capacity-help-text">
                                El máximo depende del plan aplicado y no puede superar {maxMembersAllowed} miembros.
                            </p>
                        </div>
                    </div>
               </div>
               <div className="third-section">
                    <h3>Tu rol en la comunidad</h3>
                    <div className="config-group">
                        <p style={{ fontSize: '14px', color: '#444', marginTop: 0, lineHeight: 1.5 }}>
                            Como creador serás administrador de la comunidad. Puedes elegir si tu rol docente será profesor o alumno.
                        </p>
                        {!hasTeacherProfile && (
                            <p style={{ fontSize: '13px', color: '#8a6d3b', marginTop: '0px', marginBottom: '10px' }}>
                                Puedes seleccionar Profesor, pero necesitarás tener tu perfil de tutor configurado para crear la comunidad con ese rol.
                            </p>
                        )}
                        <div className="radio-group">
                            <label className="radio-label">
                                <input
                                    type="radio"
                                    value="PROFESOR"
                                    checked={rolInicial === "PROFESOR"}
                                    onChange={(e) => { setRolInicial(e.target.value); setPerfilTutorRequerido(false); setError(null); }}
                                />
                                <span>Profesor (tus eventos podrán ser valorados por los alumnos)</span>
                            </label>
                            <label className="radio-label">
                                <input
                                    type="radio"
                                    value="ALUMNO"
                                    checked={rolInicial === "ALUMNO"}
                                    onChange={(e) => { setRolInicial(e.target.value); setPerfilTutorRequerido(false); setError(null); }}
                                />
                                <span>Alumno (organizador sin rol docente)</span>
                            </label>
                        </div>
                    </div>
               </div>
               <div className="third-section">
                    <h3>Comunidades corporativas</h3>
                    <div className="config-group">
                        <p style={{ fontSize: '14px', color: '#444', marginTop: 0, lineHeight: 1.5 }}>
                            Si representas a una academia, universidad o centro educativo, la contratación se realiza desde el flujo institucional.
                            Ahí podrás revisar el plan corporativo con múltiples administradores, aforo ampliado, estadísticas avanzadas e integración con Google Classroom.
                        </p>
                        <button
                            type="button"
                            className="btn btn-secondary"
                            onClick={() => navigate('/planes/instituciones')}
                            style={{ marginTop: '8px' }}
                        >
                            Ir a planes institucionales
                        </button>
                    </div>
               </div>
               <div>
                    <div>
                        {error && (
                            <div style={{ width: '100%', padding: '10px', backgroundColor: '#f8d7da', color: '#721c24', borderRadius: '4px', marginBottom: '15px' }}>
                                {error}
                                {perfilTutorRequerido && (
                                    <span>
                                        <button
                                            type="button"
                                            style={{ background: 'none', border: 'none', color: '#721c24', textDecoration: 'underline', cursor: 'pointer', padding: 0, font: 'inherit' }}
                                            onClick={() => navigate('/profesores/nuevo')}
                                        >
                                            Ir a configurar mi perfil de profesor
                                        </button>
                                    </span>
                                )}
                            </div>
                        )}
                        {success && (
                            <div style={{ width: '100%', padding: '10px', backgroundColor: '#d4edda', color: '#155724', borderRadius: '4px', marginBottom: '15px' }}>
                                {success}
                            </div>
                        )}
                    </div>
                    <div className="buttons-container">
                    
                        <button 
                            onClick={handleGuardarBorrador} 
                            className="btn btn-secondary"
                            disabled={loading}
                        >
                            Guardar Borrador
                        </button>
                        <button 
                            onClick={handleCrearComunidad} 
                            className="btn btn-primary"
                            disabled={loading || !nombre.trim() || reachedCommunityLimit}
                        >
                            {loading ? "Creando..." : "Crear Comunidad"}
                        </button>
                </div>
               </div>
               
            </div>
        </div>
    );
}

import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { communitiesApi } from "../../api/communities.api";
import { subscriptionsApi } from "../../api/subscriptions.api";
import Header from "../../components/Header/Header";
import PageHeader from "../../components/PageHeader";
import "./CrearComunidad.css";

const PLAN_LIMITS = {
    FREE: { planLabel: "Gratuito", maxCommunities: 3, maxMembers: 30 },
    PREMIUM: { planLabel: "Premium", maxCommunities: 10, maxMembers: 75 },
    PRO: { planLabel: "Pro", maxCommunities: 25, maxMembers: 250 },
};

export default function CrearComunidad() {
    const navigate = useNavigate();
    const [nombre, setNombre] = useState("");
    const [descripcion, setDescripcion] = useState("");
    const [imagenPortada, setImagenPortada] = useState(null);
    const [imagenPreview, setImagenPreview] = useState(null);
    const [categoriaInput, setCategoriaInput] = useState("");
    const [categorias, setCategorias] = useState([]);
    const [tipoComunidad, setTipoComunidad] = useState("COMUNIDAD_PUBLICA"); // Debe ser enum del backend
    const [maxMiembros, setMaxMiembros] = useState(30);
    const [planLimits, setPlanLimits] = useState(PLAN_LIMITS.FREE);
    const [myCommunitiesCount, setMyCommunitiesCount] = useState(0);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(null);

    const maxMembersAllowed = planLimits.maxMembers;
    const maxCommunitiesAllowed = planLimits.maxCommunities;
    const communitiesRemaining = Math.max(0, maxCommunitiesAllowed - myCommunitiesCount);
    const reachedCommunityLimit = myCommunitiesCount >= maxCommunitiesAllowed;

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
                const [subscription, myCommunities] = await Promise.all([
                    subscriptionsApi.getMySubscription(),
                    communitiesApi.listMine({ page: 0, size: 1 }),
                ]);

                const limits = resolveLimits(subscription);
                setPlanLimits(limits);

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
            setError(
                `Has alcanzado el límite de ${maxCommunitiesAllowed} comunidades para tu plan ${planLimits.planLabel}.`
            );
            return;
        }

        setLoading(true);
        setError(null);
        setSuccess(null);

        try {
            // Preparar datos para el API (no enviar imagen como base64)
            const data = {
                nombre: nombre.trim(),
                descripcion: descripcion.trim(),
                tipoGrupo: tipoComunidad,
                imagenUrl: 'empty',
                maxMiembros,
            };

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
                            <p><strong>Plan actual:</strong> {planLimits.planLabel}</p>
                            <p><strong>Comunidades creadas:</strong> {myCommunitiesCount} / {maxCommunitiesAllowed}</p>
                            <p><strong>Comunidades restantes:</strong> {communitiesRemaining}</p>
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
                                El máximo depende de tu plan y no puede superar {maxMembersAllowed} miembros.
                            </p>
                        </div>
                    </div>
               </div>
               <div>
                    <div>
                        {error && (
                            <div style={{ width: '100%', padding: '10px', backgroundColor: '#f8d7da', color: '#721c24', borderRadius: '4px', marginBottom: '15px' }}>
                                {error}
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

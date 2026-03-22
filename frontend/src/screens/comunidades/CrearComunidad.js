import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { communitiesApi } from "../../api/communities.api";
import { getMyTutorProfiles } from "../../api/tutorEndpoints";
import { useAuth } from "../../contexts/AuthContext";
import Header from "../../components/Header/Header";
import PageHeader from "../../components/PageHeader";
import "./CrearComunidad.css";

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
    const [rolInicial, setRolInicial] = useState("PROFESOR");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(null);
    const [perfilTutorRequerido, setPerfilTutorRequerido] = useState(false);

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
                if (Array.isArray(draft.categorias)) setCategorias(draft.categorias);
                if (draft.imagenPreview) setImagenPreview(draft.imagenPreview);
            }
        } catch (err) {
            console.error('Error cargando borrador:', err);
        }
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

        // Si el usuario elige rol PROFESOR, verificar que tenga el perfil de tutor configurado
        if (hasTeacherProfile && rolInicial === 'PROFESOR') {
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
                imagenUrl: 'empty',
                ...(hasTeacherProfile && { rolInicial })
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
                        <p style={{ fontSize: '12px', color: '#666', marginTop: '10px' }}>
                            La capacidad máxima dependerá de tu plan de suscripción.
                        </p>
                    </div>
               </div>
               {hasTeacherProfile && (
               <div className="third-section">
                    <h3>Tu rol en la comunidad</h3>
                    <div className="config-group">
                        <p style={{ fontSize: '14px', color: '#444', marginTop: 0, lineHeight: 1.5 }}>
                            Como creador serás administrador de la comunidad. Al tener perfil de tutor, puedes elegir tu identidad:
                        </p>
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
               )}
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
                            disabled={loading || !nombre.trim()}
                        >
                            {loading ? "Creando..." : "Crear Comunidad"}
                        </button>
                </div>
               </div>
               
            </div>
        </div>
    );
}

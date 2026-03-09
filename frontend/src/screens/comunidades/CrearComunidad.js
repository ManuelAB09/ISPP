import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { communitiesApi } from "../../api/communities.api";
import Header from "../../components/Header/Header";
import PageHeader from "../../components/PageHeader";
import "./CrearComunidad.css";

export default function CrearComunidad() {
    const navigate = useNavigate();
    const [nombre, setNombre] = useState("");
    const [descripcion, setDescripcion] = useState("");
    const [imagenPortada, setImagenPortada] = useState(null);
    const [imagenPreview, setImagenPreview] = useState(null);
    const [categoriaInput, setCategoriaInput] = useState("");
    const [categorias, setCategorias] = useState([]);
    const [tipoComunidad, setTipoComunidad] = useState("COMUNIDAD_PUBLICA"); // Debe ser enum del backend
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(null);

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
        console.log("Guardando borrador...", {
            nombre,
            descripcion,
            imagenPortada,
            categorias,
            tipoComunidad
        });
        // Aquí iría la lógica para guardar borrador en localStorage
        alert("Funcionalidad de borrador próximamente disponible.");
    };

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

        setLoading(true);
        setError(null);
        setSuccess(null);

        try {
            // Preparar datos para el API (sin categorías, se crean después)
            const data = {
                nombre: nombre.trim(),
                descripcion: descripcion.trim(),
                tipoGrupo: tipoComunidad,
                imagenUrl: imagenPreview // URL en base64 o null
            };

            // Llamar API para crear comunidad
            const response = await communitiesApi.create(data);
            console.log("✅ Comunidad creada:", response);

            setSuccess("¡Comunidad creada con éxito!");

            // Navegar a la comunidad creada después de 2 segundos
            setTimeout(() => {
                navigate(`/comunidades/${response.id}`);
            }, 2000);
        } catch (err) {
            console.error("❌ Error al crear comunidad:", err);
            setError(err.response?.data?.message || "No se pudo crear la comunidad. Intenta de nuevo.");
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
               <div className="buttons-container">
                    {error && (
                        <div style={{ width: '100%', padding: '10px', backgroundColor: '#f8d7da', color: '#721c24', borderRadius: '4px', marginBottom: '15px' }}>
                            ❌ {error}
                        </div>
                    )}
                    {success && (
                        <div style={{ width: '100%', padding: '10px', backgroundColor: '#d4edda', color: '#155724', borderRadius: '4px', marginBottom: '15px' }}>
                            ✅ {success}
                        </div>
                    )}
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
    );
}

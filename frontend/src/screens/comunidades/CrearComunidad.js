import { useState } from "react";
import Header from "../../components/Header/Header";
import "./CrearComunidad.css";

export default function CrearComunidad() {
    const [nombre, setNombre] = useState("");
    const [descripcion, setDescripcion] = useState("");
    const [imagenPortada, setImagenPortada] = useState(null);
    const [imagenPreview, setImagenPreview] = useState(null);
    const [categoriaInput, setCategoriaInput] = useState("");
    const [categorias, setCategorias] = useState([]);
    const [tipoComunidad, setTipoComunidad] = useState("publica");
    const [capacidadMaxima, setCapacidadMaxima] = useState(30);

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

    const handleCapacidadChange = (e) => {
        const value = parseInt(e.target.value);
        if (value >= 1 && value <= 30) {
            setCapacidadMaxima(value);
        }
    };

    const handleGuardarBorrador = () => {
        console.log("Guardando borrador...", {
            nombre,
            descripcion,
            imagenPortada,
            categorias,
            tipoComunidad,
            capacidadMaxima
        });
        // Aquí iría la lógica para guardar borrador
    };

    const handleCrearComunidad = () => {
        console.log("Creando comunidad...", {
            nombre,
            descripcion,
            imagenPortada,
            categorias,
            tipoComunidad,
            capacidadMaxima
        });
        // Aquí iría la lógica para crear la comunidad
    };

    return (
        <div className="crear-comunidad-container">
            <Header page={'comunidades'} />
            <div className="header">
                <div className="headerTitle">
                    <p>Explora las comunidades que mejor se adaptan a tus necesidades y ganas de aprender </p>
                    <span className="line"></span>
                    <h1>Crear Comunidad</h1>
                </div>
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
                                    value="publica"
                                    checked={tipoComunidad === "publica"}
                                    onChange={(e) => setTipoComunidad(e.target.value)}
                                />
                                <span>Pública</span>
                            </label>
                            <label className="radio-label">
                                <input
                                    type="radio"
                                    value="privada"
                                    checked={tipoComunidad === "privada"}
                                    onChange={(e) => setTipoComunidad(e.target.value)}
                                />
                                <span>Privada</span>
                            </label>
                        </div>
                    </div>
                    <div className="config-group">
                        <label htmlFor="capacidad">Capacidad Máxima de Miembros</label>
                        <input
                            id="capacidad"
                            type="number"
                            value={capacidadMaxima}
                            onChange={handleCapacidadChange}
                            min="1"
                            max="30"
                            className="capacidad-input"
                        />
                        <span className="capacidad-info">Máximo: 30 miembros</span>
                    </div>
               </div>
               <div className="buttons-container">
                    <button onClick={handleGuardarBorrador} className="btn btn-secondary">
                        Guardar Borrador
                    </button>
                    <button onClick={handleCrearComunidad} className="btn btn-primary">
                        Crear Comunidad
                    </button>
               </div>
            </div>
        </div>
    );
}

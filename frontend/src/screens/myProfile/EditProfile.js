import { useState, useEffect } from "react"
import { useAuth } from "../../contexts/AuthContext"
import { MapContainer, TileLayer, Marker, Popup, useMapEvents } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import L from 'leaflet'
import "./EditProfile.css"

const ACADEMIC_INTERESTS = [
    'Ingeniería Software',
    'Diseño',
    'Física',
    'Historia',
    'Negocios',
    'Medicina',
    'Matemáticas',
    'Literatura',
    'Química',
    'Derecho',
]

const defaultPosition = [37.3891, -5.9845]

const selectedLocationIcon = L.icon({
    iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-green.png',
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
    shadowUrl: 'https://unpkg.com/leaflet@1.7.1/dist/images/marker-shadow.png',
    shadowSize: [41, 41],
})

function MapClickSelector({ onMapClick }) {
    useMapEvents({
        click: (e) => {
            onMapClick(e.latlng.lat, e.latlng.lng)
        },
    })

    return null
}

const EditProfile = ({ onClose, onSave, ubicacionPreseleccionada = null }) => {
    const { user, updateProfile } = useAuth()
    
    // Estados para los campos del formulario
    const [formData, setFormData] = useState({
        nombre: "",
        descripcion: "",
        universidad: "",
        grado: "",
        ubicacion: "",
        intereses: [],
    })
    
    const [profileImage, setProfileImage] = useState(null)
    const [profileImagePreview, setProfileImagePreview] = useState('')
    const [ubicacionSeleccionada, setUbicacionSeleccionada] = useState(null)
    const [markerPosition, setMarkerPosition] = useState(null)
    const [isSaving, setIsSaving] = useState(false)
    const [error, setError] = useState('')
    const [success, setSuccess] = useState('')

    // Cargar datos del usuario al montar el componente
    useEffect(() => {
        if (user) {
            const ubicacionTexto = typeof user.ubicacion === 'string'
                ? user.ubicacion
                : user.ubicacion?.nombre || "";
            
            setFormData({
                nombre: user.nombre || "",
                descripcion: user.bio || "",
                universidad: user.universidad || "",
                grado: user.grado || "",
                ubicacion: ubicacionTexto,
                intereses: user.intereses || [],
            })
            
            if (user.foto) {
                setProfileImagePreview(user.foto)
            }
            
            // Si el usuario tiene ubicación con coordenadas, cargarla
            if (user.ubicacion && typeof user.ubicacion === 'object' && 
                user.ubicacion.latitud != null && user.ubicacion.longitud != null) {
                setUbicacionSeleccionada(user.ubicacion)
                setMarkerPosition({
                    latitud: Number(user.ubicacion.latitud),
                    longitud: Number(user.ubicacion.longitud),
                })
            }
        }
    }, [user])

    useEffect(() => {
        if (!ubicacionPreseleccionada) {
            return
        }

        const nombreUbicacion =
            typeof ubicacionPreseleccionada === 'string'
                ? ubicacionPreseleccionada
                : (ubicacionPreseleccionada.nombre || '')

        setFormData((prev) => ({
            ...prev,
            ubicacion: nombreUbicacion,
        }))

        if (typeof ubicacionPreseleccionada === 'object') {
            setUbicacionSeleccionada(ubicacionPreseleccionada)
            if (
                ubicacionPreseleccionada.latitud != null
                && ubicacionPreseleccionada.longitud != null
            ) {
                setMarkerPosition({
                    latitud: Number(ubicacionPreseleccionada.latitud),
                    longitud: Number(ubicacionPreseleccionada.longitud),
                })
            }
        }
    }, [ubicacionPreseleccionada])

    const handleInputChange = (e) => {
        const { name, value, type, checked } = e.target
        setFormData((prev) => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value,
        }))
        if (name === 'ubicacion') {
            if (ubicacionSeleccionada && value.trim() !== (ubicacionSeleccionada.nombre || '').trim()) {
                setUbicacionSeleccionada(null)
            }
        }
        if (error) setError('')
    }

    const toggleInterest = (interest) => {
        setFormData((prev) => ({
            ...prev,
            intereses: prev.intereses.includes(interest)
                ? prev.intereses.filter((i) => i !== interest)
                : [...prev.intereses, interest],
        }))
    }

    const handleImageChange = (e) => {
        const file = e.target.files[0]
        if (file) {
            // Validar tipo de archivo
            if (!file.type.startsWith('image/')) {
                setError('Por favor, selecciona un archivo de imagen válido')
                return
            }
            // Validar tamaño (máx 5MB)
            if (file.size > 5 * 1024 * 1024) {
                setError('La imagen no debe superar los 5MB')
                return
            }
            setProfileImage(file)
            setProfileImagePreview(URL.createObjectURL(file))
            if (error) setError('')
        }
    }

    const removeProfileImage = () => {
        setProfileImage(null)
        if (profileImagePreview) {
            URL.revokeObjectURL(profileImagePreview)
        }
        setProfileImagePreview('')
    }

    const handleSubmit = async (e) => {
        e.preventDefault()
        setError('')
        setSuccess('')
        setIsSaving(true)

        // Validaciones básicas
        if (!formData.nombre.trim()) {
            setError('El nombre es obligatorio')
            setIsSaving(false)
            return
        }

        const ubicacionTexto = formData.ubicacion.trim()
        const ubicacionActualNombre = (
            typeof user?.ubicacion === 'string'
                ? user.ubicacion
                : user?.ubicacion?.nombre || ''
        ).trim()

        let ubicacionFinal = null
        if (
            ubicacionTexto
            && ubicacionSeleccionada
            && (ubicacionSeleccionada.nombre || '').trim().toLowerCase() === ubicacionTexto.toLowerCase()
        ) {
            ubicacionFinal = ubicacionSeleccionada
        }

        try {
            // Preparar datos para el endpoint
            const profileData = {
                nombre: formData.nombre.trim(),
                bio: formData.descripcion.trim(),
                universidad: formData.universidad.trim(),
                grado: formData.grado.trim(),
                ubicacion: ubicacionFinal?.nombre || ubicacionTexto,
                intereses: formData.intereses,
                foto: profileImage ? profileImagePreview : (user?.foto || ''),
            }

            const ubicacionParaGuardar =
                ubicacionFinal && ubicacionFinal.nombre === profileData.ubicacion
                    ? ubicacionFinal
                    : null

            const result = await updateProfile(profileData, ubicacionParaGuardar)
            
            if (result.success) {
                setSuccess('Perfil actualizado correctamente')
                
                // Llamar callback de guardado si existe
                if (onSave) {
                    onSave(result.user)
                }
                
                // Cerrar después de un momento
                setTimeout(() => {
                    onClose()
                }, 1500)
            } else {
                setError(result.error || 'Error al guardar los cambios')
            }
        } catch (err) {
            setError(err.message || 'Error al guardar los cambios')
        } finally {
            setIsSaving(false)
        }
    }

    const handleSeleccionUbicacion = (location) => {
        setUbicacionSeleccionada(location)
        if (location?.latitud != null && location?.longitud != null) {
            setMarkerPosition({
                latitud: Number(location.latitud),
                longitud: Number(location.longitud),
            })
        }
        setFormData(prev => ({
            ...prev,
            ubicacion: location?.nombre || ''
        }))
    }

    const handleMapClick = (lat, lng) => {
        const latNum = Number(lat)
        const lngNum = Number(lng)
        const nombreCoords = `${latNum.toFixed(6)}, ${lngNum.toFixed(6)}`

        setMarkerPosition({ latitud: latNum, longitud: lngNum })
        setUbicacionSeleccionada({
            nombre: nombreCoords,
            direccion: '',
            latitud: latNum,
            longitud: lngNum,
        })
        setFormData((prev) => ({
            ...prev,
            ubicacion: nombreCoords,
        }))
        if (error) setError('')
    }

    const handleBorrarUbicacion = () => {
        setUbicacionSeleccionada(null)
        setMarkerPosition(null)
        setFormData(prev => ({
            ...prev,
            ubicacion: ''
        }))
    }

    return (
        <div className="edit-profile-overlay">
            <div className="edit-profile-modal">
                {/* Botón cerrar */}
                <button className="edit-profile-close" onClick={onClose}>
                    ✕
                </button>

                <h1 className="edit-profile-title">Editar Perfil</h1>

                <form onSubmit={handleSubmit} className="edit-profile-form">
                    {/* Sección: Foto de perfil */}
                    <section className="edit-profile-section">
                        <h2 className="edit-profile-section__title">Foto de perfil</h2>
                        <div className="edit-profile-image-container">
                            {profileImagePreview ? (
                                <div className="edit-profile-image-preview">
                                    <img src={profileImagePreview} alt="Vista previa" />
                                    <button
                                        type="button"
                                        className="edit-profile-remove-image"
                                        onClick={removeProfileImage}
                                        aria-label="Eliminar imagen"
                                    >
                                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                            <line x1="18" y1="6" x2="6" y2="18"/>
                                            <line x1="6" y1="6" x2="18" y2="18"/>
                                        </svg>
                                    </button>
                                </div>
                            ) : (
                                <div className="edit-profile-image-placeholder">
                                    <span className="placeholder-icon">👤</span>
                                </div>
                            )}
                            <div className="edit-profile-image-actions">
                                <label htmlFor="profileImageEdit" className="edit-profile-upload-btn">
                                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                                        <polyline points="17 8 12 3 7 8"/>
                                        <line x1="12" y1="3" x2="12" y2="15"/>
                                    </svg>
                                    Subir imagen
                                </label>
                                <input
                                    type="file"
                                    id="profileImageEdit"
                                    name="profileImage"
                                    accept="image/*"
                                    onChange={handleImageChange}
                                    className="edit-profile-image-input"
                                />
                                <p className="edit-profile-image-hint">JPG, PNG o GIF. Máx 5MB.</p>
                            </div>
                        </div>
                    </section>

                    {/* Sección: Información personal */}
                    <section className="edit-profile-section">
                        <h2 className="edit-profile-section__title">Información personal</h2>
                        
                        <div className="edit-profile-form-group">
                            <label htmlFor="nombre">Nombre completo *</label>
                            <input
                                type="text"
                                id="nombre"
                                name="nombre"
                                value={formData.nombre}
                                onChange={handleInputChange}
                                placeholder="Tu nombre completo"
                                required
                            />
                        </div>

                        <div className="edit-profile-form-group">
                            <label htmlFor="descripcion">Descripción personal</label>
                            <textarea
                                id="descripcion"
                                name="descripcion"
                                value={formData.descripcion}
                                onChange={handleInputChange}
                                placeholder="Cuéntanos sobre ti..."
                                rows={4}
                            />
                        </div>
                    </section>

                    {/* Sección: Información académica */}
                    <section className="edit-profile-section">
                        <h2 className="edit-profile-section__title">Información académica</h2>
                        
                        <div className="edit-profile-form-group">
                            <label htmlFor="universidad">Universidad</label>
                            <input
                                type="text"
                                id="universidad"
                                name="universidad"
                                value={formData.universidad}
                                onChange={handleInputChange}
                                placeholder="Tu universidad"
                            />
                        </div>

                        <div className="edit-profile-form-group">
                            <label htmlFor="grado">Grado / Carrera</label>
                            <input
                                type="text"
                                id="grado"
                                name="grado"
                                value={formData.grado}
                                onChange={handleInputChange}
                                placeholder="Tu grado o carrera"
                            />
                        </div>

                        <div className="edit-profile-form-group">
                            <label htmlFor="ubicacion">Ubicación</label>
                            <input
                                type="text"
                                id="ubicacion"
                                name="ubicacion"
                                value={formData.ubicacion}
                                onChange={handleInputChange}
                                placeholder="Ciudad, País"
                            />
                            <p className="edit-profile-field-hint">
                                Haz click en el mapa para mover el marcador. No se cargan ubicaciones existentes automáticamente.
                            </p>
                            
                            {/* Mapa de ubicación */}
                            <div style={{ marginTop: '10px', marginBottom: '10px' }}>
                                <div style={{ height: '300px', borderRadius: '8px', overflow: 'hidden' }}>
                                    <MapContainer
                                        center={
                                            markerPosition
                                                ? [markerPosition.latitud, markerPosition.longitud]
                                                : defaultPosition
                                        }
                                        zoom={markerPosition ? 13 : 12}
                                        style={{ height: '100%', width: '100%' }}
                                    >
                                        <TileLayer
                                            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                                            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                                        />
                                        <MapClickSelector
                                            onMapClick={handleMapClick}
                                        />
                                        {markerPosition && (
                                            <Marker
                                                position={[Number(markerPosition.latitud), Number(markerPosition.longitud)]}
                                                icon={selectedLocationIcon}
                                            >
                                                <Popup>
                                                    <strong>{formData.ubicacion || 'Ubicación seleccionada'}</strong>
                                                    <br />
                                                    Marcador movible (no vinculado a catálogo de ubicaciones)
                                                </Popup>
                                            </Marker>
                                        )}
                                    </MapContainer>
                                </div>
                            </div>
                            
                            <div style={{ display: 'flex', gap: '10px', marginTop: '10px', flexWrap: 'wrap' }}>
                                <button
                                    type="button"
                                    className="edit-profile-btn edit-profile-btn--secondary"
                                    onClick={handleBorrarUbicacion}
                                    style={{ backgroundColor: '#ff4444', color: 'white' }}
                                >
                                    Borrar ubicación
                                </button>
                            </div>
                        </div>
                    </section>

                    {/* Sección: Intereses académicos */}
                    <section className="edit-profile-section">
                        <h2 className="edit-profile-section__title">Intereses académicos</h2>
                        <p className="edit-profile-section__description">
                            Selecciona tus áreas de interés para encontrar comunidades afines.
                        </p>
                        <div className="edit-profile-interests">
                            {ACADEMIC_INTERESTS.map((interest) => (
                                <button
                                    key={interest}
                                    type="button"
                                    className={`edit-profile-interest-chip ${
                                        formData.intereses.includes(interest) ? 'selected' : ''
                                    }`}
                                    onClick={() => toggleInterest(interest)}
                                >
                                    {interest}
                                </button>
                            ))}
                        </div>
                    </section>

                    {/* Mensajes de error y éxito */}
                    {error && (
                        <div className="edit-profile-error">{error}</div>
                    )}
                    {success && (
                        <div className="edit-profile-success">{success}</div>
                    )}

                    {/* Botones de acción */}
                    <div className="edit-profile-actions">
                        <button
                            type="button"
                            className="edit-profile-btn edit-profile-btn--secondary"
                            onClick={onClose}
                            disabled={isSaving}
                        >
                            Cancelar
                        </button>
                        <button
                            type="submit"
                            className="edit-profile-btn edit-profile-btn--primary"
                            disabled={isSaving}
                        >
                            {isSaving ? 'Guardando...' : 'Guardar cambios'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    )
}

export default EditProfile

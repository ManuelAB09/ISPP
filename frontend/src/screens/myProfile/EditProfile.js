import { useState, useEffect } from "react"
import { authApi } from "../../api/auth.api"
import { getApiBaseUrl } from "../../api/baseUrl"
import { useAuth } from "../../contexts/AuthContext"
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

const RENATA_PATH_PREFIX = '/static/images/renata/'

const toAbsoluteImageUrl = (imageUrl) => {
    if (!imageUrl || !String(imageUrl).trim()) {
        return ''
    }

    const value = String(imageUrl).trim()
    if (/^https?:\/\//i.test(value) || value.startsWith('data:image/') || value.startsWith('blob:')) {
        return value
    }

    const base = getApiBaseUrl()
    if (value.startsWith('/')) {
        return `${base}${value}`
    }

    return `${base}/${value}`
}

const extractRenataAvatarPath = (imageUrl) => {
    if (!imageUrl || !String(imageUrl).trim()) {
        return ''
    }

    const value = String(imageUrl).trim()
    const markerIndex = value.indexOf(RENATA_PATH_PREFIX)
    if (markerIndex >= 0) {
        return value.slice(markerIndex)
    }

    return ''
}

const EditProfile = ({ onClose, onSave }) => {
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
    
    const [selectedAvatar, setSelectedAvatar] = useState('')
    const [fotoToSave, setFotoToSave] = useState('')
    const [avatarOptions, setAvatarOptions] = useState([])
    const [loadingAvatars, setLoadingAvatars] = useState(false)
    const [profileImagePreview, setProfileImagePreview] = useState('')
    const [selectedPhotoFile, setSelectedPhotoFile] = useState(null)
    const [fileInputKey, setFileInputKey] = useState(0)
    const [isSaving, setIsSaving] = useState(false)
    const [error, setError] = useState('')
    const [success, setSuccess] = useState('')
    const [fotoBackgroundColor, setFotoBackgroundColor] = useState('#ffffff')

    // Cargar datos del usuario al montar el componente
    useEffect(() => {
        if (user) {
            setFormData({
                nombre: user.nombre || "",
                descripcion: user.bio || "",
                universidad: user.universidad || "",
                grado: user.grado || "",
                ubicacion: user.ubicacion || "",
                intereses: user.intereses || [],
            })

            const userAvatarPath = extractRenataAvatarPath(user.foto)
            setSelectedAvatar(userAvatarPath)
            setFotoToSave(userAvatarPath || user.foto || '')
            setProfileImagePreview(toAbsoluteImageUrl(user.foto))
            setFotoBackgroundColor(user.fotoBackgroundColor || '#ffffff')
        }
    }, [user])

    useEffect(() => {
        const loadAvatars = async () => {
            setLoadingAvatars(true)
            try {
                const data = await authApi.getProfileAvatars()
                const options = Array.isArray(data) ? data : []
                setAvatarOptions(options)
            } catch {
                setAvatarOptions([])
            } finally {
                setLoadingAvatars(false)
            }
        }

        loadAvatars()
    }, [])

    useEffect(() => {
        return () => {
            if (profileImagePreview.startsWith('blob:')) {
                URL.revokeObjectURL(profileImagePreview)
            }
        }
    }, [profileImagePreview])

    const handleInputChange = (e) => {
        const { name, value, type, checked } = e.target
        setFormData((prev) => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value,
        }))
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

    const handleSelectAvatar = (avatarPath) => {
        setSelectedAvatar(avatarPath)
        setSelectedPhotoFile(null)
        setFotoToSave(avatarPath)
        setProfileImagePreview(toAbsoluteImageUrl(avatarPath))
        if (error) setError('')
    }

    const handleFileSelection = (e) => {
        const file = e.target.files?.[0]
        if (!file) {
            return
        }

        const allowedTypes = ['image/jpeg', 'image/png', 'image/webp']
        if (!allowedTypes.includes(file.type)) {
            setError('Formato no permitido. Usa JPG, PNG o WEBP.')
            return
        }

        const maxBytes = 5 * 1024 * 1024
        if (file.size > maxBytes) {
            setError('La imagen supera el límite de 5MB.')
            return
        }

        const blobUrl = URL.createObjectURL(file)
        setSelectedAvatar('')
        setSelectedPhotoFile(file)
        setProfileImagePreview(blobUrl)
        if (error) setError('')
    }

    const removeProfileImage = () => {
        setSelectedAvatar('')
        setSelectedPhotoFile(null)
        setFotoToSave('')
        setProfileImagePreview('')
        setFileInputKey((prev) => prev + 1)
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

        try {
            if (selectedPhotoFile) {
                await authApi.uploadProfilePhoto(selectedPhotoFile)
            }

            // Preparar datos para el endpoint
            const profileData = {
                nombre: formData.nombre.trim(),
                bio: formData.descripcion.trim(),
                universidad: formData.universidad.trim(),
                grado: formData.grado.trim(),
                ubicacion: formData.ubicacion.trim(),
                intereses: formData.intereses,
                fotoBackgroundColor: fotoBackgroundColor,
            }

            // Solo enviamos foto cuando no se subió archivo en esta misma acción.
            // Si hubo upload, el backend ya guardó la imagen y evitamos reenviar un payload grande.
            if (!selectedPhotoFile) {
                profileData.foto = fotoToSave
            }

            const result = await updateProfile(profileData)
            
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
                                <div className="edit-profile-image-preview" style={{ backgroundColor: fotoBackgroundColor }}>
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
                                <div className="edit-profile-image-placeholder" style={{ backgroundColor: fotoBackgroundColor }}>
                                    <span className="placeholder-icon">👤</span>
                                </div>
                            )}
                            <div className="edit-profile-image-actions">
                                <label className="edit-profile-upload-btn" htmlFor="profile-photo-input">
                                    Subir foto
                                </label>
                                <input
                                    key={fileInputKey}
                                    id="profile-photo-input"
                                    type="file"
                                    className="edit-profile-image-input"
                                    accept="image/jpeg,image/png,image/webp"
                                    onChange={handleFileSelection}
                                />
                                <p className="edit-profile-image-hint">
                                    Sube una imagen (JPG, PNG o WEBP, máx. 5MB) o elige un avatar.
                                </p>
                            </div>
                        </div>
                        <div className="edit-profile-color-picker">
                            <label htmlFor="foto-background-color">Color de fondo:</label>
                            <input
                                type="color"
                                id="foto-background-color"
                                value={fotoBackgroundColor}
                                onChange={(e) => setFotoBackgroundColor(e.target.value)}
                                title="Selecciona el color de fondo"
                            />
                        </div>
                        <div className="edit-profile-avatar-gallery">
                            {loadingAvatars ? (
                                <p className="edit-profile-image-hint">Cargando avatares...</p>
                            ) : avatarOptions.length === 0 ? (
                                <p className="edit-profile-image-hint">No hay avatares disponibles ahora mismo.</p>
                            ) : (
                                avatarOptions.map((avatarPath) => (
                                    <button
                                        type="button"
                                        key={avatarPath}
                                        className={`edit-profile-avatar-option ${selectedAvatar === avatarPath ? 'selected' : ''}`}
                                        onClick={() => handleSelectAvatar(avatarPath)}
                                        aria-label={`Seleccionar avatar ${avatarPath}`}
                                        title={avatarPath.split('/').pop() || 'Avatar'}
                                    >
                                        <img src={toAbsoluteImageUrl(avatarPath)} alt={avatarPath} />
                                    </button>
                                ))
                            )}
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

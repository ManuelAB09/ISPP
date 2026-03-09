import { useEffect, useState } from "react"
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

const DEFAULT_PROFILE_AVATAR =
    "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 120 120'%3E%3Ccircle cx='60' cy='60' r='60' fill='%23E6EAF3'/%3E%3Ccircle cx='60' cy='46' r='22' fill='%2395A1BB'/%3E%3Cpath d='M20 106c6-20 22-32 40-32s34 12 40 32' fill='%2395A1BB'/%3E%3C/svg%3E";

const toAbsoluteImageUrl = (imageUrl, fallback = DEFAULT_PROFILE_AVATAR) => {
    if (!imageUrl || !String(imageUrl).trim()) {
        return fallback;
    }

    const value = String(imageUrl).trim();
    if (/^https?:\/\//i.test(value) || value.startsWith('data:image/') || value.startsWith('blob:')) {
        return value;
    }

    const base = getApiBaseUrl();
    if (value.startsWith('/')) {
        return `${base}${value}`;
    }

    return `${base}/${value}`;
};

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
        nivelEstudios: "",
        baseFormativa: "",
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
    const [fieldErrors, setFieldErrors] = useState({})

    const toFormFieldName = (backendField) => {
        if (backendField === 'bio') return 'descripcion'
        return backendField
    }

    // Cargar datos del usuario al montar el componente
    useEffect(() => {
        if (user) {
            setFormData({
                nombre: user.nombre || "",
                descripcion: user.bio || "",
                universidad: user.universidad || "",
                grado: user.grado || "",
                nivelEstudios: user.nivelEstudios || "",
                baseFormativa: user.baseFormativa || "",
                ubicacion: user.ubicacion || "",
                intereses: user.intereses || [],
            })

            const userAvatarPath = extractRenataAvatarPath(user.foto)
            setSelectedAvatar(userAvatarPath)
            setFotoToSave(userAvatarPath || user.foto || '')
            setProfileImagePreview(toAbsoluteImageUrl(user.foto, DEFAULT_PROFILE_AVATAR))
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
        setFieldErrors((prev) => {
            if (!prev[name]) return prev
            const next = { ...prev }
            delete next[name]
            return next
        })
        if (error) setError('')
    }

    const toggleInterest = (interest) => {
        setFormData((prev) => ({
            ...prev,
            intereses: prev.intereses.includes(interest)
                ? prev.intereses.filter((i) => i !== interest)
                : [...prev.intereses, interest],
        }))
        setFieldErrors((prev) => {
            if (!prev.intereses) return prev
            const next = { ...prev }
            delete next.intereses
            return next
        })
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
        setFieldErrors({})
        setIsSaving(true)

        // Validaciones básicas
        if (!formData.nombre.trim()) {
            setFieldErrors({ nombre: 'El nombre es obligatorio' })
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
                nivelEstudios: formData.nivelEstudios.trim(),
                baseFormativa: formData.baseFormativa.trim(),
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
                const backendErrors = result.validationErrors || {}
                const mappedErrors = Object.entries(backendErrors).reduce((acc, [key, value]) => {
                    acc[toFormFieldName(key)] = value
                    return acc
                }, {})

                if (Object.keys(mappedErrors).length > 0) {
                    setFieldErrors(mappedErrors)
                    setError('Revisa los campos marcados')
                } else {
                    setError(result.error || 'Error al guardar los cambios')
                }
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
                            <div className="edit-profile-image-preview" style={{ backgroundColor: fotoBackgroundColor }}>
                                <img
                                    src={profileImagePreview || DEFAULT_PROFILE_AVATAR}
                                    alt="Vista previa"
                                    onError={e => { e.target.onerror = null; e.target.src = DEFAULT_PROFILE_AVATAR; }}
                                />
                                {(profileImagePreview && profileImagePreview !== DEFAULT_PROFILE_AVATAR) && (
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
                                )}
                            </div>
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
                                className={fieldErrors.nombre ? 'edit-profile-input-error' : ''}
                                aria-invalid={Boolean(fieldErrors.nombre)}
                                required
                            />
                            {fieldErrors.nombre && (
                                <span className="edit-profile-field-error">{fieldErrors.nombre}</span>
                            )}
                        </div>

                        <div className="edit-profile-form-group">
                            <label htmlFor="descripcion">Descripción personal</label>
                            <textarea
                                id="descripcion"
                                name="descripcion"
                                value={formData.descripcion}
                                onChange={handleInputChange}
                                placeholder="Cuéntanos sobre ti..."
                                className={fieldErrors.descripcion ? 'edit-profile-input-error' : ''}
                                aria-invalid={Boolean(fieldErrors.descripcion)}
                                rows={4}
                            />
                            {fieldErrors.descripcion && (
                                <span className="edit-profile-field-error">{fieldErrors.descripcion}</span>
                            )}
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
                                className={fieldErrors.universidad ? 'edit-profile-input-error' : ''}
                                aria-invalid={Boolean(fieldErrors.universidad)}
                            />
                            {fieldErrors.universidad && (
                                <span className="edit-profile-field-error">{fieldErrors.universidad}</span>
                            )}
                        </div>

                        <div className="edit-profile-form-group">
                            <label htmlFor="grado">Grado / Carrera</label>
                            <input
                                type="text"
                                id="grado"
                                name="grado"
                                value={formData.grado}
                                onChange={handleInputChange}
                                placeholder="El nombre de tu grado o carrera"
                                className={fieldErrors.grado ? 'edit-profile-input-error' : ''}
                                aria-invalid={Boolean(fieldErrors.grado)}
                            />
                            {fieldErrors.grado && (
                                <span className="edit-profile-field-error">{fieldErrors.grado}</span>
                            )}
                        </div>

                        <div className="edit-profile-form-group">
                            <label htmlFor="nivelEstudios">Nivel de estudios</label>
                            <input
                                type="text"
                                id="nivelEstudios"
                                name="nivelEstudios"
                                value={formData.nivelEstudios}
                                onChange={handleInputChange}
                                placeholder="Ej: Grado, Máster, Doctorado"
                                className={fieldErrors.nivelEstudios ? 'edit-profile-input-error' : ''}
                                aria-invalid={Boolean(fieldErrors.nivelEstudios)}
                            />
                            {fieldErrors.nivelEstudios && (
                                <span className="edit-profile-field-error">{fieldErrors.nivelEstudios}</span>
                            )}
                        </div>

                        <div className="edit-profile-form-group">
                            <label htmlFor="baseFormativa">Base formativa</label>
                            <input
                                type="text"
                                id="baseFormativa"
                                name="baseFormativa"
                                value={formData.baseFormativa}
                                onChange={handleInputChange}
                                placeholder="Ej: Científico-tecnológica"
                                className={fieldErrors.baseFormativa ? 'edit-profile-input-error' : ''}
                                aria-invalid={Boolean(fieldErrors.baseFormativa)}
                            />
                            {fieldErrors.baseFormativa && (
                                <span className="edit-profile-field-error">{fieldErrors.baseFormativa}</span>
                            )}
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
                                className={fieldErrors.ubicacion ? 'edit-profile-input-error' : ''}
                                aria-invalid={Boolean(fieldErrors.ubicacion)}
                            />
                            {fieldErrors.ubicacion && (
                                <span className="edit-profile-field-error">{fieldErrors.ubicacion}</span>
                            )}
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
                        {fieldErrors.intereses && (
                            <span className="edit-profile-field-error">{fieldErrors.intereses}</span>
                        )}
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

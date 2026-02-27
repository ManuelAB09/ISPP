import { useState } from "react"
import { useNavigate } from "react-router-dom"
import { apiClient } from "../../api/client"
import { useAuth } from "../../contexts/AuthContext"
import "./Settings.css"

const Settings = ({ onClose }) => {
    const navigate = useNavigate()
    const { logout } = useAuth()
    
    // Estados para los toggles y configuraciones
    const [profileVisibility, setProfileVisibility] = useState(true)
    const [emailNotifications, setEmailNotifications] = useState(true)
    const [pushNotifications, setPushNotifications] = useState(false)
    const [twoFactorAuth, setTwoFactorAuth] = useState(false)

    // Estados para modales de confirmación
    const [showDeleteAccount, setShowDeleteAccount] = useState(false)
    const [showAccountDeleted, setShowAccountDeleted] = useState(false)
    const [isDeletingAccount, setIsDeletingAccount] = useState(false)
    const [deleteError, setDeleteError] = useState("")

    // Estados para cambio de contraseña
    const [currentPassword, setCurrentPassword] = useState("")
    const [newPassword, setNewPassword] = useState("")
    const [confirmPassword, setConfirmPassword] = useState("")
    const [passwordError, setPasswordError] = useState("")
    const [passwordSuccess, setPasswordSuccess] = useState("")
    const [isChangingPassword, setIsChangingPassword] = useState(false)

    const handleLogout = async () => {
        try {
            await apiClient.post('/api/v1/auth/logout')
        } catch (error) {
            // Aunque falle la llamada, cerramos sesión localmente
            console.error('Error al cerrar sesión:', error)
        } finally {
            // Usar logout del contexto para limpiar estado correctamente
            logout()
            // Redirigir a home
            navigate('/')
        }
    }

    const handleChangePassword = async (e) => {
        e.preventDefault()
        setPasswordError("")
        setPasswordSuccess("")

        // Validar que las contraseñas nuevas coincidan
        if (newPassword !== confirmPassword) {
            setPasswordError("Las contraseñas nuevas no coinciden")
            return
        }

        // Validar longitud mínima
        if (newPassword.length < 6) {
            setPasswordError("La nueva contraseña debe tener al menos 6 caracteres")
            return
        }

        setIsChangingPassword(true)

        try {
            await apiClient.put('/api/v1/users/me/password', {
                currentPassword: currentPassword,
                newPassword: newPassword
            })

            setPasswordSuccess("Contraseña actualizada correctamente")
            setCurrentPassword("")
            setNewPassword("")
            setConfirmPassword("")
        } catch (error) {
            setPasswordError(error.message || "Error al cambiar la contraseña")
        } finally {
            setIsChangingPassword(false)
        }
    }

    const handleDeleteAccount = async () => {
        setIsDeletingAccount(true)
        setDeleteError("")
        
        try {
            await apiClient.delete('/api/v1/users/me')
            
            // Mostrar mensaje de éxito
            setShowDeleteAccount(false)
            setShowAccountDeleted(true)
            
            // Redirigir a home después de 2 segundos
            setTimeout(() => {
                logout()
                navigate('/')
            }, 2000)
        } catch (error) {
            setDeleteError(error.message || "Error al eliminar la cuenta")
        } finally {
            setIsDeletingAccount(false)
        }
    }

    const handleToggleProfileVisibility = async () => {
        const newValue = !profileVisibility
        setProfileVisibility(newValue)
        
        try {
            await apiClient.put('/api/v1/users/me/visibility', {
                visibleEnListados: newValue
            })
        } catch (error) {
            // Revertir el cambio si falla
            setProfileVisibility(!newValue)
            console.error("Error al actualizar visibilidad:", error)
        }
    }

    const handleToggleEmailNotifications = () => {
        const newValue = !emailNotifications
        setEmailNotifications(newValue)
        // TODO: Guardar preferencia de notificaciones por email en la API
        console.log("Notificaciones por email:", newValue)
    }

    const handleTogglePushNotifications = () => {
        const newValue = !pushNotifications
        setPushNotifications(newValue)
        // TODO: Guardar preferencia de notificaciones push en la API
        console.log("Notificaciones push:", newValue)
    }

    const handleToggleTwoFactorAuth = () => {
        const newValue = !twoFactorAuth
        setTwoFactorAuth(newValue)
        // TODO: Activar/desactivar autenticación de dos factores en la API
        console.log("Autenticación de dos factores:", newValue)
    }

    return (
        <div className="settings-overlay">
            <div className="settings-modal">
                {/* Botón cerrar */}
                <button className="settings-close" onClick={onClose}>
                    ✕
                </button>

                <h1 className="settings-title">Configuración</h1>

                {/* Sección: Visibilidad del perfil */}
                <section className="settings-section">
                    <h2 className="settings-section__title">Visibilidad del perfil</h2>
                    <p className="settings-section__description">
                        Decide si tu perfil aparece en los listados públicos de la plataforma.
                    </p>
                    <div className="settings-toggle-row">
                        <span className="settings-toggle-label">
                            Mostrar perfil en listados públicos
                        </span>
                        <button 
                            className={`settings-toggle ${profileVisibility ? 'settings-toggle--active' : ''}`}
                            onClick={handleToggleProfileVisibility}
                        >
                            <span className="settings-toggle__slider"></span>
                        </button>
                    </div>
                </section>

                {/* Sección: Configuración de cuenta */}
                <section className="settings-section">
                    <h2 className="settings-section__title">Configuración de cuenta</h2>
                    
                    {/* Seguridad */}
                    <div className="settings-subsection">
                        <h3 className="settings-subsection__title">Seguridad</h3>
                        <div className="settings-toggle-row">
                            <span className="settings-toggle-label">
                                Autenticación de dos factores
                            </span>
                            <button 
                                className={`settings-toggle ${twoFactorAuth ? 'settings-toggle--active' : ''}`}
                                onClick={handleToggleTwoFactorAuth}
                            >
                                <span className="settings-toggle__slider"></span>
                            </button>
                        </div>
                    </div>

                    {/* Notificaciones */}
                    <div className="settings-subsection">
                        <h3 className="settings-subsection__title">Notificaciones</h3>
                        <div className="settings-toggle-row">
                            <span className="settings-toggle-label">
                                Notificaciones por email
                            </span>
                            <button 
                                className={`settings-toggle ${emailNotifications ? 'settings-toggle--active' : ''}`}
                                onClick={handleToggleEmailNotifications}
                            >
                                <span className="settings-toggle__slider"></span>
                            </button>
                        </div>
                        <div className="settings-toggle-row">
                            <span className="settings-toggle-label">
                                Notificaciones push
                            </span>
                            <button 
                                className={`settings-toggle ${pushNotifications ? 'settings-toggle--active' : ''}`}
                                onClick={handleTogglePushNotifications}
                            >
                                <span className="settings-toggle__slider"></span>
                            </button>
                        </div>
                    </div>

                    {/* Privacidad */}
                    <div className="settings-subsection">
                        <h3 className="settings-subsection__title">Privacidad</h3>
                        <p className="settings-subsection__text">
                            Gestiona quién puede ver tu actividad y tu información personal.
                        </p>
                    </div>
                </section>

                {/* Sección: Cambio de contraseña */}
                <section className="settings-section">
                    <h2 className="settings-section__title">Cambiar contraseña</h2>
                    <p className="settings-section__description">
                        Actualiza tu contraseña para mantener tu cuenta segura.
                    </p>
                    
                    <form className="settings-password-form" onSubmit={handleChangePassword}>
                        <div className="settings-form-group">
                            <label>Contraseña actual</label>
                            <input 
                                type="password" 
                                value={currentPassword}
                                onChange={(e) => setCurrentPassword(e.target.value)}
                                placeholder="Introduce tu contraseña actual"
                                required
                            />
                        </div>
                        <div className="settings-form-group">
                            <label>Nueva contraseña</label>
                            <input 
                                type="password" 
                                value={newPassword}
                                onChange={(e) => setNewPassword(e.target.value)}
                                placeholder="Introduce la nueva contraseña"
                                required
                            />
                        </div>
                        <div className="settings-form-group">
                            <label>Confirmar nueva contraseña</label>
                            <input 
                                type="password" 
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                                placeholder="Repite la nueva contraseña"
                                required
                            />
                        </div>
                        
                        {passwordError && (
                            <p className="settings-password-error">{passwordError}</p>
                        )}
                        {passwordSuccess && (
                            <p className="settings-password-success">{passwordSuccess}</p>
                        )}
                        
                        <button 
                            type="submit" 
                            className="settings-btn settings-btn--primary"
                            disabled={isChangingPassword}
                        >
                            {isChangingPassword ? 'Guardando...' : 'Cambiar contraseña'}
                        </button>
                    </form>
                </section>

                {/* Sección: Acciones de cuenta */}
                <section className="settings-section settings-section--danger">
                    <h2 className="settings-section__title">Zona de peligro</h2>
                    
                    <div className="settings-action-row">
                        <div>
                            <h3 className="settings-action-title">Cerrar sesión</h3>
                            <p className="settings-action-description">
                                Cierra tu sesión en este dispositivo.
                            </p>
                        </div>
                        <button 
                            className="settings-btn settings-btn--outline"
                            onClick={handleLogout}
                        >
                            Cerrar sesión
                        </button>
                    </div>

                    <div className="settings-action-row">
                        <div>
                            <h3 className="settings-action-title">Eliminar cuenta</h3>
                            <p className="settings-action-description">
                                Esta acción es irreversible. Todos tus datos serán eliminados permanentemente.
                            </p>
                        </div>
                        <button 
                            className="settings-btn settings-btn--danger"
                            onClick={() => setShowDeleteAccount(true)}
                        >
                            Eliminar cuenta
                        </button>
                    </div>
                </section>
            </div>

            {/* Modal: Confirmar eliminación de cuenta */}
            {showDeleteAccount && (
                <div className="settings-confirm-overlay">
                    <div className="settings-confirm-modal">
                        <h2 className="settings-confirm-title">¿Eliminar cuenta?</h2>
                        <p className="settings-confirm-text">
                            Esta acción no se puede deshacer. Se eliminarán todos tus datos, 
                            comunidades creadas y contenido subido de forma permanente.
                        </p>
                        {deleteError && (
                            <p className="settings-password-error">{deleteError}</p>
                        )}
                        <div className="settings-confirm-actions">
                            <button 
                                className="settings-btn settings-btn--outline"
                                onClick={() => setShowDeleteAccount(false)}
                                disabled={isDeletingAccount}
                            >
                                Cancelar
                            </button>
                            <button 
                                className="settings-btn settings-btn--danger"
                                onClick={handleDeleteAccount}
                                disabled={isDeletingAccount}
                            >
                                {isDeletingAccount ? 'Eliminando...' : 'Sí, eliminar mi cuenta'}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Modal: Cuenta eliminada correctamente */}
            {showAccountDeleted && (
                <div className="settings-confirm-overlay">
                    <div className="settings-confirm-modal settings-confirm-modal--success">
                        <div className="settings-success-icon">✓</div>
                        <h2 className="settings-confirm-title">Cuenta eliminada</h2>
                        <p className="settings-confirm-text">
                            Tu cuenta ha sido eliminada correctamente. Serás redirigido a la página principal.
                        </p>
                    </div>
                </div>
            )}
        </div>
    )
}

export default Settings

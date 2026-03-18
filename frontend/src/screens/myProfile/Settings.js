import { useEffect, useState } from "react"
import { useNavigate } from "react-router-dom"
import axiosInstance from "../../api/axiosConfig"
import { apiClient } from "../../api/client"
import { useAuth } from "../../contexts/AuthContext"
import "./Settings.css"

const EVENT_TYPES = [
    { value: 'REUNION', label: 'Reuniones' },
    { value: 'EXAMEN', label: 'Exámenes' },
    { value: 'CUESTIONARIO', label: 'Cuestionarios' },
    { value: 'TUTORIA', label: 'Tutorías' },
    { value: 'CLASE', label: 'Clases' },
    { value: 'OTRO', label: 'Otros' },
]

const Settings = ({ onClose, isOwner = true, calendarNotification, onCalendarNotificationRead }) => {
    const navigate = useNavigate()
    const { logout, user, updateProfile } = useAuth()

    // Estados para los toggles y configuraciones
    const [profileVisibility, setProfileVisibility] = useState(true)
    const [pushNotifications, setPushNotifications] = useState(false)
    const [twoFactorAuth, setTwoFactorAuth] = useState(false)
    const [isSavingPreferences, setIsSavingPreferences] = useState(false)
    const [preferencesError, setPreferencesError] = useState("")

    // Recordatorios por email + canal alarmas por defecto
    const [emailRecordatorios, setEmailRecordatorios] = useState({
        emailsActivados: true,
        recordatorio24h: true,
        recordatorio1h: true,
        recordatorio30min: false,
        canalAlarmasPorDefecto: 'AMBOS',
    })
    const [isSavingRecordatorios, setIsSavingRecordatorios] = useState(false)
    const [recordatoriosError, setRecordatoriosError] = useState("")

    // Google Calendar
    const [calendarStatus, setCalendarStatus] = useState({ conectado: false, sincronizacionActiva: false, tiposSincronizados: [] })
    const [isLoadingCalendar, setIsLoadingCalendar] = useState(true)
    const [calendarMsg, setCalendarMsg] = useState(null)
    const [isConnecting, setIsConnecting] = useState(false)
    const [isDisconnecting, setIsDisconnecting] = useState(false)
    const [isSavingCalendar, setIsSavingCalendar] = useState(false)

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

    // Estado para mensajes de acceso no autorizado
    const [unauthorizedMessage] = useState("")

    useEffect(() => {
        if (!user) {
            return
        }

        setProfileVisibility(user.visibleEnListados ?? true)
        setPushNotifications(user.notificacionesPush ?? false)
        setTwoFactorAuth(user.autenticacionDosFactores ?? false)
    }, [user])

    useEffect(() => {
        axiosInstance.get('/api/v1/notifications/preferences')
            .then(res => setEmailRecordatorios(res.data))
            .catch(() => { /* usa valores por defecto */ })
    }, [])

    useEffect(() => {
        setIsLoadingCalendar(true)
        axiosInstance.get('/api/v1/google-calendar/status')
            .then(res => setCalendarStatus(res.data))
            .catch(() => { /* usa valores por defecto */ })
            .finally(() => setIsLoadingCalendar(false))
    }, [])

    useEffect(() => {
        if (!calendarNotification) return
        if (calendarNotification === 'success') {
            setCalendarMsg({ type: 'success', text: 'Google Calendar conectado correctamente.' })
        } else {
            setCalendarMsg({ type: 'error', text: 'No se pudo conectar Google Calendar. Inténtalo de nuevo.' })
        }
        onCalendarNotificationRead?.()
    }, [calendarNotification]) // eslint-disable-line

    const handleConnectCalendar = async () => {
        setIsConnecting(true)
        setCalendarMsg(null)
        try {
            const res = await axiosInstance.get('/api/v1/google-calendar/auth-url')
            window.location.href = res.data
        } catch {
            setCalendarMsg({ type: 'error', text: 'No se pudo obtener la URL de autorización.' })
            setIsConnecting(false)
        }
    }

    const handleDisconnectCalendar = async () => {
        setIsDisconnecting(true)
        setCalendarMsg(null)
        try {
            await axiosInstance.delete('/api/v1/google-calendar/disconnect')
            setCalendarStatus({ conectado: false, sincronizacionActiva: false, tiposSincronizados: [] })
            setCalendarMsg({ type: 'success', text: 'Google Calendar desconectado.' })
        } catch {
            setCalendarMsg({ type: 'error', text: 'No se pudo desconectar. Inténtalo de nuevo.' })
        } finally {
            setIsDisconnecting(false)
        }
    }

    const saveCalendarPreferences = async (partial) => {
        setIsSavingCalendar(true)
        try {
            const res = await axiosInstance.put('/api/v1/google-calendar/preferences', partial)
            setCalendarStatus(res.data)
        } catch {
            setCalendarMsg({ type: 'error', text: 'No se pudo guardar la preferencia.' })
        } finally {
            setIsSavingCalendar(false)
        }
    }

    const handleToggleSyncActive = () => {
        if (isSavingCalendar) return
        const newVal = !calendarStatus.sincronizacionActiva
        setCalendarStatus(prev => ({ ...prev, sincronizacionActiva: newVal }))
        saveCalendarPreferences({ sincronizacionActiva: newVal })
    }

    const handleToggleEventType = (tipo) => {
        if (isSavingCalendar) return
        const current = calendarStatus.tiposSincronizados || []
        // Si todos seleccionados (lista vacía = todos), al desmarcar uno → seleccionar todos excepto ese
        const allSelected = current.length === 0
        const base = allSelected ? EVENT_TYPES.map(t => t.value) : current
        const updated = base.includes(tipo) ? base.filter(t => t !== tipo) : [...base, tipo]
        // Si todos quedan seleccionados, mandar lista vacía (= todos)
        const toSend = updated.length === EVENT_TYPES.length ? [] : updated
        setCalendarStatus(prev => ({ ...prev, tiposSincronizados: toSend }))
        saveCalendarPreferences({ tiposSincronizados: toSend })
    }

    const saveRecordatorios = async (partial) => {
        setRecordatoriosError("")
        setIsSavingRecordatorios(true)
        const updated = { ...emailRecordatorios, ...partial }
        try {
            const res = await axiosInstance.put('/api/v1/notifications/preferences', updated)
            setEmailRecordatorios(res.data)
        } catch {
            setRecordatoriosError("No se pudo guardar la preferencia. Inténtalo de nuevo.")
            // revert
            setEmailRecordatorios(prev => ({ ...prev }))
        } finally {
            setIsSavingRecordatorios(false)
        }
    }

    const handleToggleRecordatorio = async (field, explicitValue) => {
        if (isSavingRecordatorios) return
        const newVal = explicitValue !== undefined ? explicitValue : !emailRecordatorios[field]
        setEmailRecordatorios(prev => ({ ...prev, [field]: newVal }))
        await saveRecordatorios({ [field]: newVal })
    }

    const savePreferences = async (partial) => {
        setPreferencesError("")
        setIsSavingPreferences(true)

        try {
            const result = await updateProfile(partial)
            if (!result.success) {
                throw new Error(result.error || 'No se pudo guardar la preferencia')
            }
            return true
        } catch (error) {
            setPreferencesError(error.message || "Error al guardar la configuración")
            return false
        } finally {
            setIsSavingPreferences(false)
        }
    }

    const handleChangePassword = async (e) => {
        e.preventDefault()
        setPasswordError("")
        setPasswordSuccess("")

        // Validar que sea el propietario
        if (!isOwner) {
            setPasswordError("No puedes cambiar la contraseña de una cuenta que no es tuya.")
            return
        }

        // Validar que la nueva contraseña no sea la misma que la anterior
        if (newPassword.trim() === currentPassword.trim()) {
            setPasswordError("La contraseña nueva no puede ser igual a la anterior")
            return
        }

        // Validar que las contraseñas nuevas coincidan
        if (newPassword.trim() !== confirmPassword.trim()) {
            setPasswordError("Las contraseñas nuevas no coinciden")
            return
        }

        // Validar longitud mínima (debe coincidir con el backend: 8 caracteres)
        if (newPassword.trim().length < 8) {
            setPasswordError("La nueva contraseña debe tener al menos 8 caracteres")
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
        // Validar que sea el propietario
        if (!isOwner) {
            setDeleteError("No puedes eliminar una cuenta que no es tuya.")
            return
        }

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
        if (isSavingPreferences) {
            return
        }

        const newValue = !profileVisibility
        setProfileVisibility(newValue)

        const ok = await savePreferences({ visibleEnListados: newValue })
        if (!ok) {
            setProfileVisibility(!newValue)
        }
    }

    const handleTogglePushNotifications = async () => {
        if (isSavingPreferences) {
            return
        }

        const newValue = !pushNotifications
        setPushNotifications(newValue)

        const ok = await savePreferences({ notificacionesPush: newValue })
        if (!ok) {
            setPushNotifications(!newValue)
        }
    }

    const handleToggleTwoFactorAuth = async () => {
        if (isSavingPreferences) {
            return
        }

        const newValue = !twoFactorAuth
        setTwoFactorAuth(newValue)

        const ok = await savePreferences({ autenticacionDosFactores: newValue })
        if (!ok) {
            setTwoFactorAuth(!newValue)
        }
    }

    return (
        <div className="settings-overlay">
            <div className="settings-modal">
                {/* Botón cerrar */}
                <button className="settings-close" onClick={onClose}>
                    ✕
                </button>

                <h1 className="settings-title">Configuración</h1>

                {/* Mensaje de acceso no autorizado */}
                {unauthorizedMessage && (
                    <div className="settings-unauthorized-message">
                        ⚠️ {unauthorizedMessage}
                    </div>
                )}
                {preferencesError && (
                    <div className="settings-preferences-error">{preferencesError}</div>
                )}

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
                            disabled={isSavingPreferences}
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
                                disabled={isSavingPreferences}
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
                                Notificaciones push
                            </span>
                            <button
                                className={`settings-toggle ${pushNotifications ? 'settings-toggle--active' : ''}`}
                                onClick={handleTogglePushNotifications}
                                disabled={isSavingPreferences}
                            >
                                <span className="settings-toggle__slider"></span>
                            </button>
                        </div>
                    </div>

                    {/* Recordatorios por email de eventos */}
                    <div className="settings-subsection">
                        <h3 className="settings-subsection__title">✉️ Recordatorios por email de eventos</h3>
                        <p className="settings-subsection__text" style={{ marginBottom: '12px' }}>
                            Recibe un email antes de que comiencen tus eventos.
                        </p>
                        {recordatoriosError && (
                            <p className="settings-password-error" style={{ marginBottom: '8px' }}>{recordatoriosError}</p>
                        )}
                        <div className="settings-toggle-row">
                            <span className="settings-toggle-label" style={{ fontWeight: 600 }}>
                                Activar recordatorios por email
                            </span>
                            <button
                                className={`settings-toggle ${emailRecordatorios.emailsActivados ? 'settings-toggle--active' : ''}`}
                                onClick={() => handleToggleRecordatorio('emailsActivados')}
                                disabled={isSavingRecordatorios}
                            >
                                <span className="settings-toggle__slider"></span>
                            </button>
                        </div>
                        <p className="settings-subsection__text" style={{ margin: '4px 0 8px' }}>
                            Recordarme antes de cada evento:
                        </p>
                        <div className="settings-recordatorios-sub">
                            <div className="settings-toggle-row">
                                <span className={`settings-toggle-label ${!emailRecordatorios.emailsActivados ? 'settings-toggle-label--disabled' : ''}`}>
                                    24 horas antes
                                </span>
                                <button
                                    className={`settings-toggle ${emailRecordatorios.recordatorio24h ? 'settings-toggle--active' : ''}`}
                                    onClick={() => handleToggleRecordatorio('recordatorio24h')}
                                    disabled={isSavingRecordatorios || !emailRecordatorios.emailsActivados}
                                >
                                    <span className="settings-toggle__slider"></span>
                                </button>
                            </div>
                            <div className="settings-toggle-row">
                                <span className={`settings-toggle-label ${!emailRecordatorios.emailsActivados ? 'settings-toggle-label--disabled' : ''}`}>
                                    1 hora antes
                                </span>
                                <button
                                    className={`settings-toggle ${emailRecordatorios.recordatorio1h ? 'settings-toggle--active' : ''}`}
                                    onClick={() => handleToggleRecordatorio('recordatorio1h')}
                                    disabled={isSavingRecordatorios || !emailRecordatorios.emailsActivados}
                                >
                                    <span className="settings-toggle__slider"></span>
                                </button>
                            </div>
                            <div className="settings-toggle-row">
                                <span className={`settings-toggle-label ${!emailRecordatorios.emailsActivados ? 'settings-toggle-label--disabled' : ''}`}>
                                    30 minutos antes
                                </span>
                                <button
                                    className={`settings-toggle ${emailRecordatorios.recordatorio30min ? 'settings-toggle--active' : ''}`}
                                    onClick={() => handleToggleRecordatorio('recordatorio30min')}
                                    disabled={isSavingRecordatorios || !emailRecordatorios.emailsActivados}
                                >
                                    <span className="settings-toggle__slider"></span>
                                </button>
                            </div>
                        </div>
                    </div>

                    {/* Alarmas personalizadas — canal por defecto */}
                    <div className="settings-subsection">
                        <h3 className="settings-subsection__title">🔔 Alarmas personalizadas</h3>
                        <p className="settings-subsection__text" style={{ marginBottom: '12px' }}>
                            Canal por defecto para tus alarmas de eventos.
                        </p>
                        {recordatoriosError && (
                            <p className="settings-password-error" style={{ marginBottom: '8px' }}>{recordatoriosError}</p>
                        )}
                        <div className="settings-canal-radios">
                            {[
                                { value: 'PLATAFORMA', label: 'Solo en la app' },
                                { value: 'EMAIL', label: 'Solo por email' },
                                { value: 'AMBOS', label: 'Ambos' },
                            ].map(({ value, label }) => (
                                <label key={value} className="settings-canal-radio-label">
                                    <input
                                        type="radio"
                                        name="canalAlarmasPorDefecto"
                                        value={value}
                                        checked={emailRecordatorios.canalAlarmasPorDefecto === value}
                                        onChange={() => handleToggleRecordatorio('canalAlarmasPorDefecto', value)}
                                        disabled={isSavingRecordatorios}
                                    />
                                    {label}
                                </label>
                            ))}
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

                {/* Sección: Google Calendar */}
                <section className="settings-section">
                    <h2 className="settings-section__title">📅 Google Calendar</h2>
                    <p className="settings-section__description">
                        Sincroniza automáticamente los eventos de la plataforma con tu Google Calendar.
                    </p>

                    {calendarMsg && (
                        <div className={calendarMsg.type === 'success' ? 'settings-password-success' : 'settings-password-error'} style={{ marginBottom: '16px' }}>
                            {calendarMsg.text}
                        </div>
                    )}

                    {isLoadingCalendar ? (
                        <p className="settings-subsection__text">Cargando estado...</p>
                    ) : !calendarStatus.conectado ? (
                        <button
                            className="settings-btn settings-btn--gcalendar"
                            onClick={handleConnectCalendar}
                            disabled={isConnecting}
                        >
                            {isConnecting ? 'Redirigiendo...' : '🔗 Conectar Google Calendar'}
                        </button>
                    ) : (
                        <div className="settings-subsection">
                            <div className="settings-toggle-row">
                                <span className="settings-toggle-label" style={{ fontWeight: 600 }}>
                                    Sincronización automática activa
                                </span>
                                <button
                                    className={`settings-toggle ${calendarStatus.sincronizacionActiva ? 'settings-toggle--active' : ''}`}
                                    onClick={handleToggleSyncActive}
                                    disabled={isSavingCalendar}
                                >
                                    <span className="settings-toggle__slider"></span>
                                </button>
                            </div>

                            <p className="settings-subsection__text" style={{ margin: '12px 0 8px' }}>
                                Sincronizar tipos de evento:
                            </p>
                            <div className="settings-gcalendar-types">
                                {EVENT_TYPES.map(({ value, label }) => {
                                    const allSelected = !calendarStatus.tiposSincronizados || calendarStatus.tiposSincronizados.length === 0
                                    const isSelected = allSelected || calendarStatus.tiposSincronizados.includes(value)
                                    return (
                                        <label key={value} className="settings-gcalendar-type-label">
                                            <input
                                                type="checkbox"
                                                checked={isSelected}
                                                onChange={() => handleToggleEventType(value)}
                                                disabled={isSavingCalendar}
                                            />
                                            {label}
                                        </label>
                                    )
                                })}
                            </div>

                            {calendarStatus.ultimaSincronizacion && (
                                <p className="settings-subsection__text" style={{ marginTop: '12px' }}>
                                    Última sincronización: {new Date(calendarStatus.ultimaSincronizacion).toLocaleString('es-ES')}
                                </p>
                            )}

                            <button
                                className="settings-btn settings-btn--outline"
                                style={{ marginTop: '16px' }}
                                onClick={handleDisconnectCalendar}
                                disabled={isDisconnecting}
                            >
                                {isDisconnecting ? 'Desconectando...' : 'Desconectar Google Calendar'}
                            </button>
                        </div>
                    )}
                </section>

                {/* Sección: Eliminación de cuenta */}
                <section className="settings-section settings-section--danger">
                    <h2 className="settings-section__title">Zona de peligro</h2>

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
                            Esta acción no se puede deshacer. 
                            ¿Qué pasará si elimino mi cuenta?
                        </p>
                        <ul className="settings-confirm-list">
                            <li>Se eliminará tu perfil y toda tu información personal.</li>
                            <li>Si has creado alguna comunidad, esta permanecerá pero se le asignará otro creador en caso de que haya miembros o se eliminará permanentemente.</li>
                            <li>Tu asistencia a eventos será cancelada.</li>
                            <li>Si has creado algún evento, será eliminado.</li>
                            <li>Tus solicitudes para unirte a una comunidad serán eliminados.</li>
                            <li>Tus suscripciones serán canceladas.</li>
                            <li>Tus membresías a comunidades serán canceladas.</li>
                        </ul>
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

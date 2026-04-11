import "./BecomeTutorInfoModal.css";

const BecomeTutorInfoModal = ({ onClose, onContinue }) => {
    return (
        <div className="tutor-modal-overlay">
            <div className="tutor-modal">

                {/* HEADER */}
                <div className="tutor-modal__header">
                    <div>
                        <h2>Conviértete en profesor</h2>
                        <p>Empieza a enseñar, ganar dinero y crecer en la plataforma</p>
                    </div>
                    <button onClick={onClose} className="tutor-modal__close">✕</button>
                </div>

                {/* HERO */}
                <div className="tutor-modal__hero">
                    <div className="tutor-hero-card">
                        <span>💰</span>
                        <h4>Gana dinero</h4>
                        <p>Cobra por tus clases sin intermediarios</p>
                    </div>
                    <div className="tutor-hero-card">
                        <span>🎓</span>
                        <h4>Enseña</h4>
                        <p>Comparte tu conocimiento en comunidades</p>
                    </div>
                    <div className="tutor-hero-card">
                        <span>📅</span>
                        <h4>Organízate</h4>
                        <p>Gestiona tu calendario y disponibilidad</p>
                    </div>
                </div>

                {/* CONTENIDO */}
                <div className="tutor-modal__content">

                    <div className="tutor-feature">
                        <span className="icon">👤</span>
                        <div>
                            <h3>Perfil profesional</h3>
                            <p>Configura tus especialidades, precios y disponibilidad.</p>
                        </div>
                    </div>

                    <div className="tutor-feature">
                        <span className="icon">⭐</span>
                        <div>
                            <h3>Verificación y visibilidad</h3>
                            <p>Consigue el badge ✓ y destaca en búsquedas.</p>
                        </div>
                    </div>

                    <div className="tutor-feature">
                        <span className="icon">💬</span>
                        <div>
                            <h3>Contacto directo</h3>
                            <p>Los alumnos pueden escribirte y contratarte fácilmente.</p>
                        </div>
                    </div>

                    <div className="tutor-feature">
                        <span className="icon">🏫</span>
                        <div>
                            <h3>Clases y comunidades</h3>
                            <p>Da clases, crea contenido y trabaja con comunidades.</p>
                        </div>
                    </div>

                </div>

                {/* FOOTER */}
                <div className="tutor-modal__footer">
                    <button className="btn-secondary" onClick={onClose}>
                        Cancelar
                    </button>
                    <button className="btn-primary" onClick={onContinue}>
                        Crear perfil de profesor
                    </button>
                </div>

            </div>
        </div>
    );
};

export default BecomeTutorInfoModal;
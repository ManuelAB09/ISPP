import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { getApiBaseUrl } from '../../api/baseUrl';
import GoogleClassroomButton from '../GoogleClassroomButton/GoogleClassroomButton.jsx';
import PlanExpiryBanner from '../PlanExpiryBanner/PlanExpiryBanner';
import { useSubscriptionExpiry } from '../../hooks/useSubscriptionExpiry';
import { useNotificationContext } from '../../contexts/NotificationContext';
import './Header.css';

const DEFAULT_PROFILE_AVATAR =
    "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 120 120'%3E%3Ccircle cx='60' cy='60' r='60' fill='%23E6EAF3'/%3E%3Ccircle cx='60' cy='46' r='22' fill='%2395A1BB'/%3E%3Cpath d='M20 106c6-20 22-32 40-32s34 12 40 32' fill='%2395A1BB'/%3E%3C/svg%3E";

const toAbsoluteImageUrl = (imageUrl, fallback = DEFAULT_PROFILE_AVATAR) => {
    const raw = String(imageUrl || '').trim();
    if (!raw) {
        return fallback;
    }
    if (/^https?:\/\//i.test(raw) || raw.startsWith('data:') || raw.startsWith('blob:')) {
        return raw;
    }

    const base = getApiBaseUrl();
    return raw.startsWith('/') ? `${base}${raw}` : `${base}/${raw}`;
};

export default function Header({ user, page }) {
    const [isMenuOpen, setIsMenuOpen] = useState(false);
    const { showBanner, planName, fechaFin, dismiss } = useSubscriptionExpiry();
    const { communityUnreadById } = useNotificationContext();
    // Calcular total de no leídos de chats privados y comunidades
    const [privateUnread, setPrivateUnread] = useState(0);
    useEffect(() => {
        // Obtener de localStorage o API si es necesario, aquí solo ejemplo simple
        const conversaciones = JSON.parse(localStorage.getItem('conversacionesNoLeidas') || '[]');
        setPrivateUnread(conversaciones.reduce((acc, c) => acc + (c.noLeidos || 0), 0));
    }, []);
    const communityUnread = Object.values(communityUnreadById || {}).reduce((acc, n) => acc + (n || 0), 0);
    const totalChatsUnread = privateUnread + communityUnread;

    const toggleMenu = () => {
        setIsMenuOpen(!isMenuOpen);
    };

    const closeMenu = () => {
        setIsMenuOpen(false);
    };
    const storedUser = (() => {
        try {
            return JSON.parse(localStorage.getItem('userProfile') || 'null');
        } catch {
            return null;
        }
    })();

    const isAuthenticated = Boolean(localStorage.getItem('accessToken'));

    let profileImage =
        user?.avatar ||
        user?.foto ||
        storedUser?.avatar ||
        storedUser?.foto ||
        '';
    profileImage = toAbsoluteImageUrl(profileImage, DEFAULT_PROFILE_AVATAR);

    const profileBackgroundColor =
        user?.fotoBackgroundColor ||
        storedUser?.fotoBackgroundColor ||
        '#ffffff';

    return (
        <>
            <div className="header-container">
                {isAuthenticated ? (
                    <Link to="/perfil" className="header-profile-link">
                        <img
                            className="header-profile-image"
                            src={profileImage || DEFAULT_PROFILE_AVATAR}
                            alt="Perfil"
                            style={{ backgroundColor: profileBackgroundColor }}
                            onError={e => { e.target.onerror = null; e.target.src = DEFAULT_PROFILE_AVATAR; }}
                        />
                    </Link>
                ) : (
                    <Link to="/login" className="header-profile-link">
                        <img
                            className="header-profile-image"
                            src={DEFAULT_PROFILE_AVATAR}
                            alt="Iniciar sesión"
                        />
                    </Link>
                )}

                <div className="header-actions-desktop">
                    <GoogleClassroomButton />
                </div>

                <div className="header-links-desktop">
                    <Link to="/" className={page === 'inicio' ? 'active' : ''}>Inicio</Link>
                    <Link to="/comunidades" className={page === 'comunidades' ? 'active' : ''}>Comunidades</Link>
                    <Link to="/profesores" className={page === 'profesores' ? 'active' : ''}>Profesores</Link>
                    <Link to="/cuestionarios" className={page === 'cuestionarios' ? 'active' : ''}>Cuestionarios</Link>
                    <Link to="/chats" className={page === 'chats' ? 'active' : ''}>
                        Chats
                        {totalChatsUnread > 0 && (
                            <span className="header-notification-badge">{totalChatsUnread}</span>
                        )}
                    </Link>
                    <Link to="/planes" className={page === 'planes' ? 'active' : ''}>Planes</Link>
                    {!isAuthenticated && (
                        <Link to="/login">Iniciar sesión</Link>
                    )}
                </div>

                <button
                    className={`header-hamburger ${isMenuOpen ? 'open' : ''}`}
                    onClick={toggleMenu}
                    aria-label="Menú"
                >
                    <span></span>
                    <span></span>
                    <span></span>
                </button>
            </div>

            {showBanner && (
                <PlanExpiryBanner planName={planName} fechaFin={fechaFin} onDismiss={dismiss} />
            )}

            {/* Modal móvil */}
            <div className={`header-menu-overlay ${isMenuOpen ? 'open' : ''}`} onClick={closeMenu}></div>
            <div className={`header-menu-sidebar ${isMenuOpen ? 'open' : ''}`}>
                <div className="header-menu-header">
                    <button className="header-menu-close" onClick={closeMenu}>✕</button>
                </div>
                <div className="header-actions-mobile">
                    <GoogleClassroomButton />
                </div>
                <div className="header-links-mobile">
                    <Link to="/" className={page === 'inicio' ? 'active' : ''} onClick={closeMenu}>Inicio</Link>
                    <Link to="/comunidades" className={page === 'comunidades' ? 'active' : ''} onClick={closeMenu}>Comunidades</Link>
                    <Link to="/profesores" className={page === 'profesores' ? 'active' : ''} onClick={closeMenu}>Profesores</Link>
                    <Link to="/cuestionarios" className={page === 'cuestionarios' ? 'active' : ''} onClick={closeMenu}>Cuestionarios</Link>
                    <Link to="/chats" className={page === 'chats' ? 'active' : ''} onClick={closeMenu}>
                        Chats
                        {totalChatsUnread > 0 && (
                            <span className="header-notification-badge">{totalChatsUnread}</span>
                        )}
                    </Link>
                    <Link to="/planes" className={page === 'planes' ? 'active' : ''} onClick={closeMenu}>Planes</Link>
                    {!isAuthenticated && (
                        <Link to="/login" onClick={closeMenu}>Iniciar sesión</Link>
                    )}
                </div>
            </div>
        </>
    );
}



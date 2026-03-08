import { Link } from "react-router-dom";
import { getApiBaseUrl } from '../../api/baseUrl';
import './Header.css';
import GoogleClassroomButton from '../GoogleClassroomButton/GoogleClassroomButton.jsx';

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
    const storedUser = (() => {
        try {
            return JSON.parse(localStorage.getItem('userProfile') || 'null');
        } catch {
            return null;
        }
    })();

    const profileImage = toAbsoluteImageUrl(
        user?.avatar ||
        user?.foto ||
        storedUser?.avatar ||
        storedUser?.foto ||
        DEFAULT_PROFILE_AVATAR
    );

    return (
        <div className="header-container">
            <Link to="/perfil">
                <img className="header-profile-image" src={profileImage} alt="Perfil" />
            </Link>
                        <div className="header-actions">
                <GoogleClassroomButton />
            </div>
            <div className="header-links">
                <Link to="/" className={page === 'inicio' ? 'active' : ''}>Inicio</Link>
                <Link to="/comunidades" className={page === 'comunidades' ? 'active' : ''}>Comunidades</Link>
                <Link to="/eventos-mapa" className={page === 'eventos-mapa' ? 'active' : ''}>Mapa de eventos</Link>
                <Link to="/profesores" className={page === 'profesores' ? 'active' : ''}>Profesores</Link>
                <Link to="/chats" className={page === 'chats' ? 'active' : ''}>Chats</Link>
                <Link to="/planes" className={page === 'planes' ? 'active' : ''}>Planes</Link>
                <Link to="/pagos" className={page === 'pagos' ? 'active' : ''}>Mis pagos</Link>
            </div>
        </div>
    );
}

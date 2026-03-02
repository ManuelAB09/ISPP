import { Link } from "react-router-dom";
import './Header.css';
import GoogleClassroomButton from '../GoogleClassroomButton/GoogleClassroomButton';

export default function Header({ user, page }) {
    const storedUser = (() => {
        try {
            return JSON.parse(localStorage.getItem('userProfile') || 'null');
        } catch {
            return null;
        }
    })();

    const profileImage =
        user?.avatar ||
        user?.foto ||
        storedUser?.avatar ||
        storedUser?.foto ||
        '/MeerKatters_logo.png';

    return (
        <div className="header-container">
            <Link to="/perfil">
                <img src={profileImage} alt="Perfil" />
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
            </div>
        </div>
    );
}

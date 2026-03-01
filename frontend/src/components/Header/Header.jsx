import { Link } from "react-router-dom";
import './Header.css';

export default function Header({ user, page }) {
    return (
        <div className="header-container">
            <Link to="/perfil">
                <img src={user?.avatar || '/logo192.png'} alt="" />
            </Link>
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

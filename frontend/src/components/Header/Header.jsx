import { Link } from "react-router-dom";
import './Header.css';

export default function Header({ user, page }) {
    return (
        <div className="header-container">
            <Link to="/perfil">
                <img src={user?.avatar || 'https://via.placeholder.com/150'} alt="" />
            </Link>
            <div className="header-links">
                <Link to="/" className={page === 'inicio' ? 'active' : ''}>Inicio</Link>
                <Link to="/comunidades" className={page === 'comunidades' ? 'active' : ''}>Comunidades</Link>
                <Link to="/profesores" className={page === 'profesores' ? 'active' : ''}>Profesores</Link>
                <Link to="/chats" className={page === 'chats' ? 'active' : ''}>Chats</Link>
                <Link to="/planes" className={page === 'planes' ? 'active' : ''}>Planes</Link>
                <Link to="/pagos" className={page === 'pagos' ? 'active' : ''}>Mis pagos</Link>
            </div>
        </div>
    );
}

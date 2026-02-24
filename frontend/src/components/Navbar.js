import React from 'react';
import './Navbar.css';

const Navbar = ({ avatarUrl = "https://i.pravatar.cc/150?img=11" }) => {
  return (
    <nav className="navbar top-bg-white">
      <div className="avatar">
        <img src={avatarUrl} alt="Avatar del usuario" />
      </div>
      <div className="nav-links">
        <a href="/">Inicio</a>
        <a href="/comunidades" className="active">Comunidades</a>
        <a href="/profesores">Profesores</a>
        <a href="/chats">Chats</a>
        <a href="/planes">Planes</a>
      </div>
    </nav>
  );
};

export default Navbar;
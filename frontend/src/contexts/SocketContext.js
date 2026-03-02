import React, { createContext, useContext } from 'react';
import { useSocket } from '../hooks/useSocket';

/**
 * Contexto para compartir la conexión Socket.IO entre componentes.
 */
const SocketContext = createContext(null);

/**
 * Proveedor de Socket.IO para la aplicación.
 * Envuelve el árbol de componentes para proporcionar acceso al socket.
 * @param {object} props - React props.
 * @param {React.ReactNode} props.children - Componentes hijos.
 * @param {string} props.token - Token JWT del usuario autenticado.
 */
export const SocketProvider = ({ children, token }) => {
    const { socket, isConnected } = useSocket(token);

    return (
        <SocketContext.Provider value={{ socket, isConnected }}>
            {children}
        </SocketContext.Provider>
    );
};

/**
 * Hook para acceder al contexto de Socket.IO desde cualquier componente.
 * @returns {object} Socket y estado de conexión.
 */
export const useSocketContext = () => {
    const context = useContext(SocketContext);
    if (!context) {
        throw new Error('useSocketContext debe usarse dentro de <SocketProvider>');
    }
    return context;
};

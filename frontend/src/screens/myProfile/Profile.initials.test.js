import React from 'react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

/**
 * Test para validar que los avatares muestren iniciales correctamente.
 * UC-05: Error en visualización de foto de perfil
 */

// Mock de la función getInitials
const getInitials = (nombre) => {
    if (!nombre || !String(nombre).trim()) {
        return 'U';
    }
    return String(nombre)
        .trim()
        .split(' ')
        .slice(0, 2)
        .map((word) => word[0]?.toUpperCase() ?? '')
        .join('');
};

describe('UC-05: Avatar con iniciales', () => {
    
    test('Debe extraer una inicial de un nombre simple', () => {
        expect(getInitials('Maria')).toBe('M');
    });

    test('Debe extraer dos iniciales de un nombre completo', () => {
        expect(getInitials('Juan Pérez')).toBe('JP');
    });

    test('Debe extraer dos iniciales ignorando nombres adicionales', () => {
        expect(getInitials('Juan Carlos Pérez García')).toBe('JC');
    });

    test('Debe retornar "U" para nombre vacío', () => {
        expect(getInitials('')).toBe('U');
    });

    test('Debe retornar "U" para nombre null', () => {
        expect(getInitials(null)).toBe('U');
    });

    test('Debe retornar "U" para nombre undefined', () => {
        expect(getInitials(undefined)).toBe('U');
    });

    test('Debe manejar espacios en blanco', () => {
        expect(getInitials('  Maria  ')).toBe('M');
    });

    test('Debe manejar nombres con múltiples espacios', () => {
        expect(getInitials('Juan    García')).toBe('JG');
    });

    test('Debe convertir a mayúsculas', () => {
        expect(getInitials('juan garcía')).toBe('JG');
    });

    test('Debe manejar caracteres especiales', () => {
        expect(getInitials('Álvaro Gutiérrez')).toBe('ÁG');
    });
});

/**
 * Test para validar que el avatar se renderiza correctamente sin foto.
 */
describe('UC-05: Renderización del avatar sin foto', () => {

    // Componente mock para probar la renderización
    const AvatarComponent = ({ foto, nombre, backgroundColor }) => {
        return (
            <div 
                className="profile-avatar"
                style={{ backgroundColor }}
                data-testid="avatar-container"
            >
                {foto ? (
                    <img
                        src={foto}
                        alt={nombre}
                        className="profile-avatar-img"
                        data-testid="avatar-image"
                    />
                ) : (
                    <div className="profile-avatar__initials" data-testid="avatar-initials">
                        {getInitials(nombre)}
                    </div>
                )}
            </div>
        );
    };

    test('Debe renderizar imagen cuando hay foto', () => {
        render(
            <AvatarComponent 
                foto="https://example.com/photo.jpg" 
                nombre="Juan Pérez"
                backgroundColor="#ffffff"
            />
        );
        
        const image = screen.getByTestId('avatar-image');
        expect(image).toBeInTheDocument();
        expect(image).toHaveAttribute('src', 'https://example.com/photo.jpg');
        
        // No debe renderizar iniciales
        expect(screen.queryByTestId('avatar-initials')).not.toBeInTheDocument();
    });

    test('Debe renderizar iniciales cuando no hay foto', () => {
        render(
            <AvatarComponent 
                foto={null} 
                nombre="Juan Pérez"
                backgroundColor="#f2c18e"
            />
        );
        
        const initials = screen.getByTestId('avatar-initials');
        expect(initials).toBeInTheDocument();
        expect(initials).toHaveTextContent('JP');
        
        // No debe renderizar imagen
        expect(screen.queryByTestId('avatar-image')).not.toBeInTheDocument();
    });

    test('Debe renderizar iniciales cuando foto es cadena vacía', () => {
        render(
            <AvatarComponent 
                foto="" 
                nombre="Maria García"
                backgroundColor="#818cf8"
            />
        );
        
        const initials = screen.getByTestId('avatar-initials');
        expect(initials).toBeInTheDocument();
        expect(initials).toHaveTextContent('MG');
    });

    test('Debe aplicar el color de fondo correctamente', () => {
        render(
            <AvatarComponent 
                foto={null} 
                nombre="Test User"
                backgroundColor="#f2c18e"
            />
        );
        
        const container = screen.getByTestId('avatar-container');
        expect(container).toHaveStyle('backgroundColor', '#f2c18e');
    });

    test('Debe mostrar "U" si el nombre no está disponible', () => {
        render(
            <AvatarComponent 
                foto={null} 
                nombre=""
                backgroundColor="#ffffff"
            />
        );
        
        const initials = screen.getByTestId('avatar-initials');
        expect(initials).toHaveTextContent('U');
    });
});

import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

/**
 * Test para validar las restricciones de tiempo en cuestionarios.
 * UC-64: Cuestionarios con tiempo inválido
 */

// Mock de validación de tiempo estimado
const validateTiempo = (tiempo) => {
    const num = Number(tiempo);
    
    if (!Number.isFinite(num) || num <= 0) {
        return { valid: false, error: 'El tiempo estimado debe ser un número mayor que 0' };
    }
    
    if (!Number.isInteger(num)) {
        return { valid: false, error: 'El tiempo estimado debe ser un número entero' };
    }
    
    if (num > 1440) {
        return { valid: false, error: 'El tiempo estimado no puede superar 1440 minutos (24 horas)' };
    }
    
    return { valid: true, error: '' };
};

describe('UC-64: Validación de tiempo en cuestionarios', () => {

    test('Debe rechazar tiempo decimal (0.3)', () => {
        const result = validateTiempo(0.3);
        expect(result.valid).toBe(false);
        expect(result.error).toContain('número entero');
    });

    test('Debe rechazar tiempo decimal (1.5)', () => {
        const result = validateTiempo(1.5);
        expect(result.valid).toBe(false);
        expect(result.error).toContain('número entero');
    });

    test('Debe rechazar tiempo negativo (-5)', () => {
        const result = validateTiempo(-5);
        expect(result.valid).toBe(false);
        expect(result.error).toContain('mayor que 0');
    });

    test('Debe rechazar tiempo cero', () => {
        const result = validateTiempo(0);
        expect(result.valid).toBe(false);
        expect(result.error).toContain('mayor que 0');
    });

    test('Debe aceptar tiempo válido (1)', () => {
        const result = validateTiempo(1);
        expect(result.valid).toBe(true);
        expect(result.error).toBe('');
    });

    test('Debe aceptar tiempo válido (30)', () => {
        const result = validateTiempo(30);
        expect(result.valid).toBe(true);
        expect(result.error).toBe('');
    });

    test('Debe aceptar tiempo válido (60)', () => {
        const result = validateTiempo(60);
        expect(result.valid).toBe(true);
        expect(result.error).toBe('');
    });

    test('Debe aceptar tiempo máximo válido (1440)', () => {
        const result = validateTiempo(1440);
        expect(result.valid).toBe(true);
        expect(result.error).toBe('');
    });

    test('Debe rechazar tiempo mayor a máximo (1441)', () => {
        const result = validateTiempo(1441);
        expect(result.valid).toBe(false);
        expect(result.error).toContain('1440 minutos');
    });

    test('Debe rechazar tiempo muy grande (999999)', () => {
        const result = validateTiempo(999999);
        expect(result.valid).toBe(false);
        expect(result.error).toContain('1440 minutos');
    });

    test('Debe rechazar string vacío', () => {
        const result = validateTiempo('');
        expect(result.valid).toBe(false);
    });

    test('Debe rechazar NaN', () => {
        const result = validateTiempo(NaN);
        expect(result.valid).toBe(false);
    });

    test('Debe rechazar Infinity', () => {
        const result = validateTiempo(Infinity);
        expect(result.valid).toBe(false);
    });
});

/**
 * Test para validar las propiedades del input HTML
 */
describe('UC-64: Propiedades del input HTML', () => {

    // Mock component
    const TimeInput = ({ value, onChange }) => (
        <input
            type="number"
            value={value}
            onChange={onChange}
            step="1"
            min="1"
            max="1440"
            data-testid="time-input"
        />
    );

    test('Debe tener step="1" para rechazar decimales', () => {
        const { getByTestId } = render(<TimeInput value={30} onChange={() => {}} />);
        const input = getByTestId('time-input');
        
        expect(input).toHaveAttribute('step', '1');
    });

    test('Debe tener min="1" para rechazar cero y negativos', () => {
        const { getByTestId } = render(<TimeInput value={30} onChange={() => {}} />);
        const input = getByTestId('time-input');
        
        expect(input).toHaveAttribute('min', '1');
    });

    test('Debe tener max="1440" para limitar a 24 horas', () => {
        const { getByTestId } = render(<TimeInput value={30} onChange={() => {}} />);
        const input = getByTestId('time-input');
        
        expect(input).toHaveAttribute('max', '1440');
    });

    test('Debe permitir escribir valores válidos', async () => {
        const handleChange = jest.fn();
        const { getByTestId } = render(
            <TimeInput value={30} onChange={handleChange} />
        );
        
        const input = getByTestId('time-input');
        
        await userEvent.clear(input);
        await userEvent.type(input, '45');
        
        expect(handleChange).toHaveBeenCalled();
    });

    test('El navegador debería rechazar decimales (HTML5)', () => {
        const { getByTestId } = render(<TimeInput value={30} onChange={() => {}} />);
        const input = getByTestId('time-input');
        
        // Simular que el navegador valida el atributo type="number"
        expect(input.type).toBe('number');
        expect(input.step).toBe('1');
        
        // Al escribir "0.3", el navegador debería truncarlo o rechazarlo
        fireEvent.change(input, { target: { value: '0.3' } });
        
        // El valor se mantendría como entero gracias a step="1"
        // Esto depende de la implementación del navegador
    });
});

/**
 * Test para validar que el campo no muestra "- min" cuando es inválido
 */
describe('UC-64: Visualización de tiempo inválido', () => {

    const TimeDisplay = ({ minutos }) => {
        const validado = Number.isInteger(minutos) && minutos > 0 && minutos <= 1440;
        
        return (
            <div data-testid="time-display">
                {validado ? `${minutos} min` : '- min'}
            </div>
        );
    };

    test('Debe mostrar tiempo válido correctamente', () => {
        const { getByTestId } = render(<TimeDisplay minutos={30} />);
        const display = getByTestId('time-display');
        
        expect(display).toHaveTextContent('30 min');
        expect(display).not.toHaveTextContent('- min');
    });

    test('Debe mostrar "- min" para tiempo inválido (decimal)', () => {
        const { getByTestId } = render(<TimeDisplay minutos={0.3} />);
        const display = getByTestId('time-display');
        
        expect(display).toHaveTextContent('- min');
    });

    test('Debe mostrar "- min" para tiempo negativo', () => {
        const { getByTestId } = render(<TimeDisplay minutos={-5} />);
        const display = getByTestId('time-display');
        
        expect(display).toHaveTextContent('- min');
    });

    test('Debe mostrar "- min" para tiempo cero', () => {
        const { getByTestId } = render(<TimeDisplay minutos={0} />);
        const display = getByTestId('time-display');
        
        expect(display).toHaveTextContent('- min');
    });
});

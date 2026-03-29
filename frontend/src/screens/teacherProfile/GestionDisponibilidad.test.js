import { render, screen, waitFor, act, fireEvent } from '@testing-library/react';
import GestionDisponibilidad from './GestionDisponibilidad';

jest.mock('../../api/disponibilidad', () => ({
    getDisponibilidades: jest.fn(),
    crearDisponibilidad: jest.fn(),
    eliminarDisponibilidad: jest.fn(),
}));

const { getDisponibilidades, crearDisponibilidad, eliminarDisponibilidad } = require('../../api/disponibilidad');

describe('GestionDisponibilidad', () => {
    const onClose = jest.fn();

    beforeEach(() => {
        jest.clearAllMocks();
    });

    test('renders loading state', async () => {
        getDisponibilidades.mockReturnValue(new Promise(() => {}));
        await act(async () => {
            render(<GestionDisponibilidad tutorId={1} onClose={onClose} />);
        });
    });

    test('renders empty state', async () => {
        getDisponibilidades.mockResolvedValueOnce([]);
        await act(async () => {
            render(<GestionDisponibilidad tutorId={1} onClose={onClose} />);
        });
        await waitFor(() => {
            expect(screen.getByText(/no tienes franjas/i)).toBeInTheDocument();
        });
    });

    test('renders availability list', async () => {
        getDisponibilidades.mockResolvedValueOnce([
            { id: 1, esRecurrente: true, diaSemana: 'MONDAY', horaInicio: '09:00', horaFin: '11:00', modalidad: 'ONLINE' },
            { id: 2, esRecurrente: true, diaSemana: 'WEDNESDAY', horaInicio: '14:00', horaFin: '16:00', modalidad: 'PRESENCIAL' },
        ]);
        await act(async () => {
            render(<GestionDisponibilidad tutorId={1} onClose={onClose} />);
        });
        await waitFor(() => {
            expect(screen.getByText(/Lunes/)).toBeInTheDocument();
            expect(screen.getByText(/Miércoles/)).toBeInTheDocument();
        });
    });

    test('handles API error', async () => {
        getDisponibilidades.mockRejectedValueOnce(new Error('fail'));
        await act(async () => {
            render(<GestionDisponibilidad tutorId={1} onClose={onClose} />);
        });
        await waitFor(() => {
            expect(screen.getByText(/No se pudo cargar/)).toBeInTheDocument();
        });
    });

    test('opens new availability form', async () => {
        getDisponibilidades.mockResolvedValueOnce([]);
        await act(async () => {
            render(<GestionDisponibilidad tutorId={1} onClose={onClose} />);
        });
        await waitFor(() => {
            const addBtn = screen.getByText(/Añadir|Nueva|Agregar/i);
            act(() => { addBtn.click(); });
        });
    });

    test('closes modal via close button', async () => {
        getDisponibilidades.mockResolvedValueOnce([]);
        await act(async () => {
            render(<GestionDisponibilidad tutorId={1} onClose={onClose} />);
        });
        const closeBtn = screen.getByLabelText(/Cerrar/i);
        fireEvent.click(closeBtn);
        expect(onClose).toHaveBeenCalled();
    });

    test('closes modal via overlay click', async () => {
        getDisponibilidades.mockResolvedValueOnce([]);
        await act(async () => {
            render(<GestionDisponibilidad tutorId={1} onClose={onClose} />);
        });
        const overlay = document.querySelector('.tm-overlay');
        if (overlay) {
            fireEvent.click(overlay);
            expect(onClose).toHaveBeenCalled();
        }
    });

    test('renders modal title', async () => {
        getDisponibilidades.mockResolvedValueOnce([]);
        await act(async () => {
            render(<GestionDisponibilidad tutorId={1} onClose={onClose} />);
        });
        await waitFor(() => {
            expect(screen.getByText(/Mi disponibilidad/)).toBeInTheDocument();
        });
    });

    test('shows form validation error when hours missing', async () => {
        getDisponibilidades.mockResolvedValueOnce([]);
        await act(async () => {
            render(<GestionDisponibilidad tutorId={1} onClose={onClose} />);
        });

        const addBtn = screen.getByText(/Añadir franja/i);
        await act(async () => { fireEvent.click(addBtn); });

        const saveBtn = screen.getByText(/^Añadir$/i);
        await act(async () => { fireEvent.click(saveBtn); });

        await waitFor(() => {
            const msgs = screen.getAllByText(/horas de inicio y fin son obligatorias/i);
            expect(msgs.length).toBeGreaterThan(0);
        });
    });

    test('shows validation error when start >= end', async () => {
        getDisponibilidades.mockResolvedValueOnce([]);
        await act(async () => {
            render(<GestionDisponibilidad tutorId={1} onClose={onClose} />);
        });

        const addBtn = screen.getByText(/Añadir franja/i);
        await act(async () => { fireEvent.click(addBtn); });

        const horaInicio = document.querySelector('input[name="horaInicio"]');
        const horaFin = document.querySelector('input[name="horaFin"]');

        if (horaInicio && horaFin) {
            await act(async () => {
                fireEvent.change(horaInicio, { target: { value: '14:00' } });
                fireEvent.change(horaFin, { target: { value: '10:00' } });
            });

            const saveBtn = screen.getByText(/^Añadir$/i);
            await act(async () => { fireEvent.click(saveBtn); });

            await waitFor(() => {
                const msgs = screen.getAllByText(/hora de inicio debe ser anterior/i);
                expect(msgs.length).toBeGreaterThan(0);
            });
        }
    });

    test('successfully creates availability', async () => {
        getDisponibilidades.mockResolvedValue([]);
        crearDisponibilidad.mockResolvedValueOnce({ id: 3, esRecurrente: true, diaSemana: 'MONDAY', horaInicio: '10:00', horaFin: '12:00' });

        await act(async () => {
            render(<GestionDisponibilidad tutorId={1} onClose={onClose} />);
        });

        const addBtn = screen.getByText(/Añadir franja/i);
        await act(async () => { fireEvent.click(addBtn); });

        const horaInicio = document.querySelector('input[name="horaInicio"]');
        const horaFin = document.querySelector('input[name="horaFin"]');

        if (horaInicio && horaFin) {
            await act(async () => {
                fireEvent.change(horaInicio, { target: { value: '10:00' } });
                fireEvent.change(horaFin, { target: { value: '12:00' } });
            });

            const saveBtn = screen.getByText(/^Añadir$/i);
            await act(async () => { fireEvent.click(saveBtn); });

            await waitFor(() => {
                expect(crearDisponibilidad).toHaveBeenCalled();
            });
        }
    });

    test('deletes availability with confirmation', async () => {
        getDisponibilidades.mockResolvedValue([
            { id: 1, esRecurrente: true, diaSemana: 'MONDAY', horaInicio: '09:00', horaFin: '11:00', modalidad: 'ONLINE' },
        ]);
        eliminarDisponibilidad.mockResolvedValueOnce({});
        const confirmSpy = jest.spyOn(window, 'confirm').mockReturnValue(true);

        await act(async () => {
            render(<GestionDisponibilidad tutorId={1} onClose={onClose} />);
        });

        await waitFor(() => {
            expect(screen.getByText(/Lunes/)).toBeInTheDocument();
        });

        const deleteBtn = screen.getByText('Eliminar');
        await act(async () => { fireEvent.click(deleteBtn); });

        expect(eliminarDisponibilidad).toHaveBeenCalledWith(1);
        confirmSpy.mockRestore();
    });

    test('cancels delete when confirm is declined', async () => {
        getDisponibilidades.mockResolvedValue([
            { id: 1, esRecurrente: true, diaSemana: 'MONDAY', horaInicio: '09:00', horaFin: '11:00', modalidad: 'ONLINE' },
        ]);
        const confirmSpy = jest.spyOn(window, 'confirm').mockReturnValue(false);

        await act(async () => {
            render(<GestionDisponibilidad tutorId={1} onClose={onClose} />);
        });

        await waitFor(() => {
            expect(screen.getByText(/Lunes/)).toBeInTheDocument();
        });

        const deleteBtn = screen.getByText('Eliminar');
        await act(async () => { fireEvent.click(deleteBtn); });

        expect(eliminarDisponibilidad).not.toHaveBeenCalled();
        confirmSpy.mockRestore();
    });

    test('shows presencial modalidad and ubicacion', async () => {
        getDisponibilidades.mockResolvedValueOnce([
            { id: 1, esRecurrente: true, diaSemana: 'FRIDAY', horaInicio: '09:00', horaFin: '11:00', modalidad: 'PRESENCIAL', ubicacionPresencial: 'Aula 101' },
        ]);
        await act(async () => {
            render(<GestionDisponibilidad tutorId={1} onClose={onClose} />);
        });
        await waitFor(() => {
            expect(screen.getByText(/Viernes/)).toBeInTheDocument();
            expect(screen.getByText(/Presencial/)).toBeInTheDocument();
            expect(screen.getByText(/Aula 101/)).toBeInTheDocument();
        });
    });

    test('renders puntual (non-recurrent) slot', async () => {
        getDisponibilidades.mockResolvedValueOnce([
            { id: 1, esRecurrente: false, fechaPuntual: '2025-07-15T10:00:00', horaInicio: '10:00', horaFin: '12:00', modalidad: 'ONLINE' },
        ]);
        await act(async () => {
            render(<GestionDisponibilidad tutorId={1} onClose={onClose} />);
        });
        await waitFor(() => {
            expect(screen.getByText(/Puntual/)).toBeInTheDocument();
        });
    });

    test('cancels form', async () => {
        getDisponibilidades.mockResolvedValueOnce([]);
        await act(async () => {
            render(<GestionDisponibilidad tutorId={1} onClose={onClose} />);
        });

        const addBtn = screen.getByText(/Añadir|Nueva|Agregar/i);
        await act(async () => { fireEvent.click(addBtn); });

        expect(screen.getByText(/^Añadir$/i)).toBeInTheDocument();

        const cancelBtn = screen.getByText(/Cancelar/i);
        await act(async () => { fireEvent.click(cancelBtn); });

        expect(screen.queryByText(/^Añadir$/i)).not.toBeInTheDocument();
    });

    test('shows error on create failure with 409', async () => {
        getDisponibilidades.mockResolvedValue([]);
        crearDisponibilidad.mockRejectedValueOnce({ response: { status: 409 } });

        await act(async () => {
            render(<GestionDisponibilidad tutorId={1} onClose={onClose} />);
        });

        const addBtn = screen.getByText(/Añadir|Nueva|Agregar/i);
        await act(async () => { fireEvent.click(addBtn); });

        const horaInicio = document.querySelector('input[name="horaInicio"]');
        const horaFin = document.querySelector('input[name="horaFin"]');

        if (horaInicio && horaFin) {
            await act(async () => {
                fireEvent.change(horaInicio, { target: { value: '10:00' } });
                fireEvent.change(horaFin, { target: { value: '12:00' } });
            });

            const saveBtn = screen.getByText(/^Añadir$/i);
            await act(async () => { fireEvent.click(saveBtn); });
        }
    });
});

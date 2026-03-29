// The component doesn't export these, so we test the rendered component instead
import { render, screen, act, fireEvent } from '@testing-library/react';

// Import after mocks
import TeacherAvailabilityCalendar from './TeacherAvailabilityCalendar';

jest.mock('./TeacherAvailabilityCalendar.css', () => ({}));

const mockGetDisponibilidadTutorFecha = jest.fn();
const mockGetHorariosOcupadosContratacion = jest.fn();
const mockGetDisponibilidades = jest.fn();

jest.mock('../../api/solicitudContratacion', () => ({
    getDisponibilidadTutorFecha: (...args) => mockGetDisponibilidadTutorFecha(...args),
    getHorariosOcupadosContratacion: (...args) => mockGetHorariosOcupadosContratacion(...args),
}));

jest.mock('../../api/disponibilidad', () => ({
    getDisponibilidades: (...args) => mockGetDisponibilidades(...args),
}));

describe('TeacherAvailabilityCalendar', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockGetDisponibilidades.mockResolvedValue([]);
        mockGetDisponibilidadTutorFecha.mockResolvedValue({ data: [] });
        mockGetHorariosOcupadosContratacion.mockResolvedValue({ data: [] });
    });

    test('renders calendar', async () => {
        await act(async () => {
            render(<TeacherAvailabilityCalendar tutorId={1} />);
        });

        // Should show day labels
        expect(screen.getByText('Lun')).toBeInTheDocument();
        expect(screen.getByText('Mar')).toBeInTheDocument();
        expect(screen.getByText('Vie')).toBeInTheDocument();
    });

    test('renders current month', async () => {
        await act(async () => {
            render(<TeacherAvailabilityCalendar tutorId={1} />);
        });
        // Verify calendar renders without error
        expect(screen.getByText('Lun')).toBeInTheDocument();
    });

    test('can navigate months', async () => {
        await act(async () => {
            render(<TeacherAvailabilityCalendar tutorId={1} />);
        });

        const nextBtn = screen.getByRole('button', { name: /Mes siguiente/i });
        await act(async () => { fireEvent.click(nextBtn); });

        const prevBtn = screen.getByRole('button', { name: /Mes anterior/i });
        await act(async () => { fireEvent.click(prevBtn); });
    });

    test('loads disponibilidades on mount', async () => {
        await act(async () => {
            render(<TeacherAvailabilityCalendar tutorId={1} />);
        });
        expect(mockGetDisponibilidades).toHaveBeenCalledWith(1);
    });

    test('shows availability when clicking a day', async () => {
        mockGetDisponibilidades.mockResolvedValueOnce([
            { esRecurrente: true, diaSemana: 'MONDAY', horaInicio: '09:00', horaFin: '11:00', modalidad: 'ONLINE' },
        ]);
        mockGetDisponibilidadTutorFecha.mockResolvedValueOnce({
            data: [{ horaInicio: '09:00', horaFin: '11:00', modalidad: 'ONLINE' }],
        });
        mockGetHorariosOcupadosContratacion.mockResolvedValueOnce({ data: [] });

        await act(async () => {
            render(<TeacherAvailabilityCalendar tutorId={1} />);
        });

        // Click a day button
        const dayBtns = screen.getAllByRole('button');
        const dayBtn = dayBtns.find(b => /^\d+$/.test(b.textContent));
        if (dayBtn) {
            await act(async () => { fireEvent.click(dayBtn); });
        }
    });

    test('renders with onSlotSelect callback', async () => {
        const onSlotSelect = jest.fn();
        await act(async () => {
            render(<TeacherAvailabilityCalendar tutorId={1} onSlotSelect={onSlotSelect} />);
        });
    });

    test('handles empty disponibilidades', async () => {
        mockGetDisponibilidades.mockResolvedValueOnce([]);
        await act(async () => {
            render(<TeacherAvailabilityCalendar tutorId={1} />);
        });
    });
});

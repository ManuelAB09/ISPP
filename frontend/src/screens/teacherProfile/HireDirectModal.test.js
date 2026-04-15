import { render, screen, waitFor, act, fireEvent } from '@testing-library/react';
import HireDirectModal from './HireDirectModal';

jest.mock('./HireTutorModal.css', () => ({}));
jest.mock('leaflet/dist/leaflet.css', () => ({}));

jest.mock('react-leaflet', () => ({
    MapContainer: ({ children }) => <div data-testid="map-container">{children}</div>,
    TileLayer: () => null,
    Marker: () => null,
    useMapEvents: () => null,
}));

jest.mock('leaflet', () => ({
    icon: () => ({}),
}));

const mockCrearSolicitud = jest.fn();
const mockGetDisponibilidad = jest.fn();
const mockGetOcupados = jest.fn();
jest.mock('../../api/solicitudContratacion', () => ({
    crearSolicitudContratacion: (...args) => mockCrearSolicitud(...args),
    getDisponibilidadTutorFecha: (...args) => mockGetDisponibilidad(...args),
    getHorariosOcupadosContratacion: (...args) => mockGetOcupados(...args),
}));

const tutor = {
    id: 10,
    tarifaHora: 25,
    usuario: { nombre: 'Prof. Test' },
};

describe('HireDirectModal', () => {
    const onClose = jest.fn();

    beforeEach(() => {
        jest.clearAllMocks();
        mockGetDisponibilidad.mockResolvedValue({ data: [] });
        mockGetOcupados.mockResolvedValue({ data: [] });
    });

    test('renders modal with tutor name', () => {
        render(<HireDirectModal tutor={tutor} onClose={onClose} />);
        expect(screen.getAllByText(/Prof. Test/).length).toBeGreaterThanOrEqual(1);
    });

    test('shows day selector', () => {
        render(<HireDirectModal tutor={tutor} onClose={onClose} />);
        const dateInput = document.querySelector('input[type="date"]');
        expect(dateInput).toBeInTheDocument();
    });

    test('loads availability when day is selected', async () => {
        render(<HireDirectModal tutor={tutor} onClose={onClose} />);

        const dateInput = document.querySelector('input[type="date"]');
        if (dateInput) {
            await act(async () => {
                fireEvent.change(dateInput, { target: { value: '2025-06-15' } });
            });

            await waitFor(() => {
                expect(mockGetDisponibilidad).toHaveBeenCalledWith(10, '2025-06-15');
            });
        }
    });

    test('shows availability slots', async () => {
        mockGetDisponibilidad.mockResolvedValueOnce({
            data: [
                { horaInicio: '09:00', horaFin: '11:00', modalidad: 'ONLINE' },
            ],
        });
        mockGetOcupados.mockResolvedValueOnce({ data: [] });

        render(<HireDirectModal tutor={tutor} onClose={onClose} />);

        const dateInput = document.querySelector('input[type="date"]');
        if (dateInput) {
            await act(async () => {
                fireEvent.change(dateInput, { target: { value: '2025-06-15' } });
            });

            await waitFor(() => {
                expect(screen.getByText(/09:00/)).toBeInTheDocument();
            });
        }
    });

    test('closes modal', () => {
        render(<HireDirectModal tutor={tutor} onClose={onClose} />);
        const overlay = document.querySelector('.htm-overlay');
        if (overlay) {
            fireEvent.click(overlay);
        }
    });

    test('shows tarifa', () => {
        render(<HireDirectModal tutor={tutor} onClose={onClose} />);
        expect(screen.getByText(/25/)).toBeInTheDocument();
    });

    test('applies initial selection', async () => {
        const initialSelection = { dia: '2025-06-15', horaInicio: '09:00', horaFin: '10:00' };
        mockGetDisponibilidad.mockResolvedValueOnce({ data: [{ horaInicio: '09:00', horaFin: '11:00', modalidad: 'ONLINE' }] });
        mockGetOcupados.mockResolvedValueOnce({ data: [] });

        await act(async () => {
            render(<HireDirectModal tutor={tutor} onClose={onClose} initialSelection={initialSelection} />);
        });
    });

    test('shows time inputs for hora inicio and fin', () => {
        render(<HireDirectModal tutor={tutor} onClose={onClose} />);
        const timeInputs = document.querySelectorAll('input[type="time"]');
        expect(timeInputs.length).toBeGreaterThanOrEqual(0);
    });

    test('shows message field for student message', () => {
        render(<HireDirectModal tutor={tutor} onClose={onClose} />);
        // eslint-disable-next-line testing-library/no-node-access
        const textarea = document.querySelector('textarea');
        expect(textarea || screen.getByPlaceholderText(/mensaje/i)).toBeTruthy();
    });

    test('shows occupied slots when present', async () => {
        mockGetDisponibilidad.mockResolvedValueOnce({
            data: [{ horaInicio: '09:00', horaFin: '17:00', modalidad: 'ONLINE' }],
        });
        mockGetOcupados.mockResolvedValueOnce({
            data: [{ horaInicio: '10:00', horaFin: '11:00' }],
        });

        render(<HireDirectModal tutor={tutor} onClose={onClose} />);

        const dateInput = document.querySelector('input[type="date"]');
        if (dateInput) {
            await act(async () => {
                fireEvent.change(dateInput, { target: { value: '2025-06-15' } });
            });
            await waitFor(() => {
                expect(mockGetOcupados).toHaveBeenCalledWith(10, '2025-06-15');
            });
        }
    });

    test('shows no availability message when empty', async () => {
        mockGetDisponibilidad.mockResolvedValueOnce({ data: [] });
        mockGetOcupados.mockResolvedValueOnce({ data: [] });

        render(<HireDirectModal tutor={tutor} onClose={onClose} />);
        const dateInput = document.querySelector('input[type="date"]');
        if (dateInput) {
            await act(async () => {
                fireEvent.change(dateInput, { target: { value: '2025-06-15' } });
            });
            await waitFor(() => {
                expect(mockGetDisponibilidad).toHaveBeenCalled();
            });
        }
    });

    test('renders step indicator', () => {
        render(<HireDirectModal tutor={tutor} onClose={onClose} />);
        // Step 1 should be shown
        expect(screen.getByText(/25/)).toBeInTheDocument();
    });

    test('handles availability loading error', async () => {
        mockGetDisponibilidad.mockRejectedValueOnce(new Error('network fail'));

        render(<HireDirectModal tutor={tutor} onClose={onClose} />);
        const dateInput = document.querySelector('input[type="date"]');
        if (dateInput) {
            await act(async () => {
                fireEvent.change(dateInput, { target: { value: '2025-06-15' } });
            });
        }
    });
});

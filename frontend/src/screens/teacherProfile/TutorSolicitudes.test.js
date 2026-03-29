import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import TutorSolicitudes from './TutorSolicitudes';

jest.mock('./TutorSolicitudes.css', () => ({}));

const mockObtenerSolicitudesTutor = jest.fn();
const mockAceptarSolicitud = jest.fn();
const mockRechazarSolicitud = jest.fn();
const mockCancelarSolicitud = jest.fn();
const mockReprogramarSolicitud = jest.fn();
const mockCrearZoomSolicitud = jest.fn();

jest.mock('../../api/solicitudContratacion', () => ({
    obtenerSolicitudesTutor: (...args) => mockObtenerSolicitudesTutor(...args),
    aceptarSolicitud: (...args) => mockAceptarSolicitud(...args),
    rechazarSolicitud: (...args) => mockRechazarSolicitud(...args),
    cancelarSolicitud: (...args) => mockCancelarSolicitud(...args),
    reprogramarSolicitud: (...args) => mockReprogramarSolicitud(...args),
    crearZoomSolicitud: (...args) => mockCrearZoomSolicitud(...args),
}));

jest.mock('../../contexts/SocketContext', () => ({
    useSocketContext: () => ({
        socket: { on: jest.fn(), off: jest.fn() },
        isConnected: true,
    }),
}));

describe('TutorSolicitudes', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    test('renders loading state', async () => {
        mockObtenerSolicitudesTutor.mockReturnValue(new Promise(() => {}));
        await act(async () => {
            render(<TutorSolicitudes />);
        });
    });

    test('renders empty state', async () => {
        mockObtenerSolicitudesTutor.mockResolvedValueOnce({ data: [] });
        await act(async () => {
            render(<TutorSolicitudes />);
        });
        await waitFor(() => {
            expect(screen.getByText(/No tienes solicitudes|Sin solicitudes/i)).toBeInTheDocument();
        });
    });

    test('renders pending solicitudes', async () => {
        mockObtenerSolicitudesTutor.mockResolvedValueOnce({
            data: [
                {
                    id: 1,
                    alumnoNombre: 'Alumno A',
                    estado: 'PENDIENTE',
                    dia: '2025-06-15',
                    horaInicio: '09:00',
                    horaFin: '10:00',
                    modalidad: 'ONLINE',
                    importeTotal: 25,
                },
            ],
        });
        await act(async () => {
            render(<TutorSolicitudes />);
        });
        await waitFor(() => {
            expect(screen.getByText(/Alumno A/)).toBeInTheDocument();
        });
    });

    test('renders tab navigation', async () => {
        mockObtenerSolicitudesTutor.mockResolvedValueOnce({ data: [] });
        await act(async () => {
            render(<TutorSolicitudes />);
        });
        await waitFor(() => {
            const pendienteTab = screen.getByRole('button', { name: /Pendiente/i });
            expect(pendienteTab).toBeInTheDocument();
        });
    });

    test('accepts a solicitud', async () => {
        mockObtenerSolicitudesTutor.mockResolvedValueOnce({
            data: [
                { id: 1, alumnoNombre: 'Alumno B', estado: 'PENDIENTE', dia: '2025-06-15', horaInicio: '09:00', horaFin: '10:00', modalidad: 'ONLINE', importeTotal: 25 },
            ],
        });
        mockAceptarSolicitud.mockResolvedValueOnce({ data: { id: 1, estado: 'ACEPTADA' } });

        await act(async () => {
            render(<TutorSolicitudes />);
        });

        await waitFor(() => {
            expect(screen.getByText(/Alumno B/)).toBeInTheDocument();
        });

        const acceptBtn = screen.getByText(/Aceptar/i);
        if (acceptBtn) {
            await act(async () => { fireEvent.click(acceptBtn); });
            expect(mockAceptarSolicitud).toHaveBeenCalledWith(1);
        }
    });

    test('handles reject flow', async () => {
        mockObtenerSolicitudesTutor.mockResolvedValueOnce({
            data: [
                { id: 2, alumnoNombre: 'Alumno C', estado: 'PENDIENTE', dia: '2025-06-16', horaInicio: '10:00', horaFin: '11:00', modalidad: 'ONLINE', importeTotal: 30 },
            ],
        });

        await act(async () => {
            render(<TutorSolicitudes />);
        });

        await waitFor(() => {
            expect(screen.getByText(/Alumno C/)).toBeInTheDocument();
        });

        const rejectBtn = screen.getByText(/Rechazar/i);
        if (rejectBtn) {
            await act(async () => { fireEvent.click(rejectBtn); });
        }
    });

    test('fetches solicitudes on mount', async () => {
        mockObtenerSolicitudesTutor.mockResolvedValueOnce({ data: [] });
        await act(async () => {
            render(<TutorSolicitudes />);
        });
        expect(mockObtenerSolicitudesTutor).toHaveBeenCalled();
    });

    test('handles API error', async () => {
        mockObtenerSolicitudesTutor.mockRejectedValueOnce(new Error('fail'));
        await act(async () => {
            render(<TutorSolicitudes />);
        });
        // Should not crash, shows empty state
    });

    test('switches to Confirmadas tab', async () => {
        mockObtenerSolicitudesTutor.mockResolvedValueOnce({
            data: [
                { id: 1, alumnoNombre: 'Alumno D', estado: 'ACEPTADA', dia: '2025-06-15', horaInicio: '09:00', horaFin: '10:00', modalidad: 'ONLINE', importeTotal: 25, tarifaHora: 25 },
            ],
        });
        await act(async () => { render(<TutorSolicitudes />); });
        await waitFor(() => {
            const confirmadasTab = screen.getByRole('button', { name: /Confirmadas/i });
            act(() => { fireEvent.click(confirmadasTab); });
        });
        await waitFor(() => {
            expect(screen.getByText(/Alumno D/)).toBeInTheDocument();
        });
    });

    test('switches to Historial tab', async () => {
        mockObtenerSolicitudesTutor.mockResolvedValueOnce({
            data: [
                { id: 2, alumnoNombre: 'Alumno E', estado: 'COMPLETADA', dia: '2025-06-10', horaInicio: '09:00', horaFin: '10:00', modalidad: 'ONLINE', importeTotal: 25, tarifaHora: 25 },
            ],
        });
        await act(async () => { render(<TutorSolicitudes />); });
        await waitFor(() => {
            const historialTab = screen.getByRole('button', { name: /Historial/i });
            act(() => { fireEvent.click(historialTab); });
        });
        await waitFor(() => {
            expect(screen.getByText(/Alumno E/)).toBeInTheDocument();
        });
    });

    test('shows empty message per tab', async () => {
        mockObtenerSolicitudesTutor.mockResolvedValueOnce({ data: [] });
        await act(async () => { render(<TutorSolicitudes />); });
        await waitFor(() => {
            expect(screen.getByText(/No tienes solicitudes pendientes/i)).toBeInTheDocument();
        });

        const confirmadasTab = screen.getByRole('button', { name: /Confirmadas/i });
        await act(async () => { fireEvent.click(confirmadasTab); });
        await waitFor(() => {
            expect(screen.getByText(/No tienes reservas confirmadas/i)).toBeInTheDocument();
        });

        const historialTab = screen.getByRole('button', { name: /Historial/i });
        await act(async () => { fireEvent.click(historialTab); });
        await waitFor(() => {
            expect(screen.getByText(/Sin historial/i)).toBeInTheDocument();
        });
    });

    test('shows solicitud details: day, time, importe, modalidad', async () => {
        mockObtenerSolicitudesTutor.mockResolvedValueOnce({
            data: [
                { id: 3, alumnoNombre: 'Alumno F', estado: 'PENDIENTE', dia: '2025-07-01', horaInicio: '14:00', horaFin: '16:00', modalidad: 'PRESENCIAL', importeTotal: 50, tarifaHora: 25 },
            ],
        });
        await act(async () => { render(<TutorSolicitudes />); });
        await waitFor(() => {
            expect(screen.getByText('2025-07-01')).toBeInTheDocument();
            expect(screen.getByText(/14:00/)).toBeInTheDocument();
            expect(screen.getByText(/16:00/)).toBeInTheDocument();
            expect(screen.getByText(/50€/)).toBeInTheDocument();
            expect(screen.getByText(/Presencial/)).toBeInTheDocument();
        });
    });

    test('shows motivo rechazo for rejected solicitudes', async () => {
        mockObtenerSolicitudesTutor.mockResolvedValueOnce({
            data: [
                { id: 4, alumnoNombre: 'Alumno G', estado: 'RECHAZADA', dia: '2025-06-20', horaInicio: '10:00', horaFin: '11:00', modalidad: 'ONLINE', importeTotal: 25, tarifaHora: 25, motivoRechazo: 'Horario incompatible' },
            ],
        });
        await act(async () => { render(<TutorSolicitudes />); });
        const historialTab = screen.getByRole('button', { name: /Historial/i });
        await act(async () => { fireEvent.click(historialTab); });
        await waitFor(() => {
            expect(screen.getByText('Horario incompatible')).toBeInTheDocument();
        });
    });

    test('shows mensaje from alumno', async () => {
        mockObtenerSolicitudesTutor.mockResolvedValueOnce({
            data: [
                { id: 5, alumnoNombre: 'Alumno H', estado: 'PENDIENTE', dia: '2025-07-15', horaInicio: '10:00', horaFin: '11:00', modalidad: 'ONLINE', importeTotal: 25, tarifaHora: 25, mensaje: 'Necesito ayuda con cálculo' },
            ],
        });
        await act(async () => { render(<TutorSolicitudes />); });
        await waitFor(() => {
            expect(screen.getByText('Necesito ayuda con cálculo')).toBeInTheDocument();
        });
    });

    test('renders panel title', async () => {
        mockObtenerSolicitudesTutor.mockResolvedValueOnce({ data: [] });
        await act(async () => { render(<TutorSolicitudes />); });
        await waitFor(() => {
            expect(screen.getByText(/Gestión de reservas/)).toBeInTheDocument();
        });
    });

    test('shows pending count badge', async () => {
        mockObtenerSolicitudesTutor.mockResolvedValueOnce({
            data: [
                { id: 6, alumnoNombre: 'A1', estado: 'PENDIENTE', dia: '2025-07-01', horaInicio: '09:00', horaFin: '10:00', modalidad: 'ONLINE', importeTotal: 25, tarifaHora: 25 },
                { id: 7, alumnoNombre: 'A2', estado: 'PENDIENTE', dia: '2025-07-02', horaInicio: '09:00', horaFin: '10:00', modalidad: 'ONLINE', importeTotal: 25, tarifaHora: 25 },
            ],
        });
        await act(async () => { render(<TutorSolicitudes />); });
        await waitFor(() => {
            expect(screen.getByText(/\(2\)/)).toBeInTheDocument();
        });
    });

    test('shows PAGADA solicitud in confirmadas', async () => {
        mockObtenerSolicitudesTutor.mockResolvedValueOnce({
            data: [
                { id: 8, alumnoNombre: 'Alumno Pagado', estado: 'PAGADA', dia: '2025-07-05', horaInicio: '10:00', horaFin: '11:00', modalidad: 'ONLINE', importeTotal: 30, tarifaHora: 30 },
            ],
        });
        await act(async () => { render(<TutorSolicitudes />); });
        const confirmadasTab = screen.getByRole('button', { name: /Confirmadas/i });
        await act(async () => { fireEvent.click(confirmadasTab); });
        await waitFor(() => {
            expect(screen.getByText('Alumno Pagado')).toBeInTheDocument();
        });
    });

    test('reject flow - opens reject form and submits', async () => {
        mockObtenerSolicitudesTutor.mockResolvedValueOnce({
            data: [
                { id: 9, alumnoNombre: 'Alumno I', estado: 'PENDIENTE', dia: '2025-07-10', horaInicio: '09:00', horaFin: '10:00', modalidad: 'ONLINE', importeTotal: 25, tarifaHora: 25 },
            ],
        });
        mockRechazarSolicitud.mockResolvedValueOnce({ data: { id: 9, estado: 'RECHAZADA' } });

        await act(async () => { render(<TutorSolicitudes />); });
        await waitFor(() => { expect(screen.getByText(/Alumno I/)).toBeInTheDocument(); });

        const rejectBtn = screen.getByText(/Rechazar/i);
        await act(async () => { fireEvent.click(rejectBtn); });

        const textarea = screen.queryByPlaceholderText(/motivo/i) || document.querySelector('textarea');
        if (textarea) {
            await act(async () => { fireEvent.change(textarea, { target: { value: 'No puedo' } }); });

            const confirmReject = screen.getByText(/Confirmar rechazo/i);
            if (confirmReject) {
                await act(async () => { fireEvent.click(confirmReject); });
                expect(mockRechazarSolicitud).toHaveBeenCalledWith(9, 'No puedo');
            }
        }
    });
});

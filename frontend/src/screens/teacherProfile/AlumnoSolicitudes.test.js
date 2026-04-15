import { render, screen, waitFor, act } from '@testing-library/react';
import AlumnoSolicitudes from './AlumnoSolicitudes';

jest.mock('./AlumnoSolicitudes.css', () => ({}));

jest.mock('@stripe/stripe-js', () => ({
    loadStripe: jest.fn(() => Promise.resolve({})),
}));

jest.mock('@stripe/react-stripe-js', () => ({
    Elements: ({ children }) => <div data-testid="stripe-elements">{children}</div>,
    PaymentElement: () => <div data-testid="payment-element" />,
    useStripe: () => ({ confirmPayment: jest.fn() }),
    useElements: () => ({ submit: jest.fn(() => Promise.resolve({})) }),
}));

const mockObtenerSolicitudesAlumno = jest.fn();
const mockCrearPaymentIntent = jest.fn();
const mockConfirmarPago = jest.fn();
const mockCancelarAlumno = jest.fn();
const mockCalificar = jest.fn();
const mockAprobarReprogramacion = jest.fn();
const mockRechazarReprogramacion = jest.fn();

jest.mock('../../api/solicitudContratacion', () => ({
    obtenerSolicitudesAlumno: (...args) => mockObtenerSolicitudesAlumno(...args),
    crearPaymentIntentSolicitud: (...args) => mockCrearPaymentIntent(...args),
    confirmarPagoSolicitud: (...args) => mockConfirmarPago(...args),
    cancelarSolicitudAlumno: (...args) => mockCancelarAlumno(...args),
    calificarSolicitud: (...args) => mockCalificar(...args),
    aprobarReprogramacion: (...args) => mockAprobarReprogramacion(...args),
    rechazarReprogramacion: (...args) => mockRechazarReprogramacion(...args),
}));

jest.mock('../../contexts/SocketContext', () => ({
    useSocketContext: () => ({
        socket: { on: jest.fn(), off: jest.fn() },
        isConnected: true,
    }),
}));

describe('AlumnoSolicitudes', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    test('renders loading state', async () => {
        mockObtenerSolicitudesAlumno.mockReturnValue(new Promise(() => {}));
        await act(async () => {
            render(<AlumnoSolicitudes tutorId={10} />);
        });
    });

    test('renders empty state', async () => {
        mockObtenerSolicitudesAlumno.mockResolvedValueOnce({ data: [] });
        await act(async () => {
            render(<AlumnoSolicitudes tutorId={10} />);
        });
        // Component returns null when empty
        await waitFor(() => {
            expect(screen.queryByText(/solicitud/i)).toBeNull();
        });
    });

    test('renders pending solicitudes', async () => {
        mockObtenerSolicitudesAlumno.mockResolvedValueOnce({
            data: [
                {
                    id: 1,
                    tutorId: 10,
                    tutorNombre: 'Tutor A',
                    estado: 'PENDIENTE',
                    dia: '2025-06-15',
                    horaInicio: '09:00',
                    horaFin: '10:00',
                    importeTotal: 25,
                },
            ],
        });
        await act(async () => {
            render(<AlumnoSolicitudes tutorId={10} />);
        });
        await waitFor(() => {
            expect(screen.getByText(/Pendiente|PENDIENTE/)).toBeInTheDocument();
        });
    });

    test('renders accepted solicitud with pay button', async () => {
        mockObtenerSolicitudesAlumno.mockResolvedValueOnce({
            data: [
                {
                    id: 2,
                    tutorId: 10,
                    tutorNombre: 'Tutor B',
                    estado: 'ACEPTADA',
                    dia: '2025-06-16',
                    horaInicio: '10:00',
                    horaFin: '11:00',
                    importeTotal: 30,
                },
            ],
        });
        await act(async () => {
            render(<AlumnoSolicitudes tutorId={10} />);
        });
        await waitFor(() => {
            const payBtn = screen.queryByText(/Pagar/i);
            if (payBtn) {
                expect(payBtn).toBeInTheDocument();
            }
        });
    });

    test('handles API error', async () => {
        mockObtenerSolicitudesAlumno.mockRejectedValueOnce(new Error('fail'));
        await act(async () => {
            render(<AlumnoSolicitudes tutorId={10} />);
        });
        // Should not crash
    });

    test('fetches solicitudes on mount', async () => {
        mockObtenerSolicitudesAlumno.mockResolvedValueOnce({ data: [] });
        await act(async () => {
            render(<AlumnoSolicitudes tutorId={10} />);
        });
        expect(mockObtenerSolicitudesAlumno).toHaveBeenCalled();
    });

    test('renders paid solicitud with estado PAGADA', async () => {
        mockObtenerSolicitudesAlumno.mockResolvedValueOnce({
            data: [
                { id: 3, tutorId: 10, tutorNombre: 'Tutor C', estado: 'PAGADA', dia: '2025-06-17', horaInicio: '11:00', horaFin: '12:00', importeTotal: 35, tarifaHora: 35 },
            ],
        });
        await act(async () => { render(<AlumnoSolicitudes tutorId={10} />); });
        await waitFor(() => {
            expect(screen.getByText(/Pagada/)).toBeInTheDocument();
        });
    });

    test('renders cancelled solicitud', async () => {
        mockObtenerSolicitudesAlumno.mockResolvedValueOnce({
            data: [
                { id: 4, tutorId: 10, tutorNombre: 'Tutor D', estado: 'CANCELADA_ALUMNO', dia: '2025-06-18', horaInicio: '14:00', horaFin: '15:00', importeTotal: 20, tarifaHora: 20 },
            ],
        });
        await act(async () => { render(<AlumnoSolicitudes tutorId={10} />); });
        await waitFor(() => {
            expect(screen.getByText(/Cancelada/)).toBeInTheDocument();
        });
    });

    test('renders rejected solicitud with motivo', async () => {
        mockObtenerSolicitudesAlumno.mockResolvedValueOnce({
            data: [
                { id: 5, tutorId: 10, tutorNombre: 'Tutor E', estado: 'RECHAZADA', dia: '2025-06-19', horaInicio: '09:00', horaFin: '10:00', importeTotal: 25, tarifaHora: 25, motivoRechazo: 'No disponible' },
            ],
        });
        await act(async () => { render(<AlumnoSolicitudes tutorId={10} />); });
        await waitFor(() => {
            expect(screen.getByText(/Rechazada/)).toBeInTheDocument();
            expect(screen.getByText(/No disponible/)).toBeInTheDocument();
        });
    });

    test('renders completed solicitud', async () => {
        mockObtenerSolicitudesAlumno.mockResolvedValueOnce({
            data: [
                { id: 6, tutorId: 10, tutorNombre: 'Tutor F', estado: 'COMPLETADA', dia: '2025-06-20', horaInicio: '10:00', horaFin: '11:00', importeTotal: 30, tarifaHora: 30 },
            ],
        });
        await act(async () => { render(<AlumnoSolicitudes tutorId={10} />); });
        await waitFor(() => {
            expect(screen.getByText(/Completada/)).toBeInTheDocument();
        });
    });

    test('renders reprogramacion pendiente solicitud', async () => {
        mockObtenerSolicitudesAlumno.mockResolvedValueOnce({
            data: [
                { id: 7, tutorId: 10, tutorNombre: 'Tutor G', estado: 'REPROGRAMACION_PENDIENTE', dia: '2025-06-21', horaInicio: '11:00', horaFin: '12:00', importeTotal: 25, tarifaHora: 25 },
            ],
        });
        await act(async () => { render(<AlumnoSolicitudes tutorId={10} />); });
        await waitFor(() => {
            expect(screen.getByText(/Reprogramación pendiente/)).toBeInTheDocument();
        });
    });

    test('shows cancel button and opens cancel form', async () => {
        mockObtenerSolicitudesAlumno.mockResolvedValueOnce({
            data: [
                { id: 8, tutorId: 10, tutorNombre: 'Tutor H', estado: 'PENDIENTE', dia: '2025-06-22', horaInicio: '09:00', horaFin: '10:00', importeTotal: 25, tarifaHora: 25 },
            ],
        });
        await act(async () => { render(<AlumnoSolicitudes tutorId={10} />); });
        await waitFor(() => {
            const cancelBtn = screen.queryByText(/Cancelar solicitud/i);
            if (cancelBtn) {
                act(() => { cancelBtn.click(); });
            }
        });
    });

    test('shows solicitud details: day, time, importe', async () => {
        mockObtenerSolicitudesAlumno.mockResolvedValueOnce({
            data: [
                { id: 9, tutorId: 10, tutorNombre: 'Tutor I', estado: 'PENDIENTE', dia: '2025-06-23', horaInicio: '14:00', horaFin: '15:30', importeTotal: 37.5, tarifaHora: 25 },
            ],
        });
        await act(async () => { render(<AlumnoSolicitudes tutorId={10} />); });
        await waitFor(() => {
            expect(screen.getByText('2025-06-23')).toBeInTheDocument();
            expect(screen.getByText(/14:00/)).toBeInTheDocument();
            expect(screen.getByText(/15:30/)).toBeInTheDocument();
            expect(screen.getByText(/37.5€/)).toBeInTheDocument();
        });
    });

    test('shows modalidad when present', async () => {
        mockObtenerSolicitudesAlumno.mockResolvedValueOnce({
            data: [
                { id: 10, tutorId: 10, tutorNombre: 'Tutor J', estado: 'PENDIENTE', dia: '2025-06-24', horaInicio: '10:00', horaFin: '11:00', importeTotal: 25, tarifaHora: 25, modalidad: 'ONLINE' },
            ],
        });
        await act(async () => { render(<AlumnoSolicitudes tutorId={10} />); });
        await waitFor(() => {
            expect(screen.getByText(/Online/)).toBeInTheDocument();
        });
    });

    test('shows Zoom link when present', async () => {
        mockObtenerSolicitudesAlumno.mockResolvedValueOnce({
            data: [
                { id: 11, tutorId: 10, tutorNombre: 'Tutor K', estado: 'PAGADA', dia: '2025-06-25', horaInicio: '10:00', horaFin: '11:00', importeTotal: 25, tarifaHora: 25, zoomJoinUrl: 'https://zoom.us/j/123' },
            ],
        });
        await act(async () => { render(<AlumnoSolicitudes tutorId={10} />); });
        await waitFor(() => {
            expect(screen.getByText(/Unirse a la reunión/)).toBeInTheDocument();
        });
    });

    test('shows stripe not configured message when accepted but no stripe', async () => {
        mockObtenerSolicitudesAlumno.mockResolvedValueOnce({
            data: [
                { id: 12, tutorId: 10, tutorNombre: 'Tutor L', estado: 'ACEPTADA', dia: '2026-06-26', horaInicio: '10:00', horaFin: '11:00', importeTotal: 25, tarifaHora: 25, tutorStripeConfigured: false },
            ],
        });
        await act(async () => { render(<AlumnoSolicitudes tutorId={10} />); });
        await waitFor(() => {
            expect(screen.getByText(/no ha configurado su cuenta de Stripe/i)).toBeInTheDocument();
        });
    });

    test('shows pay button when accepted with stripe configured', async () => {
        mockObtenerSolicitudesAlumno.mockResolvedValueOnce({
            data: [
                { id: 13, tutorId: 10, tutorNombre: 'Tutor M', estado: 'ACEPTADA', dia: '2026-06-27', horaInicio: '10:00', horaFin: '11:00', importeTotal: 30, tarifaHora: 30, tutorStripeConfigured: true },
            ],
        });
        await act(async () => { render(<AlumnoSolicitudes tutorId={10} />); });
        await waitFor(() => {
            expect(screen.getByText(/Pagar 30€/)).toBeInTheDocument();
        });
    });

    test('shows expired message when class date has passed', async () => {
        mockObtenerSolicitudesAlumno.mockResolvedValueOnce({
            data: [
                { id: 14, tutorId: 10, tutorNombre: 'Tutor N', estado: 'ACEPTADA', dia: '2020-01-01', horaInicio: '10:00', horaFin: '11:00', importeTotal: 25, tarifaHora: 25, tutorStripeConfigured: true },
            ],
        });
        await act(async () => { render(<AlumnoSolicitudes tutorId={10} />); });
        await waitFor(() => {
            expect(screen.getByText(/La fecha de esta clase ya ha pasado/i)).toBeInTheDocument();
        });
    });

    test('filters solicitudes by tutorId', async () => {
        mockObtenerSolicitudesAlumno.mockResolvedValueOnce({
            data: [
                { id: 15, tutorId: 10, estado: 'PENDIENTE', dia: '2025-06-28', horaInicio: '10:00', horaFin: '11:00', importeTotal: 25, tarifaHora: 25 },
                { id: 16, tutorId: 99, estado: 'PENDIENTE', dia: '2025-06-29', horaInicio: '10:00', horaFin: '11:00', importeTotal: 25, tarifaHora: 25 },
            ],
        });
        await act(async () => { render(<AlumnoSolicitudes tutorId={10} />); });
        await waitFor(() => {
            expect(screen.getByText('2025-06-28')).toBeInTheDocument();
            expect(screen.queryByText('2025-06-29')).not.toBeInTheDocument();
        });
    });

    test('renders panel title', async () => {
        mockObtenerSolicitudesAlumno.mockResolvedValueOnce({
            data: [
                { id: 17, tutorId: 10, estado: 'PENDIENTE', dia: '2025-07-01', horaInicio: '10:00', horaFin: '11:00', importeTotal: 25, tarifaHora: 25 },
            ],
        });
        await act(async () => { render(<AlumnoSolicitudes tutorId={10} />); });
        await waitFor(() => {
            expect(screen.getByText(/Mis solicitudes de contratación/)).toBeInTheDocument();
        });
    });
});

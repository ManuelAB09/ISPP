import { render, screen, waitFor, act } from '@testing-library/react';
import TutorConversaciones from './TutorConversaciones';

jest.mock('./TutorConversaciones.css', () => ({}));

jest.mock('../../api/baseUrl', () => ({
    getApiBaseUrl: () => 'http://localhost:8080',
}));

const mockObtenerConversaciones = jest.fn();
jest.mock('../../api/mensajeService', () => ({
    obtenerConversaciones: (...args) => mockObtenerConversaciones(...args),
}));

jest.mock('../../contexts/SocketContext', () => ({
    useSocketContext: () => ({
        socket: { on: jest.fn(), off: jest.fn() },
        isConnected: true,
    }),
}));

jest.mock('../chat/PrivateChat', () => ({ tutorId, tutorNombre, onClose }) => (
    <div data-testid="private-chat">Chat with {tutorNombre} <button onClick={onClose}>Close</button></div>
));

describe('TutorConversaciones', () => {
    const usuarioActual = { id: 1, nombre: 'Tutor' };

    beforeEach(() => {
        jest.clearAllMocks();
    });

    test('renders loading state', async () => {
        mockObtenerConversaciones.mockReturnValue(new Promise(() => {}));
        await act(async () => {
            render(<TutorConversaciones usuarioActual={usuarioActual} />);
        });
        expect(screen.getByText(/Cargando conversaciones/)).toBeInTheDocument();
    });

    test('renders empty state', async () => {
        mockObtenerConversaciones.mockResolvedValueOnce({ data: [] });
        await act(async () => {
            render(<TutorConversaciones usuarioActual={usuarioActual} />);
        });
        await waitFor(() => {
            expect(screen.getByText(/no tienes conversaciones/i)).toBeInTheDocument();
        });
    });

    test('renders conversations list', async () => {
        mockObtenerConversaciones.mockResolvedValueOnce({
            data: [
                { usuarioId: 2, usuarioNombre: 'Alumno 1', usuarioFoto: '/photo1.jpg', ultimoMensaje: 'Hola' },
                { usuarioId: 3, usuarioNombre: 'Alumno 2', usuarioFoto: null, ultimoMensaje: '' },
            ],
        });
        await act(async () => {
            render(<TutorConversaciones usuarioActual={usuarioActual} />);
        });
        await waitFor(() => {
            expect(screen.getByText('Alumno 1')).toBeInTheDocument();
            expect(screen.getByText('Alumno 2')).toBeInTheDocument();
        });
    });

    test('opens private chat on click', async () => {
        mockObtenerConversaciones.mockResolvedValueOnce({
            data: [
                { usuarioId: 2, usuarioNombre: 'Alumno 1', usuarioFoto: '/photo.jpg', ultimoMensaje: 'Hi' },
            ],
        });
        await act(async () => {
            render(<TutorConversaciones usuarioActual={usuarioActual} />);
        });

        await waitFor(() => {
            expect(screen.getByText('Alumno 1')).toBeInTheDocument();
        });

        await act(async () => {
            screen.getByText('Alumno 1').click();
        });

        expect(screen.getByTestId('private-chat')).toBeInTheDocument();
    });

    test('handles API error gracefully', async () => {
        mockObtenerConversaciones.mockRejectedValueOnce(new Error('fail'));
        await act(async () => {
            render(<TutorConversaciones usuarioActual={usuarioActual} />);
        });
        await waitFor(() => {
            expect(screen.getByText(/no tienes conversaciones/i)).toBeInTheDocument();
        });
    });

    test('renders title', async () => {
        mockObtenerConversaciones.mockResolvedValueOnce({ data: [] });
        await act(async () => {
            render(<TutorConversaciones usuarioActual={usuarioActual} />);
        });
        await waitFor(() => {
            expect(screen.getByText(/Mis conversaciones/)).toBeInTheDocument();
        });
    });
});

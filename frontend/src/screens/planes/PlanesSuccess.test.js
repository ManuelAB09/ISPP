import { render, screen, waitFor, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import PlanesSuccess from './PlanesSuccess';

jest.mock('../../components/Header/Header', () => () => <div data-testid="header" />);

const mockVerifySession = jest.fn();
jest.mock('../../api/subscriptions.api', () => ({
    subscriptionsApi: {
        verifySession: (...args) => mockVerifySession(...args),
    },
}));

const renderWithParams = (search = '') => {
    return render(
        <MemoryRouter initialEntries={[`/planes/success${search}`]}>
            <PlanesSuccess />
        </MemoryRouter>
    );
};

describe('PlanesSuccess', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        delete window.location;
        window.location = { href: '' };
    });

    test('redirects to /planes when no session_id', () => {
        renderWithParams('');
        expect(window.location.href).toBe('/planes');
    });

    test('shows verifying state', async () => {
        mockVerifySession.mockReturnValue(new Promise(() => {})); // never resolves
        await act(async () => { renderWithParams('?session_id=abc123'); });
        expect(screen.getByText('Verificando tu pago...')).toBeInTheDocument();
    });

    test('shows success state after verify', async () => {
        mockVerifySession.mockResolvedValueOnce({});
        await act(async () => { renderWithParams('?session_id=abc123'); });

        await waitFor(() => {
            expect(screen.getByText(/Bienvenido a Premium/)).toBeInTheDocument();
        });
    });

    test('shows error state on verify failure', async () => {
        mockVerifySession.mockRejectedValueOnce(new Error('fail'));
        await act(async () => { renderWithParams('?session_id=abc123'); });

        await waitFor(() => {
            expect(screen.getByText('Algo salió mal')).toBeInTheDocument();
        });
    });

    test('renders benefits on success', async () => {
        mockVerifySession.mockResolvedValueOnce({});
        await act(async () => { renderWithParams('?session_id=abc123'); });

        await waitFor(() => {
            expect(screen.getByText('Más límites y herramientas')).toBeInTheDocument();
            expect(screen.getByText('Sin publicidad')).toBeInTheDocument();
        });
    });

    test('renders header', async () => {
        mockVerifySession.mockResolvedValueOnce({});
        await act(async () => { renderWithParams('?session_id=abc123'); });
        expect(screen.getByTestId('header')).toBeInTheDocument();
    });
});

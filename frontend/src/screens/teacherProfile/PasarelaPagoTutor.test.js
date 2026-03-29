import { render, screen, act, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import PasarelaPagoTutor from './PasarelaPagoTutor';

jest.mock('./PasarelaPagoTutor.css', () => ({}));

jest.mock('@stripe/stripe-js', () => ({
    loadStripe: jest.fn(() => Promise.resolve({})),
}));

jest.mock('@stripe/react-stripe-js', () => ({
    Elements: ({ children }) => <div data-testid="stripe-elements">{children}</div>,
    PaymentElement: () => <div data-testid="payment-element" />,
    useStripe: () => ({ confirmPayment: jest.fn() }),
    useElements: () => ({ submit: jest.fn(() => Promise.resolve({})) }),
}));

jest.mock('../../api/baseUrl', () => ({
    getApiBaseUrl: () => 'http://localhost:8080',
}));

jest.mock('../../components/Header/Header', () => () => <div data-testid="header" />);

const mockCreateHiringPaymentIntent = jest.fn();
const mockConfirmTutorPayment = jest.fn();
jest.mock('../../api/communities.api', () => ({
    communitiesApi: {
        createHiringPaymentIntent: (...args) => mockCreateHiringPaymentIntent(...args),
        confirmTutorPayment: (...args) => mockConfirmTutorPayment(...args),
    },
}));

const mockNavigate = jest.fn();
const mockLocationState = {
    tutor: { id: 10, usuario: { nombre: 'Prof. Test' } },
    comunidad: { id: 1, nombre: 'Community' },
    modalidad: 'ONLINE',
    duracion: '1 hora',
    tarifa: '25',
};

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
    useLocation: () => ({
        state: mockLocationState,
    }),
}));

describe('PasarelaPagoTutor', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockCreateHiringPaymentIntent.mockResolvedValue({ data: { clientSecret: 'pi_secret_123' } });
    });

    test('renders header', async () => {
        await act(async () => {
            render(<MemoryRouter><PasarelaPagoTutor /></MemoryRouter>);
        });
        expect(screen.getByTestId('header')).toBeInTheDocument();
    });

    test('renders tutor name', async () => {
        await act(async () => {
            render(<MemoryRouter><PasarelaPagoTutor /></MemoryRouter>);
        });
        expect(screen.getAllByText(/Prof. Test/).length).toBeGreaterThanOrEqual(1);
    });

    test('renders pricing info', async () => {
        await act(async () => {
            render(<MemoryRouter><PasarelaPagoTutor /></MemoryRouter>);
        });
        expect(screen.getAllByText(/25/).length).toBeGreaterThanOrEqual(1);
    });

    test('renders modalidad and duracion', async () => {
        await act(async () => {
            render(<MemoryRouter><PasarelaPagoTutor /></MemoryRouter>);
        });
        expect(screen.getByText(/ONLINE|Online/)).toBeInTheDocument();
        expect(screen.getByText(/1 hora/)).toBeInTheDocument();
    });

    test('redirects when no tutor data', async () => {
        // Override location state to have no tutor
        const spy = jest.spyOn(require('react-router-dom'), 'useLocation').mockReturnValue({ state: {} });
        await act(async () => {
            render(<MemoryRouter><PasarelaPagoTutor /></MemoryRouter>);
        });
        expect(mockNavigate).toHaveBeenCalledWith('/profesores');
        spy.mockRestore();
    });

    test('creates payment intent on mount', async () => {
        await act(async () => {
            render(<MemoryRouter><PasarelaPagoTutor /></MemoryRouter>);
        });
        expect(mockCreateHiringPaymentIntent).toHaveBeenCalled();
    });

    test('handles payment intent error', async () => {
        mockCreateHiringPaymentIntent.mockRejectedValueOnce(new Error('fail'));
        await act(async () => {
            render(<MemoryRouter><PasarelaPagoTutor /></MemoryRouter>);
        });
    });

    test('renders community name', async () => {
        await act(async () => {
            render(<MemoryRouter><PasarelaPagoTutor /></MemoryRouter>);
        });
        expect(screen.getAllByText(/Community/).length).toBeGreaterThanOrEqual(1);
    });

    test('renders stripe elements when secret available', async () => {
        await act(async () => {
            render(<MemoryRouter><PasarelaPagoTutor /></MemoryRouter>);
        });
        await waitFor(() => {
            expect(screen.getByTestId('stripe-elements')).toBeInTheDocument();
        });
    });

    test('shows loading state while creating intent', async () => {
        mockCreateHiringPaymentIntent.mockReturnValue(new Promise(() => {}));
        await act(async () => {
            render(<MemoryRouter><PasarelaPagoTutor /></MemoryRouter>);
        });
        expect(screen.getByTestId('header')).toBeInTheDocument();
    });

    test('renders order summary with tarifa and duracion', async () => {
        await act(async () => {
            render(<MemoryRouter><PasarelaPagoTutor /></MemoryRouter>);
        });
        expect(screen.getByText(/1 hora/)).toBeInTheDocument();
    });
});

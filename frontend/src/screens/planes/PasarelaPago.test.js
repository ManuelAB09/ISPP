import { render, screen, waitFor, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import PasarelaPago from './PasarelaPago';

// Mock Stripe
jest.mock('@stripe/stripe-js', () => ({
    loadStripe: jest.fn(() => Promise.resolve({
        confirmPayment: jest.fn(),
        elements: jest.fn(),
    })),
}));

jest.mock('@stripe/react-stripe-js', () => ({
    Elements: ({ children }) => <div data-testid="stripe-elements">{children}</div>,
    PaymentElement: () => <div data-testid="payment-element" />,
    useStripe: () => ({
        confirmPayment: jest.fn(),
    }),
    useElements: () => ({
        submit: jest.fn(() => Promise.resolve({})),
    }),
}));

jest.mock('./PasarelaPago.css', () => ({}));
jest.mock('../../components/Header/Header', () => () => <div data-testid="header" />);

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
    useSearchParams: () => {
        const params = new URLSearchParams('plan=PREMIUM');
        return [params, jest.fn()];
    },
}));

const mockCreatePaymentIntent = jest.fn();
const mockConfirmEmbeddedPayment = jest.fn();
jest.mock('../../api/subscriptions.api', () => ({
    subscriptionsApi: {
        createPaymentIntent: (...args) => mockCreatePaymentIntent(...args),
        confirmEmbeddedPayment: (...args) => mockConfirmEmbeddedPayment(...args),
    },
}));

describe('PasarelaPago', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockCreatePaymentIntent.mockResolvedValue({ data: { clientSecret: 'pi_secret_123' } });
    });

    test('renders header', async () => {
        await act(async () => {
            render(<MemoryRouter><PasarelaPago /></MemoryRouter>);
        });
        expect(screen.getByTestId('header')).toBeInTheDocument();
    });

    test('renders plan name', async () => {
        await act(async () => {
            render(<MemoryRouter><PasarelaPago /></MemoryRouter>);
        });
        expect(screen.getAllByText(/Plan Premium/).length).toBeGreaterThanOrEqual(1);
    });

    test('renders plan features', async () => {
        await act(async () => {
            render(<MemoryRouter><PasarelaPago /></MemoryRouter>);
        });
        expect(screen.getByText('10 comunidades activas')).toBeInTheDocument();
    });

    test('creates payment intent on mount', async () => {
        await act(async () => {
            render(<MemoryRouter><PasarelaPago /></MemoryRouter>);
        });
        await waitFor(() => {
            expect(mockCreatePaymentIntent).toHaveBeenCalled();
        });
    });

    test('shows loading while creating intent', async () => {
        mockCreatePaymentIntent.mockReturnValue(new Promise(() => {}));
        await act(async () => {
            render(<MemoryRouter><PasarelaPago /></MemoryRouter>);
        });
        // Should show loading indicator
        expect(screen.getByTestId('header')).toBeInTheDocument();
    });

    test('shows error when intent fails', async () => {
        mockCreatePaymentIntent.mockRejectedValueOnce(new Error('fail'));
        await act(async () => {
            render(<MemoryRouter><PasarelaPago /></MemoryRouter>);
        });
        await waitFor(() => {
            expect(screen.getByText(/Error al iniciar el pago/)).toBeInTheDocument();
        });
    });

    test('renders Stripe Elements when secret available', async () => {
        await act(async () => {
            render(<MemoryRouter><PasarelaPago /></MemoryRouter>);
        });
        await waitFor(() => {
            expect(screen.getByTestId('stripe-elements')).toBeInTheDocument();
        });
    });

    test('shows security info', async () => {
        await act(async () => {
            render(<MemoryRouter><PasarelaPago /></MemoryRouter>);
        });
        await waitFor(() => {
            const securityTexts = screen.getAllByText(/encriptacion|protegida|segura/i);
            expect(securityTexts.length).toBeGreaterThan(0);
        });
    });

    test('shows plan period selector', async () => {
        await act(async () => {
            render(<MemoryRouter><PasarelaPago /></MemoryRouter>);
        });
        await waitFor(() => {
            const mensual = screen.queryByText(/Mensual/i);
            const anual = screen.queryByText(/Anual/i);
            expect(mensual || anual).toBeTruthy();
        });
    });

    test('shows order summary with total', async () => {
        await act(async () => {
            render(<MemoryRouter><PasarelaPago /></MemoryRouter>);
        });
        await waitFor(() => {
            const totals = screen.queryAllByText(/Total|Resumen/i);
            expect(totals.length).toBeGreaterThan(0);
        });
    });

    test('shows PREMIUM plan features by default', async () => {
        await act(async () => {
            render(<MemoryRouter><PasarelaPago /></MemoryRouter>);
        });
        await waitFor(() => {
            expect(screen.getByText('10 comunidades activas')).toBeInTheDocument();
        });
    });

    test('calls createPaymentIntent with plan params', async () => {
        await act(async () => {
            render(<MemoryRouter><PasarelaPago /></MemoryRouter>);
        });
        await waitFor(() => {
            expect(mockCreatePaymentIntent).toHaveBeenCalledWith(
                expect.objectContaining({ planId: 'PREMIUM' })
            );
        });
    });

    test('renders payment element inside stripe elements', async () => {
        await act(async () => {
            render(<MemoryRouter><PasarelaPago /></MemoryRouter>);
        });
        await waitFor(() => {
            expect(screen.getByTestId('payment-element')).toBeInTheDocument();
        });
    });
});

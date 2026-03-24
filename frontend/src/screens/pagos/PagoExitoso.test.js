import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import PagoExitoso from './PagoExitoso';

jest.mock('../../api/subscriptions.api', () => ({
  subscriptionsApi: {
    verifySession: jest.fn(),
    confirmPayment: jest.fn(),
  },
}));

jest.mock('../../api/institutions.api', () => ({
  institutionsApi: {
    verifySession: jest.fn(),
  },
}));

jest.mock('../../components/Header/Header', () => {
  return function MockHeader() {
    return <div data-testid="mock-header">Header</div>;
  };
});

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
  useSearchParams: () => [new URLSearchParams('session_id=sess_123&tipo=suscripcion')],
}));

const { subscriptionsApi } = require('../../api/subscriptions.api');

describe('PagoExitoso', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  const renderComponent = () =>
    render(
      <MemoryRouter>
        <PagoExitoso />
      </MemoryRouter>
    );

  test('shows loading state initially', () => {
    subscriptionsApi.verifySession.mockReturnValue(new Promise(() => {}));
    renderComponent();

    expect(screen.getByText(/activando/i)).toBeInTheDocument();
  });

  test('shows success message after verification', async () => {
    subscriptionsApi.verifySession.mockResolvedValue({});
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/pago realizado con éxito/i)).toBeInTheDocument();
    });
  });

  test('renders action buttons', async () => {
    subscriptionsApi.verifySession.mockResolvedValue({});
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/ir al inicio/i)).toBeInTheDocument();
    });
  });

  test('shows error state on API failure', async () => {
    subscriptionsApi.verifySession.mockRejectedValue(new Error('Server error'));
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/pago recibido/i)).toBeInTheDocument();
    });
  });
});

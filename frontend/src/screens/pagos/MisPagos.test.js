import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import MisPagos from './MisPagos';

jest.mock('../../api/payments.api', () => ({
  paymentsApi: {
    getHistory: jest.fn(),
  },
}));

jest.mock('../../components/Header/Header', () => {
  return function MockHeader() {
    return <div data-testid="mock-header">Header</div>;
  };
});

jest.mock('../../components/PageHeader', () => {
  return function MockPageHeader({ title }) {
    return <div data-testid="page-header">{title}</div>;
  };
});

const { paymentsApi } = require('../../api/payments.api');

describe('MisPagos', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  const renderComponent = () =>
    render(
      <MemoryRouter>
        <MisPagos />
      </MemoryRouter>
    );

  test('renders header and page title', async () => {
    paymentsApi.getHistory.mockResolvedValue({ content: [], totalPages: 0 });
    renderComponent();

    expect(screen.getByTestId('mock-header')).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.getByText('Mis pagos')).toBeInTheDocument();
    });
  });

  test('shows loading state initially', () => {
    paymentsApi.getHistory.mockReturnValue(new Promise(() => {})); // never resolves
    renderComponent();

    expect(screen.getByText(/cargando/i)).toBeInTheDocument();
  });

  test('shows empty state when no transactions', async () => {
    paymentsApi.getHistory.mockResolvedValue({ content: [], totalPages: 0 });
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/sin transacciones/i)).toBeInTheDocument();
    });
  });

  test('renders transaction table with data', async () => {
    paymentsApi.getHistory.mockResolvedValue({
      content: [
        {
          id: 1,
          tipo: 'SUSCRIPCION',
          monto: 2.99,
          estado: 'COMPLETADO',
          descripcion: 'Premium mensual',
          fechaCreacion: '2025-01-15T10:00:00',
        },
      ],
      totalPages: 1,
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/suscripción premium/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/completado/i)).toBeInTheDocument();
  });

  test('shows error state on API failure', async () => {
    paymentsApi.getHistory.mockRejectedValue(new Error('Network error'));
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/no se pudo cargar/i)).toBeInTheDocument();
    });
  });
});

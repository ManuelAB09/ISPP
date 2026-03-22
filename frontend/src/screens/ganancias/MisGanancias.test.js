import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import MisGanancias from './MisGanancias';

jest.mock('../../api/tutorEndpoints', () => ({
  obtenerGananciasTutor: jest.fn(),
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

const { obtenerGananciasTutor } = require('../../api/tutorEndpoints');

describe('MisGanancias', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  const renderComponent = () =>
    render(
      <MemoryRouter>
        <MisGanancias />
      </MemoryRouter>
    );

  test('renders header and page title', async () => {
    obtenerGananciasTutor.mockResolvedValue({ content: [], totalPages: 0 });
    renderComponent();

    expect(screen.getByTestId('mock-header')).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.getByText('Mis ganancias')).toBeInTheDocument();
    });
  });

  test('shows loading state initially', () => {
    obtenerGananciasTutor.mockReturnValue(new Promise(() => {}));
    renderComponent();

    expect(screen.getByText(/cargando/i)).toBeInTheDocument();
  });

  test('shows empty state when no earnings', async () => {
    obtenerGananciasTutor.mockResolvedValue({ content: [], totalPages: 0 });
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/sin ganancias/i)).toBeInTheDocument();
    });
  });

  test('renders earnings table with data', async () => {
    obtenerGananciasTutor.mockResolvedValue({
      content: [
        { id: 1, monto: 50, comision: 5, montoNeto: 45, fecha: '2025-01-15T10:00:00' },
      ],
      totalPages: 1,
      totalNeto: 45,
      totalBruto: 50,
      totalComisiones: 5,
      totalTransacciones: 1,
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/completado/i)).toBeInTheDocument();
    });
  });

  test('shows error state on API failure', async () => {
    obtenerGananciasTutor.mockRejectedValue(new Error('Network error'));
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/no se pudieron cargar/i)).toBeInTheDocument();
    });
  });
});

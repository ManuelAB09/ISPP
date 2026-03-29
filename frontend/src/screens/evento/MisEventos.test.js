import React from 'react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

jest.mock('../../contexts/AuthContext', () => ({
  useAuth: () => ({ user: { id: 1, nombre: 'Test' } }),
}));
jest.mock('../../utils/myEventsUtils', () => ({
  getMisEventos: jest.fn(),
  getMisEventosHistorial: jest.fn(),
}));
jest.mock('../../components/Header/Header', () => () => <div data-testid="header">Header</div>);
jest.mock('./MisEventos.css', () => ({}));

const { getMisEventos, getMisEventosHistorial } = require('../../utils/myEventsUtils');

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

const MisEventos = require('./MisEventos').default;

describe('MisEventos', () => {
  beforeEach(() => jest.clearAllMocks());

  test('shows loading initially', () => {
    getMisEventos.mockReturnValue(new Promise(() => {}));
    getMisEventosHistorial.mockReturnValue(new Promise(() => {}));
    render(<MemoryRouter><MisEventos /></MemoryRouter>);
    expect(screen.getByTestId('header')).toBeInTheDocument();
  });

  test('shows empty state when no events', async () => {
    getMisEventos.mockResolvedValue([]);
    getMisEventosHistorial.mockResolvedValue([]);
    render(<MemoryRouter><MisEventos /></MemoryRouter>);
    await screen.findByText(/no tienes/i);
  });

  test('renders events', async () => {
    getMisEventos.mockResolvedValue([
      {
        id: 1,
        titulo: 'Study Session',
        tipoEvento: 'REUNION',
        fechaHora: '2025-04-01T10:00:00',
        comunidadNombre: 'Dev',
        esVirtual: true,
        cancelado: false,
        asistenciaConfirmada: true,
        aforo: 10,
      },
    ]);
    getMisEventosHistorial.mockResolvedValue([]);
    render(<MemoryRouter><MisEventos /></MemoryRouter>);
    await screen.findByText('Study Session');
  });

  test('handles API failure', async () => {
    getMisEventos.mockRejectedValue(new Error('fail'));
    getMisEventosHistorial.mockRejectedValue(new Error('fail'));
    render(<MemoryRouter><MisEventos /></MemoryRouter>);
    await screen.findByTestId('header');
  });
});

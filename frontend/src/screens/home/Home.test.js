import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Home from './Home';

jest.mock('../../api/communities.api', () => ({
  communitiesApi: {
    listMine: jest.fn(),
  },
}));

jest.mock('../../components/Header/Header', () => {
  return function MockHeader() {
    return <div data-testid="mock-header">Header</div>;
  };
});

jest.mock('../../components/Comunidad/ComunidadCard', () => {
  return function MockCard({ comunidad }) {
    return <div data-testid="community-card">{comunidad.nombre}</div>;
  };
});

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

const { communitiesApi } = require('../../api/communities.api');

describe('Home', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();
  });

  const renderComponent = () =>
    render(
      <MemoryRouter>
        <Home />
      </MemoryRouter>
    );

  test('renders header', () => {
    renderComponent();
    expect(screen.getByTestId('mock-header')).toBeInTheDocument();
  });

  test('shows login prompt when not authenticated', async () => {
    localStorage.removeItem('accessToken');
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/inicia sesión/i)).toBeInTheDocument();
    });
  });

  test('fetches communities when authenticated', async () => {
    localStorage.setItem('accessToken', 'test-token');
    localStorage.setItem('userId', '1');

    communitiesApi.listMine.mockResolvedValue({
      content: [
        { id: 1, nombre: 'Test Community', miRol: 'MIEMBRO' },
      ],
    });

    renderComponent();

    await waitFor(() => {
      expect(communitiesApi.listMine).toHaveBeenCalled();
    });
  });

  test('shows community cards after loading', async () => {
    localStorage.setItem('accessToken', 'test-token');
    localStorage.setItem('userId', '1');

    communitiesApi.listMine.mockResolvedValue({
      content: [
        { id: 1, nombre: 'My Community', miRol: 'MIEMBRO' },
      ],
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('My Community')).toBeInTheDocument();
    });
  });

  test('shows error message on API failure', async () => {
    localStorage.setItem('accessToken', 'test-token');
    communitiesApi.listMine.mockRejectedValue(new Error('Network error'));

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/no se pudieron cargar/i)).toBeInTheDocument();
    });
  });
});

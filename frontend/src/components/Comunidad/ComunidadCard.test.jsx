import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import ComunidadCard from './ComunidadCard';
import { communitiesApi } from '../../api/communities.api';

// Mocks
jest.mock('../../api/communities.api');
jest.mock('../icons/Person', () => {
  return function MockPersonIcon() {
    return <span data-testid="person-icon">👤</span>;
  };
});

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

describe('ComunidadCard', () => {
  const mockComunidad = {
    id: 1,
    nombre: 'Física Elemental',
    descripcion: 'Aprende física desde cero',
    imagen: 'https://example.com/physics.jpg',
    miembrosActuales: 30,
    maxMiembros: 50,
    categoria: ['Física', 'Ciencia'],
    esMiembro: false,
  };

  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();
    communitiesApi.join.mockResolvedValue({});
  });

  const renderComponent = (comunidad = mockComunidad, onJoined = jest.fn()) => {
    return render(
      <MemoryRouter>
        <ComunidadCard comunidad={comunidad} onJoined={onJoined} />
      </MemoryRouter>
    );
  };

  test('renderiza el nombre de la comunidad', () => {
    renderComponent();
    expect(screen.getByRole('heading', { name: /Física Elemental/i })).toBeInTheDocument();
  });

  test('renderiza la descripción de la comunidad', () => {
    renderComponent();
    expect(screen.getByText(/Aprende física desde cero/i)).toBeInTheDocument();
  });

  test('muestra descripción por defecto cuando no hay descripción', () => {
    renderComponent({ ...mockComunidad, descripcion: null });
    expect(screen.getByText(/Sin descripción disponible/i)).toBeInTheDocument();
  });

  test('renderiza las categorías', () => {
    renderComponent();
    expect(screen.getByText('Física')).toBeInTheDocument();
    expect(screen.getByText('Ciencia')).toBeInTheDocument();
  });

  test('muestra el número de miembros actuales y máximos', () => {
    renderComponent();
    expect(screen.getByText(/30/)).toBeInTheDocument();
    expect(screen.getByText(/50/)).toBeInTheDocument();
  });

  test('navega al detalle al hacer clic en la tarjeta', async () => {
    renderComponent();

    const cardTitle = screen.getByRole('heading', { name: /Física Elemental/i });
    userEvent.click(cardTitle);

    expect(mockNavigate).toHaveBeenCalledWith('/comunidades/1');
  });

  test('muestra botón de unirse para usuarios logueados', () => {
    localStorage.setItem('userId', '100');
    renderComponent();
    expect(screen.getByRole('button', { name: /Unirse/i })).toBeInTheDocument();
  });

  test('muestra botón de unirse para usuarios no logueados (redirige a login)', () => {
    renderComponent();
    expect(screen.getByRole('button', { name: /Unirse/i })).toBeInTheDocument();
  });

  test('redirige a login cuando usuario no logueado intenta unirse', async () => {
    renderComponent();

    const joinButton = screen.getByRole('button', { name: /Unirse/i });
    userEvent.click(joinButton);

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/login');
    });
  });

  test('llama a la API de join al unirse', async () => {
    localStorage.setItem('userId', '100');
    localStorage.setItem('accessToken', 'test-token');
    renderComponent();

    const joinButton = screen.getByRole('button', { name: /Unirse/i });
    userEvent.click(joinButton);

    await waitFor(() => {
      expect(communitiesApi.join).toHaveBeenCalledWith(1);
    });
  });

  test('muestra estado de unido después de unirse exitosamente', async () => {
    localStorage.setItem('userId', '100');
    localStorage.setItem('accessToken', 'test-token');
    renderComponent();

    const joinButton = screen.getByRole('button', { name: /Unirse/i });
    userEvent.click(joinButton);

    await screen.findByRole('button', { name: /Unido/i });
  });

  test('muestra estado de unido si ya es miembro', () => {
    localStorage.setItem('userId', '100');
    renderComponent({ ...mockComunidad, esMiembro: true });

    expect(screen.getByRole('button', { name: /Unido/i })).toBeInTheDocument();
  });

  test('el botón de unido está deshabilitado', () => {
    localStorage.setItem('userId', '100');
    renderComponent({ ...mockComunidad, esMiembro: true });

    const joinedButton = screen.getByRole('button', { name: /Unido/i });
    expect(joinedButton).toBeDisabled();
  });

  test('muestra error cuando falla la unión', async () => {
    localStorage.setItem('userId', '100');
    localStorage.setItem('accessToken', 'test-token');
    communitiesApi.join.mockRejectedValue(new Error('Server error'));

    renderComponent();

    const joinButton = screen.getByRole('button', { name: /Unirse/i });
    userEvent.click(joinButton);

    await screen.findByText(/Error al unirse/i);
  });

  test('muestra estado de carga mientras se une', async () => {
    localStorage.setItem('userId', '100');
    localStorage.setItem('accessToken', 'test-token');
    communitiesApi.join.mockImplementation(
      () => new Promise((resolve) => setTimeout(resolve, 100))
    );

    renderComponent();

    const joinButton = screen.getByRole('button', { name: /Unirse/i });
    userEvent.click(joinButton);

    await screen.findByRole('button', { name: /Uniéndose/i });
  });

  test('llama a callback onJoined después de unirse', async () => {
    localStorage.setItem('userId', '100');
    localStorage.setItem('accessToken', 'test-token');
    const onJoined = jest.fn();
    renderComponent(mockComunidad, onJoined);

    const joinButton = screen.getByRole('button', { name: /Unirse/i });
    userEvent.click(joinButton);

    await waitFor(() => {
      expect(onJoined).toHaveBeenCalledWith(1);
    });
  });

  test('maneja 409 conflict como ya unido', async () => {
    localStorage.setItem('userId', '100');
    localStorage.setItem('accessToken', 'test-token');
    communitiesApi.join.mockRejectedValue({ status: 409 });

    renderComponent();

    const joinButton = screen.getByRole('button', { name: /Unirse/i });
    userEvent.click(joinButton);

    await screen.findByRole('button', { name: /Unido/i });
  });

  test('renderiza imagen de la comunidad', () => {
    renderComponent();
    const image = screen.getByRole('img', { name: /Física Elemental/i });
    expect(image).toHaveAttribute('src', 'https://example.com/physics.jpg');
  });

  test('usa imagen por defecto cuando no hay imagen', () => {
    renderComponent({ ...mockComunidad, imagen: null });
    const image = screen.getByRole('img', { name: /Física Elemental/i });
    expect(image).toHaveAttribute('src', expect.stringContaining('unsplash.com'));
  });

  test('renderiza el icono de persona', () => {
    renderComponent();
    expect(screen.getByTestId('person-icon')).toBeInTheDocument();
  });
});

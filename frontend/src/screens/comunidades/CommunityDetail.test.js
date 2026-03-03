import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import CommunityDetail from './CommunityDetail';
import { communitiesApi } from '../../api/communities.api';
import * as eventEndpoints from '../../api/eventEndpoints';
import { useAuth } from '../../contexts/AuthContext';

// Mocks
jest.mock('../../api/communities.api');
jest.mock('../../api/eventEndpoints');
jest.mock('../../contexts/AuthContext');
jest.mock('../../components/Header/Header', () => {
  return function MockHeader() {
    return <div data-testid="mock-header">Header</div>;
  };
});
jest.mock('../chat/CommunityChat', () => {
  return function MockCommunityChat() {
    return <div data-testid="mock-community-chat">Chat</div>;
  };
});
jest.mock('../../components/Evento/TarjetaEvento', () => {
  return function MockTarjetaEvento({ event }) {
    return <div data-testid="tarjeta-evento">{event.titulo}</div>;
  };
});

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

describe('CommunityDetail', () => {
  const mockCommunity = {
    id: 1,
    nombre: 'Comunidad de Matemáticas',
    descripcion: 'Una comunidad para amantes de las matemáticas',
    imagen: 'https://example.com/image.jpg',
    miembrosActuales: 25,
    categoria: ['Matemáticas', 'Ciencia'],
    esMiembro: false,
  };

  const mockEvents = [
    {
      id: 1,
      titulo: 'Clase de Álgebra',
      fecha: '2026-03-15',
      asistentesConfirmados: 10,
    },
    {
      id: 2,
      titulo: 'Workshop de Cálculo',
      fecha: '2026-03-20',
      asistentesConfirmados: 5,
    },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();

    useAuth.mockReturnValue({
      user: null,
    });

    communitiesApi.getById.mockResolvedValue(mockCommunity);
    communitiesApi.join.mockResolvedValue({});
    communitiesApi.leave.mockResolvedValue({});
    communitiesApi.getMyMembership.mockRejectedValue({ status: 404 });

    eventEndpoints.listCommunityEvents.mockResolvedValue(mockEvents);
    eventEndpoints.getMyAttendance.mockResolvedValue(null);
  });

  const renderComponent = async (communityId = '1') => {
    render(
      <MemoryRouter initialEntries={[`/comunidades/${communityId}`]}>
        <Routes>
          <Route path="/comunidades/:communityId" element={<CommunityDetail />} />
          <Route path="/login" element={<div>Login Page</div>} />
        </Routes>
      </MemoryRouter>
    );
    await waitFor(() => {
      expect(communitiesApi.getById).toHaveBeenCalledWith(communityId);
    });
  };

  test('muestra estado de carga inicialmente', async () => {
    communitiesApi.getById.mockImplementation(() => new Promise(() => {}));
    eventEndpoints.listCommunityEvents.mockImplementation(() => new Promise(() => {}));
    render(
      <MemoryRouter initialEntries={['/comunidades/1']}>
        <Routes>
          <Route path="/comunidades/:communityId" element={<CommunityDetail />} />
        </Routes>
      </MemoryRouter>
    );
    expect(screen.getByText(/Cargando comunidad/i)).toBeInTheDocument();
  });

  test('renderiza el nombre de la comunidad', async () => {
    await renderComponent();
    await screen.findByRole('heading', { name: /Comunidad de Matemáticas/i });
  });

  test('renderiza la descripción de la comunidad', async () => {
    await renderComponent();
    await screen.findByText(/Una comunidad para amantes de las matemáticas/i);
  });

  test('renderiza las categorías de la comunidad', async () => {
    await renderComponent();
    await screen.findByText('Matemáticas');
    expect(screen.getByText('Ciencia')).toBeInTheDocument();
  });

  test('muestra el número de miembros', async () => {
    await renderComponent();
    await screen.findByText(/25 miembros/i);
  });

  test('renderiza el Header', async () => {
    await renderComponent();
    expect(screen.getByTestId('mock-header')).toBeInTheDocument();
  });

  test('muestra error cuando falla la carga de comunidad', async () => {
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    communitiesApi.getById.mockRejectedValue(new Error('Network error'));
    await renderComponent();
    await screen.findByText(/No se pudo cargar la comunidad/i);
    consoleSpy.mockRestore();
  });

  test('muestra botón de unirse para usuarios no logueados', async () => {
    await renderComponent();
    await screen.findByRole('button', { name: /Inicia sesión para unirte/i });
  });

  test('redirige a login cuando usuario no logueado intenta unirse', async () => {
    await renderComponent();

    const joinButton = await screen.findByRole('button', { name: /Inicia sesión para unirte/i });
    userEvent.click(joinButton);

    expect(mockNavigate).toHaveBeenCalledWith('/login');
  });

  test('muestra botón de unirse para usuarios logueados no miembros', async () => {
    localStorage.setItem('userId', '100');
    useAuth.mockReturnValue({
      user: { id: 100, nombre: 'Test User' },
    });

    await renderComponent();
    await screen.findByRole('button', { name: /Unirse a la comunidad/i });
  });

  test('puede unirse a la comunidad', async () => {
    localStorage.setItem('userId', '100');
    useAuth.mockReturnValue({
      user: { id: 100, nombre: 'Test User' },
    });

    await renderComponent();

    const joinButton = await screen.findByRole('button', { name: /Unirse a la comunidad/i });
    userEvent.click(joinButton);

    await waitFor(() => {
      expect(communitiesApi.join).toHaveBeenCalledWith('1');
    });
  });

  test('muestra botón de abandonar para miembros', async () => {
    localStorage.setItem('userId', '100');
    useAuth.mockReturnValue({
      user: { id: 100, nombre: 'Test User' },
    });
    communitiesApi.getById.mockResolvedValue({
      ...mockCommunity,
      esMiembro: true,
    });

    await renderComponent();
    await screen.findByRole('button', { name: /Abandonar comunidad/i });
  });

  test('puede abandonar la comunidad', async () => {
    localStorage.setItem('userId', '100');
    useAuth.mockReturnValue({
      user: { id: 100, nombre: 'Test User' },
    });
    communitiesApi.getById.mockResolvedValue({
      ...mockCommunity,
      esMiembro: true,
    });

    await renderComponent();

    const leaveButton = await screen.findByRole('button', { name: /Abandonar comunidad/i });
    userEvent.click(leaveButton);

    await waitFor(() => {
      expect(communitiesApi.leave).toHaveBeenCalledWith('1');
    });
  });

  test('muestra error especial al intentar abandonar siendo único admin', async () => {
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    localStorage.setItem('userId', '100');
    useAuth.mockReturnValue({
      user: { id: 100, nombre: 'Test User' },
    });
    communitiesApi.getById.mockResolvedValue({
      ...mockCommunity,
      esMiembro: true,
    });
    communitiesApi.leave.mockRejectedValue({ status: 400 });

    await renderComponent();

    const leaveButton = await screen.findByRole('button', { name: /Abandonar comunidad/i });
    userEvent.click(leaveButton);

    await screen.findByText(/No puedes abandonar siendo el único admin/i);
    consoleSpy.mockRestore();
  });

  test('renderiza sección de eventos', async () => {
    await renderComponent();
    await screen.findByRole('heading', { name: /Eventos/i });
  });

  test('muestra los eventos de la comunidad', async () => {
    await renderComponent();
    await screen.findByText('Clase de Álgebra');
    expect(screen.getByText('Workshop de Cálculo')).toBeInTheDocument();
  });

  test('muestra mensaje cuando no hay eventos', async () => {
    eventEndpoints.listCommunityEvents.mockResolvedValue([]);
    await renderComponent();
    await screen.findByText(/No hay eventos/i);
  });

  test('muestra botón de crear evento para miembros', async () => {
    localStorage.setItem('userId', '100');
    useAuth.mockReturnValue({
      user: { id: 100, nombre: 'Test User' },
    });
    communitiesApi.getById.mockResolvedValue({
      ...mockCommunity,
      esMiembro: true,
    });

    await renderComponent();
    await screen.findByRole('button', { name: /Crear evento/i });
  });

  test('no muestra botón de crear evento para no miembros', async () => {
    localStorage.setItem('userId', '100');
    useAuth.mockReturnValue({
      user: { id: 100, nombre: 'Test User' },
    });

    await renderComponent();
    await screen.findByText('Comunidad de Matemáticas');

    expect(screen.queryByRole('button', { name: /Crear evento/i })).not.toBeInTheDocument();
  });

  test('muestra checkbox para filtrar eventos cancelados', async () => {
    await renderComponent();
    await screen.findByLabelText(/Mostrar cancelados/i);
  });

  test('renderiza botón de volver', async () => {
    await renderComponent();
    await screen.findByRole('button', { name: /Volver a comunidades/i });
  });

  test('navega hacia atrás al hacer clic en volver', async () => {
    await renderComponent();

    const backButton = await screen.findByRole('button', { name: /Volver a comunidades/i });
    userEvent.click(backButton);

    expect(mockNavigate).toHaveBeenCalledWith('/comunidades');
  });

  test('muestra chat para miembros logueados', async () => {
    localStorage.setItem('userId', '100');
    useAuth.mockReturnValue({
      user: { id: 100, nombre: 'Test User' },
    });
    communitiesApi.getById.mockResolvedValue({
      ...mockCommunity,
      esMiembro: true,
    });

    await renderComponent();
    await screen.findByTestId('mock-community-chat');
  });

  test('no muestra chat para no miembros', async () => {
    await renderComponent();
    await screen.findByText('Comunidad de Matemáticas');

    expect(screen.queryByTestId('mock-community-chat')).not.toBeInTheDocument();
  });
});

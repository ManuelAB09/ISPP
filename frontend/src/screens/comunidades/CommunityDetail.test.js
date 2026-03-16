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
    communitiesApi.expelMember.mockResolvedValue({});
    communitiesApi.getMyMembership.mockRejectedValue({ status: 404 });
    communitiesApi.getMembers.mockResolvedValue([]);

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
      expect(communitiesApi.join).toHaveBeenCalledWith('1', 'ALUMNO');
    });
  });

  test('si tiene perfil docente, al unirse muestra selector de rol', async () => {
    localStorage.setItem('userId', '100');
    useAuth.mockReturnValue({
      user: { id: 100, nombre: 'Test User', esTutor: true },
    });

    await renderComponent();

    const joinButton = await screen.findByRole('button', { name: /Unirse a la comunidad/i });
    userEvent.click(joinButton);

    await screen.findByText(/Elige cómo quieres unirte/i);
    expect(screen.getByRole('button', { name: /Unirme como profesor/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Unirme como alumno/i })).toBeInTheDocument();
  });

  test('si tiene perfil docente, puede unirse como profesor', async () => {
    localStorage.setItem('userId', '100');
    useAuth.mockReturnValue({
      user: { id: 100, nombre: 'Test User', esTutor: true },
    });

    await renderComponent();

    const joinButton = await screen.findByRole('button', { name: /Unirse a la comunidad/i });
    userEvent.click(joinButton);

    const joinAsTeacherButton = await screen.findByRole('button', { name: /Unirme como profesor/i });
    userEvent.click(joinAsTeacherButton);

    await waitFor(() => {
      expect(communitiesApi.join).toHaveBeenCalledWith('1', 'PROFESOR');
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
      user: { id: 100, nombre: 'Test User', esTutor: true },
    });
    communitiesApi.getById.mockResolvedValue({
      ...mockCommunity,
      esMiembro: true,
      miRol: 'PROFESOR',
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

  test('muestra el sistema de roles y el listado de administradores', async () => {
    communitiesApi.getById.mockResolvedValue({
      ...mockCommunity,
      esMiembro: true,
      miRol: 'ADMIN',
      tipoPlan: 'CORPORATIVO',
      maxMiembros: null,
    });
    communitiesApi.getMembers.mockResolvedValue([
      { id: 10, rol: 'ADMIN', usuario: { id: 10, nombre: 'Ana Admin' } },
      { id: 11, rol: 'ADMIN', usuario: { id: 11, nombre: 'Carlos Admin' } },
      { id: 12, rol: 'PROFESOR', usuario: { id: 12, nombre: 'Paula Profe' } },
      { id: 13, rol: 'MIEMBRO', usuario: { id: 13, nombre: 'Alberto Alumno' } },
    ]);

    await renderComponent();

    await screen.findByText(/Sistema de roles/i);
    expect(screen.getByText(/Tu rol: Administrador/i)).toBeInTheDocument();
    expect(screen.getByText(/Plan corporativo/i)).toBeInTheDocument();
    expect(screen.getByText('Ana Admin')).toBeInTheDocument();
    expect(screen.getByText('Carlos Admin')).toBeInTheDocument();
    expect(screen.getByText('Paula Profe')).toBeInTheDocument();
  });

  test('resuelve correctamente la foto relativa de un miembro', async () => {
    communitiesApi.getById.mockResolvedValue({
      ...mockCommunity,
      esMiembro: true,
      miRol: 'ADMIN',
    });
    communitiesApi.getMembers.mockResolvedValue([
      { id: 12, rol: 'PROFESOR', usuario: { id: 12, nombre: 'Paula Profe', foto: '/uploads/paula.png' } },
    ]);

    await renderComponent();

    const avatar = await screen.findByRole('img', { name: 'Paula Profe' });
    expect(avatar).toHaveAttribute('src', 'http://localhost:8080/uploads/paula.png');
  });

  test('resuelve correctamente la foto de un miembro cuando llega en fotoPerfil', async () => {
    communitiesApi.getById.mockResolvedValue({
      ...mockCommunity,
      esMiembro: true,
      miRol: 'ADMIN',
    });
    communitiesApi.getMembers.mockResolvedValue([
      { id: 12, rol: 'PROFESOR', usuario: { id: 12, nombre: 'Paula Profe', fotoPerfil: '/uploads/paula-perfil.png' } },
    ]);

    await renderComponent();

    const avatar = await screen.findByRole('img', { name: 'Paula Profe' });
    expect(avatar).toHaveAttribute('src', 'http://localhost:8080/uploads/paula-perfil.png');
  });

  test('resuelve correctamente la foto de un miembro cuando llega en avatarUrl', async () => {
    communitiesApi.getById.mockResolvedValue({
      ...mockCommunity,
      esMiembro: true,
      miRol: 'ADMIN',
    });
    communitiesApi.getMembers.mockResolvedValue([
      { id: 12, rol: 'PROFESOR', usuario: { id: 12, nombre: 'Paula Profe', avatarUrl: '/static/images/renata/avatar-1.png' } },
    ]);

    await renderComponent();

    const avatar = await screen.findByRole('img', { name: 'Paula Profe' });
    expect(avatar).toHaveAttribute('src', 'http://localhost:8080/static/images/renata/avatar-1.png');
  });

  test('permite abrir el perfil de un miembro desde el listado', async () => {
    localStorage.setItem('userId', '100');
    useAuth.mockReturnValue({
      user: { id: 100, nombre: 'Admin User' },
    });
    communitiesApi.getById.mockResolvedValue({
      ...mockCommunity,
      esMiembro: true,
      miRol: 'ADMIN',
    });
    communitiesApi.getMembers.mockResolvedValue([
      { id: 10, rol: 'ADMIN', usuario: { id: 100, nombre: 'Admin User' } },
      { id: 12, rol: 'PROFESOR', usuario: { id: 12, nombre: 'Paula Profe' } },
    ]);

    await renderComponent();

    const memberButton = await screen.findByRole('button', { name: /Paula Profe/i });
    userEvent.click(memberButton);

    expect(mockNavigate).toHaveBeenCalledWith('/perfil/12');
  });

  test('permite al administrador expulsar a un miembro de la comunidad', async () => {
    localStorage.setItem('userId', '100');
    useAuth.mockReturnValue({
      user: { id: 100, nombre: 'Admin User' },
    });
    communitiesApi.getById.mockResolvedValue({
      ...mockCommunity,
      esMiembro: true,
      miRol: 'ADMIN',
      miembrosActuales: 2,
    });
    communitiesApi.getMembers.mockResolvedValue([
      { id: 10, rol: 'ADMIN', usuario: { id: 100, nombre: 'Admin User' } },
      { id: 12, rol: 'PROFESOR', usuario: { id: 12, nombre: 'Paula Profe' } },
    ]);
    const confirmSpy = jest.spyOn(window, 'confirm').mockReturnValue(true);

    await renderComponent();

    const expelButton = await screen.findByRole('button', { name: /Expulsar/i });
    userEvent.click(expelButton);

    await waitFor(() => {
      expect(communitiesApi.expelMember).toHaveBeenCalledWith('1', 12);
    });
    expect(await screen.findByText(/ha sido expulsado de la comunidad/i)).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.queryByText('Paula Profe')).not.toBeInTheDocument();
    });

    confirmSpy.mockRestore();
  });

  test('no muestra acción de expulsar para usuarios que no son admin', async () => {
    localStorage.setItem('userId', '100');
    useAuth.mockReturnValue({
      user: { id: 100, nombre: 'Teacher User', esTutor: true },
    });
    communitiesApi.getById.mockResolvedValue({
      ...mockCommunity,
      esMiembro: true,
      miRol: 'PROFESOR',
    });
    communitiesApi.getMembers.mockResolvedValue([
      { id: 12, rol: 'PROFESOR', usuario: { id: 12, nombre: 'Paula Profe' } },
      { id: 13, rol: 'ALUMNO', usuario: { id: 13, nombre: 'Alberto Alumno' } },
    ]);

    await renderComponent();
    await screen.findByText('Paula Profe');

    expect(screen.queryByRole('button', { name: /Expulsar/i })).not.toBeInTheDocument();
  });

  test('muestra aviso cuando el usuario es alumno y no puede crear eventos', async () => {
    localStorage.setItem('userId', '100');
    useAuth.mockReturnValue({
      user: { id: 100, nombre: 'Test User', esTutor: false },
    });
    communitiesApi.getById.mockResolvedValue({
      ...mockCommunity,
      esMiembro: true,
      miRol: 'ALUMNO',
    });

    await renderComponent();

    await screen.findByText('Comunidad de Matemáticas');
    expect(screen.queryByRole('button', { name: /Crear evento/i })).not.toBeInTheDocument();
    expect(screen.getByText(/Solo administradores y profesores pueden crear eventos/i)).toBeInTheDocument();
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

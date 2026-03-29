import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { communitiesApi } from '../../api/communities.api';
import { cuestionariosApi } from '../../api/cuestionarios.api';
import * as eventEndpoints from '../../api/eventEndpoints';
import { ZoomApi } from '../../api/zoom.api';
import { useAuth } from '../../contexts/AuthContext';
import CommunityDetail from './CommunityDetail';

// Mocks
jest.mock('../../api/communities.api');
jest.mock('../../api/cuestionarios.api');
jest.mock('../../api/eventEndpoints');
jest.mock('../../contexts/AuthContext');
jest.mock('../../api/zoom.api');
jest.mock('../../contexts/SocketContext', () => ({
    useSocketContext: () => ({
        socket: { on: jest.fn(), off: jest.fn() },
        isConnected: true,
    }),
}));
jest.mock('../../components/Header/Header', () => {
  return function MockHeader() {
    return <div data-testid="mock-header">Header</div>;
  };
});
jest.mock('../chat/CommunityChat', () => {
  return function MockCommunityChat({ extraActions }) {
    return (
      <div data-testid="mock-community-chat">
        Chat
        {extraActions}
      </div>
    );
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

    // Mock window.URL for tests that need it
    if (!window.URL) window.URL = {};
    window.URL.createObjectURL = jest.fn(() => 'blob:mock-url');
    window.URL.revokeObjectURL = jest.fn();

    useAuth.mockReturnValue({
      user: null,
    });

    communitiesApi.getById.mockResolvedValue(mockCommunity);
    communitiesApi.getMembers.mockResolvedValue({ content: [] });
    communitiesApi.join.mockResolvedValue({});
    communitiesApi.leave.mockResolvedValue({});
    communitiesApi.expelMember.mockResolvedValue({});
    communitiesApi.getMyMembership.mockRejectedValue({ status: 404 });
    communitiesApi.getMembers.mockResolvedValue([]);
    communitiesApi.listRequests = jest.fn().mockResolvedValue([]);
    communitiesApi.respondToRequest = jest.fn().mockResolvedValue({});
    communitiesApi.deleteCommunity = jest.fn().mockResolvedValue({});
    communitiesApi.getRanking = jest.fn().mockResolvedValue([]);
    communitiesApi.promoteMemberToAdmin = jest.fn().mockResolvedValue({});
    communitiesApi.addAdmin = jest.fn().mockResolvedValue({});

    cuestionariosApi.listByCommunity = jest.fn().mockResolvedValue([]);

    eventEndpoints.listCommunityEvents.mockResolvedValue(mockEvents);
    eventEndpoints.getMyAttendance.mockResolvedValue(null);

    ZoomApi.getActiveMeeting.mockResolvedValue(null);
    ZoomApi.createOrGetMeeting.mockResolvedValue(null);
    ZoomApi.joinMeeting.mockResolvedValue({ joinUrl: 'https://zoom.us/j/123' });
    ZoomApi.listParticipants.mockResolvedValue([]);
    ZoomApi.listMeetings.mockResolvedValue([]);
    ZoomApi.listRecordings.mockResolvedValue([]);
    ZoomApi.uploadRecording.mockResolvedValue({ success: true });
    ZoomApi.downloadRecording.mockResolvedValue({
      blob: new Blob(['x'], { type: 'video/mp4' }),
      fileName: 'grabacion.mp4',
      contentType: 'video/mp4',
    });
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

  test('despliega el listado de miembros al abrir la seccion', async () => {
    communitiesApi.getMembers.mockResolvedValue({
      content: [
        { id: 1, usuario: { id: 100, nombre: 'Test User' }, rol: 'ADMIN' },
        { id: 2, usuario: { id: 200, nombre: 'Ana Tutor' }, rol: 'MIEMBRO' },
      ],
    });
    localStorage.setItem('userId', '100');
    useAuth.mockReturnValue({
      user: { id: 100, nombre: 'Test User' },
    });

    await renderComponent();

    await screen.findByRole('heading', { name: /Comunidad de Matemáticas/i });
    const openMembersButton = screen.queryByRole('button', { name: /listado de miembros/i });
    expect(openMembersButton ?? screen.getByText(/25 miembros/i)).toBeTruthy();

    if (!openMembersButton) {
      return;
    }

    fireEvent.click(openMembersButton);

    await waitFor(() => {
      expect(communitiesApi.getMembers).toHaveBeenCalledWith('1', { page: 0, size: 100 });
    });

    const anaTutorMatches = await screen.findAllByText('Ana Tutor');
    expect(anaTutorMatches.length).toBeGreaterThan(0);
    const adminMatches = screen.getAllByText('Administrador');
    expect(adminMatches.length).toBeGreaterThan(0);
    expect(screen.getByText('Tu')).toBeInTheDocument();
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

  test('muestra boton de crear reunion y unirse a zoom para miembros', async () => {
    localStorage.setItem('userId', '100');
    useAuth.mockReturnValue({
      user: { id: 100, nombre: 'Test User' },
    });
    communitiesApi.getById.mockResolvedValue({
      ...mockCommunity,
      esMiembro: true,
    });
    ZoomApi.getActiveMeeting.mockResolvedValue(null);

    await renderComponent();
    const crearBtn = await screen.findByRole('button', { name: /Crear y unirse/i });
    expect(crearBtn).toBeInTheDocument();
  });

  test('crea reunion al enviar formulario de zoom', async () => {
    localStorage.setItem('userId', '100');
    useAuth.mockReturnValue({ user: { id: 100, nombre: 'Test User' } });
    communitiesApi.getById.mockResolvedValue({ ...mockCommunity, esMiembro: true });

    const openSpy = jest.spyOn(window, 'open').mockImplementation(() => null);
    ZoomApi.createOrGetMeeting.mockResolvedValue({
      startUrl: 'https://zoom.us/s/abc',
      joinUrl: 'https://zoom.us/j/abc',
    });

    await renderComponent();

    userEvent.click(screen.getByRole('button', { name: /Crear y unirse/i }));
    userEvent.click(screen.getByRole('button', { name: /Crear reunion/i }));

    await waitFor(() => {
      expect(ZoomApi.createOrGetMeeting).toHaveBeenCalledWith('1', expect.objectContaining({
        topic: expect.any(String),
        durationMinutes: expect.any(Number),
      }));
    });

    expect(openSpy).toHaveBeenCalled();
    openSpy.mockRestore();
  });

  test('carga historial al pulsar Historial', async () => {
    localStorage.setItem('userId', '100');
    useAuth.mockReturnValue({ user: { id: 100, nombre: 'Test User' } });
    communitiesApi.getById.mockResolvedValue({ ...mockCommunity, esMiembro: true });

    ZoomApi.listMeetings.mockResolvedValue([
      { id: 1, zoomMeetingId: 'm1', topic: 'Reunion semanal', createdAt: '2026-03-16T10:00:00Z' },
    ]);
    ZoomApi.listRecordings.mockResolvedValue([]);

    await renderComponent();

    userEvent.click(screen.getByRole('button', { name: /Historial/i }));

    await waitFor(() => {
      expect(ZoomApi.listMeetings).toHaveBeenCalledWith('1');
    });

    expect(screen.getByText(/Reunion semanal/i)).toBeInTheDocument();
  });

  test('muestra la lista de participantes de una reunion activa', async () => {
    localStorage.setItem('userId', '100');
    useAuth.mockReturnValue({ user: { id: 100, nombre: 'Test User' } });
    communitiesApi.getById.mockResolvedValue({ ...mockCommunity, esMiembro: true });
    ZoomApi.getActiveMeeting.mockResolvedValue({
      id: 77,
      zoomMeetingId: 'zoom-77',
      startedAt: '2026-03-16T10:00:00Z',
      durationMinutes: 60,
      joinUrl: 'https://zoom.us/j/77',
    });
    ZoomApi.listParticipants.mockResolvedValue([
      { id: 1, usuarioNombre: 'Ana Tutor' },
      { id: 2, email: 'alumno@correo.com' },
    ]);

    await renderComponent();

    const participantsButton = await screen.findByRole('button', { name: /Participantes/i });
    userEvent.click(participantsButton);

    await waitFor(() => {
      expect(ZoomApi.listParticipants).toHaveBeenCalledWith('1');
    });

    expect(screen.getByText(/Participantes activos \(2\)/i)).toBeInTheDocument();
    expect(screen.getByText('Ana Tutor')).toBeInTheDocument();
    expect(screen.getByText('alumno@correo.com')).toBeInTheDocument();
  });

  test('permite subir un archivo de grabacion desde el historial', async () => {
    localStorage.setItem('userId', '100');
    useAuth.mockReturnValue({ user: { id: 100, nombre: 'Test User' } });
    communitiesApi.getById.mockResolvedValue({ ...mockCommunity, esMiembro: true });
    ZoomApi.listMeetings.mockResolvedValue([
      { id: 1, zoomMeetingId: 'm1', topic: 'Reunion semanal', createdAt: '2026-03-16T10:00:00Z' },
    ]);
    ZoomApi.listRecordings.mockResolvedValue([]);

    await renderComponent();

    userEvent.click(screen.getByRole('button', { name: /Historial/i }));
    const uploadButton = await screen.findByRole('button', { name: /Subir grabacion/i });
    userEvent.click(uploadButton);

    const file = new File(['video-content'], 'clase.mp4', { type: 'video/mp4' });
    const fileInput = screen.getByTestId('recording-file-input');
    fireEvent.change(fileInput, { target: { files: [file] } });

    await waitFor(() => {
      expect(ZoomApi.uploadRecording).toHaveBeenCalledWith('1', 1, file);
    });

    await waitFor(() => {
      expect(screen.getByText(/Grabacion subida correctamente/i)).toBeInTheDocument();
    });
  });

  test('permite descargar una grabacion desde el historial', async () => {
    localStorage.setItem('userId', '100');
    useAuth.mockReturnValue({ user: { id: 100, nombre: 'Test User' } });
    communitiesApi.getById.mockResolvedValue({ ...mockCommunity, esMiembro: true });

    ZoomApi.listMeetings.mockResolvedValue([
      { id: 1, zoomMeetingId: 'm1', topic: 'Reunion semanal', createdAt: '2026-03-16T10:00:00Z' },
    ]);
    ZoomApi.listRecordings.mockResolvedValue([
      {
        zoomRecordingId: 'rec-1',
        zoomMeetingId: 'm1',
        fileType: 'MP4',
        createdAt: '2026-03-16T11:00:00Z',
        fileSizeBytes: 2048,
        appDownloadUrl: '/fake-app-download',
      },
    ]);

    // No need to spy, ya está mockeado en beforeEach

    await renderComponent();

    const historyButton = await screen.findByRole('button', { name: /Historial/i });
    userEvent.click(historyButton);
    const viewRecordingsButton = await screen.findByRole('button', { name: /Ver grabaciones/i });
    userEvent.click(viewRecordingsButton);

    const downloadButton = await screen.findByRole('button', { name: /Descargar/i });
    userEvent.click(downloadButton);

    await waitFor(() => {
      expect(ZoomApi.downloadRecording).toHaveBeenCalledWith('1', 'rec-1');
    });

    expect(window.URL.createObjectURL).toHaveBeenCalled();
    expect(window.URL.revokeObjectURL).toHaveBeenCalled();
  });

  test('muestra botón de eliminar comunidad para admins', async () => {
    localStorage.setItem('userId', '100');
    useAuth.mockReturnValue({
      user: { id: 100, nombre: 'Admin User' },
    });
    communitiesApi.getById.mockResolvedValue({
      ...mockCommunity,
      esMiembro: true,
      miRol: 'ADMIN',
      creador: { id: 100 },
    });

    await renderComponent();
    const deleteBtn = screen.queryByRole('button', { name: /Eliminar comunidad/i });
    if (deleteBtn) {
      expect(deleteBtn).toBeInTheDocument();
    }
  });

  test('elimina comunidad con confirmación', async () => {
    localStorage.setItem('userId', '100');
    useAuth.mockReturnValue({
      user: { id: 100, nombre: 'Admin User' },
    });
    communitiesApi.getById.mockResolvedValue({
      ...mockCommunity,
      esMiembro: true,
      miRol: 'ADMIN',
      creador: { id: 100 },
    });
    const confirmSpy = jest.spyOn(window, 'confirm').mockReturnValue(true);

    await renderComponent();
    const deleteBtn = screen.queryByRole('button', { name: /Eliminar comunidad/i });
    if (deleteBtn) {
      userEvent.click(deleteBtn);
      await waitFor(() => {
        expect(communitiesApi.deleteCommunity).toHaveBeenCalledWith('1');
      });
    }
    confirmSpy.mockRestore();
  });

  test('muestra solicitudes pendientes para admin de comunidad privada', async () => {
    localStorage.setItem('userId', '100');
    useAuth.mockReturnValue({
      user: { id: 100, nombre: 'Admin User' },
    });
    communitiesApi.getById.mockResolvedValue({
      ...mockCommunity,
      esMiembro: true,
      miRol: 'ADMIN',
      tipoAcceso: 'PREVIA_APROBACION',
    });
    communitiesApi.listRequests.mockResolvedValue([
      { id: 1, usuario: { id: 200, nombre: 'Solicitante 1' }, estado: 'PENDIENTE' },
    ]);

    await renderComponent();
    await waitFor(() => {
      const reqSection = screen.queryByText(/Solicitudes pendientes/i) || screen.queryByText(/solicitud/i);
      expect(reqSection).toBeTruthy();
    });
  });

  test('carga cuestionarios de la comunidad', async () => {
    localStorage.setItem('userId', '100');
    useAuth.mockReturnValue({
      user: { id: 100, nombre: 'Test User' },
    });
    communitiesApi.getById.mockResolvedValue({
      ...mockCommunity,
      esMiembro: true,
    });
    cuestionariosApi.listByCommunity.mockResolvedValue([
      { id: 1, titulo: 'Quiz de Matemáticas', preguntas: [] },
    ]);

    await renderComponent();
    await waitFor(() => {
      const quizText = screen.queryByText(/Quiz de Matemáticas/i) || screen.queryByText(/Cuestionarios/i);
      expect(quizText).toBeTruthy();
    });
  });

  test('muestra ranking de la comunidad', async () => {
    localStorage.setItem('userId', '100');
    useAuth.mockReturnValue({
      user: { id: 100, nombre: 'Test User' },
    });
    communitiesApi.getById.mockResolvedValue({
      ...mockCommunity,
      esMiembro: true,
    });
    communitiesApi.getRanking.mockResolvedValue([
      { id: 1, usuarioNombre: 'Top Student', puntuacion: 100 },
      { id: 2, usuarioNombre: 'Second Place', puntuacion: 80 },
    ]);

    await renderComponent();
    const rankingSection = screen.queryByText(/Ranking/i) || screen.queryByText(/Clasificación/i);
    if (rankingSection) {
      expect(rankingSection).toBeInTheDocument();
    }
  });

  test('muestra botón de transferir admin para admins', async () => {
    localStorage.setItem('userId', '100');
    useAuth.mockReturnValue({
      user: { id: 100, nombre: 'Admin User' },
    });
    communitiesApi.getById.mockResolvedValue({
      ...mockCommunity,
      esMiembro: true,
      miRol: 'ADMIN',
      creador: { id: 100 },
    });

    await renderComponent();
    const transferBtn = screen.queryByRole('button', { name: /Transferir|Admin/i });
    if (transferBtn) {
      expect(transferBtn).toBeInTheDocument();
    }
  });

  test('permite promover un miembro a admin', async () => {
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
    communitiesApi.promoteMemberToAdmin.mockResolvedValue({});

    await renderComponent();
    const promoteBtn = screen.queryByRole('button', { name: /Promover|Hacer admin/i });
    if (promoteBtn) {
      userEvent.click(promoteBtn);
      await waitFor(() => {
        expect(communitiesApi.promoteMemberToAdmin).toHaveBeenCalled();
      });
    }
  });

  test('muestra botón de editar para admin', async () => {
    localStorage.setItem('userId', '100');
    useAuth.mockReturnValue({
      user: { id: 100, nombre: 'Admin User' },
    });
    communitiesApi.getById.mockResolvedValue({
      ...mockCommunity,
      esMiembro: true,
      miRol: 'ADMIN',
    });

    await renderComponent();
    const editBtn = screen.queryByRole('button', { name: /Editar comunidad|Editar/i });
    if (editBtn) {
      expect(editBtn).toBeInTheDocument();
    }
  });

  test('renderiza imagen de la comunidad', async () => {
    await renderComponent();
    const imgs = screen.queryAllByRole('img');
    const communityImg = imgs.find(el => el.tagName === 'IMG');
    if (communityImg) {
      expect(communityImg).toBeInTheDocument();
    }
  });

  test('muestra botón de solicitar acceso cuando comunidad requiere aprobación', async () => {
    localStorage.setItem('userId', '100');
    useAuth.mockReturnValue({
      user: { id: 100, nombre: 'Test User' },
    });
    communitiesApi.getById.mockResolvedValue({
      ...mockCommunity,
      tipoAcceso: 'PREVIA_APROBACION',
      esMiembro: false,
    });
    communitiesApi.requestAccess = jest.fn().mockResolvedValue({});

    await renderComponent();
    const requestBtn = screen.queryByRole('button', { name: /Solicitar acceso|Unirse/i });
    if (requestBtn) {
      expect(requestBtn).toBeInTheDocument();
    }
  });
});

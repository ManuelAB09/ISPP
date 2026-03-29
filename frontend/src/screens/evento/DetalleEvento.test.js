import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import axiosInstance from '../../api/axiosConfig';
import { communitiesApi } from '../../api/communities.api';
import {
  attendEvent,
  cancelAttendance,
  cancelEvent,
  getConfirmedAttendees,
  getEventById,
  getMyAttendance,
  linkClassroomTask,
  unlinkClassroomTask,
} from '../../api/eventEndpoints';
import { checkAlreadyRated } from '../../api/valoraciones.api';
import { ZoomApi } from '../../api/zoom.api';
import DetalleEvento from './DetalleEvento';

// Mocks
jest.mock('../../api/eventEndpoints');
jest.mock('../../api/valoraciones.api', () => ({
  checkAlreadyRated: jest.fn(),
}));
jest.mock('../../api/axiosConfig', () => ({
  __esModule: true,
  default: {
    get: jest.fn().mockResolvedValue({ data: [] }),
    post: jest.fn().mockResolvedValue({ data: {} }),
    delete: jest.fn().mockResolvedValue({ data: {} }),
    put: jest.fn().mockResolvedValue({ data: {} }),
  }
}));
jest.mock('../../api/zoom.api', () => ({
  ZoomApi: {
    getEventRecordings: jest.fn().mockResolvedValue([]),
    uploadRecordingToClassroom: jest.fn().mockResolvedValue({}),
    getActiveEventMeeting: jest.fn().mockResolvedValue(null),
    joinEventMeeting: jest.fn().mockResolvedValue({ joinUrl: 'https://zoom.us/j/123' }),
    createOrGetEventMeeting: jest.fn().mockResolvedValue({ startUrl: 'https://zoom.us/s/123', joinUrl: 'https://zoom.us/j/123' }),
    endEventMeeting: jest.fn().mockResolvedValue({}),
    listEventParticipants: jest.fn().mockResolvedValue([]),
    listRecordings: jest.fn().mockResolvedValue([]),
    downloadRecording: jest.fn().mockResolvedValue({ blob: new Blob(), fileName: 'rec.mp4' }),
  }
}));
jest.mock('../../api/communities.api', () => ({
  communitiesApi: {
    getMyMembership: jest.fn(),
  },
}));
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
jest.mock('../../components/RatingForm', () => {
  return function MockRatingForm({ onValorado }) {
    return <div data-testid="mock-rating-form"><button onClick={onValorado}>Rate</button></div>;
  };
});

// Mock react-leaflet para evitar dependencias de DOM/canvas en tests
jest.mock('react-leaflet', () => ({
  MapContainer: ({ children }) => <div data-testid="mock-map">{children}</div>,
  TileLayer: () => <div data-testid="mock-tile-layer" />,
  Marker: ({ children }) => <div data-testid="mock-marker">{children}</div>,
  Popup: ({ children }) => <div data-testid="mock-popup">{children}</div>,
}));

describe('DetalleEvento', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    Storage.prototype.getItem = jest.fn((key) => {
      if (key === 'userId') return '1';
      return null;
    });

    communitiesApi.getMyMembership.mockResolvedValue({});
    axiosInstance.get.mockResolvedValue({ data: [] });
    axiosInstance.post.mockResolvedValue({ data: {} });
    axiosInstance.delete.mockResolvedValue({});
    checkAlreadyRated.mockResolvedValue({ rated: false });
    getConfirmedAttendees.mockResolvedValue([
      { id: 201, usuario: { id: 20, nombre: 'Ana' } },
      { id: 202, usuario: { id: 21, nombre: 'Luis' } },
    ]);
    getMyAttendance.mockResolvedValue(null);

    getEventById.mockResolvedValue({
      id: 77,
      titulo: 'Evento React',
      descripcion: 'Descripción del evento',
      fechaHora: '2026-08-10T18:00:00',
      fechaFin: '2026-08-10T20:00:00',
      esVirtual: true,
      enlaceVirtual: 'https://meet.example.com/room',
      aforo: 50,
      asistentesConfirmados: 10,
      privado: false,
      visibleMapa: true,
      cancelado: false,
      comunidadId: 10,
      creador: { id: 2, nombre: 'Organizador' },
      queLlevar: 'Portátil,Cuaderno',
    });

    attendEvent.mockResolvedValue({});
    cancelAttendance.mockResolvedValue({});
    cancelEvent.mockResolvedValue({});
    linkClassroomTask.mockResolvedValue({});
    unlinkClassroomTask.mockResolvedValue({});
    ZoomApi.getActiveEventMeeting.mockResolvedValue(null);
    ZoomApi.joinEventMeeting.mockResolvedValue({ joinUrl: 'https://zoom.us/j/123' });
    ZoomApi.createOrGetEventMeeting.mockResolvedValue({ startUrl: 'https://zoom.us/s/123', joinUrl: 'https://zoom.us/j/123' });
    ZoomApi.endEventMeeting.mockResolvedValue({});
    ZoomApi.listEventParticipants.mockResolvedValue([]);
    ZoomApi.listRecordings.mockResolvedValue([]);
  });

  const renderComponent = () =>
    render(
      <MemoryRouter initialEntries={['/eventos/77']}>
        <Routes>
          <Route path="/eventos/:eventId" element={<DetalleEvento />} />
        </Routes>
      </MemoryRouter>
    );

  test('renderiza loading y luego información del evento', async () => {
    renderComponent();

    expect(screen.getByText(/Cargando evento.../i)).toBeInTheDocument();

    expect(await screen.findByText('Evento React')).toBeInTheDocument();
    expect(screen.getByTestId('mock-header')).toBeInTheDocument();
    expect(screen.getByText(/Descripción del evento/i)).toBeInTheDocument();
  });

  test('muestra botón de confirmar asistencia para usuario no apuntado', async () => {
    renderComponent();

    expect(await screen.findByRole('button', { name: /Confirmar asistencia/i })).toBeInTheDocument();
  });

  test('deshabilita confirmar asistencia si no es miembro de la comunidad', async () => {
    communitiesApi.getMyMembership.mockResolvedValueOnce(null);

    renderComponent();

    const btn = await screen.findByRole('button', { name: /Confirmar asistencia/i });
    expect(btn).toBeDisabled();
  });

  test('confirma asistencia al pulsar botón', async () => {
    renderComponent();

    const btn = await screen.findByRole('button', { name: /Confirmar asistencia/i });
    await waitFor(() => {
      expect(btn).toBeEnabled();
    });
    fireEvent.click(btn);

    // El botón abre un modal; hay que confirmar en él
    await screen.findByText(/¿Quieres recibir alarmas para recordarte este evento\?/i);
    const confirmButtons = await screen.findAllByRole('button', { name: /Confirmar asistencia/i });
    const modalConfirmBtn = confirmButtons[confirmButtons.length - 1];
    fireEvent.click(modalConfirmBtn);

    await waitFor(() => {
      expect(attendEvent).toHaveBeenCalledWith('77');
    });
  });

  test('si ya está confirmado, muestra estado y permite cancelar asistencia', async () => {
    getMyAttendance.mockResolvedValueOnce({ estado: 'CONFIRMADA' });

    renderComponent();

    expect(await screen.findByText(/Asistencia confirmada/i)).toBeInTheDocument();

    const cancelBtn = screen.getByRole('button', { name: /Cancelar asistencia/i });
    fireEvent.click(cancelBtn);

    await waitFor(() => {
      expect(cancelAttendance).toHaveBeenCalledWith('77');
    });
  });

  test('si es profesor de la comunidad, muestra acciones de editar/cancelar evento aunque no sea el creador', async () => {
    communitiesApi.getMyMembership.mockResolvedValueOnce({ rol: 'PROFESOR' });
    getEventById.mockResolvedValueOnce({
      id: 77,
      titulo: 'Evento React',
      fechaHora: '2026-08-10T18:00:00',
      esVirtual: true,
      aforo: 50,
      asistentesConfirmados: 10,
      cancelado: false,
      comunidadId: 10,
      creador: { id: 2, nombre: 'Organizador' },
    });

    renderComponent();

    expect(await screen.findByRole('button', { name: /Editar evento/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Cancelar evento/i })).toBeInTheDocument();
  });

  test('si es alumno aunque sea creador, no muestra editar en evento comunitario', async () => {
    communitiesApi.getMyMembership.mockResolvedValueOnce({ rol: 'ALUMNO' });
    getEventById.mockResolvedValueOnce({
      id: 77,
      titulo: 'Evento React',
      fechaHora: '2026-08-10T18:00:00',
      esVirtual: true,
      aforo: 50,
      asistentesConfirmados: 10,
      cancelado: false,
      comunidadId: 10,
      creador: { id: 1, nombre: 'Yo' },
    });

    renderComponent();

    expect(await screen.findByText('Evento React')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Editar evento/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Cancelar evento/i })).not.toBeInTheDocument();
  });

  test('abre modal y cancela evento', async () => {
    communitiesApi.getMyMembership.mockResolvedValueOnce({ rol: 'PROFESOR' });
    getEventById.mockResolvedValueOnce({
      id: 77,
      titulo: 'Evento React',
      fechaHora: '2026-08-10T18:00:00',
      esVirtual: true,
      aforo: 50,
      asistentesConfirmados: 10,
      cancelado: false,
      comunidadId: 10,
      creador: { id: 1, nombre: 'Yo' },
    });

    renderComponent();

    fireEvent.click(await screen.findByRole('button', { name: /Cancelar evento/i }));

    expect(screen.getByText(/¿Cancelar evento\?/i)).toBeInTheDocument();

    fireEvent.change(screen.getByPlaceholderText(/Explica por qué se cancela el evento.../i), {
      target: { value: 'Problema logístico' },
    });

    fireEvent.click(screen.getByRole('button', { name: /Confirmar cancelación/i }));

    await waitFor(() => {
      expect(cancelEvent).toHaveBeenCalledWith('77', 'Problema logístico');
    });
  });

  test('muestra mensaje de error si falla carga inicial', async () => {
    getEventById.mockRejectedValueOnce(new Error('boom'));

    renderComponent();

    expect(await screen.findByText(/No se pudo cargar el evento/i)).toBeInTheDocument();
  });

  // --- Cancelled event ---
  test('muestra badge cancelado y motivo de cancelación', async () => {
    getEventById.mockResolvedValueOnce({
      id: 77, titulo: 'Evento React', fechaHora: '2026-08-10T18:00:00',
      esVirtual: true, aforo: 50, asistentesConfirmados: 10,
      cancelado: true, motivoCancelacion: 'Lluvia torrencial',
      comunidadId: 10, creador: { id: 2 },
    });
    renderComponent();
    expect(await screen.findByText(/Cancelado/)).toBeInTheDocument();
    expect(screen.getByText('Lluvia torrencial')).toBeInTheDocument();
  });

  // --- Presencial event with location ---
  test('muestra ubicación y mapa para evento presencial', async () => {
    getEventById.mockResolvedValueOnce({
      id: 77, titulo: 'Evento Presencial', fechaHora: '2026-08-10T18:00:00',
      esVirtual: false, aforo: 30, asistentesConfirmados: 5,
      cancelado: false, comunidadId: 10, creador: { id: 2 },
      ubicacion: { nombre: 'Aula 101', direccion: 'Calle Mayor 1', latitud: 37.38, longitud: -5.99 },
    });
    renderComponent();
    expect(await screen.findByText('Aula 101')).toBeInTheDocument();
    expect(screen.getByText('Calle Mayor 1')).toBeInTheDocument();
    expect(screen.getByTestId('mock-map')).toBeInTheDocument();
  });

  test('muestra "Por confirmar" cuando evento presencial sin ubicación', async () => {
    getEventById.mockResolvedValueOnce({
      id: 77, titulo: 'Evento Sin Ubic', fechaHora: '2026-08-10T18:00:00',
      esVirtual: false, aforo: 30, asistentesConfirmados: 5,
      cancelado: false, comunidadId: 10, creador: { id: 2 },
    });
    renderComponent();
    expect(await screen.findByText('Por confirmar')).toBeInTheDocument();
  });

  // --- Materials ---
  test('muestra materiales necesarios', async () => {
    renderComponent();
    expect(await screen.findByText('Portátil')).toBeInTheDocument();
    expect(screen.getByText('Cuaderno')).toBeInTheDocument();
  });

  // --- Alarms management ---
  test('muestra alarmas y permite añadir alarma cuando confirmado', async () => {
    getMyAttendance.mockResolvedValueOnce({ estado: 'CONFIRMADA' });
    axiosInstance.get.mockResolvedValue({ data: [{ id: 1, minutosAntes: 60, canal: 'PLATAFORMA', disparada: false }] });
    axiosInstance.post.mockResolvedValue({ data: { id: 2, minutosAntes: 1440, canal: 'AMBOS', disparada: false } });

    renderComponent();
    expect(await screen.findByText(/Mis alarmas/i)).toBeInTheDocument();
    expect(screen.getByText(/en 1 hora/)).toBeInTheDocument();

    // Add alarm
    fireEvent.click(screen.getByText(/\+ Añadir/));
    await waitFor(() => {
      expect(axiosInstance.post).toHaveBeenCalledWith('/api/v1/events/77/alarms', expect.any(Object));
    });
  });

  test('permite eliminar alarma', async () => {
    getMyAttendance.mockResolvedValueOnce({ estado: 'CONFIRMADA' });
    axiosInstance.get.mockResolvedValue({ data: [{ id: 5, minutosAntes: 30, canal: 'EMAIL', disparada: false }] });
    axiosInstance.delete.mockResolvedValue({});

    renderComponent();
    expect(await screen.findByText(/en 30 minutos/)).toBeInTheDocument();

    const deleteBtn = screen.getByTitle('Eliminar alarma');
    fireEvent.click(deleteBtn);
    await waitFor(() => {
      expect(axiosInstance.delete).toHaveBeenCalledWith('/api/v1/events/77/alarms/5');
    });
  });

  // --- Cancel attendance deletes alarms ---
  test('cancelar asistencia elimina alarmas', async () => {
    getMyAttendance.mockResolvedValueOnce({ estado: 'CONFIRMADA' });
    axiosInstance.get.mockResolvedValue({ data: [{ id: 1, minutosAntes: 60, canal: 'AMBOS', disparada: false }] });

    renderComponent();
    expect(await screen.findByText(/Asistencia confirmada/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /Cancelar asistencia/i }));
    await waitFor(() => {
      expect(cancelAttendance).toHaveBeenCalledWith('77');
    });
  });

  // --- Full capacity ---
  test('muestra aforo completo si no hay plazas', async () => {
    getEventById.mockResolvedValueOnce({
      id: 77, titulo: 'Evento Full', fechaHora: '2026-08-10T18:00:00',
      esVirtual: true, aforo: 10, asistentesConfirmados: 10,
      cancelado: false, comunidadId: 10, creador: { id: 2 },
    });
    renderComponent();
    expect(await screen.findByText(/Aforo completo/i)).toBeInTheDocument();
  });

  // --- Confirm attendance with alarms selected ---
  test('confirma asistencia con alarmas seleccionadas', async () => {
    attendEvent.mockResolvedValue({});
    axiosInstance.post.mockResolvedValue({ data: {} });

    renderComponent();
    const btn = await screen.findByRole('button', { name: /Confirmar asistencia/i });
    await waitFor(() => expect(btn).toBeEnabled());
    fireEvent.click(btn);

    await screen.findByText(/¿Quieres recibir alarmas para recordarte este evento\?/i);

    // Select an alarm checkbox (e.g., "1 día antes")
    const checkboxes = screen.getAllByRole('checkbox');
    fireEvent.click(checkboxes[1]); // "1 día antes"

    const confirmButtons = screen.getAllByRole('button', { name: /Confirmar con alarmas/i });
    fireEvent.click(confirmButtons[confirmButtons.length - 1]);

    await waitFor(() => {
      expect(attendEvent).toHaveBeenCalledWith('77');
      expect(axiosInstance.post).toHaveBeenCalledWith('/api/v1/events/77/alarms/batch', expect.objectContaining({
        minutosAntesList: expect.any(Array),
        canal: 'AMBOS',
      }));
    });
  });

  // --- Participants list ---
  test('muestra lista de participantes', async () => {
    renderComponent();
    expect(await screen.findByText('Ana')).toBeInTheDocument();
    expect(screen.getByText('Luis')).toBeInTheDocument();
    expect(screen.getByText(/Participantes \(2\)/)).toBeInTheDocument();
  });

  // --- No participants ---
  test('muestra mensaje sin participantes', async () => {
    getConfirmedAttendees.mockResolvedValueOnce([]);
    renderComponent();
    expect(await screen.findByText(/Aún no hay participantes confirmados/i)).toBeInTheDocument();
  });

  // --- Organizer sees "Eres el organizador" instead of cancel attendance ---
  test('organizador confirmado ve mensaje de organizador sin botón cancelar asistencia', async () => {
    getEventById.mockResolvedValueOnce({
      id: 77, titulo: 'Mi Evento', fechaHora: '2026-08-10T18:00:00',
      esVirtual: true, aforo: 50, asistentesConfirmados: 10,
      cancelado: false, comunidadId: 10, creador: { id: 1, nombre: 'Yo' },
    });
    getMyAttendance.mockResolvedValueOnce({ estado: 'CONFIRMADA' });
    communitiesApi.getMyMembership.mockResolvedValueOnce({ rol: 'PROFESOR' });

    renderComponent();
    expect(await screen.findByText(/Eres el organizador/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Cancelar asistencia/i })).not.toBeInTheDocument();
  });

  // --- Zoom no active meeting for organizer ---
  test('organizador sin reunión activa ve botón Crear y unirse', async () => {
    getEventById.mockResolvedValueOnce({
      id: 77, titulo: 'Evento Virtual', fechaHora: '2026-08-10T18:00:00',
      esVirtual: true, aforo: 50, asistentesConfirmados: 10,
      cancelado: false, comunidadId: 10, creador: { id: 1, nombre: 'Yo' },
    });
    communitiesApi.getMyMembership.mockResolvedValue({ rol: 'PROFESOR' });
    ZoomApi.getActiveEventMeeting.mockResolvedValue(null);

    renderComponent();
    expect(await screen.findByText(/Crear y unirse/i)).toBeInTheDocument();
  });

  // --- Zoom active meeting ---
  test('muestra llamada activa con botones unirse y participantes', async () => {
    ZoomApi.getActiveEventMeeting.mockResolvedValue({
      id: 100, topic: 'Reunión test', startedAt: new Date().toISOString(),
      startUrl: 'https://zoom.us/s/100', joinUrl: 'https://zoom.us/j/100',
      durationMinutes: 60,
    });

    renderComponent();
    expect(await screen.findByText(/Llamada activa/)).toBeInTheDocument();
    expect(screen.getByText('Reunión test')).toBeInTheDocument();
    expect(screen.getByText(/Unirse a la llamada/)).toBeInTheDocument();
    // Zoom "Participantes" button exists (sidebar also has "Participantes")
    expect(screen.getAllByText(/Participantes/).length).toBeGreaterThanOrEqual(2);
  });

  // --- Toggle participants ---
  test('clic en participantes muestra/oculta lista', async () => {
    ZoomApi.getActiveEventMeeting.mockResolvedValue({
      id: 100, topic: 'Reunión', startedAt: new Date().toISOString(),
      joinUrl: 'https://zoom.us/j/100',
    });
    ZoomApi.listEventParticipants.mockResolvedValue([
      { usuarioId: 1, displayName: 'Carlos' },
    ]);

    renderComponent();
    // Wait for the active meeting to load, showing the Zoom buttons
    await screen.findByText(/Unirse a la llamada/);

    // Find the Zoom participants button (not the sidebar heading)
    const zoomBtns = screen.getAllByText(/Participantes/);
    // The zoom button is inside a <button> element
    const zoomParticipantsBtn = zoomBtns.find(el => el.closest('button'));
    fireEvent.click(zoomParticipantsBtn);

    expect(await screen.findByText('Carlos')).toBeInTheDocument();

    // Click again to hide (button now says "Ocultar")
    fireEvent.click(screen.getByText(/Ocultar/));
    await waitFor(() => {
      expect(screen.queryByText('Carlos')).not.toBeInTheDocument();
    });
  });

  // --- Zoom End meeting (organizer) ---
  test('organizador puede finalizar reunión', async () => {
    getEventById.mockResolvedValueOnce({
      id: 77, titulo: 'Evento Virtual', fechaHora: '2026-08-10T18:00:00',
      esVirtual: true, aforo: 50, asistentesConfirmados: 10,
      cancelado: false, comunidadId: 10, creador: { id: 1 },
    });
    communitiesApi.getMyMembership.mockResolvedValue({ rol: 'PROFESOR' });
    ZoomApi.getActiveEventMeeting.mockResolvedValue({
      id: 100, topic: 'Reunión', startedAt: new Date().toISOString(),
      startUrl: 'https://zoom.us/s/100', joinUrl: 'https://zoom.us/j/100',
    });

    renderComponent();
    const endBtn = await screen.findByText(/Finalizar/);
    fireEvent.click(endBtn);

    await waitFor(() => {
      expect(ZoomApi.endEventMeeting).toHaveBeenCalledWith('77');
    });
  });

  // --- Zoom create and join meeting form ---
  test('organizer can open meeting form, fill and create meeting', async () => {
    getEventById.mockResolvedValueOnce({
      id: 77, titulo: 'Evento Virtual', fechaHora: '2026-08-10T18:00:00',
      esVirtual: true, aforo: 50, asistentesConfirmados: 10,
      cancelado: false, comunidadId: 10, creador: { id: 1 },
    });
    communitiesApi.getMyMembership.mockResolvedValue({ rol: 'PROFESOR' });
    ZoomApi.getActiveEventMeeting.mockResolvedValue(null);
    ZoomApi.createOrGetEventMeeting.mockResolvedValue({
      startUrl: 'https://zoom.us/s/999', joinUrl: 'https://zoom.us/j/999',
    });
    const openSpy = jest.spyOn(window, 'open').mockImplementation(() => {});

    renderComponent();
    fireEvent.click(await screen.findByText(/Crear y unirse/i));

    // Form should appear
    expect(screen.getByText(/Crear reunión/)).toBeInTheDocument();

    fireEvent.click(screen.getByText(/Crear reunión/));
    await waitFor(() => {
      expect(ZoomApi.createOrGetEventMeeting).toHaveBeenCalledWith('77', expect.objectContaining({
        topic: expect.any(String),
        durationMinutes: expect.any(Number),
      }));
    });
    openSpy.mockRestore();
  });

  // --- Recordings toggle ---
  test('toggle grabaciones muestra lista', async () => {
    ZoomApi.listRecordings.mockResolvedValue([
      { id: 'r1', topic: 'Grabación 1', startTime: '2025-01-01T10:00:00', duration: 45 },
    ]);

    renderComponent();
    const recBtn = await screen.findByText(/Ver Grabaciones en la Comunidad/i);
    fireEvent.click(recBtn);

    expect(await screen.findByText('Grabación 1')).toBeInTheDocument();
    expect(screen.getByText(/45 min/)).toBeInTheDocument();
  });

  // --- Recordings empty ---
  test('muestra mensaje sin grabaciones', async () => {
    ZoomApi.listRecordings.mockResolvedValue([]);

    renderComponent();
    fireEvent.click(await screen.findByText(/Ver Grabaciones en la Comunidad/i));

    expect(await screen.findByText(/No hay grabaciones disponibles/i)).toBeInTheDocument();
  });

  // --- Non-member sees disabled confirm ---
  test('no miembro ve mensaje de membresía', async () => {
    communitiesApi.getMyMembership.mockResolvedValue(null);

    renderComponent();
    expect(await screen.findByText(/Debes ser miembro de la comunidad para apuntarte/i)).toBeInTheDocument();
  });

  // --- No userId shows login hint ---
  test('sin sesión muestra enlace de iniciar sesión', async () => {
    Storage.prototype.getItem = jest.fn(() => null);

    renderComponent();
    expect(await screen.findByText(/Inicia sesión/i)).toBeInTheDocument();
  });

  // --- Rating form for confirmed attendee of profesor event ---
  test('muestra formulario de valoración para asistente confirmado de evento de profesor', async () => {
    getEventById.mockResolvedValueOnce({
      id: 77, titulo: 'Clase de Física', fechaHora: '2024-01-01T10:00:00', fechaFin: '2024-01-01T12:00:00',
      esVirtual: true, aforo: 50, asistentesConfirmados: 10,
      cancelado: false, comunidadId: 10,
      creador: { id: 2, nombre: 'Prof', tutorId: 42 },
      creadorRolComunidad: 'PROFESOR',
    });
    getMyAttendance.mockResolvedValueOnce({ estado: 'CONFIRMADA' });

    renderComponent();
    expect(await screen.findByTestId('mock-rating-form')).toBeInTheDocument();
  });

  // --- Classroom task linked ---
  test('muestra tarea de Classroom vinculada', async () => {
    getEventById.mockResolvedValueOnce({
      id: 77, titulo: 'Evento React', fechaHora: '2026-08-10T18:00:00',
      esVirtual: true, aforo: 50, asistentesConfirmados: 10,
      cancelado: false, comunidadId: 10, creador: { id: 2 },
      classroomTaskId: 'task1', classroomTaskTitle: 'Tarea React', classroomTaskUrl: 'https://classroom.google.com/c/123',
    });
    renderComponent();
    expect(await screen.findByText('Tarea React')).toBeInTheDocument();
    expect(screen.getByText(/Tarea vinculada a este evento/)).toBeInTheDocument();
  });

  // --- Organizer sees "Vincular Tarea" when no task linked ---
  test('organizer ve Vincular Tarea si no hay tarea vinculada', async () => {
    getEventById.mockResolvedValueOnce({
      id: 77, titulo: 'Evento React', fechaHora: '2026-08-10T18:00:00',
      esVirtual: true, aforo: 50, asistentesConfirmados: 10,
      cancelado: false, comunidadId: 10, creador: { id: 1 },
    });
    communitiesApi.getMyMembership.mockResolvedValue({ rol: 'PROFESOR' });
    axiosInstance.get.mockImplementation((url) => {
      if (url.includes('/oauth2/communities/')) {
        return Promise.resolve({ data: { courseWork: { courseWork: [{ id: 't1', title: 'Homework 1', alternateLink: 'https://classroom.google.com' }] } } });
      }
      return Promise.resolve({ data: [] });
    });

    renderComponent();
    const linkBtn = await screen.findByText(/Vincular Tarea/i);
    fireEvent.click(linkBtn);

    expect(await screen.findByText('Homework 1')).toBeInTheDocument();
  });

  // --- Confirm attendance error ---
  test('muestra error al fallar confirmación de asistencia', async () => {
    attendEvent.mockRejectedValueOnce({ response: { data: { message: 'Fallo servidor' } } });

    renderComponent();
    const btn = await screen.findByRole('button', { name: /Confirmar asistencia/i });
    await waitFor(() => expect(btn).toBeEnabled());
    fireEvent.click(btn);

    await screen.findByText(/¿Quieres recibir alarmas/i);
    const confirmButtons = screen.getAllByRole('button', { name: /Confirmar asistencia/i });
    fireEvent.click(confirmButtons[confirmButtons.length - 1]);

    await waitFor(() => {
      expect(screen.getByText('Fallo servidor')).toBeInTheDocument();
    });
  });

  // --- Organizer with creator photo ---
  test('muestra foto del organizador si tiene', async () => {
    getEventById.mockResolvedValueOnce({
      id: 77, titulo: 'Evento React', fechaHora: '2026-08-10T18:00:00',
      esVirtual: true, aforo: 50, asistentesConfirmados: 10,
      cancelado: false, comunidadId: 10,
      creador: { id: 2, nombre: 'Prof', foto: '/img/prof.jpg' },
    });
    renderComponent();
    expect(await screen.findByAltText('Prof')).toBeInTheDocument();
  });

  // --- Fired alarm doesn't show delete button ---
  test('alarma disparada no muestra botón eliminar', async () => {
    getMyAttendance.mockResolvedValueOnce({ estado: 'CONFIRMADA' });
    axiosInstance.get.mockResolvedValue({
      data: [{ id: 10, minutosAntes: 2880, canal: 'AMBOS', disparada: true }],
    });

    renderComponent();
    expect(await screen.findByText(/en 2 días/)).toBeInTheDocument();
    expect(screen.queryByTitle('Eliminar alarma')).not.toBeInTheDocument();
  });
});
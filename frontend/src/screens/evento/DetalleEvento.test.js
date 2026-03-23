import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import DetalleEvento from './DetalleEvento';
import {
  getEventById,
  cancelEvent,
  attendEvent,
  cancelAttendance,
  getConfirmedAttendees,
  getMyAttendance,
} from '../../api/eventEndpoints';
import { communitiesApi } from '../../api/communities.api';

// Mocks
jest.mock('../../api/eventEndpoints');
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

  test('confirma asistencia al pulsar botón', async () => {
    renderComponent();

    const btn = await screen.findByRole('button', { name: /Confirmar asistencia/i });
    fireEvent.click(btn);

    // El botón abre un modal; hay que confirmar en él
    const modalConfirmBtn = await screen.findAllByRole('button', { name: /Confirmar asistencia/i });
    fireEvent.click(modalConfirmBtn[modalConfirmBtn.length - 1]);

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
});
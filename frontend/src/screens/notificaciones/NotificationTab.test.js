import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

jest.mock('../../api/notificationService', () => ({
  getAllEventAlerts: jest.fn(),
  getAllUserNotifications: jest.fn(),
  markEventAlertAsRead: jest.fn(),
  markUserNotificationAsRead: jest.fn(),
  markAllEventAlertsAsRead: jest.fn(),
  markAllUserNotificationsAsRead: jest.fn(),
}));
jest.mock('../../components/Header/Header', () => () => <div data-testid="header">Header</div>);
jest.mock('../../components/Modal/Modal', () => ({ isOpen, onClose, children }) =>
  isOpen ? <div data-testid="modal">{children}</div> : null
);
jest.mock('../../components/Modal/ModalChanges.css', () => ({}));
let mockNotificationsEnabled = true;
const mockMarkOnePanelNotificationRead = jest.fn();
const mockClearPanelNotificationsUnread = jest.fn();
const mockSetPanelNotificationsUnreadCount = jest.fn();

jest.mock('../../contexts/NotificationContext', () => ({
  useNotificationContext: () => ({
    get notificationsEnabled() { return mockNotificationsEnabled; },
    markOnePanelNotificationRead: mockMarkOnePanelNotificationRead,
    clearPanelNotificationsUnread: mockClearPanelNotificationsUnread,
    setPanelNotificationsUnreadCount: mockSetPanelNotificationsUnreadCount,
  }),
}));
jest.mock('./NotificationTab.css', () => ({}));

const {
  getAllEventAlerts,
  getAllUserNotifications,
  markEventAlertAsRead,
  markUserNotificationAsRead,
  markAllEventAlertsAsRead,
  markAllUserNotificationsAsRead,
} = require('../../api/notificationService');

const NotificationTab = require('./NotificationTab').default;

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

describe('NotificationTab', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockNotificationsEnabled = true;
    delete process.env.REACT_APP_API_BASE_URL;
  });

  test('shows empty state when no notifications', async () => {
    getAllEventAlerts.mockResolvedValue([]);
    getAllUserNotifications.mockResolvedValue([]);
    render(<MemoryRouter><NotificationTab /></MemoryRouter>);
    await screen.findByText('No tienes notificaciones recientes.');
  });

  test('renders event alerts', async () => {
    getAllEventAlerts.mockResolvedValue([
      { id: 1, tipo: 'EVENTO', eventoTitulo: 'Exam Tomorrow', mensaje: 'Starts at 9AM', leida: false, createdAt: '2025-03-01T10:00:00', eventoId: 5, icono: '📝' },
    ]);
    getAllUserNotifications.mockResolvedValue([]);
    render(<MemoryRouter><NotificationTab /></MemoryRouter>);
    await screen.findByText('Exam Tomorrow');
    expect(screen.getByText('Starts at 9AM')).toBeInTheDocument();
  });

  test('renders user notifications with community info', async () => {
    getAllEventAlerts.mockResolvedValue([]);
    getAllUserNotifications.mockResolvedValue([
      {
        id: 2, tipo: 'ANUNCIO', titulo: 'New Announcement', mensaje: 'Check this out',
        leida: false, createdAt: '2025-03-02T10:00:00',
        comunidadId: 10, comunidadNombre: 'Dev Community',
        anuncioId: 20,
      },
    ]);
    render(<MemoryRouter><NotificationTab /></MemoryRouter>);
    await screen.findByText('New Announcement');
    expect(screen.getByText('Check this out')).toBeInTheDocument();
    expect(screen.getByText(/Dev Community/)).toBeInTheDocument();
    expect(screen.getByText('[Anuncio]')).toBeInTheDocument();
  });

  test('marks single notification as read', async () => {
    getAllEventAlerts.mockResolvedValue([
      { id: 1, tipo: 'EVENTO', eventoTitulo: 'Alert', mensaje: 'msg', leida: false, createdAt: '2025-03-01T10:00:00', eventoId: 5 },
    ]);
    getAllUserNotifications.mockResolvedValue([]);
    markEventAlertAsRead.mockResolvedValue({});
    render(<MemoryRouter><NotificationTab /></MemoryRouter>);
    await screen.findByText('Alert');
    fireEvent.click(screen.getByText('Alert').closest('li'));
    await waitFor(() => expect(markEventAlertAsRead).toHaveBeenCalledWith('1'));
  });

  test('marks all as read', async () => {
    getAllEventAlerts.mockResolvedValue([
      { id: 1, tipo: 'EVENTO', eventoTitulo: 'A1', mensaje: 'm', leida: false, createdAt: '2025-01-01T00:00:00', eventoId: 1 },
    ]);
    getAllUserNotifications.mockResolvedValue([]);
    markAllEventAlertsAsRead.mockResolvedValue({});
    markAllUserNotificationsAsRead.mockResolvedValue({});
    render(<MemoryRouter><NotificationTab /></MemoryRouter>);
    await screen.findByText('A1');
    fireEvent.click(screen.getByText('Marcar todo como leído'));
    await waitFor(() => expect(markAllEventAlertsAsRead).toHaveBeenCalled());
    await waitFor(() => expect(markAllUserNotificationsAsRead).toHaveBeenCalled());
  });

  test('navigates to event on alert click', async () => {
    getAllEventAlerts.mockResolvedValue([
      { id: 1, tipo: 'EVENTO', eventoTitulo: 'Click Me', mensaje: 'msg', leida: true, createdAt: '2025-01-01T00:00:00', eventoId: 42 },
    ]);
    getAllUserNotifications.mockResolvedValue([]);
    render(<MemoryRouter><NotificationTab /></MemoryRouter>);
    await screen.findByText('Click Me');
    fireEvent.click(screen.getByText('Click Me').closest('li'));
    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/eventos/42'));
  });

  test('navigates to announcement on notification click', async () => {
    getAllEventAlerts.mockResolvedValue([]);
    getAllUserNotifications.mockResolvedValue([
      { id: 1, tipo: 'ANUNCIO', titulo: 'Ann', mensaje: 'msg', leida: true, createdAt: '2025-01-01T00:00:00', comunidadId: 5, anuncioId: 10 },
    ]);
    markUserNotificationAsRead.mockResolvedValue({});
    render(<MemoryRouter><NotificationTab /></MemoryRouter>);
    await screen.findByText('Ann');
    fireEvent.click(screen.getByText('Ann').closest('li'));
    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/comunidades/5?tab=anuncios&anuncioId=10'));
  });

  test('navigates to solicitudes on SOLICITUD_ACCESO click', async () => {
    getAllEventAlerts.mockResolvedValue([]);
    getAllUserNotifications.mockResolvedValue([
      { id: 1, tipo: 'SOLICITUD_ACCESO', titulo: 'Access', mensaje: 'request', leida: true, createdAt: '2025-01-01T00:00:00', comunidadId: 7 },
    ]);
    render(<MemoryRouter><NotificationTab /></MemoryRouter>);
    await screen.findByText('Access');
    fireEvent.click(screen.getByText('Access').closest('li'));
    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/comunidades/7?tab=solicitudes'));
  });

  test('navigates to community on approved RESPUESTA_SOLICITUD_ACCESO', async () => {
    getAllEventAlerts.mockResolvedValue([]);
    getAllUserNotifications.mockResolvedValue([
      { id: 1, tipo: 'RESPUESTA_SOLICITUD_ACCESO', titulo: 'Response', mensaje: 'Tu solicitud ha sido aprobada', leida: true, createdAt: '2025-01-01T00:00:00', comunidadId: 8 },
    ]);
    render(<MemoryRouter><NotificationTab /></MemoryRouter>);
    await screen.findByText('Response');
    fireEvent.click(screen.getByText('Response').closest('li'));
    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/comunidades/8'));
  });

  test('shows changes modal for EVENTO notification with changes', async () => {
    getAllEventAlerts.mockResolvedValue([]);
    getAllUserNotifications.mockResolvedValue([
      { id: 1, tipo: 'EVENTO', titulo: 'Event Changed', mensaje: 'Evento actualizado. Cambios: Hora cambiada\nLugar cambiado', leida: true, createdAt: '2025-01-01T00:00:00', eventoId: 99, comunidadId: 2 },
    ]);
    render(<MemoryRouter><NotificationTab /></MemoryRouter>);
    await screen.findByText('Event Changed');
    fireEvent.click(screen.getByText('Event Changed').closest('li'));
    await screen.findByTestId('modal');
    expect(screen.getByText('Cambios en el evento')).toBeInTheDocument();
    expect(screen.getByText('Hora cambiada')).toBeInTheDocument();
    expect(screen.getByText('Lugar cambiado')).toBeInTheDocument();
    fireEvent.click(screen.getByText('Ir al evento'));
    expect(mockNavigate).toHaveBeenCalledWith('/eventos/99');
  });

  test('navigates directly for EVENTO without changes', async () => {
    getAllEventAlerts.mockResolvedValue([]);
    getAllUserNotifications.mockResolvedValue([
      { id: 1, tipo: 'EVENTO', titulo: 'Simple Event', mensaje: 'Nuevo evento creado', leida: true, createdAt: '2025-01-01T00:00:00', eventoId: 50 },
    ]);
    render(<MemoryRouter><NotificationTab /></MemoryRouter>);
    await screen.findByText('Simple Event');
    fireEvent.click(screen.getByText('Simple Event').closest('li'));
    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/eventos/50'));
  });

  test('handles API error gracefully', async () => {
    getAllEventAlerts.mockRejectedValue(new Error('fail'));
    getAllUserNotifications.mockRejectedValue(new Error('fail'));
    render(<MemoryRouter><NotificationTab /></MemoryRouter>);
    await screen.findByText('No tienes notificaciones recientes.');
  });

  test('shows [Evento] type badge for event alerts', async () => {
    getAllEventAlerts.mockResolvedValue([
      { id: 1, tipo: 'EVENTO', eventoTitulo: 'E1', mensaje: 'msg', leida: false, createdAt: '2025-01-01T00:00:00', eventoId: 1 },
    ]);
    getAllUserNotifications.mockResolvedValue([]);
    render(<MemoryRouter><NotificationTab /></MemoryRouter>);
    await screen.findByText('[Evento]');
  });

  test('user notification marks as read on click', async () => {
    getAllEventAlerts.mockResolvedValue([]);
    getAllUserNotifications.mockResolvedValue([
      { id: 5, tipo: 'ANUNCIO', titulo: 'Note', mensaje: 'hi', leida: false, createdAt: '2025-01-01T00:00:00', comunidadId: 1, anuncioId: 2 },
    ]);
    markUserNotificationAsRead.mockResolvedValue({});
    render(<MemoryRouter><NotificationTab /></MemoryRouter>);
    await screen.findByText('Note');
    fireEvent.click(screen.getByText('Note').closest('li'));
    await waitFor(() => expect(markUserNotificationAsRead).toHaveBeenCalledWith('5'));
  });

  test('renderChangesList with empty text', async () => {
    getAllEventAlerts.mockResolvedValue([]);
    getAllUserNotifications.mockResolvedValue([
      { id: 1, tipo: 'EVENTO', titulo: 'Ev', mensaje: 'Cambios: ', leida: true, createdAt: '2025-01-01T00:00:00', eventoId: 1 },
    ]);
    render(<MemoryRouter><NotificationTab /></MemoryRouter>);
    await screen.findByText('Ev');
    fireEvent.click(screen.getByText('Ev').closest('li'));
    await screen.findByText('No hay cambios detectados.');
  });
});

describe('NotificationTab disabled', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockNotificationsEnabled = false;
  });

  test('shows disabled warning when notifications are off', async () => {
    render(<MemoryRouter><NotificationTab /></MemoryRouter>);
    expect(screen.getByText(/Las notificaciones están desactivadas/)).toBeInTheDocument();
  });
});

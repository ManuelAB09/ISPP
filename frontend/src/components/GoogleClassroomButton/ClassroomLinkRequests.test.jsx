import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import ClassroomLinkRequests from './ClassroomLinkRequests';

jest.mock('../../api/classroomLinkRequests.api', () => ({
  classroomLinkRequestsApi: {
    myPending: jest.fn(),
    complete: jest.fn(),
  },
}));
jest.mock('../../api/baseUrl', () => ({
  getApiBaseUrl: () => 'http://localhost:8080',
}));

const { classroomLinkRequestsApi } = require('../../api/classroomLinkRequests.api');

describe('ClassroomLinkRequests', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    global.fetch = jest.fn();
    global.window.open = jest.fn();
    localStorage.setItem('accessToken', 'test-token');
  });

  afterEach(() => {
    delete global.fetch;
    localStorage.removeItem('accessToken');
  });

  test('shows loading initially', () => {
    classroomLinkRequestsApi.myPending.mockReturnValue(new Promise(() => {}));
    render(<ClassroomLinkRequests />);
    expect(screen.getByText('Cargando solicitudes...')).toBeInTheDocument();
  });

  test('shows empty state when no pending requests', async () => {
    classroomLinkRequestsApi.myPending.mockResolvedValue({ data: [] });
    render(<ClassroomLinkRequests />);
    await screen.findByText('No tienes solicitudes pendientes.');
  });

  test('renders pending requests', async () => {
    classroomLinkRequestsApi.myPending.mockResolvedValue({
      data: [{ id: 1, comunidadId: 10, estado: 'PENDIENTE' }],
    });
    render(<ClassroomLinkRequests />);
    await screen.findByText('Solicitud #1');
    expect(screen.getByText(/Comunidad: 10/)).toBeInTheDocument();
  });

  test('openAuth fetches URL and opens popup', async () => {
    classroomLinkRequestsApi.myPending.mockResolvedValue({
      data: [{ id: 1, comunidadId: 10, estado: 'PENDIENTE' }],
    });
    global.fetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ url: 'https://google.com/oauth' }),
    });

    render(<ClassroomLinkRequests />);
    await screen.findByText('Autorizar');
    fireEvent.click(screen.getByText('Autorizar'));
    await waitFor(() => expect(global.fetch).toHaveBeenCalled());
    await waitFor(() => expect(window.open).toHaveBeenCalled());
  });

  test('openAuth shows error on failure', async () => {
    classroomLinkRequestsApi.myPending.mockResolvedValue({
      data: [{ id: 1, comunidadId: 10, estado: 'PENDIENTE' }],
    });
    global.fetch.mockResolvedValue({
      ok: false,
      json: () => Promise.resolve({ error: 'OAuth error' }),
    });
    jest.spyOn(console, 'error').mockImplementation();

    render(<ClassroomLinkRequests />);
    await screen.findByText('Autorizar');
    fireEvent.click(screen.getByText('Autorizar'));
    await screen.findByText(/OAuth error/);
    console.error.mockRestore();
  });

  test('handles load error', async () => {
    classroomLinkRequestsApi.myPending.mockRejectedValue(new Error('Network'));
    jest.spyOn(console, 'error').mockImplementation();
    render(<ClassroomLinkRequests />);
    await screen.findByText(/No se pudieron cargar/);
    console.error.mockRestore();
  });

  test('handles postMessage with courses and completes request', async () => {
    classroomLinkRequestsApi.myPending.mockResolvedValue({
      data: [{ id: 5, comunidadId: 10, estado: 'PENDIENTE' }],
    });
    classroomLinkRequestsApi.complete.mockResolvedValue({});

    render(<ClassroomLinkRequests />);
    await screen.findByText('Solicitud #5');

    act(() => {
      window.dispatchEvent(new MessageEvent('message', {
        data: { courses: [{ id: 'c1', name: 'Biology' }], requestId: '5' },
      }));
    });

    await screen.findByText('Biology');
    fireEvent.click(screen.getByText('Vincular'));

    await waitFor(() => expect(classroomLinkRequestsApi.complete).toHaveBeenCalledWith(5, {
      cursoId: 'c1',
      nombreCurso: 'Biology',
    }));
  });

  test('handles postMessage error', async () => {
    classroomLinkRequestsApi.myPending.mockResolvedValue({ data: [] });
    render(<ClassroomLinkRequests />);
    await screen.findByText('No tienes solicitudes pendientes.');

    act(() => {
      window.dispatchEvent(new MessageEvent('message', {
        data: { error: 'auth_failed' },
      }));
    });

    await screen.findByText(/auth_failed/);
  });

  test('handles postMessage with nested courses', async () => {
    classroomLinkRequestsApi.myPending.mockResolvedValue({ data: [] });
    render(<ClassroomLinkRequests />);
    await screen.findByText('No tienes solicitudes pendientes.');

    act(() => {
      window.dispatchEvent(new MessageEvent('message', {
        data: { courses: { courses: [{ id: 'c2', name: 'Chem' }] }, requestId: '2' },
      }));
    });

    await screen.findByText('Chem');
  });

  test('shows no courses message', async () => {
    classroomLinkRequestsApi.myPending.mockResolvedValue({ data: [] });
    render(<ClassroomLinkRequests />);
    await screen.findByText('No tienes solicitudes pendientes.');

    act(() => {
      window.dispatchEvent(new MessageEvent('message', {
        data: { courses: [], requestId: '1' },
      }));
    });

    await screen.findByText('No tienes cursos.');
  });

  test('completeRequest shows error when no activeRequestId', async () => {
    classroomLinkRequestsApi.myPending.mockResolvedValue({ data: [] });
    render(<ClassroomLinkRequests />);
    await screen.findByText('No tienes solicitudes pendientes.');

    // Send courses without requestId
    act(() => {
      window.dispatchEvent(new MessageEvent('message', {
        data: { courses: [{ id: 'c1', name: 'Test' }] },
      }));
    });

    await screen.findByText('Test');
    fireEvent.click(screen.getByText('Vincular'));
    await screen.findByText(/No hay requestId activo/);
  });

  test('completeRequest handles API error', async () => {
    classroomLinkRequestsApi.myPending.mockResolvedValue({
      data: [{ id: 3, comunidadId: 10, estado: 'PENDIENTE' }],
    });
    classroomLinkRequestsApi.complete.mockRejectedValue(new Error('fail'));
    jest.spyOn(console, 'error').mockImplementation();

    render(<ClassroomLinkRequests />);
    await screen.findByText('Solicitud #3');

    act(() => {
      window.dispatchEvent(new MessageEvent('message', {
        data: { courses: [{ id: 'c1', name: 'Art' }], requestId: '3' },
      }));
    });

    await screen.findByText('Art');
    fireEvent.click(screen.getByText('Vincular'));
    await screen.findByText(/Error completando/);
    console.error.mockRestore();
  });

  test('ignores postMessage with no data', async () => {
    classroomLinkRequestsApi.myPending.mockResolvedValue({ data: [] });
    render(<ClassroomLinkRequests />);
    await screen.findByText('No tienes solicitudes pendientes.');

    act(() => {
      window.dispatchEvent(new MessageEvent('message', { data: null }));
    });

    expect(screen.queryByText('Selecciona un curso')).not.toBeInTheDocument();
  });
});

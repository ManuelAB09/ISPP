import { getMisEventos, getMisEventosHistorial, getAlertas, getAlertasCount, marcarAlertaLeida, marcarTodasLeidas } from './myEventsUtils';

jest.mock('../api/axiosConfig', () => ({
  __esModule: true,
  default: {
    get: jest.fn(),
    patch: jest.fn(),
  },
}));

const axiosInstance = require('../api/axiosConfig').default;

describe('myEventsUtils', () => {
  beforeEach(() => jest.clearAllMocks());

  test('getMisEventos calls GET /api/v1/my-events', async () => {
    axiosInstance.get.mockResolvedValue({ data: [{ id: 1 }] });
    const result = await getMisEventos();
    expect(axiosInstance.get).toHaveBeenCalledWith('/api/v1/my-events');
    expect(result).toEqual([{ id: 1 }]);
  });

  test('getMisEventosHistorial calls with params', async () => {
    axiosInstance.get.mockResolvedValue({ data: [] });
    await getMisEventosHistorial(true);
    expect(axiosInstance.get).toHaveBeenCalledWith('/api/v1/my-events/history', { params: { incluirCancelados: true } });
  });

  test('getMisEventosHistorial defaults incluirCancelados to false', async () => {
    axiosInstance.get.mockResolvedValue({ data: [] });
    await getMisEventosHistorial();
    expect(axiosInstance.get).toHaveBeenCalledWith('/api/v1/my-events/history', { params: { incluirCancelados: false } });
  });

  test('getAlertas calls GET /api/v1/my-events/alerts', async () => {
    axiosInstance.get.mockResolvedValue({ data: [{ id: 2 }] });
    const result = await getAlertas();
    expect(axiosInstance.get).toHaveBeenCalledWith('/api/v1/my-events/alerts');
    expect(result).toEqual([{ id: 2 }]);
  });

  test('getAlertasCount calls GET /api/v1/my-events/alerts/count', async () => {
    axiosInstance.get.mockResolvedValue({ data: 5 });
    const result = await getAlertasCount();
    expect(axiosInstance.get).toHaveBeenCalledWith('/api/v1/my-events/alerts/count');
    expect(result).toBe(5);
  });

  test('marcarAlertaLeida calls PATCH with id', async () => {
    axiosInstance.patch.mockResolvedValue({ data: { ok: true } });
    const result = await marcarAlertaLeida(42);
    expect(axiosInstance.patch).toHaveBeenCalledWith('/api/v1/my-events/alerts/42/read');
    expect(result).toEqual({ ok: true });
  });

  test('marcarTodasLeidas calls PATCH read-all', async () => {
    axiosInstance.patch.mockResolvedValue({ data: { ok: true } });
    const result = await marcarTodasLeidas();
    expect(axiosInstance.patch).toHaveBeenCalledWith('/api/v1/my-events/alerts/read-all');
    expect(result).toEqual({ ok: true });
  });
});

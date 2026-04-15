jest.mock('./baseUrl', () => ({
  getApiBaseUrl: jest.fn(() => 'https://api.test')
}));

const { apiClient, ApiError } = require('./client');

describe('apiClient', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();
    apiClient.setToken(null);
    global.fetch = jest.fn();
  });

  test('get sends auth header using latest token from localStorage', async () => {
    localStorage.setItem('accessToken', 'token-local');
    global.fetch.mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ ok: true }),
    });

    const result = await apiClient.get('/ping');

    expect(result).toEqual({ ok: true });
    expect(global.fetch).toHaveBeenCalledWith('https://api.test/ping', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer token-local',
      },
    });
  });

  test('post serializes JSON body and uses instance token when set', async () => {
    apiClient.setToken('token-instance');
    global.fetch.mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ id: 1 }),
    });

    const payload = { name: 'Test' };
    const result = await apiClient.post('/items', payload);

    expect(result).toEqual({ id: 1 });
    expect(global.fetch).toHaveBeenCalledWith('https://api.test/items', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer token-instance',
      },
      body: JSON.stringify(payload),
    });
  });

  test('post with FormData omits JSON content-type', async () => {
    const formData = new FormData();
    formData.append('file', new File(['abc'], 'a.txt', { type: 'text/plain' }));

    global.fetch.mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ uploaded: true }),
    });

    await apiClient.post('/upload', formData);

    expect(global.fetch).toHaveBeenCalledWith(
      'https://api.test/upload',
      expect.objectContaining({
        method: 'POST',
        body: formData,
      })
    );

    const [, options] = global.fetch.mock.calls[0];
    expect(options.headers['Content-Type']).toBeUndefined();
  });

  test('delete returns null on 204 responses', async () => {
    global.fetch.mockResolvedValue({
      ok: true,
      status: 204,
      json: async () => ({ ignored: true }),
    });

    const result = await apiClient.delete('/items/1');

    expect(result).toBeNull();
  });

  test('request throws ApiError with status/message/details when response is not ok', async () => {
    global.fetch.mockResolvedValue({
      ok: false,
      status: 400,
      json: async () => ({ message: 'Datos inválidos', errors: [{ field: 'email' }] }),
    });

    await expect(apiClient.get('/bad')).rejects.toBeInstanceOf(ApiError);

    try {
      await apiClient.get('/bad');
    } catch (error) {
      expect(error.status).toBe(400);
      expect(error.message).toBe('Datos inválidos');
      expect(error.details).toEqual({ message: 'Datos inválidos', errors: [{ field: 'email' }] });
      expect(error.errors).toEqual([{ field: 'email' }]);
      expect(error.name).toBe('ApiError');
    }
  });

  test('request uses fallback message when error response has no message', async () => {
    global.fetch.mockResolvedValue({
      ok: false,
      status: 500,
      json: async () => ({}),
    });

    await expect(apiClient.get('/fail')).rejects.toMatchObject({
      status: 500,
      message: 'Error desconocido',
    });
  });
});

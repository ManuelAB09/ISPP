const mockApi = {
  post: jest.fn(),
  get: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
  interceptors: {
    request: {
      use: jest.fn(),
    },
  },
};

jest.mock('axios', () => ({
  create: jest.fn(() => mockApi),
}));

jest.mock('./baseUrl', () => ({
  getApiBaseUrl: jest.fn(() => 'https://api.test'),
}));

const {
  enviarMensajeComunidad,
  obtenerHistorialComunidad,
  editarMensajeComunidad,
  eliminarMensajeComunidad,
  enviarMensajePrivado,
  enviarArchivoPrivado,
  obtenerHistorialPrivado,
  enviarArchivoComunidad,
  obtenerArchivoChatBlob,
  obtenerConversaciones,
  eliminarMensajePrivado,
  editarMensajePrivado,
  obtenerPreviewEnlace,
  marcarConversacionComoLeida,
  obtenerNoLeidosPorComunidad,
  marcarComunidadComoLeida,
} = require('./mensajeService');

describe('mensajeService', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('enviarMensajeComunidad posts message payload', () => {
    enviarMensajeComunidad(10, 'Hola');
    expect(mockApi.post).toHaveBeenCalledWith('/comunidades/10/mensajes', { contenido: 'Hola' });
  });

  test('obtenerHistorialComunidad requests community history', () => {
    obtenerHistorialComunidad(10);
    expect(mockApi.get).toHaveBeenCalledWith('/comunidades/10/mensajes');
  });

  test('editar y eliminar mensaje de comunidad usan rutas correctas', () => {
    editarMensajeComunidad(10, 20, 'editado');
    eliminarMensajeComunidad(10, 20);

    expect(mockApi.put).toHaveBeenCalledWith('/comunidades/10/mensajes/20', { contenido: 'editado' });
    expect(mockApi.delete).toHaveBeenCalledWith('/comunidades/10/mensajes/20');
  });

  test('enviarMensajePrivado incluye userId solo cuando existe', () => {
    enviarMensajePrivado(5, 'hola');
    expect(mockApi.post).toHaveBeenCalledWith('/mensajes', { contenido: 'hola', userId: 5 });

    enviarMensajePrivado(null, 'solo texto');
    expect(mockApi.post).toHaveBeenLastCalledWith('/mensajes', { contenido: 'solo texto' });
  });

  test('enviarArchivoPrivado incluye userId y contenido trim cuando aplica', () => {
    const file = new File(['abc'], 'test.txt', { type: 'text/plain' });
    enviarArchivoPrivado(5, file, '  nota  ');

    const [url, formData, config] = mockApi.post.mock.calls[0];
    expect(url).toBe('/mensajes/upload');
    expect(formData).toBeInstanceOf(FormData);
    expect(formData.get('file')).toBe(file);
    expect(formData.get('userId')).toBe('5');
    expect(formData.get('contenido')).toBe('nota');
    expect(config).toEqual({ headers: { 'Content-Type': 'multipart/form-data' } });
  });

  test('enviarArchivoComunidad sube archivo con endpoint de comunidad', () => {
    const file = new File(['xyz'], 'img.png', { type: 'image/png' });
    enviarArchivoComunidad(44, file, 'caption');

    const [url, formData] = mockApi.post.mock.calls[0];
    expect(url).toBe('/comunidades/44/mensajes/upload');
    expect(formData).toBeInstanceOf(FormData);
    expect(formData.get('file')).toBe(file);
    expect(formData.get('contenido')).toBe('caption');
  });

  test('obtenerArchivoChatBlob normaliza URL con /api/v1 y pide blob', () => {
    obtenerArchivoChatBlob('/api/v1/files/doc.pdf');
    expect(mockApi.get).toHaveBeenCalledWith('/files/doc.pdf', { responseType: 'blob' });

    obtenerArchivoChatBlob('/media/doc.pdf');
    expect(mockApi.get).toHaveBeenLastCalledWith('/media/doc.pdf', { responseType: 'blob' });
  });

  test('obtenerArchivoChatBlob rejects when archivoUrl is missing', async () => {
    await expect(obtenerArchivoChatBlob('')).rejects.toThrow('archivoUrl es obligatorio');
  });

  test('chat helper endpoints use expected routes', () => {
    obtenerHistorialPrivado(7);
    obtenerConversaciones();
    eliminarMensajePrivado(77);
    editarMensajePrivado(77, 'nuevo');
    obtenerPreviewEnlace('https://example.com');
    marcarConversacionComoLeida(22);
    obtenerNoLeidosPorComunidad();
    marcarComunidadComoLeida(33);

    expect(mockApi.get).toHaveBeenCalledWith('/mensajes/usuario/7');
    expect(mockApi.get).toHaveBeenCalledWith('/mensajes/conversaciones');
    expect(mockApi.delete).toHaveBeenCalledWith('/mensajes/77');
    expect(mockApi.put).toHaveBeenCalledWith('/mensajes/77', { contenido: 'nuevo' });
    expect(mockApi.post).toHaveBeenCalledWith('/link-preview', { url: 'https://example.com' });
    expect(mockApi.post).toHaveBeenCalledWith('/mensajes/marcar-leida/22');
    expect(mockApi.get).toHaveBeenCalledWith('/comunidades/no-leidos');
    expect(mockApi.post).toHaveBeenCalledWith('/comunidades/33/marcar-leida');
  });
});

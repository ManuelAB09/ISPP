const mockGet = jest.fn();
const mockPost = jest.fn();

jest.mock('./client', () => ({
    apiClient: {
        get: (...args) => mockGet(...args),
        post: (...args) => mockPost(...args),
    },
}));

const { ubicacionesApi } = require('./ubicaciones.api');

describe('ubicacionesApi', () => {
    beforeEach(() => jest.clearAllMocks());

    test('create', () => {
        ubicacionesApi.create({ nombre: 'Test' });
        expect(mockPost).toHaveBeenCalledWith('/api/ubicaciones', { nombre: 'Test' });
    });

    test('listAll', () => {
        ubicacionesApi.listAll();
        expect(mockGet).toHaveBeenCalledWith('/api/ubicaciones');
    });

    test('buscarEstudio without tipo', () => {
        ubicacionesApi.buscarEstudio({ lat: 37, lon: -5, radio: 10 });
        expect(mockGet).toHaveBeenCalledWith(expect.stringContaining('lat=37'));
        expect(mockGet).toHaveBeenCalledWith(expect.not.stringContaining('tipo='));
    });

    test('buscarEstudio with tipo', () => {
        ubicacionesApi.buscarEstudio({ lat: 37, lon: -5, radio: 10, tipo: 'biblioteca' });
        expect(mockGet).toHaveBeenCalledWith(expect.stringContaining('tipo=biblioteca'));
    });
});

const mockGet = jest.fn();
const mockPost = jest.fn();

jest.mock('./client', () => ({
    apiClient: {
        get: (...args) => mockGet(...args),
        post: (...args) => mockPost(...args),
    },
}));

const {
    getValoracionesStats,
    getValoraciones,
    crearValoracion,
    checkAlreadyRated,
} = require('./valoraciones.api');

describe('valoraciones.api', () => {
    beforeEach(() => jest.clearAllMocks());

    test('getValoracionesStats', () => {
        getValoracionesStats(5);
        expect(mockGet).toHaveBeenCalledWith('/api/valoraciones/profesor/5');
    });

    test('getValoraciones', () => {
        getValoraciones(5);
        expect(mockGet).toHaveBeenCalledWith('/api/valoraciones/profesor/5/todas');
    });

    test('crearValoracion', () => {
        crearValoracion({ puntuacion: 5 });
        expect(mockPost).toHaveBeenCalledWith('/api/valoraciones', { puntuacion: 5 });
    });

    test('checkAlreadyRated', () => {
        checkAlreadyRated(1, 2);
        expect(mockGet).toHaveBeenCalledWith('/api/valoraciones/check?alumnoId=1&eventoId=2');
    });
});

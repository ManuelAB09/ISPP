const mockGet = jest.fn();
const mockPost = jest.fn();
const mockPut = jest.fn();
const mockDelete = jest.fn();

jest.mock('./client', () => ({
    apiClient: {
        get: (...args) => mockGet(...args),
        post: (...args) => mockPost(...args),
        put: (...args) => mockPut(...args),
        delete: (...args) => mockDelete(...args),
    },
}));

const {
    getDisponibilidades,
    crearDisponibilidad,
    actualizarDisponibilidad,
    eliminarDisponibilidad,
} = require('./disponibilidad');

describe('disponibilidad API', () => {
    beforeEach(() => jest.clearAllMocks());

    test('getDisponibilidades', () => {
        getDisponibilidades(5);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/disponibilidad/tutor/5');
    });

    test('crearDisponibilidad', () => {
        crearDisponibilidad({ dia: 'LUNES' });
        expect(mockPost).toHaveBeenCalledWith('/api/v1/disponibilidad', { dia: 'LUNES' });
    });

    test('actualizarDisponibilidad', () => {
        actualizarDisponibilidad(1, { dia: 'MARTES' });
        expect(mockPut).toHaveBeenCalledWith('/api/v1/disponibilidad/1', { dia: 'MARTES' });
    });

    test('eliminarDisponibilidad', () => {
        eliminarDisponibilidad(1);
        expect(mockDelete).toHaveBeenCalledWith('/api/v1/disponibilidad/1');
    });
});

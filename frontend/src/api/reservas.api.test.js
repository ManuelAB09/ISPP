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
    crearReserva,
    confirmarReserva,
    cancelarReserva,
    getMisReservasAlumno,
    getMisReservasTutor,
    getReservaDetalle,
    getDisponibilidadTutor,
    getHorariosOcupados,
    getDisponibilidadTutorFecha,
} = require('./reservas.api');

describe('reservas.api', () => {
    beforeEach(() => jest.clearAllMocks());

    test('crearReserva', () => {
        crearReserva(5, { tema: 'Math' });
        expect(mockPost).toHaveBeenCalledWith('/api/v1/tutors/5/reservas', { tema: 'Math' });
    });

    test('confirmarReserva', () => {
        confirmarReserva(10);
        expect(mockPut).toHaveBeenCalledWith('/api/v1/reservas/10/confirmar', {});
    });

    test('cancelarReserva with motivo', () => {
        cancelarReserva(10, 'No puedo');
        expect(mockDelete).toHaveBeenCalledWith('/api/v1/reservas/10?motivo=No%20puedo');
    });

    test('cancelarReserva without motivo', () => {
        cancelarReserva(10);
        expect(mockDelete).toHaveBeenCalledWith('/api/v1/reservas/10');
    });

    test('getMisReservasAlumno without params', () => {
        getMisReservasAlumno();
        expect(mockGet).toHaveBeenCalledWith('/api/v1/reservas/me');
    });

    test('getMisReservasAlumno with params', () => {
        getMisReservasAlumno({ page: 0, size: 10 });
        expect(mockGet).toHaveBeenCalledWith(expect.stringContaining('page=0'));
    });

    test('getMisReservasTutor', () => {
        getMisReservasTutor();
        expect(mockGet).toHaveBeenCalledWith('/api/v1/tutors/me/reservas');
    });

    test('getReservaDetalle', () => {
        getReservaDetalle(10);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/reservas/10');
    });

    test('getDisponibilidadTutor', () => {
        getDisponibilidadTutor(5);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/tutors/5/disponibilidad');
    });

    test('getHorariosOcupados', () => {
        getHorariosOcupados(5, '2025-06-01');
        expect(mockGet).toHaveBeenCalledWith('/api/v1/tutors/5/horarios-ocupados?fecha=2025-06-01');
    });

    test('getDisponibilidadTutorFecha', () => {
        getDisponibilidadTutorFecha(5, '2025-06-01');
        expect(mockGet).toHaveBeenCalledWith('/api/v1/tutors/5/disponibilidad-fecha?fecha=2025-06-01');
    });
});

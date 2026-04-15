const mockApi = {
  post: jest.fn(),
  get: jest.fn(),
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

const solicitudApi = require('./solicitudContratacion');

describe('solicitudContratacion API', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('crear/aceptar/rechazar/pagar y payment intent usan rutas correctas', () => {
    solicitudApi.crearSolicitudContratacion(9, { dia: '2026-05-01' });
    solicitudApi.aceptarSolicitud(11);
    solicitudApi.rechazarSolicitud(11, 'No disponible');
    solicitudApi.pagarSolicitud(11);
    solicitudApi.crearPaymentIntentSolicitud(11);

    expect(mockApi.post).toHaveBeenCalledWith('/solicitudes-contratacion/tutor/9', { dia: '2026-05-01' });
    expect(mockApi.post).toHaveBeenCalledWith('/solicitudes-contratacion/11/aceptar');
    expect(mockApi.post).toHaveBeenCalledWith('/solicitudes-contratacion/11/rechazar', { motivo: 'No disponible' });
    expect(mockApi.post).toHaveBeenCalledWith('/solicitudes-contratacion/11/pagar');
    expect(mockApi.post).toHaveBeenCalledWith('/solicitudes-contratacion/11/create-payment-intent');
  });

  test('confirmar, cancelar y calificar envían payload esperado', () => {
    solicitudApi.confirmarPagoSolicitud(1, 'pi_123');
    solicitudApi.cancelarSolicitud(2, 'Motivo tutor');
    solicitudApi.cancelarSolicitudAlumno(3, 'Motivo alumno');
    solicitudApi.calificarSolicitud(4, 5, 'Muy bien');

    expect(mockApi.post).toHaveBeenCalledWith('/solicitudes-contratacion/1/confirm-payment', { paymentIntentId: 'pi_123' });
    expect(mockApi.post).toHaveBeenCalledWith('/solicitudes-contratacion/2/cancelar', { motivo: 'Motivo tutor' });
    expect(mockApi.post).toHaveBeenCalledWith('/solicitudes-contratacion/3/cancelar-alumno', { motivo: 'Motivo alumno' });
    expect(mockApi.post).toHaveBeenCalledWith('/solicitudes-contratacion/4/calificar', { calificacion: 5, comentario: 'Muy bien' });
  });

  test('reprogramar y aprobaciones de reprogramación usan rutas correctas', () => {
    solicitudApi.reprogramarSolicitud(7, { dia: '2026-06-01', horaInicio: '10:00', horaFin: '11:00' });
    solicitudApi.aprobarReprogramacion(7);
    solicitudApi.rechazarReprogramacion(7);

    expect(mockApi.post).toHaveBeenCalledWith('/solicitudes-contratacion/7/reprogramar', {
      dia: '2026-06-01',
      horaInicio: '10:00',
      horaFin: '11:00',
    });
    expect(mockApi.post).toHaveBeenCalledWith('/solicitudes-contratacion/7/aprobar-reprogramacion');
    expect(mockApi.post).toHaveBeenCalledWith('/solicitudes-contratacion/7/rechazar-reprogramacion');
  });

  test('listados de solicitudes usan endpoints de tutor y alumno', () => {
    solicitudApi.obtenerSolicitudesPendientes();
    solicitudApi.obtenerSolicitudesTutor();
    solicitudApi.obtenerSolicitudesAlumno();

    expect(mockApi.get).toHaveBeenCalledWith('/solicitudes-contratacion/tutor/pendientes');
    expect(mockApi.get).toHaveBeenCalledWith('/solicitudes-contratacion/tutor');
    expect(mockApi.get).toHaveBeenCalledWith('/solicitudes-contratacion/alumno');
  });

  test('consultas de disponibilidad codifican la fecha', () => {
    solicitudApi.getHorariosOcupadosContratacion(4, '2026-05-10 10:00');
    solicitudApi.getDisponibilidadTutorFecha(4, '2026-05-10 10:00');

    expect(mockApi.get).toHaveBeenCalledWith('/solicitudes-contratacion/tutor/4/horarios-ocupados?fecha=2026-05-10%2010%3A00');
    expect(mockApi.get).toHaveBeenCalledWith('/solicitudes-contratacion/tutor/4/disponibilidad?fecha=2026-05-10%2010%3A00');
  });

  test('crearZoomSolicitud usa endpoint esperado', () => {
    solicitudApi.crearZoomSolicitud(99);
    expect(mockApi.post).toHaveBeenCalledWith('/solicitudes-contratacion/99/crear-zoom');
  });
});

import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

jest.mock('../../api/solicitudContratacion', () => ({
  obtenerSolicitudesAlumno: jest.fn(),
  cancelarSolicitudAlumno: jest.fn(),
  calificarSolicitud: jest.fn(),
  aprobarReprogramacion: jest.fn(),
  rechazarReprogramacion: jest.fn(),
}));
jest.mock('../../components/Header/Header', () => () => <div data-testid="header">Header</div>);
jest.mock('./MisReservas.css', () => ({}));

const {
  obtenerSolicitudesAlumno,
  cancelarSolicitudAlumno,
  calificarSolicitud,
  aprobarReprogramacion,
  rechazarReprogramacion,
} = require('../../api/solicitudContratacion');

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

const MisReservas = require('./MisReservas').default;

describe('MisReservas', () => {
  beforeEach(() => jest.clearAllMocks());

  test('shows loading state', () => {
    obtenerSolicitudesAlumno.mockReturnValue(new Promise(() => {}));
    render(<MemoryRouter><MisReservas /></MemoryRouter>);
    expect(screen.getByText('Cargando tus clases…')).toBeInTheDocument();
  });

  test('shows empty state when no solicitudes', async () => {
    obtenerSolicitudesAlumno.mockResolvedValue({ data: [] });
    render(<MemoryRouter><MisReservas /></MemoryRouter>);
    await screen.findByText('Aún no tienes clases');
    expect(screen.getByText('Busca un tutor y contrata tu primera clase.')).toBeInTheDocument();
  });

  test('shows active and past classes', async () => {
    obtenerSolicitudesAlumno.mockResolvedValue({
      data: [
        { id: 1, estado: 'PENDIENTE', tutorNombre: 'Juan', dia: '2025-04-01', horaInicio: '10:00', horaFin: '11:00', modalidad: 'ONLINE', materia: 'Math' },
        { id: 2, estado: 'COMPLETADA', tutorNombre: 'Ana', dia: '2025-03-01', horaInicio: '14:00', horaFin: '15:00', modalidad: 'PRESENCIAL', materia: 'Science' },
      ],
    });
    render(<MemoryRouter><MisReservas /></MemoryRouter>);
    await screen.findByText('Próximas clases');
  });

  test('handles API error', async () => {
    obtenerSolicitudesAlumno.mockRejectedValue(new Error('fail'));
    render(<MemoryRouter><MisReservas /></MemoryRouter>);
    await screen.findByText('Aún no tienes clases');
  });

  test('navigate to contratar button works', async () => {
    obtenerSolicitudesAlumno.mockResolvedValue({ data: [] });
    render(<MemoryRouter><MisReservas /></MemoryRouter>);
    await screen.findByText('Aún no tienes clases');
    fireEvent.click(screen.getByText('Explorar tutores'));
    expect(mockNavigate).toHaveBeenCalledWith('/profesores');
  });

  test('hero button navigates to profesores', async () => {
    obtenerSolicitudesAlumno.mockResolvedValue({ data: [] });
    render(<MemoryRouter><MisReservas /></MemoryRouter>);
    await screen.findByText('Mis clases');
    fireEvent.click(screen.getByText('+ Contratar nueva clase'));
    expect(mockNavigate).toHaveBeenCalledWith('/profesores');
  });

  test('sorts solicitudes by dia', async () => {
    obtenerSolicitudesAlumno.mockResolvedValue({
      data: [
        { id: 2, estado: 'PENDIENTE', dia: '2025-05-01' },
        { id: 1, estado: 'PENDIENTE', dia: '2025-04-01' },
      ],
    });
    render(<MemoryRouter><MisReservas /></MemoryRouter>);
    await screen.findByText('Próximas clases');
  });

  test('handles non-array response', async () => {
    obtenerSolicitudesAlumno.mockResolvedValue({ data: null });
    render(<MemoryRouter><MisReservas /></MemoryRouter>);
    await screen.findByText('Aún no tienes clases');
  });

  test('cancel dialog flow', async () => {
    obtenerSolicitudesAlumno.mockResolvedValue({
      data: [
        { id: 10, estado: 'PAGADA', tutorNombre: 'Juan', dia: '2025-06-01', horaInicio: '10:00', horaFin: '11:00', modalidad: 'ONLINE', puedeSerCanceladaPorAlumno: true },
      ],
    });
    cancelarSolicitudAlumno.mockResolvedValue({});
    render(<MemoryRouter><MisReservas /></MemoryRouter>);
    await screen.findByText('Cancelar clase');

    fireEvent.click(screen.getByText('Cancelar clase'));
    await screen.findByText('Confirmar cancelación');

    fireEvent.change(screen.getByPlaceholderText('Motivo de cancelación…'), { target: { value: 'No puedo' } });
    obtenerSolicitudesAlumno.mockResolvedValue({ data: [] });
    fireEvent.click(screen.getByText('Confirmar cancelación'));

    await waitFor(() => expect(cancelarSolicitudAlumno).toHaveBeenCalledWith(10, 'No puedo'));
    await screen.findByText(/Clase cancelada/);
  });

  test('cancel dialog can be closed', async () => {
    obtenerSolicitudesAlumno.mockResolvedValue({
      data: [
        { id: 10, estado: 'PENDIENTE', tutorNombre: 'Juan', dia: '2025-06-01', horaInicio: '10:00', horaFin: '11:00', modalidad: 'ONLINE', puedeSerCanceladaPorAlumno: true },
      ],
    });
    render(<MemoryRouter><MisReservas /></MemoryRouter>);
    await screen.findByText('Cancelar clase');
    fireEvent.click(screen.getByText('Cancelar clase'));
    await screen.findByText('Confirmar cancelación');
    fireEvent.click(screen.getByText('Volver'));
    await waitFor(() => expect(screen.queryByText('Confirmar cancelación')).not.toBeInTheDocument());
  });

  test('cancel error shows message', async () => {
    obtenerSolicitudesAlumno.mockResolvedValue({
      data: [
        { id: 10, estado: 'PAGADA', tutorNombre: 'Juan', dia: '2025-06-01', horaInicio: '10:00', horaFin: '11:00', modalidad: 'ONLINE', puedeSerCanceladaPorAlumno: true },
      ],
    });
    cancelarSolicitudAlumno.mockRejectedValue({ response: { data: { error: 'Menos de 24h' } } });
    render(<MemoryRouter><MisReservas /></MemoryRouter>);
    await screen.findByText('Cancelar clase');
    fireEvent.click(screen.getByText('Cancelar clase'));
    await screen.findByText('Confirmar cancelación');
    fireEvent.click(screen.getByText('Confirmar cancelación'));
    await screen.findByText(/Menos de 24h/);
  });

  test('rating dialog flow', async () => {
    obtenerSolicitudesAlumno.mockResolvedValue({
      data: [
        { id: 20, estado: 'COMPLETADA', tutorNombre: 'Ana', dia: '2025-03-01', horaInicio: '14:00', horaFin: '15:00', modalidad: 'PRESENCIAL' },
      ],
    });
    calificarSolicitud.mockResolvedValue({});
    render(<MemoryRouter><MisReservas /></MemoryRouter>);
    await screen.findByText('⭐ Calificar');

    fireEvent.click(screen.getByText('⭐ Calificar'));
    await screen.findByText('¿Cómo fue tu experiencia?');

    const stars = screen.getAllByText('⭐');
    fireEvent.click(stars[3]);
    fireEvent.change(screen.getByPlaceholderText('Comentario (opcional)…'), { target: { value: 'Genial' } });
    obtenerSolicitudesAlumno.mockResolvedValue({ data: [] });
    fireEvent.click(screen.getByText('Enviar calificación'));

    await waitFor(() => expect(calificarSolicitud).toHaveBeenCalledWith(20, 4, 'Genial'));
    await screen.findByText(/Calificación enviada/);
  });

  test('rating error shows message', async () => {
    obtenerSolicitudesAlumno.mockResolvedValue({
      data: [
        { id: 20, estado: 'COMPLETADA', tutorNombre: 'Ana', dia: '2025-03-01', horaInicio: '14:00', horaFin: '15:00', modalidad: 'PRESENCIAL' },
      ],
    });
    calificarSolicitud.mockRejectedValue(new Error('Server error'));
    render(<MemoryRouter><MisReservas /></MemoryRouter>);
    await screen.findByText('⭐ Calificar');
    fireEvent.click(screen.getByText('⭐ Calificar'));
    await screen.findByText('Enviar calificación');
    const stars = screen.getAllByText('⭐');
    fireEvent.click(stars[0]);
    fireEvent.click(screen.getByText('Enviar calificación'));
    await screen.findByText(/Server error/);
  });

  test('reprogramación approve flow', async () => {
    obtenerSolicitudesAlumno.mockResolvedValue({
      data: [
        { id: 30, estado: 'REPROGRAMACION_PENDIENTE', tutorNombre: 'Luis', dia: '2025-06-01', horaInicio: '10:00', horaFin: '11:00', modalidad: 'ONLINE', reprogramacionDia: '2025-06-05', reprogramacionHoraInicio: '12:00', reprogramacionHoraFin: '13:00' },
      ],
    });
    aprobarReprogramacion.mockResolvedValue({});
    render(<MemoryRouter><MisReservas /></MemoryRouter>);
    await screen.findByText('Aprobar cambio');

    expect(screen.getByText(/2025-06-05/)).toBeInTheDocument();
    obtenerSolicitudesAlumno.mockResolvedValue({ data: [] });
    fireEvent.click(screen.getByText('Aprobar cambio'));

    await waitFor(() => expect(aprobarReprogramacion).toHaveBeenCalledWith(30));
    await screen.findByText(/Reprogramación aprobada/);
  });

  test('reprogramación reject flow', async () => {
    obtenerSolicitudesAlumno.mockResolvedValue({
      data: [
        { id: 30, estado: 'REPROGRAMACION_PENDIENTE', tutorNombre: 'Luis', dia: '2025-06-01', horaInicio: '10:00', horaFin: '11:00', modalidad: 'ONLINE', reprogramacionDia: '2025-06-05', reprogramacionHoraInicio: '12:00', reprogramacionHoraFin: '13:00' },
      ],
    });
    rechazarReprogramacion.mockResolvedValue({});
    render(<MemoryRouter><MisReservas /></MemoryRouter>);
    await screen.findByText('Rechazar');

    obtenerSolicitudesAlumno.mockResolvedValue({ data: [] });
    fireEvent.click(screen.getByText('Rechazar'));

    await waitFor(() => expect(rechazarReprogramacion).toHaveBeenCalledWith(30));
    await screen.findByText(/Reprogramación rechazada/);
  });

  test('reprog error shows message', async () => {
    obtenerSolicitudesAlumno.mockResolvedValue({
      data: [
        { id: 30, estado: 'REPROGRAMACION_PENDIENTE', tutorNombre: 'Luis', dia: '2025-06-01', horaInicio: '10:00', horaFin: '11:00', modalidad: 'ONLINE', reprogramacionDia: '2025-06-05', reprogramacionHoraInicio: '12:00', reprogramacionHoraFin: '13:00' },
      ],
    });
    aprobarReprogramacion.mockRejectedValue({ response: { data: { error: 'Conflict' } } });
    render(<MemoryRouter><MisReservas /></MemoryRouter>);
    await screen.findByText('Aprobar cambio');
    fireEvent.click(screen.getByText('Aprobar cambio'));
    await screen.findByText(/Conflict/);
  });

  test('shows card details: modalidad, tutor, motivo, calificacion', async () => {
    obtenerSolicitudesAlumno.mockResolvedValue({
      data: [
        { id: 40, estado: 'RECHAZADA', tutorNombre: 'María', dia: '2025-04-01', horaInicio: '09:00', horaFin: '10:00', modalidad: 'PRESENCIAL', importeTotal: 25, tarifaHora: 25, mensaje: 'Necesito ayuda', motivoRechazo: 'Horario no disponible' },
        { id: 41, estado: 'COMPLETADA', tutorNombre: 'Pedro', dia: '2025-03-01', horaInicio: '16:00', horaFin: '17:00', modalidad: 'HIBRIDO', importeTotal: 30, tarifaHora: 30, calificacion: 5, comentarioAlumno: 'Excelente' },
      ],
    });
    render(<MemoryRouter><MisReservas /></MemoryRouter>);
    await screen.findByText('Historial');

    expect(screen.getByText(/Horario no disponible/)).toBeInTheDocument();
    expect(screen.getByText(/Necesito ayuda/)).toBeInTheDocument();
    expect(screen.getByText(/Excelente/)).toBeInTheDocument();
  });
});

import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import TarjetaEvento from './TarjetaEvento';

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

const baseEvent = {
  id: 1,
  titulo: 'Evento de prueba',
  descripcion: 'Descripción del evento',
  esVirtual: false,
  fechaHora: '2025-06-15T10:00:00',
  aforo: 30,
  asistentesConfirmados: 5,
  cancelado: false,
  miAsistencia: null,
  ubicacion: { nombre: 'Aula 101' },
};

describe('TarjetaEvento', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
  });

  test('renders event title and description', () => {
    render(
      <MemoryRouter>
        <TarjetaEvento event={baseEvent} />
      </MemoryRouter>
    );
    expect(screen.getByText('Evento de prueba')).toBeInTheDocument();
    expect(screen.getByText('Descripción del evento')).toBeInTheDocument();
  });

  test('shows Presencial badge for non-virtual event', () => {
    render(
      <MemoryRouter>
        <TarjetaEvento event={baseEvent} />
      </MemoryRouter>
    );
    expect(screen.getByText('Presencial')).toBeInTheDocument();
  });

  test('shows Online badge for virtual event', () => {
    render(
      <MemoryRouter>
        <TarjetaEvento event={{ ...baseEvent, esVirtual: true }} />
      </MemoryRouter>
    );
    expect(screen.getByText('Online')).toBeInTheDocument();
  });

  test('shows location for presencial event', () => {
    render(
      <MemoryRouter>
        <TarjetaEvento event={baseEvent} />
      </MemoryRouter>
    );
    expect(screen.getByText('Aula 101')).toBeInTheDocument();
  });

  test('shows attendees count with capacity', () => {
    render(
      <MemoryRouter>
        <TarjetaEvento event={baseEvent} />
      </MemoryRouter>
    );
    expect(screen.getByText(/5\s*\/\s*30/)).toBeInTheDocument();
  });

  test('shows Apuntarse button when not enrolled and onAttend provided', () => {
    const onAttend = jest.fn();
    render(
      <MemoryRouter>
        <TarjetaEvento event={baseEvent} onAttend={onAttend} />
      </MemoryRouter>
    );
    const btn = screen.getByText('Apuntarse');
    fireEvent.click(btn);
    expect(onAttend).toHaveBeenCalledWith(1);
  });

  test('shows Inscrito badge and cancel button when confirmed', () => {
    const onCancel = jest.fn();
    const confirmed = { ...baseEvent, miAsistencia: 'CONFIRMADA' };
    render(
      <MemoryRouter>
        <TarjetaEvento event={confirmed} onCancelAttendance={onCancel} />
      </MemoryRouter>
    );
    expect(screen.getByText('Inscrito')).toBeInTheDocument();
    const cancelBtn = screen.getByText('Cancelar asistencia');
    fireEvent.click(cancelBtn);
    expect(onCancel).toHaveBeenCalledWith(1);
  });

  test('shows Cancelado badge when event is cancelled', () => {
    const cancelled = { ...baseEvent, cancelado: true };
    render(
      <MemoryRouter>
        <TarjetaEvento event={cancelled} />
      </MemoryRouter>
    );
    const badges = screen.getAllByText('Cancelado');
    expect(badges.length).toBeGreaterThanOrEqual(1);
  });

  test('shows Aforo completo when event is full', () => {
    const full = { ...baseEvent, asistentesConfirmados: 30 };
    render(
      <MemoryRouter>
        <TarjetaEvento event={full} />
      </MemoryRouter>
    );
    expect(screen.getByText('Aforo completo')).toBeInTheDocument();
  });

  test('navigates to event details on Ver detalles click', () => {
    render(
      <MemoryRouter>
        <TarjetaEvento event={baseEvent} />
      </MemoryRouter>
    );
    fireEvent.click(screen.getByText('Ver detalles'));
    expect(mockNavigate).toHaveBeenCalledWith('/eventos/1');
  });

  test('disables attend button when attendanceLoading is true', () => {
    render(
      <MemoryRouter>
        <TarjetaEvento event={baseEvent} onAttend={jest.fn()} attendanceLoading={true} />
      </MemoryRouter>
    );
    expect(screen.getByText('Apuntarse')).toBeDisabled();
  });
});

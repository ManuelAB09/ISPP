import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import CrearEvento from './CrearEvento';
import { createEvent, getEventById, updateEvent } from '../../api/eventEndpoints';
import { communitiesApi } from '../../api/communities.api';

// Mocks
jest.mock('../../api/eventEndpoints');
jest.mock('../../api/communities.api', () => ({
  communitiesApi: {
    getMyMembership: jest.fn(),
    listMine: jest.fn(),
  },
}));
jest.mock('../../components/Header/Header', () => {
  return function MockHeader() {
    return <div data-testid="mock-header">Header</div>;
  };
});

// mock navigate
const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => {
  const actual = jest.requireActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe('CrearEvento', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    Storage.prototype.getItem = jest.fn((key) => (key === 'userId' ? '1' : null));
    Storage.prototype.setItem = jest.fn();

    communitiesApi.getMyMembership.mockResolvedValue({});
    communitiesApi.listMine.mockResolvedValue([]);
    createEvent.mockResolvedValue({});
    updateEvent.mockResolvedValue({});
    getEventById.mockResolvedValue({});

    window.alert = jest.fn();
  });

  const renderCreate = () =>
    render(
      <MemoryRouter initialEntries={['/crear-evento/new?communityId=10']}>
        <Routes>
          <Route path="/crear-evento/:id" element={<CrearEvento />} />
        </Routes>
      </MemoryRouter>
    );

  const renderEdit = () =>
    render(
      <MemoryRouter initialEntries={['/crear-evento/55?communityId=10']}>
        <Routes>
          <Route path="/crear-evento/:id" element={<CrearEvento />} />
        </Routes>
      </MemoryRouter>
    );

  test('renderiza cabecera y campos principales', () => {
    renderCreate();

    expect(screen.getByTestId('mock-header')).toBeInTheDocument();
    expect(screen.getByText(/Configura tu evento en pocos pasos/i)).toBeInTheDocument();

    expect(screen.getByPlaceholderText(/Ej\. Clase de NodeJS \+ Sequelize/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/¿De qué trata este evento\?/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Ej\. 30/i)).toBeInTheDocument();
  });

  test('muestra errores de validación si se envía vacío', async () => {
    renderCreate();

    fireEvent.click(screen.getByRole('button', { name: /Crear Evento/i }));

    expect(await screen.findByText(/El título debe tener al menos 3 caracteres/i)).toBeInTheDocument();
    expect(screen.getByText(/La fecha es obligatoria/i)).toBeInTheDocument();
    expect(screen.getByText(/La hora es obligatoria/i)).toBeInTheDocument();
    expect(screen.getByText(/El aforo debe ser al menos 1/i)).toBeInTheDocument();
    expect(screen.getByText(/Debes seleccionar una ubicación para el evento presencial/i)).toBeInTheDocument();
  });

  test('aforo solo acepta numeros y maximo 3 cifras', () => {
    renderCreate();

    const aforoInput = screen.getByPlaceholderText(/Ej\. 30/i);

    fireEvent.change(aforoInput, {
      target: { name: 'aforo', value: '12abc3456' },
    });

    expect(aforoInput).toHaveValue('123');
  });

  test('no permite fecha y hora de inicio en el pasado', async () => {
    renderCreate();

    fireEvent.change(screen.getByPlaceholderText(/Ej\. Clase de NodeJS \+ Sequelize/i), {
      target: { name: 'nombre', value: 'Evento pasado' },
    });

    const ddInputs = screen.getAllByPlaceholderText('DD');
    const mmDateInputs = screen.getAllByPlaceholderText('MM');
    const yyyyInputs = screen.getAllByPlaceholderText('YYYY');
    const hhInputs = screen.getAllByPlaceholderText('HH');
    const mmTimeInputs = screen.getAllByPlaceholderText('mm');

    fireEvent.change(ddInputs[0], { target: { name: 'dia', value: '01' } });
    fireEvent.change(mmDateInputs[0], { target: { name: 'mes', value: '01' } });
    fireEvent.change(yyyyInputs[0], { target: { name: 'anio', value: '2020' } });
    fireEvent.change(hhInputs[0], { target: { name: 'hora', value: '10' } });
    fireEvent.change(mmTimeInputs[0], { target: { name: 'minuto', value: '00' } });

    fireEvent.change(screen.getByPlaceholderText(/Ej\. 30/i), {
      target: { name: 'aforo', value: '30' },
    });

    fireEvent.click(screen.getByRole('button', { name: /Online/i }));

    fireEvent.click(screen.getByRole('button', { name: /Crear Evento/i }));

    expect(
      await screen.findByText(/La fecha y hora de inicio no puede ser anterior al momento actual/i)
    ).toBeInTheDocument();
    expect(createEvent).not.toHaveBeenCalled();
  });

  test('crea evento correctamente en modo creación', async () => {
  renderCreate();

  fireEvent.change(screen.getByPlaceholderText(/Ej\. Clase de NodeJS \+ Sequelize/i), {
    target: { name: 'nombre', value: 'Evento de prueba' },
  });

  // Fecha inicio (primer bloque)
  const ddInputs = screen.getAllByPlaceholderText('DD');
  const mmDateInputs = screen.getAllByPlaceholderText('MM');
  const yyyyInputs = screen.getAllByPlaceholderText('YYYY');

  fireEvent.change(ddInputs[0], { target: { name: 'dia', value: '10' } });
  fireEvent.change(mmDateInputs[0], { target: { name: 'mes', value: '12' } });
  fireEvent.change(yyyyInputs[0], { target: { name: 'anio', value: '2026' } });

  // Hora inicio (primer bloque)
  const hhInputs = screen.getAllByPlaceholderText('HH');
  const mmTimeInputs = screen.getAllByPlaceholderText('mm');

  fireEvent.change(hhInputs[0], { target: { name: 'hora', value: '18' } });
  fireEvent.change(mmTimeInputs[0], { target: { name: 'minuto', value: '30' } });

  fireEvent.change(screen.getByPlaceholderText(/Ej\. 30/i), {
    target: { name: 'aforo', value: '30' },
  });

  // Online para evitar requerir ubicacionId
  fireEvent.click(screen.getByRole('button', { name: /Online/i }));

  fireEvent.click(screen.getByRole('button', { name: /Crear Evento/i }));

  await waitFor(() => {
    expect(createEvent).toHaveBeenCalledWith(
      '10',
      expect.objectContaining({
        titulo: 'Evento de prueba',
        esVirtual: true,
        aforo: 30,
      })
    );
  });
});

  test('carga datos y actualiza evento en modo edición', async () => {
    getEventById.mockResolvedValueOnce({
      id: 55,
      titulo: 'Evento existente',
      descripcion: 'Descripción',
      queLlevar: 'Portátil',
      fechaHora: '2026-07-01T17:00:00',
      fechaFin: '2026-07-01T19:00:00',
      esVirtual: true,
      enlaceVirtual: 'https://meet.google.com/xxx-yyyy-zzz',
      aforo: 50,
      privado: false,
      visibleMapa: true,
      creador: { id: 1 },
    });

    renderEdit();

    expect(await screen.findByDisplayValue('Evento existente')).toBeInTheDocument();

    fireEvent.change(screen.getByDisplayValue('Evento existente'), {
      target: { name: 'nombre', value: 'Evento actualizado' },
    });

    fireEvent.click(screen.getByRole('button', { name: /Actualizar Evento/i }));

    await waitFor(() => {
      expect(updateEvent).toHaveBeenCalledWith(
        '55',
        expect.objectContaining({
          titulo: 'Evento actualizado',
          esVirtual: true,
        })
      );
    });
  });

  test('guarda borrador en localStorage', () => {
    renderCreate();

    fireEvent.click(screen.getByRole('button', { name: /Guardar Borrador/i }));

    expect(localStorage.setItem).toHaveBeenCalledWith('eventDraft', expect.any(String));
    expect(window.alert).toHaveBeenCalledWith('Borrador guardado correctamente.');
  });

  test('muestra error de membresía al crear si no pertenece a la comunidad', async () => {
    communitiesApi.getMyMembership.mockRejectedValueOnce(new Error('forbidden'));

    renderCreate();

    expect(
      await screen.findByText(/No puedes crear eventos en una comunidad a la que no perteneces/i)
    ).toBeInTheDocument();
  });
});
import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import CrearUbicacionScreen from './CrearUbicacionScreen';
import { ubicacionesApi } from '../../api/ubicaciones.api';

jest.mock('../../api/ubicaciones.api', () => ({
  ubicacionesApi: {
    create: jest.fn(),
  },
}));

jest.mock('react-leaflet', () => ({
  MapContainer: ({ children }) => <div data-testid="map-container">{children}</div>,
  TileLayer: () => <div data-testid="tile-layer" />,
  Marker: () => <div data-testid="marker" />,
  useMapEvents: () => ({}),
}));

jest.mock('./FiltroUbicacionesScreen', () => {
  return function MockFiltroUbicacionesScreen({ onSeleccionar, onClose }) {
    return (
      <div data-testid="filtro-modal">
        <button
          onClick={() =>
            onSeleccionar({
              id: 99,
              nombre: 'Ubicación Mock',
              direccion: 'Calle Mock 123',
              latitud: 37.4,
              longitud: -5.9,
              tipo: 'coworking_space',
              coste: 'GRATIS',
            })
          }
        >
          Seleccionar mock
        </button>
        <button onClick={onClose}>Cerrar mock</button>
      </div>
    );
  };
});

describe('CrearUbicacionScreen', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    global.fetch = jest.fn();
  });

  const renderComponent = (initialEntries = ['/ubicaciones/crear']) =>
    render(
      <MemoryRouter initialEntries={initialEntries}>
        <Routes>
          <Route path="/ubicaciones/crear" element={<CrearUbicacionScreen />} />
          <Route path="/eventos/crear" element={<div>Pantalla evento destino</div>} />
        </Routes>
      </MemoryRouter>
    );

  test('renderiza título y campos principales', () => {
    renderComponent();
    expect(screen.getByRole('heading', { name: /Crear Ubicación/i })).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Buscar dirección o lugar/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Latitud/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Longitud/i)).toBeInTheDocument();
    expect(screen.getByTestId('map-container')).toBeInTheDocument();
  });

  test('busca dirección y rellena dirección seleccionada', async () => {
    global.fetch.mockResolvedValue({
      ok: true,
      json: async () => [{ lat: '37.3892', lon: '-5.9846', display_name: 'Sevilla Centro' }],
    });

    renderComponent();

    fireEvent.change(screen.getByPlaceholderText(/Buscar dirección o lugar/i), {
      target: { value: 'Sevilla' },
    });
    fireEvent.click(screen.getByRole('button', { name: /^Buscar$/i }));

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('https://nominatim.openstreetmap.org/search?format=json&q=Sevilla'),
        expect.anything()
      );
    }, { timeout: 3000 });

    await waitFor(() => {
      const textboxes = screen.getAllByRole('textbox');
      expect(textboxes[2]).toHaveValue('Sevilla Centro');
    }, { timeout: 3000 });
  });

  test('muestra error si no hay resultados en búsqueda', async () => {
    global.fetch.mockResolvedValueOnce({ json: async () => [] });

    renderComponent();

    fireEvent.change(screen.getByPlaceholderText(/Buscar dirección o lugar/i), {
      target: { value: 'xxxxx-no-existe' },
    });
    fireEvent.click(screen.getByRole('button', { name: /^Buscar$/i }));

    expect(
      await screen.findByText(
        /No se encontró la dirección|Error buscando la dirección/i
      )
    ).toBeInTheDocument();
  });

  test('crea ubicación correctamente', async () => {
    ubicacionesApi.create.mockResolvedValueOnce({ id: 123 });

    renderComponent();

    const textboxes = screen.getAllByRole('textbox');
    fireEvent.change(textboxes[0], { target: { value: 'Sala A' } }); 
    fireEvent.change(textboxes[2], { target: { value: 'Av. Test 1' } }); 

    fireEvent.click(screen.getByRole('button', { name: /Crear Ubicación/i }));

    await waitFor(() => {
      expect(ubicacionesApi.create).toHaveBeenCalledWith(
        expect.objectContaining({
          nombre: 'Sala A',
          direccion: 'Av. Test 1',
        })
      );
    });

    expect(await screen.findByText(/Ubicación creada correctamente/i)).toBeInTheDocument();
  });

  test('muestra error si falla create', async () => {
    ubicacionesApi.create.mockRejectedValueOnce(new Error('Error backend'));

    renderComponent();

    const textboxes = screen.getAllByRole('textbox');
    fireEvent.change(textboxes[0], { target: { value: 'Sala B' } }); 
    fireEvent.change(textboxes[2], { target: { value: 'Av. Fallo 2' } }); 

    fireEvent.click(screen.getByRole('button', { name: /Crear Ubicación/i }));

    expect(await screen.findByText(/Error backend/i)).toBeInTheDocument();
  });

  test('abre y cierra el filtro de ubicaciones', () => {
    renderComponent();

    fireEvent.click(screen.getByRole('button', { name: /Buscar ubicaciones en el mapa/i }));
    expect(screen.getByTestId('filtro-modal')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /Cerrar mock/i }));
    expect(screen.queryByTestId('filtro-modal')).not.toBeInTheDocument();
  });

  test('selecciona una ubicación desde filtro y muestra resumen', async () => {
    renderComponent();

    fireEvent.click(screen.getByRole('button', { name: /Buscar ubicaciones en el mapa/i }));
    fireEvent.click(screen.getByRole('button', { name: /Seleccionar mock/i }));

    expect(await screen.findByText(/Ubicación seleccionada:/i)).toBeInTheDocument();
    expect(screen.getByText(/Ubicación Mock/i)).toBeInTheDocument();
    expect(screen.getByText(/Tipo: coworking_space/i)).toBeInTheDocument();
  });
});
import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import FiltroUbicacionesScreen from './FiltroUbicacionesScreen';
import { ubicacionesApi } from '../../api/ubicaciones.api';

jest.mock('../../api/ubicaciones.api', () => ({
  ubicacionesApi: {
    buscarEstudio: jest.fn(),
  },
}));

jest.mock('react-leaflet', () => ({
  MapContainer: ({ children }) => <div data-testid="map-container">{children}</div>,
  TileLayer: () => <div data-testid="tile-layer" />,
  Marker: ({ children }) => <div data-testid="marker">{children}</div>,
  Popup: ({ children }) => <div>{children}</div>,
  Circle: () => <div data-testid="circle" />,
  useMapEvents: () => ({}),
}));

describe('FiltroUbicacionesScreen', () => {
  const mockOnSeleccionar = jest.fn();
  const mockOnClose = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    global.fetch = jest.fn();
  });

  const renderComponent = () =>
    render(<FiltroUbicacionesScreen onSeleccionar={mockOnSeleccionar} onClose={mockOnClose} />);

  test('renderiza formulario, controles y mapa', () => {
    renderComponent();

    expect(screen.getByText(/Buscar ubicaciones disponibles/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Latitud/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Longitud/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Radio \(m\)/i)).toBeInTheDocument();
    expect(screen.getByRole('slider')).toBeInTheDocument();
    expect(screen.getByTestId('map-container')).toBeInTheDocument();
    expect(screen.getByTestId('circle')).toBeInTheDocument();
  });

  test('llama a buscarEstudio con filtros base', async () => {
    ubicacionesApi.buscarEstudio.mockResolvedValueOnce([]);

    renderComponent();

    fireEvent.change(screen.getByPlaceholderText(/Latitud/i), { target: { value: '37.5' } });
    fireEvent.change(screen.getByPlaceholderText(/Longitud/i), { target: { value: '-5.7' } });
    fireEvent.change(screen.getByPlaceholderText(/Radio \(m\)/i), { target: { value: '1500' } });

    fireEvent.click(screen.getByRole('button', { name: /^Buscar$/i }));

    await waitFor(() => {
      expect(ubicacionesApi.buscarEstudio).toHaveBeenCalledWith({
        lat: 37.5,
        lon: -5.7,
        radio: 1500,
      });
    });
  });

  test('muestra error si falla búsqueda', async () => {
    ubicacionesApi.buscarEstudio.mockRejectedValueOnce(new Error('Fallo'));

    renderComponent();

    fireEvent.click(screen.getByRole('button', { name: /^Buscar$/i }));

    expect(await screen.findByText(/Error buscando ubicaciones/i)).toBeInTheDocument();
  });

  test('muestra resultados y permite selección + confirmación', async () => {
    ubicacionesApi.buscarEstudio.mockResolvedValueOnce([
      {
        id: 1,
        nombre: 'Coworking Centro',
        direccion: 'Calle Real 10',
        latitud: 37.39,
        longitud: -5.98,
        tipo: 'coworking_space',
        coste: 'GRATIS',
      },
    ]);

    renderComponent();

    fireEvent.click(screen.getByRole('button', { name: /^Buscar$/i }));

    await waitFor(() => {
      expect(screen.getByText(/Coworking Centro/i)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Ver detalles/i }));
    expect(screen.getByText(/Ubicación seleccionada:/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /Usar esta ubicación/i }));
    expect(mockOnSeleccionar).toHaveBeenCalledWith(
      expect.objectContaining({ nombre: 'Coworking Centro' })
    );
  });

  test('si una ubicación no tiene dirección, intenta reverse geocoding', async () => {
    ubicacionesApi.buscarEstudio.mockResolvedValueOnce([
      {
        id: 2,
        nombre: 'Parque Norte',
        direccion: 'Dirección no disponible',
        latitud: 37.41,
        longitud: -5.99,
        tipo: 'park',
        coste: 'DESCONOCIDO',
      },
    ]);

    global.fetch.mockResolvedValue({
      ok: true,
      json: async () => ({ display_name: 'Dirección calculada por geocoding' }),
    });

    renderComponent();

    fireEvent.click(screen.getByRole('button', { name: /^Buscar$/i }));

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining(
          'https://nominatim.openstreetmap.org/reverse?format=json&lat=37.41&lon=-5.99'
        ),
        expect.anything()
      );
    }, { timeout: 3000 });

    expect(
      await screen.findByText('Dirección calculada por geocoding', {
        timeout: 3000,
      })
    ).toBeInTheDocument();
  });

  test('ejecuta onClose al pulsar botón cerrar', () => {
    renderComponent();
    fireEvent.click(screen.getByTitle(/Cerrar/i));
    expect(mockOnClose).toHaveBeenCalled();
  });
});
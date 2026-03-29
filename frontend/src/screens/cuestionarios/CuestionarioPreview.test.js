import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

jest.mock('../../api/cuestionarios.api', () => ({
  cuestionariosApi: {
    getPreview: jest.fn(),
  },
}));
jest.mock('../../components/Header/Header', () => () => <div data-testid="header">Header</div>);
jest.mock('./CuestionarioPreview.css', () => ({}));

const { cuestionariosApi } = require('../../api/cuestionarios.api');

// Import after mocks
const CuestionarioPreview = require('./CuestionarioPreview').default;

const renderWithRouter = (id = '1') => {
  return render(
    <MemoryRouter initialEntries={[`/cuestionarios/${id}`]}>
      <Routes>
        <Route path="/cuestionarios/:id" element={<CuestionarioPreview />} />
      </Routes>
    </MemoryRouter>
  );
};

describe('CuestionarioPreview', () => {
  beforeEach(() => jest.clearAllMocks());

  test('shows loading state', () => {
    cuestionariosApi.getPreview.mockReturnValue(new Promise(() => {}));
    renderWithRouter();
    expect(screen.getByText('Cargando cuestionario...')).toBeInTheDocument();
  });

  test('shows quiz details on success', async () => {
    cuestionariosApi.getPreview.mockResolvedValue({
      titulo: 'Math Quiz',
      descripcion: 'Test your math skills',
      materia: 'Matemáticas',
      dificultad: 'INTERMEDIO',
      numPreguntas: 10,
      tiempoEstimadoMinutos: 15,
      puedeResolver: true,
      intentosPrevios: [],
    });
    renderWithRouter();
    await screen.findByText('Math Quiz');
    expect(screen.getByText('Test your math skills')).toBeInTheDocument();
    expect(screen.getByText('Matemáticas')).toBeInTheDocument();
    expect(screen.getByText(/Preguntas: 10/)).toBeInTheDocument();
    expect(screen.getByText('Comenzar')).toBeInTheDocument();
    expect(screen.getByText('Todavía no has realizado este cuestionario.')).toBeInTheDocument();
  });

  test('shows previous attempts', async () => {
    cuestionariosApi.getPreview.mockResolvedValue({
      titulo: 'Quiz',
      materia: 'Test',
      numPreguntas: 5,
      puedeResolver: true,
      intentosPrevios: [
        { id: 1, puntuacion: 80, createdAt: '2025-01-01T10:00:00' },
        { id: 2, puntuacion: 90, createdAt: '2025-01-02T10:00:00' },
      ],
    });
    renderWithRouter();
    await screen.findByText(/80\.0%/);
    expect(screen.getByText(/90\.0%/)).toBeInTheDocument();
    expect(screen.getByText('Volver a intentarlo')).toBeInTheDocument();
  });

  test('shows error on API failure', async () => {
    cuestionariosApi.getPreview.mockRejectedValue(new Error('Not found'));
    renderWithRouter();
    await screen.findByText('Not found');
  });

  test('shows error with response data message', async () => {
    cuestionariosApi.getPreview.mockRejectedValue({
      response: { data: { message: 'Quiz not found' } },
    });
    renderWithRouter();
    await screen.findByText('Quiz not found');
  });

  test('disables button when puedeResolver is false', async () => {
    cuestionariosApi.getPreview.mockResolvedValue({
      titulo: 'Quiz',
      numPreguntas: 5,
      puedeResolver: false,
      intentosPrevios: [],
    });
    renderWithRouter();
    await waitFor(() => expect(screen.getByText('Comenzar')).toBeDisabled());
  });

  test('handles null values gracefully', async () => {
    cuestionariosApi.getPreview.mockResolvedValue({
      titulo: null,
      materia: null,
      dificultad: null,
      numPreguntas: null,
      tiempoEstimadoMinutos: null,
      puedeResolver: true,
      intentosPrevios: null,
    });
    renderWithRouter();
    await screen.findByText('Cuestionario');
    expect(screen.getByText('Sin materia')).toBeInTheDocument();
  });

  test('attempt with null date shows Fecha desconocida', async () => {
    cuestionariosApi.getPreview.mockResolvedValue({
      titulo: 'Quiz',
      numPreguntas: 1,
      puedeResolver: true,
      intentosPrevios: [{ id: 1, puntuacion: 50, createdAt: null }],
    });
    renderWithRouter();
    await screen.findByText('Fecha desconocida');
  });

  test('attempt with invalid date shows Fecha desconocida', async () => {
    cuestionariosApi.getPreview.mockResolvedValue({
      titulo: 'Quiz',
      numPreguntas: 1,
      puedeResolver: true,
      intentosPrevios: [{ id: 1, puntuacion: 50, createdAt: 'invalid' }],
    });
    renderWithRouter();
    await screen.findByText('Fecha desconocida');
  });

  test('Volver button exists', async () => {
    cuestionariosApi.getPreview.mockResolvedValue({
      titulo: 'Quiz',
      numPreguntas: 1,
      puedeResolver: true,
      intentosPrevios: [],
    });
    renderWithRouter();
    await screen.findByText('Volver');
  });
});

import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

jest.mock('../../api/cuestionarios.api', () => ({
  cuestionariosApi: {
    listPublic: jest.fn(),
    listAssigned: jest.fn(),
  },
}));
jest.mock('../../components/Header/Header', () => () => <div data-testid="header">Header</div>);
jest.mock('./CuestionariosPublicos.css', () => ({}));

const { cuestionariosApi } = require('../../api/cuestionarios.api');
const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));
const CuestionariosPublicos = require('./CuestionariosPublicos').default;

describe('CuestionariosPublicos', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();
  });

  test('routes create button through public-only flow', async () => {
    localStorage.setItem('accessToken', 'token');
    cuestionariosApi.listPublic.mockResolvedValue([]);
    cuestionariosApi.listAssigned.mockResolvedValue([]);

    render(<MemoryRouter><CuestionariosPublicos /></MemoryRouter>);

    await screen.findByText('No hay cuestionarios públicos disponibles.');
    const createButton = screen.getByText(/Crear cuestionario/);
    fireEvent.click(createButton);

    expect(mockNavigate).toHaveBeenCalledWith('/cuestionarios/crear?publicOnly=1');
  });

  test('shows loading state', () => {
    cuestionariosApi.listPublic.mockReturnValue(new Promise(() => {}));
    render(<MemoryRouter><CuestionariosPublicos /></MemoryRouter>);
    expect(screen.getByText('Cargando cuestionarios...')).toBeInTheDocument();
  });

  test('shows empty state', async () => {
    cuestionariosApi.listPublic.mockResolvedValue([]);
    render(<MemoryRouter><CuestionariosPublicos /></MemoryRouter>);
    await screen.findByText('No hay cuestionarios públicos disponibles.');
  });

  test('shows error on failure', async () => {
    cuestionariosApi.listPublic.mockRejectedValue(new Error('fail'));
    render(<MemoryRouter><CuestionariosPublicos /></MemoryRouter>);
    await screen.findByText('No se pudieron cargar los cuestionarios públicos.');
  });

  test('renders quiz cards', async () => {
    cuestionariosApi.listPublic.mockResolvedValue([
      { id: 1, titulo: 'Math Quiz', materia: 'Math', dificultad: 'BASICO', numPreguntas: 10 },
      { id: 2, titulo: 'Science Quiz', materia: 'Science', dificultad: 'AVANZADO', numPreguntas: 5 },
    ]);
    render(<MemoryRouter><CuestionariosPublicos /></MemoryRouter>);
    await screen.findByText('Math Quiz');
    expect(screen.getByText('Science Quiz')).toBeInTheDocument();
    expect(screen.getByText('Básico')).toBeInTheDocument();
    expect(screen.getByText('Avanzado')).toBeInTheDocument();
  });

  test('shows create button when authenticated', async () => {
    localStorage.setItem('accessToken', 'token');
    cuestionariosApi.listPublic.mockResolvedValue([]);
    cuestionariosApi.listAssigned.mockResolvedValue([]);
    render(<MemoryRouter><CuestionariosPublicos /></MemoryRouter>);
    await screen.findByText('No hay cuestionarios públicos disponibles.');
    await screen.findByText(/Crear cuestionario/);
  });

  test('hides create button when not authenticated', async () => {
    cuestionariosApi.listPublic.mockResolvedValue([]);
    render(<MemoryRouter><CuestionariosPublicos /></MemoryRouter>);
    await screen.findByText('No hay cuestionarios públicos disponibles.');
    expect(screen.queryByText(/Crear cuestionario/)).not.toBeInTheDocument();
  });

  test('merges assigned and public quizzes, deduplicating by id', async () => {
    localStorage.setItem('accessToken', 'token');
    cuestionariosApi.listPublic.mockResolvedValue([
      { id: 1, titulo: 'Public Quiz' },
      { id: 2, titulo: 'Shared Quiz' },
    ]);
    cuestionariosApi.listAssigned.mockResolvedValue([
      { id: 2, titulo: 'Shared Quiz' },
      { id: 3, titulo: 'Assigned Quiz' },
    ]);
    render(<MemoryRouter><CuestionariosPublicos /></MemoryRouter>);
    await screen.findByText('Public Quiz');
    expect(screen.getByText('Assigned Quiz')).toBeInTheDocument();
    // Shared Quiz should appear only once
    expect(screen.getAllByText('Shared Quiz')).toHaveLength(1);
  });

  test('handles assigned API failure gracefully', async () => {
    localStorage.setItem('accessToken', 'token');
    cuestionariosApi.listPublic.mockResolvedValue([{ id: 1, titulo: 'Public' }]);
    cuestionariosApi.listAssigned.mockRejectedValue(new Error('fail'));
    render(<MemoryRouter><CuestionariosPublicos /></MemoryRouter>);
    await screen.findByText('Public');
  });

  test('title shows', async () => {
    cuestionariosApi.listPublic.mockResolvedValue([]);
    render(<MemoryRouter><CuestionariosPublicos /></MemoryRouter>);
    await screen.findByText('No hay cuestionarios públicos disponibles.');
    expect(screen.getByText('Cuestionarios públicos')).toBeInTheDocument();
  });
});

import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

jest.mock('../../api/cuestionarios.api', () => ({
  cuestionariosApi: {
    getResolver: jest.fn(),
    submitAttempt: jest.fn(),
  },
}));
jest.mock('../../components/Header/Header', () => () => <div data-testid="header">Header</div>);
jest.mock('./CuestionarioResolver.css', () => ({}));

const { cuestionariosApi } = require('../../api/cuestionarios.api');
const CuestionarioResolver = require('./CuestionarioResolver').default;

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

const renderWithRouter = (id = '1') => {
  return render(
    <MemoryRouter initialEntries={[`/cuestionarios/${id}/resolver`]}>
      <Routes>
        <Route path="/cuestionarios/:id/resolver" element={<CuestionarioResolver />} />
      </Routes>
    </MemoryRouter>
  );
};

describe('CuestionarioResolver', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('shows loading state', () => {
    cuestionariosApi.getResolver.mockReturnValue(new Promise(() => {}));
    renderWithRouter();
    expect(screen.getByText('Cargando cuestionario...')).toBeInTheDocument();
  });

  test('shows error on API failure', async () => {
    cuestionariosApi.getResolver.mockRejectedValue(new Error('Failed'));
    renderWithRouter();
    await screen.findByText('Failed');
  });

  test('renders quiz with test questions', async () => {
    cuestionariosApi.getResolver.mockResolvedValue({
      titulo: 'Science Quiz',
      preguntas: [
        {
          id: 1,
          enunciado: 'What is H2O?',
          tipo: 'TEST',
          opciones: [
            { id: 10, texto: 'Water' },
            { id: 11, texto: 'Fire' },
          ],
        },
      ],
    });
    renderWithRouter();
    await screen.findByText('Science Quiz');
    expect(screen.getByText(/What is H2O\?/)).toBeInTheDocument();
    expect(screen.getByText('Water')).toBeInTheDocument();
    expect(screen.getByText('Fire')).toBeInTheDocument();
  });

  test('renders true/false question with radio buttons', async () => {
    cuestionariosApi.getResolver.mockResolvedValue({
      titulo: 'TF Quiz',
      preguntas: [
        {
          id: 1,
          enunciado: 'Earth is flat',
          tipo: 'VERDADERO_FALSO',
          opciones: [
            { id: 10, texto: 'Verdadero' },
            { id: 11, texto: 'Falso' },
          ],
        },
      ],
    });
    renderWithRouter();
    await screen.findByText(/Earth is flat/);
    const radios = screen.getAllByRole('radio');
    expect(radios).toHaveLength(2);
  });

  test('renders text question with textarea', async () => {
    cuestionariosApi.getResolver.mockResolvedValue({
      titulo: 'Essay',
      preguntas: [
        { id: 1, enunciado: 'Explain gravity', tipo: 'TEXTO' },
      ],
    });
    renderWithRouter();
    await screen.findByText(/Explain gravity/);
    expect(screen.getByPlaceholderText('Escribe tu respuesta')).toBeInTheDocument();
  });

  test('can select checkbox options', async () => {
    cuestionariosApi.getResolver.mockResolvedValue({
      titulo: 'Multi',
      preguntas: [
        {
          id: 1,
          enunciado: 'Q1',
          tipo: 'TEST',
          opciones: [{ id: 10, texto: 'A' }, { id: 11, texto: 'B' }],
        },
      ],
    });
    renderWithRouter();
    await screen.findByText(/Q1/);
    const checkboxes = screen.getAllByRole('checkbox');
    fireEvent.click(checkboxes[0]);
    expect(checkboxes[0]).toBeChecked();
    // Toggle off
    fireEvent.click(checkboxes[0]);
    expect(checkboxes[0]).not.toBeChecked();
  });

  test('can select radio option for true/false', async () => {
    cuestionariosApi.getResolver.mockResolvedValue({
      titulo: 'TF',
      preguntas: [
        {
          id: 1,
          enunciado: 'Q1',
          tipo: 'VERDADERO_FALSO',
          opciones: [{ id: 10, texto: 'V' }, { id: 11, texto: 'F' }],
        },
      ],
    });
    renderWithRouter();
    await screen.findByText(/Q1/);
    const radios = screen.getAllByRole('radio');
    fireEvent.click(radios[0]);
    expect(radios[0]).toBeChecked();
  });

  test('can type text answer', async () => {
    cuestionariosApi.getResolver.mockResolvedValue({
      titulo: 'Text',
      preguntas: [{ id: 1, enunciado: 'Q1', tipo: 'TEXTO' }],
    });
    renderWithRouter();
    await screen.findByText(/Q1/);
    const textarea = screen.getByPlaceholderText('Escribe tu respuesta');
    fireEvent.change(textarea, { target: { value: 'My answer' } });
    expect(textarea.value).toBe('My answer');
  });

  test('submit navigates to result', async () => {
    cuestionariosApi.getResolver.mockResolvedValue({
      titulo: 'Quiz',
      preguntas: [{ id: 1, enunciado: 'Q1', tipo: 'TEXTO' }],
    });
    cuestionariosApi.submitAttempt.mockResolvedValue({ puntuacion: 100 });

    renderWithRouter();
    await screen.findByText(/Q1/);
    fireEvent.click(screen.getByText('Entregar cuestionario'));
    await waitFor(() => expect(cuestionariosApi.submitAttempt).toHaveBeenCalled());
    expect(mockNavigate).toHaveBeenCalled();
  });

  test('submit shows error on failure', async () => {
    cuestionariosApi.getResolver.mockResolvedValue({
      titulo: 'Quiz',
      preguntas: [{ id: 1, enunciado: 'Q1', tipo: 'TEXTO' }],
    });
    cuestionariosApi.submitAttempt.mockRejectedValue(new Error('Submit failed'));

    renderWithRouter();
    await screen.findByText(/Q1/);
    fireEvent.click(screen.getByText('Entregar cuestionario'));
    await screen.findByText(/Submit failed|No se pudo enviar/);
  });

  test('Volver al resumen button exists', async () => {
    cuestionariosApi.getResolver.mockResolvedValue({
      titulo: 'Quiz',
      preguntas: [],
    });
    renderWithRouter();
    await screen.findByText('Volver al resumen');
  });

  test('handles null preguntas', async () => {
    cuestionariosApi.getResolver.mockResolvedValue({
      titulo: 'Empty',
      preguntas: null,
    });
    renderWithRouter();
    await screen.findByText('Empty');
    expect(screen.getByText('0 preguntas')).toBeInTheDocument();
  });
});

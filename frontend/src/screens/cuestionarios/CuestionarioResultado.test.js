import React from 'react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

jest.mock('../../components/Header/Header', () => () => <div data-testid="header">Header</div>);
jest.mock('./CuestionarioResultado.css', () => ({}));

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

const CuestionarioResultado = require('./CuestionarioResultado').default;

const renderWithState = (state = null) => {
  return render(
    <MemoryRouter initialEntries={[{ pathname: '/cuestionarios/1/resultado', state }]}>
      <Routes>
        <Route path="/cuestionarios/:id/resultado" element={<CuestionarioResultado />} />
      </Routes>
    </MemoryRouter>
  );
};

describe('CuestionarioResultado', () => {
  beforeEach(() => jest.clearAllMocks());

  test('shows no result message when no state', () => {
    renderWithState(null);
    expect(screen.getByText('Resultado no disponible')).toBeInTheDocument();
    expect(screen.getByText('Volver al cuestionario')).toBeInTheDocument();
  });

  test('shows result with score and questions', () => {
    renderWithState({
      resultado: {
        puntuacion: 75.5,
        totalPreguntas: 4,
        preguntasCorrectas: 3,
        preguntasIncorrectas: 1,
        preguntas: [
          {
            preguntaId: 1,
            enunciado: 'What is 2+2?',
            acertada: true,
            respuestaUsuario: ['4'],
            respuestaCorrecta: ['4'],
          },
          {
            preguntaId: 2,
            enunciado: 'Capital of Spain?',
            acertada: false,
            respuestaUsuario: ['Barcelona'],
            respuestaCorrecta: ['Madrid'],
          },
        ],
      },
      cuestionarioTitulo: 'General Knowledge',
    });

    expect(screen.getByText('General Knowledge')).toBeInTheDocument();
    expect(screen.getByText('75.5%')).toBeInTheDocument();
    expect(screen.getByText(/Total: 4/)).toBeInTheDocument();
    expect(screen.getByText(/Aciertos: 3/)).toBeInTheDocument();
    expect(screen.getByText(/Fallos: 1/)).toBeInTheDocument();
    expect(screen.getByText(/What is 2\+2\?/)).toBeInTheDocument();
    expect(screen.getByText(/Capital of Spain\?/)).toBeInTheDocument();
    expect(screen.getByText(/Correcta/)).toBeInTheDocument();
    expect(screen.getByText(/Incorrecta/)).toBeInTheDocument();
  });

  test('shows default title when cuestionarioTitulo is missing', () => {
    renderWithState({
      resultado: {
        puntuacion: 50,
        totalPreguntas: 1,
        preguntasCorrectas: 0,
        preguntasIncorrectas: 1,
        preguntas: [],
      },
    });
    expect(screen.getByText('Cuestionario')).toBeInTheDocument();
  });

  test('shows Sin responder when no user answer', () => {
    renderWithState({
      resultado: {
        puntuacion: 0,
        totalPreguntas: 1,
        preguntasCorrectas: 0,
        preguntasIncorrectas: 1,
        preguntas: [
          {
            preguntaId: 1,
            enunciado: 'Q1',
            acertada: false,
            respuestaUsuario: [],
            respuestaCorrecta: ['Answer'],
          },
        ],
      },
      cuestionarioTitulo: 'Test',
    });
    expect(screen.getByText('Sin responder')).toBeInTheDocument();
  });

  test('shows No disponible when no correct answer', () => {
    renderWithState({
      resultado: {
        puntuacion: 0,
        totalPreguntas: 1,
        preguntasCorrectas: 0,
        preguntasIncorrectas: 1,
        preguntas: [
          {
            preguntaId: 1,
            enunciado: 'Q1',
            acertada: false,
            respuestaUsuario: ['X'],
            respuestaCorrecta: [],
          },
        ],
      },
      cuestionarioTitulo: 'Test',
    });
    expect(screen.getByText('No disponible')).toBeInTheDocument();
  });

  test('shows null preguntas gracefully', () => {
    renderWithState({
      resultado: {
        puntuacion: 100,
        totalPreguntas: 0,
        preguntasCorrectas: 0,
        preguntasIncorrectas: 0,
        preguntas: null,
      },
      cuestionarioTitulo: 'Empty',
    });
    expect(screen.getByText('Empty')).toBeInTheDocument();
  });

  test('has Volver al preview and Reintentar buttons', () => {
    renderWithState({
      resultado: {
        puntuacion: 50,
        totalPreguntas: 1,
        preguntasCorrectas: 0,
        preguntasIncorrectas: 1,
        preguntas: [],
      },
      cuestionarioTitulo: 'Test',
    });
    expect(screen.getByText('Volver al preview')).toBeInTheDocument();
    expect(screen.getByText('Reintentar')).toBeInTheDocument();
  });

  test('handles result with zero puntuacion', () => {
    renderWithState({
      resultado: {
        puntuacion: null,
        totalPreguntas: 1,
        preguntasCorrectas: 0,
        preguntasIncorrectas: 1,
        preguntas: [],
      },
    });
    expect(screen.getByText('0.0%')).toBeInTheDocument();
  });
});

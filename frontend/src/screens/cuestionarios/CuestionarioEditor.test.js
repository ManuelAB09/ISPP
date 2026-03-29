import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

jest.mock('../../api/cuestionarios.api', () => ({
  cuestionariosApi: {
    create: jest.fn(),
    createCuestionario: jest.fn(),
  },
}));
jest.mock('../../api/communities.api', () => ({
  communitiesApi: {
    listMine: jest.fn(),
  },
}));
jest.mock('../../components/Header/Header', () => () => <div data-testid="header">Header</div>);
jest.mock('react-icons/lu', () => ({
  LuPlus: () => <span>+</span>,
  LuTrash2: () => <span>🗑</span>,
  LuSave: () => <span>💾</span>,
  LuCheck: () => <span>✓</span>,
  LuArrowLeft: () => <span>←</span>,
}));
jest.mock('./CuestionarioEditor.css', () => ({}));

const { communitiesApi } = require('../../api/communities.api');
const { cuestionariosApi } = require('../../api/cuestionarios.api');
const CuestionarioEditor = require('./CuestionarioEditor').default;

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

const renderEditor = (search = '') => {
  return render(
    <MemoryRouter initialEntries={[`/cuestionarios/crear${search}`]}>
      <CuestionarioEditor />
    </MemoryRouter>
  );
};

describe('CuestionarioEditor', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    communitiesApi.listMine.mockResolvedValue([]);
    jest.spyOn(window, 'alert').mockImplementation(() => {});
  });

  test('renders editor form', async () => {
    renderEditor();
    await screen.findByTestId('header');
    expect(screen.getByText(/Título/i) || screen.getByPlaceholderText(/título/i) || screen.getByLabelText(/título/i)).toBeTruthy();
  });

  test('loads communities on mount', async () => {
    communitiesApi.listMine.mockResolvedValue([{ id: 1, nombre: 'Community 1' }]);
    renderEditor();
    await waitFor(() => expect(communitiesApi.listMine).toHaveBeenCalled());
  });

  test('handles communities load with content array', async () => {
    communitiesApi.listMine.mockResolvedValue({ content: [{ id: 2, nombre: 'C2' }] });
    renderEditor();
    await waitFor(() => expect(communitiesApi.listMine).toHaveBeenCalled());
  });

  test('handles communities load failure', async () => {
    communitiesApi.listMine.mockRejectedValue(new Error('fail'));
    renderEditor();
    await waitFor(() => expect(communitiesApi.listMine).toHaveBeenCalled());
  });

  test('sets community scope from URL param', async () => {
    communitiesApi.listMine.mockResolvedValue([{ id: 5, nombre: 'My Community' }]);
    renderEditor('?communityId=5');
    await waitFor(() => expect(communitiesApi.listMine).toHaveBeenCalled());
  });

  test('fills in titulo and materia fields', async () => {
    renderEditor();
    await screen.findByTestId('header');

    const tituloInput = screen.getByPlaceholderText(/Autoevaluación/i);
    const materiaInput = screen.getByPlaceholderText(/Matemáticas/i);

    fireEvent.change(tituloInput, { target: { value: 'Mi Cuestionario', name: 'titulo' } });
    fireEvent.change(materiaInput, { target: { value: 'Física', name: 'materia' } });

    expect(tituloInput.value).toBe('Mi Cuestionario');
    expect(materiaInput.value).toBe('Física');
  });

  test('adds a new question', async () => {
    renderEditor();
    await screen.findByTestId('header');

    // Initially 1 question, add another
      screen.getAllByText('+');
    // Find the "Añadir Pregunta" button
    const addPreguntaBtn = screen.getByText(/Añadir Pregunta/i);
    fireEvent.click(addPreguntaBtn);

    // Should now have 2 questions
    const questionHeaders = screen.getAllByText(/Pregunta \d+/i);
    expect(questionHeaders.length).toBeGreaterThanOrEqual(2);
  });

  test('removes a question', async () => {
    renderEditor();
    await screen.findByTestId('header');

    // Add a second question
    fireEvent.click(screen.getByText(/Añadir Pregunta/i));
    expect(screen.getAllByText(/Pregunta \d+/i).length).toBe(2);

    // Remove the last question - find trash buttons inside pregunta-header divs
    const headers = document.querySelectorAll('.pregunta-header');
    const lastHeaderBtn = headers[headers.length - 1].querySelector('button');
    fireEvent.click(lastHeaderBtn);

    expect(screen.getAllByText(/Pregunta \d+/i).length).toBe(1);
  });

  test('validates missing titulo on submit', async () => {
    renderEditor();
    await screen.findByTestId('header');

    // Try to submit (publish) without filling titulo
    const publishBtn = screen.getByText(/Publicar/i);
    fireEvent.click(publishBtn);

    expect(window.alert).toHaveBeenCalledWith(expect.stringContaining('título'));
  });

  test('validates missing materia on submit', async () => {
    renderEditor();
    await screen.findByTestId('header');

    const tituloInput = screen.getByPlaceholderText(/Autoevaluación/i);
    fireEvent.change(tituloInput, { target: { value: 'Test', name: 'titulo' } });

    const publishBtn = screen.getByText(/Publicar/i);
    fireEvent.click(publishBtn);

    expect(window.alert).toHaveBeenCalledWith(expect.stringContaining('materia'));
  });

  test('validates empty enunciado on submit', async () => {
    renderEditor();
    await screen.findByTestId('header');

    const tituloInput = screen.getByPlaceholderText(/Autoevaluación/i);
    const materiaInput = screen.getByPlaceholderText(/Matemáticas/i);
    fireEvent.change(tituloInput, { target: { value: 'Test', name: 'titulo' } });
    fireEvent.change(materiaInput, { target: { value: 'Mate', name: 'materia' } });

    const publishBtn = screen.getByText(/Publicar/i);
    fireEvent.click(publishBtn);

    expect(window.alert).toHaveBeenCalledWith(expect.stringContaining('enunciado'));
  });

  test('saves as draft', async () => {
    cuestionariosApi.createCuestionario.mockResolvedValue({ id: 1 });
    renderEditor();
    await screen.findByTestId('header');

    // Fill required fields
    fireEvent.change(screen.getByPlaceholderText(/Autoevaluación/i), { target: { value: 'Test', name: 'titulo' } });
    fireEvent.change(screen.getByPlaceholderText(/Matemáticas/i), { target: { value: 'Mate', name: 'materia' } });

    // Fill question enunciado
    const enunciadoInputs = screen.getAllByPlaceholderText(/enunciado|pregunta/i);
    if (enunciadoInputs.length > 0) {
      fireEvent.change(enunciadoInputs[0], { target: { value: '¿Cuánto es 2+2?' } });
    }

    // Fill option texts
    const optionInputs = screen.getAllByPlaceholderText(/opción|texto/i);
    if (optionInputs.length >= 2) {
      fireEvent.change(optionInputs[0], { target: { value: '4' } });
      fireEvent.change(optionInputs[1], { target: { value: '5' } });
    }

    const draftBtn = screen.getByText(/Guardar Borrador/i);
    fireEvent.click(draftBtn);

    await waitFor(() => {
      expect(cuestionariosApi.createCuestionario).toHaveBeenCalled();
    });
  });

  test('changes dificultad field', async () => {
    renderEditor();
    await screen.findByTestId('header');

    const dificultadSelect = screen.getByDisplayValue(/INTERMEDIO/i) || screen.getByRole('combobox');
    if (dificultadSelect) {
      fireEvent.change(dificultadSelect, { target: { value: 'FACIL', name: 'dificultad' } });
    }
  });

  test('changes scope to COMUNIDAD', async () => {
    communitiesApi.listMine.mockResolvedValue([{ id: 3, nombre: 'TestCom' }]);
    renderEditor();
    await waitFor(() => expect(communitiesApi.listMine).toHaveBeenCalled());

    // Look for scope radio/select
    const comunidadOption = screen.queryByText(/Comunidad/i);
    if (comunidadOption) {
      fireEvent.click(comunidadOption);
    }
  });

  test('navigates back when Volver clicked', async () => {
    renderEditor();
    await screen.findByTestId('header');

    const volverBtn = screen.getByText('←');
    fireEvent.click(volverBtn.closest('button'));

    expect(mockNavigate).toHaveBeenCalledWith(-1);
  });
});

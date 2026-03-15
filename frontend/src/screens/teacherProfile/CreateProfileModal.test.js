import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import CreateProfileModal from './CreateProfileModal';
import * as tutorEndpoints from '../../api/tutorEndpoints';

// Mock de la API de tutores
jest.mock('../../api/tutorEndpoints');

describe('CreateProfileModal', () => {
  let mockOnClose;
  let mockOnCreado;

  beforeEach(() => {
    jest.resetAllMocks();
    mockOnClose = jest.fn();
    mockOnCreado = jest.fn();
  });

  const renderModal = () => {
    return render(
      <CreateProfileModal onClose={mockOnClose} onCreado={mockOnCreado} />
    );
  };

  // ==============================
  // TESTS DE RENDERIZADO
  // ==============================

  test('renderiza el modal con el título correcto', () => {
    renderModal();
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText(/Crear Perfil de Profesor/i)).toBeInTheDocument();
  });

  test('muestra todos los campos del formulario', () => {
    renderModal();
    expect(screen.getByLabelText(/Especialidades/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Tarifa por hora/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Disponibilidad horaria/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Biografía profesional/i)).toBeInTheDocument();
  });

  test('muestra los botones de Cancelar y Crear perfil', () => {
    renderModal();
    expect(screen.getByRole('button', { name: /Cancelar/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Crear perfil/i })).toBeInTheDocument();
  });

  // ==============================
  // TESTS DE INTERACCIÓN
  // ==============================

  test('permite escribir en el campo de especialidades', async () => {
    renderModal();
    const input = screen.getByLabelText(/Especialidades/i);
    await userEvent.type(input, 'Matemáticas, Física');
    expect(input).toHaveValue('Matemáticas, Física');
  });

  test('permite escribir en el campo de tarifa', async () => {
    renderModal();
    const input = screen.getByLabelText(/Tarifa por hora/i);
    await userEvent.type(input, '25');
    expect(input).toHaveValue(25);
  });

  test('permite escribir en el campo de disponibilidad', async () => {
    renderModal();
    const input = screen.getByLabelText(/Disponibilidad horaria/i);
    await userEvent.type(input, 'Tardes y fines de semana');
    expect(input).toHaveValue('Tardes y fines de semana');
  });

  test('permite escribir en el campo de biografía', async () => {
    renderModal();
    const textarea = screen.getByLabelText(/Biografía profesional/i);
    await userEvent.type(textarea, 'Soy un profesor experimentado');
    expect(textarea).toHaveValue('Soy un profesor experimentado');
  });

  test('cierra el modal al hacer clic en Cancelar', async () => {
    renderModal();
    const cancelBtn = screen.getByRole('button', { name: /Cancelar/i });
    await userEvent.click(cancelBtn);
    expect(mockOnClose).toHaveBeenCalledTimes(1);
  });

  test('cierra el modal al hacer clic en el botón de cerrar (X)', async () => {
    renderModal();
    const closeBtn = screen.getByRole('button', { name: /Cerrar/i });
    await userEvent.click(closeBtn);
    expect(mockOnClose).toHaveBeenCalledTimes(1);
  });

  // ==============================
  // TESTS DE ENVÍO DE FORMULARIO
  // ==============================

  test('envía el formulario con los datos correctos', async () => {
    const mockNewTutor = {
      id: 1,
      especialidades: ['Matemáticas', 'Física'],
      tarifaPorHora: 25,
      disponibilidad: 'Tardes',
      biografia: 'Profesor experto',
    };
    tutorEndpoints.createTutorProfile.mockResolvedValue(mockNewTutor);

    renderModal();

    // Rellenar el formulario
    await userEvent.type(screen.getByLabelText(/Especialidades/i), 'Matemáticas, Física');
    await userEvent.type(screen.getByLabelText(/Tarifa por hora/i), '25');
    await userEvent.type(screen.getByLabelText(/Disponibilidad horaria/i), 'Tardes');
    await userEvent.type(screen.getByLabelText(/Biografía profesional/i), 'Profesor experto');

    // Enviar el formulario
    const submitBtn = screen.getByRole('button', { name: /Crear perfil/i });
    await userEvent.click(submitBtn);

    await waitFor(() => {
      expect(tutorEndpoints.createTutorProfile).toHaveBeenCalledWith({
        especialidades: ['Matemáticas', 'Física'],
        tarifaPorHora: 25,
        disponibilidad: 'Tardes',
        biografia: 'Profesor experto',
      });
    });

    await waitFor(() => {
      expect(mockOnCreado).toHaveBeenCalledWith(mockNewTutor);
    });

    expect(mockOnClose).toHaveBeenCalled();
  });

  test('muestra texto "Creando…" mientras se envía el formulario', async () => {
    tutorEndpoints.createTutorProfile.mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve({ id: 1 }), 100))
    );

    renderModal();

    await userEvent.type(screen.getByLabelText(/Especialidades/i), 'Matemáticas');
    await userEvent.type(screen.getByLabelText(/Tarifa por hora/i), '20');
    await userEvent.type(screen.getByLabelText(/Biografía profesional/i), 'Profesor experto');

    const submitBtn = screen.getByRole('button', { name: /Crear perfil/i });
    await userEvent.click(submitBtn);

    expect(screen.getByRole('button', { name: /Creando…/i })).toBeInTheDocument();

    await waitFor(() => {
      expect(mockOnCreado).toHaveBeenCalledWith({ id: 1 });
    });
  });

  test('muestra mensaje de error cuando falla la creación', async () => {
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    tutorEndpoints.createTutorProfile.mockRejectedValue(new Error('Error de red'));

    renderModal();

    await userEvent.type(screen.getByLabelText(/Especialidades/i), 'Matemáticas');
    await userEvent.type(screen.getByLabelText(/Tarifa por hora/i), '20');
    await userEvent.type(screen.getByLabelText(/Biografía profesional/i), 'Profesor experto');

    const submitBtn = screen.getByRole('button', { name: /Crear perfil/i });
    await userEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/No se pudo crear el perfil/i)).toBeInTheDocument();
    });

    expect(mockOnCreado).not.toHaveBeenCalled();
    expect(mockOnClose).not.toHaveBeenCalled();
    consoleSpy.mockRestore();
  });

  // ==============================
  // TESTS DE VALIDACIÓN
  // ==============================

  test('el campo de especialidades es requerido', () => {
    renderModal();
    const input = screen.getByLabelText(/Especialidades/i);
    expect(input).toBeRequired();
  });

  test('el campo de tarifa es requerido', () => {
    renderModal();
    const input = screen.getByLabelText(/Tarifa por hora/i);
    expect(input).toBeRequired();
  });

  test('el campo de tarifa acepta solo números positivos', () => {
    renderModal();
    const input = screen.getByLabelText(/Tarifa por hora/i);
    expect(input).toHaveAttribute('type', 'number');
    expect(input).toHaveAttribute('min', '0');
  });
});

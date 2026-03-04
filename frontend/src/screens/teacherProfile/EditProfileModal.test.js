import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import EditProfileModal from './EditProfileModal';
import * as tutorEndpoints from '../../api/tutorEndpoints';

// Mock de la API de tutores
jest.mock('../../api/tutorEndpoints');

describe('EditProfileModal', () => {
  let mockOnClose;
  let mockOnGuardar;

  const mockTutor = {
    id: 1,
    especialidades: ['Matemáticas', 'Física'],
    tarifaPorHora: 25,
    disponibilidad: 'Tardes y fines de semana',
    biografia: 'Soy un profesor con 5 años de experiencia',
    usuario: {
      id: 100,
      nombre: 'Juan Pérez',
      email: 'juan@test.com',
    },
  };

  beforeEach(() => {
    jest.resetAllMocks();
    mockOnClose = jest.fn();
    mockOnGuardar = jest.fn();
  });

  const renderModal = (tutor = mockTutor) => {
    return render(
      <EditProfileModal
        tutor={tutor}
        onClose={mockOnClose}
        onGuardar={mockOnGuardar}
      />
    );
  };

  // ==============================
  // TESTS DE RENDERIZADO
  // ==============================

  test('renderiza el modal con el título correcto', () => {
    renderModal();
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText(/Editar Perfil/i)).toBeInTheDocument();
  });

  test('muestra los valores actuales del tutor en los campos', () => {
    renderModal();
    expect(screen.getByLabelText(/Especialidades/i)).toHaveValue('Matemáticas, Física');
    expect(screen.getByLabelText(/Tarifa por hora/i)).toHaveValue(25);
    expect(screen.getByLabelText(/Disponibilidad/i)).toHaveValue('Tardes y fines de semana');
    expect(screen.getByLabelText(/Biografía profesional/i)).toHaveValue('Soy un profesor con 5 años de experiencia');
  });

  test('muestra valores vacíos cuando el tutor no tiene datos', () => {
    const tutorVacio = {
      id: 2,
      especialidades: [],
      tarifaPorHora: null,
      disponibilidad: null,
      biografia: null,
    };
    renderModal(tutorVacio);
    expect(screen.getByLabelText(/Especialidades/i)).toHaveValue('');
    expect(screen.getByLabelText(/Tarifa por hora/i)).toHaveValue(null);
  });

  test('muestra los botones de Cancelar y Guardar cambios', () => {
    renderModal();
    expect(screen.getByRole('button', { name: /Cancelar/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Guardar cambios/i })).toBeInTheDocument();
  });

  // ==============================
  // TESTS DE INTERACCIÓN
  // ==============================

  test('permite modificar el campo de especialidades', async () => {
    renderModal();
    const input = screen.getByLabelText(/Especialidades/i);
    await userEvent.clear(input);
    await userEvent.type(input, 'Programación, Inglés');
    expect(input).toHaveValue('Programación, Inglés');
  });

  test('permite modificar el campo de tarifa', async () => {
    renderModal();
    const input = screen.getByLabelText(/Tarifa por hora/i);
    await userEvent.clear(input);
    await userEvent.type(input, '30');
    expect(input).toHaveValue(30);
  });

  test('permite modificar el campo de disponibilidad', async () => {
    renderModal();
    const input = screen.getByLabelText(/Disponibilidad/i);
    await userEvent.clear(input);
    await userEvent.type(input, 'Solo mañanas');
    expect(input).toHaveValue('Solo mañanas');
  });

  test('permite modificar el campo de biografía', async () => {
    renderModal();
    const textarea = screen.getByLabelText(/Biografía profesional/i);
    await userEvent.clear(textarea);
    await userEvent.type(textarea, 'Nueva biografía');
    expect(textarea).toHaveValue('Nueva biografía');
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

  test('envía los cambios correctamente al guardar', async () => {
    const mockUpdatedTutor = {
      ...mockTutor,
      especialidades: ['Programación', 'Bases de Datos'],
      tarifaPorHora: 35,
    };
    tutorEndpoints.updateTutorProfile.mockResolvedValue(mockUpdatedTutor);

    renderModal();

    // Modificar campos
    const especialidadesInput = screen.getByLabelText(/Especialidades/i);
    await userEvent.clear(especialidadesInput);
    await userEvent.type(especialidadesInput, 'Programación, Bases de Datos');

    const tarifaInput = screen.getByLabelText(/Tarifa por hora/i);
    await userEvent.clear(tarifaInput);
    await userEvent.type(tarifaInput, '35');

    // Enviar el formulario
    const submitBtn = screen.getByRole('button', { name: /Guardar cambios/i });
    await userEvent.click(submitBtn);

    await waitFor(() => {
      expect(tutorEndpoints.updateTutorProfile).toHaveBeenCalledWith(mockTutor.id, {
        especialidades: ['Programación', 'Bases de Datos'],
        tarifaPorHora: 35,
        disponibilidad: 'Tardes y fines de semana',
        biografia: 'Soy un profesor con 5 años de experiencia',
      });
    });

    await waitFor(() => {
      expect(mockOnGuardar).toHaveBeenCalledWith(mockUpdatedTutor);
    });

    expect(mockOnClose).toHaveBeenCalled();
  });

  test('muestra texto "Guardando…" mientras se envía el formulario', async () => {
    tutorEndpoints.updateTutorProfile.mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve(mockTutor), 100))
    );

    renderModal();

    const submitBtn = screen.getByRole('button', { name: /Guardar cambios/i });
    await userEvent.click(submitBtn);

    expect(screen.getByRole('button', { name: /Guardando…/i })).toBeInTheDocument();
  });

  test('deshabilita el botón mientras se guarda', async () => {
    tutorEndpoints.updateTutorProfile.mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve(mockTutor), 100))
    );

    renderModal();

    const submitBtn = screen.getByRole('button', { name: /Guardar cambios/i });
    await userEvent.click(submitBtn);

    expect(screen.getByRole('button', { name: /Guardando…/i })).toBeDisabled();
  });

  test('muestra mensaje de error cuando falla el guardado', async () => {
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    tutorEndpoints.updateTutorProfile.mockRejectedValue(new Error('Error de red'));

    renderModal();

    const submitBtn = screen.getByRole('button', { name: /Guardar cambios/i });
    await userEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/No se pudieron guardar los cambios/i)).toBeInTheDocument();
    });

    expect(mockOnGuardar).not.toHaveBeenCalled();
    expect(mockOnClose).not.toHaveBeenCalled();
    consoleSpy.mockRestore();
  });

  // ==============================
  // TESTS DE CONVERSIÓN DE DATOS
  // ==============================

  test('convierte especialidades en array al enviar', async () => {
    tutorEndpoints.updateTutorProfile.mockResolvedValue(mockTutor);

    renderModal();

    // Enviar el formulario sin modificar
    const submitBtn = screen.getByRole('button', { name: /Guardar cambios/i });
    await userEvent.click(submitBtn);

    await waitFor(() => {
      const llamada = tutorEndpoints.updateTutorProfile.mock.calls[0];
      expect(Array.isArray(llamada[1].especialidades)).toBe(true);
    });
  });

  test('convierte tarifa a número al enviar', async () => {
    tutorEndpoints.updateTutorProfile.mockResolvedValue(mockTutor);

    renderModal();

    const submitBtn = screen.getByRole('button', { name: /Guardar cambios/i });
    await userEvent.click(submitBtn);

    await waitFor(() => {
      const llamada = tutorEndpoints.updateTutorProfile.mock.calls[0];
      expect(typeof llamada[1].tarifaPorHora).toBe('number');
    });
  });

  test('elimina espacios en blanco de especialidades', async () => {
    tutorEndpoints.updateTutorProfile.mockResolvedValue(mockTutor);

    renderModal();

    const especialidadesInput = screen.getByLabelText(/Especialidades/i);
    await userEvent.clear(especialidadesInput);
    await userEvent.type(especialidadesInput, '  Java  ,  Python  ');

    const submitBtn = screen.getByRole('button', { name: /Guardar cambios/i });
    await userEvent.click(submitBtn);

    await waitFor(() => {
      const llamada = tutorEndpoints.updateTutorProfile.mock.calls[0];
      expect(llamada[1].especialidades).toEqual(['Java', 'Python']);
    });
  });
});

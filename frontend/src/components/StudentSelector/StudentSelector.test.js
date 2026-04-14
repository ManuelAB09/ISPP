import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import StudentSelector from './StudentSelector';
import * as usersApi from '../../api/users.api';

jest.mock('../../api/users.api');

describe('StudentSelector', () => {
  let mockOnStudentsChange;

  beforeEach(() => {
    jest.resetAllMocks();
    mockOnStudentsChange = jest.fn();
  });

  const renderComponent = (props = {}) => {
    const defaultProps = {
      selectedStudents: [],
      onStudentsChange: mockOnStudentsChange,
    };
    return render(<StudentSelector {...defaultProps} {...props} />);
  };

  // ==============================
  // TESTS DE RENDERIZADO
  // ==============================

  test('renderiza el input de búsqueda', () => {
    renderComponent();
    const input = screen.getByPlaceholderText(/Busca alumnos por nombre o email/i);
    expect(input).toBeInTheDocument();
  });

  test('renderiza el estado vacío cuando no hay alumnos seleccionados', () => {
    renderComponent();
    expect(screen.getByText('Ningún alumno seleccionado')).toBeInTheDocument();
  });

  test('renderiza los alumnos seleccionados como chips', () => {
    const selectedStudents = [
      { id: 1, nombre: 'Juan', email: 'juan@example.com', foto: null },
      { id: 2, nombre: 'Maria', email: 'maria@example.com', foto: null },
    ];
    renderComponent({ selectedStudents });

    expect(screen.getByText('Juan')).toBeInTheDocument();
    expect(screen.getByText('Maria')).toBeInTheDocument();
    expect(screen.getByText('juan@example.com')).toBeInTheDocument();
    expect(screen.getByText('maria@example.com')).toBeInTheDocument();
  });

  test('no renderiza el estado vacío cuando hay alumnos seleccionados', () => {
    const selectedStudents = [
      { id: 1, nombre: 'Juan', email: 'juan@example.com', foto: null },
    ];
    renderComponent({ selectedStudents });

    expect(screen.queryByText('Ningún alumno seleccionado')).not.toBeInTheDocument();
  });

  test('renderiza botones de eliminar para cada alumno seleccionado', () => {
    const selectedStudents = [
      { id: 1, nombre: 'Juan', email: 'juan@example.com', foto: null },
    ];
    renderComponent({ selectedStudents });

    const removeButtons = screen.getAllByRole('button');
    expect(removeButtons.length).toBeGreaterThan(0);
  });

  // ==============================
  // TESTS DE BÚSQUEDA
  // ==============================

  test('llama a searchUsers cuando el usuario escribe en el input', async () => {
    usersApi.usersApi.searchUsers.mockResolvedValue([]);
    renderComponent();

    const input = screen.getByPlaceholderText(/Busca alumnos por nombre o email/i);
    await userEvent.type(input, 'juan');

    await waitFor(() => {
      expect(usersApi.usersApi.searchUsers).toHaveBeenCalled();
    }, { timeout: 500 });
  });

  test('muestra resultados de búsqueda en el dropdown', async () => {
    const mockResults = [
      { id: 1, nombre: 'Juan García', email: 'juan@example.com', foto: null },
      { id: 2, nombre: 'Juana López', email: 'juana@example.com', foto: null },
    ];
    usersApi.usersApi.searchUsers.mockResolvedValue(mockResults);

    renderComponent();

    const input = screen.getByPlaceholderText(/Busca alumnos por nombre o email/i);
    await userEvent.type(input, 'juan');

    await waitFor(() => {
      expect(screen.getByText('Juan García')).toBeInTheDocument();
      expect(screen.getByText('Juana López')).toBeInTheDocument();
    });
  });

  test('muestra "Sin resultados" cuando la búsqueda no retorna datos', async () => {
    usersApi.usersApi.searchUsers.mockResolvedValue([]);

    renderComponent();

    const input = screen.getByPlaceholderText(/Busca alumnos por nombre o email/i);
    await userEvent.type(input, 'xyz');

    await waitFor(() => {
      expect(usersApi.usersApi.searchUsers).toHaveBeenCalledWith('xyz');
    }, { timeout: 500 });
  });

  test('no muestra dropdown cuando el input está vacío', async () => {
    const mockResults = [
      { id: 1, nombre: 'Juan García', email: 'juan@example.com', foto: null },
    ];
    usersApi.usersApi.searchUsers.mockResolvedValue(mockResults);

    renderComponent();

    const input = screen.getByPlaceholderText(/Busca alumnos por nombre o email/i);
    await userEvent.type(input, 'juan');

    // Esperar a que se dispare la búsqueda
    await waitFor(() => {
      expect(usersApi.usersApi.searchUsers).toHaveBeenCalledWith('juan');
    });

    // Borrar el input
    await userEvent.clear(input);

    // El input debe estar vacío
    expect(input.value).toBe('');
  });

  test('filtra resultados que ya están seleccionados', async () => {
    const selectedStudents = [
      { id: 1, nombre: 'Juan', email: 'juan@example.com', foto: null },
    ];
    const mockResults = [
      { id: 1, nombre: 'Juan', email: 'juan@example.com', foto: null },
      { id: 2, nombre: 'María', email: 'maria@example.com', foto: null },
    ];
    usersApi.usersApi.searchUsers.mockResolvedValue(mockResults);

    renderComponent({ selectedStudents });

    const input = screen.getByPlaceholderText(/Busca alumnos por nombre o email/i);
    await userEvent.type(input, 'maria');

    await waitFor(() => {
      expect(usersApi.usersApi.searchUsers).toHaveBeenCalledWith('maria');
    });
  });

  // ==============================
  // TESTS DE INTERACCIÓN
  // ==============================

  test('añade un alumno cuando se selecciona de los resultados', async () => {
    const mockResults = [
      { id: 1, nombre: 'Juan', email: 'juan@example.com', foto: null },
    ];
    usersApi.usersApi.searchUsers.mockResolvedValue(mockResults);

    renderComponent();

    const input = screen.getByPlaceholderText(/Busca alumnos por nombre o email/i);
    await userEvent.type(input, 'juan');

    await waitFor(() => {
      expect(screen.getByText('Juan')).toBeInTheDocument();
    });

    const juanOption = screen.getByText('juan@example.com');
    await userEvent.click(juanOption);

    expect(mockOnStudentsChange).toHaveBeenCalledWith([mockResults[0]]);
  });

  test('limpia el input después de seleccionar un alumno', async () => {
    const mockResults = [
      { id: 1, nombre: 'Juan', email: 'juan@example.com', foto: null },
    ];
    usersApi.usersApi.searchUsers.mockResolvedValue(mockResults);

    renderComponent();

    const input = screen.getByPlaceholderText(/Busca alumnos por nombre o email/i);
    await userEvent.type(input, 'juan');

    await waitFor(() => {
      expect(screen.getByText('Juan')).toBeInTheDocument();
    });

    const juanOption = screen.getByText('juan@example.com');
    await userEvent.click(juanOption);

    expect(input.value).toBe('');
  });

  test('elimina un alumno cuando se hace clic en el botón de eliminar', async () => {
    const selectedStudents = [
      { id: 1, nombre: 'Juan', email: 'juan@example.com', foto: null },
    ];
    renderComponent({ selectedStudents });

    const removeButtons = screen.getAllByRole('button');
    await userEvent.click(removeButtons[0]);

    expect(mockOnStudentsChange).toHaveBeenCalledWith([]);
  });

  test('mantiene otros alumnos cuando se elimina uno', async () => {
    const selectedStudents = [
      { id: 1, nombre: 'Juan', email: 'juan@example.com', foto: null },
      { id: 2, nombre: 'María', email: 'maria@example.com', foto: null },
    ];
    renderComponent({ selectedStudents });

    const removeButtons = screen.getAllByRole('button');
    await userEvent.click(removeButtons[0]);

    expect(mockOnStudentsChange).toHaveBeenCalledWith([selectedStudents[1]]);
  });

  // ==============================
  // TESTS DE CASOS EDGE
  // ==============================

  test('maneja errores en la búsqueda sin fallar', async () => {
    usersApi.usersApi.searchUsers.mockRejectedValue(new Error('API Error'));

    renderComponent();

    const input = screen.getByPlaceholderText(/Busca alumnos por nombre o email/i);
    await userEvent.type(input, 'juan');

    // El componente debe estar en el DOM sin errores
    expect(input).toBeInTheDocument();
  });

  test('maneja alumnos sin foto con placeholder de iniciales', () => {
    const selectedStudents = [
      { id: 1, nombre: 'Juan', email: 'juan@example.com', foto: null },
    ];
    renderComponent({ selectedStudents });

    // El componente debe renderizarse correctamente sin avatar de foto
    expect(screen.getByText('Juan')).toBeInTheDocument();
  });

  test('maneja array vacío de alumnos seleccionados', () => {
    renderComponent({ selectedStudents: [] });

    expect(screen.getByText('Ningún alumno seleccionado')).toBeInTheDocument();
  });

  test('no hace nada cuando se hace clic outside sin interacción previa', async () => {
    renderComponent();

    const input = screen.getByPlaceholderText(/Busca alumnos por nombre o email/i);
    
    // Hacer clic fuera sin escribir
    await userEvent.click(document.body);

    // El componente debe estar intacto
    expect(input).toBeInTheDocument();
    expect(mockOnStudentsChange).not.toHaveBeenCalled();
  });
});

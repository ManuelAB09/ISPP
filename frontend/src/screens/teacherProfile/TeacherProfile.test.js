import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import TeacherProfile from './TeacherProfile';
import * as tutorEndpoints from '../../api/tutorEndpoints';
import { useAuth } from '../../contexts/AuthContext';

// Increase default timeout for all tests in this file
jest.setTimeout(15000);

// Mocks
jest.mock('../../api/tutorEndpoints');
jest.mock('../../contexts/AuthContext');
jest.mock('../../components/Header/Header', () => {
  return function MockHeader() {
    return <div data-testid="mock-header">Header</div>;
  };
});
jest.mock('../myProfile/Settings', () => {
  return function MockSettings({ onClose }) {
    return (
      <div data-testid="settings-modal">
        <button onClick={onClose}>Cerrar Settings</button>
      </div>
    );
  };
});
jest.mock('./EditProfileModal', () => {
  return function MockEditModal({ onClose, onGuardar }) {
    return (
      <div data-testid="edit-modal">
        <button onClick={() => onGuardar({ especialidades: ['Actualizado'] })}>
          Mock Guardar
        </button>
        <button onClick={onClose}>Mock Cerrar</button>
      </div>
    );
  };
});
jest.mock('./CreateProfileModal', () => {
  return function MockCreateModal({ onClose, onCreado }) {
    return (
      <div data-testid="create-modal">
        <button onClick={() => onCreado({ id: 999 })}>Mock Crear</button>
        <button onClick={onClose}>Mock Cerrar</button>
      </div>
    );
  };
});
jest.mock('./VerificacionModal', () => {
  return function MockVerificacionModal({ onClose, onVerificado }) {
    return (
      <div data-testid="verificacion-modal">
        <button onClick={onVerificado}>Mock Verificar</button>
        <button onClick={onClose}>Mock Cerrar</button>
      </div>
    );
  };
});

describe('TeacherProfile', () => {
  const mockTutor = {
    id: 1,
    usuario: {
      id: 100,
      nombre: 'Juan Pérez García',
      foto: 'https://example.com/foto.jpg',
      bio: 'Profesor experimentado en ciencias',
    },
    especialidades: ['Matemáticas', 'Física', 'Química'],
    tarifaPorHora: 30,
    disponibilidad: 'Tardes y fines de semana',
    biografia: 'Profesor con 10 años de experiencia',
    verificado: true,
    actividad: {
      comunidades: 5,
      apuntes: 12,
      valoracion: 4.8,
      descargas: 1500,
    },
    opiniones: [{ id: 1 }, { id: 2 }, { id: 3 }],
    comunidades: [
      { nombre: 'Matemáticas Avanzadas', descripcion: 'Grupo de estudio' },
    ],
    comunidadesCreadas: [],
  };

  beforeEach(() => {
    jest.clearAllMocks();
    useAuth.mockReturnValue({
      user: { id: 100, esTutor: true },
    });
    tutorEndpoints.getTutorById.mockResolvedValue(mockTutor);
    tutorEndpoints.getMyTutorProfiles.mockResolvedValue(null);
  });

  const renderWithId = (id = '1') => {
    return render(
      <MemoryRouter initialEntries={[`/profesores/${id}`]}>
        <Routes>
          <Route path="/profesores/:id" element={<TeacherProfile />} />
        </Routes>
      </MemoryRouter>
    );
  };

  // ==============================
  // TESTS DE RENDERIZADO DEL PERFIL
  // ==============================

  test('muestra loading mientras carga el perfil', () => {
    tutorEndpoints.getTutorById.mockImplementation(
      () => new Promise(() => {}) // Never resolves
    );
    renderWithId();
    expect(screen.getByText(/Cargando perfil/i)).toBeInTheDocument();
  });

  test('muestra el nombre del profesor', async () => {
    renderWithId();
    // El nombre aparece en varios lugares, usamos findAllByText
    const nombres = await screen.findAllByText('Juan Pérez García', {}, { timeout: 10000 });
    expect(nombres.length).toBeGreaterThan(0);
  });

  test('muestra la foto del profesor', async () => {
    renderWithId();
    const img = await screen.findByAltText('Juan Pérez García');
    expect(img).toBeInTheDocument();
    expect(img).toHaveAttribute('src', 'https://example.com/foto.jpg');
  });

  test('muestra las especialidades del profesor', async () => {
    renderWithId();
    await waitFor(() => {
      expect(screen.getByText(/Profesor de Matemáticas, Física, Química/i)).toBeInTheDocument();
    });
  });

  test('muestra la insignia de verificado cuando está verificado', async () => {
    renderWithId();
    await waitFor(() => {
      expect(screen.getByTitle('Tutor Verificado')).toBeInTheDocument();
    });
  });

  test('no muestra insignia de verificado cuando no está verificado', async () => {
    tutorEndpoints.getTutorById.mockResolvedValue({
      ...mockTutor,
      verificado: false,
    });
    renderWithId();
    // Esperar a que se cargue el perfil (el nombre aparece varias veces)
    await screen.findAllByText('Juan Pérez García', {}, { timeout: 10000 });
    // Una vez cargado, verificamos que NO hay insignia de verificado
    expect(screen.queryByTitle('Tutor Verificado')).not.toBeInTheDocument();
  });

  test('muestra la valoración con estrellas', async () => {
    renderWithId();
    // Esperar a que se cargue el perfil
    await screen.findAllByText('Juan Pérez García', {}, { timeout: 10000 });
    // Verificar la valoración (usar getAllByText porque puede haber múltiples)
    const valoraciones = screen.getAllByText('4.8');
    expect(valoraciones.length).toBeGreaterThan(0);
    expect(screen.getByText('(3 reseñas)')).toBeInTheDocument();
  });

  test('muestra el badge de Profesor', async () => {
    renderWithId();
    await waitFor(() => {
      expect(screen.getByText('Profesor')).toBeInTheDocument();
    });
  });

  // ==============================
  // TESTS DE DATOS DEL PERFIL
  // ==============================

  test('muestra la sección Mis datos', async () => {
    renderWithId();
    await waitFor(() => {
      expect(screen.getByText('Mis datos')).toBeInTheDocument();
    });
  });

  test('muestra el nombre completo en los datos', async () => {
    renderWithId();
    await waitFor(() => {
      expect(screen.getByText('NOMBRE COMPLETO')).toBeInTheDocument();
    });
  });

  test('muestra la bio del profesor', async () => {
    renderWithId();
    await waitFor(() => {
      expect(screen.getByText('BIO')).toBeInTheDocument();
    });
  });

  test('muestra el estado de verificación en los datos', async () => {
    renderWithId();
    expect(await screen.findByText('VERIFICADO')).toBeInTheDocument();
    expect(screen.getByText('Sí ✓')).toBeInTheDocument();
  });

  test('muestra la tarifa por hora', async () => {
    renderWithId();
    expect(await screen.findByText('TARIFA POR HORA')).toBeInTheDocument();
    expect(screen.getByText('30€ / h')).toBeInTheDocument();
  });

  // ==============================
  // TESTS DE ACTIVIDAD
  // ==============================

  test('muestra la sección Tu Actividad', async () => {
    renderWithId();
    await waitFor(() => {
      expect(screen.getByText('Tu Actividad')).toBeInTheDocument();
    });
  });

  test('muestra el número de comunidades', async () => {
    renderWithId();
    expect(await screen.findByText('COMUNIDADES')).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument();
  });

  test('muestra el número de apuntes subidos', async () => {
    renderWithId();
    expect(await screen.findByText('APUNTES SUBIDOS')).toBeInTheDocument();
    expect(screen.getByText('12')).toBeInTheDocument();
  });

  test('muestra las descargas formateadas (1.5k)', async () => {
    renderWithId();
    expect(await screen.findByText('DESCARGAS')).toBeInTheDocument();
    expect(screen.getByText('1.5k')).toBeInTheDocument();
  });

  // ==============================
  // TESTS DE ACCIONES DEL PROPIETARIO
  // ==============================

  test('muestra botón Editar Perfil para el propietario', async () => {
    renderWithId();
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Editar Perfil/i })).toBeInTheDocument();
    });
  });

  test('muestra botón Promocionarse/Verificado para el propietario', async () => {
    renderWithId();
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Verificado/i })).toBeInTheDocument();
    });
  });

  test('muestra botón Configuración para el propietario', async () => {
    renderWithId();
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Configuración/i })).toBeInTheDocument();
    });
  });

  test('no muestra botones de acción para otros usuarios', async () => {
    useAuth.mockReturnValue({
      user: { id: 999, esTutor: false }, // Usuario diferente
    });
    renderWithId();
    // Esperar a que se cargue el perfil (el nombre aparece varias veces)
    await screen.findAllByText('Juan Pérez García', {}, { timeout: 10000 });
    // Verificamos que los botones de acción NO están presentes
    expect(screen.queryByRole('button', { name: /Editar Perfil/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Promocionarse/i })).not.toBeInTheDocument();
  });

  // ==============================
  // TESTS DE MODALES
  // ==============================

  test('abre el modal de edición al hacer clic en Editar Perfil', async () => {
    renderWithId();
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Editar Perfil/i })).toBeInTheDocument();
    });

    const editBtn = screen.getByRole('button', { name: /Editar Perfil/i });
    await userEvent.click(editBtn);

    expect(screen.getByTestId('edit-modal')).toBeInTheDocument();
  });

  test('abre el modal de verificación al hacer clic en Promocionarse', async () => {
    tutorEndpoints.getTutorById.mockResolvedValue({
      ...mockTutor,
      verificado: false,
    });
    renderWithId();
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Promocionarse/i })).toBeInTheDocument();
    });

    const promoBtn = screen.getByRole('button', { name: /Promocionarse/i });
    await userEvent.click(promoBtn);

    expect(screen.getByTestId('verificacion-modal')).toBeInTheDocument();
  });

  // ==============================
  // TESTS DE ERROR
  // ==============================

  test('muestra mensaje de error cuando falla la carga', async () => {
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    tutorEndpoints.getTutorById.mockRejectedValue(new Error('Error de red'));
    renderWithId();
    await waitFor(() => {
      expect(screen.getByText(/No se pudo cargar el perfil del tutor/i)).toBeInTheDocument();
    });
    consoleSpy.mockRestore();
  });

  // ==============================
  // TESTS DE VISTA DE CREACIÓN (NUEVO)
  // ==============================

  test('muestra vista de creación para /profesores/nuevo', async () => {
    useAuth.mockReturnValue({
      user: { id: 100, esTutor: true },
    });
    render(
      <MemoryRouter initialEntries={['/profesores/nuevo']}>
        <Routes>
          <Route path="/profesores/:id" element={<TeacherProfile />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText(/Aún no has creado tu perfil profesional/i)).toBeInTheDocument();
  });

  test('muestra botón Crear Perfil de Profesor en vista de creación', async () => {
    useAuth.mockReturnValue({
      user: { id: 100, esTutor: true },
    });
    render(
      <MemoryRouter initialEntries={['/profesores/nuevo']}>
        <Routes>
          <Route path="/profesores/:id" element={<TeacherProfile />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getAllByRole('button', { name: /Crear Perfil de Profesor/i })).toHaveLength(2);
  });

  test('muestra mensaje informativo en vista de creación', async () => {
    render(
      <MemoryRouter initialEntries={['/profesores/nuevo']}>
        <Routes>
          <Route path="/profesores/:id" element={<TeacherProfile />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText(/Añade tus especialidades/i)).toBeInTheDocument();
  });

  // ==============================
  // TESTS DE SECCIONES ADICIONALES
  // ==============================

  test('muestra la sección Mis comunidades', async () => {
    renderWithId();
    await waitFor(() => {
      expect(screen.getByText('Mis comunidades')).toBeInTheDocument();
    });
  });

  test('muestra la sección Comunidades creadas', async () => {
    renderWithId();
    await waitFor(() => {
      expect(screen.getByText('Comunidades creadas')).toBeInTheDocument();
    });
  });

  test('muestra el enlace Explorar más comunidades', async () => {
    renderWithId();
    await waitFor(() => {
      expect(screen.getByText('Explorar más comunidades')).toBeInTheDocument();
    });
  });

  // ==============================
  // TESTS DE COMPONENTE ESTRELLAS
  // ==============================

  test('renderiza las estrellas de valoración', async () => {
    renderWithId();
    await waitFor(() => {
      const stars = screen.getAllByText('★');
      expect(stars.length).toBeGreaterThan(0);
    });
  });
});

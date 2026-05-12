import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { communitiesApi } from '../../api/communities.api';
import { subscriptionsApi } from '../../api/subscriptions.api';
import { getMyTutorProfiles } from '../../api/tutorEndpoints';
import CrearComunidad from './CrearComunidad';

// Mocks
jest.mock('../../api/communities.api');
jest.mock('../../api/subscriptions.api');
jest.mock('../../api/tutorEndpoints', () => ({
  getMyTutorProfiles: jest.fn(),
}));
jest.mock('../../contexts/AuthContext', () => ({
  useAuth: () => ({
    user: {
      id: 1,
      esTutor: true,
      esProfesor: true,
    },
  }),
}));
jest.mock('../../components/Header/Header', () => {
  return function MockHeader() {
    return <div data-testid="mock-header">Header</div>;
  };
});

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

describe('CrearComunidad', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    communitiesApi.create.mockResolvedValue({ id: 123 });
    communitiesApi.listMine.mockResolvedValue({ content: [], page: { totalElements: 0 } });
    subscriptionsApi.getMySubscription.mockResolvedValue({ plan: 'FREE', activa: true });
    getMyTutorProfiles.mockResolvedValue({
      especialidades: ['Matematicas'],
      tarifaPorHora: 15,
      biografia: 'Tutor de prueba',
    });
  });

  const renderComponent = () => {
    return render(
      <MemoryRouter>
        <CrearComunidad />
      </MemoryRouter>
    );
  };

  test('renderiza el título de la página', () => {
    renderComponent();
    expect(screen.getByRole('heading', { name: /Crear Comunidad/i })).toBeInTheDocument();
  });

  test('renderiza el Header', () => {
    renderComponent();
    expect(screen.getByTestId('mock-header')).toBeInTheDocument();
  });

  test('renderiza el campo de nombre', () => {
    renderComponent();
    expect(screen.getByLabelText(/Nombre de la Comunidad/i)).toBeInTheDocument();
  });

  test('renderiza el campo de descripción', () => {
    renderComponent();
    expect(screen.getByLabelText(/Descripción/i)).toBeInTheDocument();
  });

  test('renderiza opciones de tipo de comunidad', () => {
    renderComponent();
    expect(screen.getByText(/Pública \(acceso libre\)/i)).toBeInTheDocument();
    expect(screen.getByText(/Privada \(requiere solicitud\)/i)).toBeInTheDocument();
  });

  test('comunidad pública está seleccionada por defecto', () => {
    renderComponent();
    const publicOption = screen.getByDisplayValue('COMUNIDAD_PUBLICA');
    expect(publicOption).toBeChecked();
  });

  test('puede cambiar a comunidad privada', () => {
    renderComponent();

    const privateOption = screen.getByDisplayValue('GRUPO_PRIVADO');
    userEvent.click(privateOption);

    expect(privateOption).toBeChecked();
  });

  test('el botón de crear está deshabilitado sin nombre', () => {
    renderComponent();
    const createButton = screen.getByRole('button', { name: /Crear Comunidad/i });
    expect(createButton).toBeDisabled();
  });

  test('muestra error si el nombre tiene menos de 3 caracteres', async () => {
    renderComponent();

    const nameInput = screen.getByLabelText(/Nombre de la Comunidad/i);
    userEvent.type(nameInput, 'AB');

    const createButton = screen.getByRole('button', { name: /Crear Comunidad/i });
    userEvent.click(createButton);

    await screen.findByText(/El nombre debe tener al menos 3 caracteres/i);
  });

  test('muestra error si el nombre excede 100 caracteres', async () => {
    renderComponent();

    const nameInput = screen.getByLabelText(/Nombre de la Comunidad/i);
    const longName = 'A'.repeat(101);
    userEvent.type(nameInput, longName);

    const createButton = screen.getByRole('button', { name: /Crear Comunidad/i });
    userEvent.click(createButton);

    await screen.findByText(/El nombre no puede exceder 100 caracteres/i);
  });

  test('puede agregar categorías', async () => {
    renderComponent();

    const categoriaInput = screen.getByPlaceholderText(/Agregar categoría/i);
    userEvent.type(categoriaInput, 'Matemáticas');

    const addButton = screen.getByRole('button', { name: /\+/ });
    userEvent.click(addButton);

    await screen.findByText('Matemáticas');
  });

  test('puede eliminar categorías', async () => {
    renderComponent();

    // Agregar categoría
    const categoriaInput = screen.getByPlaceholderText(/Agregar categoría/i);
    userEvent.type(categoriaInput, 'Historia');
    const addButton = screen.getByRole('button', { name: /\+/ });
    userEvent.click(addButton);

    await screen.findByText('Historia');

    // Eliminar categoría
    const deleteButton = screen.getByRole('button', { name: /×/ });
    userEvent.click(deleteButton);

    await waitFor(() => {
      expect(screen.queryByText('Historia')).not.toBeInTheDocument();
    });
  });

  test('no agrega categorías duplicadas', async () => {
    renderComponent();

    const categoriaInput = screen.getByPlaceholderText(/Agregar categoría/i);
    const addButton = screen.getByRole('button', { name: /\+/ });

    // Agregar la primera vez
    userEvent.type(categoriaInput, 'Física');
    userEvent.click(addButton);

    await screen.findByText('Física');

    // Intentar agregar de nuevo
    userEvent.type(categoriaInput, 'Física');
    userEvent.click(addButton);

    // Solo debería haber una
    const fisicaElements = screen.getAllByText('Física');
    expect(fisicaElements).toHaveLength(1);
  });

  test('crea comunidad exitosamente con datos válidos', async () => {
    renderComponent();

    const nameInput = screen.getByLabelText(/Nombre de la Comunidad/i);
    userEvent.type(nameInput, 'Mi Nueva Comunidad');

    const descInput = screen.getByLabelText(/Descripción/i);
    userEvent.type(descInput, 'Una descripción de prueba');

    const createButton = screen.getByRole('button', { name: /Crear Comunidad/i });
    userEvent.click(createButton);

    await waitFor(() => {
      expect(communitiesApi.create).toHaveBeenCalledWith({
        nombre: 'Mi Nueva Comunidad',
        descripcion: 'Una descripción de prueba',
        tipoGrupo: 'COMUNIDAD_PUBLICA',
        imagenUrl: null,
        maxMiembros: 30,
        rolInicial: 'ALUMNO',
      });
    });
  });

  test('muestra mensaje de éxito después de crear', async () => {
    renderComponent();

    const nameInput = screen.getByLabelText(/Nombre de la Comunidad/i);
    userEvent.type(nameInput, 'Mi Nueva Comunidad');

    const createButton = screen.getByRole('button', { name: /Crear Comunidad/i });
    userEvent.click(createButton);

    await screen.findByText(/Comunidad creada con éxito/i);
  });

  test('muestra estado de carga mientras se crea', async () => {
    let resolveCreate;
    communitiesApi.create.mockImplementation(
      () => new Promise((resolve) => { resolveCreate = resolve; })
    );
    renderComponent();

    const nameInput = screen.getByLabelText(/Nombre de la Comunidad/i);
    userEvent.type(nameInput, 'Test Comunidad');

    const createButton = screen.getByRole('button', { name: /Crear Comunidad/i });
    userEvent.click(createButton);

    await screen.findByRole('button', { name: /Creando/i });

    resolveCreate({ id: 1 });
  });

  test('muestra error cuando falla la creación', async () => {
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    communitiesApi.create.mockRejectedValue({
      response: { data: { message: 'Error del servidor' } },
    });

    renderComponent();

    const nameInput = screen.getByLabelText(/Nombre de la Comunidad/i);
    userEvent.type(nameInput, 'Test Comunidad');

    const createButton = screen.getByRole('button', { name: /Crear Comunidad/i });
    userEvent.click(createButton);

    await screen.findByText(/No se pudo crear la comunidad. Intenta de nuevo./i);
    consoleSpy.mockRestore();
  });

  test('renderiza la sección de subida de imagen', () => {
    renderComponent();
    expect(screen.getByText(/Subir imagen de portada/i)).toBeInTheDocument();
  });

  test('renderiza el botón de guardar borrador', () => {
    renderComponent();
    expect(screen.getByRole('button', { name: /Guardar Borrador/i })).toBeInTheDocument();
  });

  test('muestra información sobre capacidad según plan', () => {
    renderComponent();
    expect(screen.getByText(/Máx\. miembros de la comunidad/i)).toBeInTheDocument();
  });

  test('ajusta el máximo del slider según plan Pro', async () => {
    subscriptionsApi.getMySubscription.mockResolvedValue({ plan: 'PRO', activa: true });
    renderComponent();

    const slider = await screen.findByLabelText(/Máx\. miembros de la comunidad/i);
    await waitFor(() => {
      expect(slider).toHaveAttribute('max', '250');
    });
  });

  test('muestra acceso al flujo de planes institucionales', () => {
    renderComponent();
    expect(screen.getByRole('button', { name: /Ir a planes institucionales/i })).toBeInTheDocument();
  });

  // ==============================
  // TESTS DE BORRADOR
  // ==============================

  test('guarda borrador al hacer clic en Guardar Borrador', async () => {
    const setItemSpy = jest.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {});
    renderComponent();

    const nameInput = screen.getByLabelText(/Nombre de la Comunidad/i);
    userEvent.type(nameInput, 'Mi Borrador');

    const draftBtn = screen.getByRole('button', { name: /Guardar Borrador/i });
    userEvent.click(draftBtn);

    await screen.findByText(/Borrador guardado/i);
    expect(setItemSpy).toHaveBeenCalledWith('crearComunidadDraft', expect.any(String));
    setItemSpy.mockRestore();
  });

  test('carga borrador desde localStorage al montar', async () => {
    const draft = JSON.stringify({ nombre: 'Borrador Previo', descripcion: 'Desc guardada', tipoComunidad: 'GRUPO_PRIVADO', maxMiembros: 20, categorias: ['Cat1'] });
    jest.spyOn(Storage.prototype, 'getItem').mockImplementation((key) => {
      if (key === 'crearComunidadDraft') return draft;
      return null;
    });
    renderComponent();

    await waitFor(() => {
      expect(screen.getByLabelText(/Nombre de la Comunidad/i)).toHaveValue('Borrador Previo');
    });
    expect(screen.getByLabelText(/Descripción/i)).toHaveValue('Desc guardada');
    Storage.prototype.getItem.mockRestore();
  });

  // ==============================
  // TESTS DE ROL
  // ==============================

  test('puede seleccionar rol Profesor', () => {
    renderComponent();
    const profesorRadio = screen.getByDisplayValue('PROFESOR');
    userEvent.click(profesorRadio);
    expect(profesorRadio).toBeChecked();
  });

  test('muestra error si perfil tutor incompleto al crear como profesor', async () => {
    getMyTutorProfiles.mockResolvedValue({ especialidades: [], tarifaPorHora: null, biografia: '' });
    renderComponent();

    const nameInput = screen.getByLabelText(/Nombre de la Comunidad/i);
    userEvent.type(nameInput, 'Comunidad Profesor');

    const profesorRadio = screen.getByDisplayValue('PROFESOR');
    userEvent.click(profesorRadio);

    const createButton = screen.getByRole('button', { name: /Crear Comunidad/i });
    userEvent.click(createButton);

    await screen.findByText(/perfil de tutor configurado/i);
  });

  test('muestra enlace a configurar perfil cuando tutor requerido', async () => {
    getMyTutorProfiles.mockRejectedValue(new Error('not found'));
    renderComponent();

    const nameInput = screen.getByLabelText(/Nombre de la Comunidad/i);
    userEvent.type(nameInput, 'Comunidad Prof');

    userEvent.click(screen.getByDisplayValue('PROFESOR'));
    userEvent.click(screen.getByRole('button', { name: /Crear Comunidad/i }));

    await screen.findByText(/Ir a configurar mi perfil de profesor/i);
  });

  // ==============================
  // TESTS DE LÍMITES
  // ==============================

  test('muestra error si se alcanza límite de comunidades', async () => {
    communitiesApi.listMine.mockResolvedValue({
      content: Array(3).fill({ id: 1 }),
      page: { totalElements: 3, totalPages: 1 },
    });
    renderComponent();

    await waitFor(() => {
      const createButton = screen.getByRole('button', { name: /Crear Comunidad/i });
      expect(createButton).toBeDisabled();
    });
  });

  test('slider de miembros con plan Premium tiene máximo 75', async () => {
    subscriptionsApi.getMySubscription.mockResolvedValue({ plan: 'PREMIUM', activa: true });
    renderComponent();

    const slider = await screen.findByLabelText(/Máx\. miembros de la comunidad/i);
    await waitFor(() => {
      expect(slider).toHaveAttribute('max', '75');
    });
  });

  // ==============================
  // TESTS DE IMAGEN
  // ==============================

  test('muestra preview de imagen al subir archivo', async () => {
    renderComponent();
    const fileInput = document.querySelector('input[type="file"]');
    const file = new File(['img'], 'foto.png', { type: 'image/png' });

    // Mock FileReader
    const mockReader = { readAsDataURL: jest.fn(), onloadend: null, result: 'data:image/png;base64,abc' };
    jest.spyOn(window, 'FileReader').mockImplementation(() => mockReader);

    userEvent.upload(fileInput, file);

    // Trigger onloadend callback
    mockReader.onloadend();

    await waitFor(() => {
      expect(screen.getByAltText('Preview')).toBeInTheDocument();
    });
    window.FileReader.mockRestore();
  });

  test('muestra error al subir archivo con formato no válido', async () => {
    renderComponent();
    const fileInput = document.querySelector('input[type="file"]');
    const file = new File(['audio'], 'audio.mp3', { type: 'audio/mpeg' });

    userEvent.upload(fileInput, file);

    await waitFor(() => {
      expect(screen.getByText(/Formato de imagen no válido/i)).toBeInTheDocument();
    });
  });

  test('muestra error al subir imagen mayor de 5 MB', async () => {
    renderComponent();
    const fileInput = document.querySelector('input[type="file"]');
    const bigContent = new Uint8Array(6 * 1024 * 1024);
    const file = new File([bigContent], 'grande.png', { type: 'image/png' });

    userEvent.upload(fileInput, file);

    await waitFor(() => {
      expect(screen.getByText(/La imagen no puede superar 5 MB/i)).toBeInTheDocument();
    });
  });

  // ==============================
  // TESTS DE PLAN INSTITUCIONAL
  // ==============================

  test('muestra toggle institucional cuando hay plan activo', async () => {
    subscriptionsApi.getMySubscription.mockResolvedValue({
      plan: 'PREMIUM', activa: true,
      planCorporativoActivo: true, planCorporativo: 'BASICO',
      institutionId: 99, institutionNombre: 'Academia Test',
    });
    renderComponent();

    await screen.findByText(/Crear como comunidad institucional/i);
  });

  test('navega a planes institucionales al hacer clic', async () => {
    renderComponent();
    const instBtn = screen.getByRole('button', { name: /Ir a planes institucionales/i });
    userEvent.click(instBtn);
    expect(mockNavigate).toHaveBeenCalledWith('/planes/instituciones');
  });

  test('muestra comunidades activas y restantes', async () => {
    renderComponent();
    await waitFor(() => {
      expect(screen.getByText(/Comunidades activas:/i)).toBeInTheDocument();
      expect(screen.getByText(/Comunidades restantes:/i)).toBeInTheDocument();
    });
  });

  test('muestra rol alumno seleccionado por defecto', () => {
    renderComponent();
    const alumnoRadio = screen.getByDisplayValue('ALUMNO');
    expect(alumnoRadio).toBeChecked();
  });
});

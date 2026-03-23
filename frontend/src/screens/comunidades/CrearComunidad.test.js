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
        imagenUrl: 'empty',
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
});

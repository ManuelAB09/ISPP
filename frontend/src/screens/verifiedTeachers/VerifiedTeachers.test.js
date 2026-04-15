import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import * as tutorEndpoints from '../../api/tutorEndpoints';
import { useAuth } from '../../contexts/AuthContext';
import VerifiedTeachers from './VerifiedTeachers';

// Mocks
jest.mock('../../api/tutorEndpoints');
jest.mock('../../contexts/AuthContext');
jest.mock('../../api/valoraciones.api', () => ({
  getValoracionesStats: jest.fn().mockResolvedValue({ media: 4.5, total: 20 }),
}));
jest.mock('../../api/baseUrl', () => ({
  getApiBaseUrl: () => 'http://localhost:8080',
}));
jest.mock('../../utils/geoUtils', () => ({
  filterTutorsByDistance: jest.fn((tutors) => tutors),
  formatDistance: jest.fn((d) => `${d.toFixed(1)} km`),
  calculateDistance: jest.fn(() => 5),
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

describe('VerifiedTeachers', () => {
  const mockProfesores = [
    {
      id: 1,
      userId: 100,
      usuario: { id: 100, nombre: 'Juan García', foto: null },
      especialidades: ['Matemáticas', 'Física'],
      tarifaHora: 25.00,
      disponibilidad: 'Tardes y fines de semana',
      verificado: true,
      ubicacion: { latitud: 37.38, longitud: -5.97 },
    },
    {
      id: 2,
      userId: 101,
      usuario: { id: 101, nombre: 'María López', foto: '/img/maria.jpg' },
      especialidades: ['Inglés'],
      tarifaHora: 30.50,
      disponibilidad: 'Mañanas',
      verificado: false,
      ubicacion: null,
    },
  ];

  const mockPageResponse = {
    content: mockProfesores,
    totalElements: 2,
    number: 0,
    size: 20,
  };

  beforeEach(() => {
    jest.clearAllMocks();
    useAuth.mockReturnValue({
      isAuthenticated: true,
      user: { id: 99, nombre: 'Me', ubicacion: { latitud: 37.39, longitud: -5.98 }, esTutor: false },
      refreshUser: jest.fn(),
    });
    tutorEndpoints.getVerifiedTutors.mockResolvedValue(mockPageResponse);
  });

  const renderComponent = async () => {
    render(
      <MemoryRouter>
        <VerifiedTeachers />
      </MemoryRouter>
    );
    await waitFor(() => {
      expect(tutorEndpoints.getVerifiedTutors).toHaveBeenCalled();
    });
    await screen.findByText('Juan García', {}, { timeout: 10000 });
  };

  test('renderiza el título y subtítulo', async () => {
    await renderComponent();
    expect(screen.getByRole('heading', { name: /Profesores/i })).toBeInTheDocument();
    expect(screen.getByText(/Profesionales con identidad confirmada/i)).toBeInTheDocument();
  });

  test('renderiza el Header', async () => {
    await renderComponent();
    expect(screen.getByTestId('mock-header')).toBeInTheDocument();
  });

  test('muestra los profesores cargados', async () => {
    await renderComponent();
    expect(screen.getByText('Juan García')).toBeInTheDocument();
    expect(screen.getByText('María López')).toBeInTheDocument();
  });

  test('muestra las especialidades', async () => {
    await renderComponent();
    expect(screen.getByText('Matemáticas')).toBeInTheDocument();
    expect(screen.getByText('Física')).toBeInTheDocument();
    expect(screen.getByText('Inglés')).toBeInTheDocument();
  });

  test('muestra las tarifas formateadas', async () => {
    await renderComponent();
    expect(screen.getByText(/25.00 €/i)).toBeInTheDocument();
    expect(screen.getByText(/30.50 €/i)).toBeInTheDocument();
  });

  test('muestra la insignia solo para tutores verificados', async () => {
    await renderComponent();
    const badges = screen.getAllByText('Verificado');
    expect(badges.length).toBe(1);
    expect(screen.queryByText('No verificado')).not.toBeInTheDocument();
  });

  test('renderiza los filtros de búsqueda', async () => {
    await renderComponent();
    expect(screen.getByPlaceholderText(/Buscar por especialidad/i)).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /filtros/i }));
    expect(screen.getByPlaceholderText(/m[ií]n/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/m[aá]x/i)).toBeInTheDocument();
  });

  test('llama a la API al montar el componente', async () => {
    await renderComponent();
    expect(tutorEndpoints.getVerifiedTutors).toHaveBeenCalled();
  });

  test('muestra estado de error cuando falla la carga', async () => {
    tutorEndpoints.getVerifiedTutors.mockRejectedValue(new Error('Network error'));
    render(
      <MemoryRouter>
        <VerifiedTeachers />
      </MemoryRouter>
    );
    await waitFor(() => {
      expect(screen.getByText(/no se pudieron cargar los profesores/i)).toBeInTheDocument();
    });
  });

  test('muestra estado vacío cuando no hay profesores', async () => {
    tutorEndpoints.getVerifiedTutors.mockResolvedValue({
      content: [],
      totalElements: 0,
      number: 0,
      size: 20,
    });
    render(
      <MemoryRouter>
        <VerifiedTeachers />
      </MemoryRouter>
    );
    await waitFor(() => {
      expect(screen.getByText(/no se encontraron profesores/i)).toBeInTheDocument();
    });
  });

  test('aplica filtro de especialidad al buscar', async () => {
    await renderComponent();
    const espInput = screen.getByPlaceholderText(/buscar por especialidad/i);
    await userEvent.type(espInput, 'Matemáticas');
    const buscarBtn = screen.getByRole('button', { name: /^buscar$/i });
    await userEvent.click(buscarBtn);

    await waitFor(() => {
      expect(tutorEndpoints.getVerifiedTutors).toHaveBeenCalledWith(
        expect.objectContaining({ especialidad: 'Matemáticas' })
      );
    });
  });

  test('aplica filtros de tarifa al buscar', async () => {
    await renderComponent();
    await userEvent.click(screen.getByRole('button', { name: /filtros/i }));
    const minInput = screen.getByPlaceholderText(/m[ií]n/i);
    const maxInput = screen.getByPlaceholderText(/m[aá]x/i);
    await userEvent.type(minInput, '10');
    await userEvent.type(maxInput, '50');
    const buscarBtn = screen.getByRole('button', { name: /^buscar$/i });
    await userEvent.click(buscarBtn);

    await waitFor(() => {
      expect(tutorEndpoints.getVerifiedTutors).toHaveBeenCalledWith(
        expect.objectContaining({ tarifaMin: '10', tarifaMax: '50' })
      );
    });
  });

  test('limpiar filtros resets campos', async () => {
    await renderComponent();
    const espInput = screen.getByPlaceholderText(/buscar por especialidad/i);
    await userEvent.type(espInput, 'Física');
    const buscarBtn = screen.getByRole('button', { name: /^buscar$/i });
    await userEvent.click(buscarBtn);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /limpiar/i })).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: /limpiar/i }));

    await waitFor(() => {
      expect(espInput.value).toBe('');
    });
  });

  test('muestra enlace Ver perfil para cada profesor', async () => {
    await renderComponent();
    const verPerfilLinks = screen.getAllByText(/ver perfil/i);
    expect(verPerfilLinks.length).toBe(2);
  });

  test('muestra botón Contactar para otros usuarios', async () => {
    await renderComponent();
    const contactarBtns = screen.getAllByText(/contactar/i);
    expect(contactarBtns.length).toBeGreaterThan(0);
  });

  test('muestra disponibilidad del tutor', async () => {
    await renderComponent();
    expect(screen.getByText('Tardes y fines de semana')).toBeInTheDocument();
    expect(screen.getByText('Mañanas')).toBeInTheDocument();
  });

  test('muestra total de profesores', async () => {
    await renderComponent();
    expect(screen.getByText(/2 profesores/i)).toBeInTheDocument();
  });

  test('muestra botón cargar más cuando hay más páginas', async () => {
    tutorEndpoints.getVerifiedTutors.mockResolvedValue({
      content: mockProfesores,
      totalElements: 40,
      number: 0,
      size: 20,
    });
    await renderComponent();
    expect(screen.getByText(/cargar más profesores/i)).toBeInTheDocument();
  });

  test('no muestra cargar más cuando todos están cargados', async () => {
    await renderComponent();
    expect(screen.queryByText(/cargar más profesores/i)).not.toBeInTheDocument();
  });

  test('muestra badges de distancia cuando usuario tiene ubicación', async () => {
    await renderComponent();
    const badges = screen.getAllByText(/sin ubicación/i);
    expect(badges.length).toBeGreaterThan(0);
  });

  test('error state shows retry button', async () => {
    tutorEndpoints.getVerifiedTutors.mockRejectedValue(new Error('fail'));
    render(
      <MemoryRouter>
        <VerifiedTeachers />
      </MemoryRouter>
    );
    await waitFor(() => {
      expect(screen.getByText(/reintentar/i)).toBeInTheDocument();
    });
  });

  test('buscar por cercanía opens modal', async () => {
    await renderComponent();
    const cercaniaBtn = screen.getByRole('button', { name: /buscar por cercanía/i });
    await userEvent.click(cercaniaBtn);
    await waitFor(() => {
      expect(screen.getByText(/buscar por cercanía/i, { selector: 'h2' })).toBeInTheDocument();
    });
  });
});

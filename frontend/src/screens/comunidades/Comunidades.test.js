import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { communitiesApi } from '../../api/communities.api';
import { institutionsApi } from '../../api/institutions.api';
import Comunidades from './Comunidades';

// Mocks
jest.mock('../../api/communities.api');
jest.mock('../../api/institutions.api');
jest.mock('../../components/Header/Header', () => {
  return function MockHeader() {
    return <div data-testid="mock-header">Header</div>;
  };
});
jest.mock('../../components/Comunidad/ComunidadCard', () => {
  return function MockComunidadCard({ comunidad }) {
    return <div data-testid="comunidad-card">{comunidad.nombre}</div>;
  };
});
jest.mock('../../components/InputSearch/InputSearch', () => {
  return function MockInputSearch({ placeholder, onChange, value }) {
    return (
      <input
        data-testid="search-input"
        placeholder={placeholder}
        onChange={onChange}
        value={value}
      />
    );
  };
});
jest.mock('../../components/PageHeader', () => {
  return function MockPageHeader({ title, subtitle }) {
    return (
      <div data-testid="page-header">
        <h1>{title}</h1>
        <p>{subtitle}</p>
      </div>
    );
  };
});

describe('Comunidades', () => {
  const mockComunidades = [
    {
      id: 1,
      nombre: 'Matemáticas Avanzadas',
      descripcion: 'Comunidad para estudiar matemáticas',
      miembrosActuales: 25,
      maxMiembros: 50,
      categoria: ['Matemáticas'],
    },
    {
      id: 2,
      nombre: 'Física Cuántica',
      descripcion: 'Explorando el mundo cuántico',
      miembrosActuales: 15,
      maxMiembros: 30,
      categoria: ['Física'],
    },
  ];

  const mockPageResponse = {
    content: mockComunidades,
    page: { totalPages: 2 },
  };

  const mockCategoriesResponse = [
    { id: 1, nombre: 'Matemáticas', descripcion: 'Categoria 1', orden: 1 },
    { id: 2, nombre: 'Física', descripcion: 'Categoria 2', orden: 2 },
  ];

  const mockInstitutionsResponse = [
    { id: 1, nombre: 'Universidad de Sevilla' },
    { id: 2, nombre: 'IES Guadalquivir' },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    communitiesApi.list.mockResolvedValue(mockPageResponse);
    communitiesApi.listCategories.mockResolvedValue(mockCategoriesResponse);
    institutionsApi.list.mockResolvedValue(mockInstitutionsResponse);
  });

  const renderComponent = async () => {
    render(
      <MemoryRouter>
        <Comunidades />
      </MemoryRouter>
    );
    await waitFor(() => {
      expect(communitiesApi.list).toHaveBeenCalled();
    });
    await waitFor(() => {
      expect(communitiesApi.listCategories).toHaveBeenCalled();
      expect(institutionsApi.list).toHaveBeenCalled();
    });
    await screen.findByText('Matemáticas Avanzadas');
    await screen.findByRole('button', { name: 'Matemáticas' });
    await screen.findByRole('option', { name: 'Universidad de Sevilla' });
  };

  test('renderiza el título de la página', async () => {
    await renderComponent();
    expect(screen.getByRole('heading', { name: /Comunidades/i })).toBeInTheDocument();
  });

  test('renderiza el Header', async () => {
    await renderComponent();
    expect(screen.getByTestId('mock-header')).toBeInTheDocument();
  });

  test('muestra mensaje de carga mientras se cargan las comunidades', async () => {
    communitiesApi.list.mockImplementation(() => new Promise(() => {})); // Never resolves
    render(
      <MemoryRouter>
        <Comunidades />
      </MemoryRouter>
    );
    expect(await screen.findByText(/Cargando\.{3}/i)).toBeInTheDocument();
  });

  test('muestra las comunidades cargadas', async () => {
    await renderComponent();
    expect(screen.getByText('Matemáticas Avanzadas')).toBeInTheDocument();
    expect(screen.getByText('Física Cuántica')).toBeInTheDocument();
  });

  test('muestra mensaje de error cuando falla la carga', async () => {
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    communitiesApi.list.mockRejectedValue(new Error('Network error'));
    render(
      <MemoryRouter>
        <Comunidades />
      </MemoryRouter>
    );
    await screen.findByText(/Error al cargar comunidades/i);
    consoleSpy.mockRestore();
  });

  test('renderiza controles de paginación cuando hay múltiples páginas', async () => {
    await renderComponent();
    expect(screen.getByText(/Página 1 de 2/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Anterior/i })).toBeDisabled();
    expect(screen.getByRole('button', { name: /Siguiente/i })).toBeEnabled();
  });

  test('no muestra paginación cuando solo hay una página', async () => {
    communitiesApi.list.mockResolvedValue({
      content: mockComunidades,
      page: { totalPages: 1 },
    });
    render(
      <MemoryRouter>
        <Comunidades />
      </MemoryRouter>
    );
    await screen.findByText('Matemáticas Avanzadas');
    expect(screen.queryByText(/Página/i)).not.toBeInTheDocument();
  });

  test('navega a la página siguiente al hacer clic en Siguiente', async () => {
    await renderComponent();

    const nextButton = screen.getByRole('button', { name: /Siguiente/i });
    userEvent.click(nextButton);

    await waitFor(() => {
      expect(communitiesApi.list).toHaveBeenCalledWith(
        expect.objectContaining({ page: 1 })
      );
    });
  });

  test('actualiza búsqueda al escribir en el campo de búsqueda', async () => {
    await renderComponent();

    const searchInput = screen.getByTestId('search-input');
    userEvent.type(searchInput, 'Física');

    await waitFor(() => {
      expect(communitiesApi.list).toHaveBeenCalledWith(
        expect.objectContaining({ search: 'Física' })
      );
    });
  });

  test('envia filtros cuando se seleccionan', async () => {
    await renderComponent();

    const filterInstitucion = screen.getByLabelText(/Filtrar por institución/i);

    await userEvent.click(screen.getByRole('button', { name: 'Privadas' }));
    await userEvent.click(screen.getByRole('button', { name: 'Públicas' }));
    await userEvent.click(screen.getByRole('button', { name: 'PREMIUM' }));
    await userEvent.click(screen.getByRole('button', { name: 'FREE' }));
    await userEvent.click(screen.getByRole('button', { name: 'Matemáticas' }));
    await userEvent.click(screen.getByRole('button', { name: 'Física' }));
    await userEvent.selectOptions(filterInstitucion, 'Universidad de Sevilla');

    await waitFor(() => {
      expect(communitiesApi.list).toHaveBeenLastCalledWith(
        expect.objectContaining({
          tipoGrupo: ['GRUPO_PRIVADO', 'COMUNIDAD_PUBLICA'],
          tipoPlan: ['PREMIUM', 'FREE'],
          categoria: ['Matemáticas', 'Física'],
          institucion: 'Universidad de Sevilla',
        })
      );
    });
  });

  test('limita la longitud de la busqueda para evitar errores por exceso de caracteres', async () => {
    await renderComponent();

    const searchInput = screen.getByTestId('search-input');
    const longSearch = 'a'.repeat(160);
    await userEvent.clear(searchInput);
    await userEvent.type(searchInput, longSearch);

    await waitFor(() => {
      expect(communitiesApi.list).toHaveBeenLastCalledWith(
        expect.objectContaining({ search: 'a'.repeat(120) })
      );
    });
  });

  test('renderiza el botón de crear comunidad', async () => {
    localStorage.setItem('accessToken', 'test-token');
    await renderComponent();
    const createButton = screen.getByRole('button', { name: /Crear comunidad/i });
    expect(createButton).toBeInTheDocument();
  });

  test('renderiza el texto descriptivo del encabezado', async () => {
    await renderComponent();
    expect(screen.getByText(/Explora las comunidades que mejor se adaptan/i)).toBeInTheDocument();
  });
});

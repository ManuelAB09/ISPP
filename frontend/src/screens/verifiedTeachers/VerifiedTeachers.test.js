import React from 'react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import VerifiedTeachers from './VerifiedTeachers';
import * as tutorEndpoints from '../../api/tutorEndpoints';
import { useAuth } from '../../contexts/AuthContext';

// Mocks
jest.mock('../../api/tutorEndpoints');
jest.mock('../../contexts/AuthContext');
jest.mock('../../components/Header/Header', () => {
  return function MockHeader() {
    return <div data-testid="mock-header">Header</div>;
  };
});

describe('VerifiedTeachers', () => {
  const mockProfesores = [
    {
      id: 1,
      usuario: { id: 100, nombre: 'Juan García' },
      especialidades: ['Matemáticas', 'Física'],
      tarifaHora: 25.00,
      disponibilidad: 'Tardes y fines de semana',
      verificado: true,
    },
    {
      id: 2,
      usuario: { id: 101, nombre: 'María López' },
      especialidades: ['Inglés'],
      tarifaHora: 30.50,
      disponibilidad: 'Mañanas',
      verificado: true,
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
      isAuthenticated: false,
      user: null,
    });
    tutorEndpoints.getVerifiedTutors.mockResolvedValue(mockPageResponse);
    tutorEndpoints.getMyTutorProfiles.mockResolvedValue([]);
  });

  const renderComponent = async () => {
    render(
      <MemoryRouter>
        <VerifiedTeachers />
      </MemoryRouter>
    );
    await screen.findByText('Juan García');
  };

  test('renderiza el título y subtítulo', async () => {
    await renderComponent();
    expect(screen.getByRole('heading', { name: /Profesores Verificados/i })).toBeInTheDocument();
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

  test('muestra la insignia de Verificado', async () => {
    await renderComponent();
    const badges = screen.getAllByText('Verificado');
    expect(badges.length).toBe(2);
  });

  test('renderiza los filtros de búsqueda', async () => {
    await renderComponent();
    expect(screen.getByPlaceholderText(/Buscar por especialidad/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/€ mín/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/€ máx/i)).toBeInTheDocument();
  });

  test('llama a la API al montar el componente', async () => {
    await renderComponent();
    expect(tutorEndpoints.getVerifiedTutors).toHaveBeenCalled();
  });
});

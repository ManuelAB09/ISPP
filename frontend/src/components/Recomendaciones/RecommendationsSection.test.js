import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import RecommendationsSection from './RecommendationsSection';
import * as recommendationsApi from '../../api/recommendations.api';

jest.mock('../../api/recommendations.api');
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => jest.fn(),
}));

const mockRecommendations = {
  paraTi: [
    {
      id: 1,
      titulo: 'Recomendación 1',
      descripcion: 'Descripción 1',
      razon: 'Basado en tu actividad',
      puntuacion: 4.5,
      imagenUrl: 'https://example.com/image1.jpg'
    }
  ],
  profesores: [
    {
      id: 2,
      titulo: 'Profesor 1',
      razon: 'Especialización en tu área',
      imagenUrl: 'https://example.com/profesor1.jpg'
    }
  ],
  contenidos: [],
  cuestionarios: [],
  comunidades: []
};

describe('RecommendationsSection', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should show loading state initially', () => {
    recommendationsApi.getRecommendationsPage.mockImplementation(
      () => new Promise(() => {})
    );

    render(
      <BrowserRouter>
        <RecommendationsSection />
      </BrowserRouter>
    );

    expect(screen.getByText('Cargando recomendaciones...')).toBeInTheDocument();
  });

  it('should display recommendations when loaded', async () => {
    recommendationsApi.getRecommendationsPage.mockResolvedValue(mockRecommendations);

    render(
      <BrowserRouter>
        <RecommendationsSection />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Recomendación 1')).toBeInTheDocument();
      expect(screen.getByText('Profesor 1')).toBeInTheDocument();
    });
  });

  it('should display error when fetching fails', async () => {
    recommendationsApi.getRecommendationsPage.mockRejectedValue(
      new Error('API Error')
    );

    render(
      <BrowserRouter>
        <RecommendationsSection />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(
        screen.getByText('No se pudieron cargar las recomendaciones')
      ).toBeInTheDocument();
    });
  });

  it('should return null when no recommendations exist', async () => {
    recommendationsApi.getRecommendationsPage.mockResolvedValue({
      paraTi: [],
      profesores: [],
      contenidos: [],
      cuestionarios: [],
      comunidades: []
    });

    const { container } = render(
      <BrowserRouter>
        <RecommendationsSection />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(container.firstChild).toBeNull();
    });
  });

  it('should mark recommendation as seen when clicking checkmark', async () => {
    recommendationsApi.getRecommendationsPage.mockResolvedValue(mockRecommendations);
    recommendationsApi.markRecommendationAsSeen.mockResolvedValue(null);

    render(
      <BrowserRouter>
        <RecommendationsSection />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Recomendación 1')).toBeInTheDocument();
    });

    const checkButton = screen.getAllByRole('button')[0];
    await userEvent.click(checkButton);

    expect(recommendationsApi.markRecommendationAsSeen).toHaveBeenCalledWith(1);
  });

  it('should delete recommendation when clicking delete button', async () => {
    recommendationsApi.getRecommendationsPage.mockResolvedValue(mockRecommendations);
    recommendationsApi.deleteRecommendation.mockResolvedValue(null);

    render(
      <BrowserRouter>
        <RecommendationsSection />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Recomendación 1')).toBeInTheDocument();
    });

    const buttons = screen.getAllByRole('button');
    const deleteButton = buttons[buttons.length - 1];
    await userEvent.click(deleteButton);

    expect(recommendationsApi.deleteRecommendation).toHaveBeenCalledWith(1);
  });

  it('should display all sections when recommendations exist', async () => {
    const fullRecommendations = {
      paraTi: [
        {
          id: 1,
          titulo: 'Para Ti',
          descripcion: 'Test',
          razon: 'Razón',
          puntuacion: 4.5,
          imagenUrl: ''
        }
      ],
      profesores: [
        {
          id: 2,
          titulo: 'Profesor',
          razon: 'Razón',
          imagenUrl: ''
        }
      ],
      contenidos: [
        {
          id: 3,
          titulo: 'Contenido',
          razon: 'Razón',
          imagenUrl: ''
        }
      ],
      cuestionarios: [
        {
          id: 4,
          titulo: 'Cuestionario',
          razon: 'Razón',
          imagenUrl: ''
        }
      ],
      comunidades: [
        {
          id: 5,
          titulo: 'Comunidad',
          razon: 'Razón',
          imagenUrl: ''
        }
      ]
    };

    recommendationsApi.getRecommendationsPage.mockResolvedValue(fullRecommendations);

    render(
      <BrowserRouter>
        <RecommendationsSection />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Lo mejor para ti')).toBeInTheDocument();
      expect(screen.getByText('👨‍🏫 Profesores recomendados')).toBeInTheDocument();
      expect(screen.getByText('📚 Contenidos sugeridos')).toBeInTheDocument();
      expect(screen.getByText('❓ Cuestionarios recomendados')).toBeInTheDocument();
      expect(screen.getByText('👥 Comunidades que te pueden interesar')).toBeInTheDocument();
    });
  });
});

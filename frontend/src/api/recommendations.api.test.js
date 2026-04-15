import * as recommendationsApi from './recommendations.api';
import { apiClient } from './client';

jest.mock('./client');

describe('recommendationsApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('getRecommendationsPage', () => {
    it('should call GET /api/recommendations/page', async () => {
      const mockResponse = {
        paraTi: [],
        profesores: [],
        contenidos: [],
        cuestionarios: [],
        comunidades: [],
        generadoEn: '2024-01-01T00:00:00'
      };
      apiClient.get.mockResolvedValue(mockResponse);

      const result = await recommendationsApi.getRecommendationsPage();

      expect(apiClient.get).toHaveBeenCalledWith('/api/recommendations/page');
      expect(result).toEqual(mockResponse);
    });
  });

  describe('getRecommendedTutors', () => {
    it('should call GET /api/recommendations/profesores with pagination', async () => {
      const mockResponse = { content: [], totalElements: 0 };
      apiClient.get.mockResolvedValue(mockResponse);

      const result = await recommendationsApi.getRecommendedTutors({ page: 0, size: 6 });

      expect(apiClient.get).toHaveBeenCalledWith('/api/recommendations/profesores?page=0&size=6');
      expect(result).toEqual(mockResponse);
    });
  });

  describe('getRecommendedContent', () => {
    it('should call GET /api/recommendations/contenido with pagination', async () => {
      const mockResponse = { content: [], totalElements: 0 };
      apiClient.get.mockResolvedValue(mockResponse);

      const result = await recommendationsApi.getRecommendedContent({ page: 0, size: 8 });

      expect(apiClient.get).toHaveBeenCalledWith('/api/recommendations/contenido?page=0&size=8');
      expect(result).toEqual(mockResponse);
    });
  });

  describe('getRecommendedQuestions', () => {
    it('should call GET /api/recommendations/cuestionarios with pagination', async () => {
      const mockResponse = { content: [], totalElements: 0 };
      apiClient.get.mockResolvedValue(mockResponse);

      const result = await recommendationsApi.getRecommendedQuestions({ page: 0, size: 6 });

      expect(apiClient.get).toHaveBeenCalledWith('/api/recommendations/cuestionarios?page=0&size=6');
      expect(result).toEqual(mockResponse);
    });
  });

  describe('getRecommendedCommunities', () => {
    it('should call GET /api/recommendations/comunidades with pagination', async () => {
      const mockResponse = { content: [], totalElements: 0 };
      apiClient.get.mockResolvedValue(mockResponse);

      const result = await recommendationsApi.getRecommendedCommunities({ page: 0, size: 4 });

      expect(apiClient.get).toHaveBeenCalledWith('/api/recommendations/comunidades?page=0&size=4');
      expect(result).toEqual(mockResponse);
    });
  });

  describe('getActiveRecommendations', () => {
    it('should call GET /api/recommendations with pagination', async () => {
      const mockResponse = { content: [], totalElements: 0 };
      apiClient.get.mockResolvedValue(mockResponse);

      const result = await recommendationsApi.getActiveRecommendations({ page: 0, size: 20 });

      expect(apiClient.get).toHaveBeenCalledWith('/api/recommendations?page=0&size=20');
      expect(result).toEqual(mockResponse);
    });
  });

  describe('getUnseenRecommendations', () => {
    it('should call GET /api/recommendations/no-vistas with pagination', async () => {
      const mockResponse = { content: [], totalElements: 0 };
      apiClient.get.mockResolvedValue(mockResponse);

      const result = await recommendationsApi.getUnseenRecommendations({ page: 0, size: 10 });

      expect(apiClient.get).toHaveBeenCalledWith('/api/recommendations/no-vistas?page=0&size=10');
      expect(result).toEqual(mockResponse);
    });
  });

  describe('markRecommendationAsSeen', () => {
    it('should call POST /api/recommendations/{id}/vista', async () => {
      apiClient.post.mockResolvedValue(null);

      await recommendationsApi.markRecommendationAsSeen(123);

      expect(apiClient.post).toHaveBeenCalledWith('/api/recommendations/123/vista');
    });
  });

  describe('deleteRecommendation', () => {
    it('should call DELETE /api/recommendations/{id}', async () => {
      apiClient.delete.mockResolvedValue(null);

      await recommendationsApi.deleteRecommendation(123);

      expect(apiClient.delete).toHaveBeenCalledWith('/api/recommendations/123');
    });
  });

  describe('giveRecommendationFeedback', () => {
    it('should call POST /api/recommendations/{id}/feedback with feedback data', async () => {
      const feedback = { esUtil: true, comentario: 'Buena recomendación', satisfaccion: 4 };
      apiClient.post.mockResolvedValue(null);

      await recommendationsApi.giveRecommendationFeedback(123, feedback);

      expect(apiClient.post).toHaveBeenCalledWith(
        '/api/recommendations/123/feedback',
        feedback
      );
    });
  });

  describe('registerUserActivity', () => {
    it('should call POST /api/recommendations/actividad with activity data', async () => {
      const activity = { tipoActividad: 'BUSQUEDA', categoriaObjeto: 'Tutor', terminosBusqueda: 'física' };
      apiClient.post.mockResolvedValue(null);

      await recommendationsApi.registerUserActivity(activity);

      expect(apiClient.post).toHaveBeenCalledWith('/api/recommendations/actividad', activity);
    });
  });

  describe('refreshRecommendations', () => {
    it('should call POST /api/recommendations/refresh', async () => {
      apiClient.post.mockResolvedValue(null);

      await recommendationsApi.refreshRecommendations();

      expect(apiClient.post).toHaveBeenCalledWith('/api/recommendations/refresh');
    });
  });
});

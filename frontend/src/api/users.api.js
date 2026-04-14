import axiosInstance from './axiosConfig';

export const usersApi = {
  /**
   * Busca usuarios por nombre o email para asignación en cuestionarios
   * GET /api/v1/users/search
   * @param {string} search - Término de búsqueda (nombre o email)
   * @returns {Promise<Array>} Lista de usuarios (UserSimpleResponse)
   */
  searchUsers: async (search) => {
    const response = await axiosInstance.get('/api/v1/users/search', {
      params: { search }
    });
    return response.data;
  }
};

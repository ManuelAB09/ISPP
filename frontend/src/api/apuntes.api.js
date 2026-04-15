import axiosInstance from './axiosConfig';

/**
 * API client para apuntes
 */
export const apuntesApi = {
  /**
   * Sube un nuevo apunte a una comunidad
   * @param {number} communityId ID de la comunidad
   * @param {string} titulo Título del apunte
   * @param {string} descripcion Descripción del apunte (opcional)
   * @param {File} file Archivo del apunte
   */
  subirApunte(communityId, titulo, descripcion, file) {
    const formData = new FormData();
    formData.append('file', file);
    if (titulo) formData.append('titulo', titulo);
    if (descripcion) formData.append('descripcion', descripcion);

    return axiosInstance.post(
      `/api/v1/communities/${communityId}/apuntes/upload`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    );
  },

  /**
   * Obtiene los apuntes de una comunidad
   * @param {number} communityId ID de la comunidad
   * @param {number} page Número de página (default: 0)
   * @param {number} size Tamaño de página (default: 10)
   */
  obtenerApuntes(communityId, page = 0, size = 10) {
    return axiosInstance.get(
      `/api/v1/communities/${communityId}/apuntes?page=${page}&size=${size}`
    );
  },

  /**
   * Obtiene un apunte específico
   * @param {number} communityId ID de la comunidad
   * @param {number} apunteId ID del apunte
   */
  obtenerApunte(communityId, apunteId) {
    return axiosInstance.get(
      `/api/v1/communities/${communityId}/apuntes/${apunteId}`
    );
  },

  /**
   * Descarga un apunte
   * @param {number} communityId ID de la comunidad
   * @param {number} apunteId ID del apunte
   */
  descargarApunte(communityId, apunteId) {
    return axiosInstance.get(
      `/api/v1/communities/${communityId}/apuntes/${apunteId}/download`,
      { responseType: 'blob' }
    );
  },

  /**
   * Busca apuntes por título
   * @param {number} communityId ID de la comunidad
   * @param {string} titulo Título a buscar
   * @param {number} page Número de página (default: 0)
   * @param {number} size Tamaño de página (default: 10)
   */
  buscarApuntes(communityId, titulo, page = 0, size = 10) {
    return axiosInstance.get(
      `/api/v1/communities/${communityId}/apuntes/search?titulo=${encodeURIComponent(titulo)}&page=${page}&size=${size}`
    );
  },

  /**
   * Elimina un apunte
   * @param {number} communityId ID de la comunidad
   * @param {number} apunteId ID del apunte
   */
  eliminarApunte(communityId, apunteId) {
    return axiosInstance.delete(
      `/api/v1/communities/${communityId}/apuntes/${apunteId}`
    );
  },
};

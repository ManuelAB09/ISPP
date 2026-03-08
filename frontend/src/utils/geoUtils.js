/**
 * Utilidades para cálculos geográficos
 */

/**
 * Calcula la distancia entre dos puntos geográficos usando la fórmula de Haversine
 * @param {number} lat1 - Latitud del primer punto
 * @param {number} lon1 - Longitud del primer punto
 * @param {number} lat2 - Latitud del segundo punto
 * @param {number} lon2 - Longitud del segundo punto
 * @returns {number} Distancia en kilómetros
 */
export const calculateDistance = (lat1, lon1, lat2, lon2) => {
  const EARTH_RADIUS_KM = 6371;

  const toRadians = (degrees) => degrees * (Math.PI / 180);

  const lat1Rad = toRadians(lat1);
  const lat2Rad = toRadians(lat2);
  const deltaLat = toRadians(lat2 - lat1);
  const deltaLon = toRadians(lon2 - lon1);

  const a =
    Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
    Math.cos(lat1Rad) *
      Math.cos(lat2Rad) *
      Math.sin(deltaLon / 2) *
      Math.sin(deltaLon / 2);

  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

  return EARTH_RADIUS_KM * c;
};

/**
 * Formatea la distancia para mostrar en la UI
 * @param {number} distanciaKm - Distancia en kilómetros
 * @returns {string} Distancia formateada (ej: "1.5 km" o "500 m")
 */
export const formatDistance = (distanciaKm) => {
  if (distanciaKm < 1.0) {
    const metros = Math.round(distanciaKm * 1000);
    return `${metros} m`;
  }
  return `${distanciaKm.toFixed(1)} km`;
};

/**
 * Filtra y ordena tutores por distancia desde una ubicación
 * @param {Array} tutores - Array de tutores con ubicacion.latitud y ubicacion.longitud
 * @param {number} userLat - Latitud del usuario
 * @param {number} userLon - Longitud del usuario
 * @param {number} maxRadioKm - Radio máximo en kilómetros (opcional)
 * @returns {Array} Tutores ordenados por distancia con campo distanciaKm añadido
 */
export const filterTutorsByDistance = (
  tutores,
  userLat,
  userLon,
  maxRadioKm = null
) => {
  return tutores
    .filter((tutor) => tutor.ubicacion && tutor.ubicacion.latitud && tutor.ubicacion.longitud)
    .map((tutor) => ({
      ...tutor,
      distanciaKm: calculateDistance(
        userLat,
        userLon,
        tutor.ubicacion.latitud,
        tutor.ubicacion.longitud
      ),
    }))
    .filter((tutor) => (maxRadioKm ? tutor.distanciaKm <= maxRadioKm : true))
    .sort((a, b) => a.distanciaKm - b.distanciaKm);
};

/**
 * Obtiene la ubicación actual del navegador
 * @returns {Promise<{latitude: number, longitude: number}>}
 */
export const getCurrentPosition = () => {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('Geolocalización no soportada por el navegador'));
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        resolve({
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
        });
      },
      (error) => {
        reject(error);
      },
      {
        enableHighAccuracy: true,
        timeout: 5000,
        maximumAge: 0,
      }
    );
  });
};

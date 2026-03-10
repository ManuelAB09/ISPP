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
  const pLat1 = Number(lat1);
  const pLon1 = Number(lon1);
  const pLat2 = Number(lat2);
  const pLon2 = Number(lon2);

  if (
    !Number.isFinite(pLat1) ||
    !Number.isFinite(pLon1) ||
    !Number.isFinite(pLat2) ||
    !Number.isFinite(pLon2)
  ) {
    return Number.NaN;
  }

  const EARTH_RADIUS_KM = 6371;

  const toRadians = (degrees) => degrees * (Math.PI / 180);

  const lat1Rad = toRadians(pLat1);
  const lat2Rad = toRadians(pLat2);
  const deltaLat = toRadians(pLat2 - pLat1);
  const deltaLon = toRadians(pLon2 - pLon1);

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
  if (!Number.isFinite(distanciaKm)) {
    return 'Sin ubicación';
  }
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
  const hasValidCoords = (ubicacion) => {
    if (!ubicacion || typeof ubicacion !== 'object') return false;
    const lat = Number(ubicacion.latitud);
    const lon = Number(ubicacion.longitud);
    return Number.isFinite(lat) && Number.isFinite(lon);
  };

  const parsedUserLat = Number(userLat);
  const parsedUserLon = Number(userLon);

  if (!Number.isFinite(parsedUserLat) || !Number.isFinite(parsedUserLon)) {
    return [];
  }

  return tutores
    .filter((tutor) => hasValidCoords(tutor.ubicacion))
    .map((tutor) => ({
      ...tutor,
      distanciaKm: calculateDistance(
        parsedUserLat,
        parsedUserLon,
        Number(tutor.ubicacion.latitud),
        Number(tutor.ubicacion.longitud)
      ),
    }))
    .filter((tutor) => Number.isFinite(tutor.distanciaKm))
    .filter((tutor) => (maxRadioKm != null ? tutor.distanciaKm <= maxRadioKm : true))
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

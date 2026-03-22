import {
  calculateDistance,
  formatDistance,
  filterTutorsByDistance,
} from './geoUtils';

describe('calculateDistance', () => {
  test('returns 0 for same point', () => {
    expect(calculateDistance(40.4168, -3.7038, 40.4168, -3.7038)).toBeCloseTo(0, 1);
  });

  test('calculates Madrid to Barcelona approximately 505 km', () => {
    const dist = calculateDistance(40.4168, -3.7038, 41.3851, 2.1734);
    expect(dist).toBeGreaterThan(400);
    expect(dist).toBeLessThan(700);
  });

  test('returns NaN for non-finite inputs', () => {
    expect(calculateDistance(NaN, 0, 0, 0)).toBeNaN();
    expect(calculateDistance(0, Infinity, 0, 0)).toBeNaN();
    expect(calculateDistance(0, 0, undefined, 0)).toBeNaN();
  });

  test('handles string numbers', () => {
    const dist = calculateDistance('40.4168', '-3.7038', '41.3851', '2.1734');
    expect(dist).toBeGreaterThan(400);
  });
});

describe('formatDistance', () => {
  test('formats kilometers with one decimal', () => {
    expect(formatDistance(5.3)).toBe('5.3 km');
  });

  test('formats meters when less than 1 km', () => {
    expect(formatDistance(0.5)).toBe('500 m');
  });

  test('returns "Sin ubicación" for NaN', () => {
    expect(formatDistance(NaN)).toBe('Sin ubicación');
  });

  test('returns "Sin ubicación" for non-finite', () => {
    expect(formatDistance(Infinity)).toBe('Sin ubicación');
  });
});

describe('filterTutorsByDistance', () => {
  const tutores = [
    { id: 1, ubicacion: { latitud: 41.3851, longitud: 2.1734 } }, // Barcelona
    { id: 2, ubicacion: { latitud: 37.3891, longitud: -5.9845 } }, // Sevilla
    { id: 3, ubicacion: null }, // no location
  ];

  test('filters and sorts by distance from Madrid', () => {
    const result = filterTutorsByDistance(tutores, 40.4168, -3.7038);
    expect(result).toHaveLength(2);
    // Sevilla closer to Madrid than Barcelona
    expect(result[0].id).toBe(2);
    expect(result[1].id).toBe(1);
  });

  test('applies max radius filter', () => {
    // ~400 km radius: Sevilla is ~390 km from Madrid, Barcelona ~505 km
    const result = filterTutorsByDistance(tutores, 40.4168, -3.7038, 400);
    expect(result).toHaveLength(1);
    expect(result[0].id).toBe(2);
  });

  test('returns empty for invalid user coordinates', () => {
    expect(filterTutorsByDistance(tutores, NaN, NaN)).toEqual([]);
  });

  test('excludes tutors with invalid location objects', () => {
    const withBad = [
      { id: 1, ubicacion: { latitud: 'abc', longitud: 2.0 } },
    ];
    expect(filterTutorsByDistance(withBad, 40, -3)).toEqual([]);
  });
});

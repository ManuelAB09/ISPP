// jest-dom adds custom jest matchers for asserting on DOM nodes.
// allows you to do things like:
// expect(element).toHaveTextContent(/react/i)
// learn more: https://github.com/testing-library/jest-dom
import '@testing-library/jest-dom';

// Mock react-leaflet and leaflet to avoid ES6 module parsing issues in Jest
jest.mock('react-leaflet', () => ({
  MapContainer: ({ children }) => <div data-testid="map-container">{children}</div>,
  TileLayer: () => null,
  Marker: () => null,
  Popup: () => null,
  useMapEvents: () => ({}),
  useMap: () => ({
    setView: jest.fn(),
  }),
}));

jest.mock('leaflet', () => ({
  icon: jest.fn(() => ({})),
  divIcon: jest.fn(() => ({})),
}));

const originalConsoleWarn = console.warn;
const originalConsoleLog = console.log;

jest.spyOn(console, 'warn').mockImplementation((...args) => {
  const firstArg = typeof args[0] === 'string' ? args[0] : '';
  if (firstArg.includes('React Router Future Flag Warning')) {
    return;
  }
  originalConsoleWarn(...args);
});

jest.spyOn(console, 'log').mockImplementation((...args) => {
  const firstArg = typeof args[0] === 'string' ? args[0] : '';
  if (firstArg.includes('API_BASE_URL =')) {
    return;
  }
  originalConsoleLog(...args);
});

afterAll(() => {
  console.warn.mockRestore();
  console.log.mockRestore();
});

// jest-dom adds custom jest matchers for asserting on DOM nodes.
// allows you to do things like:
// expect(element).toHaveTextContent(/react/i)
// learn more: https://github.com/testing-library/jest-dom
import '@testing-library/jest-dom';

// Mock axios to avoid ESM parsing issues in Jest/CRA
jest.mock('axios', () => {
  const axiosMock = {
    create: jest.fn(() => axiosMock),
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
    patch: jest.fn(),
    delete: jest.fn(),
    interceptors: {
      request: { use: jest.fn(), eject: jest.fn() },
      response: { use: jest.fn(), eject: jest.fn() },
    },
    defaults: { headers: { common: {} } },
  };

  return {
    __esModule: true,
    default: axiosMock,
    ...axiosMock,
  };
});

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

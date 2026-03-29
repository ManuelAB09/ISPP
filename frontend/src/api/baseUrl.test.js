import { getApiBaseUrl } from './baseUrl';

describe('getApiBaseUrl', () => {
    const originalEnv = process.env;

    beforeEach(() => {
        jest.resetModules();
        process.env = { ...originalEnv };
        delete process.env.REACT_APP_API_URL;
    });

    afterEach(() => {
        process.env = originalEnv;
    });

    test('returns localhost:8080 for local development', () => {
        Object.defineProperty(window, 'location', {
            value: { hostname: 'localhost', port: '3000' },
            writable: true,
        });
        expect(getApiBaseUrl()).toBe('http://localhost:8080');
    });

    test('returns empty string for non-local deployment', () => {
        Object.defineProperty(window, 'location', {
            value: { hostname: 'myapp.example.com', port: '' },
            writable: true,
        });
        expect(getApiBaseUrl()).toBe('');
    });

    test('uses REACT_APP_API_URL when set', () => {
        process.env.REACT_APP_API_URL = 'https://api.example.com';
        Object.defineProperty(window, 'location', {
            value: { hostname: 'localhost', port: '3000' },
            writable: true,
        });
        const { getApiBaseUrl: fn } = require('./baseUrl');
        expect(fn()).toBe('https://api.example.com');
    });

    test('handles :8080 as empty URL', () => {
        process.env.REACT_APP_API_URL = ':8080';
        Object.defineProperty(window, 'location', {
            value: { hostname: 'localhost', port: '3000' },
            writable: true,
        });
        expect(getApiBaseUrl()).toBe('http://localhost:8080');
    });

    test('returns localhost:8080 for 127.0.0.1', () => {
        Object.defineProperty(window, 'location', {
            value: { hostname: '127.0.0.1', port: '3000' },
            writable: true,
        });
        expect(getApiBaseUrl()).toBe('http://localhost:8080');
    });

    test('returns localhost:8080 for ::1 IPv6', () => {
        Object.defineProperty(window, 'location', {
            value: { hostname: '::1', port: '3000' },
            writable: true,
        });
        expect(getApiBaseUrl()).toBe('http://localhost:8080');
    });

    test('handles URL without protocol', () => {
        process.env.REACT_APP_API_URL = 'api.example.com:8080';
        Object.defineProperty(window, 'location', {
            value: { hostname: 'localhost', port: '3000' },
            writable: true,
        });
        const { getApiBaseUrl: fn } = require('./baseUrl');
        expect(fn()).toBe('http://api.example.com:8080');
    });

    test('returns default for 0.0.0.0 hostname', () => {
        process.env.REACT_APP_API_URL = 'http://0.0.0.0:8080';
        Object.defineProperty(window, 'location', {
            value: { hostname: 'localhost', port: '3000' },
            writable: true,
        });
        const { getApiBaseUrl: fn } = require('./baseUrl');
        expect(fn()).toBe('http://localhost:8080');
    });

    test('guardrail prevents pointing to same port as frontend', () => {
        process.env.REACT_APP_API_URL = 'http://localhost:3000';
        Object.defineProperty(window, 'location', {
            value: { hostname: 'localhost', port: '3000' },
            writable: true,
        });
        const { getApiBaseUrl: fn } = require('./baseUrl');
        expect(fn()).toBe('http://localhost:8080');
    });
});

import React from 'react';
import { render, screen, fireEvent, act } from '@testing-library/react';
import GoogleAuthButton from './GoogleAuthButton';

jest.mock('../api/baseUrl', () => ({
    getApiBaseUrl: () => 'http://localhost:8080',
}));

describe('GoogleAuthButton', () => {
    const onSuccess = jest.fn();
    const onError = jest.fn();

    beforeEach(() => {
        jest.clearAllMocks();
        global.fetch = jest.fn();
        global.window.open = jest.fn();
    });

    afterEach(() => {
        delete global.fetch;
    });

    test('renders default text', () => {
        render(<GoogleAuthButton onSuccess={onSuccess} onError={onError} />);
        expect(screen.getByText('Continuar con Google')).toBeInTheDocument();
    });

    test('renders custom text', () => {
        render(<GoogleAuthButton onSuccess={onSuccess} onError={onError} text="Vincular Google" />);
        expect(screen.getByText('Vincular Google')).toBeInTheDocument();
    });

    test('opens popup on click for login flow', async () => {
        global.fetch.mockResolvedValueOnce({
            ok: true,
            json: () => Promise.resolve({ url: 'https://accounts.google.com/auth' }),
        });

        render(<GoogleAuthButton onSuccess={onSuccess} onError={onError} />);
        await act(async () => {
            fireEvent.click(screen.getByText('Continuar con Google'));
        });

        expect(global.fetch).toHaveBeenCalledWith(
            'http://localhost:8080/api/v1/auth/google/authorize',
            expect.objectContaining({ method: 'GET' })
        );
        expect(global.window.open).toHaveBeenCalled();
    });

    test('uses link endpoint for link flowType', async () => {
        global.fetch.mockResolvedValueOnce({
            ok: true,
            json: () => Promise.resolve({ url: 'https://accounts.google.com/link' }),
        });

        render(<GoogleAuthButton onSuccess={onSuccess} onError={onError} flowType="link" />);
        await act(async () => {
            fireEvent.click(screen.getByText('Continuar con Google'));
        });

        expect(global.fetch).toHaveBeenCalledWith(
            'http://localhost:8080/api/v1/auth/google/link/authorize',
            expect.objectContaining({ method: 'GET' })
        );
    });

    test('shows loading state while connecting', async () => {
        let resolvePromise;
        global.fetch.mockReturnValue(new Promise((res) => { resolvePromise = res; }));

        render(<GoogleAuthButton onSuccess={onSuccess} onError={onError} />);
        fireEvent.click(screen.getByText('Continuar con Google'));

        expect(screen.getByText('Conectando...')).toBeInTheDocument();
        expect(screen.getByRole('button')).toBeDisabled();

        await act(async () => {
            resolvePromise({ ok: true, json: () => Promise.resolve({ url: 'http://x' }) });
        });
    });

    test('calls onError when fetch fails', async () => {
        global.fetch.mockRejectedValueOnce(new Error('Network error'));

        render(<GoogleAuthButton onSuccess={onSuccess} onError={onError} />);
        await act(async () => {
            fireEvent.click(screen.getByText('Continuar con Google'));
        });

        expect(onError).toHaveBeenCalledWith('Network error');
    });

    test('calls onError when response not ok', async () => {
        global.fetch.mockResolvedValueOnce({ ok: false });

        render(<GoogleAuthButton onSuccess={onSuccess} onError={onError} />);
        await act(async () => {
            fireEvent.click(screen.getByText('Continuar con Google'));
        });

        expect(onError).toHaveBeenCalledWith('Error al contactar con el servidor');
    });

    test('calls onSuccess on google-auth-success message', async () => {
        const listeners = [];
        jest.spyOn(window, 'addEventListener').mockImplementation((type, fn) => {
            if (type === 'message') listeners.push(fn);
        });
        jest.spyOn(window, 'removeEventListener').mockImplementation(() => {});

        global.fetch.mockResolvedValueOnce({
            ok: true,
            json: () => Promise.resolve({ url: 'https://auth.url' }),
        });

        render(<GoogleAuthButton onSuccess={onSuccess} onError={onError} />);
        await act(async () => {
            fireEvent.click(screen.getByText('Continuar con Google'));
        });

        expect(listeners.length).toBe(1);

        act(() => {
            listeners[0]({ data: { type: 'google-auth-success', payload: { token: 'abc' } } });
        });

        expect(onSuccess).toHaveBeenCalledWith({ token: 'abc' });
        window.addEventListener.mockRestore();
        window.removeEventListener.mockRestore();
    });

    test('calls onError on google-auth-error message', async () => {
        const listeners = [];
        jest.spyOn(window, 'addEventListener').mockImplementation((type, fn) => {
            if (type === 'message') listeners.push(fn);
        });
        jest.spyOn(window, 'removeEventListener').mockImplementation(() => {});

        global.fetch.mockResolvedValueOnce({
            ok: true,
            json: () => Promise.resolve({ url: 'https://auth.url' }),
        });

        render(<GoogleAuthButton onSuccess={onSuccess} onError={onError} />);
        await act(async () => {
            fireEvent.click(screen.getByText('Continuar con Google'));
        });

        act(() => {
            listeners[0]({ data: { type: 'google-auth-error', error: 'Something failed' } });
        });

        expect(onError).toHaveBeenCalledWith('Something failed');
        window.addEventListener.mockRestore();
        window.removeEventListener.mockRestore();
    });

    test('calls onSuccess on google-link-success message', async () => {
        const listeners = [];
        jest.spyOn(window, 'addEventListener').mockImplementation((type, fn) => {
            if (type === 'message') listeners.push(fn);
        });
        jest.spyOn(window, 'removeEventListener').mockImplementation(() => {});

        global.fetch.mockResolvedValueOnce({
            ok: true,
            json: () => Promise.resolve({ url: 'https://auth.url' }),
        });

        render(<GoogleAuthButton onSuccess={onSuccess} onError={onError} flowType="link" />);
        await act(async () => {
            fireEvent.click(screen.getByText('Continuar con Google'));
        });

        act(() => {
            listeners[0]({ data: { type: 'google-link-success' } });
        });

        expect(onSuccess).toHaveBeenCalledWith({});
        window.addEventListener.mockRestore();
        window.removeEventListener.mockRestore();
    });

    test('hover changes background color', () => {
        render(<GoogleAuthButton onSuccess={onSuccess} onError={onError} />);
        const button = screen.getByRole('button');
        fireEvent.mouseOver(button);
        expect(button.style.backgroundColor).toBe('rgb(248, 249, 250)');
        fireEvent.mouseOut(button);
        expect(button.style.backgroundColor).toBe('rgb(255, 255, 255)');
    });
});

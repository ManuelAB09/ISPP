import React from 'react';
import { render, screen } from '@testing-library/react';
import { renderHook } from '@testing-library/react';

const mockUseSocket = jest.fn();
jest.mock('../hooks/useSocket', () => ({
    useSocket: (...args) => mockUseSocket(...args),
}));

const { SocketProvider, useSocketContext } = require('../contexts/SocketContext');

describe('SocketContext', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseSocket.mockReturnValue({ socket: { id: 'mock-socket' }, isConnected: true });
    });

    test('SocketProvider renders children', () => {
        render(
            <SocketProvider token="test-token">
                <div>Child Component</div>
            </SocketProvider>
        );
        expect(screen.getByText('Child Component')).toBeInTheDocument();
    });

    test('SocketProvider passes token to useSocket', () => {
        render(
            <SocketProvider token="my-jwt-token">
                <div>Test</div>
            </SocketProvider>
        );
        expect(mockUseSocket).toHaveBeenCalledWith('my-jwt-token');
    });

    test('useSocketContext returns socket and isConnected', () => {
        const wrapper = ({ children }) => (
            <SocketProvider token="test">{children}</SocketProvider>
        );
        const { result } = renderHook(() => useSocketContext(), { wrapper });
        expect(result.current.socket).toEqual({ id: 'mock-socket' });
        expect(result.current.isConnected).toBe(true);
    });

    test('useSocketContext throws outside provider', () => {
        expect(() => {
            renderHook(() => useSocketContext());
        }).toThrow('useSocketContext debe usarse dentro de <SocketProvider>');
    });

    test('provides disconnected state', () => {
        mockUseSocket.mockReturnValue({ socket: null, isConnected: false });
        const wrapper = ({ children }) => (
            <SocketProvider token="test">{children}</SocketProvider>
        );
        const { result } = renderHook(() => useSocketContext(), { wrapper });
        expect(result.current.socket).toBeNull();
        expect(result.current.isConnected).toBe(false);
    });
});

import { render, screen, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { NotificationProvider, useNotificationContext } from './NotificationContext';

const mockSocketOn = jest.fn();
const mockSocketOff = jest.fn();

jest.mock('../api/auth.api', () => ({
    authApi: {
        getUserPublicProfile: jest.fn(() => Promise.resolve({ data: { nombre: 'User', foto: null } })),
    },
}));

jest.mock('../api/baseUrl', () => ({
    getApiBaseUrl: () => 'http://localhost:8080',
}));

jest.mock('../api/communities.api', () => ({
    communitiesApi: {
        listMine: jest.fn(() => Promise.resolve({ content: [] })),
    },
}));

jest.mock('../api/mensajeService', () => ({
    obtenerConversaciones: jest.fn(() => Promise.resolve({ data: [] })),
    obtenerHistorialComunidad: jest.fn(() => Promise.resolve({ data: [] })),
}));

const mockGetAllEventAlerts = jest.fn(() => Promise.resolve([]));
const mockGetAllUserNotifications = jest.fn(() => Promise.resolve([]));
jest.mock('../api/notificationService', () => ({
    getAllEventAlerts: (...args) => mockGetAllEventAlerts(...args),
    getAllUserNotifications: (...args) => mockGetAllUserNotifications(...args),
}));

const mockShowNotification = jest.fn();
const mockRequestPermission = jest.fn(() => Promise.resolve('granted'));
jest.mock('../hooks/useNotifications', () => ({
    useNotifications: () => ({
        permission: 'granted',
        requestPermission: mockRequestPermission,
        showNotification: mockShowNotification,
        isSupported: true,
    }),
}));

jest.mock('../screens/chat/Chats', () => ({
    resolveCommunityImage: jest.fn(() => '/default.png'),
}));

const mockUser = { id: 1, nombre: 'Test User', notificacionesPush: undefined };
jest.mock('./AuthContext', () => ({
    useAuth: () => ({
        isAuthenticated: true,
        user: mockUser,
    }),
}));

jest.mock('./SocketContext', () => ({
    useSocketContext: () => ({
        socket: {
            on: mockSocketOn,
            off: mockSocketOff,
        },
        isConnected: true,
    }),
}));

function TestConsumer() {
    const ctx = useNotificationContext();
    return (
        <div>
            <span data-testid="enabled">{String(ctx?.notificationsEnabled ?? 'null')}</span>
            <span data-testid="unread">{ctx?.panelUnreadCount ?? 0}</span>
            <span data-testid="permission">{ctx?.permission ?? 'none'}</span>
            <span data-testid="supported">{String(ctx?.isSupported ?? false)}</span>
            <span data-testid="community-unread">{JSON.stringify(ctx?.communityUnreadById ?? {})}</span>
            <span data-testid="muted">{JSON.stringify(ctx?.mutedChats ?? {})}</span>
            <button data-testid="toggle" onClick={ctx?.toggleNotifications}>Toggle</button>
            <button data-testid="clear-panel" onClick={ctx?.clearPanelNotificationsUnread}>Clear</button>
            <button data-testid="mark-one" onClick={ctx?.markOnePanelNotificationRead}>Mark One</button>
            <button data-testid="toggle-mute-private" onClick={() => ctx?.toggleChatMuted('private', '5')}>Mute Private</button>
            <button data-testid="toggle-mute-community" onClick={() => ctx?.toggleChatMuted('community', '10')}>Mute Community</button>
            <button data-testid="inc-community" onClick={() => ctx?.incrementCommunityUnread('10')}>Inc Community</button>
            <button data-testid="clear-community" onClick={() => ctx?.clearCommunityUnread('10')}>Clear Community</button>
            <button data-testid="set-panel" onClick={() => ctx?.setPanelNotificationsUnreadCount(7)}>Set Panel 7</button>
            <span data-testid="is-muted-private">{String(ctx?.isChatMuted('private', '5'))}</span>
        </div>
    );
}

const renderWithProvider = () => {
    return render(
        <MemoryRouter>
            <NotificationProvider>
                <TestConsumer />
            </NotificationProvider>
        </MemoryRouter>
    );
};

// Helper: get the registered socket handler for a given event
const getSocketHandler = (eventName) => {
    const call = mockSocketOn.mock.calls.find(c => c[0] === eventName);
    expect(call).toBeTruthy();
    return call[1];
};

describe('NotificationContext', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        localStorage.clear();
    });

    test('provides context values', async () => {
        await act(async () => { renderWithProvider(); });

        expect(screen.getByTestId('enabled').textContent).toBe('false');
        expect(screen.getByTestId('unread').textContent).toBe('0');
        expect(screen.getByTestId('permission').textContent).toBe('granted');
        expect(screen.getByTestId('supported').textContent).toBe('true');
    });

    test('toggleNotifications toggles value', async () => {
        await act(async () => { renderWithProvider(); });
        expect(screen.getByTestId('enabled').textContent).toBe('false');

        await act(async () => { screen.getByTestId('toggle').click(); });
        expect(screen.getByTestId('enabled').textContent).toBe('true');
    });

    test('clearPanelNotificationsUnread resets count', async () => {
        await act(async () => { renderWithProvider(); });

        await act(async () => { screen.getByTestId('clear-panel').click(); });
        expect(screen.getByTestId('unread').textContent).toBe('0');
    });

    test('markOnePanelNotificationRead decrements', async () => {
        await act(async () => { renderWithProvider(); });

        await act(async () => { screen.getByTestId('mark-one').click(); });
        expect(screen.getByTestId('unread').textContent).toBe('0');
    });

    test('reads from localStorage', async () => {
        localStorage.setItem('notificationsEnabled', 'true');
        await act(async () => { renderWithProvider(); });
        expect(screen.getByTestId('enabled').textContent).toBe('true');
    });

    test('useNotificationContext returns null outside provider', () => {
        function Alone() {
            const ctx = useNotificationContext();
            return <span data-testid="val">{ctx === null ? 'null' : 'has value'}</span>;
        }

        render(<MemoryRouter><Alone /></MemoryRouter>);
        expect(screen.getByTestId('val').textContent).toBe('null');
    });

    test('toggleChatMuted toggles private chat mute', async () => {
        await act(async () => { renderWithProvider(); });

        expect(screen.getByTestId('is-muted-private').textContent).toBe('false');

        await act(async () => { screen.getByTestId('toggle-mute-private').click(); });
        expect(screen.getByTestId('is-muted-private').textContent).toBe('true');

        await act(async () => { screen.getByTestId('toggle-mute-private').click(); });
        expect(screen.getByTestId('is-muted-private').textContent).toBe('false');
    });

    test('context exposes community unread functions', async () => {
        await act(async () => { renderWithProvider(); });
        expect(screen.getByTestId('inc-community')).toBeInTheDocument();
        expect(screen.getByTestId('clear-community')).toBeInTheDocument();
        expect(screen.getByTestId('community-unread').textContent).toBe('{}');
    });

    test('incrementCommunityUnread increments via button', async () => {
        await act(async () => { renderWithProvider(); });

        await act(async () => { screen.getByTestId('inc-community').click(); });

        const unread = JSON.parse(screen.getByTestId('community-unread').textContent);
        expect(unread['10']).toBe(1);
    });

    test('setPanelNotificationsUnreadCount sets count', async () => {
        await act(async () => { renderWithProvider(); });

        await act(async () => { screen.getByTestId('set-panel').click(); });
        expect(screen.getByTestId('unread').textContent).toBe('7');
    });

    test('registers socket event listeners', async () => {
        await act(async () => { renderWithProvider(); });

        const registeredEvents = mockSocketOn.mock.calls.map(c => c[0]);
        expect(registeredEvents).toContain('dm_message');
        expect(registeredEvents).toContain('solicitud_contratacion');
        expect(registeredEvents).toContain('solicitud_contratacion_respuesta');
        expect(registeredEvents).toContain('solicitud_contratacion_pagada');
        expect(registeredEvents).toContain('alerts_count');
        expect(registeredEvents).toContain('community_message');
        expect(registeredEvents).toContain('notificaciones');
    });

    test('saves notificationsEnabled to localStorage', async () => {
        await act(async () => { renderWithProvider(); });

        await act(async () => { screen.getByTestId('toggle').click(); });
        expect(localStorage.getItem('notificationsEnabled')).toBe('true');
    });

    test('persists mutedChats in localStorage per user', async () => {
        await act(async () => { renderWithProvider(); });

        await act(async () => { screen.getByTestId('toggle-mute-community').click(); });

        const key = `mutedChatNotificationsByUser:${mockUser.id}`;
        const stored = JSON.parse(localStorage.getItem(key));
        expect(stored.community['10']).toBe(true);
    });

    test('double toggle enables then disables notifications', async () => {
        await act(async () => { renderWithProvider(); });
        expect(screen.getByTestId('enabled').textContent).toBe('false');

        await act(async () => { screen.getByTestId('toggle').click(); });
        expect(screen.getByTestId('enabled').textContent).toBe('true');

        await act(async () => { screen.getByTestId('toggle').click(); });
        expect(screen.getByTestId('enabled').textContent).toBe('false');
    });

    test('clearCommunityUnread resets community unread count via socket then clear', async () => {
        await act(async () => { renderWithProvider(); });

        const handler = getSocketHandler('community_message');

        await act(async () => {
            handler({
                comunidadId: 10,
                usuarioId: 999,
                usuarioNombre: 'Other',
                contenido: 'msg',
                comunidadNombre: 'C',
            });
        });

        const unread = JSON.parse(screen.getByTestId('community-unread').textContent);
        expect(unread['10']).toBe(1);

        await act(async () => { screen.getByTestId('clear-community').click(); });

        const cleared = JSON.parse(screen.getByTestId('community-unread').textContent);
        expect(cleared['10']).toBe(0);
    });

    test('community_message handler increments community unread', async () => {
        await act(async () => { renderWithProvider(); });

        const handler = getSocketHandler('community_message');

        await act(async () => {
            handler({
                comunidadId: 42,
                usuarioId: 999,
                usuarioNombre: 'Other User',
                contenido: 'Hello community',
                comunidadNombre: 'Test Comm',
            });
        });

        const unread = JSON.parse(screen.getByTestId('community-unread').textContent);
        expect(unread['42']).toBe(1);
    });

    // NEW: muted community does NOT increment badge
    test('community_message does not increment unread when chat is muted', async () => {
        await act(async () => { renderWithProvider(); });

        // Mute community 10
        await act(async () => { screen.getByTestId('toggle-mute-community').click(); });

        const handler = getSocketHandler('community_message');

        await act(async () => {
            handler({
                comunidadId: 10,
                usuarioId: 999,
                usuarioNombre: 'Other',
                contenido: 'msg',
                comunidadNombre: 'C',
            });
        });

        const unread = JSON.parse(screen.getByTestId('community-unread').textContent);
        expect(unread['10']).toBeUndefined();
    });

    // NEW: mention in muted community STILL increments badge
    test('community_message increments unread for mention even when muted', async () => {
        await act(async () => { renderWithProvider(); });

        // Mute community 10
        await act(async () => { screen.getByTestId('toggle-mute-community').click(); });

        const handler = getSocketHandler('community_message');

        await act(async () => {
            handler({
                comunidadId: 10,
                usuarioId: 999,
                usuarioNombre: 'Other',
                contenido: '@Test User hey!',   // mentions mockUser.nombre
                comunidadNombre: 'C',
            });
        });

        const unread = JSON.parse(screen.getByTestId('community-unread').textContent);
        expect(unread['10']).toBe(1);
    });

    test('alerts_count handler updates panel unread count with number', async () => {
        await act(async () => { renderWithProvider(); });

        const handler = getSocketHandler('alerts_count');

        await act(async () => { handler(5); });
        expect(screen.getByTestId('unread').textContent).toBe('5');
    });

    test('alerts_count handler updates panel with object containing total', async () => {
        await act(async () => { renderWithProvider(); });

        const handler = getSocketHandler('alerts_count');

        await act(async () => { handler({ total: 3 }); });
        expect(screen.getByTestId('unread').textContent).toBe('3');
    });

    test('notificaciones handler processes notification without crash', async () => {
        await act(async () => { renderWithProvider(); });

        const handler = getSocketHandler('notificaciones');

        await act(async () => {
            handler({ tipo: 'GENERAL', leida: false, mensaje: 'New notification' });
        });

        expect(screen.getByTestId('enabled')).toBeInTheDocument();
    });

    test('handles null payload gracefully for socket handlers', async () => {
        await act(async () => { renderWithProvider(); });

        await act(async () => {
            getSocketHandler('community_message')(null);
            getSocketHandler('alerts_count')(null);
            getSocketHandler('notificaciones')(null);
            getSocketHandler('dm_message')(null);
        });

        expect(screen.getByTestId('enabled')).toBeInTheDocument();
    });

    test('community_message does not increment unread for own messages', async () => {
        await act(async () => { renderWithProvider(); });

        const handler = getSocketHandler('community_message');

        await act(async () => {
            handler({
                comunidadId: 50,
                usuarioId: 1, // same as mockUser.id
                usuarioNombre: 'Test User',
                contenido: 'My own message',
                comunidadNombre: 'My Comm',
            });
        });

        const unread = JSON.parse(screen.getByTestId('community-unread').textContent);
        expect(unread['50']).toBeUndefined();
    });

    test('muting community chat persists correctly', async () => {
        await act(async () => { renderWithProvider(); });

        await act(async () => { screen.getByTestId('toggle-mute-community').click(); });

        const key = `mutedChatNotificationsByUser:${mockUser.id}`;
        const stored = JSON.parse(localStorage.getItem(key));
        expect(stored.community['10']).toBe(true);

        await act(async () => { screen.getByTestId('toggle-mute-community').click(); });

        const storedAfter = JSON.parse(localStorage.getItem(key));
        expect(storedAfter.community['10']).toBe(false);
    });

    test('markOnePanelNotificationRead decrements but not below 0', async () => {
        await act(async () => { renderWithProvider(); });

        await act(async () => { screen.getByTestId('mark-one').click(); });
        expect(screen.getByTestId('unread').textContent).toBe('0');
    });

    test('setPanelNotificationsUnreadCount ignores negative values', async () => {
        await act(async () => { renderWithProvider(); });

        await act(async () => { screen.getByTestId('set-panel').click(); });
        expect(screen.getByTestId('unread').textContent).toBe('7');
    });
});
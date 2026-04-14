const mockGet = jest.fn();
const mockPatch = jest.fn();

jest.mock('./axiosConfig', () => ({
    __esModule: true,
    default: {
        get: (...args) => mockGet(...args),
        patch: (...args) => mockPatch(...args),
    },
}));

const {
    getAllEventAlerts,
    markEventAlertAsRead,
    markAllEventAlertsAsRead,
    getAllUserNotifications,
    markUserNotificationAsRead,
    markAllUserNotificationsAsRead,
} = require('./notificationService');

describe('notificationService', () => {
    beforeEach(() => jest.clearAllMocks());

    test('getAllEventAlerts', async () => {
        mockGet.mockResolvedValue({ data: [{ id: 1 }] });
        const result = await getAllEventAlerts();
        expect(mockGet).toHaveBeenCalledWith('/api/v1/my-events/alerts/all');
        expect(result).toEqual([{ id: 1 }]);
    });

    test('markEventAlertAsRead', async () => {
        mockPatch.mockResolvedValue({ data: { read: true } });
        const result = await markEventAlertAsRead(5);
        expect(mockPatch).toHaveBeenCalledWith('/api/v1/my-events/alerts/5/read');
        expect(result).toEqual({ read: true });
    });

    test('markAllEventAlertsAsRead', async () => {
        mockPatch.mockResolvedValue({});
        await markAllEventAlertsAsRead();
        expect(mockPatch).toHaveBeenCalledWith('/api/v1/my-events/alerts/read-all');
    });

    test('getAllUserNotifications', async () => {
        mockGet.mockResolvedValue({ data: [{ id: 1 }] });
        const result = await getAllUserNotifications();
        expect(mockGet).toHaveBeenCalledWith('/api/v1/notifications');
        expect(result).toEqual([{ id: 1 }]);
    });

    test('markUserNotificationAsRead', async () => {
        mockPatch.mockResolvedValue({ data: { read: true } });
        const result = await markUserNotificationAsRead(5);
        expect(mockPatch).toHaveBeenCalledWith('/api/v1/notifications/5/read');
        expect(result).toEqual({ read: true });
    });

    test('markAllUserNotificationsAsRead success', async () => {
        mockPatch.mockResolvedValue({ data: { success: true } });
        const result = await markAllUserNotificationsAsRead();
        expect(mockPatch).toHaveBeenCalledWith('/api/v1/notifications/read-all');
        expect(result).toEqual({ success: true });
    });

    test('markAllUserNotificationsAsRead fallback individual', async () => {
        mockPatch.mockRejectedValueOnce(new Error('Not found'));
        mockGet.mockResolvedValue({ data: [{ id: 1, leida: false }, { id: 2, leida: true }] });
        mockPatch.mockResolvedValue({ data: {} });

        const result = await markAllUserNotificationsAsRead();
        expect(result).toEqual(expect.objectContaining({ mode: 'fallback', updated: 1 }));
    });

    test('markAllUserNotificationsAsRead fallback with all read', async () => {
        mockPatch.mockRejectedValueOnce(new Error('Not found'));
        mockGet.mockResolvedValue({ data: [{ id: 1, leida: true }] });

        const result = await markAllUserNotificationsAsRead();
        expect(result).toEqual(expect.objectContaining({ mode: 'fallback', updated: 0 }));
    });
});

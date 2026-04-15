const mockGet = jest.fn();
const mockPost = jest.fn();
const mockPut = jest.fn();
const mockDelete = jest.fn();

jest.mock('./axiosConfig', () => ({
    __esModule: true,
    default: {
        get: (...args) => mockGet(...args),
        post: (...args) => mockPost(...args),
        put: (...args) => mockPut(...args),
        delete: (...args) => mockDelete(...args),
    },
}));

const {
    listMapEvents,
    createEvent,
    getEventById,
    updateEvent,
    listCommunityEvents,
    listPublicEvents,
    cancelEvent,
    attendEvent,
    getMyAttendance,
    cancelAttendance,
    listAttendees,
    getConfirmedAttendees,
    getAttendanceCount,
    linkClassroomTask,
    unlinkClassroomTask,
} = require('./eventEndpoints');

describe('eventEndpoints', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        localStorage.setItem('userId', '42');
    });

    test('listMapEvents without params', async () => {
        mockGet.mockResolvedValue({ data: [] });
        await listMapEvents();
        expect(mockGet).toHaveBeenCalledWith('/api/v1/events/map', { params: {} });
    });

    test('listMapEvents with params', async () => {
        mockGet.mockResolvedValue({ data: [] });
        await listMapEvents({ lat: 37, lon: -5, radioKm: 10 });
        expect(mockGet).toHaveBeenCalledWith('/api/v1/events/map', { params: { lat: 37, lon: -5, radioKm: 10 } });
    });

    test('createEvent with communityId', async () => {
        mockPost.mockResolvedValue({ data: { id: 1 } });
        await createEvent(5, { titulo: 'Test' });
        expect(mockPost).toHaveBeenCalledWith('/api/v1/communities/5/events', expect.objectContaining({ communityId: 5 }));
    });

    test('createEvent throws without communityId', async () => {
        await expect(createEvent(null, {})).rejects.toThrow('communityId es obligatorio');
    });

    test('createEvent fallback on 404', async () => {
        mockPost.mockRejectedValueOnce({ response: { status: 404 } });
        mockPost.mockResolvedValueOnce({ data: { id: 1 } });
        await createEvent(5, { titulo: 'Test' });
        expect(mockPost).toHaveBeenCalledTimes(2);
    });

    test('createEvent fallback on 405', async () => {
        mockPost.mockRejectedValueOnce({ response: { status: 405 } });
        mockPost.mockResolvedValueOnce({ data: { id: 2 } });

        const result = await createEvent(5, { titulo: 'Fallback405' });

        expect(mockPost).toHaveBeenNthCalledWith(1, '/api/v1/communities/5/events', expect.any(Object));
        expect(mockPost).toHaveBeenNthCalledWith(2, '/api/v1/events/5', expect.any(Object));
        expect(result).toEqual({ id: 2 });
    });

    test('createEvent rethrows non-fallback errors', async () => {
        const error = { response: { status: 500 }, message: 'boom' };
        mockPost.mockRejectedValueOnce(error);

        await expect(createEvent(5, { titulo: 'Explota' })).rejects.toBe(error);
        expect(mockPost).toHaveBeenCalledTimes(1);
    });

    test('getEventById', async () => {
        mockGet.mockResolvedValue({ data: { id: 1 } });
        const result = await getEventById(1);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/events/1');
        expect(result).toEqual({ id: 1 });
    });

    test('updateEvent', async () => {
        mockPut.mockResolvedValue({ data: {} });
        await updateEvent(1, { titulo: 'Updated' });
        expect(mockPut).toHaveBeenCalledWith('/api/v1/events/1', null, { params: { titulo: 'Updated' } });
    });

    test('listCommunityEvents', async () => {
        mockGet.mockResolvedValue({ data: [] });
        await listCommunityEvents(5);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/communities/5/events', { params: {} });
    });

    test('listPublicEvents', async () => {
        mockGet.mockResolvedValue({ data: [] });
        await listPublicEvents();
        expect(mockGet).toHaveBeenCalledWith('/api/v1/events', { params: {} });
    });

    test('cancelEvent', async () => {
        mockPost.mockResolvedValue({ data: {} });
        await cancelEvent(1, 'reason');
        expect(mockPost).toHaveBeenCalledWith('/api/v1/events/1/cancel', null, { params: { motivo: 'reason' } });
    });

    test('attendEvent', async () => {
        mockPost.mockResolvedValue({ data: {} });
        await attendEvent(1);
        expect(mockPost).toHaveBeenCalledWith('/api/v1/events/1/attendance', null, { params: { usuarioId: '42' } });
    });

    test('getMyAttendance', async () => {
        mockGet.mockResolvedValue({ data: { confirmed: true } });
        const result = await getMyAttendance(1);
        expect(result).toEqual({ confirmed: true });
    });

    test('getMyAttendance returns null on 404', async () => {
        mockGet.mockRejectedValue({ response: { status: 404 } });
        const result = await getMyAttendance(1);
        expect(result).toBeNull();
    });

    test('getMyAttendance throws on other errors', async () => {
        mockGet.mockRejectedValue({ response: { status: 500 } });
        await expect(getMyAttendance(1)).rejects.toEqual({ response: { status: 500 } });
    });

    test('cancelAttendance', async () => {
        mockDelete.mockResolvedValue({ data: {} });
        await cancelAttendance(1);
        expect(mockDelete).toHaveBeenCalledWith('/api/v1/events/1/attendance/me', { params: { usuarioId: '42' } });
    });

    test('listAttendees', async () => {
        mockGet.mockResolvedValue({ data: [] });
        await listAttendees(1);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/events/1/attendance');
    });

    test('getConfirmedAttendees', async () => {
        mockGet.mockResolvedValue({ data: [] });
        await getConfirmedAttendees(1);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/events/1/attendance/confirmed');
    });

    test('getAttendanceCount', async () => {
        mockGet.mockResolvedValue({ data: 5 });
        const result = await getAttendanceCount(1);
        expect(result).toBe(5);
    });

    test('linkClassroomTask', async () => {
        mockPost.mockResolvedValue({ data: {} });
        await linkClassroomTask(1, { taskId: 't1' });
        expect(mockPost).toHaveBeenCalledWith('/api/v1/events/1/classroom-task', { taskId: 't1' });
    });

    test('unlinkClassroomTask', async () => {
        mockDelete.mockResolvedValue({ data: {} });
        await unlinkClassroomTask(1);
        expect(mockDelete).toHaveBeenCalledWith('/api/v1/events/1/classroom-task');
    });
});

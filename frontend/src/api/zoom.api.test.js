const mockGet = jest.fn();
const mockPost = jest.fn();
const mockDelete = jest.fn();

jest.mock('./client', () => ({
    apiClient: {
        get: (...args) => mockGet(...args),
        post: (...args) => mockPost(...args),
        delete: (...args) => mockDelete(...args),
    },
    ApiError: class ApiError extends Error {
        constructor(status, message, details) {
            super(message);
            this.status = status;
            this.details = details;
        }
    },
}));

const { ZoomApi } = require('./zoom.api');

describe('ZoomApi', () => {
    beforeEach(() => jest.clearAllMocks());

    test('createOrGetMeeting', () => {
        ZoomApi.createOrGetMeeting(5, { topic: 'Test' });
        expect(mockPost).toHaveBeenCalledWith('/api/v1/zoom/communities/5/meeting', { topic: 'Test' });
    });

    test('getActiveMeeting', () => {
        ZoomApi.getActiveMeeting(5);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/zoom/communities/5/meeting');
    });

    test('joinMeeting', () => {
        ZoomApi.joinMeeting(5);
        expect(mockPost).toHaveBeenCalledWith('/api/v1/zoom/communities/5/meeting/join');
    });

    test('listParticipants', () => {
        ZoomApi.listParticipants(5);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/zoom/communities/5/meeting/participants');
    });

    test('endMeeting', () => {
        ZoomApi.endMeeting(5);
        expect(mockDelete).toHaveBeenCalledWith('/api/v1/zoom/communities/5/meeting');
    });

    test('listMeetings', () => {
        ZoomApi.listMeetings(5);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/zoom/communities/5/meetings');
    });

    test('listRecordings', () => {
        ZoomApi.listRecordings(5);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/zoom/communities/5/recordings');
    });

    test('getRecording', () => {
        ZoomApi.getRecording(5, 'r1');
        expect(mockGet).toHaveBeenCalledWith('/api/v1/zoom/communities/5/recordings/r1');
    });

    test('getMyActiveCalls', () => {
        ZoomApi.getMyActiveCalls();
        expect(mockGet).toHaveBeenCalledWith('/api/v1/zoom/me/calls');
    });

    test('createOrGetEventMeeting', () => {
        ZoomApi.createOrGetEventMeeting(10);
        expect(mockPost).toHaveBeenCalledWith('/api/v1/zoom/events/10/meeting', null);
    });

    test('getActiveEventMeeting', () => {
        ZoomApi.getActiveEventMeeting(10);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/zoom/events/10/meeting');
    });

    test('joinEventMeeting', () => {
        ZoomApi.joinEventMeeting(10);
        expect(mockPost).toHaveBeenCalledWith('/api/v1/zoom/events/10/meeting/join');
    });

    test('listEventParticipants', () => {
        ZoomApi.listEventParticipants(10);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/zoom/events/10/meeting/participants');
    });

    test('endEventMeeting', () => {
        ZoomApi.endEventMeeting(10);
        expect(mockDelete).toHaveBeenCalledWith('/api/v1/zoom/events/10/meeting');
    });
});

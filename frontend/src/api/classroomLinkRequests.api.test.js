const mockGet = jest.fn();
const mockPost = jest.fn();

jest.mock('./client', () => ({
    apiClient: {
        get: (...args) => mockGet(...args),
        post: (...args) => mockPost(...args),
    },
}));

const { classroomLinkRequestsApi } = require('./classroomLinkRequests.api');

describe('classroomLinkRequestsApi', () => {
    beforeEach(() => jest.clearAllMocks());

    test('myPending', () => {
        classroomLinkRequestsApi.myPending();
        expect(mockGet).toHaveBeenCalledWith('/api/v1/classroom-link-requests/me');
    });

    test('complete', () => {
        classroomLinkRequestsApi.complete(5, { cursoId: 'c1', nombreCurso: 'Math' });
        expect(mockPost).toHaveBeenCalledWith('/api/v1/classroom-link-requests/5/link', { cursoId: 'c1', nombreCurso: 'Math' });
    });
});

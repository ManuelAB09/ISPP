const mockGet = jest.fn();
const mockPost = jest.fn();

jest.mock('./axiosConfig', () => ({
    __esModule: true,
    default: {
        get: (...args) => mockGet(...args),
        post: (...args) => mockPost(...args),
    },
}));

const { getAnnouncementComments, postAnnouncementComment } = require('./announcementComments');

describe('announcementComments', () => {
    beforeEach(() => jest.clearAllMocks());

    test('getAnnouncementComments', async () => {
        mockGet.mockResolvedValue({ data: [{ id: 1, texto: 'Hi' }] });
        const result = await getAnnouncementComments(5);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/announcements/5/comments');
        expect(result).toEqual([{ id: 1, texto: 'Hi' }]);
    });

    test('postAnnouncementComment', async () => {
        mockPost.mockResolvedValue({ data: { id: 2, texto: 'Reply' } });
        const result = await postAnnouncementComment(5, 'Reply');
        expect(mockPost).toHaveBeenCalledWith('/api/v1/announcements/5/comments', { texto: 'Reply' });
        expect(result).toEqual({ id: 2, texto: 'Reply' });
    });
});

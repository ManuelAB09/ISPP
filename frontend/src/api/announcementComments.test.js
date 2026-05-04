const mockGet = jest.fn();
const mockPost = jest.fn();
const mockDelete = jest.fn();

jest.mock('./axiosConfig', () => ({
    __esModule: true,
    default: {
        get: (...args) => mockGet(...args),
        post: (...args) => mockPost(...args),
        delete: (...args) => mockDelete(...args),
    },
}));

const { getAnnouncementComments, postAnnouncementComment, deleteAnnouncementComment } = require('./announcementComments');

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

    test('deleteAnnouncementComment calls correct endpoint', async () => {
        mockDelete.mockResolvedValue({});
        await deleteAnnouncementComment(5, 99);
        expect(mockDelete).toHaveBeenCalledWith('/api/v1/announcements/5/comments/99');
    });

    test('deleteAnnouncementComment resolves without return value', async () => {
        mockDelete.mockResolvedValue({});
        const result = await deleteAnnouncementComment(5, 99);
        expect(result).toBeUndefined();
    });

    test('deleteAnnouncementComment rejects on API error', async () => {
        mockDelete.mockRejectedValue(new Error('Server error'));
        await expect(deleteAnnouncementComment(5, 99)).rejects.toThrow('Server error');
    });
});
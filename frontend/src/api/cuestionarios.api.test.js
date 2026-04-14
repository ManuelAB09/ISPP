const mockGet = jest.fn();
const mockPost = jest.fn();
const mockPut = jest.fn();

jest.mock('./axiosConfig', () => ({
    __esModule: true,
    default: {
        get: (...args) => mockGet(...args),
        post: (...args) => mockPost(...args),
        put: (...args) => mockPut(...args),
    },
}));

const { cuestionariosApi } = require('./cuestionarios.api');

describe('cuestionariosApi', () => {
    beforeEach(() => jest.clearAllMocks());

    test('createCuestionario', async () => {
        mockPost.mockResolvedValue({ data: { id: 1 } });
        const result = await cuestionariosApi.createCuestionario({ titulo: 'Test' });
        expect(mockPost).toHaveBeenCalledWith('/api/v1/cuestionarios', { titulo: 'Test' });
        expect(result).toEqual({ id: 1 });
    });

    test('listMine', async () => {
        mockGet.mockResolvedValue({ data: [{ id: 1 }] });
        const result = await cuestionariosApi.listMine();
        expect(mockGet).toHaveBeenCalledWith('/api/v1/cuestionarios/mine');
        expect(result).toEqual([{ id: 1 }]);
    });

    test('listPublic', async () => {
        mockGet.mockResolvedValue({ data: [] });
        await cuestionariosApi.listPublic();
        expect(mockGet).toHaveBeenCalledWith('/api/v1/cuestionarios/public');
    });

    test('listAssigned', async () => {
        mockGet.mockResolvedValue({ data: [] });
        await cuestionariosApi.listAssigned();
        expect(mockGet).toHaveBeenCalledWith('/api/v1/cuestionarios/assigned');
    });

    test('listPublicByUserId', async () => {
        mockGet.mockResolvedValue({ data: [] });
        await cuestionariosApi.listPublicByUserId(5);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/cuestionarios/user/5/public');
    });

    test('listByCommunity', async () => {
        mockGet.mockResolvedValue({ data: [] });
        await cuestionariosApi.listByCommunity(10);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/cuestionarios/community/10');
    });

    test('getById', async () => {
        mockGet.mockResolvedValue({ data: { id: 1 } });
        // eslint-disable-next-line testing-library/no-await-sync-query
        const result = await cuestionariosApi.getById(1);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/cuestionarios/1');
        expect(result).toEqual({ id: 1 });
    });

    test('getPreview', async () => {
        mockGet.mockResolvedValue({ data: { id: 1 } });
        await cuestionariosApi.getPreview(1);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/cuestionarios/1/preview');
    });

    test('getResolver', async () => {
        mockGet.mockResolvedValue({ data: { id: 1 } });
        await cuestionariosApi.getResolver(1);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/cuestionarios/1/resolver');
    });

    test('publishCuestionario', async () => {
        mockPut.mockResolvedValue({ data: { published: true } });
        await cuestionariosApi.publishCuestionario(1);
        expect(mockPut).toHaveBeenCalledWith('/api/v1/cuestionarios/1/publish');
    });

    test('draftCuestionario', async () => {
        mockPut.mockResolvedValue({ data: { draft: true } });
        await cuestionariosApi.draftCuestionario(1);
        expect(mockPut).toHaveBeenCalledWith('/api/v1/cuestionarios/1/draft');
    });

    test('submitAttempt', async () => {
        mockPost.mockResolvedValue({ data: { score: 100 } });
        const answers = [{ questionId: 1, answer: 'A' }];
        const result = await cuestionariosApi.submitAttempt(1, answers);
        expect(mockPost).toHaveBeenCalledWith('/api/v1/cuestionarios/1/submit', answers);
        expect(result).toEqual({ score: 100 });
    });
});

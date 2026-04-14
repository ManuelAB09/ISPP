const mockGet = jest.fn();

jest.mock('./axiosConfig', () => ({
    __esModule: true,
    default: {
        get: mockGet,
    },
}));

const { usersApi } = require('./users.api');

describe('usersApi', () => {
    beforeEach(() => jest.clearAllMocks());

    test('searchUsers with search term', async () => {
        mockGet.mockResolvedValue({ data: [
            { id: 1, nombre: 'Juan', email: 'juan@example.com', foto: null },
            { id: 2, nombre: 'Juana', email: 'juana@example.com', foto: null }
        ]});

        const result = await usersApi.searchUsers('juan');

        expect(mockGet).toHaveBeenCalledWith('/api/v1/users/search', {
            params: { search: 'juan' }
        });
        expect(result).toHaveLength(2);
        expect(result[0].nombre).toBe('Juan');
    });

    test('searchUsers with empty search returns empty array', async () => {
        mockGet.mockResolvedValue({ data: [] });

        const result = await usersApi.searchUsers('xyz');

        expect(mockGet).toHaveBeenCalledWith('/api/v1/users/search', {
            params: { search: 'xyz' }
        });
        expect(result).toHaveLength(0);
    });

    test('searchUsers filters out current user', async () => {
        mockGet.mockResolvedValue({ data: [
            { id: 2, nombre: 'Alumno2', email: 'alumno2@example.com', foto: null }
        ]});

        const result = await usersApi.searchUsers('alumno');

        expect(result).toHaveLength(1);
        expect(result[0].email).toBe('alumno2@example.com');
    });
});

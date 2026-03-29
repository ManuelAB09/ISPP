const mockGet = jest.fn();
const mockPost = jest.fn();
const mockPut = jest.fn();
const mockDelete = jest.fn();

jest.mock('./client', () => ({
    apiClient: {
        get: (...args) => mockGet(...args),
        post: (...args) => mockPost(...args),
        put: (...args) => mockPut(...args),
        delete: (...args) => mockDelete(...args),
    },
}));

const { paymentsApi } = require('./payments.api');

describe('paymentsApi', () => {
    beforeEach(() => jest.clearAllMocks());

    test('getHistory without params', () => {
        paymentsApi.getHistory();
        expect(mockGet).toHaveBeenCalledWith('/api/v1/payments/history');
    });

    test('getHistory with params', () => {
        paymentsApi.getHistory({ tipo: 'SUBSCRIPTION', page: 0, size: 10 });
        expect(mockGet).toHaveBeenCalledWith(expect.stringContaining('tipo=SUBSCRIPTION'));
        expect(mockGet).toHaveBeenCalledWith(expect.stringContaining('page=0'));
    });

    test('getTransaction', () => {
        paymentsApi.getTransaction(42);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/payments/42');
    });
});

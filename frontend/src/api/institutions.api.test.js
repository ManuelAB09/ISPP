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

const { institutionsApi } = require('./institutions.api');

describe('institutionsApi', () => {
    beforeEach(() => jest.clearAllMocks());

    test('create', () => {
        institutionsApi.create({ nombre: 'Test' });
        expect(mockPost).toHaveBeenCalledWith('/api/v1/institutions', { nombre: 'Test' });
    });

    test('getById', () => {
        institutionsApi.getById(1);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/institutions/1');
    });

    test('update', () => {
        institutionsApi.update(1, { nombre: 'New' });
        expect(mockPut).toHaveBeenCalledWith('/api/v1/institutions/1', { nombre: 'New' });
    });

    test('hirePlan', () => {
        institutionsApi.hirePlan(1, { tipoPlan: 'PREMIUM' });
        expect(mockPost).toHaveBeenCalledWith('/api/v1/institutions/1/plan', { tipoPlan: 'PREMIUM' });
    });

    test('cancelPlan', () => {
        institutionsApi.cancelPlan(1);
        expect(mockDelete).toHaveBeenCalledWith('/api/v1/institutions/1/plan');
    });

    test('getPlanStatus', () => {
        institutionsApi.getPlanStatus(1);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/institutions/1/plan/status');
    });

    test('verifySession', () => {
        institutionsApi.verifySession('sess_123');
        expect(mockPost).toHaveBeenCalledWith('/api/v1/institutions/verify-session', { sessionId: 'sess_123' });
    });

    test('createPlanPaymentIntent', () => {
        institutionsApi.createPlanPaymentIntent(1, { amount: 100 });
        expect(mockPost).toHaveBeenCalledWith('/api/v1/institutions/1/create-plan-payment-intent', { amount: 100 });
    });

    test('confirmPlanPayment', () => {
        institutionsApi.confirmPlanPayment('pi_123');
        expect(mockPost).toHaveBeenCalledWith('/api/v1/institutions/confirm-plan-payment', { paymentIntentId: 'pi_123' });
    });
});

const mockGet = jest.fn();
const mockPost = jest.fn();
const mockDelete = jest.fn();

jest.mock('./client', () => ({
    apiClient: {
        get: (...args) => mockGet(...args),
        post: (...args) => mockPost(...args),
        delete: (...args) => mockDelete(...args),
    },
}));

const { subscriptionsApi } = require('./subscriptions.api');

describe('subscriptionsApi', () => {
    beforeEach(() => jest.clearAllMocks());

    test('listPlans', () => {
        subscriptionsApi.listPlans();
        expect(mockGet).toHaveBeenCalledWith('/subscriptions/plans');
    });

    test('getMySubscription', () => {
        subscriptionsApi.getMySubscription();
        expect(mockGet).toHaveBeenCalledWith('/api/v1/subscriptions/me');
    });

    test('subscribe with defaults', () => {
        subscriptionsApi.subscribe();
        expect(mockPost).toHaveBeenCalledWith('/api/v1/subscriptions/me', {
            planId: 'PREMIUM',
            aceptarTerminos: true,
            periodo: 'mensual',
        });
    });

    test('subscribe with custom params', () => {
        subscriptionsApi.subscribe({ planId: 'PRO', periodo: 'anual' });
        expect(mockPost).toHaveBeenCalledWith('/api/v1/subscriptions/me', expect.objectContaining({ planId: 'PRO', periodo: 'anual' }));
    });

    test('cancelSubscription', () => {
        subscriptionsApi.cancelSubscription();
        expect(mockDelete).toHaveBeenCalledWith('/api/v1/subscriptions/me');
    });

    test('confirmPayment', () => {
        subscriptionsApi.confirmPayment();
        expect(mockPost).toHaveBeenCalledWith('/api/v1/subscriptions/me/confirm-payment');
    });

    test('verifySession', () => {
        subscriptionsApi.verifySession('sess_123');
        expect(mockPost).toHaveBeenCalledWith('/api/v1/subscriptions/me/verify-session', { sessionId: 'sess_123' });
    });

    test('createPaymentIntent', () => {
        subscriptionsApi.createPaymentIntent();
        expect(mockPost).toHaveBeenCalledWith('/api/v1/subscriptions/me/create-payment-intent', expect.objectContaining({ planId: 'PREMIUM' }));
    });

    test('confirmEmbeddedPayment', () => {
        subscriptionsApi.confirmEmbeddedPayment('pi_123');
        expect(mockPost).toHaveBeenCalledWith('/api/v1/subscriptions/me/confirm-embedded-payment', { paymentIntentId: 'pi_123' });
    });
});

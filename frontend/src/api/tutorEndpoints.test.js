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

const {
    getVerifiedTutors,
    createTutorProfile,
    getTutorById,
    updateTutorProfile,
    getTutorVerificationStatus,
    requestTutorVerification,
    verificarTutor,
    createVerificationPaymentIntent,
    confirmVerificationPayment,
    getMyTutorProfiles,
    getMyTutorProfile,
    iniciarOnboardingStripe,
    obtenerEstadoStripeConnect,
    obtenerGananciasTutor,
} = require('./tutorEndpoints');

describe('tutorEndpoints', () => {
    beforeEach(() => jest.clearAllMocks());

    test('getVerifiedTutors without params', () => {
        getVerifiedTutors();
        expect(mockGet).toHaveBeenCalledWith('/api/v1/tutors');
    });

    test('getVerifiedTutors with params', () => {
        getVerifiedTutors({ especialidad: 'Math', tarifaMin: 10 });
        expect(mockGet).toHaveBeenCalledWith(expect.stringContaining('especialidad=Math'));
    });

    test('createTutorProfile', () => {
        createTutorProfile({ especialidades: ['Math'] });
        expect(mockPost).toHaveBeenCalledWith('/api/v1/tutors', { especialidades: ['Math'] });
    });

    test('getTutorById', () => {
        getTutorById(5);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/tutors/5');
    });

    test('updateTutorProfile', () => {
        updateTutorProfile(5, { bio: 'New' });
        expect(mockPut).toHaveBeenCalledWith('/api/v1/tutors/me', { bio: 'New' });
    });

    test('getTutorVerificationStatus', () => {
        getTutorVerificationStatus(5);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/tutors/me/5/verification-status');
    });

    test('requestTutorVerification', () => {
        requestTutorVerification(5);
        expect(mockPost).toHaveBeenCalledWith('/api/v1/tutors/me/5/verification', {});
    });

    test('verificarTutor', () => {
        verificarTutor();
        expect(mockPost).toHaveBeenCalledWith('/api/v1/tutors/me/verificar', {});
    });

    test('createVerificationPaymentIntent', () => {
        createVerificationPaymentIntent();
        expect(mockPost).toHaveBeenCalledWith('/api/v1/tutors/me/create-verification-payment-intent', {});
    });

    test('confirmVerificationPayment', () => {
        confirmVerificationPayment('pi_123');
        expect(mockPost).toHaveBeenCalledWith('/api/v1/tutors/me/confirm-verification-payment', { paymentIntentId: 'pi_123' });
    });

    test('getMyTutorProfiles', () => {
        getMyTutorProfiles();
        expect(mockGet).toHaveBeenCalledWith('/api/v1/tutors/me');
    });

    test('getMyTutorProfile', () => {
        getMyTutorProfile(5);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/tutors/me/5');
    });

    test('iniciarOnboardingStripe', () => {
        iniciarOnboardingStripe('http://return.url');
        expect(mockPost).toHaveBeenCalledWith('/api/v1/tutors/me/stripe-connect/onboarding', { returnUrl: 'http://return.url' });
    });

    test('obtenerEstadoStripeConnect', () => {
        obtenerEstadoStripeConnect();
        expect(mockGet).toHaveBeenCalledWith('/api/v1/tutors/me/stripe-connect/status');
    });

    test('obtenerGananciasTutor without params', () => {
        obtenerGananciasTutor();
        expect(mockGet).toHaveBeenCalledWith('/api/v1/tutors/me/earnings');
    });

    test('obtenerGananciasTutor with params', () => {
        obtenerGananciasTutor({ page: 0, size: 10 });
        expect(mockGet).toHaveBeenCalledWith(expect.stringContaining('page=0'));
    });
});

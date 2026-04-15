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

const { authApi } = require('./auth.api');

describe('authApi', () => {
    beforeEach(() => jest.clearAllMocks());

    test('login calls POST /api/v1/auth/login', () => {
        authApi.login({ email: 'a@b.com', password: '123' });
        expect(mockPost).toHaveBeenCalledWith('/api/v1/auth/login', { email: 'a@b.com', password: '123' });
    });

    test('register calls POST /api/v1/auth/register', () => {
        authApi.register({ email: 'a@b.com', password: '123', nombre: 'Test' });
        expect(mockPost).toHaveBeenCalledWith('/api/v1/auth/register', { email: 'a@b.com', password: '123', nombre: 'Test' });
    });

    test('refresh calls POST /api/v1/auth/refresh', () => {
        authApi.refresh('token123');
        expect(mockPost).toHaveBeenCalledWith('/api/v1/auth/refresh', { refreshToken: 'token123' });
    });

    test('getMe calls GET /api/v1/users/me', () => {
        authApi.getMe();
        expect(mockGet).toHaveBeenCalledWith('/api/v1/users/me');
    });

    test('getUserById calls correct URL', () => {
        authApi.getUserById(42);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/users/42');
    });

    test('updateMe calls PUT /api/v1/users/me', () => {
        authApi.updateMe({ nombre: 'New' });
        expect(mockPut).toHaveBeenCalledWith('/api/v1/users/me', { nombre: 'New' });
    });

    test('uploadProfilePhoto sends FormData', () => {
        const file = new File(['img'], 'test.png');
        authApi.uploadProfilePhoto(file);
        expect(mockPost).toHaveBeenCalledWith('/api/v1/users/me/photo', expect.any(FormData));
    });

    test('getProfileAvatars calls GET', () => {
        authApi.getProfileAvatars();
        expect(mockGet).toHaveBeenCalledWith('/api/v1/users/profile-avatars');
    });

    test('getUserPublicProfile calls correct URL', () => {
        authApi.getUserPublicProfile(99);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/users/99');
    });

    test('verifyEmail calls correct URL with encoded token', () => {
        authApi.verifyEmail('abc=123');
        expect(mockGet).toHaveBeenCalledWith('/api/v1/auth/verify?token=abc%3D123');
    });

    test('resendVerification calls POST', () => {
        authApi.resendVerification('a@b.com');
        expect(mockPost).toHaveBeenCalledWith('/api/v1/auth/resend-verification', { email: 'a@b.com' });
    });

    test('loginWithGoogle calls POST', () => {
        authApi.loginWithGoogle({ idToken: 'tok' });
        expect(mockPost).toHaveBeenCalledWith('/api/v1/auth/google', { idToken: 'tok' });
    });

    test('linkGoogle calls POST', () => {
        authApi.linkGoogle({ idToken: 'tok' });
        expect(mockPost).toHaveBeenCalledWith('/api/v1/auth/google/link', { idToken: 'tok' });
    });

    test('unlinkGoogle calls POST', () => {
        authApi.unlinkGoogle();
        expect(mockPost).toHaveBeenCalledWith('/api/v1/auth/google/unlink');
    });

    test('login2fa calls POST', () => {
        authApi.login2fa({ tempToken: 't', code: '123456' });
        expect(mockPost).toHaveBeenCalledWith('/api/v1/auth/2fa/login', { tempToken: 't', code: '123456' });
    });

    test('setup2fa calls POST', () => {
        authApi.setup2fa();
        expect(mockPost).toHaveBeenCalledWith('/api/v1/auth/2fa/setup');
    });

    test('enable2fa calls POST', () => {
        authApi.enable2fa('123456');
        expect(mockPost).toHaveBeenCalledWith('/api/v1/auth/2fa/enable', { code: '123456' });
    });

    test('disable2fa calls POST', () => {
        authApi.disable2fa('code');
        expect(mockPost).toHaveBeenCalledWith('/api/v1/auth/2fa/disable', { code: 'code' });
    });
});

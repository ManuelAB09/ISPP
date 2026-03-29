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

const { communitiesApi } = require('./communities.api');

describe('communitiesApi', () => {
    beforeEach(() => jest.clearAllMocks());

    test('list without params', () => {
        communitiesApi.list();
        expect(mockGet).toHaveBeenCalledWith('/api/v1/communities');
    });

    test('list with search param', () => {
        communitiesApi.list({ search: 'math' });
        expect(mockGet).toHaveBeenCalledWith(expect.stringContaining('search=math'));
    });

    test('listMine', () => {
        communitiesApi.listMine();
        expect(mockGet).toHaveBeenCalledWith('/api/v1/communities/members/me');
    });

    test('create', () => {
        communitiesApi.create({ nombre: 'Test' });
        expect(mockPost).toHaveBeenCalledWith('/api/v1/communities', { nombre: 'Test' });
    });

    test('getById', () => {
        communitiesApi.getById(5);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/communities/5');
    });

    test('join', () => {
        communitiesApi.join(5, 'PROFESOR');
        expect(mockPost).toHaveBeenCalledWith('/api/v1/communities/5/members', { rol: 'PROFESOR' });
    });

    test('getMembers', () => {
        communitiesApi.getMembers(5);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/communities/5/members');
    });

    test('upgradeCommunity', () => {
        communitiesApi.upgradeCommunity(5);
        expect(mockPost).toHaveBeenCalledWith('/api/v1/communities/5/upgrade', {});
    });

    test('hireTutor', () => {
        communitiesApi.hireTutor(5, 10, { modalidad: 'ONLINE' });
        expect(mockPost).toHaveBeenCalledWith('/api/v1/communities/5/tutor/10', { modalidad: 'ONLINE' });
    });

    test('getHiredTutor', () => {
        communitiesApi.getHiredTutor(5);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/communities/5/tutor');
    });

    test('cancelTutor', () => {
        communitiesApi.cancelTutor(5, 'motivo');
        expect(mockDelete).toHaveBeenCalledWith(expect.stringContaining('/api/v1/communities/5/tutor?motivo='));
    });

    test('getMyMembership returns null on 404', async () => {
        mockGet.mockRejectedValue({ status: 404 });
        const result = await communitiesApi.getMyMembership(5);
        expect(result).toBeNull();
    });

    test('getMyMembership throws on other errors', async () => {
        mockGet.mockRejectedValue({ status: 500 });
        await expect(communitiesApi.getMyMembership(5)).rejects.toEqual({ status: 500 });
    });

    test('leave', () => {
        communitiesApi.leave(5);
        expect(mockDelete).toHaveBeenCalledWith('/api/v1/communities/5/members/me');
    });

    test('expelMember', () => {
        communitiesApi.expelMember(5, 10);
        expect(mockDelete).toHaveBeenCalledWith('/api/v1/communities/5/members/10');
    });

    test('transferAdmin', () => {
        communitiesApi.transferAdmin(5, 10, 'ALUMNO');
        expect(mockPost).toHaveBeenCalledWith('/api/v1/communities/5/admin/transfer', { nuevoAdminId: 10, nuevoRolOrigen: 'ALUMNO' });
    });

    test('requestAccess', () => {
        communitiesApi.requestAccess(5, 'Please let me in');
        expect(mockPost).toHaveBeenCalledWith('/api/v1/communities/5/requests', { mensaje: 'Please let me in', rolDeseado: 'ALUMNO' });
    });

    test('respondToRequest', () => {
        communitiesApi.respondToRequest(5, 99, true);
        expect(mockPut).toHaveBeenCalledWith('/api/v1/communities/5/requests/99', { aceptado: true });
    });

    test('deleteCommunity', () => {
        communitiesApi.deleteCommunity(5);
        expect(mockDelete).toHaveBeenCalledWith('/api/v1/communities/5');
    });

    test('update', () => {
        communitiesApi.update(5, { nombre: 'New' });
        expect(mockPut).toHaveBeenCalledWith('/api/v1/communities/5', { nombre: 'New' });
    });

    test('uploadPhoto', () => {
        const fd = new FormData();
        communitiesApi.uploadPhoto(5, fd);
        expect(mockPost).toHaveBeenCalledWith('/api/v1/communities/5/photo', fd);
    });

    test('getRanking', () => {
        communitiesApi.getRanking(5);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/communities/5/ranking');
    });

    test('createHiringPaymentIntent', () => {
        communitiesApi.createHiringPaymentIntent(5, 10, { amount: 100 });
        expect(mockPost).toHaveBeenCalledWith('/api/v1/communities/5/tutor/10/create-payment-intent', { amount: 100 });
    });

    test('confirmTutorPayment', () => {
        communitiesApi.confirmTutorPayment('pi_123');
        expect(mockPost).toHaveBeenCalledWith('/api/v1/communities/confirm-tutor-payment', { paymentIntentId: 'pi_123' });
    });

    test('getClassroom', () => {
        communitiesApi.getClassroom(5);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/communities/5/classroom');
    });

    test('linkClassroom', () => {
        communitiesApi.linkClassroom(5, { courseId: 'c1' });
        expect(mockPost).toHaveBeenCalledWith('/api/v1/communities/5/classroom', { courseId: 'c1' });
    });

    test('unlinkClassroom', () => {
        communitiesApi.unlinkClassroom(5);
        expect(mockDelete).toHaveBeenCalledWith('/api/v1/communities/5/classroom');
    });

    test('addAdmin', () => {
        communitiesApi.addAdmin(5, 10);
        expect(mockPost).toHaveBeenCalledWith('/api/v1/communities/5/admins/10', {});
    });

    test('activateTeacherRole tries primary endpoint', async () => {
        mockPut.mockResolvedValue({ ok: true });
        const result = await communitiesApi.activateTeacherRole(5);
        expect(mockPut).toHaveBeenCalledWith('/api/v1/communities/5/members/me/role', { rol: 'PROFESOR' });
        expect(result).toEqual({ ok: true });
    });

    test('activateTeacherRole falls back to second endpoint on 404', async () => {
        mockPut.mockRejectedValue({ status: 404 });
        mockPost.mockResolvedValue({ ok: true });
    await communitiesApi.activateTeacherRole(5);
        expect(mockPost).toHaveBeenCalledWith('/api/v1/communities/5/members/me/teacher', {});
    });

    test('getMyRequestStatus', () => {
        communitiesApi.getMyRequestStatus(5);
        expect(mockGet).toHaveBeenCalledWith('/api/v1/communities/5/requests/me');
    });

    test('listRequests with params', () => {
        communitiesApi.listRequests(5, { estado: 'PENDIENTE', page: 0 });
        expect(mockGet).toHaveBeenCalledWith(expect.stringContaining('estado=PENDIENTE'));
    });

    test('promoteMemberToAdmin', () => {
        communitiesApi.promoteMemberToAdmin(5, 10);
        expect(mockPost).toHaveBeenCalledWith('/api/v1/communities/5/admin/10', {});
    });
});

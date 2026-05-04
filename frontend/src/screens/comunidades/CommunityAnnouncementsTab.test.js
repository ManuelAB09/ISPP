import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import CommunityAnnouncementsTab from './CommunityAnnouncementsTab';

jest.mock('./CommunityAnnouncementsTab.css', () => ({}));

jest.mock('../../api/baseUrl', () => ({
    getApiBaseUrl: () => 'http://localhost:8080',
}));

const mockGet = jest.fn();
const mockPost = jest.fn();
const mockDelete = jest.fn();
jest.mock('../../api/axiosConfig', () => ({
    __esModule: true,
    default: {
        get: (...args) => mockGet(...args),
        post: (...args) => mockPost(...args),
        delete: (...args) => mockDelete(...args),
    },
}));

const mockGetComments = jest.fn();
const mockPostComment = jest.fn();
const mockDeleteComment = jest.fn();
jest.mock('../../api/announcementComments', () => ({
    getAnnouncementComments: (...args) => mockGetComments(...args),
    postAnnouncementComment: (...args) => mockPostComment(...args),
    deleteAnnouncementComment: (...args) => mockDeleteComment(...args),
}));

const renderTab = (props = {}) => {
    return render(
        <MemoryRouter>
            <CommunityAnnouncementsTab communityId={1} isAdmin={false} {...props} />
        </MemoryRouter>
    );
};

const announcementWithComments = {
    id: 1,
    titulo: 'Test Announcement',
    contenido: 'Content',
    createdAt: '2025-06-01T10:00:00',
    autor: { nombre: 'Admin', foto: null },
    permitirComentarios: true,
};

describe('CommunityAnnouncementsTab', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        window.confirm = jest.fn(() => true);
        mockGetComments.mockResolvedValue([]);
        mockPostComment.mockResolvedValue({});
        mockDeleteComment.mockResolvedValue(undefined);
        // Clear localStorage userId between tests
        localStorage.removeItem('userId');
    });

    test('renders loading then announcements', async () => {
        mockGet.mockResolvedValueOnce({
            data: {
                anuncios: [
                    { id: 1, titulo: 'Test Announcement', contenido: 'Content', createdAt: '2025-06-01T10:00:00', autor: { nombre: 'Admin', foto: null }, permitirComentarios: true },
                ],
            },
        });

        await act(async () => { renderTab(); });

        await waitFor(() => {
            expect(screen.getByText('Test Announcement')).toBeInTheDocument();
        });
    });

    test('renders empty state', async () => {
        mockGet.mockResolvedValueOnce({ data: { anuncios: [] } });
        await act(async () => { renderTab(); });
        await waitFor(() => {
            expect(screen.getByText(/No hay anuncios|sin anuncios/i)).toBeInTheDocument();
        });
    });

    test('shows error on API failure', async () => {
        mockGet.mockRejectedValueOnce(new Error('fail'));
        await act(async () => { renderTab(); });
        await waitFor(() => {
            expect(screen.getByText(/No se pudieron cargar/)).toBeInTheDocument();
        });
    });

    test('admin can see create button', async () => {
        mockGet.mockResolvedValueOnce({ data: { anuncios: [] } });
        await act(async () => { renderTab({ isAdmin: true }); });
        await waitFor(() => {
            expect(screen.getByText(/Crear anuncio/)).toBeInTheDocument();
        });
    });

    test('non-admin cannot see create button', async () => {
        mockGet.mockResolvedValueOnce({ data: { anuncios: [] } });
        await act(async () => { renderTab({ isAdmin: false }); });
        await waitFor(() => {
            expect(screen.queryByText(/Crear anuncio/)).not.toBeInTheDocument();
        });
    });

    test('opens create modal', async () => {
        mockGet.mockResolvedValueOnce({ data: { anuncios: [] } });
        await act(async () => { renderTab({ isAdmin: true }); });

        await waitFor(() => {
            expect(screen.getByText(/Crear anuncio/)).toBeInTheDocument();
        });

        await act(async () => {
            fireEvent.click(screen.getByText(/Crear anuncio/));
        });

        expect(screen.getByPlaceholderText?.(/título|Título/i) || document.querySelector('input[name="titulo"]')).toBeInTheDocument();
    });

    test('validates short title', async () => {
        mockGet.mockResolvedValueOnce({ data: { anuncios: [] } });
        await act(async () => { renderTab({ isAdmin: true }); });

        await waitFor(() => {
            expect(screen.getByText(/Crear anuncio/)).toBeInTheDocument();
        });

        await act(async () => {
            fireEvent.click(screen.getByText(/Crear anuncio/));
        });

        const titleInput = document.querySelector('input[name="titulo"]');
        const contentInput = document.querySelector('textarea[name="contenido"]');

        if (titleInput && contentInput) {
            fireEvent.change(titleInput, { target: { name: 'titulo', value: 'ab' } });
            fireEvent.change(contentInput, { target: { name: 'contenido', value: 'enough content for the test' } });

            const submitBtn = document.querySelector('button.catab-btn-submit');
            if (submitBtn) {
                await act(async () => { fireEvent.click(submitBtn); });
                expect(screen.getByText(/título debe tener/i)).toBeInTheDocument();
            }
        }
    });

    test('creates announcement successfully', async () => {
        mockGet.mockResolvedValueOnce({ data: { anuncios: [] } });
        mockPost.mockResolvedValueOnce({});
        mockGet.mockResolvedValueOnce({
            data: {
                anuncios: [
                    { id: 10, titulo: 'New Announcement', contenido: 'New content', createdAt: '2025-06-01T10:00:00', autor: { nombre: 'Admin' }, permitirComentarios: true },
                ],
            },
        });

        await act(async () => { renderTab({ isAdmin: true }); });

        await waitFor(() => {
            expect(screen.getByText(/Crear anuncio/)).toBeInTheDocument();
        });

        await act(async () => {
            fireEvent.click(screen.getByText(/Crear anuncio/));
        });

        const titleInput = document.querySelector('input[name="titulo"]');
        const contentInput = document.querySelector('textarea[name="contenido"]');

        if (titleInput && contentInput) {
            fireEvent.change(titleInput, { target: { name: 'titulo', value: 'Valid Title Here' } });
            fireEvent.change(contentInput, { target: { name: 'contenido', value: 'This is valid content for the announcement' } });

            const submitBtn = document.querySelector('button.catab-btn-submit');
            if (submitBtn) {
                await act(async () => { fireEvent.click(submitBtn); });
            }
        }
    });

    test('admin can delete announcement from UI', async () => {
        mockGet.mockResolvedValueOnce({
            data: {
                anuncios: [
                    {
                        id: 44,
                        titulo: 'Delete Me',
                        contenido: 'Delete content',
                        createdAt: '2025-06-01T10:00:00',
                        autor: { nombre: 'Admin' },
                        permitirComentarios: false,
                    },
                ],
            },
        });
        mockDelete.mockResolvedValueOnce({});

        await act(async () => {
            renderTab({ isAdmin: true });
        });

        await waitFor(() => {
            expect(screen.getByText('Delete Me')).toBeInTheDocument();
        });

        await act(async () => {
            fireEvent.click(screen.getByText('Eliminar'));
        });

        expect(mockDelete).toHaveBeenCalledWith('/api/v1/communities/1/announcements/44');
        await waitFor(() => {
            expect(screen.queryByText('Delete Me')).not.toBeInTheDocument();
        });
    });

    test('non-admin cannot see delete action', async () => {
        mockGet.mockResolvedValueOnce({
            data: {
                anuncios: [
                    {
                        id: 45,
                        titulo: 'Read Only',
                        contenido: 'No delete action for non-admin',
                        createdAt: '2025-06-01T10:00:00',
                        autor: { nombre: 'Admin' },
                        permitirComentarios: false,
                    },
                ],
            },
        });

        await act(async () => {
            renderTab({ isAdmin: false });
        });

        await waitFor(() => {
            expect(screen.getByText('Read Only')).toBeInTheDocument();
        });

        expect(screen.queryByText('Eliminar')).not.toBeInTheDocument();
    });

    // ─── Tests de eliminación de comentarios ───

    test('admin can see delete button on any comment', async () => {
        mockGet.mockResolvedValueOnce({ data: { anuncios: [announcementWithComments] } });
        mockGetComments.mockResolvedValueOnce([
            { id: 10, texto: 'Comentario de otro', createdAt: '2025-06-01T10:00:00', usuario: { id: 99, nombre: 'Otro', foto: null } },
        ]);

        await act(async () => {
            renderTab({ isAdmin: true });
        });

        await waitFor(() => {
            expect(screen.getByText('Comentario de otro')).toBeInTheDocument();
        });

        expect(screen.getByText('🗑️')).toBeInTheDocument();
    });

    test('non-admin author can see delete button on own comment', async () => {
        localStorage.setItem('userId', '42');
        mockGet.mockResolvedValueOnce({ data: { anuncios: [announcementWithComments] } });
        mockGetComments.mockResolvedValueOnce([
            { id: 10, texto: 'Mi propio comentario', createdAt: '2025-06-01T10:00:00', usuario: { id: 42, nombre: 'Yo', foto: null } },
        ]);

        await act(async () => {
            renderTab({ isAdmin: false });
        });

        await waitFor(() => {
            expect(screen.getByText('Mi propio comentario')).toBeInTheDocument();
        });

        expect(screen.getByText('🗑️')).toBeInTheDocument();
    });

    test('non-admin cannot see delete button on others comment', async () => {
        localStorage.setItem('userId', '42');
        mockGet.mockResolvedValueOnce({ data: { anuncios: [announcementWithComments] } });
        mockGetComments.mockResolvedValueOnce([
            { id: 10, texto: 'Comentario ajeno', createdAt: '2025-06-01T10:00:00', usuario: { id: 99, nombre: 'Otro', foto: null } },
        ]);

        await act(async () => {
            renderTab({ isAdmin: false });
        });

        await waitFor(() => {
            expect(screen.getByText('Comentario ajeno')).toBeInTheDocument();
        });

        expect(screen.queryByText('🗑️')).not.toBeInTheDocument();
    });

    test('deleting a comment calls API and removes it from UI', async () => {
        localStorage.setItem('userId', '42');
        mockGet.mockResolvedValueOnce({ data: { anuncios: [announcementWithComments] } });
        mockGetComments.mockResolvedValueOnce([
            { id: 10, texto: 'Borrame', createdAt: '2025-06-01T10:00:00', usuario: { id: 42, nombre: 'Yo', foto: null } },
        ]);
        mockDeleteComment.mockResolvedValueOnce(undefined);

        await act(async () => {
            renderTab({ isAdmin: false });
        });

        await waitFor(() => {
            expect(screen.getByText('Borrame')).toBeInTheDocument();
        });

        await act(async () => {
            fireEvent.click(screen.getByText('🗑️'));
        });

        expect(mockDeleteComment).toHaveBeenCalledWith(1, 10);
        await waitFor(() => {
            expect(screen.queryByText('Borrame')).not.toBeInTheDocument();
        });
    });

    test('delete comment shows confirmation dialog', async () => {
        localStorage.setItem('userId', '42');
        mockGet.mockResolvedValueOnce({ data: { anuncios: [announcementWithComments] } });
        mockGetComments.mockResolvedValueOnce([
            { id: 10, texto: 'Confirmame', createdAt: '2025-06-01T10:00:00', usuario: { id: 42, nombre: 'Yo', foto: null } },
        ]);

        await act(async () => {
            renderTab({ isAdmin: false });
        });

        await waitFor(() => {
            expect(screen.getByText('Confirmame')).toBeInTheDocument();
        });

        await act(async () => {
            fireEvent.click(screen.getByText('🗑️'));
        });

        expect(window.confirm).toHaveBeenCalledWith('¿Eliminar este comentario?');
    });

    test('cancel on confirm dialog does not delete comment', async () => {
        window.confirm = jest.fn(() => false);
        localStorage.setItem('userId', '42');
        mockGet.mockResolvedValueOnce({ data: { anuncios: [announcementWithComments] } });
        mockGetComments.mockResolvedValueOnce([
            { id: 10, texto: 'No me borres', createdAt: '2025-06-01T10:00:00', usuario: { id: 42, nombre: 'Yo', foto: null } },
        ]);

        await act(async () => {
            renderTab({ isAdmin: false });
        });

        await waitFor(() => {
            expect(screen.getByText('No me borres')).toBeInTheDocument();
        });

        await act(async () => {
            fireEvent.click(screen.getByText('🗑️'));
        });

        expect(mockDeleteComment).not.toHaveBeenCalled();
        expect(screen.getByText('No me borres')).toBeInTheDocument();
    });

    test('delete comment API error shows error message', async () => {
        localStorage.setItem('userId', '42');
        mockGet.mockResolvedValueOnce({ data: { anuncios: [announcementWithComments] } });
        mockGetComments.mockResolvedValueOnce([
            { id: 10, texto: 'Error al borrar', createdAt: '2025-06-01T10:00:00', usuario: { id: 42, nombre: 'Yo', foto: null } },
        ]);
        mockDeleteComment.mockRejectedValueOnce(new Error('Server error'));

        await act(async () => {
            renderTab({ isAdmin: false });
        });

        await waitFor(() => {
            expect(screen.getByText('Error al borrar')).toBeInTheDocument();
        });

        await act(async () => {
            fireEvent.click(screen.getByText('🗑️'));
        });

        await waitFor(() => {
            expect(screen.getByText(/No se pudo eliminar el comentario/i)).toBeInTheDocument();
        });
    });
});
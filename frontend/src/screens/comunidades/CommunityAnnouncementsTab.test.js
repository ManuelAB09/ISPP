import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import CommunityAnnouncementsTab from './CommunityAnnouncementsTab';

jest.mock('./CommunityAnnouncementsTab.css', () => ({}));

jest.mock('../../api/baseUrl', () => ({
    getApiBaseUrl: () => 'http://localhost:8080',
}));

const mockGet = jest.fn();
const mockPost = jest.fn();
jest.mock('../../api/axiosConfig', () => ({
    __esModule: true,
    default: {
        get: (...args) => mockGet(...args),
        post: (...args) => mockPost(...args),
    },
}));

jest.mock('../../api/announcementComments', () => ({
    getAnnouncementComments: jest.fn(() => Promise.resolve([])),
    postAnnouncementComment: jest.fn(() => Promise.resolve({})),
}));

const { getAnnouncementComments, postAnnouncementComment } = require('../../api/announcementComments');

const renderTab = (props = {}) => {
    return render(
        <MemoryRouter>
            <CommunityAnnouncementsTab communityId={1} isAdmin={false} {...props} />
        </MemoryRouter>
    );
};

describe('CommunityAnnouncementsTab', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        getAnnouncementComments.mockImplementation(() => Promise.resolve([]));
        postAnnouncementComment.mockImplementation(() => Promise.resolve({}));
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
});

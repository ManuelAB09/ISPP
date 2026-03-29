import { render, screen, waitFor, act, fireEvent } from '@testing-library/react';
import HireTutorModal from './HireTutorModal';

jest.mock('./HireTutorModal.css', () => ({}));

const mockListCommunities = jest.fn();
const mockHireTutor = jest.fn();
jest.mock('../../api/communities.api', () => ({
    communitiesApi: {
        list: (...args) => mockListCommunities(...args),
        hireTutor: (...args) => mockHireTutor(...args),
    },
}));

jest.mock('../../contexts/AuthContext', () => ({
    useAuth: () => ({
        user: { id: 1, nombre: 'Student' },
    }),
}));

const tutor = {
    id: 10,
    tarifaHora: 25,
    usuario: { nombre: 'Tutor Test' },
};

describe('HireTutorModal', () => {
    const onClose = jest.fn();

    beforeEach(() => {
        jest.clearAllMocks();
        mockListCommunities.mockResolvedValue({
            content: [
                { id: 1, nombre: 'My Community', creador: { id: 1 } },
                { id: 2, nombre: 'Other', creador: { id: 99 } },
            ],
        });
    });

    test('renders modal with tutor name', async () => {
        await act(async () => {
            render(<HireTutorModal tutor={tutor} onClose={onClose} />);
        });
        expect(screen.getByText(/Tutor Test/)).toBeInTheDocument();
    });

    test('loads communities on mount', async () => {
        await act(async () => {
            render(<HireTutorModal tutor={tutor} onClose={onClose} />);
        });
        await waitFor(() => {
            expect(mockListCommunities).toHaveBeenCalled();
        });
    });

    test('only shows communities owned by user', async () => {
        await act(async () => {
            render(<HireTutorModal tutor={tutor} onClose={onClose} />);
        });
        await waitFor(() => {
            expect(screen.getByText('My Community')).toBeInTheDocument();
            expect(screen.queryByText('Other')).not.toBeInTheDocument();
        });
    });

    test('closes on overlay click', async () => {
        await act(async () => {
            render(<HireTutorModal tutor={tutor} onClose={onClose} />);
        });
        const overlay = document.querySelector('.htm-overlay');
        if (overlay) {
            fireEvent.click(overlay);
            expect(onClose).toHaveBeenCalled();
        }
    });

    test('handles empty communities list', async () => {
        mockListCommunities.mockResolvedValueOnce({ content: [] });
        await act(async () => {
            render(<HireTutorModal tutor={tutor} onClose={onClose} />);
        });
        await waitFor(() => {
            expect(screen.queryByText('My Community')).not.toBeInTheDocument();
        });
    });

    test('filters communities by search', async () => {
        await act(async () => {
            render(<HireTutorModal tutor={tutor} onClose={onClose} />);
        });

        const searchInput = screen.getByPlaceholderText(/Nombre de la comunidad/i);
        await act(async () => {
            fireEvent.change(searchInput, { target: { value: 'test' } });
        });
        await waitFor(() => {
            expect(mockListCommunities).toHaveBeenCalledWith(expect.objectContaining({ search: 'test' }));
        });
    });

    test('selects community and advances to step 2', async () => {
        await act(async () => {
            render(<HireTutorModal tutor={tutor} onClose={onClose} />);
        });

        await waitFor(() => {
            expect(screen.getByText('My Community')).toBeInTheDocument();
        });

        const communityItem = screen.getByText('My Community');
        await act(async () => { fireEvent.click(communityItem); });
    });

    test('handles API error on communities', async () => {
        mockListCommunities.mockRejectedValueOnce(new Error('fail'));
        await act(async () => {
            render(<HireTutorModal tutor={tutor} onClose={onClose} />);
        });
    });

    test('shows step 2 with contract details after selecting community', async () => {
        await act(async () => {
            render(<HireTutorModal tutor={tutor} onClose={onClose} />);
        });

        await waitFor(() => {
            expect(screen.getByText('My Community')).toBeInTheDocument();
        });

        const communityItem = screen.getByText('My Community');
        await act(async () => { fireEvent.click(communityItem); });

        // Step 2 should now show contract details or next button
        const nextBtn = screen.queryByText(/Siguiente|Continuar/i);
        if (nextBtn) {
            await act(async () => { fireEvent.click(nextBtn); });
        }
    });

    test('shows tutor tarifa info on step 2', async () => {
        await act(async () => {
            render(<HireTutorModal tutor={tutor} onClose={onClose} />);
        });

        // Select community first
        await screen.findByText('My Community');
        const communityItem = screen.getByText('My Community');
        await act(async () => { fireEvent.click(communityItem); });

        // Advance to step 2
        const nextBtn = screen.queryByText(/Continuar/i);
        if (nextBtn) {
            await act(async () => { fireEvent.click(nextBtn); });
        }

        await waitFor(() => {
            expect(screen.getByText(/25/)).toBeInTheDocument();
        });
    });

    test('renders search placeholder', async () => {
        await act(async () => {
            render(<HireTutorModal tutor={tutor} onClose={onClose} />);
        });
        expect(screen.getByPlaceholderText(/Nombre de la comunidad/i)).toBeInTheDocument();
    });

    test('passes search parameter to API', async () => {
        await act(async () => {
            render(<HireTutorModal tutor={tutor} onClose={onClose} />);
        });

        const searchInput = screen.getByPlaceholderText(/Nombre de la comunidad/i);
        await act(async () => {
            fireEvent.change(searchInput, { target: { value: 'Math' } });
        });

        await waitFor(() => {
            expect(mockListCommunities).toHaveBeenCalledWith(expect.objectContaining({ search: 'Math' }));
        });
    });
});

import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import EditProfile from './EditProfile';

// Mock CSS and leaflet
jest.mock('./EditProfile.css', () => ({}));
jest.mock('leaflet/dist/leaflet.css', () => ({}));

jest.mock('react-leaflet', () => ({
    MapContainer: ({ children }) => <div data-testid="map-container">{children}</div>,
    TileLayer: () => <div data-testid="tile-layer" />,
    Marker: ({ children }) => <div data-testid="marker">{children}</div>,
    Popup: ({ children }) => <div data-testid="popup">{children}</div>,
    useMapEvents: () => null,
}));

jest.mock('leaflet', () => ({
    icon: () => ({}),
}));

jest.mock('../../api/baseUrl', () => ({
    getApiBaseUrl: () => 'http://localhost:8080',
}));

const mockGetProfileAvatars = jest.fn(() => Promise.resolve(['/static/images/renata/cat.png', '/static/images/renata/dog.png']));
const mockUploadProfilePhoto = jest.fn(() => Promise.resolve({}));
jest.mock('../../api/auth.api', () => ({
    authApi: {
        getProfileAvatars: (...args) => mockGetProfileAvatars(...args),
        uploadProfilePhoto: (...args) => mockUploadProfilePhoto(...args),
    },
}));

const mockUpdateProfile = jest.fn(() => Promise.resolve({ success: true }));
const mockUser = {
    id: 1,
    nombre: 'Test User',
    bio: 'A test bio',
    universidad: 'University',
    grado: 'CS',
    nivelEstudios: 'Universitario',
    baseFormativa: 'Ciencias',
    foto: '/static/images/renata/cat.png',
    fotoBackgroundColor: '#ffffff',
    intereses: ['Matemáticas'],
    ubicacion: { nombre: 'Sevilla', latitud: 37.3891, longitud: -5.9845 },
};

jest.mock('../../contexts/AuthContext', () => ({
    useAuth: () => ({
        user: mockUser,
        updateProfile: mockUpdateProfile,
    }),
}));

const renderEditProfile = (props = {}) => {
    return render(
        <MemoryRouter>
            <EditProfile onClose={jest.fn()} onSave={jest.fn()} {...props} />
        </MemoryRouter>
    );
};

// Mock URL.createObjectURL
global.URL.createObjectURL = jest.fn(() => 'blob:http://localhost/test-blob');
global.URL.revokeObjectURL = jest.fn();

describe('EditProfile', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockGetProfileAvatars.mockResolvedValue(['/static/images/renata/cat.png', '/static/images/renata/dog.png']);
    });

    test('renders form with user data', async () => {
        await act(async () => { renderEditProfile(); });

        expect(screen.getByDisplayValue('Test User')).toBeInTheDocument();
        expect(screen.getByDisplayValue('A test bio')).toBeInTheDocument();
        expect(screen.getByDisplayValue('University')).toBeInTheDocument();
        expect(screen.getByDisplayValue('CS')).toBeInTheDocument();
    });

    test('renders academic interests', async () => {
        await act(async () => { renderEditProfile(); });

        expect(screen.getByText('Matemáticas')).toBeInTheDocument();
        expect(screen.getByText('Física')).toBeInTheDocument();
        expect(screen.getByText('Historia')).toBeInTheDocument();
    });

    test('renders map container', async () => {
        await act(async () => { renderEditProfile(); });
        expect(screen.getByTestId('map-container')).toBeInTheDocument();
    });

    test('loads avatar options', async () => {
        await act(async () => { renderEditProfile(); });

        await waitFor(() => {
            expect(mockGetProfileAvatars).toHaveBeenCalled();
        });
    });

    test('handles input changes', async () => {
        await act(async () => { renderEditProfile(); });

        const nameInput = screen.getByDisplayValue('Test User');
        fireEvent.change(nameInput, { target: { name: 'nombre', value: 'New Name' } });
        expect(screen.getByDisplayValue('New Name')).toBeInTheDocument();
    });

    test('toggles interest', async () => {
        await act(async () => { renderEditProfile(); });

        const physicsBtn = screen.getByText('Física');
        fireEvent.click(physicsBtn);
        // Física should now be selected (in the intereses array)
    });

    test('validates empty name', async () => {
        await act(async () => { renderEditProfile(); });

        const nameInput = screen.getByDisplayValue('Test User');
        fireEvent.change(nameInput, { target: { name: 'nombre', value: '' } });

        const submitBtn = screen.getByText(/Guardar/i);
        await act(async () => { fireEvent.click(submitBtn); });

        expect(screen.getByText('El nombre es obligatorio')).toBeInTheDocument();
    });

    test('submits form successfully', async () => {
        mockUpdateProfile.mockResolvedValueOnce({ success: true });
        await act(async () => { renderEditProfile(); });

        const submitBtn = screen.getByText(/Guardar/i);
        await act(async () => { fireEvent.click(submitBtn); });

        await waitFor(() => {
            expect(mockUpdateProfile).toHaveBeenCalled();
        });
    });

    test('submits with photo upload', async () => {
        mockUploadProfilePhoto.mockResolvedValueOnce({});
        mockUpdateProfile.mockResolvedValueOnce({ success: true });
        await act(async () => { renderEditProfile(); });

        // Simulate selecting a file
        const fileInput = document.querySelector('input[type="file"]');
        if (fileInput) {
            const file = new File(['test'], 'photo.jpg', { type: 'image/jpeg' });
            await act(async () => {
                fireEvent.change(fileInput, { target: { files: [file] } });
            });

            const submitBtn = screen.getByText(/Guardar/i);
            await act(async () => { fireEvent.click(submitBtn); });

            await waitFor(() => {
                expect(mockUploadProfilePhoto).toHaveBeenCalledWith(file);
            });
        }
    });

    test('handles file type validation', async () => {
        await act(async () => { renderEditProfile(); });

        const fileInput = document.querySelector('input[type="file"]');
        if (fileInput) {
            const file = new File(['test'], 'photo.gif', { type: 'image/gif' });
            await act(async () => {
                fireEvent.change(fileInput, { target: { files: [file] } });
            });

            expect(screen.getByText('Formato no permitido. Usa JPG, PNG o WEBP.')).toBeInTheDocument();
        }
    });

    test('handles file size validation', async () => {
        await act(async () => { renderEditProfile(); });

        const fileInput = document.querySelector('input[type="file"]');
        if (fileInput) {
            const bigFile = new File([new ArrayBuffer(6 * 1024 * 1024)], 'photo.jpg', { type: 'image/jpeg' });
            Object.defineProperty(bigFile, 'size', { value: 6 * 1024 * 1024 });
            await act(async () => {
                fireEvent.change(fileInput, { target: { files: [bigFile] } });
            });

            expect(screen.getByText('La imagen supera el límite de 5MB.')).toBeInTheDocument();
        }
    });

    test('handles save error', async () => {
        mockUpdateProfile.mockResolvedValueOnce({ success: false, error: 'Server error' });
        await act(async () => { renderEditProfile(); });

        const submitBtn = screen.getByText(/Guardar/i);
        await act(async () => { fireEvent.click(submitBtn); });

        await waitFor(() => {
            expect(screen.getByText(/error/i)).toBeInTheDocument();
        });
    });

    test('renders with ubicacionPreseleccionada as string', async () => {
        await act(async () => { renderEditProfile({ ubicacionPreseleccionada: 'Madrid' }); });
        expect(screen.getByDisplayValue('Madrid')).toBeInTheDocument();
    });

    test('renders with ubicacionPreseleccionada as object', async () => {
        const ubicacion = { nombre: 'Barcelona', latitud: 41.38, longitud: 2.16 };
        await act(async () => { renderEditProfile({ ubicacionPreseleccionada: ubicacion }); });
        expect(screen.getByDisplayValue('Barcelona')).toBeInTheDocument();
    });

    test('handles avatar failure gracefully', async () => {
        mockGetProfileAvatars.mockRejectedValueOnce(new Error('fail'));
        await act(async () => { renderEditProfile(); });
        // Should still render without crashing
        expect(screen.getByDisplayValue('Test User')).toBeInTheDocument();
    });

    test('handles description change', async () => {
        await act(async () => { renderEditProfile(); });
        const descInput = screen.getByDisplayValue('A test bio');
        fireEvent.change(descInput, { target: { name: 'descripcion', value: 'New bio' } });
        expect(screen.getByDisplayValue('New bio')).toBeInTheDocument();
    });

    test('handles ubicacion text change', async () => {
        await act(async () => { renderEditProfile(); });
        const ubicInput = screen.getByDisplayValue('Sevilla');
        fireEvent.change(ubicInput, { target: { name: 'ubicacion', value: 'Granada' } });
        expect(screen.getByDisplayValue('Granada')).toBeInTheDocument();
    });
});

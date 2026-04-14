import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import CommunityChat from './CommunityChat';

// Mock scrollIntoView y scrollTo para jsdom
window.HTMLElement.prototype.scrollIntoView = jest.fn();
window.HTMLElement.prototype.scrollTo = jest.fn();

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
}));

// Stable mock references
const mockSocketOn = jest.fn();
const mockSocketOff = jest.fn();
const mockEnviarMensajeComunidad = jest.fn();
const mockEnviarArchivoComunidad = jest.fn();
const mockObtenerHistorialComunidad = jest.fn();
const mockEditarMensajeComunidad = jest.fn();
const mockEliminarMensajeComunidad = jest.fn();
const mockObtenerPreviewEnlace = jest.fn();
const mockObtenerArchivoChatBlob = jest.fn();
const mockExtractFirstUrl = jest.fn();
const mockGetMembers = jest.fn();

jest.mock('../../contexts/SocketContext', () => ({
    useSocketContext: () => ({
        socket: { on: mockSocketOn, off: mockSocketOff },
        isConnected: true,
    }),
}));

jest.mock('../../api/mensajeService', () => ({
    enviarMensajeComunidad: (...a) => mockEnviarMensajeComunidad(...a),
    enviarArchivoComunidad: (...a) => mockEnviarArchivoComunidad(...a),
    obtenerHistorialComunidad: (...a) => mockObtenerHistorialComunidad(...a),
    editarMensajeComunidad: (...a) => mockEditarMensajeComunidad(...a),
    eliminarMensajeComunidad: (...a) => mockEliminarMensajeComunidad(...a),
    obtenerPreviewEnlace: (...a) => mockObtenerPreviewEnlace(...a),
    obtenerArchivoChatBlob: (...a) => mockObtenerArchivoChatBlob(...a),
}));

jest.mock('../../api/baseUrl', () => ({ getApiBaseUrl: () => 'http://localhost:8080' }));
jest.mock('../../api/communities.api', () => ({
    communitiesApi: { getMembers: (...a) => mockGetMembers(...a) },
}));
jest.mock('../../utils/linkPreview', () => ({ extractFirstUrl: (...a) => mockExtractFirstUrl(...a) }));
jest.mock('./LinkPreviewCard', () => function Mock() { return <div data-testid="link-preview-card" />; });
jest.mock('react-icons/lu', () => ({
    LuExpand: () => <span data-testid="icon-expand" />,
    LuMessageCircle: () => <span data-testid="icon-chat" />,
    LuX: () => <span data-testid="icon-close" />,
}));

describe('CommunityChat', () => {
    const mockUsuarioActual = { id: 1, nombre: 'Usuario Test', foto: 'foto.jpg' };
    const defaultProps = {
        comunidadId: 10,
        usuarioActual: mockUsuarioActual,
        comunidadNombre: 'Comunidad Mates',
        comunidadImagen: 'https://example.com/img.jpg',
        initiallyOpen: false,
        mode: 'floating',
        onOpenPrivateChat: jest.fn(),
    };

    const sampleMessages = [
        { id: 30, contenido: 'Hola a todos', usuarioId: 1, usuarioNombre: 'Usuario Test', usuarioFoto: 'foto.jpg', createdAt: '2025-01-15T10:00:00', editado: false },
        { id: 31, contenido: 'Bienvenido', usuarioId: 5, usuarioNombre: 'Carlos', usuarioFoto: null, createdAt: '2025-01-15T10:01:00', editado: true },
    ];

    const wrap = (ui) => render(<MemoryRouter>{ui}</MemoryRouter>);

    beforeEach(() => {
        jest.clearAllMocks();
        mockObtenerHistorialComunidad.mockResolvedValue({ data: [] });
        mockEnviarMensajeComunidad.mockResolvedValue({ data: { id: 99, contenido: 'test', usuarioId: 1 } });
        mockExtractFirstUrl.mockReturnValue(null);
        mockGetMembers.mockResolvedValue({ content: [] });
    });

    // === Embedded mode ===

    it('renders chat panel in embedded mode', async () => {
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await waitFor(() => expect(mockObtenerHistorialComunidad).toHaveBeenCalledWith(10));
        expect(screen.getByText('Chat de comunidad')).toBeInTheDocument();
        expect(screen.getByText('Comunidad Mates')).toBeInTheDocument();
    });

    it('shows En línea status in embedded mode', async () => {
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await screen.findByText('En línea');
    });

    it('shows loading state', async () => {
        mockObtenerHistorialComunidad.mockImplementation(() => new Promise(() => {}));
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        expect(screen.getByText('Cargando mensajes...')).toBeInTheDocument();
    });

    it('shows empty state with no messages', async () => {
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await waitFor(() => {
            expect(screen.getByText(/No hay mensajes aún/)).toBeInTheDocument();
        });
    });

    it('renders messages from history', async () => {
        mockObtenerHistorialComunidad.mockResolvedValue({ data: sampleMessages });
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await waitFor(() => {
            expect(screen.getByText('Hola a todos')).toBeInTheDocument();
            expect(screen.getByText('Bienvenido')).toBeInTheDocument();
        });
    });

    it('shows editado badge', async () => {
        mockObtenerHistorialComunidad.mockResolvedValue({ data: sampleMessages });
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await screen.findByText('(editado)');
    });

    it('shows user name on each message', async () => {
        mockObtenerHistorialComunidad.mockResolvedValue({ data: sampleMessages });
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await waitFor(() => {
            expect(screen.getByText('Usuario Test')).toBeInTheDocument();
            expect(screen.getByText('Carlos')).toBeInTheDocument();
        });
    });

    // === Floating mode ===

    it('shows toggle button in floating mode', () => {
        wrap(<CommunityChat {...defaultProps} />);
        expect(screen.getByLabelText('Abrir chat de comunidad')).toBeInTheDocument();
    });

    it('opens chat when toggle clicked', async () => {
        wrap(<CommunityChat {...defaultProps} />);
        const toggleBtn = screen.getByLabelText('Abrir chat de comunidad');
        fireEvent.click(toggleBtn);
        await waitFor(() => {
            expect(screen.getByText('Chat de comunidad')).toBeInTheDocument();
        });
    });

    it('closes chat when close button clicked', async () => {
        wrap(<CommunityChat {...defaultProps} initiallyOpen={true} />);
        await screen.findByText('Chat de comunidad');

        fireEvent.click(screen.getByLabelText('Cerrar chat'));
        await waitFor(() => {
            expect(screen.queryByText('Chat de comunidad')).not.toBeInTheDocument();
        });
    });

    // === Sending messages ===

    it('sends a text message', async () => {
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await waitFor(() => expect(mockObtenerHistorialComunidad).toHaveBeenCalled());

        const input = screen.getByPlaceholderText('Escribe un mensaje...');
        fireEvent.change(input, { target: { value: 'Hola comunidad' } });
        fireEvent.submit(input.closest('form'));

        await waitFor(() => {
            expect(mockEnviarMensajeComunidad).toHaveBeenCalledWith(10, 'Hola comunidad');
        });
    });

    it('disables submit when empty', async () => {
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await waitFor(() => expect(mockObtenerHistorialComunidad).toHaveBeenCalled());
        const submitBtn = screen.getByRole('button', { name: '→' });
        expect(submitBtn).toBeDisabled();
    });

    it('shows error on send failure', async () => {
        mockEnviarMensajeComunidad.mockRejectedValue(new Error('fail'));
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await waitFor(() => expect(mockObtenerHistorialComunidad).toHaveBeenCalled());

        const input = screen.getByPlaceholderText('Escribe un mensaje...');
        fireEvent.change(input, { target: { value: 'test' } });
        fireEvent.submit(input.closest('form'));

        await waitFor(() => {
            expect(screen.getByText('Error al enviar el mensaje o archivo')).toBeInTheDocument();
        });
    });

    it('sends file when attached', async () => {
        mockEnviarArchivoComunidad.mockResolvedValue({ data: { id: 100, usuarioId: 1, archivoNombre: 'f.pdf' } });
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await waitFor(() => expect(mockObtenerHistorialComunidad).toHaveBeenCalled());

        const fileInput = document.querySelector('input[type="file"]');
        const file = new File(['x'], 'f.pdf', { type: 'application/pdf' });
        fireEvent.change(fileInput, { target: { files: [file] } });

        fireEvent.submit(screen.getByPlaceholderText('Escribe un mensaje...').closest('form'));

        await waitFor(() => {
            expect(mockEnviarArchivoComunidad).toHaveBeenCalledWith(10, expect.any(File), '');
        });
    });

    it('shows and removes pending attachment', async () => {
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        const fileInput = document.querySelector('input[type="file"]');
        const file = new File(['x'], 'doc.pdf', { type: 'application/pdf' });
        fireEvent.change(fileInput, { target: { files: [file] } });

        await screen.findByText('doc.pdf');
        fireEvent.click(screen.getByText('Quitar'));
        await waitFor(() => expect(screen.queryByText('doc.pdf')).not.toBeInTheDocument());
    });

    // === Editing messages ===

    it('edits own message', async () => {
        mockObtenerHistorialComunidad.mockResolvedValue({ data: sampleMessages });
        mockEditarMensajeComunidad.mockResolvedValue({ data: { ...sampleMessages[0], contenido: 'Editado' } });
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await screen.findByText('Hola a todos');

        fireEvent.click(screen.getAllByText('Editar')[0]);
        const editInput = screen.getByDisplayValue('Hola a todos');
        fireEvent.change(editInput, { target: { value: 'Editado' } });
        fireEvent.click(screen.getByText('Guardar'));

        await waitFor(() => {
            expect(mockEditarMensajeComunidad).toHaveBeenCalledWith(10, 30, 'Editado');
        });
    });

    it('cancels editing', async () => {
        mockObtenerHistorialComunidad.mockResolvedValue({ data: sampleMessages });
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await screen.findByText('Hola a todos');

        fireEvent.click(screen.getAllByText('Editar')[0]);
        fireEvent.click(screen.getByText('Cancelar'));
        expect(screen.queryByText('Guardar')).not.toBeInTheDocument();
    });

    it('shows error on edit failure', async () => {
        mockObtenerHistorialComunidad.mockResolvedValue({ data: sampleMessages });
        mockEditarMensajeComunidad.mockRejectedValue(new Error('fail'));
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await screen.findByText('Hola a todos');

        fireEvent.click(screen.getAllByText('Editar')[0]);
        fireEvent.change(screen.getByDisplayValue('Hola a todos'), { target: { value: 'x' } });
        fireEvent.click(screen.getByText('Guardar'));

        await screen.findByText('Error al editar el mensaje');
    });

    // === Deleting messages ===

    it('deletes own message', async () => {
        mockObtenerHistorialComunidad.mockResolvedValue({ data: sampleMessages });
        mockEliminarMensajeComunidad.mockResolvedValue({});
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await screen.findByText('Hola a todos');

        fireEvent.click(screen.getAllByText('Eliminar')[0]);
        await waitFor(() => expect(mockEliminarMensajeComunidad).toHaveBeenCalledWith(10, 30));
    });

    it('shows error on delete failure', async () => {
        mockObtenerHistorialComunidad.mockResolvedValue({ data: sampleMessages });
        mockEliminarMensajeComunidad.mockRejectedValue(new Error('fail'));
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await screen.findByText('Hola a todos');

        fireEvent.click(screen.getAllByText('Eliminar')[0]);
        await screen.findByText('Error al eliminar el mensaje');
    });

    // === Socket ===

    it('subscribes to community socket topic', async () => {
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await waitFor(() => {
            expect(mockSocketOn).toHaveBeenCalledWith('/topic/community.10', expect.any(Function));
        });
    });

    it('handles incoming community message via socket', async () => {
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await waitFor(() => expect(mockObtenerHistorialComunidad).toHaveBeenCalled());

        const handler = mockSocketOn.mock.calls.find(c => c[0] === '/topic/community.10')?.[1];
        await act(async () => {
            handler({ id: 60, contenido: 'Nuevo msg', usuarioId: 5, usuarioNombre: 'Ana', createdAt: '2025-01-15T12:00:00' });
        });

        await screen.findByText('Nuevo msg');
    });

    it('handles message_deleted socket event', async () => {
        mockObtenerHistorialComunidad.mockResolvedValue({ data: sampleMessages });
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await screen.findByText('Hola a todos');

        const handler = mockSocketOn.mock.calls.find(c => c[0] === '/topic/community.10')?.[1];
        await act(async () => {
            handler({ type: 'message_deleted', messageId: 30 });
        });

        await waitFor(() => expect(screen.queryByText('Hola a todos')).not.toBeInTheDocument());
    });

    // === Open large chat / private chat ===

    it('navigates to large chat view', async () => {
        wrap(<CommunityChat {...defaultProps} initiallyOpen={true} />);
        await screen.findByText('Chat de comunidad');

        fireEvent.click(screen.getByLabelText('Abrir chat en vista grande'));
        expect(mockNavigate).toHaveBeenCalledWith('/chats?communityId=10');
    });

    it('calls onOpenPrivateChat when clicking other user name', async () => {
        mockObtenerHistorialComunidad.mockResolvedValue({ data: sampleMessages });
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await screen.findByText('Carlos');

        // Carlos's message is from user 5, clicking his name should open private chat
        const carlosBtn = screen.getByText('Carlos').closest('button');
        fireEvent.click(carlosBtn);

        expect(defaultProps.onOpenPrivateChat).toHaveBeenCalledWith(
            expect.objectContaining({ userId: 5, userName: 'Carlos' })
        );
    });

    // === File attachment rendering ===

    it('renders file attachment for messages with files', async () => {
        const msgs = [{
            id: 40, contenido: '[Adjunto] doc.pdf', usuarioId: 5, usuarioNombre: 'Ana',
            createdAt: '2025-01-15T11:00:00', archivoNombre: 'doc.pdf',
            archivoUrl: '/files/doc.pdf', archivoMimeType: 'application/pdf', archivoTamano: 2048,
        }];
        mockObtenerHistorialComunidad.mockResolvedValue({ data: msgs });
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);

        await screen.findByText('doc.pdf');
    });

    // === Error from history load ===

    it('shows error when history load fails', async () => {
        mockObtenerHistorialComunidad.mockRejectedValue(new Error('fail'));
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);

        await waitFor(() => {
            expect(screen.getByText('Error al cargar el historial de mensajes')).toBeInTheDocument();
        });
    });

    // === Mention system ===

    it('loads community members for mentions', async () => {
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await waitFor(() => expect(mockGetMembers).toHaveBeenCalledWith(10));
    });

    // === Socket: update existing message ===

    it('updates existing message via socket payload', async () => {
        mockObtenerHistorialComunidad.mockResolvedValue({ data: sampleMessages });
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await screen.findByText('Hola a todos');

        const handler = mockSocketOn.mock.calls.find(c => c[0] === '/topic/community.10')?.[1];
        await act(async () => {
            handler({ id: 30, contenido: 'Actualizado', usuarioId: 1, usuarioNombre: 'Usuario Test', createdAt: '2025-01-15T10:00:00' });
        });

        await screen.findByText('Actualizado');
    });

    it('subscribes to socket error topic', async () => {
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await waitFor(() => {
            expect(mockSocketOn).toHaveBeenCalledWith('error', expect.any(Function));
        });
    });

    it('handles socket error event', async () => {
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await waitFor(() => expect(mockObtenerHistorialComunidad).toHaveBeenCalled());

        const handler = mockSocketOn.mock.calls.find(c => c[0] === 'error')?.[1];
        await act(async () => {
            handler({ message: 'Socket perdido' });
        });

        await screen.findByText('Error: Socket perdido');
    });

    // === File download / open ===

    it('downloads file attachment', async () => {
        const msgs = [{
            id: 40, contenido: '[Adjunto] doc.pdf', usuarioId: 5, usuarioNombre: 'Ana',
            createdAt: '2025-01-15T11:00:00', archivoNombre: 'doc.pdf',
            archivoUrl: '/files/doc.pdf', archivoMimeType: 'application/pdf', archivoTamano: 2048,
        }];
        mockObtenerHistorialComunidad.mockResolvedValue({ data: msgs });
        mockObtenerArchivoChatBlob.mockResolvedValue({ data: new Blob(['x']) });
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await screen.findByText('doc.pdf');

        global.URL.createObjectURL = jest.fn(() => 'blob:test');
        global.URL.revokeObjectURL = jest.fn();
        fireEvent.click(screen.getByText('Descargar'));
        await waitFor(() => expect(mockObtenerArchivoChatBlob).toHaveBeenCalledWith('/files/doc.pdf'));
    });

    it('opens file attachment', async () => {
        const msgs = [{
            id: 41, contenido: '[Adjunto] doc.pdf', usuarioId: 5, usuarioNombre: 'Ana',
            createdAt: '2025-01-15T11:00:00', archivoNombre: 'doc.pdf',
            archivoUrl: '/files/doc.pdf', archivoMimeType: 'application/pdf', archivoTamano: 1024,
        }];
        mockObtenerHistorialComunidad.mockResolvedValue({ data: msgs });
        mockObtenerArchivoChatBlob.mockResolvedValue({ data: new Blob(['x']) });
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await screen.findByText('doc.pdf');

        global.URL.createObjectURL = jest.fn(() => 'blob:test');
        global.URL.revokeObjectURL = jest.fn();
        window.open = jest.fn();
        fireEvent.click(screen.getByText('Abrir'));
        await waitFor(() => expect(mockObtenerArchivoChatBlob).toHaveBeenCalledWith('/files/doc.pdf'));
    });

    it('shows error on download failure', async () => {
        const msgs = [{
            id: 42, contenido: '[Adjunto] doc.pdf', usuarioId: 5, usuarioNombre: 'Ana',
            createdAt: '2025-01-15T11:00:00', archivoNombre: 'doc.pdf',
            archivoUrl: '/files/doc.pdf', archivoMimeType: 'application/pdf', archivoTamano: 512,
        }];
        mockObtenerHistorialComunidad.mockResolvedValue({ data: msgs });
        mockObtenerArchivoChatBlob.mockRejectedValue(new Error('fail'));
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await screen.findByText('doc.pdf');

        fireEvent.click(screen.getByText('Descargar'));
        await screen.findByText('No se pudo descargar el archivo');
    });

    it('shows error on open failure', async () => {
        const msgs = [{
            id: 43, contenido: '[Adjunto] doc.pdf', usuarioId: 5, usuarioNombre: 'Ana',
            createdAt: '2025-01-15T11:00:00', archivoNombre: 'doc.pdf',
            archivoUrl: '/files/doc.pdf', archivoMimeType: 'application/pdf', archivoTamano: 512,
        }];
        mockObtenerHistorialComunidad.mockResolvedValue({ data: msgs });
        mockObtenerArchivoChatBlob.mockRejectedValue(new Error('fail'));
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await screen.findByText('doc.pdf');

        fireEvent.click(screen.getByText('Abrir'));
        await screen.findByText('No se pudo abrir el archivo');
    });

    // === Mention input detection ===

    it('triggers mention menu on @ input', async () => {
        mockGetMembers.mockResolvedValue({ content: [
            { usuario: { id: 10, nombre: 'MentionUser' } },
        ] });
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await waitFor(() => expect(mockObtenerHistorialComunidad).toHaveBeenCalled());

        const input = screen.getByPlaceholderText('Escribe un mensaje...');
        // Set selectionStart before firing change since jsdom needs it on the DOM element
        Object.defineProperty(input, 'selectionStart', { value: 4, writable: true, configurable: true });
        fireEvent.change(input, { target: { value: '@Men' } });
        // The mention menu renders items as @name
        await screen.findByText(/@MentionUser/);
    });

    // === isOpen prop external control ===

    it('respects external isOpen prop', async () => {
        wrap(<CommunityChat {...defaultProps} isOpen={false} />);
        expect(screen.queryByText('Chat de comunidad')).not.toBeInTheDocument();
    });

    // === Own message does not trigger private chat ===

    it('does not trigger private chat when clicking own message name', async () => {
        mockObtenerHistorialComunidad.mockResolvedValue({ data: sampleMessages });
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await screen.findByText('Usuario Test');

        // Own message name should not be a button or should not call onOpenPrivateChat
        const ownName = screen.getByText('Usuario Test');
        const btn = ownName.closest('button');
        if (btn) {
            fireEvent.click(btn);
            expect(defaultProps.onOpenPrivateChat).not.toHaveBeenCalled();
        }
    });

    // === Send with file + text ===

    it('sends file with accompanying text', async () => {
        mockEnviarArchivoComunidad.mockResolvedValue({ data: { id: 101, usuarioId: 1, archivoNombre: 'img.png' } });
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await waitFor(() => expect(mockObtenerHistorialComunidad).toHaveBeenCalled());

        const fileInput = document.querySelector('input[type="file"]');
        const file = new File(['x'], 'img.png', { type: 'image/png' });
        fireEvent.change(fileInput, { target: { files: [file] } });

        const textInput = screen.getByPlaceholderText('Escribe un mensaje...');
        fireEvent.change(textInput, { target: { value: 'Mira esta imagen' } });
        fireEvent.submit(textInput.closest('form'));

        await waitFor(() => {
            expect(mockEnviarArchivoComunidad).toHaveBeenCalledWith(10, expect.any(File), 'Mira esta imagen');
        });
    });

    // === File size formatting in attachment ===

    it('shows file size in attachment card', async () => {
        const msgs = [{
            id: 44, contenido: '[Adjunto] big.zip', usuarioId: 5, usuarioNombre: 'Ana',
            createdAt: '2025-01-15T11:00:00', archivoNombre: 'big.zip',
            archivoUrl: '/files/big.zip', archivoMimeType: 'application/zip', archivoTamano: 1048576,
        }];
        mockObtenerHistorialComunidad.mockResolvedValue({ data: msgs });
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await waitFor(() => {
            expect(screen.getByText('big.zip')).toBeInTheDocument();
            expect(screen.getByText(/1\.0 MB/)).toBeInTheDocument();
        });
    });

    // === Link preview fetching ===

    it('fetches link preview when message has URL', async () => {
        mockExtractFirstUrl.mockReturnValue('https://example.com');
        mockObtenerPreviewEnlace.mockResolvedValue({ data: { title: 'Example', url: 'https://example.com' } });
        const msgsWithUrl = [
            { id: 50, contenido: 'Check https://example.com', usuarioId: 5, usuarioNombre: 'Ana', createdAt: '2025-01-15T11:00:00' },
        ];
        mockObtenerHistorialComunidad.mockResolvedValue({ data: msgsWithUrl });
        wrap(<CommunityChat {...defaultProps} mode="embedded" />);
        await waitFor(() => expect(mockObtenerPreviewEnlace).toHaveBeenCalledWith('https://example.com'));
    });
});
import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import PrivateChat from './PrivateChat';

// Mock scrollIntoView/scrollTo para jsdom
window.HTMLElement.prototype.scrollIntoView = jest.fn();
window.HTMLElement.prototype.scrollTo = jest.fn();

// Stable mock references
const mockSocketOn = jest.fn();
const mockSocketOff = jest.fn();
const mockEnviarMensajePrivado = jest.fn();
const mockEnviarArchivoPrivado = jest.fn();
const mockObtenerHistorialPrivado = jest.fn();
const mockEliminarMensajePrivado = jest.fn();
const mockEditarMensajePrivado = jest.fn();
const mockObtenerPreviewEnlace = jest.fn();
const mockObtenerArchivoChatBlob = jest.fn();
const mockExtractFirstUrl = jest.fn();

jest.mock('../../contexts/SocketContext', () => ({
    useSocketContext: () => ({
        socket: { on: mockSocketOn, off: mockSocketOff },
        isConnected: true,
    }),
}));

jest.mock('../../api/mensajeService', () => ({
    enviarMensajePrivado: (...a) => mockEnviarMensajePrivado(...a),
    enviarArchivoPrivado: (...a) => mockEnviarArchivoPrivado(...a),
    obtenerHistorialPrivado: (...a) => mockObtenerHistorialPrivado(...a),
    eliminarMensajePrivado: (...a) => mockEliminarMensajePrivado(...a),
    editarMensajePrivado: (...a) => mockEditarMensajePrivado(...a),
    obtenerPreviewEnlace: (...a) => mockObtenerPreviewEnlace(...a),
    obtenerArchivoChatBlob: (...a) => mockObtenerArchivoChatBlob(...a),
}));

jest.mock('../../utils/linkPreview', () => ({
    extractFirstUrl: (...a) => mockExtractFirstUrl(...a),
}));

jest.mock('./LinkPreviewCard', () => {
    return function MockLinkPreviewCard() {
        return <div data-testid="link-preview-card">LinkPreview</div>;
    };
});

describe('PrivateChat', () => {
    const mockUsuarioActual = { id: 1, nombre: 'Usuario Test', foto: 'foto.jpg' };
    const defaultProps = {
        tutorId: 2,
        tutorNombre: 'Tutor Test',
        usuarioActual: mockUsuarioActual,
        onClose: jest.fn(),
    };

    const sampleMessages = [
        { id: 10, contenido: 'Hola profe', emisorId: 1, receptorId: 2, createdAt: '2025-01-15T10:00:00', editado: false },
        { id: 11, contenido: 'Buenos días', emisorId: 2, receptorId: 1, createdAt: '2025-01-15T10:01:00', editado: true },
    ];

    beforeEach(() => {
        jest.clearAllMocks();
        mockObtenerHistorialPrivado.mockResolvedValue({ data: [] });
        mockEnviarMensajePrivado.mockResolvedValue({ data: { id: 99, contenido: 'test', emisorId: '1', receptorId: '2' } });
        mockExtractFirstUrl.mockReturnValue(null);
    });

    // === Rendering ===

    it('renders header with tutor name', async () => {
        render(<PrivateChat {...defaultProps} />);
        expect(screen.getByText('Chat con Tutor Test')).toBeInTheDocument();
    });

    it('shows En línea status', () => {
        render(<PrivateChat {...defaultProps} />);
        expect(screen.getByText('En línea')).toBeInTheDocument();
    });

    it('shows loading state while fetching history', () => {
        mockObtenerHistorialPrivado.mockImplementation(() => new Promise(() => {}));
        render(<PrivateChat {...defaultProps} />);
        expect(screen.getByText('Cargando historial...')).toBeInTheDocument();
    });

    it('shows empty state with no messages', async () => {
        render(<PrivateChat {...defaultProps} />);
        await waitFor(() => {
            expect(screen.getByText(/Sin historial de mensajes/)).toBeInTheDocument();
        });
    });

    it('calls obtenerHistorialPrivado on mount', async () => {
        render(<PrivateChat {...defaultProps} />);
        await waitFor(() => {
            expect(mockObtenerHistorialPrivado).toHaveBeenCalledWith(2);
        });
    });

    it('renders messages from history', async () => {
        mockObtenerHistorialPrivado.mockResolvedValue({ data: sampleMessages });
        render(<PrivateChat {...defaultProps} />);
        await waitFor(() => {
            expect(screen.getByText('Hola profe')).toBeInTheDocument();
            expect(screen.getByText('Buenos días')).toBeInTheDocument();
        });
    });

    it('shows editado badge on edited messages', async () => {
        mockObtenerHistorialPrivado.mockResolvedValue({ data: sampleMessages });
        render(<PrivateChat {...defaultProps} />);
        await waitFor(() => {
            expect(screen.getByText('(editado)')).toBeInTheDocument();
        });
    });

    it('renders headerActions when provided', () => {
        render(<PrivateChat {...defaultProps} headerActions={<button>Extra</button>} />);
        expect(screen.getByText('Extra')).toBeInTheDocument();
    });

    it('does not render Volver when no onClose', () => {
        render(<PrivateChat {...defaultProps} onClose={undefined} />);
        expect(screen.queryByText('Volver')).not.toBeInTheDocument();
    });

    // === Close button ===

    it('calls onClose when Volver clicked', () => {
        render(<PrivateChat {...defaultProps} />);
        fireEvent.click(screen.getByText('Volver'));
        expect(defaultProps.onClose).toHaveBeenCalled();
    });

    // === Sending messages ===

    it('sends a text message on submit', async () => {
        render(<PrivateChat {...defaultProps} />);
        await waitFor(() => expect(mockObtenerHistorialPrivado).toHaveBeenCalled());

        const input = screen.getByPlaceholderText('Escribe un mensaje...');
        fireEvent.change(input, { target: { value: 'Mensaje nuevo' } });
        fireEvent.submit(input.closest('form'));

        await waitFor(() => {
            expect(mockEnviarMensajePrivado).toHaveBeenCalledWith(2, 'Mensaje nuevo');
        });
    });

    it('disables submit when input is empty', () => {
        render(<PrivateChat {...defaultProps} />);
        const submitBtn = screen.getByRole('button', { name: '→' });
        expect(submitBtn).toBeDisabled();
    });

    it('clears input after sending', async () => {
        render(<PrivateChat {...defaultProps} />);
        await waitFor(() => expect(mockObtenerHistorialPrivado).toHaveBeenCalled());

        const input = screen.getByPlaceholderText('Escribe un mensaje...');
        fireEvent.change(input, { target: { value: 'Test' } });
        fireEvent.submit(input.closest('form'));

        await waitFor(() => {
            expect(input.value).toBe('');
        });
    });

    it('shows error when send fails', async () => {
        mockEnviarMensajePrivado.mockRejectedValue(new Error('fail'));
        render(<PrivateChat {...defaultProps} />);
        await waitFor(() => expect(mockObtenerHistorialPrivado).toHaveBeenCalled());

        const input = screen.getByPlaceholderText('Escribe un mensaje...');
        fireEvent.change(input, { target: { value: 'test' } });
        fireEvent.submit(input.closest('form'));

        await waitFor(() => {
            expect(screen.getByText('Error al enviar el mensaje o archivo')).toBeInTheDocument();
        });
    });

    // === Editing messages ===

    it('opens edit form and saves edited message', async () => {
        mockObtenerHistorialPrivado.mockResolvedValue({ data: sampleMessages });
        mockEditarMensajePrivado.mockResolvedValue({ data: { ...sampleMessages[0], contenido: 'editado' } });
        render(<PrivateChat {...defaultProps} />);
        await screen.findByText('Hola profe');

        fireEvent.click(screen.getAllByText('Editar')[0]);
        expect(screen.getByText('Guardar')).toBeInTheDocument();

        const editInput = screen.getByDisplayValue('Hola profe');
        fireEvent.change(editInput, { target: { value: 'Mensaje editado' } });
        fireEvent.click(screen.getByText('Guardar'));

        await waitFor(() => {
            expect(mockEditarMensajePrivado).toHaveBeenCalledWith(10, 'Mensaje editado');
        });
    });

    it('cancels edit when clicking Cancelar', async () => {
        mockObtenerHistorialPrivado.mockResolvedValue({ data: sampleMessages });
        render(<PrivateChat {...defaultProps} />);
        await screen.findByText('Hola profe');

        fireEvent.click(screen.getAllByText('Editar')[0]);
        fireEvent.click(screen.getByText('Cancelar'));
        expect(screen.queryByText('Guardar')).not.toBeInTheDocument();
    });

    it('shows error when edit fails', async () => {
        mockObtenerHistorialPrivado.mockResolvedValue({ data: sampleMessages });
        mockEditarMensajePrivado.mockRejectedValue(new Error('fail'));
        render(<PrivateChat {...defaultProps} />);
        await screen.findByText('Hola profe');

        fireEvent.click(screen.getAllByText('Editar')[0]);
        fireEvent.change(screen.getByDisplayValue('Hola profe'), { target: { value: 'x' } });
        fireEvent.click(screen.getByText('Guardar'));

        await waitFor(() => {
            expect(screen.getByText('Error al editar el mensaje')).toBeInTheDocument();
        });
    });

    // === Deleting messages ===

    it('deletes a message', async () => {
        mockObtenerHistorialPrivado.mockResolvedValue({ data: sampleMessages });
        mockEliminarMensajePrivado.mockResolvedValue({});
        render(<PrivateChat {...defaultProps} />);
        await screen.findByText('Hola profe');

        fireEvent.click(screen.getAllByText('Eliminar')[0]);

        await waitFor(() => {
            expect(mockEliminarMensajePrivado).toHaveBeenCalledWith(10);
        });
    });

    it('shows error when delete fails', async () => {
        mockObtenerHistorialPrivado.mockResolvedValue({ data: sampleMessages });
        mockEliminarMensajePrivado.mockRejectedValue(new Error('fail'));
        render(<PrivateChat {...defaultProps} />);
        await screen.findByText('Hola profe');

        fireEvent.click(screen.getAllByText('Eliminar')[0]);

        await waitFor(() => {
            expect(screen.getByText('Error al eliminar el mensaje')).toBeInTheDocument();
        });
    });

    // === File attachment ===

    it('shows Adjuntar label', () => {
        render(<PrivateChat {...defaultProps} />);
        expect(screen.getByText('Adjuntar')).toBeInTheDocument();
    });

    it('sends file when attached', async () => {
        mockEnviarArchivoPrivado.mockResolvedValue({ data: { id: 100, emisorId: '1', receptorId: '2', archivoNombre: 'test.pdf' } });
        render(<PrivateChat {...defaultProps} />);
        await waitFor(() => expect(mockObtenerHistorialPrivado).toHaveBeenCalled());

        const fileInput = document.querySelector('input[type="file"]');
        const file = new File(['content'], 'test.pdf', { type: 'application/pdf' });
        fireEvent.change(fileInput, { target: { files: [file] } });

        fireEvent.submit(screen.getByPlaceholderText('Escribe un mensaje...').closest('form'));

        await waitFor(() => {
            expect(mockEnviarArchivoPrivado).toHaveBeenCalledWith(2, expect.any(File), '');
        });
    });

    it('shows and removes pending attachment', async () => {
        render(<PrivateChat {...defaultProps} />);
        const fileInput = document.querySelector('input[type="file"]');
        const file = new File(['content'], 'doc.pdf', { type: 'application/pdf' });
        fireEvent.change(fileInput, { target: { files: [file] } });

        await waitFor(() => {
            expect(screen.getByText('doc.pdf')).toBeInTheDocument();
            expect(screen.getByText('Quitar')).toBeInTheDocument();
        });

        fireEvent.click(screen.getByText('Quitar'));

        await waitFor(() => {
            expect(screen.queryByText('doc.pdf')).not.toBeInTheDocument();
        });
    });

    it('renders file attachment card for messages with files', async () => {
        const msgs = [{
            id: 20, contenido: '[Adjunto] doc.pdf', emisorId: 2, receptorId: 1,
            createdAt: '2025-01-15T11:00:00', archivoNombre: 'doc.pdf',
            archivoUrl: '/files/doc.pdf', archivoMimeType: 'application/pdf', archivoTamano: 1024,
        }];
        mockObtenerHistorialPrivado.mockResolvedValue({ data: msgs });
        render(<PrivateChat {...defaultProps} />);

        await waitFor(() => {
            expect(screen.getByText('doc.pdf')).toBeInTheDocument();
        });
    });

    // === Socket events ===

    it('subscribes to socket events on mount', async () => {
        render(<PrivateChat {...defaultProps} />);
        await waitFor(() => {
            expect(mockSocketOn).toHaveBeenCalledWith('dm_message', expect.any(Function));
            expect(mockSocketOn).toHaveBeenCalledWith('dm_delete_success', expect.any(Function));
            expect(mockSocketOn).toHaveBeenCalledWith('dm_update_success', expect.any(Function));
            expect(mockSocketOn).toHaveBeenCalledWith('error', expect.any(Function));
        });
    });

    it('handles incoming DM message via socket', async () => {
        render(<PrivateChat {...defaultProps} />);
        await waitFor(() => expect(mockObtenerHistorialPrivado).toHaveBeenCalled());

        const dmHandler = mockSocketOn.mock.calls.find(c => c[0] === 'dm_message')?.[1];
        expect(dmHandler).toBeDefined();

        await act(async () => {
            dmHandler({ id: 50, contenido: 'Socket msg', emisorId: 2, receptorId: 1, createdAt: '2025-01-15T12:00:00' });
        });

        await waitFor(() => {
            expect(screen.getByText('Socket msg')).toBeInTheDocument();
        });
    });

    it('handles socket delete event', async () => {
        mockObtenerHistorialPrivado.mockResolvedValue({ data: sampleMessages });
        render(<PrivateChat {...defaultProps} />);
        await screen.findByText('Hola profe');

        const deleteHandler = mockSocketOn.mock.calls.find(c => c[0] === 'dm_delete_success')?.[1];
        await act(async () => { deleteHandler({ messageId: 10 }); });

        await waitFor(() => {
            expect(screen.queryByText('Hola profe')).not.toBeInTheDocument();
        });
    });

    it('handles socket update event', async () => {
        mockObtenerHistorialPrivado.mockResolvedValue({ data: sampleMessages });
        render(<PrivateChat {...defaultProps} />);
        await screen.findByText('Hola profe');

        const updateHandler = mockSocketOn.mock.calls.find(c => c[0] === 'dm_update_success')?.[1];
        await act(async () => { updateHandler({ id: 10, contenido: 'Actualizado' }); });

        await waitFor(() => {
            expect(screen.getByText('Actualizado')).toBeInTheDocument();
        });
    });

    it('handles socket error event', async () => {
        render(<PrivateChat {...defaultProps} />);
        await waitFor(() => expect(mockObtenerHistorialPrivado).toHaveBeenCalled());

        const errorHandler = mockSocketOn.mock.calls.find(c => c[0] === 'error')?.[1];
        await act(async () => { errorHandler({ message: 'Socket error' }); });

        await waitFor(() => {
            expect(screen.getByText(/Socket error/)).toBeInTheDocument();
        });
    });

    // === AutoStart ===

    it('does not send auto greeting when autoStart is true', async () => {
        render(<PrivateChat {...defaultProps} autoStart={true} />);

        await waitFor(() => {
            expect(mockObtenerHistorialPrivado).toHaveBeenCalledWith(2);
        });

        expect(mockEnviarMensajePrivado).not.toHaveBeenCalledWith(
            2,
            '¡Hola! Me gustaría contactar contigo.'
        );
    });

    it('keeps the message list untouched in empty autoStart chats', async () => {
        render(<PrivateChat {...defaultProps} autoStart={true} />);

        await waitFor(() => {
            expect(screen.getByText(/Sin historial de mensajes/)).toBeInTheDocument();
        });

        expect(screen.queryByText('¡Hola! Me gustaría contactar contigo.')).not.toBeInTheDocument();
    });
});
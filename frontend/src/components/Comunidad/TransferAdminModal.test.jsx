import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import TransferAdminModal from './TransferAdminModal';
import { communitiesApi } from '../../api/communities.api';

jest.mock('../../api/communities.api', () => ({
    communitiesApi: {
        getMembers: jest.fn(),
        transferAdmin: jest.fn(),
    },
}));

const members = [
    { usuario: { id: 10, nombre: 'Ana García', avatarUrl: 'https://img.com/ana.jpg' }, rol: 'ALUMNO' },
    { usuario: { id: 11, nombre: 'Luis López', avatarUrl: null }, rol: 'PROFESOR' },
];

describe('TransferAdminModal', () => {
    const defaultProps = {
        communityId: 5,
        currentUserId: 1,
        hasTeacherProfile: false,
        onClose: jest.fn(),
        onTransferred: jest.fn(),
    };

    beforeEach(() => {
        jest.clearAllMocks();
        communitiesApi.getMembers.mockResolvedValue({ content: members });
        communitiesApi.transferAdmin.mockResolvedValue({});
    });

    it('shows loading state', () => {
        communitiesApi.getMembers.mockImplementation(() => new Promise(() => {}));
        render(<TransferAdminModal {...defaultProps} />);
        expect(screen.getByText('Cargando miembros...')).toBeInTheDocument();
    });

    it('renders member list', async () => {
        render(<TransferAdminModal {...defaultProps} />);
        await waitFor(() => {
            expect(screen.getByText('Ana García')).toBeInTheDocument();
            expect(screen.getByText('Luis López')).toBeInTheDocument();
        });
    });

    it('shows empty message when no other members', async () => {
        communitiesApi.getMembers.mockResolvedValue({ content: [{ usuario: { id: 1 }, rol: 'ADMIN' }] });
        render(<TransferAdminModal {...defaultProps} />);
        await waitFor(() => {
            expect(screen.getByText(/No hay otros miembros/)).toBeInTheDocument();
        });
    });

    it('shows error when members load fails', async () => {
        communitiesApi.getMembers.mockRejectedValue(new Error('fail'));
        render(<TransferAdminModal {...defaultProps} />);
        await waitFor(() => {
            expect(screen.getByText('No se pudieron cargar los miembros')).toBeInTheDocument();
        });
    });

    it('selects a member and shows confirm step', async () => {
        render(<TransferAdminModal {...defaultProps} />);
        await screen.findByText('Ana García');

        fireEvent.click(screen.getByText('Ana García'));
        fireEvent.click(screen.getByRole('button', { name: /Transferir/i }));

        expect(screen.getByText(/¿Estás seguro/)).toBeInTheDocument();
        expect(screen.getByText(/Ana García/)).toBeInTheDocument();
    });

    it('transfers admin on confirm', async () => {
        render(<TransferAdminModal {...defaultProps} />);
        await screen.findByText('Ana García');

        fireEvent.click(screen.getByText('Ana García'));
        fireEvent.click(screen.getByRole('button', { name: /Transferir/i }));
        fireEvent.click(screen.getByRole('button', { name: /Confirmar transferencia/i }));

        await waitFor(() => {
            expect(communitiesApi.transferAdmin).toHaveBeenCalledWith(5, 10, 'ALUMNO');
            expect(defaultProps.onTransferred).toHaveBeenCalled();
        });
    });

    it('goes back from confirm step', async () => {
        render(<TransferAdminModal {...defaultProps} />);
        await screen.findByText('Ana García');

        fireEvent.click(screen.getByText('Ana García'));
        fireEvent.click(screen.getByRole('button', { name: /Transferir/i }));
        fireEvent.click(screen.getByRole('button', { name: /Volver/i }));

        expect(screen.getByText(/Selecciona un miembro/)).toBeInTheDocument();
    });

    it('shows role selection when hasTeacherProfile', async () => {
        render(<TransferAdminModal {...defaultProps} hasTeacherProfile={true} />);
        await screen.findByText('Ana García');

        fireEvent.click(screen.getByText('Ana García'));
        fireEvent.click(screen.getByRole('button', { name: /Transferir/i }));

        expect(screen.getByText(/Elige tu nuevo rol/)).toBeInTheDocument();
        expect(screen.getByDisplayValue('PROFESOR')).toBeChecked();

        fireEvent.click(screen.getByDisplayValue('ALUMNO'));
        fireEvent.click(screen.getByRole('button', { name: /Confirmar transferencia/i }));

        await waitFor(() => {
            expect(communitiesApi.transferAdmin).toHaveBeenCalledWith(5, 10, 'ALUMNO');
        });
    });

    it('shows error on transfer failure', async () => {
        communitiesApi.transferAdmin.mockRejectedValue(new Error('Transfer failed'));
        render(<TransferAdminModal {...defaultProps} />);
        await screen.findByText('Ana García');

        fireEvent.click(screen.getByText('Ana García'));
        fireEvent.click(screen.getByRole('button', { name: /Transferir/i }));
        fireEvent.click(screen.getByRole('button', { name: /Confirmar transferencia/i }));

        await waitFor(() => {
            expect(screen.getByText('Transfer failed')).toBeInTheDocument();
        });
    });

    it('calls onClose when Cancel clicked', async () => {
        render(<TransferAdminModal {...defaultProps} />);
        await screen.findByText('Ana García');

        fireEvent.click(screen.getByRole('button', { name: /Cancelar/i }));
        expect(defaultProps.onClose).toHaveBeenCalled();
    });

    it('calls onClose when X clicked', async () => {
        render(<TransferAdminModal {...defaultProps} />);
        fireEvent.click(screen.getByText('✕'));
        expect(defaultProps.onClose).toHaveBeenCalled();
    });

    it('calls onClose when overlay clicked', async () => {
        render(<TransferAdminModal {...defaultProps} />);
        const overlay = document.querySelector('.tam-overlay');
        fireEvent.click(overlay);
        expect(defaultProps.onClose).toHaveBeenCalled();
    });

    it('disables Transfer button when no member selected', async () => {
        render(<TransferAdminModal {...defaultProps} />);
        await screen.findByText('Ana García');

        expect(screen.getByRole('button', { name: /Transferir/i })).toBeDisabled();
    });

    it('shows member initials when no avatar', async () => {
        render(<TransferAdminModal {...defaultProps} />);
        await waitFor(() => {
            expect(screen.getByText('L')).toBeInTheDocument(); // Luis initial
        });
    });
});

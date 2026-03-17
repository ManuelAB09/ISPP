import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import EditCommunityModal from './EditCommunityModal';
import { communitiesApi } from '../../api/communities.api';

jest.mock('../../api/communities.api', () => ({
  communitiesApi: {
    update: jest.fn(),
  },
}));

const baseCommunity = {
  id: 42,
  nombre: 'Comunidad Original',
  descripcion: 'Descripción original',
  imagenUrl: 'https://example.com/img.png',
};

describe('EditCommunityModal', () => {
  const onClose = jest.fn();
  const onSaved = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('renders form with community data', () => {
    render(<EditCommunityModal community={baseCommunity} onClose={onClose} onSaved={onSaved} />);
    expect(screen.getByText('Editar comunidad')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Comunidad Original')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Descripción original')).toBeInTheDocument();
  });

  test('shows error when name is empty', async () => {
    render(<EditCommunityModal community={baseCommunity} onClose={onClose} onSaved={onSaved} />);
    const nameInput = screen.getByLabelText('Nombre');
    await userEvent.clear(nameInput);
    fireEvent.click(screen.getByRole('button', { name: /guardar cambios/i }));
    expect(await screen.findByText('El nombre es obligatorio')).toBeInTheDocument();
    expect(communitiesApi.update).not.toHaveBeenCalled();
  });

  test('calls communitiesApi.update on valid submit', async () => {
    communitiesApi.update.mockResolvedValueOnce({});
    render(<EditCommunityModal community={baseCommunity} onClose={onClose} onSaved={onSaved} />);
    const nameInput = screen.getByLabelText('Nombre');
    await userEvent.clear(nameInput);
    await userEvent.type(nameInput, 'Nuevo nombre');
    fireEvent.click(screen.getByRole('button', { name: /guardar cambios/i }));
    await waitFor(() => {
      expect(communitiesApi.update).toHaveBeenCalledWith(42, {
        nombre: 'Nuevo nombre',
        descripcion: 'Descripción original',
        imagenUrl: 'https://example.com/img.png',
      });
    });
    expect(onSaved).toHaveBeenCalled();
  });

  test('shows error message on API failure', async () => {
    communitiesApi.update.mockRejectedValueOnce(new Error('Network error'));
    render(<EditCommunityModal community={baseCommunity} onClose={onClose} onSaved={onSaved} />);
    fireEvent.click(screen.getByRole('button', { name: /guardar cambios/i }));
    expect(await screen.findByText('Network error')).toBeInTheDocument();
  });

  test('calls onClose when clicking Cancelar', () => {
    render(<EditCommunityModal community={baseCommunity} onClose={onClose} onSaved={onSaved} />);
    fireEvent.click(screen.getByText('Cancelar'));
    expect(onClose).toHaveBeenCalled();
  });

  test('calls onClose when clicking overlay', () => {
    render(
      <EditCommunityModal community={baseCommunity} onClose={onClose} onSaved={onSaved} />
    );
    fireEvent.click(screen.getByTestId('ecm-overlay'));
    expect(onClose).toHaveBeenCalled();
  });

  test('calls onClose when clicking X button', () => {
    render(<EditCommunityModal community={baseCommunity} onClose={onClose} onSaved={onSaved} />);
    fireEvent.click(screen.getByText('X'));
    expect(onClose).toHaveBeenCalled();
  });
});

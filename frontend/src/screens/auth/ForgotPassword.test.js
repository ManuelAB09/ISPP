import React from 'react';
import { MemoryRouter } from 'react-router-dom';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';

import ForgotPassword from './ForgotPassword';
import { apiClient } from '../../api/client';

jest.mock('../../api/client', () => ({
  apiClient: {
    post: jest.fn(),
  },
}));

describe('ForgotPassword', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('shows validation error when email is empty', async () => {
    render(
      <MemoryRouter>
        <ForgotPassword />
      </MemoryRouter>
    );

    fireEvent.click(screen.getByRole('button', { name: /enviar enlace de recuperación/i }));

    expect(await screen.findByText(/correo electrónico no puede estar vacío/i)).toBeInTheDocument();
    expect(apiClient.post).not.toHaveBeenCalled();
  });

  test('shows validation error when email format is invalid', async () => {
    render(
      <MemoryRouter>
        <ForgotPassword />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText(/correo electrónico/i), {
      target: { value: 'correo-invalido' },
    });
    fireEvent.submit(screen.getByRole('button', { name: /enviar enlace de recuperación/i }).closest('form'));

    expect(await screen.findByText(/introduce un correo electrónico válido/i)).toBeInTheDocument();
    expect(apiClient.post).not.toHaveBeenCalled();
  });

  test('submits form and shows success message', async () => {
    apiClient.post.mockResolvedValue({
      data: { message: 'Correo enviado correctamente' },
    });

    render(
      <MemoryRouter>
        <ForgotPassword />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText(/correo electrónico/i), {
      target: { value: 'user@example.com' },
    });
    fireEvent.click(screen.getByRole('button', { name: /enviar enlace de recuperación/i }));

    await waitFor(() => {
      expect(apiClient.post).toHaveBeenCalledWith('/api/v1/auth/password/forgot', {
        email: 'user@example.com',
      });
    });

    expect(await screen.findByText(/correo enviado correctamente/i)).toBeInTheDocument();
    expect(screen.getByText(/el enlace expirará en 15 minutos/i)).toBeInTheDocument();
  });

  test('shows generic success message when API request fails', async () => {
    apiClient.post.mockRejectedValue(new Error('Server error'));

    render(
      <MemoryRouter>
        <ForgotPassword />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText(/correo electrónico/i), {
      target: { value: 'user@example.com' },
    });
    fireEvent.click(screen.getByRole('button', { name: /enviar enlace de recuperación/i }));

    expect(
      await screen.findByText(
        /si el email existe en el sistema, recibirás instrucciones de recuperación en tu bandeja de entrada/i
      )
    ).toBeInTheDocument();
  });
});

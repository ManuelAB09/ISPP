import React from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';

import ResetPassword from './ResetPassword';
import { apiClient } from '../../api/client';

jest.mock('../../api/client', () => ({
  apiClient: {
    post: jest.fn(),
  },
}));

function renderWithRoute(route) {
  return render(
    <MemoryRouter initialEntries={[route]}>
      <Routes>
        <Route path="/reset-password" element={<ResetPassword />} />
      </Routes>
    </MemoryRouter>
  );
}

describe('ResetPassword', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('shows invalid-link screen when token is missing', () => {
    renderWithRoute('/reset-password');

    expect(screen.getByText(/enlace inválido/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /solicitar nuevo enlace/i })).toBeInTheDocument();
  });

  test('shows password requirement error when password does not meet policy', async () => {
    renderWithRoute('/reset-password?token=abc123');

    fireEvent.change(screen.getByLabelText(/nueva contraseña/i), {
      target: { value: 'abc' },
    });
    fireEvent.change(screen.getByLabelText(/confirmar contraseña/i), {
      target: { value: 'abc' },
    });

    const submit = screen.getByRole('button', { name: /restablecer contraseña/i });
    submit.removeAttribute('disabled');
    fireEvent.click(submit);

    expect(await screen.findByText(/no cumple todos los requisitos/i)).toBeInTheDocument();
    expect(apiClient.post).not.toHaveBeenCalled();
  });

  test('shows mismatch error when passwords are different', async () => {
    renderWithRoute('/reset-password?token=abc123');

    fireEvent.change(screen.getByLabelText(/nueva contraseña/i), {
      target: { value: 'Abcd1234' },
    });
    fireEvent.change(screen.getByLabelText(/confirmar contraseña/i), {
      target: { value: 'Abcd9999' },
    });

    const submit = screen.getByRole('button', { name: /restablecer contraseña/i });
    submit.removeAttribute('disabled');
    fireEvent.click(submit);

    expect(await screen.findByText(/las contraseñas no coinciden/i)).toBeInTheDocument();
    expect(apiClient.post).not.toHaveBeenCalled();
  });

  test('submits valid password and shows success state', async () => {
    apiClient.post.mockResolvedValue({ ok: true });

    renderWithRoute('/reset-password?token=token-ok');

    fireEvent.change(screen.getByLabelText(/nueva contraseña/i), {
      target: { value: 'Abcd1234' },
    });
    fireEvent.change(screen.getByLabelText(/confirmar contraseña/i), {
      target: { value: 'Abcd1234' },
    });

    fireEvent.click(screen.getByRole('button', { name: /restablecer contraseña/i }));

    await waitFor(() => {
      expect(apiClient.post).toHaveBeenCalledWith('/api/v1/auth/password/reset', {
        token: 'token-ok',
        newPassword: 'Abcd1234',
      });
    });

    expect(await screen.findByText(/contraseña restablecida/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /iniciar sesión/i })).toBeInTheDocument();
  });

  test('shows api error message when reset fails', async () => {
    apiClient.post.mockRejectedValue({ message: 'Token expirado' });

    renderWithRoute('/reset-password?token=token-fail');

    fireEvent.change(screen.getByLabelText(/nueva contraseña/i), {
      target: { value: 'Abcd1234' },
    });
    fireEvent.change(screen.getByLabelText(/confirmar contraseña/i), {
      target: { value: 'Abcd1234' },
    });

    fireEvent.click(screen.getByRole('button', { name: /restablecer contraseña/i }));

    expect(await screen.findByText(/token expirado/i)).toBeInTheDocument();
  });

  test('clears error on input change and toggles password visibility', async () => {
    renderWithRoute('/reset-password?token=token-ui');

    const newPasswordInput = screen.getByLabelText(/nueva contraseña/i);
    const confirmPasswordInput = screen.getByLabelText(/confirmar contraseña/i);

    fireEvent.change(newPasswordInput, { target: { value: 'Abcd1234' } });
    fireEvent.change(confirmPasswordInput, { target: { value: 'Different123' } });
    const submit = screen.getByRole('button', { name: /restablecer contraseña/i });
    submit.removeAttribute('disabled');
    fireEvent.click(submit);

    expect(await screen.findByText(/las contraseñas no coinciden/i)).toBeInTheDocument();

    fireEvent.change(newPasswordInput, { target: { value: 'Abcd1235' } });
    expect(screen.queryByText(/las contraseñas no coinciden/i)).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /mostrar contraseña/i }));
    expect(newPasswordInput).toHaveAttribute('type', 'text');
    expect(confirmPasswordInput).toHaveAttribute('type', 'text');

    fireEvent.change(confirmPasswordInput, { target: { value: 'Abcd1235' } });
    fireEvent.click(screen.getByRole('button', { name: /ocultar contraseña/i }));
    expect(newPasswordInput).toHaveAttribute('type', 'password');
    expect(confirmPasswordInput).toHaveAttribute('type', 'password');
  });
});

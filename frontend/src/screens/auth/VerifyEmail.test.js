import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import VerifyEmail from './VerifyEmail';

const mockVerifyEmail = jest.fn();
const mockResendVerification = jest.fn();
let mockIsAuthenticated = false;
let mockSearchParamsStr = 'token=abc123';

jest.mock('../../contexts/AuthContext', () => ({
  useAuth: () => ({
    verifyEmail: mockVerifyEmail,
    resendVerification: mockResendVerification,
    isAuthenticated: mockIsAuthenticated,
  }),
}));

jest.mock('../../static/images/MeerKatters_logo.png', () => 'logo.png');

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
  useSearchParams: () => [new URLSearchParams(mockSearchParamsStr)],
}));

describe('VerifyEmail', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();
    mockIsAuthenticated = false;
    mockSearchParamsStr = 'token=abc123';
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  const renderComponent = () =>
    render(
      <MemoryRouter>
        <VerifyEmail />
      </MemoryRouter>
    );

  test('shows verifying state initially', () => {
    mockVerifyEmail.mockReturnValue(new Promise(() => {}));
    renderComponent();
    expect(screen.getByText(/Verificando tu cuenta/i)).toBeInTheDocument();
  });

  test('shows success state after successful verification', async () => {
    mockVerifyEmail.mockResolvedValue({ success: true });
    renderComponent();
    expect(await screen.findByText(/Email verificado/i)).toBeInTheDocument();
    expect(screen.getByText(/ir al inicio ahora/i)).toBeInTheDocument();
  });

  test('shows error state when verification fails', async () => {
    mockVerifyEmail.mockResolvedValue({ success: false, error: 'Token inválido' });
    renderComponent();
    await waitFor(() => {
      expect(screen.getByText(/token inválido/i)).toBeInTheDocument();
    });
  });

  test('shows error with default message when no error provided', async () => {
    mockVerifyEmail.mockResolvedValue({ success: false });
    renderComponent();
    await waitFor(() => {
      expect(screen.getByText(/error al verificar el email/i)).toBeInTheDocument();
    });
  });

  test('shows no-token state when token is missing', async () => {
    mockSearchParamsStr = '';
    renderComponent();
    await waitFor(() => {
      expect(screen.getByText(/token no encontrado/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/ir a iniciar sesión/i)).toBeInTheDocument();
    expect(screen.getByText(/crear nueva cuenta/i)).toBeInTheDocument();
  });

  test('redirects if already authenticated', () => {
    mockIsAuthenticated = true;
    renderComponent();
    expect(mockNavigate).toHaveBeenCalledWith('/');
  });

  test('error state shows resend form', async () => {
    mockVerifyEmail.mockResolvedValue({ success: false, error: 'Expirado' });
    renderComponent();
    await waitFor(() => {
      expect(screen.getByText(/error de verificación/i)).toBeInTheDocument();
    });
    expect(screen.getByPlaceholderText(/tu email/i)).toBeInTheDocument();
    expect(screen.getByText(/reenviar email de verificación/i)).toBeInTheDocument();
  });

  test('resend verification with empty email shows error', async () => {
    mockVerifyEmail.mockResolvedValue({ success: false, error: 'Expirado' });
    renderComponent();
    await waitFor(() => {
      expect(screen.getByText(/reenviar email de verificación/i)).toBeInTheDocument();
    });
    await userEvent.click(screen.getByText(/reenviar email de verificación/i));
    await waitFor(() => {
      expect(screen.getByText(/introduce tu email/i)).toBeInTheDocument();
    });
  });

  test('resend verification success', async () => {
    mockVerifyEmail.mockResolvedValue({ success: false, error: 'Expirado' });
    mockResendVerification.mockResolvedValue({ success: true });
    renderComponent();
    await waitFor(() => {
      expect(screen.getByPlaceholderText(/tu email/i)).toBeInTheDocument();
    });
    await userEvent.type(screen.getByPlaceholderText(/tu email/i), 'user@test.com');
    await userEvent.click(screen.getByText(/reenviar email de verificación/i));
    await waitFor(() => {
      expect(mockResendVerification).toHaveBeenCalledWith('user@test.com');
      expect(screen.getByText(/reenviado correctamente/i)).toBeInTheDocument();
    });
  });

  test('resend verification failure', async () => {
    mockVerifyEmail.mockResolvedValue({ success: false, error: 'Expirado' });
    mockResendVerification.mockResolvedValue({ success: false, error: 'Error al reenviar' });
    renderComponent();
    await waitFor(() => {
      expect(screen.getByPlaceholderText(/tu email/i)).toBeInTheDocument();
    });
    await userEvent.type(screen.getByPlaceholderText(/tu email/i), 'user@test.com');
    await userEvent.click(screen.getByText(/reenviar email de verificación/i));
    await waitFor(() => {
      expect(screen.getByText(/error al reenviar/i)).toBeInTheDocument();
    });
  });

  test('success state redirects after timeout', async () => {
    mockVerifyEmail.mockResolvedValue({ success: true });
    renderComponent();
    await waitFor(() => {
      expect(screen.getByText(/email verificado/i)).toBeInTheDocument();
    });
    jest.advanceTimersByTime(3000);
    expect(mockNavigate).toHaveBeenCalledWith('/');
  });

  test('error state shows links to login and register', async () => {
    mockVerifyEmail.mockResolvedValue({ success: false, error: 'Expirado' });
    renderComponent();
    await waitFor(() => {
      expect(screen.getByText(/ir a iniciar sesión/i)).toBeInTheDocument();
      expect(screen.getByText(/crear nueva cuenta/i)).toBeInTheDocument();
    });
  });

  test('calls verifyEmail with the token from URL', async () => {
    mockVerifyEmail.mockResolvedValue({ success: true });
    renderComponent();
    await waitFor(() => {
      expect(mockVerifyEmail).toHaveBeenCalledWith('abc123');
    });
  });
});

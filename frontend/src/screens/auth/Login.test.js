import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import Login from './Login';

const mockLogin = jest.fn();
const mockClearError = jest.fn();
const mockResendVerification = jest.fn();
const mockProcessDirectLogin = jest.fn();
const mockLogin2fa = jest.fn();

let mockAuthValues = {};

jest.mock('../../contexts/AuthContext', () => ({
  useAuth: () => mockAuthValues,
}));

jest.mock('../../static/images/MeerKatters_logo.png', () => 'logo.png');

let capturedGoogleOnError;
jest.mock('../../components/GoogleAuthButton', () => {
  return function MockGoogleAuthButton({ onSuccess, onError, text }) {
    capturedGoogleOnError = onError;
    return <button data-testid="google-auth-btn" onClick={() => onSuccess({ token: 'gtoken' })}>{text}</button>;
  };
});

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

describe('Login', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockAuthValues = {
      login: mockLogin,
      processDirectLogin: mockProcessDirectLogin,
      login2fa: mockLogin2fa,
      resendVerification: mockResendVerification,
      error: null,
      clearError: mockClearError,
      isAuthenticated: false,
      loading: false,
    };
  });

  const renderComponent = () =>
    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    );

  test('renders email and password fields', () => {
    renderComponent();
    expect(screen.getByPlaceholderText('tu@correo.com')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('••••••••')).toBeInTheDocument();
  });

  test('renders login button', () => {
    renderComponent();
    expect(screen.getByRole('button', { name: /iniciar sesión/i })).toBeInTheDocument();
  });

  test('renders page heading', () => {
    renderComponent();
    expect(screen.getByText(/bienvenido/i)).toBeInTheDocument();
  });

  test('shows error for empty email', async () => {
    renderComponent();
    const submitBtn = screen.getByRole('button', { name: /iniciar sesión/i });
    await userEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/no puede estar vacío/i)).toBeInTheDocument();
    });
  });

  test('shows error for invalid email format', async () => {
    renderComponent();
    const emailInput = screen.getByPlaceholderText('tu@correo.com');
    await userEvent.type(emailInput, 'notanemail');

    const passwordInput = screen.getByPlaceholderText('••••••••');
    await userEvent.type(passwordInput, 'password123');

    const submitBtn = screen.getByRole('button', { name: /iniciar sesión/i });
    await userEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/correo electrónico válido/i)).toBeInTheDocument();
    });
  });

  test('shows error for empty password', async () => {
    renderComponent();
    const emailInput = screen.getByPlaceholderText('tu@correo.com');
    await userEvent.type(emailInput, 'user@test.com');

    const submitBtn = screen.getByRole('button', { name: /iniciar sesión/i });
    await userEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/contraseña no puede estar vacía/i)).toBeInTheDocument();
    });
  });

  test('calls login with credentials on submit and navigates on success', async () => {
    mockLogin.mockResolvedValue({ success: true });
    renderComponent();

    await userEvent.type(screen.getByPlaceholderText('tu@correo.com'), 'user@test.com');
    await userEvent.type(screen.getByPlaceholderText('••••••••'), 'password123');
    await userEvent.click(screen.getByRole('button', { name: /iniciar sesión/i }));

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith('user@test.com', 'password123');
      expect(mockNavigate).toHaveBeenCalledWith('/');
    });
  });

  test('has link to register page', () => {
    renderComponent();
    expect(screen.getByText(/crear cuenta/i)).toBeInTheDocument();
  });

  test('shows error message on login failure', async () => {
    mockLogin.mockResolvedValue({ success: false, error: 'Credenciales inválidas' });
    renderComponent();

    await userEvent.type(screen.getByPlaceholderText('tu@correo.com'), 'user@test.com');
    await userEvent.type(screen.getByPlaceholderText('••••••••'), 'wrong');
    await userEvent.click(screen.getByRole('button', { name: /iniciar sesión/i }));

    await waitFor(() => {
      expect(screen.getByText(/credenciales inválidas/i)).toBeInTheDocument();
    });
  });

  test('shows email not verified with resend button', async () => {
    mockLogin.mockResolvedValue({ success: false, error: 'Debes verificar tu email' });
    renderComponent();

    await userEvent.type(screen.getByPlaceholderText('tu@correo.com'), 'user@test.com');
    await userEvent.type(screen.getByPlaceholderText('••••••••'), 'pass123');
    await userEvent.click(screen.getByRole('button', { name: /iniciar sesión/i }));

    await waitFor(() => {
      expect(screen.getByText(/reenviar email de verificación/i)).toBeInTheDocument();
    });
  });

  test('resend verification sends email', async () => {
    mockLogin.mockResolvedValue({ success: false, error: 'Debes verificar tu email' });
    mockResendVerification.mockResolvedValue({ success: true });
    renderComponent();

    await userEvent.type(screen.getByPlaceholderText('tu@correo.com'), 'user@test.com');
    await userEvent.type(screen.getByPlaceholderText('••••••••'), 'pass123');
    await userEvent.click(screen.getByRole('button', { name: /iniciar sesión/i }));

    await waitFor(() => {
      expect(screen.getByText(/reenviar email de verificación/i)).toBeInTheDocument();
    });

    await userEvent.click(screen.getByText(/reenviar email de verificación/i));

    await waitFor(() => {
      expect(mockResendVerification).toHaveBeenCalledWith('user@test.com');
      expect(screen.getByText(/reenviado correctamente/i)).toBeInTheDocument();
    });
  });

  test('shows 2FA form when twoFactorRequired', async () => {
    mockLogin.mockResolvedValue({ success: true, twoFactorRequired: true, tempToken: 'temp123' });
    renderComponent();

    await userEvent.type(screen.getByPlaceholderText('tu@correo.com'), 'user@test.com');
    await userEvent.type(screen.getByPlaceholderText('••••••••'), 'pass123');
    await userEvent.click(screen.getByRole('button', { name: /iniciar sesión/i }));

    await waitFor(() => {
      expect(screen.getByText(/verificación en dos pasos/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/código 2fa/i)).toBeInTheDocument();
    });
  });

  test('submits 2FA code and navigates', async () => {
    mockLogin.mockResolvedValue({ success: true, twoFactorRequired: true, tempToken: 'temp123' });
    mockLogin2fa.mockResolvedValue({ success: true });
    renderComponent();

    await userEvent.type(screen.getByPlaceholderText('tu@correo.com'), 'user@test.com');
    await userEvent.type(screen.getByPlaceholderText('••••••••'), 'pass123');
    await userEvent.click(screen.getByRole('button', { name: /iniciar sesión/i }));

    await waitFor(() => {
      expect(screen.getByLabelText(/código 2fa/i)).toBeInTheDocument();
    });

    await userEvent.type(screen.getByLabelText(/código 2fa/i), '123456');
    await userEvent.click(screen.getByRole('button', { name: /verificar código/i }));

    await waitFor(() => {
      expect(mockLogin2fa).toHaveBeenCalledWith('temp123', '123456');
      expect(mockNavigate).toHaveBeenCalledWith('/');
    });
  });

  test('2FA back button returns to login form', async () => {
    mockLogin.mockResolvedValue({ success: true, twoFactorRequired: true, tempToken: 'temp123' });
    renderComponent();

    await userEvent.type(screen.getByPlaceholderText('tu@correo.com'), 'user@test.com');
    await userEvent.type(screen.getByPlaceholderText('••••••••'), 'pass123');
    await userEvent.click(screen.getByRole('button', { name: /iniciar sesión/i }));

    await waitFor(() => {
      expect(screen.getByText(/verificación en dos pasos/i)).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: /atrás/i }));

    await waitFor(() => {
      expect(screen.getByText(/bienvenido/i)).toBeInTheDocument();
    });
  });

  test('toggle password visibility', async () => {
    renderComponent();
    const passwordInput = screen.getByPlaceholderText('••••••••');
    expect(passwordInput.type).toBe('password');

    const toggleBtn = screen.getByLabelText(/mostrar contraseña/i);
    await userEvent.click(toggleBtn);
    expect(passwordInput.type).toBe('text');
  });

  test('shows already logged in message when authenticated', () => {
    mockAuthValues = { ...mockAuthValues, isAuthenticated: true };
    renderComponent();
    expect(screen.getByText(/ya has iniciado sesión/i)).toBeInTheDocument();
    expect(screen.getByText(/ir al inicio/i)).toBeInTheDocument();
    expect(screen.getByText(/ver mi perfil/i)).toBeInTheDocument();
  });

  test('Google auth success navigates', async () => {
    mockProcessDirectLogin.mockReturnValue({ success: true });
    renderComponent();

    await userEvent.click(screen.getByTestId('google-auth-btn'));

    await waitFor(() => {
      expect(mockProcessDirectLogin).toHaveBeenCalled();
      expect(mockNavigate).toHaveBeenCalledWith('/');
    });
  });

  test('Google auth error shows message', async () => {
    renderComponent();
    capturedGoogleOnError('Google falló');

    await waitFor(() => {
      expect(screen.getByText(/google falló/i)).toBeInTheDocument();
    });
  });

  test('Google auth 2FA', async () => {
    mockProcessDirectLogin.mockReturnValue({ success: true, twoFactorRequired: true, tempToken: 'gtemp' });
    renderComponent();

    await userEvent.click(screen.getByTestId('google-auth-btn'));

    await waitFor(() => {
      expect(screen.getByText(/verificación en dos pasos/i)).toBeInTheDocument();
    });
  });

  test('has forgot password link', () => {
    renderComponent();
    expect(screen.getByText(/olvidaste la contraseña/i)).toBeInTheDocument();
  });

  test('remember me checkbox', async () => {
    renderComponent();
    const checkbox = screen.getByText(/recordarme por 30 días/i);
    expect(checkbox).toBeInTheDocument();
  });

  test('2FA error shows message', async () => {
    mockLogin.mockResolvedValue({ success: true, twoFactorRequired: true, tempToken: 'temp123' });
    mockLogin2fa.mockResolvedValue({ success: false, error: 'Código incorrecto' });
    renderComponent();

    await userEvent.type(screen.getByPlaceholderText('tu@correo.com'), 'user@test.com');
    await userEvent.type(screen.getByPlaceholderText('••••••••'), 'pass123');
    await userEvent.click(screen.getByRole('button', { name: /iniciar sesión/i }));

    await waitFor(() => {
      expect(screen.getByLabelText(/código 2fa/i)).toBeInTheDocument();
    });

    await userEvent.type(screen.getByLabelText(/código 2fa/i), '000000');
    await userEvent.click(screen.getByRole('button', { name: /verificar código/i }));

    await waitFor(() => {
      expect(screen.getByText(/código incorrecto/i)).toBeInTheDocument();
    });
  });

  test('resend verification failure shows error', async () => {
    mockLogin.mockResolvedValue({ success: false, error: 'Debes verificar tu email' });
    mockResendVerification.mockResolvedValue({ success: false, error: 'Error al reenviar' });
    renderComponent();

    await userEvent.type(screen.getByPlaceholderText('tu@correo.com'), 'user@test.com');
    await userEvent.type(screen.getByPlaceholderText('••••••••'), 'pass123');
    await userEvent.click(screen.getByRole('button', { name: /iniciar sesión/i }));

    await waitFor(() => {
      expect(screen.getByText(/reenviar email de verificación/i)).toBeInTheDocument();
    });

    await userEvent.click(screen.getByText(/reenviar email de verificación/i));

    await waitFor(() => {
      expect(screen.getByText(/error al reenviar/i)).toBeInTheDocument();
    });
  });
});

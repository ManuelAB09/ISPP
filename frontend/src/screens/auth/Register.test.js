import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import Register from './Register';

const mockRegister = jest.fn();
const mockClearError = jest.fn();
const mockResendVerification = jest.fn();

let mockAuthValues = {};

jest.mock('../../contexts/AuthContext', () => ({
  useAuth: () => mockAuthValues,
}));

jest.mock('../../static/images/MeerKatters_logo.png', () => 'logo.png');

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

describe('Register', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockAuthValues = {
      register: mockRegister,
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
        <Register />
      </MemoryRouter>
    );

  const fillValidForm = async () => {
    await userEvent.type(screen.getByPlaceholderText('Nombre Apellidos'), 'Test User');
    await userEvent.type(screen.getByPlaceholderText('tu@correo.com'), 'user@test.com');
    await userEvent.type(screen.getByPlaceholderText('Introduce tu contraseña'), 'Password123');
    await userEvent.type(screen.getByPlaceholderText('Repite tu contraseña'), 'Password123');
    const checkboxes = screen.getAllByRole('checkbox');
    const termsCheckbox = checkboxes.find(cb => cb.name === 'acceptTerms') || checkboxes[0];
    await userEvent.click(termsCheckbox);
  };

  test('renders registration form fields', () => {
    renderComponent();
    expect(screen.getByPlaceholderText('Nombre Apellidos')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('tu@correo.com')).toBeInTheDocument();
  });

  test('shows error when terms not accepted', async () => {
    renderComponent();
    await userEvent.type(screen.getByPlaceholderText('Nombre Apellidos'), 'Test User');
    await userEvent.type(screen.getByPlaceholderText('tu@correo.com'), 'user@test.com');
    await userEvent.type(screen.getByPlaceholderText('Introduce tu contraseña'), 'Password123');
    await userEvent.type(screen.getByPlaceholderText('Repite tu contraseña'), 'Password123');

    const submitBtn = screen.getByRole('button', { name: /registrar cuenta/i });
    await userEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/términos y condiciones/i)).toBeInTheDocument();
    });
  });

  test('shows error when passwords do not match', async () => {
    renderComponent();
    await userEvent.type(screen.getByPlaceholderText('Nombre Apellidos'), 'Test User');
    await userEvent.type(screen.getByPlaceholderText('tu@correo.com'), 'user@test.com');
    await userEvent.type(screen.getByPlaceholderText('Introduce tu contraseña'), 'Password123');
    await userEvent.type(screen.getByPlaceholderText('Repite tu contraseña'), 'Different456');

    const checkboxes = screen.getAllByRole('checkbox');
    const termsCheckbox = checkboxes.find(cb => cb.name === 'acceptTerms') || checkboxes[0];
    await userEvent.click(termsCheckbox);

    const submitBtn = screen.getByRole('button', { name: /registrar cuenta/i });
    await userEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/contraseñas no coinciden/i)).toBeInTheDocument();
    });
  });

  test('shows error for invalid email format', async () => {
    renderComponent();
    await userEvent.type(screen.getByPlaceholderText('Nombre Apellidos'), 'User');
    await userEvent.type(screen.getByPlaceholderText('tu@correo.com'), 'bademail');
    await userEvent.type(screen.getByPlaceholderText('Introduce tu contraseña'), 'Password123');
    await userEvent.type(screen.getByPlaceholderText('Repite tu contraseña'), 'Password123');

    const checkboxes = screen.getAllByRole('checkbox');
    const termsCheckbox = checkboxes.find(cb => cb.name === 'acceptTerms') || checkboxes[0];
    await userEvent.click(termsCheckbox);

    const submitBtn = screen.getByRole('button', { name: /registrar cuenta/i });
    await userEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/correo electrónico válido/i)).toBeInTheDocument();
    });
  });

  test('successful registration shows verification message', async () => {
    mockRegister.mockResolvedValue({ success: true, requiresVerification: true });
    renderComponent();
    await fillValidForm();

    await userEvent.click(screen.getByRole('button', { name: /registrar cuenta/i }));

    await waitFor(() => {
      expect(mockRegister).toHaveBeenCalledWith('user@test.com', 'Password123', 'Test User', false);
      expect(screen.getByText(/revisa tu correo/i)).toBeInTheDocument();
      expect(screen.getByText(/user@test.com/i)).toBeInTheDocument();
    });
  });

  test('successful registration without verification navigates home', async () => {
    mockRegister.mockResolvedValue({ success: true });
    renderComponent();
    await fillValidForm();

    await userEvent.click(screen.getByRole('button', { name: /registrar cuenta/i }));

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/');
    });
  });

  test('registration failure shows error', async () => {
    mockRegister.mockResolvedValue({ success: false, error: 'Email ya registrado' });
    renderComponent();
    await fillValidForm();

    await userEvent.click(screen.getByRole('button', { name: /registrar cuenta/i }));

    await waitFor(() => {
      expect(screen.getByText(/email ya registrado/i)).toBeInTheDocument();
    });
  });

  test('resend verification from success screen', async () => {
    mockRegister.mockResolvedValue({ success: true, requiresVerification: true });
    mockResendVerification.mockResolvedValue({ success: true });
    renderComponent();
    await fillValidForm();

    await userEvent.click(screen.getByRole('button', { name: /registrar cuenta/i }));

    await waitFor(() => {
      expect(screen.getByText(/revisa tu correo/i)).toBeInTheDocument();
    });

    await userEvent.click(screen.getByText(/reenviar email/i));

    await waitFor(() => {
      expect(mockResendVerification).toHaveBeenCalledWith('user@test.com');
      expect(screen.getByText(/reenviado correctamente/i)).toBeInTheDocument();
    });
  });

  test('resend verification failure shows error', async () => {
    mockRegister.mockResolvedValue({ success: true, requiresVerification: true });
    mockResendVerification.mockResolvedValue({ success: false, error: 'Error al reenviar' });
    renderComponent();
    await fillValidForm();

    await userEvent.click(screen.getByRole('button', { name: /registrar cuenta/i }));

    await waitFor(() => {
      expect(screen.getByText(/revisa tu correo/i)).toBeInTheDocument();
    });

    await userEvent.click(screen.getByText(/reenviar email/i));

    await waitFor(() => {
      expect(screen.getByText(/error al reenviar/i)).toBeInTheDocument();
    });
  });

  test('shows error for empty name', async () => {
    renderComponent();
    await userEvent.type(screen.getByPlaceholderText('tu@correo.com'), 'user@test.com');
    await userEvent.type(screen.getByPlaceholderText('Introduce tu contraseña'), 'Password123');
    await userEvent.type(screen.getByPlaceholderText('Repite tu contraseña'), 'Password123');
    const checkboxes = screen.getAllByRole('checkbox');
    await userEvent.click(checkboxes.find(cb => cb.name === 'acceptTerms') || checkboxes[0]);

    await userEvent.click(screen.getByRole('button', { name: /registrar cuenta/i }));

    await waitFor(() => {
      expect(screen.getByText(/nombre no puede estar vacío/i)).toBeInTheDocument();
    });
  });

  test('shows error for short name', async () => {
    renderComponent();
    await userEvent.type(screen.getByPlaceholderText('Nombre Apellidos'), 'A');
    await userEvent.type(screen.getByPlaceholderText('tu@correo.com'), 'user@test.com');
    await userEvent.type(screen.getByPlaceholderText('Introduce tu contraseña'), 'Password123');
    await userEvent.type(screen.getByPlaceholderText('Repite tu contraseña'), 'Password123');
    const checkboxes = screen.getAllByRole('checkbox');
    await userEvent.click(checkboxes.find(cb => cb.name === 'acceptTerms') || checkboxes[0]);

    await userEvent.click(screen.getByRole('button', { name: /registrar cuenta/i }));

    await waitFor(() => {
      expect(screen.getByText(/al menos 2 caracteres/i)).toBeInTheDocument();
    });
  });

  test('shows error for short password', async () => {
    renderComponent();
    await userEvent.type(screen.getByPlaceholderText('Nombre Apellidos'), 'Test User');
    await userEvent.type(screen.getByPlaceholderText('tu@correo.com'), 'user@test.com');
    await userEvent.type(screen.getByPlaceholderText('Introduce tu contraseña'), 'Pass1');
    await userEvent.type(screen.getByPlaceholderText('Repite tu contraseña'), 'Pass1');
    const checkboxes = screen.getAllByRole('checkbox');
    await userEvent.click(checkboxes.find(cb => cb.name === 'acceptTerms') || checkboxes[0]);

    await userEvent.click(screen.getByRole('button', { name: /registrar cuenta/i }));

    await waitFor(() => {
      expect(screen.getByText(/al menos 8 caracteres/i)).toBeInTheDocument();
    });
  });

  test('shows password requirements when typing', async () => {
    renderComponent();
    await userEvent.type(screen.getByPlaceholderText('Introduce tu contraseña'), 'Ab1');

    await waitFor(() => {
      expect(screen.getByText(/mínimo 8 caracteres/i)).toBeInTheDocument();
      expect(screen.getByText(/al menos una mayúscula/i)).toBeInTheDocument();
      expect(screen.getByText(/al menos una minúscula/i)).toBeInTheDocument();
      expect(screen.getByText(/al menos un número/i)).toBeInTheDocument();
    });
  });

  test('toggle password visibility', async () => {
    renderComponent();
    const passwordInput = screen.getByPlaceholderText('Introduce tu contraseña');
    expect(passwordInput.type).toBe('password');

    const toggleBtns = screen.getAllByLabelText(/mostrar contraseña/i);
    await userEvent.click(toggleBtns[0]);
    expect(passwordInput.type).toBe('text');
  });

  test('tutor toggle checkbox is present', () => {
    renderComponent();
    expect(screen.getByText(/registrarme como tutor/i)).toBeInTheDocument();
  });

  test('registers as tutor when toggle checked', async () => {
    mockRegister.mockResolvedValue({ success: true });
    renderComponent();
    await fillValidForm();

    // Click tutor toggle
    const checkboxes = screen.getAllByRole('checkbox');
    const tutorToggle = checkboxes.find(cb => cb.name === 'esTutor');
    if (tutorToggle) await userEvent.click(tutorToggle);

    await userEvent.click(screen.getByRole('button', { name: /registrar cuenta/i }));

    await waitFor(() => {
      expect(mockRegister).toHaveBeenCalledWith('user@test.com', 'Password123', 'Test User', true);
    });
  });

  test('shows already logged in when authenticated', () => {
    mockAuthValues = { ...mockAuthValues, isAuthenticated: true };
    renderComponent();
    expect(screen.getByText(/ya has iniciado sesión/i)).toBeInTheDocument();
    expect(screen.getByText(/ir al inicio/i)).toBeInTheDocument();
  });

  test('has link to login page', () => {
    renderComponent();
    expect(screen.getByText(/iniciar sesión/i)).toBeInTheDocument();
  });

  test('shows password weak validation error', async () => {
    renderComponent();
    await userEvent.type(screen.getByPlaceholderText('Nombre Apellidos'), 'Test User');
    await userEvent.type(screen.getByPlaceholderText('tu@correo.com'), 'user@test.com');
    await userEvent.type(screen.getByPlaceholderText('Introduce tu contraseña'), 'abcdefgh');
    await userEvent.type(screen.getByPlaceholderText('Repite tu contraseña'), 'abcdefgh');
    const checkboxes = screen.getAllByRole('checkbox');
    await userEvent.click(checkboxes.find(cb => cb.name === 'acceptTerms') || checkboxes[0]);

    await userEvent.click(screen.getByRole('button', { name: /registrar cuenta/i }));

    await waitFor(() => {
      expect(screen.getByText(/mayúsculas, minúsculas y números/i)).toBeInTheDocument();
    });
  });
});

import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import Register from './Register';

const mockRegister = jest.fn();
const mockClearError = jest.fn();
const mockResendVerification = jest.fn();

jest.mock('../../contexts/AuthContext', () => ({
  useAuth: () => ({
    register: mockRegister,
    resendVerification: mockResendVerification,
    error: null,
    clearError: mockClearError,
    isAuthenticated: false,
    loading: false,
  }),
}));

jest.mock('../../static/images/MeerKatters_logo.png', () => 'logo.png');

jest.mock('@react-oauth/google', () => ({
  GoogleLogin: () => <div data-testid="google-login-mock" />,
  GoogleOAuthProvider: ({ children }) => <div>{children}</div>,
}));

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

describe('Register', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  const renderComponent = () =>
    render(
      <MemoryRouter>
        <Register />
      </MemoryRouter>
    );

  test('renders registration form fields', () => {
    renderComponent();
    expect(screen.getByPlaceholderText('Nombre Apellidos')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('tu@correo.com')).toBeInTheDocument();
  });

  test('shows error when terms not accepted', async () => {
    renderComponent();
    const nameInput = screen.getByPlaceholderText('Nombre Apellidos');
    const emailInput = screen.getByPlaceholderText('tu@correo.com');
    const passwordInput = screen.getByLabelText('Contraseña');
    const confirmPasswordInput = screen.getByLabelText('Repetir contraseña');

    await userEvent.type(nameInput, 'Test User');
    await userEvent.type(emailInput, 'user@test.com');
    await userEvent.type(passwordInput, 'Password123');
    await userEvent.type(confirmPasswordInput, 'Password123');

    const submitBtn = screen.getByRole('button', { name: /registrar cuenta/i });
    await userEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/términos y condiciones/i)).toBeInTheDocument();
    });
  });

  test('shows error when passwords do not match', async () => {
    renderComponent();
    const nameInput = screen.getByPlaceholderText('Nombre Apellidos');
    const emailInput = screen.getByPlaceholderText('tu@correo.com');
    const passwordInput = screen.getByLabelText('Contraseña');
    const confirmPasswordInput = screen.getByLabelText('Repetir contraseña');

    await userEvent.type(nameInput, 'Test User');
    await userEvent.type(emailInput, 'user@test.com');
    await userEvent.type(passwordInput, 'Password123');
    await userEvent.type(confirmPasswordInput, 'Different456');

    // Check the terms checkbox - there are multiple checkboxes (terms + tutor toggle)
    const checkboxes = screen.getAllByRole('checkbox');
    const termsCheckbox = checkboxes.find(cb => cb.name === 'acceptTerms') || checkboxes[checkboxes.length - 1];
    await userEvent.click(termsCheckbox);

    const submitBtn = screen.getByRole('button', { name: /registrar cuenta/i });
    await userEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/contraseñas no coinciden/i)).toBeInTheDocument();
    });
  });

  test('shows error for invalid email format', async () => {
    renderComponent();
    const emailInput = screen.getByPlaceholderText('tu@correo.com');
    const passwordInput = screen.getByLabelText('Contraseña');
    const confirmPasswordInput = screen.getByLabelText('Repetir contraseña');

    await userEvent.type(screen.getByPlaceholderText('Nombre Apellidos'), 'User');
    await userEvent.type(emailInput, 'bademail');
    await userEvent.type(passwordInput, 'Password123');
    await userEvent.type(confirmPasswordInput, 'Password123');

    const checkboxes = screen.getAllByRole('checkbox');
    const termsCheckbox = checkboxes.find(cb => cb.name === 'acceptTerms') || checkboxes[checkboxes.length - 1];
    await userEvent.click(termsCheckbox);

    const submitBtn = screen.getByRole('button', { name: /registrar cuenta/i });
    await userEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/correo electrónico válido/i)).toBeInTheDocument();
    });
  });

  test('has link to login page', () => {
    renderComponent();
    expect(screen.getByText(/iniciar sesión/i)).toBeInTheDocument();
  });
});

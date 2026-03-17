import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import Login from './Login';

const mockLogin = jest.fn();
const mockClearError = jest.fn();
const mockResendVerification = jest.fn();

jest.mock('../../contexts/AuthContext', () => ({
  useAuth: () => ({
    login: mockLogin,
    resendVerification: mockResendVerification,
    error: null,
    clearError: mockClearError,
    isAuthenticated: false,
    loading: false,
  }),
}));

jest.mock('../../static/images/MeerKatters_logo.png', () => 'logo.png');

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

describe('Login', () => {
  beforeEach(() => {
    jest.clearAllMocks();
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

  test('calls login with credentials on submit', async () => {
    mockLogin.mockResolvedValue({ success: true });
    renderComponent();

    await userEvent.type(screen.getByPlaceholderText('tu@correo.com'), 'user@test.com');
    await userEvent.type(screen.getByPlaceholderText('••••••••'), 'password123');
    await userEvent.click(screen.getByRole('button', { name: /iniciar sesión/i }));

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith('user@test.com', 'password123');
    });
  });

  test('has link to register page', () => {
    renderComponent();
    expect(screen.getByText(/crear cuenta/i)).toBeInTheDocument();
  });
});

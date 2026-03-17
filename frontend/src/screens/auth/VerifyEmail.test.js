import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import VerifyEmail from './VerifyEmail';

const mockVerifyEmail = jest.fn();
const mockResendVerification = jest.fn();

jest.mock('../../contexts/AuthContext', () => ({
  useAuth: () => ({
    verifyEmail: mockVerifyEmail,
    resendVerification: mockResendVerification,
    isAuthenticated: false,
  }),
}));

jest.mock('../../static/images/MeerKatters_logo.png', () => 'logo.png');

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
  useSearchParams: () => [new URLSearchParams('token=abc123')],
}));

describe('VerifyEmail', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  const renderComponent = () =>
    render(
      <MemoryRouter>
        <VerifyEmail />
      </MemoryRouter>
    );

  test('shows verifying state initially', () => {
    mockVerifyEmail.mockReturnValue(new Promise(() => {})); // never resolves
    renderComponent();

    expect(screen.getByText(/Verificando tu cuenta/i)).toBeInTheDocument();
  });

  test('shows success state after successful verification', async () => {
    mockVerifyEmail.mockResolvedValue({ success: true });
    renderComponent();

    expect(await screen.findByText(/Email verificado/i)).toBeInTheDocument();
  });

  test('shows error state when verification fails', async () => {
    mockVerifyEmail.mockResolvedValue({ success: false, error: 'Token inválido' });
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/token inválido|error al verificar/i)).toBeInTheDocument();
    });
  });

  test('calls verifyEmail with token from URL', async () => {
    mockVerifyEmail.mockResolvedValue({ success: true });
    renderComponent();

    await waitFor(() => {
      expect(mockVerifyEmail).toHaveBeenCalledWith('abc123');
    });
  });
});

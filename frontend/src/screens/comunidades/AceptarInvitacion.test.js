import { act, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { communitiesApi } from '../../api/communities.api';
import { useAuth } from '../../contexts/AuthContext';
import AceptarInvitacion from './AceptarInvitacion';

jest.mock('../../api/communities.api');
jest.mock('../../contexts/AuthContext');

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

describe('AceptarInvitacion', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();
  });

  test('guarda invitación pendiente y muestra acciones de autenticación si no hay sesión', async () => {
    useAuth.mockReturnValue({ isAuthenticated: false, loading: false });

    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/invitacion/codigo-demo/aceptar']}>
          <Routes>
            <Route path="/invitacion/:codigo/aceptar" element={<AceptarInvitacion />} />
          </Routes>
        </MemoryRouter>
      );
    });

    await screen.findByText(/Necesitas iniciar sesión o registrarte/i);

    const pending = JSON.parse(localStorage.getItem('pendingCommunityInvitation'));
    expect(pending.code).toBe('codigo-demo');
    expect(screen.getByRole('link', { name: /Iniciar sesión/i })).toHaveAttribute(
      'href',
      '/login?next=%2Finvitacion%2Fcodigo-demo%2Faceptar'
    );
  });

  test('acepta invitación cuando hay sesión y communityId en query', async () => {
    jest.useFakeTimers();
    useAuth.mockReturnValue({ isAuthenticated: true, loading: false });
    communitiesApi.acceptInvitationByCode.mockResolvedValue({ message: 'ok' });

    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/invitacion/codigo-demo/aceptar?communityId=12']}>
          <Routes>
            <Route path="/invitacion/:codigo/aceptar" element={<AceptarInvitacion />} />
          </Routes>
        </MemoryRouter>
      );
    });

    await waitFor(() => {
      expect(communitiesApi.acceptInvitationByCode).toHaveBeenCalledWith(12, 'codigo-demo');
    });

    await screen.findByText(/Invitación aceptada correctamente/i);
    act(() => {
      jest.advanceTimersByTime(1500);
    });
    expect(mockNavigate).toHaveBeenCalledWith('/comunidades/12');
    jest.useRealTimers();
  });
});

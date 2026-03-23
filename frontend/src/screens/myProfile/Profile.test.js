import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import Profile from './Profile';
import { useAuth } from '../../contexts/AuthContext';
import { authApi } from '../../api/auth.api';
import { communitiesApi } from '../../api/communities.api';
import { getMyTutorProfiles, getVerifiedTutors } from '../../api/tutorEndpoints';

jest.mock('../../contexts/AuthContext');
jest.mock('../../api/auth.api', () => ({
  authApi: {
    getUserById: jest.fn(),
  },
}));
jest.mock('../../api/communities.api', () => ({
  communitiesApi: {
    listMine: jest.fn(),
  },
}));
jest.mock('../../api/tutorEndpoints', () => ({
  getMyTutorProfiles: jest.fn(),
  getVerifiedTutors: jest.fn(),
}));
jest.mock('../../components/Header/Header', () => {
  return function MockHeader() {
    return <div data-testid="mock-header">Header</div>;
  };
});
jest.mock('../teacherProfile/CreateProfileModal', () => {
  return function MockCreateProfileModal() {
    return <div data-testid="mock-create-tutor-modal" />;
  };
});
jest.mock('./EditProfile', () => {
  return function MockEditProfile() {
    return <div data-testid="mock-edit-profile" />;
  };
});
jest.mock('./Settings', () => {
  return function MockSettings() {
    return <div data-testid="mock-settings" />;
  };
});

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

describe('Profile public view', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();
    localStorage.setItem('userId', '1');

    useAuth.mockReturnValue({
      isAuthenticated: true,
      loading: false,
      user: {
        id: 1,
        nombre: 'Owner User',
        email: 'owner@example.com',
        esTutor: false,
        bio: 'Bio privada',
      },
      updateProfile: jest.fn(),
      logout: jest.fn(),
    });

    communitiesApi.listMine.mockResolvedValue({ content: [] });
    getMyTutorProfiles.mockResolvedValue([]);
    getVerifiedTutors.mockResolvedValue({ content: [] });
  });

  const renderRoute = (initialEntry = '/perfil/12') =>
    render(
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route path="/perfil" element={<Profile />} />
          <Route path="/perfil/:userId" element={<Profile />} />
        </Routes>
      </MemoryRouter>
    );

  test('carga y muestra el perfil público de otro usuario ocultando controles privados', async () => {
    authApi.getUserById.mockResolvedValue({
      id: 12,
      nombre: 'Paula Profe',
      foto: null,
      bio: 'Profesora de matemáticas',
      universidad: 'Universidad de Sevilla',
      grado: 'Matemáticas',
      ubicacion: { nombre: 'Sevilla' },
      intereses: ['Álgebra', 'Didáctica'],
      esTutor: true,
    });
    getVerifiedTutors.mockResolvedValue({
      content: [
        {
          id: 44,
          userId: 12,
          verificado: true,
          especialidades: ['Álgebra', 'Didáctica'],
        },
      ],
    });

    renderRoute('/perfil/12');

    await waitFor(() => {
      expect(authApi.getUserById).toHaveBeenCalledWith('12');
    });

    expect(await screen.findByRole('heading', { name: 'Paula Profe' })).toBeInTheDocument();
    expect(screen.getByText(/Profesora de matemáticas/i)).toBeInTheDocument();
    expect(screen.getByText('Universidad de Sevilla')).toBeInTheDocument();
    expect(screen.getByText('Matemáticas')).toBeInTheDocument();
    expect(screen.getByText('Sevilla')).toBeInTheDocument();
    expect(screen.getAllByText('Álgebra').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Didáctica').length).toBeGreaterThan(0);
    expect(screen.getByTitle(/Tutor verificado/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Ver perfil de profesor/i })).toBeInTheDocument();

    expect(screen.queryByRole('button', { name: /Editar Perfil/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Configuración/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Cerrar Sesión/i })).not.toBeInTheDocument();
    expect(screen.queryByText(/Mis comunidades/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/EMAIL/i)).not.toBeInTheDocument();
  });

  test('permite ir al perfil de profesor desde el perfil público verificado', async () => {
    authApi.getUserById.mockResolvedValue({
      id: 12,
      nombre: 'Paula Profe',
      foto: null,
      bio: 'Profesora de matemáticas',
      universidad: 'Universidad de Sevilla',
      grado: 'Matemáticas',
      ubicacion: { nombre: 'Sevilla' },
      intereses: ['Álgebra'],
      esTutor: true,
    });
    getVerifiedTutors.mockResolvedValue({
      content: [
        {
          id: 44,
          userId: 12,
          verificado: true,
          especialidades: ['Álgebra'],
        },
      ],
    });

    renderRoute('/perfil/12');

    const teacherProfileButton = await screen.findByRole('button', { name: /Ver perfil de profesor/i });
    userEvent.click(teacherProfileButton);

    expect(mockNavigate).toHaveBeenCalledWith('/profesores/44');
  });

  test('si el usuario público no está verificado como tutor, no muestra la insignia visible', async () => {
    authApi.getUserById.mockResolvedValue({
      id: 12,
      nombre: 'Paula Profe',
      foto: null,
      bio: 'Profesora de matemáticas',
      universidad: 'Universidad de Sevilla',
      grado: 'Matemáticas',
      ubicacion: { nombre: 'Sevilla' },
      intereses: ['Álgebra'],
      esTutor: true,
    });
    getVerifiedTutors.mockResolvedValue({ content: [] });

    renderRoute('/perfil/12');

    expect(await screen.findByRole('heading', { name: 'Paula Profe' })).toBeInTheDocument();
    expect(screen.queryByText(/Tutor verificado/i)).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Ver perfil de profesor/i })).not.toBeInTheDocument();
  });

  test('muestra estado de error si no se puede cargar el perfil público', async () => {
    authApi.getUserById.mockRejectedValue(new Error('not found'));

    renderRoute('/perfil/99');

    expect(await screen.findByText(/Perfil no disponible/i)).toBeInTheDocument();
    expect(screen.getByText(/No se pudo cargar este perfil/i)).toBeInTheDocument();
  });

  test('en el perfil propio no llama al endpoint público y mantiene controles de propietario', async () => {
    renderRoute('/perfil');

    expect(screen.getByRole('button', { name: /Editar Perfil/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Configuración/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Cerrar Sesión/i })).toBeInTheDocument();
    expect(authApi.getUserById).not.toHaveBeenCalled();
  });
});

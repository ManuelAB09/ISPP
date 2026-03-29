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
jest.mock('../../api/cuestionarios.api', () => ({
  cuestionariosApi: {
    listMine: jest.fn(),
    listPublicByUserId: jest.fn(),
  },
}));
jest.mock('../../api/client', () => ({
  apiClient: {
    post: jest.fn(),
  },
}));
jest.mock('../../api/baseUrl', () => ({
  getApiBaseUrl: () => 'http://localhost:8080',
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

  test('muestra nombre y bio del perfil propio', async () => {
    renderRoute('/perfil');

    expect(await screen.findByRole('heading', { name: 'Owner User' })).toBeInTheDocument();
    expect(screen.getByText('Bio privada')).toBeInTheDocument();
  });

  test('muestra email del propietario', async () => {
    renderRoute('/perfil');

    expect(await screen.findByText('owner@example.com')).toBeInTheDocument();
  });

  test('muestra rol Estudiante cuando no es tutor', async () => {
    renderRoute('/perfil');

    expect(await screen.findByText('Estudiante')).toBeInTheDocument();
  });

  test('muestra rol Estudiante y Profesor cuando es tutor', async () => {
    useAuth.mockReturnValue({
      isAuthenticated: true,
      loading: false,
      user: {
        id: 1,
        nombre: 'Tutor User',
        email: 'tutor@test.com',
        esTutor: true,
        bio: '',
      },
      updateProfile: jest.fn(),
      logout: jest.fn(),
    });
    getMyTutorProfiles.mockResolvedValue({ id: 10 });

    renderRoute('/perfil');

    expect(await screen.findByText('Estudiante y Profesor')).toBeInTheDocument();
  });

  test('muestra loading al cargar', () => {
    useAuth.mockReturnValue({
      isAuthenticated: true,
      loading: true,
      user: null,
      updateProfile: jest.fn(),
      logout: jest.fn(),
    });

    renderRoute('/perfil');

    expect(screen.getByText('Cargando...')).toBeInTheDocument();
  });

  test('muestra mensaje de no autenticado', () => {
    useAuth.mockReturnValue({
      isAuthenticated: false,
      loading: false,
      user: null,
      updateProfile: jest.fn(),
      logout: jest.fn(),
    });

    render(
      <MemoryRouter initialEntries={['/perfil']}>
        <Routes>
          <Route path="/perfil" element={<Profile />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText(/No has iniciado sesión/i)).toBeInTheDocument();
    expect(screen.getAllByText(/Iniciar sesión/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/Registrarse/i).length).toBeGreaterThan(0);
  });

  test('abre modal de editar perfil al clicar', async () => {
    renderRoute('/perfil');

    const editBtn = await screen.findByRole('button', { name: /Editar Perfil/i });
    await userEvent.click(editBtn);

    expect(screen.getByTestId('mock-edit-profile')).toBeInTheDocument();
  });

  test('abre modal de configuración al clicar', async () => {
    renderRoute('/perfil');

    const settingsBtn = await screen.findByRole('button', { name: /Configuración/i });
    await userEvent.click(settingsBtn);

    expect(screen.getByTestId('mock-settings')).toBeInTheDocument();
  });

  test('muestra comunidades del usuario', async () => {
    communitiesApi.listMine.mockResolvedValue({
      content: [
        { id: 1, nombre: 'Mi Comunidad', miRol: 'ADMIN', creador: { id: 1 } },
        { id: 2, nombre: 'Otra Comunidad', miRol: 'MIEMBRO', creador: { id: 2 } },
      ],
    });

    renderRoute('/perfil');

    expect(await screen.findByText('Mi Comunidad')).toBeInTheDocument();
    expect(screen.getByText('Otra Comunidad')).toBeInTheDocument();
  });

  test('carga cuestionarios del usuario propietario', async () => {
    const { cuestionariosApi } = require('../../api/cuestionarios.api');
    cuestionariosApi.listMine.mockResolvedValue([
      { id: 1, titulo: 'Quiz de Mates', materia: 'Matemáticas', createdAt: '2025-01-01' },
    ]);

    renderRoute('/perfil');

    await waitFor(() => {
      expect(cuestionariosApi.listMine).toHaveBeenCalled();
    });
  });

  test('maneja error de carga de comunidades', async () => {
    communitiesApi.listMine.mockRejectedValue(new Error('fail'));

    renderRoute('/perfil');

    await waitFor(() => {
      expect(communitiesApi.listMine).toHaveBeenCalled();
    });
    // Should still render the profile without crashing
    expect(screen.getByRole('heading', { name: 'Owner User' })).toBeInTheDocument();
  });

  test('llama a logout al cerrar sesión', async () => {
    const mockLogout = jest.fn();
    const { apiClient } = require('../../api/client');
    apiClient.post.mockResolvedValue({});
    useAuth.mockReturnValue({
      isAuthenticated: true,
      loading: false,
      user: { id: 1, nombre: 'Owner User', email: 'a@b.com', esTutor: false, bio: '' },
      updateProfile: jest.fn(),
      logout: mockLogout,
    });

    renderRoute('/perfil');

    const logoutBtn = await screen.findByRole('button', { name: /Cerrar Sesión/i });
    await userEvent.click(logoutBtn);

    await waitFor(() => {
      expect(mockLogout).toHaveBeenCalled();
    });
  });
});

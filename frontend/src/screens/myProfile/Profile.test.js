import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

jest.mock('../../api/communities.api', () => ({
  communitiesApi: {
    listMine: jest.fn(),
  },
}));

jest.mock('../../api/tutorEndpoints', () => ({
  getMyTutorProfiles: jest.fn(),
}));

jest.mock('../../api/client', () => ({
  apiClient: {
    get: jest.fn(),
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

jest.mock('../../contexts/AuthContext', () => ({
  useAuth: () => ({
    isAuthenticated: true,
    loading: false,
    user: {
      id: 1,
      nombre: 'Test User',
      email: 'test@example.com',
      bio: 'Test bio',
    },
    updateProfile: jest.fn(),
    logout: jest.fn(),
  }),
}));

jest.mock('../teacherProfile/CreateProfileModal', () => {
  return function MockCreateProfileModal() {
    return null;
  };
});

jest.mock('./EditProfile', () => {
  return function MockEditProfile() {
    return null;
  };
});

jest.mock('./Settings', () => {
  return function MockSettings() {
    return null;
  };
});

const { communitiesApi } = require('../../api/communities.api');
const { getMyTutorProfiles } = require('../../api/tutorEndpoints');

// Dynamic import to ensure mocks are set up first
let MyProfile;
beforeAll(() => {
  MyProfile = require('./Profile').default;
});

describe('MyProfile', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    communitiesApi.listMine.mockResolvedValue({ content: [] });
    getMyTutorProfiles.mockResolvedValue(null);
  });

  const renderComponent = () =>
    render(
      <MemoryRouter>
        <MyProfile />
      </MemoryRouter>
    );

  test('renders header', async () => {
    renderComponent();
    expect(screen.getByTestId('mock-header')).toBeInTheDocument();
  });

  test('displays user name', async () => {
    renderComponent();
    await waitFor(() => {
      expect(screen.getAllByText('Test User').length).toBeGreaterThan(0);
    });
  });

  test('displays user email', async () => {
    renderComponent();
    await waitFor(() => {
      expect(screen.getByText('test@example.com')).toBeInTheDocument();
    });
  });
});

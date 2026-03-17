import React from 'react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import LandingPage from './LandingPage';

jest.mock('../../static/images/MeerKatters_logo.png', () => 'logo.png');

jest.mock('../../contexts/AuthContext', () => ({
  useAuth: () => ({
    isAuthenticated: false,
  }),
}));

describe('LandingPage', () => {
  const renderComponent = () =>
    render(
      <MemoryRouter>
        <LandingPage />
      </MemoryRouter>
    );

  test('renders hero section with main heading', () => {
    renderComponent();
    expect(screen.getAllByText(/MeerKatters/i).length).toBeGreaterThan(0);
  });

  test('renders pricing section with plans', () => {
    renderComponent();
    expect(screen.getAllByText(/Básico/).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/Premium/).length).toBeGreaterThan(0);
  });

  test('renders features section', () => {
    renderComponent();
    expect(screen.getAllByText(/Comunidades de Estudio/i).length).toBeGreaterThan(0);
  });

  test('renders footer with copyright', () => {
    renderComponent();
    expect(screen.getByText(/Todos los derechos reservados/i)).toBeInTheDocument();
  });

  test('renders registration link', () => {
    renderComponent();
    expect(screen.getAllByText(/Registrarse/i).length).toBeGreaterThan(0);
  });
});

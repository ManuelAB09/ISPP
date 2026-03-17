import React from 'react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Privacy from './Privacy';

describe('Privacy', () => {
  const renderComponent = () =>
    render(
      <MemoryRouter>
        <Privacy />
      </MemoryRouter>
    );

  test('renders privacy policy heading', () => {
    renderComponent();
    expect(screen.getByRole('heading', { level: 1, name: /Política de Privacidad/i })).toBeInTheDocument();
  });

  test('renders information collection section', () => {
    renderComponent();
    expect(screen.getByText(/Información que Recopilamos/i)).toBeInTheDocument();
  });

  test('renders usage section', () => {
    renderComponent();
    expect(screen.getByText(/Cómo Usamos tu Información/i)).toBeInTheDocument();
  });

  test('renders last update date', () => {
    renderComponent();
    expect(screen.getByText(/Última actualización/i)).toBeInTheDocument();
  });

  test('renders logo link to home', () => {
    renderComponent();
    const logoLink = screen.getByRole('link', { name: /meerkatters logo/i });
    expect(logoLink).toHaveAttribute('href', '/');
  });
});

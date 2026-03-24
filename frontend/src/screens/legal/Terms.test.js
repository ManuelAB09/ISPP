import React from 'react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Terms from './Terms';

describe('Terms', () => {
  const renderComponent = () =>
    render(
      <MemoryRouter>
        <Terms />
      </MemoryRouter>
    );

  test('renders terms of service heading', () => {
    renderComponent();
    expect(screen.getByRole('heading', { level: 1, name: /Términos de Servicio/i })).toBeInTheDocument();
  });

  test('renders acceptance section', () => {
    renderComponent();
    expect(screen.getByText(/Aceptación de los Términos/i)).toBeInTheDocument();
  });

  test('renders service description section', () => {
    renderComponent();
    expect(screen.getByText(/Descripción del Servicio/i)).toBeInTheDocument();
  });

  test('renders acceptable use section', () => {
    renderComponent();
    expect(screen.getByText(/Uso Aceptable/i)).toBeInTheDocument();
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

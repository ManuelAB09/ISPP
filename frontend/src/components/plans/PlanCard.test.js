import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PlanCard from './PlanCard';

describe('PlanCard', () => {
  let mockOnCta;

  beforeEach(() => {
    jest.resetAllMocks();
    mockOnCta = jest.fn();
  });

  const renderCard = (props = {}) => {
    const defaultProps = {
      title: 'Plan Test',
      priceLabel: '9.99€/mes',
      badge: null,
      features: ['Feature 1', 'Feature 2', 'Feature 3'],
      ctaLabel: 'Seleccionar',
      ctaDisabled: false,
      onCta: mockOnCta,
    };
    return render(<PlanCard {...defaultProps} {...props} />);
  };

  // ==============================
  // TESTS DE RENDERIZADO
  // ==============================

  test('renderiza el título del plan', () => {
    renderCard({ title: 'PREMIUM' });
    expect(screen.getByRole('heading', { name: /PREMIUM/i })).toBeInTheDocument();
  });

  test('renderiza el precio del plan', () => {
    renderCard({ priceLabel: '2.99€/mes' });
    expect(screen.getByText('2.99€/mes')).toBeInTheDocument();
  });

  test('renderiza el badge cuando se proporciona', () => {
    renderCard({ badge: 'RECOMENDADO' });
    expect(screen.getByText('RECOMENDADO')).toBeInTheDocument();
  });

  test('no renderiza badge cuando no se proporciona', () => {
    renderCard({ badge: null });
    expect(screen.queryByText('RECOMENDADO')).not.toBeInTheDocument();
  });

  test('renderiza todas las características del plan', () => {
    const features = ['Comunidades ilimitadas', 'Soporte prioritario', 'Sin anuncios'];
    renderCard({ features });
    
    features.forEach(feature => {
      expect(screen.getByText(feature)).toBeInTheDocument();
    });
  });

  test('renderiza lista vacía cuando no hay características', () => {
    renderCard({ features: [] });
    const list = screen.getByRole('list');
    expect(list).toBeInTheDocument();
    expect(screen.queryAllByRole('listitem')).toHaveLength(0);
  });

  test('renderiza el botón con el texto del CTA', () => {
    renderCard({ ctaLabel: 'Mejorar a Premium' });
    expect(screen.getByRole('button', { name: /Mejorar a Premium/i })).toBeInTheDocument();
  });

  // ==============================
  // TESTS DE INTERACCIÓN
  // ==============================

  test('llama a onCta al hacer clic en el botón', async () => {
    renderCard();
    const button = screen.getByRole('button', { name: /Seleccionar/i });
    await userEvent.click(button);
    expect(mockOnCta).toHaveBeenCalledTimes(1);
  });

  test('no llama a onCta cuando el botón está deshabilitado', async () => {
    renderCard({ ctaDisabled: true });
    const button = screen.getByRole('button', { name: /Seleccionar/i });
    await userEvent.click(button);
    expect(mockOnCta).not.toHaveBeenCalled();
  });

  test('el botón está deshabilitado cuando ctaDisabled es true', () => {
    renderCard({ ctaDisabled: true });
    const button = screen.getByRole('button', { name: /Seleccionar/i });
    expect(button).toBeDisabled();
  });

  test('el botón está habilitado cuando ctaDisabled es false', () => {
    renderCard({ ctaDisabled: false });
    const button = screen.getByRole('button', { name: /Seleccionar/i });
    expect(button).not.toBeDisabled();
  });

  // ==============================
  // TESTS DE CASOS EDGE
  // ==============================

  test('maneja features como undefined', () => {
    renderCard({ features: undefined });
    const list = screen.getByRole('list');
    expect(list).toBeInTheDocument();
  });

  test('renderiza precio GRATIS correctamente', () => {
    renderCard({ priceLabel: 'GRATIS' });
    expect(screen.getByText('GRATIS')).toBeInTheDocument();
  });
});

import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import CheckoutModal from './CheckoutModal';

describe('CheckoutModal', () => {
  let mockOnClose;
  let mockOnConfirm;

  beforeEach(() => {
    jest.resetAllMocks();
    mockOnClose = jest.fn();
    mockOnConfirm = jest.fn();
  });

  const renderModal = (props = {}) => {
    const defaultProps = {
      open: true,
      plan: 'PREMIUM',
      onClose: mockOnClose,
      onConfirm: mockOnConfirm,
      loading: false,
    };
    return render(<CheckoutModal {...defaultProps} {...props} />);
  };

  // ==============================
  // TESTS DE RENDERIZADO
  // ==============================

  test('no renderiza nada cuando open es false', () => {
    renderModal({ open: false });
    expect(screen.queryByText(/Confirmar suscripción/i)).not.toBeInTheDocument();
  });

  test('renderiza el modal con el título correcto cuando open es true', () => {
    renderModal();
    expect(screen.getByText(/Confirmar suscripción/i)).toBeInTheDocument();
  });

  test('muestra el nombre del plan seleccionado', () => {
    renderModal({ plan: 'PREMIUM' });
    expect(screen.getByText(/PREMIUM mensual/i)).toBeInTheDocument();
    expect(screen.getByText(/PREMIUM anual/i)).toBeInTheDocument();
  });

  test('muestra los botones de período mensual y anual', () => {
    renderModal();
    expect(screen.getByRole('button', { name: /mensual/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /anual/i })).toBeInTheDocument();
  });

  test('muestra el texto informativo sobre el plan', () => {
    renderModal({ plan: 'PREMIUM' });
    expect(screen.getByText(/Al continuar, se activará tu plan/i)).toBeInTheDocument();
  });

  test('muestra los botones de Cancelar y Confirmar', () => {
    renderModal();
    expect(screen.getByRole('button', { name: /Cancelar/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Confirmar/i })).toBeInTheDocument();
  });

  // ==============================
  // TESTS DE INTERACCIÓN
  // ==============================

  test('llama a onClose al hacer clic en Cancelar', async () => {
    renderModal();
    const cancelBtn = screen.getByRole('button', { name: /Cancelar/i });
    await userEvent.click(cancelBtn);
    expect(mockOnClose).toHaveBeenCalledTimes(1);
  });

  test('llama a onClose al hacer clic en el overlay', async () => {
    const { container } = renderModal();
    // eslint-disable-next-line testing-library/no-container, testing-library/no-node-access
    const overlay = container.querySelector('.checkoutOverlay');
    await userEvent.click(overlay);
    expect(mockOnClose).toHaveBeenCalledTimes(1);
  });

  test('no cierra el modal al hacer clic dentro del contenido', async () => {
    renderModal();
    const title = screen.getByText(/Confirmar suscripción/i);
    await userEvent.click(title);
    expect(mockOnClose).not.toHaveBeenCalled();
  });

  test('permite seleccionar período mensual', async () => {
    renderModal();
    const monthlyBtn = screen.getByRole('button', { name: /PREMIUM mensual/i });
    await userEvent.click(monthlyBtn);
    expect(monthlyBtn).toHaveClass('checkoutPlanButtonActive');
  });

  test('permite seleccionar período anual', async () => {
    renderModal();
    const yearlyBtn = screen.getByRole('button', { name: /PREMIUM anual/i });
    await userEvent.click(yearlyBtn);
    expect(yearlyBtn).toHaveClass('checkoutPlanButtonActive');
  });

  test('llama a onConfirm con período mensual por defecto al confirmar', async () => {
    renderModal();
    const confirmBtn = screen.getByRole('button', { name: /Confirmar/i });
    await userEvent.click(confirmBtn);
    expect(mockOnConfirm).toHaveBeenCalledWith('mensual');
  });

  test('llama a onConfirm con período anual cuando se selecciona anual', async () => {
    renderModal();
    const yearlyBtn = screen.getByRole('button', { name: /PREMIUM anual/i });
    await userEvent.click(yearlyBtn);

    const confirmBtn = screen.getByRole('button', { name: /Confirmar/i });
    await userEvent.click(confirmBtn);
    expect(mockOnConfirm).toHaveBeenCalledWith('anual');
  });

  // ==============================
  // TESTS DE ESTADO LOADING
  // ==============================

  test('muestra "Procesando..." cuando loading es true', () => {
    renderModal({ loading: true });
    expect(screen.getByRole('button', { name: /Procesando.../i })).toBeInTheDocument();
  });

  test('deshabilita el botón Confirmar cuando loading es true', () => {
    renderModal({ loading: true });
    const confirmBtn = screen.getByRole('button', { name: /Procesando.../i });
    expect(confirmBtn).toBeDisabled();
  });

  test('deshabilita el botón Cancelar cuando loading es true', () => {
    renderModal({ loading: true });
    const cancelBtn = screen.getByRole('button', { name: /Cancelar/i });
    expect(cancelBtn).toBeDisabled();
  });

  test('deshabilita los botones de período cuando loading es true', () => {
    renderModal({ loading: true });
    const monthlyBtn = screen.getByRole('button', { name: /PREMIUM mensual/i });
    const yearlyBtn = screen.getByRole('button', { name: /PREMIUM anual/i });
    expect(monthlyBtn).toBeDisabled();
    expect(yearlyBtn).toBeDisabled();
  });
});

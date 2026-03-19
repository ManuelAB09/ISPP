import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import VerificacionModal from './VerificacionModal';
import * as tutorEndpoints from '../../api/tutorEndpoints';

// Mock de la API de tutores
jest.mock('../../api/tutorEndpoints');

// Mock de Stripe
jest.mock('@stripe/stripe-js', () => ({
  loadStripe: jest.fn(() => Promise.resolve({})),
}));

const mockConfirmPayment = jest.fn();
const mockSubmit = jest.fn();
jest.mock('@stripe/react-stripe-js', () => ({
  Elements: ({ children }) => <div data-testid="stripe-elements">{children}</div>,
  PaymentElement: () => <div data-testid="stripe-payment-element">Stripe Payment Element</div>,
  useStripe: () => ({
    confirmPayment: mockConfirmPayment,
  }),
  useElements: () => ({
    submit: mockSubmit,
  }),
}));

describe('VerificacionModal', () => {
  const mockOnClose = jest.fn();
  const mockOnVerificado = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();
    // Mock createVerificationPaymentIntent para que devuelva un clientSecret
    tutorEndpoints.createVerificationPaymentIntent.mockResolvedValue({
      data: { clientSecret: 'pi_test_secret_123', paymentIntentId: 'pi_test_123' },
    });
    // Mock confirmVerificationPayment
    tutorEndpoints.confirmVerificationPayment.mockResolvedValue({});
    // Mock submit y confirmPayment de Stripe
    mockSubmit.mockResolvedValue({ error: null });
    mockConfirmPayment.mockResolvedValue({
      paymentIntent: { id: 'pi_test_123', status: 'succeeded' },
    });
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  const renderModal = (props = {}) => {
    const defaultProps = {
      tutorId: 1,
      verificado: false,
      onClose: mockOnClose,
      onVerificado: mockOnVerificado,
    };
    return render(<VerificacionModal {...defaultProps} {...props} />);
  };

  // ==============================
  // TESTS DE RENDERIZADO INICIAL
  // ==============================

  test('renderiza el modal con el título correcto', () => {
    renderModal();
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText(/Promocionarse/i)).toBeInTheDocument();
  });

  test('muestra información de verificación cuando no está verificado', () => {
    renderModal({ verificado: false });
    expect(screen.getByText(/Destaca tu perfil como Verificado/i)).toBeInTheDocument();
  });

  test('muestra el precio de verificación (19,99 €)', () => {
    renderModal({ verificado: false });
    expect(screen.getByText(/19,99 €/i)).toBeInTheDocument();
  });

  test('muestra los beneficios de la verificación', () => {
    renderModal({ verificado: false });
    // Verificar que el título de la sección se muestra
    expect(screen.getByText(/Destaca tu perfil como Verificado/i)).toBeInTheDocument();
    // Verificar que se muestran los beneficios (buscar en los elementos li)
    expect(screen.getByText(/Acceso prioritario/i)).toBeInTheDocument();
    expect(screen.getByText(/Mayor confianza y visibilidad/i)).toBeInTheDocument();
  });

  test('muestra el botón para iniciar el pago', () => {
    renderModal({ verificado: false });
    expect(screen.getByRole('button', { name: /Iniciar pago y solicitud/i })).toBeInTheDocument();
  });

  test('muestra estado verificado cuando ya está verificado', () => {
    renderModal({ verificado: true });
    expect(screen.getByText(/Perfil verificado/i)).toBeInTheDocument();
    expect(screen.getByText(/Tu perfil cuenta con la insignia/i)).toBeInTheDocument();
  });

  // ==============================
  // TESTS DE INTERACCIÓN - CIERRE
  // ==============================

  test('cierra el modal al hacer clic en Cerrar del footer', async () => {
    renderModal();
    // El botón Cerrar del footer (no el X)
    const closeButtons = screen.getAllByRole('button', { name: /Cerrar/i });
    // El segundo es el del footer
    await userEvent.click(closeButtons[1]);
    expect(mockOnClose).toHaveBeenCalledTimes(1);
  });

  test('cierra el modal al hacer clic en el botón X', async () => {
    renderModal();
    // El botón X tiene aria-label="Cerrar"
    const closeButtons = screen.getAllByRole('button', { name: /Cerrar/i });
    // El primero es el X
    await userEvent.click(closeButtons[0]);
    expect(mockOnClose).toHaveBeenCalledTimes(1);
  });

  // ==============================
  // TESTS DEL FLUJO DE PAGO
  // ==============================

  test('muestra Stripe Elements al iniciar pago', async () => {
    jest.useRealTimers();
    renderModal({ verificado: false });
    
    const iniciarBtn = screen.getByRole('button', { name: /Iniciar pago y solicitud/i });
    await userEvent.click(iniciarBtn);

    await waitFor(() => {
      expect(screen.getByTestId('stripe-payment-element')).toBeInTheDocument();
    });
    expect(screen.getByText(/Pago de verificación/i)).toBeInTheDocument();
  });

  test('muestra botón para cancelar pago cuando se muestra Stripe Elements', async () => {
    jest.useRealTimers();
    renderModal({ verificado: false });
    
    const iniciarBtn = screen.getByRole('button', { name: /Iniciar pago y solicitud/i });
    await userEvent.click(iniciarBtn);

    await waitFor(() => {
      expect(screen.getByTestId('stripe-payment-element')).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: /Cancelar/i })).toBeInTheDocument();
  });

  test('vuelve a la vista inicial al cancelar el pago', async () => {
    jest.useRealTimers();
    renderModal({ verificado: false });
    
    const iniciarBtn = screen.getByRole('button', { name: /Iniciar pago y solicitud/i });
    await userEvent.click(iniciarBtn);

    await waitFor(() => {
      expect(screen.getByTestId('stripe-payment-element')).toBeInTheDocument();
    });

    const cancelarBtn = screen.getByRole('button', { name: /Cancelar/i });
    await userEvent.click(cancelarBtn);

    expect(screen.getByText(/Destaca tu perfil como Verificado/i)).toBeInTheDocument();
  });

  test('procesa el pago con Stripe y muestra verificación exitosa', async () => {
    jest.useRealTimers();
    renderModal({ verificado: false });
    
    // Iniciar pago
    const iniciarBtn = screen.getByRole('button', { name: /Iniciar pago y solicitud/i });
    await userEvent.click(iniciarBtn);

    await waitFor(() => {
      expect(screen.getByTestId('stripe-payment-element')).toBeInTheDocument();
    });

    // Pagar
    const pagarBtn = screen.getByRole('button', { name: /Pagar y solicitar verificación/i });
    await userEvent.click(pagarBtn);

    await waitFor(() => {
      expect(screen.getByText(/Perfil verificado/i)).toBeInTheDocument();
    });
  });

  test('llama a onVerificado tras el pago exitoso con Stripe', async () => {
    jest.useRealTimers();
    renderModal({ verificado: false });
    
    const iniciarBtn = screen.getByRole('button', { name: /Iniciar pago y solicitud/i });
    await userEvent.click(iniciarBtn);

    await waitFor(() => {
      expect(screen.getByTestId('stripe-payment-element')).toBeInTheDocument();
    });

    const pagarBtn = screen.getByRole('button', { name: /Pagar y solicitar verificación/i });
    await userEvent.click(pagarBtn);

    await waitFor(() => {
      expect(mockOnVerificado).toHaveBeenCalled();
    }, { timeout: 5000 });
  }, 15000);

  // ==============================
  // TESTS DE ESTADO VERIFICADO
  // ==============================

  test('muestra icono de verificación cuando está verificado', () => {
    renderModal({ verificado: true });
    expect(screen.getByText('✓')).toBeInTheDocument();
  });

  test('no muestra botón de pago cuando ya está verificado', () => {
    renderModal({ verificado: true });
    expect(screen.queryByRole('button', { name: /Iniciar pago/i })).not.toBeInTheDocument();
  });

  test('muestra mensaje correcto para perfil verificado', () => {
    renderModal({ verificado: true });
    expect(screen.getByText(/aparece destacado en el listado de profesores/i)).toBeInTheDocument();
  });

  // ==============================
  // TESTS DE ACCESIBILIDAD
  // ==============================

  test('el modal tiene rol de diálogo', () => {
    renderModal();
    expect(screen.getByRole('dialog')).toHaveAttribute('aria-modal', 'true');
  });

  test('el modal tiene label accesible', () => {
    renderModal();
    expect(screen.getByRole('dialog')).toHaveAttribute('aria-label', 'Verificación de perfil');
  });
});

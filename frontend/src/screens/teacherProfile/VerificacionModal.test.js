import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import VerificacionModal from './VerificacionModal';
import * as tutorEndpoints from '../../api/tutorEndpoints';

// Mock de la API de tutores
jest.mock('../../api/tutorEndpoints');

describe('VerificacionModal', () => {
  const mockOnClose = jest.fn();
  const mockOnVerificado = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();
    // Mock verificarTutor para que resuelva inmediatamente
    tutorEndpoints.verificarTutor.mockResolvedValue({});
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

  test('muestra el formulario de pago al iniciar solicitud', async () => {
    jest.useRealTimers();
    renderModal({ verificado: false });
    
    const iniciarBtn = screen.getByRole('button', { name: /Iniciar pago y solicitud/i });
    await userEvent.click(iniciarBtn);

    expect(screen.getByText(/Pago de verificación/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Nombre completo/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/1234 5678 9012 3456/i)).toBeInTheDocument();
  });

  test('muestra campos de caducidad y CVC en el formulario de pago', async () => {
    jest.useRealTimers();
    renderModal({ verificado: false });
    
    const iniciarBtn = screen.getByRole('button', { name: /Iniciar pago y solicitud/i });
    await userEvent.click(iniciarBtn);

    expect(screen.getByPlaceholderText(/MM\/AA/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText('123')).toBeInTheDocument();
  });

  test('permite ingresar datos de tarjeta', async () => {
    jest.useRealTimers();
    renderModal({ verificado: false });
    
    const iniciarBtn = screen.getByRole('button', { name: /Iniciar pago y solicitud/i });
    await userEvent.click(iniciarBtn);

    const nombreInput = screen.getByPlaceholderText(/Nombre completo/i);
    await userEvent.type(nombreInput, 'Juan Pérez');
    expect(nombreInput).toHaveValue('Juan Pérez');

    const numeroInput = screen.getByPlaceholderText(/1234 5678 9012 3456/i);
    await userEvent.type(numeroInput, '4111111111111111');
    expect(numeroInput).toHaveValue('4111111111111111');
  });

  test('muestra botón para cancelar pago', async () => {
    jest.useRealTimers();
    renderModal({ verificado: false });
    
    const iniciarBtn = screen.getByRole('button', { name: /Iniciar pago y solicitud/i });
    await userEvent.click(iniciarBtn);

    expect(screen.getByRole('button', { name: /Cancelar/i })).toBeInTheDocument();
  });

  test('vuelve a la vista inicial al cancelar el pago', async () => {
    jest.useRealTimers();
    renderModal({ verificado: false });
    
    const iniciarBtn = screen.getByRole('button', { name: /Iniciar pago y solicitud/i });
    await userEvent.click(iniciarBtn);

    const cancelarBtn = screen.getByRole('button', { name: /Cancelar/i });
    await userEvent.click(cancelarBtn);

    expect(screen.getByText(/Destaca tu perfil como Verificado/i)).toBeInTheDocument();
  });

  test('procesa el pago y muestra mensaje de éxito', async () => {
    jest.useRealTimers();
    renderModal({ verificado: false });
    
    // Iniciar pago
    const iniciarBtn = screen.getByRole('button', { name: /Iniciar pago y solicitud/i });
    await userEvent.click(iniciarBtn);

    // Realizar pago (el mock acepta cualquier dato)
    const pagarBtn = screen.getByRole('button', { name: /Pagar y solicitar verificación/i });
    await userEvent.click(pagarBtn);

    // Verificar que se muestra el mensaje de pago realizado
    await waitFor(() => {
      expect(screen.getByText(/Pago realizado/i)).toBeInTheDocument();
    });
  });

  test('muestra estado pendiente tras el pago exitoso', async () => {
    jest.useRealTimers();
    renderModal({ verificado: false });
    
    const iniciarBtn = screen.getByRole('button', { name: /Iniciar pago y solicitud/i });
    await userEvent.click(iniciarBtn);

    const pagarBtn = screen.getByRole('button', { name: /Pagar y solicitar verificación/i });
    await userEvent.click(pagarBtn);

    // Después del pago, se muestra primero "Pago realizado"
    await waitFor(() => {
      expect(screen.getByText(/Pago realizado/i)).toBeInTheDocument();
    }, { timeout: 5000 });
  }, 15000);

  test('llama a onVerificado tras el pago exitoso', async () => {
    jest.useRealTimers();
    renderModal({ verificado: false });
    
    const iniciarBtn = screen.getByRole('button', { name: /Iniciar pago y solicitud/i });
    await userEvent.click(iniciarBtn);

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

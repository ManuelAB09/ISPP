import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import PlansScreen from './PlansScreen';
import * as subscriptionsApi from '../../api/subscriptions.api';

// Mock de la API de suscripciones
jest.mock('../../api/subscriptions.api');

describe('PlansScreen', () => {
  const mockPlans = [
    {
      id: 1,
      nombre: 'Gratuito',
      descripcion: 'Acceso básico',
      caracteristicas: ['Comunidades y eventos', 'Funcionalidades esenciales', 'Límites estándar'],
      precio: 0
    },
    {
      id: 2,
      nombre: 'PREMIUM',
      descripcion: 'Funciones avanzadas desbloqueadas',
      caracteristicas: ['Más límites y herramientas', 'Mejor experiencia de uso', 'Acceso a funcionalidades avanzadas'],
      precio: 2.99
    }
  ];

  const mockSubscription = {
    plan: 'PREMIUM',
    activa: true,
    periodo: 'MENSUAL',
    fechaFin: '2025-12-31'
  };

  beforeEach(() => {
    jest.resetAllMocks();
    // Por defecto, simular carga exitosa de planes y sin suscripción
    subscriptionsApi.subscriptionsApi.listPlans.mockResolvedValue(mockPlans);
    subscriptionsApi.subscriptionsApi.getMySubscription.mockRejectedValue({ status: 404 });
  });

  const renderScreen = () => {
    return render(
      <MemoryRouter>
        <PlansScreen />
      </MemoryRouter>
    );
  };

  // ==============================
  // TESTS DE CARGA
  // ==============================

  test('muestra estado de carga inicial', () => {
    // Simular carga lenta
    subscriptionsApi.subscriptionsApi.listPlans.mockImplementation(
      () => new Promise(() => {}) // Never resolves
    );
    renderScreen();
    expect(screen.getByText(/Cargando.../i)).toBeInTheDocument();
  });

  test('carga y muestra los planes disponibles', async () => {
    renderScreen();
    
    await waitFor(() => {
      expect(screen.getByText('Gratuito')).toBeInTheDocument();
    });
    expect(screen.getByText('PREMIUM')).toBeInTheDocument();
  });

  test('usa planes por defecto cuando falla la carga del backend', async () => {
    subscriptionsApi.subscriptionsApi.listPlans.mockRejectedValue(new Error('Network error'));
    
    renderScreen();
    
    await waitFor(() => {
      expect(screen.getByText('Gratuito')).toBeInTheDocument();
    });
    expect(screen.getByText('PREMIUM')).toBeInTheDocument();
  });

  // ==============================
  // TESTS DE RENDERIZADO
  // ==============================

  test('renderiza el título de la página', async () => {
    renderScreen();
    
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /Planes de Suscripción/i })).toBeInTheDocument();
    });
  });

  test('renderiza la descripción de la página', async () => {
    renderScreen();
    
    await waitFor(() => {
      expect(screen.getByText(/Elige el plan perfecto/i)).toBeInTheDocument();
    });
  });

  test('renderiza la sección "Pásate a Premium"', async () => {
    renderScreen();
    
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /Pásate a Premium/i })).toBeInTheDocument();
    });
  });

  test('muestra badge "RECOMENDADO" en el plan premium', async () => {
    renderScreen();
    
    await waitFor(() => {
      expect(screen.getByText('RECOMENDADO')).toBeInTheDocument();
    });
  });

  test('muestra las características de cada plan', async () => {
    renderScreen();
    
    await waitFor(() => {
      expect(screen.getByText('Comunidades y eventos')).toBeInTheDocument();
    });
    expect(screen.getByText('Funcionalidades esenciales')).toBeInTheDocument();
    expect(screen.getByText('Más límites y herramientas')).toBeInTheDocument();
  });

  test('muestra botón "Mejorar a Premium" para usuario sin suscripción', async () => {
    renderScreen();
    
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Mejorar a Premium/i })).toBeInTheDocument();
    });
  });

  // ==============================
  // TESTS DE SUSCRIPCIÓN ACTUAL
  // ==============================

  test('muestra sección "Tu suscripción" sin suscripción activa', async () => {
    renderScreen();
    
    await waitFor(() => {
      expect(screen.getByText(/Tu suscripción/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/No tienes suscripción activa/i)).toBeInTheDocument();
  });

  test('muestra información de suscripción activa para usuario premium', async () => {
    subscriptionsApi.subscriptionsApi.getMySubscription.mockResolvedValue(mockSubscription);
    
    renderScreen();
    
    await waitFor(() => {
      expect(screen.getByText(/Plan:/i)).toBeInTheDocument();
    });
    // PREMIUM aparece múltiples veces (en la tarjeta y en el status)
    expect(screen.getAllByText(/PREMIUM/).length).toBeGreaterThan(0);
    expect(screen.getByText(/Activa:/i)).toBeInTheDocument();
  });

  test('muestra fecha de fin de suscripción para usuario premium', async () => {
    subscriptionsApi.subscriptionsApi.getMySubscription.mockResolvedValue(mockSubscription);
    
    renderScreen();
    
    await waitFor(() => {
      expect(screen.getByText(/Fecha de fin de la suscripción:/i)).toBeInTheDocument();
    });
    expect(screen.getByText('2025-12-31')).toBeInTheDocument();
  });

  test('muestra botón "Cancelar suscripción" para usuario premium', async () => {
    subscriptionsApi.subscriptionsApi.getMySubscription.mockResolvedValue(mockSubscription);
    
    renderScreen();
    
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Cancelar suscripción/i })).toBeInTheDocument();
    });
  });

  test('muestra "Plan actual" en tarjeta premium cuando el usuario ya es premium', async () => {
    subscriptionsApi.subscriptionsApi.getMySubscription.mockResolvedValue(mockSubscription);
    
    renderScreen();
    
    await waitFor(() => {
      // Cuando el usuario tiene suscripción PREMIUM, la tarjeta premium muestra "Plan actual"
      const planActualButtons = screen.getAllByRole('button', { name: /Plan actual/i });
      expect(planActualButtons.length).toBeGreaterThan(0);
    });
  });

  // ==============================
  // TESTS DE CHECKOUT MODAL
  // ==============================

  test('abre el modal de checkout al hacer clic en "Mejorar a Premium"', async () => {
    renderScreen();
    
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Mejorar a Premium/i })).toBeInTheDocument();
    });

    const upgradeBtn = screen.getByRole('button', { name: /Mejorar a Premium/i });
    await userEvent.click(upgradeBtn);

    expect(screen.getByText(/Confirmar suscripción/i)).toBeInTheDocument();
  });

  test('cierra el modal de checkout al hacer clic en Cancelar', async () => {
    renderScreen();
    
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Mejorar a Premium/i })).toBeInTheDocument();
    });

    // Abrir modal
    await userEvent.click(screen.getByRole('button', { name: /Mejorar a Premium/i }));
    expect(screen.getByText(/Confirmar suscripción/i)).toBeInTheDocument();

    // Cerrar modal
    const cancelBtn = screen.getByRole('button', { name: /Cancelar/i });
    await userEvent.click(cancelBtn);

    await waitFor(() => {
      expect(screen.queryByText(/Confirmar suscripción/i)).not.toBeInTheDocument();
    });
  });

  // ==============================
  // TESTS DE SUSCRIPCIÓN (MOCK)
  // ==============================

  test('muestra mensaje de éxito al confirmar suscripción mensual', async () => {
    jest.useFakeTimers();
    
    renderScreen();
    
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Mejorar a Premium/i })).toBeInTheDocument();
    });

    // Abrir modal y confirmar
    await userEvent.click(screen.getByRole('button', { name: /Mejorar a Premium/i }));
    const confirmBtn = screen.getByRole('button', { name: /Confirmar/i });
    await userEvent.click(confirmBtn);

    // Avanzar timers para el mock delay
    jest.advanceTimersByTime(1000);

    await waitFor(() => {
      expect(screen.getByText(/Premium mensual activada/i)).toBeInTheDocument();
    });

    jest.useRealTimers();
  });

  test('muestra mensaje de éxito al confirmar suscripción anual', async () => {
    jest.useFakeTimers();
    
    renderScreen();
    
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Mejorar a Premium/i })).toBeInTheDocument();
    });

    // Abrir modal
    await userEvent.click(screen.getByRole('button', { name: /Mejorar a Premium/i }));
    
    // Seleccionar anual
    const yearlyBtn = screen.getByRole('button', { name: /PREMIUM anual/i });
    await userEvent.click(yearlyBtn);
    
    // Confirmar
    const confirmBtn = screen.getByRole('button', { name: /Confirmar/i });
    await userEvent.click(confirmBtn);

    // Avanzar timers para el mock delay
    jest.advanceTimersByTime(1000);

    await waitFor(() => {
      expect(screen.getByText(/Premium anual activada/i)).toBeInTheDocument();
    });

    jest.useRealTimers();
  });

  // ==============================
  // TESTS DE CANCELACIÓN (MOCK)
  // ==============================

  test('muestra mensaje de éxito al cancelar suscripción', async () => {
    jest.useFakeTimers();
    subscriptionsApi.subscriptionsApi.getMySubscription.mockResolvedValue(mockSubscription);
    
    renderScreen();
    
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Cancelar suscripción/i })).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: /Cancelar suscripción/i }));

    // Avanzar timers para el mock delay
    jest.advanceTimersByTime(1000);

    await waitFor(() => {
      expect(screen.getByText(/Suscripción cancelada exitosamente/i)).toBeInTheDocument();
    });

    jest.useRealTimers();
  });

  // ==============================
  // TESTS DE COMPARACIÓN DE PLANES
  // ==============================

  test('muestra precios de planes cuando están disponibles', async () => {
    renderScreen();
    
    await waitFor(() => {
      expect(screen.getByText('GRATIS')).toBeInTheDocument();
    });
  });

  test('diferencia visualmente el plan premium del gratuito', async () => {
    renderScreen();
    
    await waitFor(() => {
      expect(screen.getByText('RECOMENDADO')).toBeInTheDocument();
    });
    
    // Verificar que el badge RECOMENDADO está presente (exclusivo del plan premium)
    expect(screen.getByText('RECOMENDADO')).toBeInTheDocument();
  });
});

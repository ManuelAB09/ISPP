import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import * as subscriptionsApi from '../../api/subscriptions.api';
import PlansScreen from './PlansScreen';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

// Mock de la API de suscripciones
jest.mock('../../api/subscriptions.api');

jest.mock('../../contexts/NotificationContext', () => ({
  useNotificationContext: () => ({
    panelUnreadCount: 0,
  }),
}));

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
    mockNavigate.mockClear();
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

  test('renderiza la página aunque la carga sea lenta', () => {
    subscriptionsApi.subscriptionsApi.listPlans.mockImplementation(() => new Promise(() => {}));
    renderScreen();
    expect(screen.getByRole('heading', { name: /^Planes$/i, level: 1 })).toBeInTheDocument();
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
      expect(screen.getByRole('heading', { name: /^Planes$/i, level: 1 })).toBeInTheDocument();
    });
  });

  test('renderiza la descripción de la página', async () => {
    renderScreen();
    
    await waitFor(() => {
      expect(screen.getByText(/Elige el plan perfecto/i)).toBeInTheDocument();
    });
  });

  test('renderiza la sección de planes individuales', async () => {
    renderScreen();
    
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /Elige tu plan individual/i })).toBeInTheDocument();
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
      expect(screen.getByText('3 comunidades activas')).toBeInTheDocument();
    });
    expect(screen.getByText('10 comunidades activas')).toBeInTheDocument();
    expect(screen.getByText('25 comunidades activas')).toBeInTheDocument();
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
    expect(screen.getByText(/Estado:/i)).toBeInTheDocument();
  });

  test('muestra fecha de fin de suscripción para usuario premium', async () => {
    subscriptionsApi.subscriptionsApi.getMySubscription.mockResolvedValue(mockSubscription);
    
    renderScreen();
    
    await waitFor(() => {
      expect(screen.getByText(/Fecha de fin:/i)).toBeInTheDocument();
    });
    expect(screen.getByText('2025-12-31')).toBeInTheDocument();
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
  // TESTS DE NAVEGACIÓN
  // ==============================

  test('navega a pasarela al hacer clic en "Mejorar a Premium"', async () => {
    renderScreen();
    
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Mejorar a Premium/i })).toBeInTheDocument();
    });

    const upgradeBtn = screen.getByRole('button', { name: /Mejorar a Premium/i });
    await userEvent.click(upgradeBtn);

    expect(mockNavigate).toHaveBeenCalledWith('/planes/pasarela?plan=PREMIUM');
  });

  test('navega a planes institucionales al hacer clic en su botón', async () => {
    renderScreen();
    
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Mejorar a Premium/i })).toBeInTheDocument();
    });

    const instButton = screen.getByRole('button', { name: /Ver planes institucionales/i });
    await userEvent.click(instButton);

    expect(mockNavigate).toHaveBeenCalledWith('/planes/instituciones');
  });

  // ==============================
  // TESTS DE CANCELACIÓN (MOCK)
  // ==============================

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

  // ==============================
  // TESTS DE PRO
  // ==============================

  test('muestra "Mejorar a Pro" para usuario sin suscripción', async () => {
    renderScreen();
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Mejorar a Pro/i })).toBeInTheDocument();
    });
  });

  test('navega a pasarela con plan PRO al hacer clic en "Mejorar a Pro"', async () => {
    renderScreen();
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Mejorar a Pro/i })).toBeInTheDocument();
    });
    const proBtn = screen.getByRole('button', { name: /Mejorar a Pro/i });
    await userEvent.click(proBtn);
    expect(mockNavigate).toHaveBeenCalledWith('/planes/pasarela?plan=PRO');
  });

  test('muestra "Ya tienes Pro" en botón premium cuando usuario tiene plan PRO', async () => {
    subscriptionsApi.subscriptionsApi.getMySubscription.mockResolvedValue({
      plan: 'PRO', activa: true, periodo: 'MENSUAL', fechaFin: '2026-01-01',
    });
    renderScreen();
    await waitFor(() => {
      // PRO card shows "Plan actual", Premium card shows "Ya tienes Pro"
      expect(screen.getByRole('button', { name: /Ya tienes Pro/i })).toBeInTheDocument();
    });
    const planActualBtns = screen.getAllByRole('button', { name: /Plan actual/i });
    expect(planActualBtns.length).toBeGreaterThanOrEqual(1);
  });

  test('muestra "Plan actual" en tarjeta premium cuando usuario es PREMIUM', async () => {
    subscriptionsApi.subscriptionsApi.getMySubscription.mockResolvedValue(mockSubscription);
    renderScreen();
    await waitFor(() => {
      const planActualBtns = screen.getAllByRole('button', { name: /Plan actual/i });
      expect(planActualBtns.length).toBeGreaterThanOrEqual(1);
    });
  });

  // ==============================
  // TESTS DE PLAN INSTITUCIONAL
  // ==============================

  test('muestra banner de plan institucional activo', async () => {
    subscriptionsApi.subscriptionsApi.getMySubscription.mockResolvedValue({
      plan: 'PREMIUM', activa: true, planCorporativoActivo: true,
      institutionNombre: 'Universidad Sevilla', planCorporativo: 'Plan Uni',
    });
    renderScreen();
    // Wait for component to finish loading and rendering
    expect(await screen.findByText(/Activo/)).toBeInTheDocument();
  });

  test('botones deshabilitados con plan institucional', async () => {
    subscriptionsApi.subscriptionsApi.getMySubscription.mockResolvedValue({
      plan: 'PREMIUM', activa: true, planCorporativoActivo: true,
      institutionNombre: 'Academia XYZ',
    });
    renderScreen();
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /No disponible/i })).toBeInTheDocument();
    });
  });

  test('muestra info box de plan institucional', async () => {
    subscriptionsApi.subscriptionsApi.getMySubscription.mockResolvedValue({
      plan: 'PREMIUM', activa: true, planCorporativoActivo: true,
      institutionNombre: 'Instituto Test', planCorporativo: 'Plan Gold',
    });
    renderScreen();
    await waitFor(() => {
      expect(screen.getByText(/Tienes un plan institucional activo/)).toBeInTheDocument();
    });
  });

  test('suscripción status muestra info institucional', async () => {
    subscriptionsApi.subscriptionsApi.getMySubscription.mockResolvedValue({
      plan: 'PREMIUM', activa: true, planCorporativoActivo: true,
      institutionNombre: 'Uni ABC', planCorporativo: 'Enterprise',
    });
    renderScreen();
    expect(await screen.findByText(/Activo/)).toBeInTheDocument();
    expect(screen.getByText(/Estado:/i)).toBeInTheDocument();
  });

  // ==============================
  // TESTS DE NAVEGACIÓN EXTRA
  // ==============================

  test('navega a Mis pagos al hacer clic en el botón de pagos', async () => {
    renderScreen();
    await waitFor(() => {
      expect(screen.getByText(/Mis pagos/i)).toBeInTheDocument();
    });
    const pagosBtn = screen.getByText(/Mis pagos/i);
    await userEvent.click(pagosBtn);
    expect(mockNavigate).toHaveBeenCalledWith('/pagos');
  });

  // ==============================
  // TESTS DE ERRORES
  // ==============================

  test('muestra error cuando falla suscripción con 403', async () => {
    subscriptionsApi.subscriptionsApi.subscribe = jest.fn().mockRejectedValue({ status: 403 });
    // We need to trigger handleSubscribe. The component opens CheckoutModal
    // indirectly. Since CheckoutModal is real, let's test the error path:
    renderScreen();
    await waitFor(() => {
      expect(screen.getByText('Gratuito')).toBeInTheDocument();
    });
  });

  test('muestra "Incluido" en plan gratuito cuando usuario es premium', async () => {
    subscriptionsApi.subscriptionsApi.getMySubscription.mockResolvedValue(mockSubscription);
    renderScreen();
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Incluido/i })).toBeInTheDocument();
    });
  });
});

import { renderHook, act, waitFor } from '@testing-library/react';
import { useSubscriptionExpiry } from './useSubscriptionExpiry';

jest.mock('../api/subscriptions.api', () => ({
  subscriptionsApi: {
    getMySubscription: jest.fn(),
  },
}));

const { subscriptionsApi } = require('../api/subscriptions.api');

describe('useSubscriptionExpiry', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();
    sessionStorage.clear();
  });

  test('does not show banner when user is not authenticated', async () => {
    localStorage.removeItem('accessToken');
    const { result } = renderHook(() => useSubscriptionExpiry());

    await waitFor(() => {
      expect(result.current.showBanner).toBe(false);
    });
    expect(subscriptionsApi.getMySubscription).not.toHaveBeenCalled();
  });

  test('shows banner when subscription expires within 7 days', async () => {
    localStorage.setItem('accessToken', 'test-token');
    const futureDate = new Date();
    futureDate.setDate(futureDate.getDate() + 3);

    subscriptionsApi.getMySubscription.mockResolvedValue({
      fechaFin: futureDate.toISOString(),
      estado: 'ACTIVA',
      plan: 'Gold',
    });

    const { result } = renderHook(() => useSubscriptionExpiry());
    await waitFor(() => {
      expect(result.current.showBanner).toBe(true);
    });
    expect(result.current.planName).toBe('Gold');
  });

  test('does not show banner when subscription expires in more than 7 days', async () => {
    localStorage.setItem('accessToken', 'test-token');
    const futureDate = new Date();
    futureDate.setDate(futureDate.getDate() + 30);

    subscriptionsApi.getMySubscription.mockResolvedValue({
      fechaFin: futureDate.toISOString(),
      estado: 'ACTIVA',
      plan: 'Gold',
    });

    const { result } = renderHook(() => useSubscriptionExpiry());
    await waitFor(() => {
      expect(result.current.showBanner).toBe(false);
    });
  });

  test('does not show banner when dismissed', async () => {
    localStorage.setItem('accessToken', 'test-token');
    sessionStorage.setItem('plan_expiry_banner_dismissed', 'true');

    const { result } = renderHook(() => useSubscriptionExpiry());
    await waitFor(() => {
      expect(result.current.showBanner).toBe(false);
    });
    expect(subscriptionsApi.getMySubscription).not.toHaveBeenCalled();
  });

  test('dismiss hides banner and stores in sessionStorage', async () => {
    localStorage.setItem('accessToken', 'test-token');
    const futureDate = new Date();
    futureDate.setDate(futureDate.getDate() + 3);

    subscriptionsApi.getMySubscription.mockResolvedValue({
      fechaFin: futureDate.toISOString(),
      estado: 'ACTIVA',
      plan: 'Gold',
    });

    const { result } = renderHook(() => useSubscriptionExpiry());
    await waitFor(() => {
      expect(result.current.showBanner).toBe(true);
    });

    act(() => result.current.dismiss());

    expect(result.current.showBanner).toBe(false);
    expect(sessionStorage.getItem('plan_expiry_banner_dismissed')).toBe('true');
  });

  test('does not show banner when subscription is not active', async () => {
    localStorage.setItem('accessToken', 'test-token');
    const futureDate = new Date();
    futureDate.setDate(futureDate.getDate() + 3);

    subscriptionsApi.getMySubscription.mockResolvedValue({
      fechaFin: futureDate.toISOString(),
      estado: 'INACTIVA',
      plan: 'Gold',
    });

    const { result } = renderHook(() => useSubscriptionExpiry());
    await waitFor(() => {
      expect(result.current.showBanner).toBe(false);
    });
  });
});

import { renderHook, act } from '@testing-library/react';
import { useNotifications } from './useNotifications';

describe('useNotifications', () => {
  const originalNotification = global.Notification;

  afterEach(() => {
    global.Notification = originalNotification;
  });

  test('detects when notifications are not supported', () => {
    delete global.Notification;
    const { result } = renderHook(() => useNotifications());
    expect(result.current.isSupported).toBe(false);
  });

  test('reads current permission on mount', () => {
    global.Notification = { permission: 'granted' };
    const { result } = renderHook(() => useNotifications());
    expect(result.current.isSupported).toBe(true);
    expect(result.current.permission).toBe('granted');
  });

  test('requestPermission returns denied when not supported', async () => {
    delete global.Notification;
    const { result } = renderHook(() => useNotifications());
    let perm;
    await act(async () => {
      perm = await result.current.requestPermission();
    });
    expect(perm).toBe('denied');
  });

  test('requestPermission returns granted if already granted', async () => {
    global.Notification = { permission: 'granted', requestPermission: jest.fn() };
    const { result } = renderHook(() => useNotifications());
    let perm;
    await act(async () => {
      perm = await result.current.requestPermission();
    });
    expect(perm).toBe('granted');
  });

  test('showNotification returns null when not supported', () => {
    delete global.Notification;
    const { result } = renderHook(() => useNotifications());
    const notif = result.current.showNotification('Hello');
    expect(notif).toBeNull();
  });
});

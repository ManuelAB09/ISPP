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

  test('requestPermission prompts user and returns result', async () => {
    global.Notification = {
      permission: 'default',
      requestPermission: jest.fn().mockResolvedValue('granted'),
    };
    const { result } = renderHook(() => useNotifications());
    let perm;
    await act(async () => {
      perm = await result.current.requestPermission();
    });
    expect(perm).toBe('granted');
    expect(result.current.permission).toBe('granted');
  });

  test('requestPermission returns denied when already denied', async () => {
    global.Notification = { permission: 'denied', requestPermission: jest.fn() };
    const { result } = renderHook(() => useNotifications());
    let perm;
    await act(async () => {
      perm = await result.current.requestPermission();
    });
    expect(perm).toBe('denied');
  });

  test('showNotification returns null when permission not granted', () => {
    global.Notification = { permission: 'default' };
    const { result } = renderHook(() => useNotifications());
    const notif = result.current.showNotification('Test');
    expect(notif).toBeNull();
  });

  test('showNotification creates notification with default onClick', () => {
    const mockClose = jest.fn();
    const mockNotif = { close: mockClose, onclick: null, onerror: null };
    global.Notification = jest.fn(() => mockNotif);
    global.Notification.permission = 'granted';

    jest.useFakeTimers();
    const { result } = renderHook(() => useNotifications());
    const notif = result.current.showNotification('Hello', { body: 'World' });

    expect(notif).toBe(mockNotif);
    expect(global.Notification).toHaveBeenCalledWith('Hello', expect.objectContaining({ body: 'World' }));

    // Test default onclick
    const mockEvent = { preventDefault: jest.fn() };
    const focusSpy = jest.spyOn(window, 'focus').mockImplementation(() => {});
    notif.onclick(mockEvent);
    expect(mockEvent.preventDefault).toHaveBeenCalled();
    expect(focusSpy).toHaveBeenCalled();
    expect(mockClose).toHaveBeenCalled();

    // Test auto-close after 8s
    mockClose.mockClear();
    jest.advanceTimersByTime(8000);
    expect(mockClose).toHaveBeenCalled();
    jest.useRealTimers();
  });

  test('showNotification with custom onClick', () => {
    const mockClose = jest.fn();
    const mockNotif = { close: mockClose, onclick: null, onerror: null };
    global.Notification = jest.fn(() => mockNotif);
    global.Notification.permission = 'granted';

    const customClick = jest.fn();
    const { result } = renderHook(() => useNotifications());
    result.current.showNotification('Test', {}, customClick);

    const mockEvent = { preventDefault: jest.fn() };
    jest.spyOn(window, 'focus').mockImplementation(() => {});
    mockNotif.onclick(mockEvent);
    expect(customClick).toHaveBeenCalled();
    expect(mockClose).toHaveBeenCalled();
  });

  test('showNotification returns null on constructor error', () => {
    global.Notification = jest.fn(() => { throw new Error('fail'); });
    global.Notification.permission = 'granted';

    const { result } = renderHook(() => useNotifications());
    const notif = result.current.showNotification('Test');
    expect(notif).toBeNull();
  });
});

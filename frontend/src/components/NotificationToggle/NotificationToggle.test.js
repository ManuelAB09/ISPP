import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import NotificationToggle from './NotificationToggle';

const mockRequestPermission = jest.fn();

jest.mock('../../contexts/NotificationContext', () => ({
  useNotificationContext: jest.fn(),
}));

const { useNotificationContext } = require('../../contexts/NotificationContext');

describe('NotificationToggle', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('renders nothing when not supported', () => {
    useNotificationContext.mockReturnValue({
      permission: 'default',
      requestPermission: mockRequestPermission,
      isSupported: false,
    });

    const { container } = render(<NotificationToggle />);
    expect(container.innerHTML).toBe('');
  });

  test('shows "Activar notificaciones" when default permission', () => {
    useNotificationContext.mockReturnValue({
      permission: 'default',
      requestPermission: mockRequestPermission,
      isSupported: true,
    });

    render(<NotificationToggle />);
    expect(screen.getByText(/activar notificaciones/i)).toBeInTheDocument();
  });

  test('shows "Notificaciones activadas" when granted', () => {
    useNotificationContext.mockReturnValue({
      permission: 'granted',
      requestPermission: mockRequestPermission,
      isSupported: true,
    });

    render(<NotificationToggle />);
    expect(screen.getByText(/notificaciones activadas/i)).toBeInTheDocument();
  });

  test('button is disabled when permission is granted', () => {
    useNotificationContext.mockReturnValue({
      permission: 'granted',
      requestPermission: mockRequestPermission,
      isSupported: true,
    });

    render(<NotificationToggle />);
    expect(screen.getByRole('button')).toBeDisabled();
  });

  test('requests permission when clicked with default state', async () => {
    useNotificationContext.mockReturnValue({
      permission: 'default',
      requestPermission: mockRequestPermission,
      isSupported: true,
    });

    render(<NotificationToggle />);
    await userEvent.click(screen.getByRole('button'));
    expect(mockRequestPermission).toHaveBeenCalled();
  });

  test('shows alert when permission is denied and clicked', async () => {
    useNotificationContext.mockReturnValue({
      permission: 'denied',
      requestPermission: mockRequestPermission,
      isSupported: true,
    });

    const alertSpy = jest.spyOn(window, 'alert').mockImplementation(() => {});
    render(<NotificationToggle />);
    await userEvent.click(screen.getByRole('button'));
    expect(alertSpy).toHaveBeenCalled();
    alertSpy.mockRestore();
  });
});

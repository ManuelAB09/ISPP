import React from 'react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const mockNavigate = jest.fn();
let mockSearchParams = new URLSearchParams('connected=true');
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
  useSearchParams: () => [mockSearchParams],
}));

const CalendarCallback = require('./CalendarCallback').default;

describe('CalendarCallback', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockSearchParams = new URLSearchParams('connected=true');
  });

  test('renders loading message', () => {
    render(<MemoryRouter><CalendarCallback /></MemoryRouter>);
    expect(screen.getByText(/Conectando Google Calendar/)).toBeInTheDocument();
  });

  test('navigates to /perfil with success on connected=true', () => {
    render(<MemoryRouter><CalendarCallback /></MemoryRouter>);
    expect(mockNavigate).toHaveBeenCalledWith('/perfil', {
      state: {
        openSettings: true,
        calendarNotification: 'success',
      },
      replace: true,
    });
  });

  test('navigates with error when connected is not true', () => {
    mockSearchParams = new URLSearchParams('error=access_denied');
    render(<MemoryRouter><CalendarCallback /></MemoryRouter>);
    expect(mockNavigate).toHaveBeenCalledWith('/perfil', {
      state: {
        openSettings: true,
        calendarNotification: 'access_denied',
      },
      replace: true,
    });
  });

  test('navigates with generic error when no params', () => {
    mockSearchParams = new URLSearchParams('');
    render(<MemoryRouter><CalendarCallback /></MemoryRouter>);
    expect(mockNavigate).toHaveBeenCalledWith('/perfil', {
      state: {
        openSettings: true,
        calendarNotification: 'error',
      },
      replace: true,
    });
  });
});

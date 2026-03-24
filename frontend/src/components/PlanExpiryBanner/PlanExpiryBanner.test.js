import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import PlanExpiryBanner from './PlanExpiryBanner';

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

describe('PlanExpiryBanner', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  const renderComponent = (props = {}) =>
    render(
      <MemoryRouter>
        <PlanExpiryBanner
          planName="Premium"
          fechaFin="2025-02-15"
          onDismiss={jest.fn()}
          {...props}
        />
      </MemoryRouter>
    );

  test('displays plan name', () => {
    renderComponent();
    expect(screen.getByText('Premium')).toBeInTheDocument();
  });

  test('displays expiry date', () => {
    renderComponent();
    expect(screen.getByText('2025-02-15')).toBeInTheDocument();
  });

  test('navigates to plans on renew button click', async () => {
    renderComponent();
    await userEvent.click(screen.getByText(/renovar plan/i));
    expect(mockNavigate).toHaveBeenCalledWith('/planes');
  });

  test('calls onDismiss when close button clicked', async () => {
    const onDismiss = jest.fn();
    renderComponent({ onDismiss });
    await userEvent.click(screen.getByLabelText(/cerrar/i));
    expect(onDismiss).toHaveBeenCalled();
  });

  test('shows renewal message', () => {
    renderComponent();
    expect(screen.getByText(/renuévalo para no perder/i)).toBeInTheDocument();
  });
});

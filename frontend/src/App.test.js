import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import App from './App';

test('renders MeerKatters Home', () => {
  render(
    <MemoryRouter>
      <App />
    </MemoryRouter>
  );
  const elements = screen.getAllByText(/MeerKatters/i);
  expect(elements.length).toBeGreaterThan(0);
});


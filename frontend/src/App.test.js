import { render, screen } from '@testing-library/react';
import App from './App';

test('renderiza el título de la app', () => {
  render(<App />);
  const heading = screen.getByText(/MeerKatters/i);
  expect(heading).toBeInTheDocument();
});

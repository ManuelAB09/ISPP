import React from 'react';
import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ScrollToTop from '../ScrollToTop';

describe('ScrollToTop', () => {
  beforeEach(() => {
    window.scrollTo = jest.fn();
  });

  test('calls window.scrollTo on initial render', () => {
    render(
      <MemoryRouter initialEntries={['/page1']}>
        <ScrollToTop />
      </MemoryRouter>
    );
    expect(window.scrollTo).toHaveBeenCalledWith(0, 0);
  });

  test('renders nothing (returns null)', () => {
    const { container } = render(
      <MemoryRouter>
        <ScrollToTop />
      </MemoryRouter>
    );
    expect(container.innerHTML).toBe('');
  });
});

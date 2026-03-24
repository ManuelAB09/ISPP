import React from 'react';
import { render, screen } from '@testing-library/react';
import PageHeader from './PageHeader';

describe('PageHeader', () => {
  test('renders title', () => {
    render(<PageHeader title="Test Title" subtitle="Test subtitle" />);
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Test Title');
  });

  test('renders subtitle', () => {
    render(<PageHeader title="Title" subtitle="My subtitle text" />);
    expect(screen.getByText('My subtitle text')).toBeInTheDocument();
  });

  test('applies additional className', () => {
    render(
      <PageHeader title="Title" subtitle="Sub" className="extra-class" />
    );
    expect(screen.getByTestId('page-header')).toHaveClass('page-header-title');
    expect(screen.getByTestId('page-header')).toHaveClass('extra-class');
  });

  test('renders separator line', () => {
    render(<PageHeader title="T" subtitle="S" />);
    expect(screen.getByTestId('page-header-line')).toBeInTheDocument();
  });
});

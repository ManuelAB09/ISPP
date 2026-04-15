import React from 'react';
import { render } from '@testing-library/react';
import CreateIcon from './Create';
import FilterIcon from './Filter';
import PersonIcon from './Person';

describe('CreateIcon', () => {
  test('renders with default props', () => {
    const { container } = render(<CreateIcon />);
    const svg = container.querySelector('svg');
    expect(svg).toBeInTheDocument();
    expect(svg).toHaveAttribute('width', '26');
    expect(svg).toHaveAttribute('height', '26');
  });

  test('renders with custom props', () => {
    const { container } = render(<CreateIcon width={40} height={40} fill="red" stroke="blue" strokeWidth={1} />);
    const svg = container.querySelector('svg');
    expect(svg).toHaveAttribute('width', '40');
    expect(svg).toHaveAttribute('height', '40');
  });
});

describe('FilterIcon', () => {
  test('renders with default props', () => {
    const { container } = render(<FilterIcon />);
    const svg = container.querySelector('svg');
    expect(svg).toBeInTheDocument();
    expect(svg).toHaveAttribute('width', '30');
    expect(svg).toHaveAttribute('height', '30');
  });

  test('renders with custom props', () => {
    const { container } = render(<FilterIcon width={50} height={50} stroke="red" strokeWidth={3} />);
    const svg = container.querySelector('svg');
    expect(svg).toHaveAttribute('width', '50');
  });
});

describe('PersonIcon', () => {
  test('renders with default props', () => {
    const { container } = render(<PersonIcon />);
    const svg = container.querySelector('svg');
    expect(svg).toBeInTheDocument();
    expect(svg).toHaveAttribute('width', '21');
    expect(svg).toHaveAttribute('height', '19');
  });

  test('renders with custom props', () => {
    const { container } = render(<PersonIcon width={30} height={30} fill="green" />);
    const svg = container.querySelector('svg');
    expect(svg).toHaveAttribute('width', '30');
  });
});

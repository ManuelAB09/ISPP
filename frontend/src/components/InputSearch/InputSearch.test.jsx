import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import InputSearch from './InputSearch';

describe('InputSearch', () => {
  test('renders with default placeholder', () => {
    render(<InputSearch value="" onChange={() => {}} />);
    expect(screen.getByPlaceholderText('Buscar elemento')).toBeInTheDocument();
  });

  test('renders with custom placeholder', () => {
    render(<InputSearch value="" onChange={() => {}} placeholder="Buscar comunidad" />);
    expect(screen.getByPlaceholderText('Buscar comunidad')).toBeInTheDocument();
  });

  test('displays current value', () => {
    render(<InputSearch value="test query" onChange={() => {}} />);
    expect(screen.getByDisplayValue('test query')).toBeInTheDocument();
  });

  test('calls onChange when typing', () => {
    const handleChange = jest.fn();
    render(<InputSearch value="" onChange={handleChange} />);
    const input = screen.getByPlaceholderText('Buscar elemento');
    fireEvent.change(input, { target: { value: 'hello' } });
    expect(handleChange).toHaveBeenCalled();
  });

  test('renders input element', () => {
    render(<InputSearch value="" onChange={() => {}} />);
    expect(screen.getByRole('textbox')).toBeInTheDocument();
  });
});

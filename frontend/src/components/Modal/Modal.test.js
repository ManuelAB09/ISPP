import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import Modal from './Modal';

jest.mock('./Modal.css', () => ({}));

describe('Modal', () => {
  test('returns null when isOpen is false', () => {
    const { container } = render(
      <Modal isOpen={false} onClose={jest.fn()}>
        <p>Content</p>
      </Modal>
    );
    expect(container.innerHTML).toBe('');
  });

  test('renders children when isOpen is true', () => {
    render(
      <Modal isOpen={true} onClose={jest.fn()}>
        <p>Hello Modal</p>
      </Modal>
    );
    expect(screen.getByText('Hello Modal')).toBeInTheDocument();
  });

  test('renders close button', () => {
    render(
      <Modal isOpen={true} onClose={jest.fn()}>
        <p>Content</p>
      </Modal>
    );
    expect(screen.getByText('×')).toBeInTheDocument();
  });

  test('calls onClose when overlay is clicked', () => {
    const onClose = jest.fn();
    render(
      <Modal isOpen={true} onClose={onClose}>
        <p>Content</p>
      </Modal>
    );
    fireEvent.click(document.querySelector('.modal-overlay'));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  test('calls onClose when close button is clicked', () => {
    const onClose = jest.fn();
    render(
      <Modal isOpen={true} onClose={onClose}>
        <p>Content</p>
      </Modal>
    );
    fireEvent.click(screen.getByText('×'));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  test('does not call onClose when content area is clicked', () => {
    const onClose = jest.fn();
    render(
      <Modal isOpen={true} onClose={onClose}>
        <p>Content</p>
      </Modal>
    );
    fireEvent.click(screen.getByText('Content'));
    expect(onClose).not.toHaveBeenCalled();
  });
});

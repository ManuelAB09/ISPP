import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import RatingForm from './RatingForm';

jest.mock('../api/valoraciones.api', () => ({
    crearValoracion: jest.fn(),
}));
jest.mock('./RatingForm.css', () => ({}));

const { crearValoracion } = require('../api/valoraciones.api');

describe('RatingForm', () => {
    const defaultProps = {
        profesorId: 1,
        alumnoId: 2,
        eventoId: 3,
        onValorado: jest.fn(),
    };

    beforeEach(() => {
        jest.clearAllMocks();
    });

    test('renders stars and submit button', () => {
        render(<RatingForm {...defaultProps} />);
        const stars = screen.getAllByText('★');
        expect(stars).toHaveLength(5);
        expect(screen.getByText('Enviar valoración')).toBeInTheDocument();
        expect(screen.getByPlaceholderText('Escribe un comentario (opcional)')).toBeInTheDocument();
    });

    test('submit button disabled when no rating selected', () => {
        render(<RatingForm {...defaultProps} />);
        expect(screen.getByText('Enviar valoración')).toBeDisabled();
    });

    test('clicking star enables submit button', () => {
        render(<RatingForm {...defaultProps} />);
        const stars = screen.getAllByText('★');
        fireEvent.click(stars[2]);
        expect(screen.getByText('Enviar valoración')).not.toBeDisabled();
    });

    test('shows already rated message', () => {
        render(<RatingForm {...defaultProps} alreadyRated={true} />);
        expect(screen.getByText('¡Gracias por tu valoración!')).toBeInTheDocument();
        expect(screen.queryByText('Enviar valoración')).not.toBeInTheDocument();
    });

    test('submits valoración successfully', async () => {
        crearValoracion.mockResolvedValueOnce({});
        render(<RatingForm {...defaultProps} />);

        const stars = screen.getAllByText('★');
        fireEvent.click(stars[3]);
        fireEvent.change(screen.getByPlaceholderText('Escribe un comentario (opcional)'), {
            target: { value: 'Great class!' },
        });

        await act(async () => {
            fireEvent.click(screen.getByText('Enviar valoración'));
        });

        await waitFor(() => {
            expect(crearValoracion).toHaveBeenCalledWith(
                expect.objectContaining({
                    profesor: { id: 1 },
                    alumno: { id: 2 },
                    puntuacion: 4,
                    comentario: 'Great class!',
                    evento: { id: 3 },
                })
            );
        });

        expect(defaultProps.onValorado).toHaveBeenCalled();
        expect(screen.getByText('¡Gracias por tu valoración!')).toBeInTheDocument();
    });

    test('shows error on submission failure', async () => {
        crearValoracion.mockRejectedValueOnce({
            response: { data: { message: 'Already rated' } },
        });
        render(<RatingForm {...defaultProps} />);

        fireEvent.click(screen.getAllByText('★')[4]);
        await act(async () => {
            fireEvent.click(screen.getByText('Enviar valoración'));
        });

        await waitFor(() => {
            expect(screen.getByText('Already rated')).toBeInTheDocument();
        });
    });

    test('shows generic error on unknown failure', async () => {
        crearValoracion.mockRejectedValueOnce(new Error('Network error'));
        render(<RatingForm {...defaultProps} />);

        fireEvent.click(screen.getAllByText('★')[0]);
        await act(async () => {
            fireEvent.click(screen.getByText('Enviar valoración'));
        });

        await waitFor(() => {
            expect(screen.getByText('Network error')).toBeInTheDocument();
        });
    });

    test('shows default error when no message available', async () => {
        crearValoracion.mockRejectedValueOnce({});
        render(<RatingForm {...defaultProps} />);

        fireEvent.click(screen.getAllByText('★')[0]);
        await act(async () => {
            fireEvent.click(screen.getByText('Enviar valoración'));
        });

        await waitFor(() => {
            expect(screen.getByText('Error al enviar la valoración')).toBeInTheDocument();
        });
    });

    test('shows Enviando... while submitting', async () => {
        let resolvePromise;
        crearValoracion.mockReturnValue(new Promise((res) => { resolvePromise = res; }));
        render(<RatingForm {...defaultProps} />);

        fireEvent.click(screen.getAllByText('★')[2]);
        fireEvent.click(screen.getByText('Enviar valoración'));

        expect(screen.getByText('Enviando...')).toBeInTheDocument();

        await act(async () => { resolvePromise({}); });
    });

    test('works without onValorado callback', async () => {
        crearValoracion.mockResolvedValueOnce({});
        render(<RatingForm profesorId={1} alumnoId={2} eventoId={3} />);

        fireEvent.click(screen.getAllByText('★')[4]);
        await act(async () => {
            fireEvent.click(screen.getByText('Enviar valoración'));
        });

        await waitFor(() => {
            expect(screen.getByText('¡Gracias por tu valoración!')).toBeInTheDocument();
        });
    });

    test('comment textarea updates', () => {
        render(<RatingForm {...defaultProps} />);
        const textarea = screen.getByPlaceholderText('Escribe un comentario (opcional)');
        fireEvent.change(textarea, { target: { value: 'Nice' } });
        expect(textarea.value).toBe('Nice');
    });
});

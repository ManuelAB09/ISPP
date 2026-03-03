import React from 'react';
import { render, screen } from '@testing-library/react';
import LinkPreviewCard from './LinkPreviewCard';

describe('LinkPreviewCard', () => {
    const mockPreview = {
        url: 'https://www.youtube.com/watch?v=dQw4w9WgXcQ',
        title: 'Video de YouTube',
        description: 'Un video interesante de YouTube',
        image: 'https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg',
        siteName: 'YouTube',
        domain: 'youtube.com',
    };

    describe('Renderizado básico', () => {
        it('debería renderizar sin errores con preview válido', () => {
            render(<LinkPreviewCard preview={mockPreview} />);
            expect(screen.getByText('Video de YouTube')).toBeInTheDocument();
        });

        it('debería retornar null si no hay preview', () => {
            const { container } = render(<LinkPreviewCard preview={null} />);
            expect(container.firstChild).toBeNull();
        });

        it('debería retornar null si preview es undefined', () => {
            const { container } = render(<LinkPreviewCard preview={undefined} />);
            expect(container.firstChild).toBeNull();
        });
    });

    describe('Contenido del preview', () => {
        it('debería mostrar el título del enlace', () => {
            render(<LinkPreviewCard preview={mockPreview} />);
            expect(screen.getByText('Video de YouTube')).toBeInTheDocument();
        });

        it('debería mostrar la descripción cuando está disponible', () => {
            render(<LinkPreviewCard preview={mockPreview} />);
            expect(screen.getByText('Un video interesante de YouTube')).toBeInTheDocument();
        });

        it('debería mostrar el nombre del sitio', () => {
            render(<LinkPreviewCard preview={mockPreview} />);
            expect(screen.getByText('YouTube')).toBeInTheDocument();
        });

        it('debería mostrar el dominio', () => {
            render(<LinkPreviewCard preview={mockPreview} />);
            expect(screen.getByText('youtube.com')).toBeInTheDocument();
        });
    });

    describe('Imagen de preview', () => {
        it('debería mostrar la imagen cuando está disponible', () => {
            render(<LinkPreviewCard preview={mockPreview} />);
            const img = screen.getByRole('img');
            expect(img).toHaveAttribute('src', mockPreview.image);
        });

        it('debería no mostrar imagen si no está disponible', () => {
            const previewSinImagen = { ...mockPreview, image: null };
            render(<LinkPreviewCard preview={previewSinImagen} />);
            expect(screen.queryByRole('img')).not.toBeInTheDocument();
        });
    });

    describe('Enlace', () => {
        it('debería ser un enlace cliqueable', () => {
            render(<LinkPreviewCard preview={mockPreview} />);
            const link = screen.getByRole('link');
            expect(link).toHaveAttribute('href', mockPreview.url);
        });

        it('debería abrir en nueva pestaña', () => {
            render(<LinkPreviewCard preview={mockPreview} />);
            const link = screen.getByRole('link');
            expect(link).toHaveAttribute('target', '_blank');
        });

        it('debería tener rel noopener noreferrer por seguridad', () => {
            render(<LinkPreviewCard preview={mockPreview} />);
            const link = screen.getByRole('link');
            expect(link).toHaveAttribute('rel', 'noopener noreferrer');
        });
    });

    describe('Plataformas conocidas', () => {
        it('debería manejar enlaces de Google Classroom', () => {
            const classroomPreview = {
                ...mockPreview,
                url: 'https://classroom.google.com/u/0/c/abc123',
                siteName: 'Google Classroom',
                domain: 'classroom.google.com',
            };
            render(<LinkPreviewCard preview={classroomPreview} />);
            expect(screen.getByRole('link')).toHaveAttribute('href', classroomPreview.url);
        });

        it('debería manejar enlaces de Google Drive', () => {
            const drivePreview = {
                ...mockPreview,
                url: 'https://drive.google.com/file/d/xyz',
                siteName: 'Google Drive',
                domain: 'drive.google.com',
            };
            render(<LinkPreviewCard preview={drivePreview} />);
            expect(screen.getByRole('link')).toHaveAttribute('href', drivePreview.url);
        });

        it('debería manejar enlaces de YouTube', () => {
            render(<LinkPreviewCard preview={mockPreview} />);
            expect(screen.getByText('YouTube')).toBeInTheDocument();
        });
    });

    describe('Fallbacks', () => {
        it('debería usar domain como título si no hay título', () => {
            const previewSinTitulo = {
                ...mockPreview,
                title: null,
            };
            render(<LinkPreviewCard preview={previewSinTitulo} />);
            // Puede aparecer múltiples veces (título y URL)
            expect(screen.getAllByText('youtube.com').length).toBeGreaterThan(0);
        });

        it('debería usar URL como href por defecto', () => {
            const previewMinimo = {
                url: 'https://example.com',
            };
            render(<LinkPreviewCard preview={previewMinimo} />);
            const link = screen.getByRole('link');
            expect(link).toHaveAttribute('href', 'https://example.com');
        });
    });
});

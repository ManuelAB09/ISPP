import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';

import CommunityApuntesTab from './CommunityApuntesTab';
import { apuntesApi } from '../../api/apuntes.api';

jest.mock('../../api/apuntes.api', () => ({
  apuntesApi: {
    obtenerApuntes: jest.fn(),
    buscarApuntes: jest.fn(),
    subirApunte: jest.fn(),
    descargarApunte: jest.fn(),
    eliminarApunte: jest.fn(),
  },
}));

const sampleApunte = {
  id: 1,
  titulo: 'Tema 1',
  descripcion: 'Descripción corta',
  tipoMime: 'application/pdf',
  usuarioNombre: 'María',
  usuarioId: 7,
  createdAt: '2026-01-01T10:00:00.000Z',
  tamanioArchivo: 2048,
  descargas: 3,
  nombreArchivo: 'tema1.pdf',
};

describe('CommunityApuntesTab', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.setItem('userId', '7');

    apuntesApi.obtenerApuntes.mockResolvedValue({ data: { content: [sampleApunte] } });
    apuntesApi.buscarApuntes.mockResolvedValue({ data: { content: [sampleApunte] } });

    window.URL.createObjectURL = jest.fn(() => 'blob:test');
    window.URL.revokeObjectURL = jest.fn();
    jest.spyOn(window, 'alert').mockImplementation(() => {});
    jest.spyOn(window, 'confirm').mockReturnValue(true);
  });

  test('loads and renders community notes', async () => {
    render(<CommunityApuntesTab communityId={10} isAdmin={false} isMember={true} />);

    expect(await screen.findByText(/tema 1/i)).toBeInTheDocument();
    expect(apuntesApi.obtenerApuntes).toHaveBeenCalledWith(10, 0, 10);
    expect(screen.getByText(/maría/i)).toBeInTheDocument();
  });

  test('searches notes by title when user types text', async () => {
    render(<CommunityApuntesTab communityId={10} isAdmin={false} isMember={true} />);
    await screen.findByText(/tema 1/i);

    fireEvent.change(screen.getByPlaceholderText(/buscar apuntes/i), {
      target: { value: 'spring' },
    });

    await waitFor(() => {
      expect(apuntesApi.buscarApuntes).toHaveBeenCalledWith(10, 'spring', 0, 10);
    });

    fireEvent.change(screen.getByPlaceholderText(/buscar apuntes/i), {
      target: { value: '   ' },
    });

    await waitFor(() => {
      expect(apuntesApi.obtenerApuntes).toHaveBeenCalledTimes(2);
    });
  });

  test('shows upload validation error when no file is selected', async () => {
    render(<CommunityApuntesTab communityId={10} isAdmin={false} isMember={true} />);
    await screen.findByText(/tema 1/i);

    fireEvent.click(screen.getByRole('button', { name: /subir apunte/i }));
    fireEvent.click(screen.getAllByRole('button', { name: /subir apunte/i })[1]);

    expect(await screen.findByText(/debes seleccionar un archivo/i)).toBeInTheDocument();
  });

  test('uploads note and prepends it to list', async () => {
    const uploaded = { ...sampleApunte, id: 2, titulo: 'Tema 2', nombreArchivo: 'tema2.pdf' };
    apuntesApi.subirApunte.mockResolvedValue({ data: uploaded });

    render(<CommunityApuntesTab communityId={10} isAdmin={false} isMember={true} />);
    await screen.findByText(/tema 1/i);

    fireEvent.click(screen.getByRole('button', { name: /subir apunte/i }));
    fireEvent.change(screen.getByLabelText(/título/i), { target: { value: 'Tema 2' } });
    fireEvent.change(screen.getByLabelText(/descripción/i), { target: { value: 'Nuevo contenido' } });
    fireEvent.change(screen.getByLabelText(/archivo/i), {
      target: { files: [new File(['abc'], 'tema2.pdf', { type: 'application/pdf' })] },
    });

    fireEvent.click(screen.getAllByRole('button', { name: /subir apunte/i })[1]);

    await waitFor(() => {
      expect(apuntesApi.subirApunte).toHaveBeenCalled();
    });
    expect(await screen.findByText(/tema 2/i)).toBeInTheDocument();
  });

  test('shows upload error message when API upload fails', async () => {
    apuntesApi.subirApunte.mockRejectedValueOnce({
      response: { data: { message: 'No se pudo subir' } },
    });

    render(<CommunityApuntesTab communityId={10} isAdmin={false} isMember={true} />);
    await screen.findByText(/tema 1/i);

    fireEvent.click(screen.getByRole('button', { name: /subir apunte/i }));
    fireEvent.change(screen.getByLabelText(/archivo/i), {
      target: { files: [new File(['abc'], 'tema3.pdf', { type: 'application/pdf' })] },
    });
    fireEvent.click(screen.getAllByRole('button', { name: /subir apunte/i })[1]);

    expect(await screen.findByText(/no se pudo subir/i)).toBeInTheDocument();
  });

  test('closes upload form when cancel button is clicked', async () => {
    render(<CommunityApuntesTab communityId={10} isAdmin={false} isMember={true} />);
    await screen.findByText(/tema 1/i);

    fireEvent.click(screen.getByRole('button', { name: /subir apunte/i }));
    expect(screen.getByText(/subir nuevo apunte/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /cancelar/i }));
    expect(screen.queryByText(/subir nuevo apunte/i)).not.toBeInTheDocument();
  });

  test('downloads a note file', async () => {
    const clickSpy = jest.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});

    apuntesApi.descargarApunte.mockResolvedValue({ data: new Uint8Array([1, 2, 3]) });

    render(<CommunityApuntesTab communityId={10} isAdmin={false} isMember={true} />);
    await screen.findByText(/tema 1/i);

    fireEvent.click(screen.getByRole('button', { name: /descargar/i }));

    await waitFor(() => {
      expect(apuntesApi.descargarApunte).toHaveBeenCalledWith(10, 1);
      expect(window.URL.createObjectURL).toHaveBeenCalled();
      expect(clickSpy).toHaveBeenCalled();
      expect(window.URL.revokeObjectURL).toHaveBeenCalledWith('blob:test');
    });

    clickSpy.mockRestore();
  });

  test('deletes a note when confirmed', async () => {
    apuntesApi.eliminarApunte.mockResolvedValue({});

    render(<CommunityApuntesTab communityId={10} isAdmin={true} isMember={true} />);
    await screen.findByText(/tema 1/i);

    const deleteButton = screen.getByTitle(/eliminar apunte/i);
    fireEvent.click(deleteButton);

    await waitFor(() => {
      expect(apuntesApi.eliminarApunte).toHaveBeenCalledWith(10, 1);
    });
  });

  test('does not delete note when user cancels confirmation', async () => {
    window.confirm.mockReturnValueOnce(false);

    render(<CommunityApuntesTab communityId={10} isAdmin={true} isMember={true} />);
    await screen.findByText(/tema 1/i);

    fireEvent.click(screen.getByTitle(/eliminar apunte/i));

    expect(apuntesApi.eliminarApunte).not.toHaveBeenCalled();
  });

  test('shows alert when download fails', async () => {
    apuntesApi.descargarApunte.mockRejectedValueOnce(new Error('download failed'));

    render(<CommunityApuntesTab communityId={10} isAdmin={false} isMember={true} />);
    await screen.findByText(/tema 1/i);

    fireEvent.click(screen.getByRole('button', { name: /descargar/i }));

    await waitFor(() => {
      expect(window.alert).toHaveBeenCalledWith('Error al descargar el apunte');
    });
  });

  test('shows alert when delete API fails', async () => {
    apuntesApi.eliminarApunte.mockRejectedValueOnce(new Error('delete failed'));

    render(<CommunityApuntesTab communityId={10} isAdmin={true} isMember={true} />);
    await screen.findByText(/tema 1/i);

    fireEvent.click(screen.getByTitle(/eliminar apunte/i));

    await waitFor(() => {
      expect(window.alert).toHaveBeenCalledWith('Error al eliminar el apunte');
    });
  });

  test('formats empty file size as 0 B', async () => {
    apuntesApi.obtenerApuntes.mockResolvedValueOnce({
      data: {
        content: [{ ...sampleApunte, id: 2, titulo: 'Sin tamaño', tamanioArchivo: 0 }],
      },
    });

    render(<CommunityApuntesTab communityId={10} isAdmin={false} isMember={true} />);

    expect(await screen.findByText(/sin tamaño/i)).toBeInTheDocument();
    expect(screen.getByText(/0 B/i)).toBeInTheDocument();
  });

  test('shows error message when initial load fails', async () => {
    apuntesApi.obtenerApuntes.mockRejectedValueOnce(new Error('Network error'));

    render(<CommunityApuntesTab communityId={10} isAdmin={false} isMember={true} />);

    expect(await screen.findByText(/no se pudieron cargar los apuntes/i)).toBeInTheDocument();
  });
});

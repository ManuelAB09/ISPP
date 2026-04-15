import React from 'react';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';

import MisBorradores from './MisBorradores';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

jest.mock('../../contexts/AuthContext', () => ({
  useAuth: () => ({ user: { id: 1, nombre: 'Tester' } }),
}));

jest.mock('../../components/Header/Header', () => () => <div data-testid="header-mock">Header</div>);

describe('MisBorradores', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();
  });

  test('shows empty state for event drafts and navigates to create event', () => {
    render(<MisBorradores />);

    expect(screen.getByText(/no tienes borradores de eventos guardados/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /crear evento/i }));
    expect(mockNavigate).toHaveBeenCalledWith('/crear-evento/new');
  });

  test('loads event drafts, allows continue and delete with confirmation', async () => {
    localStorage.setItem(
      'eventDrafts',
      JSON.stringify([
        {
          nombre: 'Evento ISPP',
          descripcion: 'Descripción del borrador',
          dia: '10',
          mes: '05',
          anio: '2026',
          hora: '10',
          minuto: '30',
          tipoLocalizacion: 'Online',
          aforo: '20',
          selectedCommunityId: 55,
          savedAt: '2026-05-01T10:00:00.000Z',
        },
      ])
    );

    render(<MisBorradores />);

    expect(await screen.findByText(/evento ispp/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /continuar/i }));
    expect(localStorage.getItem('eventDraftIndex')).toBe('0');
    expect(mockNavigate).toHaveBeenCalledWith('/crear-evento/new?communityId=55', {
      state: { eventFormDraft: expect.objectContaining({ nombre: 'Evento ISPP' }) },
    });

    fireEvent.click(screen.getByRole('button', { name: /eliminar/i }));
    const eventModal = screen.getByText(/eliminar este borrador de evento/i).closest('.borradores-modal');
    fireEvent.click(within(eventModal).getByRole('button', { name: /^eliminar$/i }));

    await waitFor(() => {
      const drafts = JSON.parse(localStorage.getItem('eventDrafts') || '[]');
      expect(drafts).toHaveLength(0);
    });
  });

  test('loads community draft and supports continue/delete actions', async () => {
    localStorage.setItem(
      'crearComunidadDraft',
      JSON.stringify({
        nombre: 'Comunidad QA',
        descripcion: 'Grupo de pruebas',
        tipoComunidad: 'GRUPO_PRIVADO',
        maxMiembros: 30,
        categorias: ['QA', 'Testing'],
        savedAt: '2026-04-01T09:00:00.000Z',
      })
    );

    render(<MisBorradores />);

    fireEvent.click(screen.getByRole('button', { name: /comunidades/i }));

    expect(await screen.findByText(/comunidad qa/i)).toBeInTheDocument();

    const continuarButtons = screen.getAllByRole('button', { name: /continuar/i });
    fireEvent.click(continuarButtons[0]);
    expect(mockNavigate).toHaveBeenCalledWith('/crear-comunidad');

    const deleteButtons = screen.getAllByRole('button', { name: /eliminar/i });
    fireEvent.click(deleteButtons[0]);
    const communityModal = screen.getByText(/eliminar el borrador de comunidad/i).closest('.borradores-modal');
    fireEvent.click(within(communityModal).getByRole('button', { name: /^eliminar$/i }));

    await waitFor(() => {
      expect(localStorage.getItem('crearComunidadDraft')).toBeNull();
    });
  });

  test('handles malformed draft storage and allows create actions in empty tabs', async () => {
    localStorage.setItem('eventDrafts', '{invalid json');
    localStorage.setItem('crearComunidadDraft', '{invalid json');

    render(<MisBorradores />);

    expect(await screen.findByText(/no tienes borradores de eventos guardados/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /comunidades/i }));
    expect(await screen.findByText(/no tienes borradores de comunidades guardados/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /crear comunidad/i }));
    expect(mockNavigate).toHaveBeenCalledWith('/crear-comunidad');

    fireEvent.click(screen.getByRole('button', { name: /eventos/i }));
    expect(screen.getByText(/no tienes borradores de eventos guardados/i)).toBeInTheDocument();
  });
});

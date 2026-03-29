import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import GoogleClassroomButton from './GoogleClassroomButton';

jest.mock('./GoogleClassroomButton.css', () => ({}));
jest.mock('../../api/communities.api', () => ({
  communitiesApi: {
    linkClassroom: jest.fn(),
    unlinkClassroom: jest.fn(),
  },
}));
jest.mock('../../api/baseUrl', () => ({
  getApiBaseUrl: () => 'http://localhost:8080',
}));

const { communitiesApi } = require('../../api/communities.api');

describe('GoogleClassroomButton', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    global.fetch = jest.fn();
    global.window.open = jest.fn();
  });

  afterEach(() => {
    delete global.fetch;
  });

  test('returns null when not admin and no linked course', () => {
    const { container } = render(
      <GoogleClassroomButton communityId={1} linkedCourse={null} isAdmin={false} onLinked={jest.fn()} />
    );
    expect(container.innerHTML).toBe('');
  });

  test('shows linked course info when linkedCourse is provided', () => {
    render(
      <GoogleClassroomButton
        communityId={1}
        linkedCourse={{ courseId: '123', courseName: 'Math 101' }}
        isAdmin={false}
        onLinked={jest.fn()}
      />
    );
    expect(screen.getByText('Google Classroom vinculado')).toBeInTheDocument();
    expect(screen.getByText('Math 101')).toBeInTheDocument();
  });

  test('shows unlink button when admin and linked', () => {
    render(
      <GoogleClassroomButton
        communityId={1}
        linkedCourse={{ courseId: '123', courseName: 'Math 101' }}
        isAdmin={true}
        onLinked={jest.fn()}
      />
    );
    expect(screen.getByText('Desvincular curso')).toBeInTheDocument();
  });

  test('does not show unlink button when not admin', () => {
    render(
      <GoogleClassroomButton
        communityId={1}
        linkedCourse={{ courseId: '123', courseName: 'Math 101' }}
        isAdmin={false}
        onLinked={jest.fn()}
      />
    );
    expect(screen.queryByText('Desvincular curso')).not.toBeInTheDocument();
  });

  test('shows auth button when admin and no linked course', () => {
    render(
      <GoogleClassroomButton communityId={1} linkedCourse={null} isAdmin={true} onLinked={jest.fn()} />
    );
    expect(screen.getByText('Vincular curso de Google Classroom')).toBeInTheDocument();
  });

  test('handleUnlink calls unlinkClassroom and onLinked', async () => {
    const onLinked = jest.fn();
    communitiesApi.unlinkClassroom.mockResolvedValue({});
    render(
      <GoogleClassroomButton
        communityId={5}
        linkedCourse={{ courseId: '123', courseName: 'Math' }}
        isAdmin={true}
        onLinked={onLinked}
      />
    );
    fireEvent.click(screen.getByText('Desvincular curso'));
    await waitFor(() => expect(communitiesApi.unlinkClassroom).toHaveBeenCalledWith(5));
    await waitFor(() => expect(onLinked).toHaveBeenCalled());
  });

  test('handleUnlink shows error on failure', async () => {
    communitiesApi.unlinkClassroom.mockRejectedValue(new Error('Network error'));
    render(
      <GoogleClassroomButton
        communityId={5}
        linkedCourse={{ courseId: '123', courseName: 'Math' }}
        isAdmin={true}
        onLinked={jest.fn()}
      />
    );
    fireEvent.click(screen.getByText('Desvincular curso'));
    await screen.findByText(/Network error/);
  });

  test('openAuth fetches URL and opens window', async () => {
    global.fetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ url: 'https://accounts.google.com/oauth' }),
    });
    localStorage.setItem('accessToken', 'test-token');

    render(
      <GoogleClassroomButton communityId={1} linkedCourse={null} isAdmin={true} onLinked={jest.fn()} />
    );

    fireEvent.click(screen.getByText('Vincular curso de Google Classroom'));
    await waitFor(() => expect(global.fetch).toHaveBeenCalled());
    await waitFor(() => expect(global.window.open).toHaveBeenCalled());

    localStorage.removeItem('accessToken');
  });

  test('openAuth handles fetch error', async () => {
    global.fetch.mockResolvedValue({
      ok: false,
      json: () => Promise.resolve({ error: 'bad request' }),
    });

    const consoleSpy = jest.spyOn(console, 'error').mockImplementation();

    render(
      <GoogleClassroomButton communityId={1} linkedCourse={null} isAdmin={true} onLinked={jest.fn()} />
    );
    fireEvent.click(screen.getByText('Vincular curso de Google Classroom'));
    await waitFor(() => expect(global.fetch).toHaveBeenCalled());
    expect(global.window.open).not.toHaveBeenCalled();
    consoleSpy.mockRestore();
  });

  test('listens for postMessage with courses and allows linking', async () => {
    const onLinked = jest.fn();
    communitiesApi.linkClassroom.mockResolvedValue({});

    render(
      <GoogleClassroomButton communityId={1} linkedCourse={null} isAdmin={true} onLinked={onLinked} />
    );

    act(() => {
      window.dispatchEvent(new MessageEvent('message', {
        data: { courses: [{ id: 'c1', name: 'Science' }, { id: 'c2', name: 'History' }] },
      }));
    });

    await screen.findByText('Selecciona un curso para vincular:');
    expect(screen.getByText('Science')).toBeInTheDocument();
    expect(screen.getByText('History')).toBeInTheDocument();

    fireEvent.click(screen.getAllByText('Vincular')[0]);
    await waitFor(() => expect(communitiesApi.linkClassroom).toHaveBeenCalledWith(1, {
      courseId: 'c1',
      courseName: 'Science',
    }));
    await waitFor(() => expect(onLinked).toHaveBeenCalled());
  });

  test('shows empty courses message', async () => {
    render(
      <GoogleClassroomButton communityId={1} linkedCourse={null} isAdmin={true} onLinked={jest.fn()} />
    );

    act(() => {
      window.dispatchEvent(new MessageEvent('message', {
        data: { courses: [] },
      }));
    });

    await screen.findByText('No tienes cursos en Google Classroom');
  });

  test('handles postMessage error', async () => {
    render(
      <GoogleClassroomButton communityId={1} linkedCourse={null} isAdmin={true} onLinked={jest.fn()} />
    );

    act(() => {
      window.dispatchEvent(new MessageEvent('message', {
        data: { error: 'insufficient_scopes' },
      }));
    });

    await screen.findByText(/Debes conceder permisos/);
  });

  test('handles postMessage with generic error', async () => {
    render(
      <GoogleClassroomButton communityId={1} linkedCourse={null} isAdmin={true} onLinked={jest.fn()} />
    );

    act(() => {
      window.dispatchEvent(new MessageEvent('message', {
        data: { error: 'some_error' },
      }));
    });

    await screen.findByText(/some_error/);
  });

  test('handles postMessage with nested courses.courses', async () => {
    render(
      <GoogleClassroomButton communityId={1} linkedCourse={null} isAdmin={true} onLinked={jest.fn()} />
    );

    act(() => {
      window.dispatchEvent(new MessageEvent('message', {
        data: { courses: { courses: [{ id: 'c1', name: 'Nested' }] } },
      }));
    });

    await screen.findByText('Nested');
  });

  test('handles linkClassroom error', async () => {
    communitiesApi.linkClassroom.mockRejectedValue(new Error('Link failed'));

    render(
      <GoogleClassroomButton communityId={1} linkedCourse={null} isAdmin={true} onLinked={jest.fn()} />
    );

    act(() => {
      window.dispatchEvent(new MessageEvent('message', {
        data: { courses: [{ id: 'c1', name: 'Science' }] },
      }));
    });

    await screen.findByText('Science');
    fireEvent.click(screen.getAllByText('Vincular')[0]);
    await screen.findByText(/Link failed/);
  });

  test('ignores postMessage with no data', () => {
    render(
      <GoogleClassroomButton communityId={1} linkedCourse={null} isAdmin={true} onLinked={jest.fn()} />
    );

    act(() => {
      window.dispatchEvent(new MessageEvent('message', { data: null }));
    });

    expect(screen.queryByText('Selecciona un curso para vincular:')).not.toBeInTheDocument();
  });

  test('course without name shows courseState or JSON', async () => {
    render(
      <GoogleClassroomButton communityId={1} linkedCourse={null} isAdmin={true} onLinked={jest.fn()} />
    );

    act(() => {
      window.dispatchEvent(new MessageEvent('message', {
        data: { courses: [{ id: 'c1', courseState: 'ACTIVE' }] },
      }));
    });

    await screen.findByText('ACTIVE');
  });
});

import React, { useEffect, useState } from 'react';
import { communitiesApi } from '../../api/communities.api';
import './GoogleClassroomButton.css';

/**
 * Botón de Google Classroom para vincular un curso a una comunidad.
 *
 * Props:
 *  - communityId:  ID de la comunidad a vincular
 *  - linkedCourse: objeto { courseId, courseName } si ya hay un curso vinculado (puede ser null)
 *  - isAdmin:      si el usuario actual es admin de la comunidad
 *  - onLinked:     callback tras vincular/desvincular (para refrescar el padre)
 */
export default function GoogleClassroomButton({ communityId, linkedCourse, isAdmin, onLinked }) {
  const [courses, setCourses] = useState(null);
  const [error, setError] = useState(null);
  const [linking, setLinking] = useState(false);
  const [unlinking, setUnlinking] = useState(false);

  // Escuchar el postMessage del popup de OAuth
  useEffect(() => {
    function handler(e) {
      if (!e.data) return;
      if (e.data.error) {
        setError(e.data.error);
        setCourses(null);
      } else if (e.data.courses) {
        const courseData = e.data.courses.courses || e.data.courses;
        setCourses(Array.isArray(courseData) ? courseData : []);
        setError(null);
      }
    }
    window.addEventListener('message', handler);
    return () => window.removeEventListener('message', handler);
  }, []);

  const openAuth = async () => {
    const width = 600, height = 700;
    const left = window.screenX + (window.innerWidth - width) / 2;
    const top = window.screenY + (window.innerHeight - height) / 2;

    const backendBase = process.env.REACT_APP_BACKEND_URL || 'http://localhost:8080';
    const token = localStorage.getItem('accessToken');

    const resp = await fetch(`${backendBase}/oauth2/authorize/google-classroom-url`, {
      headers: { Authorization: `Bearer ${token}` },
    });

    const data = await resp.json();
    if (!resp.ok) {
      console.error(data);
      return;
    }

    window.open(data.url, 'google_classroom', `width=${width},height=${height},left=${left},top=${top}`);
  };

  const handleLink = async (course) => {
    try {
      setLinking(true);
      await communitiesApi.linkClassroom(communityId, {
        courseId: course.id,
        courseName: course.name || course.courseState || 'Sin nombre',
      });
      setCourses(null); // Ocultar lista tras vincular
      if (onLinked) onLinked();
    } catch (err) {
      console.error('Error al vincular curso:', err);
      setError(err.message || 'Error al vincular el curso');
    } finally {
      setLinking(false);
    }
  };

  const handleUnlink = async () => {
    try {
      setUnlinking(true);
      await communitiesApi.unlinkClassroom(communityId);
      if (onLinked) onLinked();
    } catch (err) {
      console.error('Error al desvincular curso:', err);
      setError(err.message || 'Error al desvincular el curso');
    } finally {
      setUnlinking(false);
    }
  };

  // Si ya hay curso vinculado, mostrar info + botón de desvincular (solo admins)
  if (linkedCourse) {
    return (
      <div className="gc-section">
        <div className="gc-linked">
          <span className="gc-linked-icon">📚</span>
          <div>
            <strong>Google Classroom vinculado</strong>
            <p style={{ margin: '0.25rem 0 0', fontSize: '0.875rem', color: '#555' }}>
              {linkedCourse.courseName}
            </p>
          </div>
        </div>
        {isAdmin && (
          <button
            className="gc-btn gc-btn-unlink"
            onClick={handleUnlink}
            disabled={unlinking}
          >
            {unlinking ? 'Desvinculando...' : 'Desvincular curso'}
          </button>
        )}
        {error && <div style={{ color: 'red', marginTop: '0.5rem' }}>Error: {error}</div>}
      </div>
    );
  }

  // Si no es admin, no mostrar nada
  if (!isAdmin) return null;

  return (
    <div className="gc-section">
      <button className="gc-btn gc-btn-link" onClick={openAuth}>
        Vincular curso de Google Classroom
      </button>

      {error && <div style={{ color: 'red', marginTop: '0.5rem' }}>Error: {error}</div>}

      {courses !== null && (
        <div className="gc-courses-list">
          {courses.length === 0 ? (
            <p style={{ fontSize: '0.875rem', color: '#888' }}>No tienes cursos en Google Classroom</p>
          ) : (
            <>
              <h4 style={{ margin: '0.75rem 0 0.5rem' }}>Selecciona un curso para vincular:</h4>
              <ul className="gc-courses">
                {courses.map((c) => (
                  <li key={c.id} className="gc-course-item">
                    <span>{c.name || c.courseState || JSON.stringify(c)}</span>
                    <button
                      className="gc-btn gc-btn-select"
                      onClick={() => handleLink(c)}
                      disabled={linking}
                    >
                      {linking ? 'Vinculando...' : 'Vincular'}
                    </button>
                  </li>
                ))}
              </ul>
            </>
          )}
        </div>
      )}
    </div>
  );
}

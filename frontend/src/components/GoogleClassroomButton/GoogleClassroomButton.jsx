import React, { useEffect, useState } from 'react';

export default function GoogleClassroomButton() {
  const [courses, setCourses] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    function handler(e) {
      if (!e.data) return;
      if (e.data.error) {
        setError(e.data.error);
        setCourses(null);
      } else if (e.data.courses) {
        setCourses(e.data.courses.courses || e.data.courses);
        setError(null);
      }
    }
    window.addEventListener('message', handler);
    return () => window.removeEventListener('message', handler);
  }, []);

  const openAuth = () => {
    const width = 600, height = 700;
    const left = window.screenX + (window.innerWidth - width) / 2;
    const top = window.screenY + (window.innerHeight - height) / 2;
    const backendBase = process.env.REACT_APP_BACKEND_URL || 'http://localhost:8080';
    const url = `${backendBase.replace(/\/$/, '')}/oauth2/authorize/google-classroom`;
    window.open(url, 'google_classroom', `width=${width},height=${height},left=${left},top=${top}`);
  };

  return (
    <div>
      <button onClick={openAuth}>Iniciar sesión con Google Classroom</button>

      {error && <div style={{ color: 'red' }}>Error: {error}</div>}

      {courses && (
        <div>
          <h4>Mis cursos</h4>
          <ul>
            {courses.map((c) => (
              <li key={c.id}>{c.name || c.courseState || JSON.stringify(c)}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

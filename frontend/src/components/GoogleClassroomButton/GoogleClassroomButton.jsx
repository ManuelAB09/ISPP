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
  const token = localStorage.getItem("accessToken"); 
  console.log("TOKEN:", token);

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

  return (
    <div>
      <button className="classroom-btn" onClick={openAuth}>Iniciar sesión con Google Classroom</button>

      {error && <div style={{ color: 'red' }}>Error: {error}</div>}

      {courses !== null && (
        <div>
          {courses.length === 0 ? (
            <p>No tienes cursos actualmente</p>
          ) : (
            <>
              <h4>Mis cursos</h4>
              <ul>
                {courses.map((c) => (
                  <li key={c.id}>{c.name || c.courseState || JSON.stringify(c)}</li>
                ))}
              </ul>
            </>
          )}
        </div>
      )}
    </div>
  );
}

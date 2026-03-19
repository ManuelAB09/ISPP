import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { LuActivity, LuBookOpen, LuMessageSquare, LuTrophy, LuArrowLeft } from 'react-icons/lu';
import { usersApi } from '../../api/users.api';
import Header from '../../components/Header/Header';
import './DashboardProgreso.css';
import { useAuth } from '../../contexts/AuthContext';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

const DashboardProgreso = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [activity, setActivity] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchActivity = async () => {
      try {
        const data = await usersApi.getMyActivity();
        setActivity(data);
      } catch (error) {
        console.error('Error fetching dashboard progress', error);
      } finally {
        setLoading(false);
      }
    };
    fetchActivity();
  }, []);

  if (loading) return <div>Cargando progreso...</div>;
  if (!activity) return <div>No se pudo cargar la información.</div>;

  const cuestionarios = activity.cuestionarios || [];
  const asistencias = activity.asistencias || [];
  const feedbacks = activity.feedbacks || [];

  // Data map for chart (Puntuaciones by Cuestionario)
  const chartData = cuestionarios.map((c, index) => ({
    name: c.titulo?.substring(0, 15) || `C. ${index+1}`,
    puntuacion: c.puntuacion || 0,
    fullTitle: c.titulo
  }));

  const avgScore = cuestionarios.length > 0 
    ? (cuestionarios.reduce((acc, c) => acc + (c.puntuacion || 0), 0) / cuestionarios.length).toFixed(1)
    : 0;

  return (
    <>
      <Header />
      <div className="dashboard-progreso-container">
        <div className="dashboard-header">
          <button className="btn-secondary" onClick={() => navigate(-1)} style={{ padding: '0.4rem', border:'none', background:'transparent', fontSize: '1.5rem', cursor: 'pointer' }}>
             <LuArrowLeft />
          </button>
          <h1>Tu Progreso y Rendimiento</h1>
        </div>

        <div className="stats-grid">
          <div className="stat-card">
            <div className="stat-icon"><LuBookOpen /></div>
            <h3>Cuestionarios Realizados</h3>
            <p className="stat-value">{cuestionarios.length}</p>
          </div>
          <div className="stat-card">
            <div className="stat-icon"><LuActivity /></div>
            <h3>Nota Media</h3>
            <p className="stat-value">{avgScore} / 10</p>
          </div>
          <div className="stat-card">
            <div className="stat-icon"><LuTrophy /></div>
            <h3>Eventos/Clases Asistidas</h3>
            <p className="stat-value">{asistencias.length}</p>
          </div>
          <div className="stat-card">
            <div className="stat-icon"><LuMessageSquare /></div>
            <h3>Feedbacks Recibidos</h3>
            <p className="stat-value">{feedbacks.length}</p>
          </div>
        </div>

        <div className="dashboard-section">
          <h2>Rendimiento en Cuestionarios</h2>
          {chartData.length > 0 ? (
            <div className="chart-container">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData} margin={{ top: 20, right: 30, left: 0, bottom: 5 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="name" />
                  <YAxis domain={[0, 10]} />
                  <Tooltip />
                  <Bar dataKey="puntuacion" fill="#339af0" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          ) : (
             <p>Aún no tienes cuestionarios para mostrar gráficas.</p>
          )}
        </div>

        <div className="dashboard-section">
          <h2>Último Feedback de Profesores</h2>
          {feedbacks.length === 0 ? (
            <p>Aún no has recibido valoraciones personalizadas.</p>
          ) : (
            <div>
              {feedbacks.slice(0, 5).map((fb, idx) => (
                <div key={idx} className="feedback-card">
                  <div className="feedback-header">
                    <span className="feedback-author">Profesor ID: {fb.profesorId || 'Desconocido'}</span>
                    <span className="feedback-date">{fb.fecha ? new Date(fb.fecha).toLocaleDateString() : ''}</span>
                  </div>
                  <p className="feedback-content">"{fb.contenido}"</p>
                  {fb.calificacion && (
                    <div style={{ marginTop: '0.5rem', color: '#f08c00', fontWeight: 'bold' }}>
                       Valoración: {fb.calificacion} / 5
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="dashboard-section">
          <h2>Historial de Cuestionarios</h2>
          {cuestionarios.length === 0 ? (
             <p>No hay historial.</p>
          ) : (
            <div>
              {cuestionarios.map((c, idx) => (
                <div key={idx} className="list-item">
                  <div>
                    <div className="list-item-title">{c.titulo || `Cuestionario #${c.cuestionarioId}`}</div>
                    <div className="list-item-subtitle">{c.fecha ? new Date(c.fecha).toLocaleString() : ''}</div>
                  </div>
                  <div className="score-badge">
                     Nota: {c.puntuacion || 0}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

      </div>
    </>
  );
};

export default DashboardProgreso;

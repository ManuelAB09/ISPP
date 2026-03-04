import React, { useEffect, useState } from "react";
import { classroomLinkRequestsApi } from "../../api/classroomLinkRequests.api";

export default function ClassroomLinkRequests() {
  const [requests, setRequests] = useState([]);
  const [activeRequestId, setActiveRequestId] = useState(null);
  const [courses, setCourses] = useState(null);
  const [loading, setLoading] = useState(true);
  const [linking, setLinking] = useState(false);
  const [error, setError] = useState(null);

  const backendBase = process.env.REACT_APP_BACKEND_URL || "http://localhost:8080";

  const load = async () => {
    try {
      setLoading(true);
      const resp = await classroomLinkRequestsApi.myPending();
      setRequests(resp.data || []);
    } catch (e) {
      console.error(e);
      setError("No se pudieron cargar las solicitudes");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  // Recibir postMessage del popup
  useEffect(() => {
    const handler = (e) => {
      if (!e.data) return;

      if (e.data.error) {
        setError(e.data.error);
        setCourses(null);
        setActiveRequestId(null);
        return;
      }

      if (e.data.courses) {
        const courseData = e.data.courses.courses || e.data.courses;
        setCourses(Array.isArray(courseData) ? courseData : []);
        setError(null);

        if (e.data.requestId) {
          setActiveRequestId(Number(e.data.requestId));
        }
      }
    };

    window.addEventListener("message", handler);
    return () => window.removeEventListener("message", handler);
  }, []);

  const openAuth = async (requestId) => {
    const width = 600,
      height = 700;
    const left = window.screenX + (window.innerWidth - width) / 2;
    const top = window.screenY + (window.innerHeight - height) / 2;

    const token = localStorage.getItem("accessToken");

    const resp = await fetch(
      `${backendBase}/oauth2/authorize/google-classroom-url?requestId=${requestId}`,
      { headers: { Authorization: `Bearer ${token}` } }
    );

    const data = await resp.json();
    if (!resp.ok) {
      console.error(data);
      setError(data?.error || "Error iniciando OAuth");
      return;
    }

    window.open(
      data.url,
      "google_classroom",
      `width=${width},height=${height},left=${left},top=${top}`
    );
  };

  const completeRequest = async (course) => {
    if (!activeRequestId) {
      setError("No hay requestId activo (¿se abrió OAuth desde una solicitud?)");
      return;
    }

    try {
      setLinking(true);
      await classroomLinkRequestsApi.complete(activeRequestId, {
        cursoId: course.id,
        nombreCurso: course.name || "Sin nombre",
      });

      // limpiar UI
      setCourses(null);
      setActiveRequestId(null);

      // refrescar solicitudes (ya debería desaparecer la que acabas de completar)
      await load();
    } catch (e) {
      console.error(e);
      setError("Error completando la solicitud");
    } finally {
      setLinking(false);
    }
  };

  if (loading) return <div>Cargando solicitudes...</div>;

  return (
    <div style={{ padding: "1rem", border: "1px solid #e2e5f1", borderRadius: 10 }}>
      <h3 style={{ marginTop: 0 }}>Solicitudes de vinculación Classroom</h3>

      {error && <div style={{ color: "red", marginBottom: "0.75rem" }}>Error: {error}</div>}

      {requests.length === 0 && <div>No tienes solicitudes pendientes.</div>}

      {requests.length > 0 && (
        <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
          {requests.map((r) => (
            <li
              key={r.id}
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                padding: "0.6rem 0.75rem",
                background: "white",
                border: "1px solid #e2e5f1",
                borderRadius: 8,
                marginBottom: "0.5rem",
              }}
            >
              <div>
                <div style={{ fontWeight: 600 }}>Solicitud #{r.id}</div>
                <div style={{ fontSize: "0.85rem", color: "#666" }}>
                  Comunidad: {r.comunidadId} · Estado: {r.estado}
                </div>
              </div>

              <button onClick={() => openAuth(r.id)} style={{ padding: "0.4rem 0.8rem" }}>
                Autorizar
              </button>
            </li>
          ))}
        </ul>
      )}

      {courses !== null && (
        <div style={{ marginTop: "1rem" }}>
          <h4 style={{ margin: "0 0 0.5rem" }}>Selecciona un curso</h4>

          {courses.length === 0 ? (
            <div style={{ color: "#666" }}>No tienes cursos.</div>
          ) : (
            <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
              {courses.map((c) => (
                <li
                  key={c.id}
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    padding: "0.6rem 0.75rem",
                    border: "1px solid #e2e5f1",
                    borderRadius: 8,
                    marginBottom: "0.5rem",
                  }}
                >
                  <span>{c.name || c.id}</span>
                  <button
                    onClick={() => completeRequest(c)}
                    disabled={linking}
                    style={{ padding: "0.35rem 0.75rem" }}
                  >
                    {linking ? "Vinculando..." : "Vincular"}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
import { useCallback, useEffect, useState } from "react";
import { acceptHiringRequest, getMyHiringRequests, rejectHiringRequest } from "../../api/tutorEndpoints";
import { useAuth } from "../../contexts/AuthContext";
import "./TutorHiringRequests.css";

/**
 * Pantalla para que los tutores vean y gestionen sus solicitudes de contratación
 */
const TutorHiringRequests = () => {
  const { user } = useAuth();
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [processingId, setProcessingId] = useState(null);
  const [rejecting, setRejecting] = useState(null);
  const [rejectMotivo, setRejectMotivo] = useState("");

  const loadRequests = useCallback(async () => {
    if (!user) return;
    
    setLoading(true);
    setError("");
    try {
      const response = await getMyHiringRequests({ page: 0, size: 50 });
      setRequests(response?.content || []);
    } catch (err) {
      setError(err?.response?.data?.message || "Error al cargar solicitudes");
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    loadRequests();
  }, [loadRequests]);

  const handleAccept = async (requestId) => {
    if (!window.confirm("¿Estás seguro de aceptar esta solicitud?")) {
      return;
    }

    setProcessingId(requestId);
    setError("");
    try {
      const response = await acceptHiringRequest(requestId);
      alert("Solicitud aceptada. Se ha notificado a la comunidad para que proceda al pago.");
      
      // Si el backend retorna paymentUrl, podríamos mostrarla aquí
      if (response?.paymentUrl) {
        console.log("URL de pago generada:", response.paymentUrl);
      }
      
      // Recargar solicitudes
      await loadRequests();
    } catch (err) {
      const msg = err?.response?.data?.error || err?.message || "Error al aceptar solicitud";
      setError(msg);
    } finally {
      setProcessingId(null);
    }
  };

  const handleReject = async (requestId) => {
    if (!rejectMotivo.trim()) {
      alert("Por favor, indica un motivo para el rechazo");
      return;
    }

    setProcessingId(requestId);
    setError("");
    try {
      await rejectHiringRequest(requestId, rejectMotivo);
      alert("Solicitud rechazada. Se ha notificado a la comunidad.");
      setRejecting(null);
      setRejectMotivo("");
      
      // Recargar solicitudes
      await loadRequests();
    } catch (err) {
      const msg = err?.response?.data?.error || err?.message || "Error al rechazar solicitud";
      setError(msg);
    } finally {
      setProcessingId(null);
    }
  };

  if (loading) {
    return (
      <div className="thr-container">
        <h1 className="thr-title">Solicitudes de Contratación</h1>
        <p className="thr-loading">Cargando solicitudes...</p>
      </div>
    );
  }

  return (
    <div className="thr-container">
      <div className="thr-header">
        <h1 className="thr-title">Solicitudes de Contratación</h1>
        <p className="thr-subtitle">
          Revisa y gestiona las solicitudes de contratación de las comunidades
        </p>
      </div>

      {error && (
        <div className="thr-error">
          <span>⚠️</span> {error}
        </div>
      )}

      {requests.length === 0 ? (
        <div className="thr-empty">
          <div className="thr-empty-icon">📭</div>
          <h2>No tienes solicitudes pendientes</h2>
          <p>Cuando una comunidad te solicite, aparecerá aquí</p>
        </div>
      ) : (
        <div className="thr-list">
          {requests.map((request) => (
            <div key={request.id} className="thr-card">
              <div className="thr-card-header">
                <div className="thr-community-info">
                  {request.comunidad?.foto && (
                    <img
                      src={request.comunidad.foto}
                      alt={request.comunidad.nombre}
                      className="thr-community-photo"
                    />
                  )}
                  <div>
                    <h3 className="thr-community-name">{request.comunidad?.nombre}</h3>
                    <p className="thr-community-creator">
                      Solicitud de: <strong>{request.comunidad?.creador?.nombre}</strong>
                    </p>
                  </div>
                </div>
                <span className={`thr-badge thr-badge--${request.estado?.toLowerCase()}`}>
                  {request.estado}
                </span>
              </div>

              <div className="thr-card-body">
                {request.comunidad?.descripcion && (
                  <p className="thr-community-desc">{request.comunidad.descripcion}</p>
                )}

                <div className="thr-details">
                  <div className="thr-detail">
                    <span className="thr-detail-label">Modalidad:</span>
                    <span className="thr-detail-value">{request.modalidad}</span>
                  </div>
                  <div className="thr-detail">
                    <span className="thr-detail-label">Duración:</span>
                    <span className="thr-detail-value">{request.duracion}</span>
                  </div>
                  <div className="thr-detail">
                    <span className="thr-detail-label">Tarifa acordada:</span>
                    <span className="thr-detail-value thr-detail-value--price">
                      {request.tarifaAcordada}€
                    </span>
                  </div>
                  <div className="thr-detail">
                    <span className="thr-detail-label">Fecha de solicitud:</span>
                    <span className="thr-detail-value">
                      {new Date(request.createdAt).toLocaleDateString("es-ES", {
                        year: "numeric",
                        month: "long",
                        day: "numeric",
                      })}
                    </span>
                  </div>
                </div>
              </div>

              {request.estado === "PENDIENTE_APROBACION" && (
                <div className="thr-card-actions">
                  {rejecting === request.id ? (
                    <div className="thr-reject-form">
                      <textarea
                        className="thr-reject-textarea"
                        placeholder="Motivo del rechazo..."
                        value={rejectMotivo}
                        onChange={(e) => setRejectMotivo(e.target.value)}
                        rows={3}
                      />
                      <div className="thr-reject-buttons">
                        <button
                          className="thr-btn thr-btn--secondary"
                          onClick={() => {
                            setRejecting(null);
                            setRejectMotivo("");
                          }}
                          disabled={processingId === request.id}
                        >
                          Cancelar
                        </button>
                        <button
                          className="thr-btn thr-btn--danger"
                          onClick={() => handleReject(request.id)}
                          disabled={processingId === request.id}
                        >
                          {processingId === request.id ? "Rechazando..." : "Confirmar Rechazo"}
                        </button>
                      </div>
                    </div>
                  ) : (
                    <>
                      <button
                        className="thr-btn thr-btn--success"
                        onClick={() => handleAccept(request.id)}
                        disabled={processingId === request.id}
                      >
                        {processingId === request.id ? "Aceptando..." : "✓ Aceptar"}
                      </button>
                      <button
                        className="thr-btn thr-btn--danger"
                        onClick={() => setRejecting(request.id)}
                        disabled={processingId === request.id}
                      >
                        ✕ Rechazar
                      </button>
                    </>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default TutorHiringRequests;

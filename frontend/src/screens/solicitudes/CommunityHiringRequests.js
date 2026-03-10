import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { communitiesApi } from "../../api/communities.api";
import { useAuth } from "../../contexts/AuthContext";
import "./CommunityHiringRequests.css";

/**
 * Pantalla para que los administradores de comunidad vean y gestionen
 * las solicitudes de contratación de sus comunidades
 */
const CommunityHiringRequests = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [communities, setCommunities] = useState([]);
  const [requests, setRequests] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [processingPayment, setProcessingPayment] = useState(null);

  const loadData = useCallback(async () => {
    if (!user) return;

    setLoading(true);
    setError("");
    
    try {
      // Obtener comunidades del usuario
      const communitiesResponse = await communitiesApi.listMine({ page: 0, size: 100 });
      const myCommunities = communitiesResponse?.content || [];
      
      // Filtrar solo las comunidades donde soy creador/admin
      const adminCommunities = myCommunities.filter(
        c => c.miRol === 'ADMIN' || c.miRol === 'ADMINISTRADOR' ||
             (c.creador?.id != null && c.creador.id === user.id)
      );
      
      setCommunities(adminCommunities);

      // Cargar solicitudes para cada comunidad
      const requestsMap = {};
      await Promise.all(
        adminCommunities.map(async (community) => {
          try {
            const response = await communitiesApi.getHiringRequests(community.id, { page: 0, size: 10 });
            requestsMap[community.id] = response?.content || [];
          } catch (err) {
            // Si no hay solicitudes o error, dejarlo vacío
            requestsMap[community.id] = [];
          }
        })
      );
      
      setRequests(requestsMap);
    } catch (err) {
      setError(err?.response?.data?.message || "Error al cargar solicitudes");
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handlePayment = async (communityId, tutorId, requestId) => {
    if (!window.confirm("¿Deseas proceder al pago para contratar este tutor?")) {
      return;
    }

    setProcessingPayment(requestId);
    setError("");
    
    try {
      const response = await communitiesApi.proceedToPayment(communityId, tutorId);
      
      if (response?.paymentUrl) {
        // Redirigir a Stripe
        window.location.href = response.paymentUrl;
      } else {
        alert("Se ha iniciado el proceso de pago");
        await loadData();
      }
    } catch (err) {
      const msg = err?.response?.data?.message || err?.message || "Error al procesar el pago";
      setError(msg);
    } finally {
      setProcessingPayment(null);
    }
  };

  if (loading) {
    return (
      <div className="chr-container">
        <h1 className="chr-title">Mis Solicitudes de Contratación</h1>
        <p className="chr-loading">Cargando solicitudes...</p>
      </div>
    );
  }

  // Filtrar comunidades que tienen al menos una solicitud
  const communitiesWithRequests = communities.filter(
    c => requests[c.id] && requests[c.id].length > 0
  );

  return (
    <div className="chr-container">
      <div className="chr-header">
        <h1 className="chr-title">Mis Solicitudes de Contratación</h1>
        <p className="chr-subtitle">
          Gestiona las solicitudes de tus comunidades
        </p>
      </div>

      {error && (
        <div className="chr-error">
          <span>⚠️</span> {error}
        </div>
      )}

      {communitiesWithRequests.length === 0 ? (
        <div className="chr-empty">
          <div className="chr-empty-icon">📭</div>
          <h2>No hay solicitudes pendientes</h2>
          <p>Las solicitudes de contratación que envíes aparecerán aquí</p>
          <button
            className="chr-btn chr-btn--primary"
            onClick={() => navigate("/profesores-verificados")}
          >
            Buscar Tutores
          </button>
        </div>
      ) : (
        <div className="chr-communities-list">
          {communitiesWithRequests.map((community) => (
            <div key={community.id} className="chr-community-section">
              <div className="chr-community-header">
                {community.foto && (
                  <img
                    src={community.foto}
                    alt={community.nombre}
                    className="chr-community-photo"
                  />
                )}
                <div>
                  <h2 className="chr-community-name">{community.nombre}</h2>
                  {community.descripcion && (
                    <p className="chr-community-desc">{community.descripcion}</p>
                  )}
                </div>
              </div>

              <div className="chr-requests-list">
                {requests[community.id].map((request) => (
                  <div key={request.id} className="chr-request-card">
                    <div className="chr-request-header">
                      <div className="chr-request-status">
                        <span className={`chr-status-badge chr-status--${request.estado?.toLowerCase()}`}>
                          {request.estado === "PENDIENTE_APROBACION" && "⏳ Esperando aprobación del tutor"}
                          {request.estado === "APROBADA" && "✓ Aprobada - Pendiente de pago"}
                          {request.estado === "PENDIENTE_PAGO" && "💳 Pago en proceso"}
                          {request.estado === "ACTIVA" && "✓ Activa"}
                          {request.estado === "RECHAZADA" && "✕ Rechazada"}
                          {request.estado === "CANCELADA" && "✕ Cancelada"}
                          {request.estado === "COMPLETADA" && "✓ Completada"}
                        </span>
                      </div>
                    </div>

                    <div className="chr-request-body">
                      <div className="chr-request-details">
                        <div className="chr-detail-row">
                          <span className="chr-detail-label">Modalidad:</span>
                          <span className="chr-detail-value">{request.modalidad}</span>
                        </div>
                        <div className="chr-detail-row">
                          <span className="chr-detail-label">Duración:</span>
                          <span className="chr-detail-value">{request.duracion}</span>
                        </div>
                        <div className="chr-detail-row">
                          <span className="chr-detail-label">Tarifa acordada:</span>
                          <span className="chr-detail-value chr-detail-value--price">
                            {request.tarifaAcordada}€
                          </span>
                        </div>
                        {request.fechaInicio && (
                          <div className="chr-detail-row">
                            <span className="chr-detail-label">Fecha de inicio:</span>
                            <span className="chr-detail-value">
                              {new Date(request.fechaInicio).toLocaleDateString("es-ES")}
                            </span>
                          </div>
                        )}
                      </div>

                      {request.estado === "APROBADA" && (
                        <div className="chr-request-action">
                          <p className="chr-action-message">
                            ¡El tutor ha aceptado tu solicitud! Procede al pago para activar la contratación.
                          </p>
                          <button
                            className="chr-btn chr-btn--success"
                            onClick={() => handlePayment(community.id, request.tutor?.id, request.id)}
                            disabled={processingPayment === request.id}
                          >
                            {processingPayment === request.id ? "Procesando..." : "💳 Proceder al Pago"}
                          </button>
                        </div>
                      )}

                      {request.estado === "RECHAZADA" && request.motivoCancelacion && (
                        <div className="chr-reject-info">
                          <strong>Motivo del rechazo:</strong>
                          <p>{request.motivoCancelacion}</p>
                        </div>
                      )}

                      {request.estado === "PENDIENTE_APROBACION" && (
                        <div className="chr-pending-message">
                          <p>⏳ Esperando que el tutor responda a tu solicitud...</p>
                        </div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default CommunityHiringRequests;

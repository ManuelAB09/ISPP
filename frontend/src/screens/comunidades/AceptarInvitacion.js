import { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { communitiesApi } from '../../api/communities.api';
import { useAuth } from '../../contexts/AuthContext';
import studyShareLogo from '../../static/images/MeerKatters_logo.png';
import {
    clearPendingInvitation,
    getPendingInvitation,
    savePendingInvitation,
} from '../../utils/invitationFlow';
import './AceptarInvitacion.css';

const MAX_DISCOVERY_PAGES = 5;
const DISCOVERY_PAGE_SIZE = 30;

const normalizeCommunityId = (value) => {
  if (!value) return null;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
};

const buildAuthRedirect = (pathname, search) => {
  const next = `${pathname || ''}${search || ''}`;
  return encodeURIComponent(next || '/');
};

const discoverCommunityIdByCode = async (code) => {
  let page = 0;
  let totalPages = 1;

  while (page < totalPages && page < MAX_DISCOVERY_PAGES) {
    const response = await communitiesApi.list({ page, size: DISCOVERY_PAGE_SIZE });
    const communities = response?.content || [];
    totalPages = response?.page?.totalPages ?? totalPages;

    if (communities.length > 0) {
      const checks = await Promise.allSettled(
        communities.map(async (community) => {
          await communitiesApi.getInvitationByCode(community.id, code);
          return community.id;
        })
      );

      const found = checks.find((result) => result.status === 'fulfilled');
      if (found) {
        return found.value;
      }
    }

    page += 1;
  }

  return null;
};

const AceptarInvitacion = () => {
  const { codigo } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthenticated, loading } = useAuth();

  const [status, setStatus] = useState('checking');
  const [message, setMessage] = useState('Estamos procesando tu invitación.');

  const queryCommunityId = useMemo(() => {
    const searchParams = new URLSearchParams(location.search);
    return normalizeCommunityId(searchParams.get('communityId'));
  }, [location.search]);

  useEffect(() => {
    let active = true;

    const run = async () => {
      if (!codigo) {
        if (!active) return;
        setStatus('error');
        setMessage('No se ha encontrado un código de invitación válido.');
        return;
      }

      if (loading) {
        return;
      }

      if (!isAuthenticated) {
        savePendingInvitation({ code: codigo, communityId: queryCommunityId });
        if (!active) return;
        setStatus('auth-required');
        setMessage('Necesitas iniciar sesión o registrarte para aceptar la invitación.');
        return;
      }

      try {
        if (!active) return;
        setStatus('accepting');
        setMessage('Aceptando invitación...');

        const pendingInvitation = getPendingInvitation();
        let communityId =
          queryCommunityId ||
          (pendingInvitation?.code === codigo ? pendingInvitation.communityId : null);

        if (!communityId) {
          communityId = await discoverCommunityIdByCode(codigo);
        }

        if (!communityId) {
          throw new Error(
            'No hemos podido localizar la comunidad de esta invitación. Pide un nuevo enlace al administrador.'
          );
        }

        await communitiesApi.acceptInvitationByCode(communityId, codigo);

        if (pendingInvitation?.code === codigo) {
          clearPendingInvitation();
        }

        if (!active) return;
        setStatus('success');
        setMessage('Invitación aceptada correctamente. Redirigiendo a la comunidad...');

        setTimeout(() => {
          navigate(`/comunidades/${communityId}`);
        }, 1500);
      } catch (err) {
        if (!active) return;
        setStatus('error');
        setMessage(err?.message || 'No se pudo aceptar la invitación.');
      }
    };

    run();

    return () => {
      active = false;
    };
  }, [codigo, isAuthenticated, loading, navigate, queryCommunityId]);

  const authRedirect = buildAuthRedirect(location.pathname, location.search);
  const statusIcon =
    status === 'success'
      ? '✓'
      : status === 'error'
        ? '✗'
        : status === 'auth-required'
          ? '👤'
          : '⏳';
  const statusTitle =
    status === 'success'
      ? 'Invitación aceptada'
      : status === 'error'
        ? 'No se pudo completar'
        : status === 'auth-required'
          ? 'Continúa para unirte'
          : 'Procesando invitación';

  return (
    <div className="accept-invitation-layout">
      <aside className="accept-invitation-brand-panel">
        <div className="accept-invitation-brand-content">
          <Link to="/" className="accept-invitation-logo-wrapper" aria-label="Ir al inicio">
            <img src={studyShareLogo} alt="MeerKatters Logo" className="accept-invitation-logo" />
          </Link>
          <h1 className="accept-invitation-brand-title">MeerKatters</h1>
          <p className="accept-invitation-brand-description">
            Únete a tu comunidad para aprender, compartir recursos y colaborar con otros estudiantes.
          </p>
          <div className="accept-invitation-brand-pills" aria-hidden="true">
            <span>Comunidades activas</span>
            <span>Aprendizaje colaborativo</span>
          </div>
        </div>
      </aside>

      <main className="accept-invitation-content-panel">
        <section className={`accept-invitation-card status-${status}`}>
          <div className="accept-invitation-status-icon" aria-hidden="true">{statusIcon}</div>
          <h2>{statusTitle}</h2>
          <p>{message}</p>

          {status === 'auth-required' && (
            <div className="accept-invitation-actions">
              <Link to={`/login?next=${authRedirect}`} className="accept-invitation-btn primary">
                Iniciar sesión
              </Link>
              <Link
                to={`/register?next=${authRedirect}`}
                className="accept-invitation-btn secondary"
              >
                Crear cuenta
              </Link>
            </div>
          )}

          {status === 'success' && (
            <div className="accept-invitation-actions">
              <button
                type="button"
                className="accept-invitation-btn primary"
                onClick={() => navigate('/comunidades')}
              >
                Ver comunidades
              </button>
            </div>
          )}

          {status === 'error' && (
            <div className="accept-invitation-actions">
              <Link to="/comunidades" className="accept-invitation-btn secondary">
                Volver a comunidades
              </Link>
            </div>
          )}
        </section>
      </main>
    </div>
  );
};

export default AceptarInvitacion;

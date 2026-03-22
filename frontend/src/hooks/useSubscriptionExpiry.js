import { useState, useEffect } from 'react';
import { subscriptionsApi } from '../api/subscriptions.api';

const DISMISS_KEY = 'plan_expiry_banner_dismissed';
const DAYS_THRESHOLD = 7;

export function useSubscriptionExpiry() {
  const [showBanner, setShowBanner] = useState(false);
  const [planName, setPlanName] = useState('');
  const [fechaFin, setFechaFin] = useState('');

  useEffect(() => {
    const isAuthenticated = Boolean(localStorage.getItem('accessToken'));
    if (!isAuthenticated) return;

    const dismissed = sessionStorage.getItem(DISMISS_KEY);
    if (dismissed === 'true') return;

    subscriptionsApi.getMySubscription()
      .then((sub) => {
        if (!sub || !sub.fechaFin || sub.estado !== 'ACTIVA') return;

        const end = new Date(sub.fechaFin);
        const now = new Date();
        const diffMs = end.getTime() - now.getTime();
        const diffDays = diffMs / (1000 * 60 * 60 * 24);

        if (diffDays > 0 && diffDays <= DAYS_THRESHOLD) {
          setShowBanner(true);
          setPlanName(sub.plan || 'Premium');
          setFechaFin(sub.fechaFin);
        }
      })
      .catch(() => {});
  }, []);

  const dismiss = () => {
    setShowBanner(false);
    sessionStorage.setItem(DISMISS_KEY, 'true');
  };

  return { showBanner, planName, fechaFin, dismiss };
}

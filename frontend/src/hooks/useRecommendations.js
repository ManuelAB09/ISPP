import { useCallback, useEffect, useRef, useState } from 'react';
import {
  getRecommendationsPage,
  enviarFeedback,
  descartarRecomendacion,
  marcarVista,
  registrarActividad,
  forzarRefresh,
} from '../api/recommendations.api';

/**
 * Hook principal para el motor de recomendaciones.
 *
 * Retorna:
 *   data        – objeto con paraTi, profesores, contenidos, cuestionarios, comunidades
 *   loading     – boolean
 *   error       – string|null
 *   refresh     – () => void   fuerza recarga desde el backend
 *   submitFeedback(id, esUtil) – optimistic update + POST feedback
 *   discard(id)               – elimina la recomendación localmente y en backend
 *   trackActivity(payload)    – fire-and-forget POST /actividad
 *   markSeen(id)              – POST /vista (llamar al renderizar una tarjeta)
 */
export function useRecommendations() {
  const [data, setData] = useState({
    paraTi: [],
    profesores: [],
    contenidos: [],
    cuestionarios: [],
    comunidades: [],
    generadoEn: null,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Evita doble petición en StrictMode
  const fetchedRef = useRef(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await getRecommendationsPage();
      setData({
        paraTi:        result?.paraTi        ?? [],
        profesores:    result?.profesores    ?? [],
        contenidos:    result?.contenidos    ?? [],
        cuestionarios: result?.cuestionarios ?? [],
        comunidades:   result?.comunidades   ?? [],
        generadoEn:    result?.generadoEn    ?? null,
      });
    } catch (err) {
      setError(err?.message || 'Error al cargar recomendaciones');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (fetchedRef.current) return;
    fetchedRef.current = true;
    load();
  }, [load]);

  /** Fuerza regeneración en backend y recarga la UI. */
  const refresh = useCallback(async () => {
    try { await forzarRefresh(); } catch { /* continuar igual */ }
    fetchedRef.current = false;
    await load();
  }, [load]);

  /**
   * Actualización optimista: cambia esFavorable localmente,
   * luego envía al backend. Si falla, revierte.
   */
  const submitFeedback = useCallback(async (id, esUtil) => {
    const applyUpdate = (prev) => {
      const update = (list) =>
        list.map((r) => (r.id === id ? { ...r, esFavorable: esUtil } : r));
      return {
        ...prev,
        paraTi:        update(prev.paraTi),
        profesores:    update(prev.profesores),
        contenidos:    update(prev.contenidos),
        cuestionarios: update(prev.cuestionarios),
        comunidades:   update(prev.comunidades),
      };
    };

    setData(applyUpdate);

    try {
      await enviarFeedback(id, { esUtil });
    } catch {
      // Revertir en caso de fallo
      setData((prev) => {
        const revert = (list) =>
          list.map((r) => (r.id === id ? { ...r, esFavorable: null } : r));
        return {
          ...prev,
          paraTi:        revert(prev.paraTi),
          profesores:    revert(prev.profesores),
          contenidos:    revert(prev.contenidos),
          cuestionarios: revert(prev.cuestionarios),
          comunidades:   revert(prev.comunidades),
        };
      });
    }
  }, []);

  /** Elimina la recomendación localmente y en backend. */
  const discard = useCallback(async (id) => {
    const remove = (list) => list.filter((r) => r.id !== id);
    setData((prev) => ({
      ...prev,
      paraTi:        remove(prev.paraTi),
      profesores:    remove(prev.profesores),
      contenidos:    remove(prev.contenidos),
      cuestionarios: remove(prev.cuestionarios),
      comunidades:   remove(prev.comunidades),
    }));
    try { await descartarRecomendacion(id); } catch { /* silencioso */ }
  }, []);

  /** Fire-and-forget: registra actividad del usuario. */
  const trackActivity = useCallback((payload) => {
    registrarActividad(payload).catch(() => { /* silencioso */ });
  }, []);

  /** Marca vista cuando la tarjeta se renderiza. */
  const markSeen = useCallback((id) => {
    marcarVista(id).catch(() => { /* silencioso */ });
  }, []);

  return { data, loading, error, refresh, submitFeedback, discard, trackActivity, markSeen };
}

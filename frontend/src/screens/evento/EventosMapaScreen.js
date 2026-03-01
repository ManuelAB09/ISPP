import React, { useEffect, useMemo, useState } from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import { listMapEvents } from '../../api/eventEndpoints';
import { useNavigate } from 'react-router-dom';
import Header from '../../components/Header/Header';

const defaultPosition = [37.3891, -5.9845];

const eventIconRed = L.icon({
  iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-red.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowUrl: 'https://unpkg.com/leaflet@1.7.1/dist/images/marker-shadow.png',
  shadowSize: [41, 41],
});

const normalizeText = (value = '') =>
  value
    .toString()
    .trim()
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '');

const toLabel = (value = '') =>
  value ? value.charAt(0).toUpperCase() + value.slice(1) : '';

const getCityFromAddress = (address = {}) => {
  const cityCandidate =
    address.city ||
    address.town ||
    address.village ||
    address.municipality ||
    address.city_district ||
    '';

  const normalized = normalizeText(cityCandidate);

  const blocked = new Set(['espana', 'españa', 'spain', 'andalucia', 'andalucía']);
  if (!normalized || blocked.has(normalized)) return '';
  return normalized;
};

const getCoordsKey = (lat, lon) => `${Number(lat).toFixed(6)},${Number(lon).toFixed(6)}`;

// 👇 Componente que recentra el mapa según eventos visibles
function AutoFitMapToEvents({ eventos, defaultCenter, defaultZoom = 13 }) {
  const map = useMap();

  useEffect(() => {
    const points = eventos
      .map((ev) => {
        const lat = ev?.ubicacion?.latitud;
        const lon = ev?.ubicacion?.longitud;
        if (lat == null || lon == null) return null;
        return [lat, lon];
      })
      .filter(Boolean);

    if (!points.length) {
      map.setView(defaultCenter, defaultZoom, { animate: true });
      return;
    }

    if (points.length === 1) {
      map.setView(points[0], 14, { animate: true });
      return;
    }

    const bounds = L.latLngBounds(points);
    map.fitBounds(bounds, { padding: [40, 40], animate: true, maxZoom: 14 });
  }, [eventos, map, defaultCenter, defaultZoom]);

  return null;
}

const EventosMapaScreen = () => {
  const [eventos, setEventos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [cityLoading, setCityLoading] = useState(false);
  const [error, setError] = useState(null);

  const [selectedCity, setSelectedCity] = useState('all');
  const [cityByCoords, setCityByCoords] = useState({});

  const navigate = useNavigate();

  useEffect(() => {
    const fetchEventos = async () => {
      setLoading(true);
      setError(null);
      try {
        const res = await listMapEvents();
        setEventos(Array.isArray(res) ? res : []);
      } catch (err) {
        console.error(err);
        setError('Error cargando eventos');
      } finally {
        setLoading(false);
      }
    };
    fetchEventos();
  }, []);

  const inferCityFromEvent = (ev) => {
    const ubic = ev?.ubicacion;
    if (!ubic) return '';
    if (ubic.ciudad) return normalizeText(ubic.ciudad);
    if (ubic.address && typeof ubic.address === 'object') return getCityFromAddress(ubic.address);
    return '';
  };

  useEffect(() => {
    const resolveCities = async () => {
      if (!eventos.length) {
        setCityByCoords({});
        return;
      }

      const byCoord = new Map();

      eventos.forEach((ev) => {
        const lat = ev?.ubicacion?.latitud;
        const lon = ev?.ubicacion?.longitud;
        if (lat == null || lon == null) return;

        const key = getCoordsKey(lat, lon);
        const cityFromEvent = inferCityFromEvent(ev);

        if (cityFromEvent) byCoord.set(key, cityFromEvent);
        else if (!byCoord.has(key)) byCoord.set(key, null);
      });

      const pending = [...byCoord.entries()].filter(([, city]) => city === null);

      if (!pending.length) {
        setCityByCoords(Object.fromEntries(byCoord.entries()));
        return;
      }

      setCityLoading(true);

      const resolved = Object.fromEntries([...byCoord.entries()].filter(([, city]) => city !== null));

      for (const [key] of pending) {
        const [lat, lon] = key.split(',');
        try {
          const resp = await fetch(
            `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${lat}&lon=${lon}&accept-language=es`
          );
          const data = await resp.json();
          resolved[key] = getCityFromAddress(data?.address || {});
        } catch {
          resolved[key] = '';
        }
      }

      setCityByCoords(resolved);
      setCityLoading(false);
    };

    resolveCities();
  }, [eventos]);

  const cities = useMemo(() => {
    const set = new Set();

    eventos.forEach((ev) => {
      const lat = ev?.ubicacion?.latitud;
      const lon = ev?.ubicacion?.longitud;
      if (lat == null || lon == null) return;

      const key = getCoordsKey(lat, lon);
      const city = cityByCoords[key];
      if (city) set.add(city);
    });

    return [...set].sort((a, b) => a.localeCompare(b, 'es'));
  }, [eventos, cityByCoords]);

  const eventosFiltrados = useMemo(() => {
    if (selectedCity === 'all') return eventos;

    return eventos.filter((ev) => {
      const lat = ev?.ubicacion?.latitud;
      const lon = ev?.ubicacion?.longitud;
      if (lat == null || lon == null) return false;

      const key = getCoordsKey(lat, lon);
      return cityByCoords[key] === selectedCity;
    });
  }, [eventos, cityByCoords, selectedCity]);

  const ubicacionEventos = {};
  eventosFiltrados.forEach((ev) => {
    const lat = ev?.ubicacion?.latitud;
    const lon = ev?.ubicacion?.longitud;
    if (lat != null && lon != null) {
      const key = getCoordsKey(lat, lon);
      if (!ubicacionEventos[key]) ubicacionEventos[key] = [];
      ubicacionEventos[key].push(ev);
    }
  });

  return (
    <>
      <Header page={'eventos-mapa'} />
      <div style={{ padding: 24, maxWidth: 900, margin: '0 auto' }}>
        <h2 style={{ color: '#1a237e', fontWeight: 700, fontSize: 26, marginBottom: 18 }}>
          Eventos en el mapa
        </h2>

        <div style={{ marginBottom: 14, display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
          <label htmlFor="cityFilter" style={{ fontWeight: 600, color: '#2E3559' }}>
            Filtrar por ciudad:
          </label>
          <select
            id="cityFilter"
            value={selectedCity}
            onChange={(e) => setSelectedCity(e.target.value)}
            style={{
              minWidth: 220,
              padding: '8px 10px',
              borderRadius: 8,
              border: '1px solid #d1d5db',
              background: 'white'
            }}
          >
            <option value="all">Todas las ciudades</option>
            {cities.map((city) => (
              <option key={city} value={city}>
                {toLabel(city)}
              </option>
            ))}
          </select>

          {cityLoading && <span style={{ fontSize: 13, color: '#666' }}>Resolviendo ciudades…</span>}
        </div>

        {loading && <div>Cargando eventos...</div>}
        {error && <div style={{ color: 'red' }}>{error}</div>}

        <div
          style={{
            position: 'relative',
            height: 500,
            width: '100%',
            borderRadius: 10,
            overflow: 'hidden',
            border: '1px solid #e0e0e0',
            boxShadow: '0 2px 8px #0001'
          }}
        >
          <MapContainer center={defaultPosition} zoom={13} style={{ height: '100%', width: '100%' }}>
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />

            {/* 👇 Recentrado automático cuando cambias ciudad */}
            <AutoFitMapToEvents eventos={eventosFiltrados} defaultCenter={defaultPosition} defaultZoom={13} />

            {eventosFiltrados.map((ev) => {
              const lat = ev?.ubicacion?.latitud;
              const lon = ev?.ubicacion?.longitud;
              if (lat == null || lon == null) return null;

              const key = getCoordsKey(lat, lon);
              let offsetLat = 0;
              let offsetLon = 0;

              if (ubicacionEventos[key]?.length > 1) {
                const idx = ubicacionEventos[key].findIndex((e) => e.id === ev.id);
                const angle = (idx / ubicacionEventos[key].length) * 2 * Math.PI;
                offsetLat = Math.sin(angle) * 0.00008;
                offsetLon = Math.cos(angle) * 0.00008;
              }

              const markerLat = lat + offsetLat;
              const markerLon = lon + offsetLon;

              return (
                <Marker key={ev.id} position={[markerLat, markerLon]} icon={eventIconRed}>
                  <Popup>
                    <div style={{ minWidth: 180 }}>
                      <div style={{ fontWeight: 700, color: '#b71c1c', fontSize: 17 }}>
                        {ev.titulo}
                      </div>
                      <div style={{ fontSize: 14, color: '#333', marginBottom: 4 }}>
                        {ev.ubicacion?.nombre || 'Ubicación'}
                      </div>
                      <div style={{ fontSize: 13, color: '#555', marginBottom: 6 }}>
                        {ev.descripcion}
                      </div>
                      <button className="btn btn-primary" onClick={() => navigate(`/eventos/${ev.id}`)}>
                        Ver evento
                      </button>
                    </div>
                  </Popup>
                </Marker>
              );
            })}
          </MapContainer>
        </div>

        {eventosFiltrados.length === 0 && !loading && (
          <div style={{ color: '#888', marginTop: 18, textAlign: 'center' }}>
            No hay eventos para la ciudad seleccionada.
          </div>
        )}
      </div>
    </>
  );
};

export default EventosMapaScreen;
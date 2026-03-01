import React, { useEffect, useState } from 'react';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
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

const EventosMapaScreen = () => {
    const [eventos, setEventos] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const navigate = useNavigate();

    useEffect(() => {
        const fetchEventos = async () => {
            setLoading(true);
            setError(null);
            try {
                const res = await listMapEvents();
                setEventos(res || []);
            } catch (err) {
                setError('Error cargando eventos');
            } finally {
                setLoading(false);
            }
        };
        fetchEventos();
    }, []);

    const ubicacionEventos = {};
    eventos.forEach(ev => {
        if (ev.ubicacion && ev.ubicacion.latitud != null && ev.ubicacion.longitud != null) {
            const key = `${ev.ubicacion.latitud.toFixed(6)},${ev.ubicacion.longitud.toFixed(6)}`;
            if (!ubicacionEventos[key]) ubicacionEventos[key] = [];
            ubicacionEventos[key].push(ev);
        }
    });

    return (
        <>
        <Header page={'eventos-mapa'}/>
        <div style={{ padding: 24, maxWidth: 900, margin: '0 auto' }}>
            <h2 style={{ color: '#1a237e', fontWeight: 700, fontSize: 26, marginBottom: 18 }}>Eventos en el mapa</h2>
            {loading && <div>Cargando eventos...</div>}
            {error && <div style={{ color: 'red' }}>{error}</div>}
            <div style={{ position: 'relative', height: 500, width: '100%', borderRadius: 10, overflow: 'hidden', border: '1px solid #e0e0e0', boxShadow: '0 2px 8px #0001' }}>
                <MapContainer center={defaultPosition} zoom={13} style={{ height: '100%', width: '100%' }}>
                    <TileLayer
                        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                    />
                    {eventos.map(ev => {
                        const icon = eventIconRed;
                        const key = ev.ubicacion && `${ev.ubicacion.latitud?.toFixed(6)},${ev.ubicacion.longitud?.toFixed(6)}`;
                        let offsetLat = 0, offsetLon = 0;
                        if (key && ubicacionEventos[key].length > 1) {
                            const idx = ubicacionEventos[key].findIndex(e => e.id === ev.id);
                            const angle = (idx / ubicacionEventos[key].length) * 2 * Math.PI;
                            offsetLat = Math.sin(angle) * 0.00008;
                            offsetLon = Math.cos(angle) * 0.00008;
                        }
                        const lat = ev.ubicacion.latitud + offsetLat;
                        const lon = ev.ubicacion.longitud + offsetLon;
                        return (
                            <Marker key={ev.id} position={[lat, lon]} icon={icon}>
                                <Popup>
                                    <div style={{ minWidth: 180 }}>
                                        <div style={{ fontWeight: 700, color: '#b71c1c', fontSize: 17 }}>{ev.titulo}</div>
                                        <div style={{ fontSize: 14, color: '#333', marginBottom: 4 }}>{ev.ubicacion.nombre}</div>
                                        <div style={{ fontSize: 13, color: '#555', marginBottom: 6 }}>{ev.descripcion}</div>
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
            {eventos.length === 0 && !loading && (
                <div style={{ color: '#888', marginTop: 18, textAlign: 'center' }}>No hay eventos con ubicación visible en el mapa.</div>
            )}
        </div>
        </>
    );
};

export default EventosMapaScreen;

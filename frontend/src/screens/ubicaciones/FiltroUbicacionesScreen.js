import React, { useState, useRef, useEffect } from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMapEvents, Circle } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import { ubicacionesApi } from '../../api/ubicaciones.api';

const defaultPosition = [37.3891, -5.9845];

const icon = L.icon({
    iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-green.png',
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
    shadowUrl: 'https://unpkg.com/leaflet@1.7.1/dist/images/marker-shadow.png',
    shadowSize: [41, 41],
});

const iconReferencia = L.divIcon({
    className: '',
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
    html: `<svg width="25" height="41" viewBox="0 0 20 32" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M10 0C4.477 0 0 4.477 0 10c0 7.5 10 22 10 22s10-14.5 10-22C20 4.477 15.523 0 10 0zm0 14.5A4.5 4.5 0 1 1 10 5.5a4.5 4.5 0 0 1 0 9z" fill="#6B73A1"/>
            <circle cx="10" cy="10" r="4.5" fill="#fff"/>
        </svg>`
});

const FiltroUbicacionesScreen = ({ onSeleccionar, onClose }) => {
    const [lat, setLat] = useState(defaultPosition[0]);
    const [lon, setLon] = useState(defaultPosition[1]);
    const modalRef = useRef(null);

    useEffect(() => {
        function handleClickOutside(event) {
            if (modalRef.current && !modalRef.current.contains(event.target)) {
                if (onClose) onClose();
            }
        }
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, [onClose]);
    const [radio, setRadio] = useState(1000);
    const tiposDisponibles = [
        '',
        'library',
        'community_centre',
        'training',
        'university',
        'hackerspace',
        'coworking_space',
        'studio',
        'park',
        'playground',
    ];
    const [tipo, setTipo] = useState('');
    const costesDisponibles = [
        '',
        'GRATIS',
        'DE_PAGO',
        'PROBABLEMENTE_GRATIS',
        'PROBABLEMENTE_DE_PAGO',
        'PARCIALMENTE_GRATIS',
        'DESCONOCIDO',
    ];
    const [coste, setCoste] = useState('');
    const [resultados, setResultados] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [seleccionPrevia, setSeleccionPrevia] = useState(null);
    const [noResultados, setNoResultados] = useState(false);

    // Mapeo de tipos a español
    const tiposTraduccion = {
        '': 'Todos los tipos',
        'library': 'Biblioteca',
        'community_centre': 'Centro comunitario',
        'training': 'Formación',
        'university': 'Universidad',
        'hackerspace': 'Espacio de hackers',
        'coworking_space': 'Espacio de coworking',
        'studio': 'Estudio',
        'park': 'Parque',
        'playground': 'Área de juegos',
    };

    // Mapeo de costes a español
    const costesTraduccion = {
        '': 'Todos los costes',
        'GRATIS': 'Gratis',
        'DE_PAGO': 'De pago',
        'PROBABLEMENTE_GRATIS': 'Probablemente gratis',
        'PROBABLEMENTE_DE_PAGO': 'Probablemente de pago',
        'PARCIALMENTE_GRATIS': 'Parcialmente gratis',
        'DESCONOCIDO': 'Desconocido',
    };

    const traducirTipo = (tipo) => tiposTraduccion[tipo] || tipo;
    const traducirCoste = (coste) => costesTraduccion[coste] || coste;

    function ReferenceMarker() {
        useMapEvents({
            click: (e) => {
                setLat(e.latlng.lat);
                setLon(e.latlng.lng);
            },
        });
        return <Marker position={[lat, lon]} icon={iconReferencia}><Popup>Referencia</Popup></Marker>;
    }

    const handleBuscar = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError(null);
        setNoResultados(false);
        setSeleccionPrevia(null);
        try {
            const res = await ubicacionesApi.buscarEstudio({ lat, lon, radio });
            let filtrados = res;
            if (tipo && tipo.trim() !== '') {
                filtrados = filtrados.filter(u => (u.tipo || '').toLowerCase() === tipo.trim().toLowerCase());
            }
            if (coste && coste.trim() !== '') {
                filtrados = filtrados.filter(u => (u.coste || '').toUpperCase() === coste.trim().toUpperCase());
            }
            if (filtrados.length === 0) {
                setNoResultados(true);
                setResultados([]);
            } else {
                const geocodeAll = await Promise.all(filtrados.map(async u => {
                    if (!u.direccion || u.direccion === 'Dirección no disponible') {
                        try {
                            const response = await fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${u.latitud}&lon=${u.longitud}`);
                            const data = await response.json();
                            return { ...u, direccion: data && data.display_name ? data.display_name : '' };
                        } catch {
                            return { ...u, direccion: '' };
                        }
                    } else {
                        return u;
                    }
                }));
                setResultados(geocodeAll);
            }
        } catch (err) {
            setError('Error buscando ubicaciones');
        } finally {
            setLoading(false);
        }
    };

    const handleSeleccionar = (u) => {
        setSeleccionPrevia(u);
    };

    const handleConfirmar = () => {
        if (seleccionPrevia && onSeleccionar) {
            onSeleccionar(seleccionPrevia);
            setSeleccionPrevia(null);
        }
    };

    return (
        <div style={{
            position: 'fixed',
            top: 0, left: 0, right: 0, bottom: 0,
            background: 'rgba(30,40,60,0.25)',
            zIndex: 1000,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
            <div ref={modalRef} style={{
                maxWidth: 700,
                width: '95%',
                background: '#fff',
                borderRadius: 12,
                padding: 32,
                boxShadow: '0 8px 32px #0003',
                fontFamily: 'inherit',
                position: 'relative',
                maxHeight: '90vh',
                overflowY: 'auto',
            }}>
                <button onClick={onClose} style={{ position: 'absolute', top: 18, right: 18, background: 'none', border: 'none', fontSize: 26, color: '#888', cursor: 'pointer', zIndex: 2 }} title="Cerrar">×</button>
                <h2 style={{ marginBottom: 20, color: '#1a237e', fontWeight: 700, fontSize: 26 }}>Buscar ubicaciones disponibles</h2>
                <form onSubmit={handleBuscar} style={{ marginBottom: 20, display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, alignItems: 'end' }}>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                        <label style={{ fontWeight: 500 }}>Latitud</label>
                        <input type="number" step="any" value={lat} onChange={e => setLat(Number(e.target.value))} placeholder="Latitud" required className="input-box input-large" />
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                        <label style={{ fontWeight: 500 }}>Longitud</label>
                        <input type="number" step="any" value={lon} onChange={e => setLon(Number(e.target.value))} placeholder="Longitud" required className="input-box input-large" />
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                        <label style={{ fontWeight: 500 }}>Radio (m)</label>
                        <input type="number" value={radio} onChange={e => setRadio(Number(e.target.value))} placeholder="Radio (m)" required className="input-box input-large" />
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                        <label style={{ fontWeight: 500 }}>Tipo</label>
                        <select value={tipo} onChange={e => setTipo(e.target.value)} className="input-box input-large">
                            {tiposDisponibles.map(t => (
                                <option key={t} value={t}>{traducirTipo(t)}</option>
                            ))}
                        </select>
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                        <label style={{ fontWeight: 500 }}>Coste</label>
                        <select value={coste} onChange={e => setCoste(e.target.value)} className="input-box input-large">
                            {costesDisponibles.map(c => (
                                <option key={c} value={c}>{traducirCoste(c)}</option>
                            ))}
                        </select>
                    </div>
                    <button type="submit" disabled={loading} className="btn btn-primary" style={{ height: 40, marginTop: 20 }}>{loading ? 'Buscando...' : 'Buscar'}</button>
                </form>
                {error && <div style={{ color: 'red', marginBottom: 8 }}>{error}</div>}
                {noResultados && <div style={{ color: '#ff9800', marginBottom: 8, fontWeight: 500 }}>No hay ubicaciones disponibles con estos filtros</div>}
                <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 12 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                        {/* Icono SVG personalizado para referencia en azul #6B73A1 */}
                        <svg width="20" height="32" viewBox="0 0 20 32" fill="none" xmlns="http://www.w3.org/2000/svg" style={{ display: 'block' }}>
                            <path d="M10 0C4.477 0 0 4.477 0 10c0 7.5 10 22 10 22s10-14.5 10-22C20 4.477 15.523 0 10 0zm0 14.5A4.5 4.5 0 1 1 10 5.5a4.5 4.5 0 0 1 0 9z" fill="#6B73A1" />
                            <circle cx="10" cy="10" r="4.5" fill="#fff" />
                        </svg>
                        <span style={{ fontSize: 13, color: '#6B73A1', fontWeight: 700 }}>Referencia</span>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                        <img src="https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-green.png" alt="Resultado" style={{ width: 20, height: 32 }} />
                        <span style={{ fontSize: 13, color: '#388e3c' }}>Ubicación encontrada</span>
                    </div>
                </div>
                {seleccionPrevia && (
                    <div style={{ background: '#e3f2fd', border: '1px solid #90caf9', borderRadius: 8, padding: 18, marginBottom: 18, boxShadow: '0 2px 8px #2196f322' }}>
                        <div style={{ fontWeight: 600, color: '#1976d2', marginBottom: 4 }}>Ubicación seleccionada:</div>
                        <div style={{ fontSize: 17, fontWeight: 600 }}>{seleccionPrevia.nombre}</div>
                        <div style={{ fontSize: 15, color: '#333', marginBottom: 2 }}>{seleccionPrevia.direccion}</div>
                        <div style={{ fontSize: 14, color: '#555' }}>Tipo: <b>{traducirTipo(seleccionPrevia.tipo)}</b> | Coste: <b>{traducirCoste(seleccionPrevia.coste)}</b></div>
                        <div style={{ marginTop: 10 }}>
                            <button className="btn btn-primary" onClick={handleConfirmar}>Usar esta ubicación</button>
                            <button className="btn btn-outline" style={{ marginLeft: 10 }} onClick={() => setSeleccionPrevia(null)}>Cancelar</button>
                        </div>
                    </div>
                )}
                <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 18,
                    margin: '0 0 12px 0',
                    justifyContent: 'center',
                    background: '#f5faff',
                    border: '1px solid #e3eafc',
                    borderRadius: 8,
                    padding: '10px 18px',
                    boxShadow: '0 2px 8px #1976d211',
                    fontFamily: 'inherit',
                }}>
                    <span style={{ fontSize: 15, color: '#6B73A1', fontWeight: 700, minWidth: 60, textAlign: 'right', letterSpacing: 0.5 }}>Radio</span>
                    <input
                        type="range"
                        min={100}
                        max={5000}
                        step={50}
                        value={radio}
                        onChange={e => setRadio(Number(e.target.value))}
                        style={{
                            width: 320,
                            height: 6,
                            borderRadius: 4,
                            accentColor: '#6B73A1',
                            background: 'linear-gradient(90deg, #6B73A1 0%, #a7a9c9 100%)',
                            boxShadow: '0 1px 4px #6B73A133',
                            outline: 'none',
                        }}
                    />
                    <style>{`
                        input[type=range]::-webkit-slider-thumb {
                            background: #6B73A1;
                            border: 2px solid #fff;
                            box-shadow: 0 2px 8px #6B73A144;
                        }
                        input[type=range]::-moz-range-thumb {
                            background: #6B73A1;
                            border: 2px solid #fff;
                            box-shadow: 0 2px 8px #6B73A144;
                        }
                        input[type=range]::-ms-thumb {
                            background: #6B73A1;
                            border: 2px solid #fff;
                            box-shadow: 0 2px 8px #6B73A144;
                        }
                    `}</style>
                    <span style={{
                        background: '#6B73A1',
                        color: '#fff',
                        borderRadius: 8,
                        padding: '4px 16px',
                        fontSize: 15,
                        fontWeight: 700,
                        boxShadow: '0 2px 8px #6B73A144',
                        minWidth: 70,
                        textAlign: 'center',
                        letterSpacing: 0.5
                    }}>{radio} m</span>
                </div>
                <div style={{ position: 'relative', height: 350, width: '100%', marginBottom: 8, borderRadius: 10, overflow: 'hidden', border: '1px solid #e0e0e0', boxShadow: '0 2px 8px #0001' }}>
                    <MapContainer center={[lat, lon]} zoom={14} style={{ height: '100%', width: '100%' }}>
                        <TileLayer
                            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                        />
                        <Circle center={[lat, lon]} radius={radio} pathOptions={{ color: '#6B73A1', fillColor: '#6B73A1', fillOpacity: 0.18, weight: 3 }} />
                        <ReferenceMarker />
                        {resultados.map((u, i) => (
                            <Marker key={i} position={[u.latitud, u.longitud]} icon={icon} eventHandlers={{ click: () => handleSeleccionar(u) }}>
                                <Popup>
                                    <div style={{ fontWeight: 600, color: '#388e3c' }}>{u.nombre}</div>
                                    <div style={{ fontSize: 13 }}>{u.direccion}</div>
                                    <div style={{ fontSize: 13 }}>Tipo: <b>{traducirTipo(u.tipo)}</b></div>
                                    <div style={{ fontSize: 13 }}>Coste: <b>{traducirCoste(u.coste)}</b></div>
                                    <button className="btn btn-outline" style={{ marginTop: 6 }} onClick={() => handleSeleccionar(u)}>Ver detalles</button>
                                </Popup>
                            </Marker>
                        ))}
                    </MapContainer>
                </div>
                {resultados.length > 0 && !seleccionPrevia && (
                    <div style={{ fontSize: 14, color: '#888', marginBottom: 8, textAlign: 'center' }}>
                        Haz clic en un marcador verde para ver detalles y seleccionar una ubicación.
                    </div>
                )}
            </div>
        </div>
    );
};

export default FiltroUbicacionesScreen;

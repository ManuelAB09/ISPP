

import React, { useState, useRef } from 'react';
import { ubicacionesApi } from '../../api/ubicaciones.api';
import { MapContainer, TileLayer, Marker, useMapEvents } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';

import './CrearUbicacionScreen.css';


const defaultPosition = [37.3891, -5.9845]; // Sevilla por defecto


function LocationMarker({ latitud, longitud, setLatitud, setLongitud, setDireccion }) {
    useMapEvents({
        click: async (e) => {
            setLatitud(e.latlng.lat);
            setLongitud(e.latlng.lng);
            // Reverse geocoding para actualizar dirección
            try {
                const response = await fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${e.latlng.lat}&lon=${e.latlng.lng}`);
                const data = await response.json();
                if (data && data.display_name) {
                    setDireccion(data.display_name);
                }
            } catch { }
        },
    });
    return latitud && longitud ? (
        <Marker position={[latitud, longitud]} icon={L.icon({ iconUrl: 'https://unpkg.com/leaflet@1.7.1/dist/images/marker-icon.png', iconSize: [25, 41], iconAnchor: [12, 41] })} />
    ) : null;
}

const CrearUbicacionScreen = () => {
    const [nombre, setNombre] = useState('');
    const [direccion, setDireccion] = useState('');
    const [latitud, setLatitud] = useState(defaultPosition[0]);
    const [longitud, setLongitud] = useState(defaultPosition[1]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(false);
    const [search, setSearch] = useState('');
    const mapRef = useRef();

    // Buscar dirección y actualizar lat/lng usando Nominatim
    const handleSearch = async (e) => {
        e.preventDefault();
        if (!search) return;
        setError(null);
        try {
            const response = await fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(search)}`);
            const data = await response.json();
            if (data && data.length > 0) {
                setLatitud(parseFloat(data[0].lat));
                setLongitud(parseFloat(data[0].lon));
                setDireccion(data[0].display_name);
            } else {
                setError('No se encontró la dirección.');
            }
        } catch (err) {
            setError('Error buscando la dirección.');
        }
    };

    // Cuando lat/lng cambian manualmente, actualizar dirección
    const handleLatLngChange = async (lat, lng) => {
        setLatitud(lat);
        setLongitud(lng);
        try {
            const response = await fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}`);
            const data = await response.json();
            if (data && data.display_name) {
                setDireccion(data.display_name);
            }
        } catch { }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError(null);
        setSuccess(false);
        try {
            await ubicacionesApi.create({
                nombre,
                direccion,
                latitud,
                longitud,
            });
            setSuccess(true);
            setNombre('');
            setDireccion('');
            setLatitud(defaultPosition[0]);
            setLongitud(defaultPosition[1]);
            setSearch('');
        } catch (err) {
            setError(err.message || 'Error al crear la ubicación');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="page-container">
            <div className="content-wrapper">
                <div className="header-section">
                    <h1 className="header-title">Crear Ubicación</h1>
                </div>
                {error && <div style={{ color: 'red', marginBottom: 16 }}>{error}</div>}
                {success && <div style={{ color: 'green', marginBottom: 16 }}>Ubicación creada correctamente.</div>}
                <form onSubmit={handleSubmit} style={{ maxWidth: 500, margin: '0 auto' }}>
                    <div className="input-group">
                        <label>Nombre</label>
                        <input type="text" value={nombre} onChange={e => setNombre(e.target.value)} required className="input-box input-large" />
                    </div>
                    <div className="input-group">
                        <label>Buscar dirección</label>
                        <div style={{ display: 'flex', gap: 8 }}>
                            <input type="text" value={search} onChange={e => setSearch(e.target.value)} placeholder="Buscar dirección o lugar" className="input-box input-large" />
                            <button type="button" className="btn btn-outline" onClick={handleSearch}>Buscar</button>
                        </div>
                    </div>
                    <div className="input-group">
                        <label>Dirección seleccionada</label>
                        <input type="text" value={direccion} onChange={e => setDireccion(e.target.value)} required className="input-box input-large" />
                    </div>
                    <div className="input-group">
                        <label>Selecciona la ubicación en el mapa</label>
                        <div style={{ height: 300, width: '100%', marginBottom: 16 }}>
                            <MapContainer
                                center={[latitud, longitud]}
                                zoom={16}
                                style={{ height: '100%', width: '100%' }}
                                whenCreated={mapInstance => { mapRef.current = mapInstance; }}
                            >
                                <TileLayer
                                    attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                                />
                                <LocationMarker latitud={latitud} longitud={longitud} setLatitud={setLatitud} setLongitud={setLongitud} setDireccion={setDireccion} />
                            </MapContainer>
                        </div>
                        <div style={{ display: 'flex', gap: 8 }}>
                            <input type="number" step="any" value={latitud} onChange={e => handleLatLngChange(Number(e.target.value), longitud)} required className="input-box input-large" placeholder="Latitud" />
                            <input type="number" step="any" value={longitud} onChange={e => handleLatLngChange(latitud, Number(e.target.value))} required className="input-box input-large" placeholder="Longitud" />
                        </div>
                    </div>
                    <button type="submit" className="btn btn-primary" disabled={loading} style={{ marginTop: 24 }}>
                        {loading ? 'Creando...' : 'Crear Ubicación'}
                    </button>
                </form>
            </div>
        </div>
    );
};

export default CrearUbicacionScreen;

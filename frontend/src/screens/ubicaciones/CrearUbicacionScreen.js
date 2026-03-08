import React, { useState, useRef } from 'react';
import { useNavigate, useSearchParams, useLocation } from 'react-router-dom';
import { ubicacionesApi } from '../../api/ubicaciones.api';
import { MapContainer, TileLayer, Marker, useMapEvents } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import FiltroUbicacionesScreen from './FiltroUbicacionesScreen';
import './CrearUbicacionScreen.css';


const defaultPosition = [37.3891, -5.9845]; // Sevilla por defecto

const getFriendlyErrorMessage = (err) => {
    if (!err) return 'No se pudo completar la operación.';

    if (err.status === 401 || err.status === 403) {
        return 'Tu sesión ha expirado o no tienes permisos. Vuelve a iniciar sesión.';
    }

    if (err.status >= 500) {
        return 'Ha ocurrido un error en el servidor. Inténtalo de nuevo en unos segundos.';
    }

    if (typeof err.message === 'string' && err.message.trim()) {
        return err.message;
    }

    return 'Error al crear la ubicación.';
};


function LocationMarker({ latitud, longitud, setLatitud, setLongitud, setDireccion }) {
    useMapEvents({
        click: async (e) => {
            setLatitud(e.latlng.lat);
            setLongitud(e.latlng.lng);
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
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const location = useLocation();
    const returnTo = searchParams.get('returnTo');
    const eventFormDraft = location.state?.eventFormDraft || null;
    const fromSource = location.state?.from || null;

    const [nombre, setNombre] = useState('');
    const [direccion, setDireccion] = useState('');
    const [latitud, setLatitud] = useState(defaultPosition[0]);
    const [longitud, setLongitud] = useState(defaultPosition[1]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(false);
    const [search, setSearch] = useState('');
    const mapRef = useRef();
    const [mostrarFiltro, setMostrarFiltro] = useState(false);
    const [ubicacionSeleccionada, setUbicacionSeleccionada] = useState(null);

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
            if (!direccion.trim()) {
                setError('Debes indicar una dirección o seleccionar un punto en el mapa.');
                setLoading(false);
                return;
            }

            const payload = {
                nombre: nombre.trim(),
                direccion,
                latitud,
                longitud,
                tipo: ubicacionSeleccionada?.tipo || '',
                coste: ubicacionSeleccionada?.coste || 'desconocido',
            };
            console.log('Payload enviado a crearUbicacion:', payload);
            const createdUbicacion = await ubicacionesApi.create(payload);

            // Si venimos desde crear/editar evento, volver con la ubicación creada
            if (returnTo) {
                const nombreFinal =
                    (createdUbicacion?.nombre && String(createdUbicacion.nombre).trim())
                    || nombre.trim()
                    || direccion.split(',')[0]?.trim()
                    || 'Ubicación';

                navigate(returnTo, {
                    state: {
                        ubicacion: {
                            id: createdUbicacion?.id || createdUbicacion?.ubicacionId,
                            nombre: nombreFinal,
                            direccion: direccion,
                            latitud: latitud,
                            longitud: longitud
                        },
                        eventFormDraft: eventFormDraft,
                        from: fromSource
                    }
                });
                return;
            }

            setSuccess(true);
            setNombre('');
            setDireccion('');
            setLatitud(defaultPosition[0]);
            setLongitud(defaultPosition[1]);
            setSearch('');
        } catch (err) {
            setError(getFriendlyErrorMessage(err));
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
                {returnTo && (
                    <div style={{ background: '#fff8e1', border: '1px solid #ffe082', borderRadius: 8, padding: '12px 16px', marginBottom: 16 }}>
                        <strong>Selecciona o crea una ubicación</strong> para vincular
                        <button type="button" className="btn btn-outline" style={{ marginLeft: 12, fontSize: '0.85rem', padding: '4px 12px' }} onClick={() => navigate(returnTo, { state: { eventFormDraft: eventFormDraft } })}>
                            Volver sin seleccionar
                        </button>
                    </div>
                )}
                <button type="button" className="btn btn-outline" style={{ marginBottom: 16 }} onClick={() => setMostrarFiltro(m => !m)}>
                    {mostrarFiltro ? 'Cerrar filtro de ubicaciones' : 'Buscar ubicaciones en el mapa'}
                </button>
                {mostrarFiltro && (
                    <FiltroUbicacionesScreen
                        onSeleccionar={async u => {
                            setNombre(u.nombre || '');
                            setLatitud(u.latitud);
                            setLongitud(u.longitud);
                            // Calcular dirección si es necesario
                            let dir = u.direccion;
                            if (!dir || dir === 'Dirección no disponible') {
                                try {
                                    const response = await fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${u.latitud}&lon=${u.longitud}`);
                                    const data = await response.json();
                                    dir = data && data.display_name ? data.display_name : '';
                                } catch { }
                            }
                            setDireccion(dir);
                            setUbicacionSeleccionada({ ...u, direccion: dir });
                            setMostrarFiltro(false);

                            // Si venimos desde crear/editar evento y la ubicación ya tiene ID, volver directamente
                            if (returnTo && u.id) {
                                navigate(returnTo, {
                                    state: {
                                        ubicacion: {
                                            id: u.id,
                                            nombre: u.nombre || '',
                                            direccion: dir,
                                            latitud: u.latitud,
                                            longitud: u.longitud
                                        },
                                        eventFormDraft: eventFormDraft,
                                        from: fromSource
                                    }
                                });
                            }
                        }}
                        onClose={() => setMostrarFiltro(false)}
                    />
                )}
                {ubicacionSeleccionada && (
                    <div style={{ background: '#e6f7ff', border: '1px solid #91d5ff', borderRadius: 6, padding: 16, marginBottom: 16 }}>
                        <b>Ubicación seleccionada:</b><br />
                        <span><b>{ubicacionSeleccionada.nombre}</b></span><br />
                        <span>{ubicacionSeleccionada.direccion}</span><br />
                        <span>Tipo: {ubicacionSeleccionada.tipo}</span><br />
                        <span>Coste: {ubicacionSeleccionada.coste}</span><br />
                    </div>
                )}
                <form onSubmit={handleSubmit} style={{ maxWidth: 500, margin: '0 auto', background: '#fff', borderRadius: 8, padding: 24, boxShadow: '0 2px 8px #0001' }}>
                    <div className="input-group">
                        <label>Nombre (opcional)</label>
                        <input type="text" value={nombre} onChange={e => setNombre(e.target.value)} className="input-box input-large" placeholder="Ej: Biblioteca Central" />
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
                        <div style={{ height: 300, width: '100%', marginBottom: 16, borderRadius: 8, overflow: 'hidden', border: '1px solid #eee' }}>
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
                        {loading ? 'Creando...' : (returnTo ? 'Crear y vincular' : 'Crear Ubicación')}
                    </button>
                </form>
            </div>
        </div>
    );
};

export default CrearUbicacionScreen;

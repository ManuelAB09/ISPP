import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../../components/Header/Header';
import { usersApi } from '../../api/users.api';
import InputSearch from '../../components/InputSearch/InputSearch';
import PageHeader from '../../components/PageHeader/PageHeader';
import { getApiBaseUrl } from '../../api/baseUrl';
import './Alumnos.css';

const DEFAULT_PROFILE_AVATAR =
    "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 120 120'%3E%3Ccircle cx='60' cy='60' r='60' fill='%23E6EAF3'/%3E%3Ccircle cx='60' cy='46' r='22' fill='%2395A1BB'/%3E%3Cpath d='M20 106c6-20 22-32 40-32s34 12 40 32' fill='%2395A1BB'/%3E%3C/svg%3E";

const toAbsoluteImageUrl = (imageUrl, fallback = DEFAULT_PROFILE_AVATAR) => {
    const raw = String(imageUrl || '').trim();
    if (!raw) {
        return fallback;
    }
    if (/^https?:\/\//i.test(raw) || raw.startsWith('data:') || raw.startsWith('blob:')) {
        return raw;
    }

    const base = getApiBaseUrl();
    return raw.startsWith('/') ? `${base}${raw}` : `${base}/${raw}`;
};

const Alumnos = () => {
    const navigate = useNavigate();
    const [searchQuery, setSearchQuery] = useState('');
    const [searchResults, setSearchResults] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [searched, setSearched] = useState(false);

    const isAuthenticated = !!localStorage.getItem('accessToken');

    const getStudentPhoto = (student) => {
        if (!student || typeof student !== 'object') {
            return null;
        }

        return (
            student.foto
            || student.avatarUrl
            || student.fotoUrl
            || student.fotoPerfil
            || student.avatar
            || student.imagen
            || student.image
            || student.usuarioFoto
            || null
        );
    };

    useEffect(() => {
        const performSearch = async () => {
            if (!searchQuery.trim()) {
                setSearchResults([]);
                setSearched(false);
                setError(null);
                return;
            }

            setLoading(true);
            setError(null);
            setSearched(true);

            try {
                const results = await usersApi.searchUsers(searchQuery);
                
                const filteredResults = (results || []).filter(user => {
                
                    if (user.rol === 'ADMIN' || user.rol === 'ADMINISTRADOR') {
                        return false;
                    }
                
                    if (user.email && user.email.toLowerCase().includes('admin')) {
                        return false;
                    }
                    return true;
                });
                setSearchResults(filteredResults);
            } catch (err) {
                console.error('Error al buscar usuarios:', err);
                setError('Error al buscar alumnos. Por favor, intenta de nuevo.');
                setSearchResults([]);
            } finally {
                setLoading(false);
            }
        };

        const timeoutId = setTimeout(performSearch, 300);

        return () => clearTimeout(timeoutId);
    }, [searchQuery]);

    const handleStudentClick = (studentId) => {
        navigate(`/perfil/${studentId}`);
    };

    return (
        <>
            <Header page="alumnos" />
            <div className="header">
                <PageHeader 
                    title="Alumnos"
                    subtitle="Busca y conoce a otros estudiantes de la aplicación"
                    className="alumnos"
                />
                <div className="search">
                    <InputSearch
                        placeholder='Buscar alumno por nombre o email'
                        mobilePlaceholder='Buscar alumno'
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                    />
                </div>
            </div>

            <div className="body">
                {error && <p className="error">{error}</p>}

                {loading && searched && (
                    <p className="loading-state">Buscando alumnos...</p>
                )}

                {searched && !loading && searchResults.length === 0 && !error && (
                    <p className="empty-state">No se encontraron alumnos para "{searchQuery}"</p>
                )}

                {searchResults.length > 0 && (
                    <ul className="alumnos-list">
                        {searchResults.map((student) => (
                            <li
                                key={student.id}
                                className="alumno-card"
                                onClick={() => handleStudentClick(student.id)}
                            >
                                <div className="alumno-avatar">
                                    <img
                                        src={toAbsoluteImageUrl(getStudentPhoto(student))}
                                        alt={student.nombre}
                                        onError={(e) => {
                                            e.target.onerror = null;
                                            e.target.src = DEFAULT_PROFILE_AVATAR;
                                        }}
                                    />
                                </div>
                                <div className="alumno-info">
                                    <h3>{student.nombre}</h3>
                                    <p className="email">{student.email}</p>
                                    {student.apellido && (
                                        <p className="apellido">{student.apellido}</p>
                                    )}
                                </div>
                            </li>
                        ))}
                    </ul>
                )}
            </div>
        </>
    );
};

export default Alumnos;

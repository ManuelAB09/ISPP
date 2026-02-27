import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PersonIcon from '../icons/Person';
import { communitiesApi } from '../../api/communities.api';
import './ComunidadCard.css';

export default function ComunidadCard({ comunidad, onJoined }) {
    const navigate = useNavigate();
    const [joining, setJoining] = useState(false);
    const [joined, setJoined] = useState(comunidad.esMiembro || false);
    const [error, setError] = useState(null);
    const currentUserId = localStorage.getItem('userId');

    const handleJoin = async (e) => {
        e.stopPropagation();
        const token = localStorage.getItem('accessToken');
        if (!token) {
            navigate('/login');
            return;
        }
        setJoining(true);
        setError(null);
        try {
            await communitiesApi.join(comunidad.id);
            setJoined(true);
            if (onJoined) onJoined(comunidad.id);
        } catch (err) {
            if (err.message?.includes('401') || err.status === 401) {
                navigate('/login');
            } else if (err.message?.includes('409') || err.status === 409) {
                // Already a member
                setJoined(true);
            } else {
                setError('Error al unirse');
            }
        } finally {
            setJoining(false);
        }
    };

    return (
        <div
            key={comunidad.id}
            className="comunidad-card"
            onClick={() => navigate(`/comunidades/${comunidad.id}`)}
            style={{ cursor: 'pointer' }}
        >
            <img src={comunidad.imagenUrl || comunidad.imagen || 'https://via.placeholder.com/300x200?text=Sin+imagen'} alt={comunidad.nombre} className="comunidad-image" />
            <div className="comunidad-info">
                <div className='top-info'>
                    <h2>{comunidad.nombre}</h2>
                    {comunidad?.categoria?.map(cat => (
                        <span key={cat} className="comunidad-tag">{cat}</span>
                    ))}
                    <p>{comunidad.descripcion || 'Sin descripción disponible'}</p>
                </div>
                <div className='bottom-info'>
                    <div className="members-info">
                        <PersonIcon width={20} height={20} />
                        <p>{comunidad.miembrosActuales || 0}/ <span>{comunidad.maxMiembros || 0}</span></p>
                    </div>
                    {error && <span style={{color:'red',fontSize:'0.8rem'}}>{error}</span>}
                    {currentUserId && !joined && (
                        <button
                            className="join-button"
                            onClick={handleJoin}
                            disabled={joining}
                        >
                            {joining ? 'Uniéndose...' : 'Unirse'}
                        </button>
                    )}
                    {joined && (
                        <button className="join-button joined" disabled>
                            ✓ Unido
                        </button>
                    )}
                    {!currentUserId && (
                        <button
                            className="join-button"
                            onClick={(e) => { e.stopPropagation(); navigate('/login'); }}
                        >
                            Unirse
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
}

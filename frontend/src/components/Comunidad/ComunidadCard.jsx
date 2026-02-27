import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PersonIcon from '../icons/Person';
import { communitiesApi } from '../../api/communities.api';
import './ComunidadCard.css';

export default function ComunidadCard({ comunidad }) {
    const navigate = useNavigate();
    const [joining, setJoining] = useState(false);
    const [joined, setJoined] = useState(false);
    const [error, setError] = useState(null);

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
        } catch (err) {
            if (err.message?.includes('401') || err.status === 401) {
                navigate('/login');
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
                    <button
                        className="join-button"
                        onClick={handleJoin}
                        disabled={joining || joined}
                    >
                        {joined ? 'Unido' : joining ? 'Uniéndose...' : 'Unirse'}
                    </button>
                </div>
            </div>
        </div>
    );
}

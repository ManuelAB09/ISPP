import PersonIcon from '../icons/Person';
import './ComunidadCard.css';

export default function ComunidadCard({ comunidad }) {
    return (
        <div key={comunidad.id} className="comunidad-card">
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
                    <button className="join-button">Unirse</button>
                </div>
            </div>
        </div>
    );
}

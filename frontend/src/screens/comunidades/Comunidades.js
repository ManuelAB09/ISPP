import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { communitiesApi } from '../../api/communities.api';
import ComunidadCard from '../../components/Comunidad/ComunidadCard';
import Header from '../../components/Header/Header';
import InputSearch from '../../components/InputSearch/InputSearch';
import PageHeader from '../../components/PageHeader';
import './Comunidades.css';

export default function Comunidades() {
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [comunidades, setComunidades] = useState([])
    const [page, setPage] = useState(0)
    const [totalPages, setTotalPages] = useState(0)
    const [search, setSearch] = useState('')

    useEffect(() => {
        setLoading(true);
        setError(null);
        communitiesApi.list({ page: page, size: 10, search: search })
            .then(response => {
                setComunidades(response.content || []);
                setTotalPages(response.page?.totalPages || 0);
            })
            .catch(err => {
                console.error("Error fetching communities:", err);
                setError('No se pudieron cargar las comunidades. Inténtalo de nuevo más tarde.');
                setComunidades([]);
                setTotalPages(0);
            })
            .finally(() => setLoading(false));
    }, [page, search])

    return (
        <>
            <Header page={'comunidades'}/>
            <div className="header">
                <PageHeader 
                    title="Comunidades"
                    subtitle="Explora las comunidades que mejor se adaptan a tus necesidades y ganas de aprender"
                    className="comunidad"
                />
                <div className="search">
                    <InputSearch 
                        placeholder='Buscar una comunidad por nombre, etiqueta o palabra clave' 
                        mobilePlaceholder='Buscar comunidad'
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                    />
                    <button 
                        className="create-community-btn"
                        onClick={() => navigate('/crear-comunidad')}
                    >
                        Crear comunidad
                    </button>
                </div>
            </div>
            <div className="body">
                {error && <p className="error">{error}</p>}
                {loading ? (
                    <p>Cargando comunidades...</p>
                ) : (
                    <ul className="comunidades-list">
                        {comunidades?.map(comunidad => (
                            <ComunidadCard key={comunidad.id} comunidad={comunidad} />
                        ))}
                    </ul>
                )}
                {totalPages > 1 && !loading && (
                    <div className="pagination">
                        <button onClick={() => setPage(prev => Math.max(prev - 1, 0))} disabled={page === 0}>Anterior</button>
                        <span>Página {page + 1} de {totalPages}</span>
                        <button onClick={() => setPage(prev => Math.min(prev + 1, totalPages - 1))} disabled={page === totalPages - 1}>Siguiente</button>
                    </div>
                )}
            </div>
        </>
    );
}

import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { communitiesApi } from '../../api/communities.api';
import { institutionsApi } from '../../api/institutions.api';
import ComunidadCard from '../../components/Comunidad/ComunidadCard';
import Header from '../../components/Header/Header';
import InputSearch from '../../components/InputSearch/InputSearch';
import PageHeader from '../../components/PageHeader';
import './Comunidades.css';

export default function Comunidades() {
    const SEARCH_MAX_LENGTH = 120;
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [comunidades, setComunidades] = useState([]);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [search, setSearch] = useState('');
    const [tipoGrupo, setTipoGrupo] = useState([]);
    const [tipoPlan, setTipoPlan] = useState([]);
    const [categoria, setCategoria] = useState([]);
    const [institucion, setInstitucion] = useState('');
    const [availableCategories, setAvailableCategories] = useState([]);
    const [availableInstitutions, setAvailableInstitutions] = useState([]);

    const isAuthenticated = Boolean(localStorage.getItem('accessToken'));
    const normalizedSearch = search.trim().slice(0, SEARCH_MAX_LENGTH);

    useEffect(() => {
        communitiesApi.listCategories()
            .then((categoriesResponse) => {
                setAvailableCategories(
                    Array.isArray(categoriesResponse)
                        ? categoriesResponse.map((category) => category.nombre).filter(Boolean)
                        : []
                );
            })
            .catch((err) => console.error('Error fetching categories:', err));

        institutionsApi.list()
            .then((institutionsResponse) => {
                setAvailableInstitutions(
                    Array.isArray(institutionsResponse)
                        ? institutionsResponse.map((institution) => institution.nombre).filter(Boolean)
                        : []
                );
            })
            .catch((err) => console.error('Error fetching institutions:', err));
    }, []);

    useEffect(() => {
        setLoading(true);
        communitiesApi.list({
            page,
            size: 10,
            search: normalizedSearch,
            categoria: categoria.length ? categoria : undefined,
            institucion: institucion || undefined,
            tipoGrupo: tipoGrupo.length ? tipoGrupo : undefined,
            tipoPlan: tipoPlan.length ? tipoPlan : undefined,
        })
            .then(res => {
                setComunidades(res.content || []);
                setTotalPages(res.page?.totalPages || 0);
            })
            .catch(() => setError('Error al cargar comunidades'))
            .finally(() => setLoading(false));
    }, [page, normalizedSearch, categoria, institucion, tipoGrupo, tipoPlan]);

    // Función auxiliar para manejar la lógica de selección múltiple (Toggle)
    const handleToggleFilter = (currentList, value, setter) => {
        const nextList = currentList.includes(value)
            ? currentList.filter(item => item !== value)
            : [...currentList, value];
        setter(nextList);
        setPage(0);
    };

    return (
        <>
            <Header page={'comunidades'}/>
            <div className="header">
                <PageHeader 
                    title="Comunidades"
                    subtitle="Explora las comunidades que mejor se adaptan a tus necesidades"
                    className="comunidad"
                />
                
                <div className="search">
                    <div className="search-row">
                        <InputSearch
                            placeholder='Buscar comunidad...'
                            value={search}
                            onChange={(e) => { setSearch(e.target.value.slice(0, SEARCH_MAX_LENGTH)); setPage(0); }}
                        />
                        {isAuthenticated && (
                            <button className="create-community-btn" onClick={() => navigate('/crear-comunidad')}>
                                Crear comunidad
                            </button>
                        )}
                    </div>

                    <div className="filters-panel">
                        <div className="filters-panel__header">
                            <div>
                                <p className="filters-panel__eyebrow">Filtros</p>
                                <h2>Afina la búsqueda</h2>
                            </div>
                        </div>

                        <div className="comunidades-filters">
                            {/* FILTRO PRIVACIDAD */}
                            <div className="filter-group">
                                <span>Privacidad</span>
                                <div className="chips-container">
                                    {[
                                        { id: 'COMUNIDAD_PUBLICA', label: 'Públicas' },
                                        { id: 'GRUPO_PRIVADO', label: 'Privadas' }
                                    ].map(opt => (
                                        <button
                                            key={opt.id}
                                            className={`filter-chip ${tipoGrupo.includes(opt.id) ? 'active' : ''}`}
                                            onClick={() => handleToggleFilter(tipoGrupo, opt.id, setTipoGrupo)}
                                        >
                                            {opt.label}
                                        </button>
                                    ))}
                                </div>
                            </div>

                            {/* FILTRO PLAN */}
                            <div className="filter-group">
                                <span>Plan</span>
                                <div className="chips-container">
                                    {['FREE', 'PREMIUM', 'UNLIMITED'].map(plan => (
                                        <button
                                            key={plan}
                                            className={`filter-chip ${tipoPlan.includes(plan) ? 'active' : ''}`}
                                            onClick={() => handleToggleFilter(tipoPlan, plan, setTipoPlan)}
                                        >
                                            {plan}
                                        </button>
                                    ))}
                                </div>
                            </div>

                            {/* FILTRO CATEGORÍAS */}
                            <div className="filter-group filter-group--wide">
                                <span>Categorías</span>
                                <div className="chips-container scrollable">
                                    {availableCategories.map(cat => (
                                        <button
                                            key={cat}
                                            className={`filter-chip ${categoria.includes(cat) ? 'active' : ''}`}
                                            onClick={() => handleToggleFilter(categoria, cat, setCategoria)}
                                        >
                                            {cat}
                                        </button>
                                    ))}
                                </div>
                            </div>

                            {/* FILTRO INSTITUCIÓN (Select simple - se mantiene por practicidad) */}
                            <div className="filter-group filter-group--wide">
                                <span>Institución</span>
                                <select 
                                    className="filter-select-simple"
                                    value={institucion} 
                                    aria-label="Filtrar por institución"
                                    onChange={(e) => { setInstitucion(e.target.value); setPage(0); }}
                                >
                                    <option value="">Todas las instituciones</option>
                                    {availableInstitutions.map(inst => (
                                        <option key={inst} value={inst}>{inst}</option>
                                    ))}
                                </select>
                            </div>
                        </div>
                    </div>
                </div>
                <p className="search-limit-hint">{search.length}/{SEARCH_MAX_LENGTH}</p>
            </div>

            <div className="body">
                {error && <p className="error">{error}</p>}
                {loading ? <p>Cargando...</p> : (
                    <ul className="comunidades-list">
                        {comunidades.map(c => <ComunidadCard key={c.id} comunidad={c} />)}
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
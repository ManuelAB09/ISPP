import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { communitiesApi } from '../../api/communities.api';
import { institutionsApi } from '../../api/institutions.api';
import ComunidadCard from '../../components/Comunidad/ComunidadCard';
import Header from '../../components/Header/Header';
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
    const [filtersOpen, setFiltersOpen] = useState(false);

    const isAuthenticated = Boolean(localStorage.getItem('accessToken'));
    const normalizedSearch = search.trim().slice(0, SEARCH_MAX_LENGTH);
    const activeFiltersCount =
        tipoGrupo.length + tipoPlan.length + categoria.length + (institucion ? 1 : 0);

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

    const handleToggleFilter = (currentList, value, setter) => {
        const nextList = currentList.includes(value)
            ? currentList.filter(item => item !== value)
            : [...currentList, value];
        setter(nextList);
        setPage(0);
    };

    const clearAllFilters = () => {
        setTipoGrupo([]);
        setTipoPlan([]);
        setCategoria([]);
        setInstitucion('');
        setPage(0);
    };

    return (
        <>
            <Header page={'comunidades'}/>

            {/* ── HERO HEADER ───────────────────────────────────── */}
            <div className="comunidades-hero">
                <div className="comunidades-hero__decoration" aria-hidden="true"/>
                <div className="comunidades-hero__content">
                    <span className="comunidades-hero__badge">Descubre · Conecta · Aprende</span>
                    <h1 className="comunidades-hero__title">Comunidades</h1>
                    <p className="comunidades-hero__subtitle">
                        Explora las comunidades que mejor se adaptan a tus necesidades
                    </p>
                </div>
            </div>

            {/* ── CONTROLS (search + filtros) ───────────────────── */}
            <div className="comunidades-controls">

                {/* Search row */}
                <div className="comunidades-search-row">

                    {/* Enhanced search input */}
                    <div className="comunidades-search-wrap">
                        <svg className="search-icon" xmlns="http://www.w3.org/2000/svg"
                            viewBox="0 0 24 24" fill="none" stroke="currentColor"
                            strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <circle cx="11" cy="11" r="8"/>
                            <line x1="21" y1="21" x2="16.65" y2="16.65"/>
                        </svg>
                        <input
                            type="text"
                            data-testid="search-input"
                            className="comunidades-search-input"
                            placeholder="Buscar comunidad..."
                            value={search}
                            onChange={(e) => {
                                setSearch(e.target.value.slice(0, SEARCH_MAX_LENGTH));
                                setPage(0);
                            }}
                        />
                        {search && (
                            <button
                                className="search-clear-btn"
                                onClick={() => { setSearch(''); setPage(0); }}
                                aria-label="Limpiar búsqueda"
                            >
                                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"
                                    fill="none" stroke="currentColor" strokeWidth="2.5"
                                    strokeLinecap="round" strokeLinejoin="round">
                                    <line x1="18" y1="6" x2="6" y2="18"/>
                                    <line x1="6" y1="6" x2="18" y2="18"/>
                                </svg>
                            </button>
                        )}
                    </div>

                    {/* Action buttons */}
                    <div className="comunidades-action-btns">
                        <button
                            className={`filters-toggle-btn${filtersOpen ? ' filters-toggle-btn--open' : ''}`}
                            onClick={() => setFiltersOpen(prev => !prev)}
                        >
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"
                                fill="none" stroke="currentColor" strokeWidth="2"
                                strokeLinecap="round" strokeLinejoin="round">
                                <line x1="4" y1="6" x2="20" y2="6"/>
                                <line x1="8" y1="12" x2="16" y2="12"/>
                                <line x1="11" y1="18" x2="13" y2="18"/>
                            </svg>
                            Filtros
                            {activeFiltersCount > 0 && (
                                <span className="filters-badge">{activeFiltersCount}</span>
                            )}
                            <svg className="filters-chevron" xmlns="http://www.w3.org/2000/svg"
                                viewBox="0 0 24 24" fill="none" stroke="currentColor"
                                strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                                <polyline points="6 9 12 15 18 9"/>
                            </svg>
                        </button>

                        {isAuthenticated && (
                            <button
                                className="create-community-btn"
                                onClick={() => navigate('/crear-comunidad')}
                            >
                                + Crear comunidad
                            </button>
                        )}
                    </div>
                </div>

                <p className="search-limit-hint">{search.length}/{SEARCH_MAX_LENGTH}</p>

                {/* ── COLLAPSIBLE FILTER PANEL ─────────────────── */}
                <div className={`filters-panel${filtersOpen ? ' filters-panel--open' : ''}`}>
                    <div className="filters-panel-inner">
                        <div className="filters-panel-content">

                            <div className="filters-panel__top">
                                <span className="filters-panel__label">Refina tu búsqueda</span>
                                {activeFiltersCount > 0 && (
                                    <button className="filters-clear-btn" onClick={clearAllFilters}>
                                        Limpiar filtros
                                    </button>
                                )}
                            </div>

                            <div className="comunidades-filters">

                                {/* PRIVACIDAD */}
                                <div className="filter-group">
                                    <span>Privacidad</span>
                                    <div className="chips-container">
                                        {[
                                            { id: 'COMUNIDAD_PUBLICA', label: 'Públicas' },
                                            { id: 'GRUPO_PRIVADO', label: 'Privadas' }
                                        ].map(opt => (
                                            <button
                                                key={opt.id}
                                                className={`filter-chip${tipoGrupo.includes(opt.id) ? ' active' : ''}`}
                                                onClick={() => handleToggleFilter(tipoGrupo, opt.id, setTipoGrupo)}
                                            >
                                                {opt.label}
                                            </button>
                                        ))}
                                    </div>
                                </div>

                                {/* PLAN */}
                                <div className="filter-group">
                                    <span>Plan</span>
                                    <div className="chips-container">
                                        {['FREE', 'PREMIUM', 'UNLIMITED'].map(plan => (
                                            <button
                                                key={plan}
                                                className={`filter-chip${tipoPlan.includes(plan) ? ' active' : ''}`}
                                                onClick={() => handleToggleFilter(tipoPlan, plan, setTipoPlan)}
                                            >
                                                {plan}
                                            </button>
                                        ))}
                                    </div>
                                </div>

                                {/* CATEGORÍAS */}
                                <div className="filter-group filter-group--wide">
                                    <span>Categorías</span>
                                    <div className="chips-container scrollable">
                                        {availableCategories.map(cat => (
                                            <button
                                                key={cat}
                                                className={`filter-chip${categoria.includes(cat) ? ' active' : ''}`}
                                                onClick={() => handleToggleFilter(categoria, cat, setCategoria)}
                                            >
                                                {cat}
                                            </button>
                                        ))}
                                    </div>
                                </div>

                                {/* INSTITUCIÓN */}
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
                </div>

            </div>

            {/* ── BODY ─────────────────────────────────────────── */}
            <div className="body">
                {error && <p className="error">{error}</p>}
                {loading ? <p>Cargando...</p> : (
                    <ul className="comunidades-list">
                        {comunidades.map(c => <ComunidadCard key={c.id} comunidad={c}/>)}
                    </ul>
                )}
                {totalPages > 1 && !loading && (
                    <div className="pagination">
                        <button
                            onClick={() => setPage(prev => Math.max(prev - 1, 0))}
                            disabled={page === 0}
                        >
                            Anterior
                        </button>
                        <span>Página {page + 1} de {totalPages}</span>
                        <button
                            onClick={() => setPage(prev => Math.min(prev + 1, totalPages - 1))}
                            disabled={page === totalPages - 1}
                        >
                            Siguiente
                        </button>
                    </div>
                )}
            </div>
        </>
    );
}

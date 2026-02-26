import { useEffect, useState } from 'react';
import { communitiesApi } from '../../api/communities.api';
import ComunidadCard from '../../components/Comunidad/ComunidadCard';
import Header from '../../components/Header/Header';
import CreateIcon from '../../components/icons/Create';
import FilterIcon from '../../components/icons/Filter';
import InputSearch from '../../components/InputSearch/InputSearch';
import './Comunidades.css';

// =====================================================
// Mock data para comunidades de ejemplo
// TODO: Eliminar cuando los datos reales estén disponibles del API
// =====================================================
const MOCK_COMUNIDADES = [
    {
        id: 1,
        nombre: "ISSI 2 - US",
        descripcion: "Comunidad para resolver exámenes y dudas de la asignatura ISSI 2 en la Universidad de Sevilla.",
        tipoGrupo: "COMUNIDAD_PUBLICA",
        tipoPlan: "FREE",
        maxMiembros: 30,
        miembrosActuales: 24,
        creador: {
            id: 1,
            nombre: "Alberto Gómez",
            email: "alberto@us.es",
            avatarUrl: "https://randomuser.me/api/portraits/men/32.jpg"
        },
        estado: "ACTIVA",
        esMiembro: false,
        miRol: null,
        createdAt: "2025-10-15T10:30:00Z",
        updatedAt: "2025-10-15T10:30:00Z",
        imagenUrl: "https://images.unsplash.com/photo-1552664730-d307ca884978?w=400&h=300&fit=crop"
    },
    {
        id: 2,
        nombre: "Fundamentos de Programación",
        descripcion: "Comunidad para principiantes en C++. Compartimos apuntes, ejercicios resueltos y dudas de programación.",
        tipoGrupo: "COMUNIDAD_PUBLICA",
        tipoPlan: "FREE",
        maxMiembros: 50,
        miembrosActuales: 38,
        creador: {
            id: 2,
            nombre: "María López",
            email: "maria@us.es",
            avatarUrl: "https://randomuser.me/api/portraits/women/28.jpg"
        },
        estado: "ACTIVA",
        esMiembro: false,
        miRol: null,
        createdAt: "2025-09-20T14:45:00Z",
        updatedAt: "2025-10-10T08:15:00Z",
        imagenUrl: "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=400&h=300&fit=crop"
    },
    {
        id: 3,
        nombre: "Álgebra Lineal Avanzada",
        descripcion: "Espacios vectoriales, matrices, determinantes y aplicaciones. Nivel intermedio-avanzado.",
        tipoGrupo: "COMUNIDAD_PUBLICA",
        tipoPlan: "PREMIUM",
        maxMiembros: 100,
        miembrosActuales: 67,
        creador: {
            id: 3,
            nombre: "Manuel Nuño",
            email: "manuel@us.es",
            avatarUrl: "https://randomuser.me/api/portraits/men/44.jpg"
        },
        estado: "ACTIVA",
        esMiembro: false,
        miRol: null,
        createdAt: "2025-08-01T09:00:00Z",
        updatedAt: "2025-10-12T16:30:00Z",
        imagenUrl: "https://images.unsplash.com/photo-1552664730-d307ca884978?w=400&h=300&fit=crop"
    },
    {
        id: 4,
        nombre: "Preparación PEvAU - Física",
        descripcion: "Preparación intensiva para la prueba de acceso a la universidad (PEvAU). Exámenes simulados y análisis de errores.",
        tipoGrupo: "GRUPO_PRIVADO",
        tipoPlan: "FREE",
        maxMiembros: 20,
        miembrosActuales: 18,
        creador: {
            id: 4,
            nombre: "Laura Fernández",
            email: "laura@us.es",
            avatarUrl: "https://randomuser.me/api/portraits/women/35.jpg"
        },
        estado: "ACTIVA",
        esMiembro: false,
        miRol: null,
        createdAt: "2025-09-01T11:20:00Z",
        updatedAt: "2025-10-14T13:45:00Z",
        imagenUrl: "https://images.unsplash.com/photo-1460661419201-fd4cecdf8a8b?w=400&h=300&fit=crop"
    },
    {
        id: 5,
        nombre: "Inglés B2 Conversacional",
        descripcion: "Grupo de conversación en inglés para alcanzar nivel B2. Debates, pronunciación y comprensión auditiva.",
        tipoGrupo: "COMUNIDAD_PUBLICA",
        tipoPlan: "FREE",
        maxMiembros: 25,
        miembrosActuales: 19,
        creador: {
            id: 5,
            nombre: "David Chen",
            email: "david@us.es",
            avatarUrl: "https://randomuser.me/api/portraits/men/52.jpg"
        },
        estado: "ACTIVA",
        esMiembro: false,
        miRol: null,
        createdAt: "2025-07-15T15:00:00Z",
        updatedAt: "2025-10-13T10:10:00Z",
        imagenUrl: "https://images.unsplash.com/photo-1552664730-d307ca884978?w=400&h=300&fit=crop"
    },
    {
        id: 6,
        nombre: "Estruturas de Datos Java Premium",
        descripcion: "Comunidad premium con contenido exclusivo sobre estructuras de datos, algoritmos y patrones de diseño.",
        tipoGrupo: "COMUNIDAD_PUBLICA",
        tipoPlan: "PREMIUM",
        maxMiembros: 150,
        miembrosActuales: 142,
        creador: {
            id: 1,
            nombre: "Alberto Gómez",
            email: "alberto@us.es",
            avatarUrl: "https://randomuser.me/api/portraits/men/32.jpg"
        },
        estado: "ACTIVA",
        esMiembro: false,
        miRol: null,
        createdAt: "2025-06-10T12:30:00Z",
        updatedAt: "2025-10-15T09:50:00Z",
        imagenUrl: "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=400&h=300&fit=crop"
    }
];

export default function Comunidades() {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [comunidades, setComunidades] = useState([])
    const [page, setPage] = useState(0)
    const [totalPages, setTotalPages] = useState(0)
    const [search, setSearch] = useState('')
    const [usandoMock, setUsandoMock] = useState(false)

    useEffect(() => {
        setLoading(true);
        setError(null);
        communitiesApi.list({ page: page, size: 10, search: search })
            .then(response => {
                console.log("🚀 ~ Comunidades ~ response:", response)
                setComunidades(response.content || []);
                setTotalPages(response.page?.totalPages || 0);
                setUsandoMock(false);
            })
            .catch(err => {
                console.error("Error fetching communities:", err);
                // Usar mock data como fallback
                console.log("📦 Usando datos de ejemplo (mock)");
                setComunidades(MOCK_COMUNIDADES);
                setTotalPages(1);
                setUsandoMock(true);
            })
            .finally(() => setLoading(false));
    }, [page, search])

    return (
        <>
            <Header page={'comunidades'}/>
            <div className="header">
                <div className="headerTitle">
                    <p>Explora las comunidades que mejor se adaptan a tus necesidades y ganas de aprender </p>
                    <span className="line"></span>
                    <h1>Comunidades</h1>
                </div>
                <div className="search">
                    <InputSearch 
                        placeholder='Buscar una comunidad por nombre, etiqueta o palabra clave' 
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        />
                    <FilterIcon />
                    <CreateIcon />
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

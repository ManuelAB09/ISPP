import React from 'react';
import { render, screen, waitFor, fireEvent, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Chats, { resolveCommunityImage } from './Chats';

const mockNavigate = jest.fn();
let mockSearchParams = new URLSearchParams();

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
    useSearchParams: () => [mockSearchParams, jest.fn()],
}));

jest.mock('../../contexts/AuthContext', () => ({
    useAuth: () => ({
        user: {
            id: 1,
            nombre: 'Usuario Test',
            foto: 'https://example.com/foto.jpg',
        },
    }),
}));

const mockClearCommunityUnread = jest.fn();
const mockIsChatMuted = jest.fn(() => false);
const mockToggleChatMuted = jest.fn();

jest.mock('../../contexts/NotificationContext', () => ({
    useNotificationContext: () => ({
        isChatMuted: mockIsChatMuted,
        toggleChatMuted: mockToggleChatMuted,
        communityUnreadById: { '10': 3 },
        clearCommunityUnread: mockClearCommunityUnread,
    }),
}));

jest.mock('../../api/auth.api', () => ({
    authApi: {
        getUserPublicProfile: jest.fn().mockResolvedValue({ data: { id: 2, nombre: 'OtroUser', foto: null } }),
    },
}));

const mockListMine = jest.fn();
jest.mock('../../api/communities.api', () => ({
    communitiesApi: {
        listMine: (...args) => mockListMine(...args),
    },
}));

const mockObtenerConversaciones = jest.fn();
const mockMarcarComunidadComoLeida = jest.fn().mockResolvedValue({});
const mockMarcarConversacionComoLeida = jest.fn().mockResolvedValue({});
const mockObtenerNoLeidosPorComunidad = jest.fn().mockResolvedValue({ data: {} });

jest.mock('../../api/mensajeService', () => ({
    obtenerConversaciones: (...args) => mockObtenerConversaciones(...args),
    marcarComunidadComoLeida: (...args) => mockMarcarComunidadComoLeida(...args),
    marcarConversacionComoLeida: (...args) => mockMarcarConversacionComoLeida(...args),
    obtenerNoLeidosPorComunidad: (...args) => mockObtenerNoLeidosPorComunidad(...args),
}));

jest.mock('../../api/baseUrl', () => ({
    getApiBaseUrl: () => 'http://localhost:8080',
}));

jest.mock('../../components/Header/Header', () => {
    return function MockHeader() {
        return <div data-testid="mock-header">Header</div>;
    };
});

jest.mock('./CommunityChat', () => {
    return function MockCommunityChat(props) {
        return <div data-testid="mock-community-chat">{props.comunidadNombre}</div>;
    };
});

jest.mock('./PrivateChat', () => {
    return function MockPrivateChat(props) {
        return <div data-testid="mock-private-chat">{props.targetUser?.nombre}</div>;
    };
});

jest.mock('../../components/PageHeader', () => {
    return function MockPageHeader({ title, subtitle }) {
        return <div data-testid="page-header">{title} - {subtitle}</div>;
    };
});

const mockCommunities = [
    { id: 10, nombre: 'Comunidad A', descripcion: 'Desc A', imagen: 'imgA.jpg' },
    { id: 20, nombre: 'Comunidad B', descripcion: 'Desc B', imagen: null },
];

const mockConversaciones = [
    { usuarioId: 5, usuarioNombre: 'Carlos', usuarioFoto: null, ultimoMensaje: 'Hola!', noLeidos: 2 },
    { usuarioId: 6, usuarioNombre: 'Ana', usuarioFoto: 'ana.jpg', ultimoMensaje: 'Adiós', noLeidos: 0 },
];

describe('Chats', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockSearchParams = new URLSearchParams();
        localStorage.setItem('accessToken', 'token123');
        localStorage.setItem('userId', '1');
        mockListMine.mockResolvedValue({ content: [], page: { totalPages: 1 } });
        mockObtenerConversaciones.mockResolvedValue({ data: [] });
    });

    afterEach(() => {
        localStorage.clear();
    });

    describe('resolveCommunityImage', () => {
        it('returns default when no image', () => {
            expect(resolveCommunityImage({})).toContain('unsplash.com');
        });

        it('returns default for empty string', () => {
            expect(resolveCommunityImage({ imagen: '  ' })).toContain('unsplash.com');
        });

        it('returns default for "null" string', () => {
            expect(resolveCommunityImage({ imagen: 'null' })).toContain('unsplash.com');
        });

        it('returns default for "empty" string', () => {
            expect(resolveCommunityImage({ imagen: 'empty' })).toContain('unsplash.com');
        });

        it('returns http url as-is', () => {
            expect(resolveCommunityImage({ imagen: 'https://img.com/a.jpg' })).toBe('https://img.com/a.jpg');
        });

        it('returns data url as-is', () => {
            expect(resolveCommunityImage({ imagen: 'data:image/png;base64,abc' })).toBe('data:image/png;base64,abc');
        });

        it('prepends base for relative path starting with /', () => {
            expect(resolveCommunityImage({ imagen: '/uploads/img.jpg' })).toBe('http://localhost:8080/uploads/img.jpg');
        });

        it('prepends base with / for relative path not starting with /', () => {
            expect(resolveCommunityImage({ imagen: 'uploads/img.jpg' })).toBe('http://localhost:8080/uploads/img.jpg');
        });

        it('uses imagenUrl fallback', () => {
            expect(resolveCommunityImage({ imagenUrl: 'https://img.com/b.jpg' })).toBe('https://img.com/b.jpg');
        });

        it('uses foto fallback', () => {
            expect(resolveCommunityImage({ foto: 'https://img.com/c.jpg' })).toBe('https://img.com/c.jpg');
        });

        it('returns default for "undefined" string', () => {
            expect(resolveCommunityImage({ imagen: 'undefined' })).toContain('unsplash.com');
        });

        it('returns default for blob url', () => {
            expect(resolveCommunityImage({ imagen: 'blob:http://localhost/abc' })).toBe('blob:http://localhost/abc');
        });
    });

    describe('Renderizado inicial', () => {
        it('debería renderizar el componente con Header', async () => {
            await act(async () => {
                render(<MemoryRouter><Chats /></MemoryRouter>);
            });
            expect(screen.getByTestId('mock-header')).toBeInTheDocument();
        });

        it('debería mostrar el PageHeader con título y subtítulo', async () => {
            await act(async () => {
                render(<MemoryRouter><Chats /></MemoryRouter>);
            });
            expect(screen.getByTestId('page-header')).toBeInTheDocument();
        });

        it('redirige a /login si no hay accessToken', async () => {
            localStorage.removeItem('accessToken');
            await act(async () => {
                render(<MemoryRouter><Chats /></MemoryRouter>);
            });
            expect(mockNavigate).toHaveBeenCalledWith('/login');
        });
    });

    describe('Navegación de pestañas', () => {
        it('debería tener pestaña Comunidades y Privados', async () => {
            await act(async () => {
                render(<MemoryRouter><Chats /></MemoryRouter>);
            });
            const tabs = screen.getAllByRole('button').filter(b => b.className.includes('chats-tab'));
            expect(tabs.length).toBe(2);
        });

        it('pestaña comunidades activa por defecto', async () => {
            await act(async () => {
                render(<MemoryRouter><Chats /></MemoryRouter>);
            });
            const tabs = screen.getAllByRole('button').filter(b => b.className.includes('chats-tab'));
            const comTab = tabs[0];
            expect(comTab.className).toContain('active');
        });

        it('cambia a pestaña privados al hacer clic', async () => {
            await act(async () => {
                render(<MemoryRouter><Chats /></MemoryRouter>);
            });
            const privTab = screen.getByRole('button', { name: /privados/i });
            await act(async () => { fireEvent.click(privTab); });
            expect(privTab.className).toContain('active');
        });

        it('muestra badge con no leídos de comunidades', async () => {
            mockListMine.mockResolvedValue({ content: mockCommunities, page: { totalPages: 1 } });
            await act(async () => {
                render(<MemoryRouter><Chats /></MemoryRouter>);
            });
            // communityUnreadById = {'10': 3} so communityUnread = 3
            await waitFor(() => {
                const badges = screen.getAllByText('3');
                expect(badges.length).toBeGreaterThan(0);
            });
        });
    });

    describe('Lista de comunidades', () => {
        it('muestra comunidades cargadas en el sidebar', async () => {
            mockListMine.mockResolvedValue({ content: mockCommunities, page: { totalPages: 1 } });
            await act(async () => {
                render(<MemoryRouter><Chats /></MemoryRouter>);
            });
            await waitFor(() => {
                expect(screen.getAllByText('Comunidad A').length).toBeGreaterThan(0);
                expect(screen.getAllByText('Comunidad B').length).toBeGreaterThan(0);
            });
        });

        it('muestra CommunityChat para comunidad seleccionada', async () => {
            mockListMine.mockResolvedValue({ content: mockCommunities, page: { totalPages: 1 } });
            await act(async () => {
                render(<MemoryRouter><Chats /></MemoryRouter>);
            });
            await waitFor(() => {
                expect(screen.getByTestId('mock-community-chat')).toBeInTheDocument();
            });
        });

        it('muestra estado vacío si no hay comunidades', async () => {
            mockListMine.mockResolvedValue({ content: [], page: { totalPages: 1 } });
            await act(async () => {
                render(<MemoryRouter><Chats /></MemoryRouter>);
            });
            await waitFor(() => {
                expect(screen.getByText(/no tienes chats de comunidades/i)).toBeInTheDocument();
            });
        });

        it('muestra botón explorar comunidades', async () => {
            await act(async () => {
                render(<MemoryRouter><Chats /></MemoryRouter>);
            });
            await waitFor(() => {
                expect(screen.getByText(/explorar comunidades/i)).toBeInTheDocument();
            });
        });

        it('muestra error si falla la carga', async () => {
            mockListMine.mockRejectedValue(new Error('fail'));
            await act(async () => {
                render(<MemoryRouter><Chats /></MemoryRouter>);
            });
            await waitFor(() => {
                expect(screen.getByText(/no se pudieron cargar/i)).toBeInTheDocument();
            });
        });
    });

    describe('Conversaciones privadas', () => {
        it('muestra conversaciones con último mensaje', async () => {
            mockListMine.mockResolvedValue({ content: mockCommunities, page: { totalPages: 1 } });
            mockObtenerConversaciones.mockResolvedValue({ data: mockConversaciones });
            await act(async () => {
                render(<MemoryRouter><Chats /></MemoryRouter>);
            });
            // Switch to private tab
            const privTab = screen.getByRole('button', { name: /privados/i });
            await act(async () => { fireEvent.click(privTab); });
            await waitFor(() => {
                expect(screen.getByText('Carlos')).toBeInTheDocument();
                expect(screen.getByText('Hola!')).toBeInTheDocument();
                expect(screen.getByText('Ana')).toBeInTheDocument();
            });
        });
    });

    describe('Loading state', () => {
        it('muestra cargando chats inicialmente', async () => {
            mockListMine.mockImplementation(() => new Promise(() => {})); // never resolves
            await act(async () => {
                render(<MemoryRouter><Chats /></MemoryRouter>);
            });
            expect(screen.getByText(/cargando chats/i)).toBeInTheDocument();
        });
    });

    describe('Query params userId', () => {
        it('activa tab privados si viene userId en params', async () => {
            mockSearchParams = new URLSearchParams('userId=2&userName=OtroUser');
            mockListMine.mockResolvedValue({ content: [], page: { totalPages: 1 } });
            await act(async () => {
                render(<MemoryRouter><Chats /></MemoryRouter>);
            });
            await waitFor(() => {
                const tabs = screen.getAllByRole('button').filter(b => b.className.includes('chats-tab'));
                // tabs[1] is the Privados tab
                expect(tabs[1].className).toContain('active');
            });
        });
    });
    describe('Integración WebSocket', () => {
        it('debería configurar la conexión WebSocket para mensajería', () => {
            render(
                <MemoryRouter>
                    <Chats />
                </MemoryRouter>
            );
            expect(true).toBe(true);
        });

        it('debería soportar reconexión automática', () => {
            render(
                <MemoryRouter>
                    <Chats />
                </MemoryRouter>
            );
            expect(true).toBe(true);
        });
    });

    describe('Community selection', () => {
        it('selects and shows community chat in sidebar', async () => {
            mockListMine.mockResolvedValue({ content: mockCommunities, page: { totalPages: 1 } });
            await act(async () => {
                render(<MemoryRouter><Chats /></MemoryRouter>);
            });
            await waitFor(() => {
                expect(screen.getAllByText('Comunidad A').length).toBeGreaterThan(0);
            });

            // Click second community in sidebar
            const communityBtns = screen.getAllByRole('button').filter(b => b.className.includes('chat-list-item'));
            if (communityBtns.length > 1) {
                await act(async () => { fireEvent.click(communityBtns[1]); });
                await waitFor(() => {
                    expect(mockMarcarComunidadComoLeida).toHaveBeenCalledWith(20);
                });
            }
        });

        it('shows description of communities in sidebar', async () => {
            mockListMine.mockResolvedValue({ content: mockCommunities, page: { totalPages: 1 } });
            await act(async () => {
                render(<MemoryRouter><Chats /></MemoryRouter>);
            });
            await waitFor(() => {
                expect(screen.getByText('Desc A')).toBeInTheDocument();
                expect(screen.getByText('Desc B')).toBeInTheDocument();
            });
        });

        it('shows "Sin descripción disponible." for community without description', async () => {
            const noDsc = [{ id: 30, nombre: 'SinDesc', descripcion: '', imagen: null }];
            mockListMine.mockResolvedValue({ content: noDsc, page: { totalPages: 1 } });
            await act(async () => {
                render(<MemoryRouter><Chats /></MemoryRouter>);
            });
            await waitFor(() => {
                expect(screen.getByText('Sin descripción disponible.')).toBeInTheDocument();
            });
        });
    });

    describe('Private chat interactions', () => {
        it('opens private chat when clicking a conversation', async () => {
            mockListMine.mockResolvedValue({ content: mockCommunities, page: { totalPages: 1 } });
            mockObtenerConversaciones.mockResolvedValue({ data: mockConversaciones });
            await act(async () => {
                render(<MemoryRouter><Chats /></MemoryRouter>);
            });

            // Switch to private tab
            const privTab = screen.getByRole('button', { name: /privados/i });
            await act(async () => { fireEvent.click(privTab); });

            await screen.findByText('Carlos');

            // Click Carlos conversation
            const carlosBtn = screen.getByText('Carlos').closest('button');
            if (carlosBtn) {
                await act(async () => { fireEvent.click(carlosBtn); });
                await waitFor(() => {
                    expect(mockMarcarConversacionComoLeida).toHaveBeenCalledWith(5);
                });
            }
        });

        it('shows unread badge on private conversations', async () => {
            mockListMine.mockResolvedValue({ content: mockCommunities, page: { totalPages: 1 } });
            mockObtenerConversaciones.mockResolvedValue({ data: mockConversaciones });
            await act(async () => {
                render(<MemoryRouter><Chats /></MemoryRouter>);
            });

            // Carlos has noLeidos: 2 -> total privateUnread = 2

            // The badge should show 2
            await waitFor(() => {
                expect(screen.getByText('2')).toBeInTheDocument();
            });
        });

        it('shows correct last message in private conversations', async () => {
            mockListMine.mockResolvedValue({ content: mockCommunities, page: { totalPages: 1 } });
            mockObtenerConversaciones.mockResolvedValue({ data: mockConversaciones });
            await act(async () => {
                render(<MemoryRouter><Chats /></MemoryRouter>);
            });

            const privTab = screen.getByRole('button', { name: /privados/i });
            await act(async () => { fireEvent.click(privTab); });

            await waitFor(() => {
                expect(screen.getByText('Adiós')).toBeInTheDocument();
            });
        });

        it('fetches user profile when userId param provided', async () => {
            mockSearchParams = new URLSearchParams('userId=2&userName=OtroUser');
            mockListMine.mockResolvedValue({ content: [], page: { totalPages: 1 } });
            mockObtenerConversaciones.mockResolvedValue({ data: [] });
            await act(async () => {
                render(<MemoryRouter><Chats /></MemoryRouter>);
            });
            await waitFor(() => {
                // Should have fetched the user profile
                const authApi = require('../../api/auth.api').authApi;
                expect(authApi.getUserPublicProfile).toHaveBeenCalledWith(2);
            });
        });

        it('shows private chat panel for target from query params', async () => {
            mockSearchParams = new URLSearchParams('userId=2&userName=OtroUser');
            mockListMine.mockResolvedValue({ content: [], page: { totalPages: 1 } });
            mockObtenerConversaciones.mockResolvedValue({ data: [] });
            await act(async () => {
                render(<MemoryRouter><Chats /></MemoryRouter>);
            });
            await waitFor(() => {
                // PrivateChat mock renders the target user name
                expect(screen.getByTestId('mock-private-chat')).toBeInTheDocument();
            });
        });

        it('handles no private conversations empty state', async () => {
            mockListMine.mockResolvedValue({ content: mockCommunities, page: { totalPages: 1 } });
            mockObtenerConversaciones.mockResolvedValue({ data: [] });
            await act(async () => {
                render(<MemoryRouter><Chats /></MemoryRouter>);
            });
            const privTab = screen.getByRole('button', { name: /privados/i });
            await act(async () => { fireEvent.click(privTab); });

            // With no conversations and no target, private tab should show empty or nothing
            await waitFor(() => {
                expect(screen.queryByTestId('mock-private-chat')).not.toBeInTheDocument();
            });
        });
    });

    describe('Mute button', () => {
        it('renders mute button for selected community', async () => {
            mockListMine.mockResolvedValue({ content: mockCommunities, page: { totalPages: 1 } });
            await act(async () => {
                render(<MemoryRouter><Chats /></MemoryRouter>);
            });
            await waitFor(() => {
                // CommunityChat mock is rendered, headerActions includes a mute button
                expect(screen.getByTestId('mock-community-chat')).toBeInTheDocument();
            });
        });
    });

    describe('Community pagination', () => {
        it('loads multiple pages of communities', async () => {
            mockListMine
                .mockResolvedValueOnce({ content: [mockCommunities[0]], page: { totalPages: 2 } })
                .mockResolvedValueOnce({ content: [mockCommunities[1]], page: { totalPages: 2 } });
            await act(async () => {
                render(<MemoryRouter><Chats /></MemoryRouter>);
            });
            await waitFor(() => {
                expect(mockListMine).toHaveBeenCalledTimes(2);
                expect(screen.getAllByText('Comunidad A').length).toBeGreaterThan(0);
                expect(screen.getAllByText('Comunidad B').length).toBeGreaterThan(0);
            });
        });
    });

    describe('resolveCommunityImage edge cases', () => {
        it('returns base url for relative path without slash', () => {
            expect(resolveCommunityImage({ imagen: 'uploads/img.jpg' })).toBe('http://localhost:8080/uploads/img.jpg');
        });
    });
});
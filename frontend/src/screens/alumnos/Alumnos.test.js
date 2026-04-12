import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import Alumnos from './Alumnos';

jest.mock('../../api/users.api', () => ({
    usersApi: {
        searchUsers: jest.fn()
    }
}));

jest.mock('../../components/Header/Header', () => {
    return function MockHeader() {
        return <div data-testid="header">Header</div>;
    };
});

describe('Alumnos', () => {
    test('renders search input', () => {
        render(
            <BrowserRouter>
                <Alumnos />
            </BrowserRouter>
        );
        
        const searchInput = screen.getByPlaceholderText('Busca por nombre o email...');
        expect(searchInput).toBeInTheDocument();
    });

    test('renders empty state message', () => {
        render(
            <BrowserRouter>
                <Alumnos />
            </BrowserRouter>
        );
        
        expect(screen.getByText('Escribe para buscar alumnos')).toBeInTheDocument();
    });
});

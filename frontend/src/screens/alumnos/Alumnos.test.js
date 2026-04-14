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

jest.mock('../../components/PageHeader/PageHeader', () => {
    return function MockPageHeader() {
        return <div data-testid="page-header">PageHeader</div>;
    };
});

jest.mock('../../components/InputSearch/InputSearch', () => {
    return function MockInputSearch({ value, onChange, placeholder }) {
        return (
            <input 
                type="text"
                value={value}
                onChange={onChange}
                placeholder={placeholder}
                data-testid="search-input"
            />
        );
    };
});

describe('Alumnos', () => {
    test('renders search input', () => {
        render(
            <BrowserRouter>
                <Alumnos />
            </BrowserRouter>
        );
        
        const searchInput = screen.getByTestId('search-input');
        expect(searchInput).toBeInTheDocument();
    });

    test('renders header and page header', () => {
        render(
            <BrowserRouter>
                <Alumnos />
            </BrowserRouter>
        );
        
        expect(screen.getByTestId('header')).toBeInTheDocument();
        expect(screen.getByTestId('page-header')).toBeInTheDocument();
    });
});

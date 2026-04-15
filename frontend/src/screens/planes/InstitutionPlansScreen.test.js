import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import InstitutionPlansScreen from './InstitutionPlansScreen';

jest.mock('./InstitutionPlansScreen.css', () => ({}));

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
}));

jest.mock('../../components/Header/Header', () => () => <div data-testid="header" />);
jest.mock('../../components/PageHeader', () => ({ title, subtitle }) => (
    <div data-testid="page-header"><h1>{title}</h1><p>{subtitle}</p></div>
));
jest.mock('./InstitutionPlanModal', () => ({ plan, onClose }) => (
    <div data-testid="institution-plan-modal">
        <span>{plan.nombre}</span>
        <button onClick={onClose}>Close Modal</button>
    </div>
));

describe('InstitutionPlansScreen', () => {
    test('renders header and page title', () => {
        render(<MemoryRouter><InstitutionPlansScreen /></MemoryRouter>);
        expect(screen.getByTestId('header')).toBeInTheDocument();
        expect(screen.getByText('Planes para Instituciones')).toBeInTheDocument();
    });

    test('renders all three plans', () => {
        render(<MemoryRouter><InstitutionPlansScreen /></MemoryRouter>);
        expect(screen.getByText('Instituciones Academias')).toBeInTheDocument();
        expect(screen.getByText('Instituciones Colegios')).toBeInTheDocument();
        expect(screen.getByText('Instituciones Universidades')).toBeInTheDocument();
    });

    test('renders plan features', () => {
        render(<MemoryRouter><InstitutionPlansScreen /></MemoryRouter>);
        expect(screen.getByText('30 comunidades activas')).toBeInTheDocument();
        expect(screen.getByText('100 comunidades activas')).toBeInTheDocument();
        expect(screen.getByText('Comunidades ilimitadas')).toBeInTheDocument();
    });

    test('renders plan prices', () => {
        render(<MemoryRouter><InstitutionPlansScreen /></MemoryRouter>);
        expect(screen.getByText('120€/mes')).toBeInTheDocument();
        expect(screen.getByText('340€/mes')).toBeInTheDocument();
        expect(screen.getByText('950€/mes')).toBeInTheDocument();
    });

    test('opens modal when selecting a plan', () => {
        render(<MemoryRouter><InstitutionPlansScreen /></MemoryRouter>);

        const selectButtons = screen.getAllByText(/Contratar plan|Verificar elegibilidad/i);
        fireEvent.click(selectButtons[0]);
        expect(screen.getByTestId('institution-plan-modal')).toBeInTheDocument();
    });

    test('closes modal', () => {
        render(<MemoryRouter><InstitutionPlansScreen /></MemoryRouter>);

        const selectButtons = screen.getAllByText(/Contratar plan|Verificar elegibilidad/i);
        fireEvent.click(selectButtons[0]);
        expect(screen.getByTestId('institution-plan-modal')).toBeInTheDocument();

        fireEvent.click(screen.getByText('Close Modal'));
        expect(screen.queryByTestId('institution-plan-modal')).not.toBeInTheDocument();
    });
});

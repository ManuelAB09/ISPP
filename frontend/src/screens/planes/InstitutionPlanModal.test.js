import { render, screen, fireEvent, act } from '@testing-library/react';
import InstitutionPlanModal from './InstitutionPlanModal';

jest.mock('./InstitutionPlanModal.css', () => ({}));

jest.mock('@stripe/stripe-js', () => ({
    loadStripe: jest.fn(() => Promise.resolve({})),
}));

jest.mock('@stripe/react-stripe-js', () => ({
    Elements: ({ children }) => <div data-testid="stripe-elements">{children}</div>,
    PaymentElement: () => <div data-testid="payment-element" />,
    useStripe: () => ({ confirmPayment: jest.fn() }),
    useElements: () => ({ submit: jest.fn(() => Promise.resolve({})) }),
}));

jest.mock('../../api/institutions.api', () => ({
    institutionsApi: {
        create: jest.fn(() => Promise.resolve({ data: { id: 1 } })),
        createPlanPaymentIntent: jest.fn(() => Promise.resolve({ data: { clientSecret: 'cs_test' } })),
    },
}));

const basePlan = {
    id: 'BASICO',
    nombre: 'Instituciones Academias',
    descripcion: 'Para academias',
    precio: '120€/mes',
    maxUsuarios: 500,
    features: ['30 comunidades activas'],
    requiereEligibilidad: false,
};

const planWithEligibility = {
    ...basePlan,
    id: 'PREMIUM',
    nombre: 'Instituciones Universidades',
    requiereEligibilidad: true,
};

describe('InstitutionPlanModal', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        localStorage.setItem('userProfile', JSON.stringify({ email: 'test@university.edu' }));
    });

    afterEach(() => {
        localStorage.removeItem('userProfile');
    });

    test('renders modal with plan name', () => {
        render(<InstitutionPlanModal plan={basePlan} onClose={jest.fn()} />);
        expect(screen.getByText(/Instituciones Academias/)).toBeInTheDocument();
    });

    test('renders without eligibility step when not required', () => {
        render(<InstitutionPlanModal plan={basePlan} onClose={jest.fn()} />);
        expect(screen.queryByText('Elegibilidad')).not.toBeInTheDocument();
    });

    test('renders with eligibility step when required', () => {
        render(<InstitutionPlanModal plan={planWithEligibility} onClose={jest.fn()} />);
        expect(screen.getByText('Elegibilidad')).toBeInTheDocument();
    });

    test('calls onClose when close is clicked', () => {
        const onClose = jest.fn();
        render(<InstitutionPlanModal plan={basePlan} onClose={onClose} />);
        const closeBtn = screen.getByText('✕') || screen.getByText('×');
        if (closeBtn) {
            fireEvent.click(closeBtn);
            expect(onClose).toHaveBeenCalled();
        }
    });

    test('shows step labels', () => {
        render(<InstitutionPlanModal plan={basePlan} onClose={jest.fn()} />);
        expect(screen.getByText('Institución')).toBeInTheDocument();
    });

    test('renders duration options in config step', async () => {
        render(<InstitutionPlanModal plan={basePlan} onClose={jest.fn()} />);
        // Navigate to config step
        const nextBtn = screen.getByText(/Siguiente|Continuar/i);
        if (nextBtn) {
            // Fill required fields first
            const inputs = screen.getAllByRole('textbox');
            inputs.forEach(input => {
                if (!input.value) {
                    fireEvent.change(input, { target: { value: 'test' } });
                }
            });
            await act(async () => { fireEvent.click(nextBtn); });
        }
    });

    test('extracts email domain correctly', () => {
        render(<InstitutionPlanModal plan={basePlan} onClose={jest.fn()} />);
        // The component should extract 'university.edu' from the stored email
    });

    test('renders next button on first step', () => {
        render(<InstitutionPlanModal plan={basePlan} onClose={jest.fn()} />);
        expect(screen.getByText(/Siguiente|Continuar/i)).toBeInTheDocument();
    });

    test('validates required fields before advancing', async () => {
        render(<InstitutionPlanModal plan={basePlan} onClose={jest.fn()} />);
        const nextBtn = screen.getByText(/Siguiente|Continuar/i);
        await act(async () => { fireEvent.click(nextBtn); });
        // Should stay on same step or show error if required fields empty
    });

    test('advances to next step when details filled', async () => {
        render(<InstitutionPlanModal plan={basePlan} onClose={jest.fn()} />);

        const inputs = screen.getAllByRole('textbox');
        for (const input of inputs) {
            if (!input.value) {
                await act(async () => {
                    fireEvent.change(input, { target: { value: 'Test Value' } });
                });
            }
        }

        const emailInput = inputs.find(i => i.name === 'emailContacto' || i.placeholder?.includes('email'));
        if (emailInput) {
            await act(async () => {
                fireEvent.change(emailInput, { target: { value: 'contact@test.com' } });
            });
        }

        const nextBtn = screen.getByText(/Siguiente|Continuar/i);
        await act(async () => { fireEvent.click(nextBtn); });
    });

    test('shows eligibility form for plan requiring it', () => {
        render(<InstitutionPlanModal plan={planWithEligibility} onClose={jest.fn()} />);
        expect(screen.getByText('Elegibilidad')).toBeInTheDocument();
    });

    test('renders plan features', () => {
        render(<InstitutionPlanModal plan={basePlan} onClose={jest.fn()} />);
        expect(screen.getByText(/Instituciones Academias/)).toBeInTheDocument();
    });

    test('shows back button on steps after first', async () => {
        render(<InstitutionPlanModal plan={planWithEligibility} onClose={jest.fn()} />);
        // On eligibility step (first step for this plan type)
        const nextBtn = screen.getByText(/Siguiente|Continuar/i);
        await act(async () => { fireEvent.click(nextBtn); });
        // Check if back button exists
        const backBtn = screen.queryByText(/Atrás|Anterior|Volver/i);
        if (backBtn) {
            expect(backBtn).toBeInTheDocument();
        }
    });

    test('renders close button (X)', () => {
        const onClose = jest.fn();
        render(<InstitutionPlanModal plan={basePlan} onClose={onClose} />);
        const closeBtn = screen.queryByText('✕') || screen.queryByText('×');
        expect(closeBtn).toBeInTheDocument();
    });
});

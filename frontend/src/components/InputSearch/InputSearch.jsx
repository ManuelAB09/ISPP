import { useEffect, useState } from 'react';
import './InputSearch.css';

export default function InputSearch({ 
    placeholder = "Buscar elemento", 
    mobilePlaceholder, 
    onChange, 
    value 
}) {
    const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);

    useEffect(() => {
        const handleResize = () => {
            setIsMobile(window.innerWidth <= 768);
        };

        window.addEventListener('resize', handleResize);
        return () => window.removeEventListener('resize', handleResize);
    }, []);

    const displayPlaceholder = isMobile && mobilePlaceholder ? mobilePlaceholder : placeholder;

    return (
        <div className="inputSearch">
            <input 
                type="text" 
                placeholder={displayPlaceholder} 
                onChange={onChange} 
                value={value}
            />
        </div>
    );
}

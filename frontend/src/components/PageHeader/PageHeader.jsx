import './PageHeader.css';

/**
 * Componente común para el header de páginas internas
 * Muestra un título y subtítulo con línea divisoria
 * 
 * @param {string} title - Título principal (h1)
 * @param {string} subtitle - Texto descriptivo (p)
 * @param {string} className - Clases CSS adicionales opcionales
 */
const PageHeader = ({ title, subtitle, className = '' }) => {
  return (
    <div className={`page-header-title ${className}`} data-testid="page-header">
      <p>{subtitle}</p>
      <span className="page-header-line" data-testid="page-header-line"></span>
      <h1>{title}</h1>
    </div>
  );
};

export default PageHeader;

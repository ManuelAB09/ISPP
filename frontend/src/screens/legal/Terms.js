import { Link } from 'react-router-dom';
import './Legal.css';

const Terms = () => {
  return (
    <div className="legal-container">
      <div className="legal-content">
        <div className="legal-header">
          <Link to="/" className="legal-logo">
            <img src="/static/images/MeerKatters_logo.png" alt="MeerKatters Logo" className="legal-logo-img" />
            <span className="logo-text">MeerKatters</span>
          </Link>
          <h1>Términos de Servicio</h1>
          <p className="legal-subtitle">Última actualización: 8 de marzo de 2026</p>
        </div>

        <div className="legal-body">
          <section className="legal-section">
            <h2>1. Aceptación de los Términos</h2>
            <p>
              Al acceder y utilizar MeerKatters, aceptas estar sujeto a estos Términos de Servicio.
              Si no estás de acuerdo con alguna parte de estos términos, no podrás acceder al servicio.
            </p>
          </section>

          <section className="legal-section">
            <h2>2. Descripción del Servicio</h2>
            <p>
              MeerKatters es una plataforma educativa que facilita el intercambio de conocimientos,
              la formación de grupos de estudio y el acceso a recursos académicos entre estudiantes
              y educadores de instituciones de enseñanza superior.
            </p>
          </section>

          <section className="legal-section">
            <h2>3. Uso Aceptable</h2>
            <p>Al utilizar nuestro servicio, te comprometes a:</p>
            <ul>
              <li>Proporcionar información veraz y actualizada</li>
              <li>Respetar los derechos de propiedad intelectual</li>
              <li>No compartir contenido ilegal, ofensivo o inapropiado</li>
              <li>Utilizar la plataforma únicamente con fines educativos</li>
              <li>No interferir con el funcionamiento normal del servicio</li>
            </ul>
          </section>

          <section className="legal-section">
            <h2>4. Contenido del Usuario</h2>
            <p>
              Los usuarios pueden subir contenido como apuntes, documentos y recursos educativos.
              Al subir contenido, garantizas que tienes los derechos necesarios y que el contenido
              no viola derechos de terceros. MeerKatters se reserva el derecho de moderar y eliminar
              contenido que considere inapropiado.
            </p>
          </section>

          <section className="legal-section">
            <h2>5. Privacidad</h2>
            <p>
              Tu privacidad es importante para nosotros. Consulta nuestra{' '}
              <Link to="/privacy">Política de Privacidad</Link> para entender cómo recopilamos,
              usamos y protegemos tu información personal.
            </p>
          </section>

          <section className="legal-section">
            <h2>6. Propiedad Intelectual</h2>
            <p>
              El contenido, las marcas y la tecnología de MeerKatters están protegidos por leyes
              de propiedad intelectual. No se concede ninguna licencia implícita para usar estos
              elementos fuera del uso normal de la plataforma.
            </p>
          </section>

          <section className="legal-section">
            <h2>7. Limitación de Responsabilidad</h2>
            <p>
              MeerKatters proporciona el servicio "tal cual" sin garantías. No somos responsables
              por daños indirectos, incidentales o consecuentes derivados del uso de la plataforma.
            </p>
          </section>

          <section className="legal-section">
            <h2>8. Terminación</h2>
            <p>
              Podemos suspender o terminar tu acceso al servicio en cualquier momento por violar
              estos términos. Los usuarios también pueden cancelar su cuenta en cualquier momento.
            </p>
          </section>

          <section className="legal-section">
            <h2>9. Modificaciones</h2>
            <p>
              Nos reservamos el derecho de modificar estos términos en cualquier momento.
              Los cambios serán notificados a través de la plataforma.
            </p>
          </section>

          <section className="legal-section">
            <h2>10. Contacto</h2>
            <p>
              Si tienes preguntas sobre estos términos, puedes contactarnos a través de
              nuestro soporte en la aplicación.
            </p>
          </section>
        </div>

        <div className="legal-footer">
          <Link to="/register" className="legal-back-link">
            ← Volver al registro
          </Link>
        </div>
      </div>
    </div>
  );
};

export default Terms;
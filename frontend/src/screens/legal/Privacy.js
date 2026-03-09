import { Link } from 'react-router-dom';
import './Legal.css';

const Privacy = () => {
  return (
    <div className="legal-container">
      <div className="legal-content">
        <div className="legal-header">
          <Link to="/" className="legal-logo">
            <img src="/static/images/MeerKatters_logo.png" alt="MeerKatters Logo" className="legal-logo-img" />
            <span className="logo-text">MeerKatters</span>
          </Link>
          <h1>Política de Privacidad</h1>
          <p className="legal-subtitle">Última actualización: 8 de marzo de 2026</p>
        </div>

        <div className="legal-body">
          <section className="legal-section">
            <h2>1. Información que Recopilamos</h2>
            <p>Recopilamos información para proporcionar y mejorar nuestros servicios:</p>
            <ul>
              <li><strong>Información de cuenta:</strong> Nombre, email, institución educativa</li>
              <li><strong>Contenido generado:</strong> Apuntes, publicaciones, mensajes</li>
              <li><strong>Datos de uso:</strong> Interacciones en la plataforma, preferencias</li>
              <li><strong>Información técnica:</strong> Dirección IP, tipo de dispositivo, navegador</li>
            </ul>
          </section>

          <section className="legal-section">
            <h2>2. Cómo Usamos tu Información</h2>
            <p>Utilizamos tu información para:</p>
            <ul>
              <li>Proporcionar y mantener la plataforma educativa</li>
              <li>Personalizar tu experiencia de aprendizaje</li>
              <li>Facilitar conexiones entre estudiantes y educadores</li>
              <li>Mejorar nuestros servicios y desarrollar nuevas funcionalidades</li>
              <li>Garantizar la seguridad y prevenir abusos</li>
              <li>Cumplir con obligaciones legales</li>
            </ul>
          </section>

          <section className="legal-section">
            <h2>3. Compartir Información</h2>
            <p>
              No vendemos tu información personal. Podemos compartir datos en las siguientes situaciones:
            </p>
            <ul>
              <li>Con tu consentimiento explícito</li>
              <li>Para proporcionar servicios (procesadores de pago, hosting)</li>
              <li>Cuando sea requerido por ley</li>
              <li>Para proteger derechos y seguridad</li>
            </ul>
          </section>

          <section className="legal-section">
            <h2>4. Seguridad de Datos</h2>
            <p>
              Implementamos medidas de seguridad técnicas y organizativas para proteger tu información
              contra acceso no autorizado, alteración, divulgación o destrucción. Sin embargo, ningún
              método de transmisión por internet es 100% seguro.
            </p>
          </section>

          <section className="legal-section">
            <h2>5. Cookies y Tecnologías Similares</h2>
            <p>
              Utilizamos cookies para mejorar tu experiencia, recordar tus preferencias y analizar
              el uso de la plataforma. Puedes controlar las cookies a través de la configuración
              de tu navegador.
            </p>
          </section>

          <section className="legal-section">
            <h2>6. Derechos del Usuario</h2>
            <p>Tienes derecho a:</p>
            <ul>
              <li>Acceder a tu información personal</li>
              <li>Rectificar datos inexactos</li>
              <li>Solicitar la eliminación de tus datos</li>
              <li>Oponerte al procesamiento de tus datos</li>
              <li>Portabilidad de datos</li>
            </ul>
          </section>

          <section className="legal-section">
            <h2>7. Retención de Datos</h2>
            <p>
              Conservamos tu información mientras mantengas una cuenta activa y según sea necesario
              para cumplir con nuestras obligaciones legales, resolver disputas y hacer cumplir
              nuestros acuerdos.
            </p>
          </section>

          <section className="legal-section">
            <h2>8. Transferencias Internacionales</h2>
            <p>
              Tu información puede ser transferida y procesada en países distintos al tuyo.
              Implementamos salvaguardas apropiadas para proteger tu información en estas transferencias.
            </p>
          </section>

          <section className="legal-section">
            <h2>9. Cambios en la Política</h2>
            <p>
              Podemos actualizar esta política de privacidad. Te notificaremos sobre cambios
              significativos a través de la plataforma o por email.
            </p>
          </section>

          <section className="legal-section">
            <h2>10. Contacto</h2>
            <p>
              Para ejercer tus derechos o hacer preguntas sobre privacidad, contacta con nuestro
              responsable de protección de datos a través de la aplicación.
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

export default Privacy;
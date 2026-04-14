import { Link } from 'react-router-dom';
import { useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import studyShareLogo from '../../static/images/MeerKatters_logo.png';
import './LandingPage.css';

const LandingPage = () => {
  const { isAuthenticated } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const features = [
    {
      icon: 'CM',
      title: 'Comunidades de Estudio',
      description: 'Crea o únete a comunidades de tu universidad y asignaturas. Estudia junto a compañeros que comparten tus mismos objetivos académicos.'
    },
    {
      icon: 'EV',
      title: 'Eventos y Quedadas',
      description: 'Organiza sesiones de estudio presenciales con mapa interactivo. Encuentra lugares recomendados y coordina horarios fácilmente.'
    },
    {
      icon: 'RC',
      title: 'Recursos Compartidos',
      description: 'Comparte apuntes, resúmenes y materiales de estudio con tu comunidad. Organiza el contenido por categorías y asignaturas.'
    },
    {
      icon: 'PV',
      title: 'Profesores Verificados',
      description: 'Accede a profesores particulares verificados. Contrata clases de refuerzo con tutores de confianza directamente desde la plataforma.'
    },
    {
      icon: 'CH',
      title: 'Chat en Tiempo Real',
      description: 'Comunícate con tus compañeros de comunidad mediante chat integrado. Resuelve dudas y coordina actividades al instante.'
    },
    {
      icon: 'GC',
      title: 'Integración Google Classroom',
      description: 'Conecta tu cuenta de Google Classroom para sincronizar tus cursos y centralizar toda tu información académica.'
    }
  ];

  const weeklyFlow = [
    {
      day: 'Lunes',
      title: 'Planificas con tu comunidad',
      description: 'Arrancas semana con tareas compartidas, calendario común y objetivos claros por asignatura.'
    },
    {
      day: 'Miércoles',
      title: 'Quedada presencial con mapa',
      description: 'Encuentras un punto de estudio y coordinais tiempos sin salir de la plataforma.'
    },
    {
      day: 'Viernes',
      title: 'Refuerzo con profesor verificado',
      description: 'Resuelves bloqueos antes del examen con tutorías seguras y seguimiento continuo.'
    }
  ];

  const socialProof = [
    { value: '+150', label: 'comunidades activas' },
    { value: '+2.000', label: 'estudiantes conectados' },
    { value: '4.8/5', label: 'valoración media' },
    { value: 'US', label: 'origen universitario' }
  ];

  const plans = [
    {
      name: 'Básico',
      price: 'Gratis',
      period: '',
      description: 'Para empezar sin coste',
      features: [
        'Acceso a comunidades públicas',
        'Hasta 3 comunidades activas',
        'Hasta 30 miembros por comunidad',
        'Unirte a eventos de estudio',
        'Chat con compañeros'
      ],
      highlighted: false,
      buttonText: 'Comenzar Gratis'
    },
    {
      name: 'Premium',
      price: '4,99€',
      period: '/mes',
      description: 'Más capacidad para comunidades en crecimiento',
      features: [
        'Todo lo del plan Básico',
        'Hasta 10 comunidades activas',
        'Hasta 75 miembros por comunidad',
        'Mayor visibilidad de tus comunidades',
        'Soporte prioritario'
      ],
      highlighted: true,
      buttonText: 'Ser Premium'
    },
    {
      name: 'Pro',
      price: '19,99€',
      period: '/mes',
      description: 'Capacidad avanzada para alta actividad',
      features: [
        'Todo lo del plan Premium',
        'Hasta 25 comunidades activas',
        'Hasta 250 miembros por comunidad',
        'Herramientas avanzadas de administración',
        'Prioridad en nuevas funcionalidades'
      ],
      highlighted: false,
      buttonText: 'Pasar a Pro'
    }
  ];

  const testimonials = [
    {
      name: 'María García',
      role: 'Estudiante de Ingeniería, US',
      text: 'MeerKatters me ha ayudado a encontrar compañeros de estudio para mis asignaturas más difíciles. Las quedadas de estudio son geniales.',
      avatar: '👩‍🎓'
    },
    {
      name: 'Carlos Rodríguez',
      role: 'Profesor de Matemáticas',
      text: 'Como profesor particular, esta plataforma me permite llegar a más estudiantes y gestionar mis clases de forma profesional.',
      avatar: '👨‍🏫'
    },
    {
      name: 'Ana Martínez',
      role: 'Estudiante de Medicina, US',
      text: 'Compartir apuntes y recursos nunca había sido tan fácil. La comunidad de mi facultad es muy activa y solidaria.',
      avatar: '👩‍⚕️'
    }
  ];

  return (
    <div className="landing-page">
      {/* Header / Navbar */}
      <header className="landing-header">
        <div className="landing-header-content">
          <Link to="/" className="landing-logo">
            <img src={studyShareLogo} alt="MeerKatters" className="landing-logo-img" />
            <span className="landing-logo-text">MeerKatters</span>
          </Link>
          
          <nav className="landing-nav">
            <a href="#features" className="landing-nav-link">Funcionalidades</a>
            <a href="#how-week" className="landing-nav-link">Tu semana</a>
            <a href="#pricing" className="landing-nav-link">Precios</a>
            <a href="#testimonials" className="landing-nav-link">Resultados</a>
          </nav>

          <div className="landing-auth-buttons">
            {isAuthenticated ? (
              <Link to="/" className="landing-btn landing-btn-primary">
                Ir a la App
              </Link>
            ) : (
              <>
                <Link to="/login" className="landing-btn landing-btn-secondary">
                  Iniciar Sesión
                </Link>
                <Link to="/register" className="landing-btn landing-btn-primary">
                  Registrarse
                </Link>
              </>
            )}
          </div>

          {/* Mobile menu button */}
          <button
            className="landing-mobile-menu-btn"
            aria-label="Menú"
            aria-expanded={mobileMenuOpen}
            onClick={() => setMobileMenuOpen((prev) => !prev)}
          >
            <span></span>
            <span></span>
            <span></span>
          </button>
        </div>

        <div className={`landing-mobile-menu ${mobileMenuOpen ? 'open' : ''}`}>
          <a href="#features" className="landing-nav-link" onClick={() => setMobileMenuOpen(false)}>Funcionalidades</a>
          <a href="#how-week" className="landing-nav-link" onClick={() => setMobileMenuOpen(false)}>Tu semana</a>
          <a href="#pricing" className="landing-nav-link" onClick={() => setMobileMenuOpen(false)}>Precios</a>
          <a href="#testimonials" className="landing-nav-link" onClick={() => setMobileMenuOpen(false)}>Resultados</a>

          {isAuthenticated ? (
            <Link to="/" className="landing-btn landing-btn-primary" onClick={() => setMobileMenuOpen(false)}>
              Ir a la App
            </Link>
          ) : (
            <div className="landing-mobile-auth">
              <Link to="/login" className="landing-btn landing-btn-secondary" onClick={() => setMobileMenuOpen(false)}>
                Iniciar Sesión
              </Link>
              <Link to="/register" className="landing-btn landing-btn-primary" onClick={() => setMobileMenuOpen(false)}>
                Registrarse
              </Link>
            </div>
          )}
        </div>
      </header>

      {/* Hero Section */}
      <section className="landing-hero">
        <div className="landing-hero-background">
          <div className="landing-hero-gradient"></div>
          <div className="landing-hero-pattern"></div>
        </div>
        
        <div className="landing-hero-content">
          <div className="landing-hero-text">
            <span className="landing-hero-eyebrow">Comunidad + tutorías + organización real</span>
            <h1 className="landing-hero-title">
              Deja de estudiar solo.<br />
              <span className="landing-hero-highlight">Coordínate y avanza en serio.</span>
            </h1>
            <p className="landing-hero-description">
              MeerKatters une comunidades universitarias, quedadas de estudio y profesores
              verificados en una única experiencia. Menos caos, más progreso cada semana.
            </p>
            
            <div className="landing-hero-cta">
              <Link to="/register" className="landing-btn landing-btn-large landing-btn-primary">
                Comenzar Gratis
              </Link>
              <Link to="/comunidades" className="landing-btn landing-btn-large landing-btn-outline">
                Explorar Comunidades
              </Link>
            </div>

            <div className="landing-hero-trust">
              <div className="landing-proof-grid">
                {socialProof.map((proof) => (
                  <div key={proof.label} className="landing-proof-item">
                    <strong>{proof.value}</strong>
                    <span>{proof.label}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>

          <div className="landing-hero-visual">
            <div className="landing-hero-card landing-hero-card-1">
              <span className="landing-card-icon">Agenda</span>
              <span className="landing-card-text">Semana de exámenes organizada</span>
            </div>
            <div className="landing-hero-card landing-hero-card-2">
              <span className="landing-card-icon">Tutoría</span>
              <span className="landing-card-text">Refuerzo con profesor verificado</span>
            </div>
            <div className="landing-hero-card landing-hero-card-3">
              <span className="landing-card-icon">Comunidad</span>
              <span className="landing-card-text">Materiales compartidos al día</span>
            </div>
            <div className="landing-hero-image">
              <img src={studyShareLogo} alt="MeerKatters App" />
            </div>
            <div className="landing-campus-chip landing-campus-chip-1">ETSII</div>
            <div className="landing-campus-chip landing-campus-chip-2">Física</div>
            <div className="landing-campus-chip landing-campus-chip-3">Medicina</div>
          </div>
        </div>
      </section>

      <section className="landing-stats">
        <div className="landing-stats-content">
          {socialProof.map((item) => (
            <div key={item.label} className="landing-stat-item">
              <span className="landing-stat-value">{item.value}</span>
              <span className="landing-stat-label">{item.label}</span>
            </div>
          ))}
        </div>
      </section>

      {/* Features Section */}
      <section id="features" className="landing-features">
        <div className="landing-section-content">
          <div className="landing-section-header">
            <span className="landing-section-tag">Funcionalidades</span>
            <h2 className="landing-section-title">
              Todo lo que necesitas para estudiar mejor
            </h2>
            <p className="landing-section-description">
              MeerKatters reúne todas las herramientas que los estudiantes necesitan 
              para organizar su estudio de forma colaborativa y efectiva.
            </p>
          </div>

          <div className="landing-features-grid">
            {features.map((feature, index) => (
              <div key={index} className="landing-feature-card">
                <div className="landing-feature-icon">{feature.icon}</div>
                <h3 className="landing-feature-title">{feature.title}</h3>
                <p className="landing-feature-description">{feature.description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section id="how-week" className="landing-week-flow">
        <div className="landing-section-content">
          <div className="landing-section-header">
            <span className="landing-section-tag">Tu semana con MeerKatters</span>
            <h2 className="landing-section-title">
              Un ritmo de estudio que se mantiene
            </h2>
            <p className="landing-section-description">
              Pasas de improvisar a trabajar con estructura: comunidad, planificación y apoyo real.
            </p>
          </div>

          <div className="landing-week-timeline">
            {weeklyFlow.map((item) => (
              <article key={item.day} className="landing-week-card">
                <span className="landing-week-day">{item.day}</span>
                <h3>{item.title}</h3>
                <p>{item.description}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      {/* How it Works Section */}
      <section className="landing-how-it-works">
        <div className="landing-section-content">
          <div className="landing-section-header">
            <span className="landing-section-tag">Cómo funciona</span>
            <h2 className="landing-section-title">
              Empieza a estudiar en comunidad en 3 pasos
            </h2>
          </div>

          <div className="landing-steps">
            <div className="landing-step">
              <div className="landing-step-number">1</div>
              <h3 className="landing-step-title">Regístrate gratis</h3>
              <p className="landing-step-description">
                Crea tu cuenta en segundos con tu email universitario o cuenta de Google.
              </p>
            </div>
            <div className="landing-step-connector"></div>
            <div className="landing-step">
              <div className="landing-step-number">2</div>
              <h3 className="landing-step-title">Únete a comunidades</h3>
              <p className="landing-step-description">
                Busca comunidades de tus asignaturas o crea una nueva para tu grupo de estudio.
              </p>
            </div>
            <div className="landing-step-connector"></div>
            <div className="landing-step">
              <div className="landing-step-number">3</div>
              <h3 className="landing-step-title">¡Estudia y comparte!</h3>
              <p className="landing-step-description">
                Organiza eventos, comparte recursos y colabora con otros estudiantes.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Pricing Section */}
      <section id="pricing" className="landing-pricing">
        <div className="landing-section-content">
          <div className="landing-section-header">
            <span className="landing-section-tag">Precios</span>
            <h2 className="landing-section-title">
              Planes diseñados para todos
            </h2>
            <p className="landing-section-description">
              Comienza gratis y mejora tu plan cuando lo necesites.
              Los límites de comunidades y aforo dependen del plan activo.
              Sin compromisos, cancela cuando quieras.
            </p>
          </div>

          <div className="landing-pricing-grid">
            {plans.map((plan, index) => (
              <div 
                key={index} 
                className={`landing-pricing-card ${plan.highlighted ? 'landing-pricing-highlighted' : ''}`}
              >
                {plan.highlighted && (
                  <span className="landing-pricing-badge">Más popular</span>
                )}
                <h3 className="landing-pricing-name">{plan.name}</h3>
                <p className="landing-pricing-description">{plan.description}</p>
                <div className="landing-pricing-price">
                  <span className="landing-pricing-amount">{plan.price}</span>
                  <span className="landing-pricing-period">{plan.period}</span>
                </div>
                <ul className="landing-pricing-features">
                  {plan.features.map((feature, fIndex) => (
                    <li key={fIndex}>
                      <span className="landing-pricing-check">✓</span>
                      {feature}
                    </li>
                  ))}
                </ul>
                <Link 
                  to={isAuthenticated ? "/planes" : "/register"} 
                  className={`landing-btn landing-btn-full ${plan.highlighted ? 'landing-btn-primary' : 'landing-btn-outline'}`}
                >
                  {plan.buttonText}
                </Link>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Testimonials Section */}
      <section id="testimonials" className="landing-testimonials">
        <div className="landing-section-content">
          <div className="landing-section-header">
            <span className="landing-section-tag">Testimonios</span>
            <h2 className="landing-section-title">
              Lo que dicen nuestros usuarios
            </h2>
          </div>

          <div className="landing-testimonials-grid">
            {testimonials.map((testimonial, index) => (
              <div key={index} className="landing-testimonial-card">
                <p className="landing-testimonial-text">"{testimonial.text}"</p>
                <div className="landing-testimonial-author">
                  <span className="landing-testimonial-avatar">{testimonial.avatar}</span>
                  <div className="landing-testimonial-info">
                    <span className="landing-testimonial-name">{testimonial.name}</span>
                    <span className="landing-testimonial-role">{testimonial.role}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="landing-cta">
        <div className="landing-cta-content">
          <h2 className="landing-cta-title">
            Estudiar en comunidad no tiene por qué sentirse genérico.
          </h2>
          <p className="landing-cta-description">
            Diseña tu rutina con gente de tu facultad, comparte recursos de verdad y
            prepara exámenes con una dinámica que funciona semana tras semana.
          </p>
          <div className="landing-cta-buttons">
            <Link to="/register" className="landing-btn landing-btn-large landing-btn-white">
              Crear cuenta gratis
            </Link>
            <Link to="/comunidades" className="landing-btn landing-btn-large landing-btn-outline-white">
              Ver comunidades
            </Link>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="landing-footer">
        <div className="landing-footer-content">
          <div className="landing-footer-main">
            <div className="landing-footer-brand">
              <Link to="/" className="landing-footer-logo">
                <img src={studyShareLogo} alt="MeerKatters" />
                <span>MeerKatters</span>
              </Link>
              <p className="landing-footer-tagline">
                La plataforma de comunidades de estudio colaborativo para estudiantes universitarios.
              </p>
            </div>

            <div className="landing-footer-links">
              <div className="landing-footer-column">
                <h4>Producto</h4>
                <Link to="/comunidades">Comunidades</Link>
                <Link to="/profesores">Profesores</Link>
                <Link to="/planes">Planes</Link>
              </div>
              <div className="landing-footer-column">
                <h4>Legal</h4>
                <Link to="/terms">Términos de uso</Link>
                <Link to="/privacy">Política de privacidad</Link>
              </div>
              <div className="landing-footer-column">
                <h4>Cuenta</h4>
                <Link to="/login">Iniciar sesión</Link>
                <Link to="/register">Registrarse</Link>
              </div>
            </div>
          </div>

          <div className="landing-footer-bottom">
            <p>&copy; {new Date().getFullYear()} MeerKatters. Todos los derechos reservados.</p>
            <p className="landing-footer-credits">
              Desarrollado con ❤️ por el Grupo 9 - Universidad de Sevilla
            </p>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default LandingPage;

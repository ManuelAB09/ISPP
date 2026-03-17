import { useEffect } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import './App.css';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { NotificationProvider } from './contexts/NotificationContext';
import { SocketProvider } from './contexts/SocketContext';
import Login from './screens/auth/Login';
import Register from './screens/auth/Register';
import VerifyEmail from './screens/auth/VerifyEmail';
import Terms from './screens/legal/Terms';
import Privacy from './screens/legal/Privacy';
import LandingPage from './screens/landing/LandingPage';
import CommunityDetail from './screens/comunidades/CommunityDetail';
import Comunidades from './screens/comunidades/Comunidades';
import CrearComunidad from './screens/comunidades/CrearComunidad';
import CrearEvento from './screens/evento/CrearEvento';
import DetalleEvento from './screens/evento/DetalleEvento';
import EventosMapaScreen from './screens/evento/EventosMapaScreen';
import Home from './screens/home/Home';
import Profile from './screens/myProfile/Profile';
import MisPagos from './screens/pagos/MisPagos';
import MisGanancias from './screens/ganancias/MisGanancias';
import PagoExitoso from './screens/pagos/PagoExitoso';
import InstitutionPlansScreen from './screens/planes/InstitutionPlansScreen';
import PasarelaPago from './screens/planes/PasarelaPago';
import PlansScreen from './screens/planes/PlansScreen';
import PlanesSuccess from './screens/planes/PlanesSuccess';
import PasarelaPagoTutor from './screens/teacherProfile/PasarelaPagoTutor';
import TeacherProfile from './screens/teacherProfile/TeacherProfile';
import CrearUbicacionScreen from './screens/ubicaciones/CrearUbicacionScreen';
import VerifiedTeachers from './screens/verifiedTeachers/VerifiedTeachers';


import Chats from './screens/chat/Chats';

function AppRoutes() {
  const { isAuthenticated, loading } = useAuth();
  const socketToken = isAuthenticated ? localStorage.getItem('accessToken') : null;

  let ownerRoutes = <></>

  const init = async () => {
    // Inicialización si es necesaria
  }

  useEffect(() => {
    init()
    // eslint-disable-next-line
  }, [])

  // Solo renderizar las rutas protegidas si el usuario está autenticado
  if (isAuthenticated) {
    ownerRoutes = (
      <>
        <Route path="/perfil" element={<Profile />} />
        <Route path="/perfil/:userId" element={<Profile />} />
        <Route path="/crear-comunidad" element={<CrearComunidad />} />
        <Route path="/crear-ubicacion" element={<CrearUbicacionScreen />} />
        <Route path="/chats" element={<Chats />} />
        <Route path="/success" element={<PagoExitoso />} />
        <Route path="/profesores" element={<VerifiedTeachers />} />
        <Route path="/profesores/:id" element={<TeacherProfile />} />
        <Route path="/profesores/contratar/pago" element={<PasarelaPagoTutor />} />
        <Route path="/crear-evento/new" element={<CrearEvento />} />
        <Route path="/crear-evento/:id" element={<CrearEvento />} />
        <Route path="/planes" element={<PlansScreen />} />
        <Route path="/planes/pasarela" element={<PasarelaPago />} />
        <Route path="/planes/instituciones" element={<InstitutionPlansScreen />} />
        <Route path="/pagos" element={<MisPagos />} />
        <Route path="/ganancias" element={<MisGanancias />} />
        <Route path="/eventos/:eventId" element={<DetalleEvento />} />
        <Route path="/eventos-mapa" element={<EventosMapaScreen />} />
        <Route path="/planes/success" element={<PlanesSuccess />} />
      </>
    )
  }

  return (
    <SocketProvider token={socketToken}>
      <Routes>
        {/* Ruta principal - muestra landing page si no está autenticado */}
        <Route path="/" element={
          loading ? (
            <div style={{
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
              minHeight: '100vh',
              fontFamily: 'inter, sans-serif',
              fontSize: '18px',
              color: '#666'
            }}>
              Cargando...
            </div>
          ) : isAuthenticated ? (
            <Home />
          ) : (
            <LandingPage />
          )
        } />

        {/* Landing page - siempre accesible */}
        <Route path="/landing" element={<LandingPage />} />

        {/* Rutas públicas */}
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/verify-email" element={<VerifyEmail />} />
        <Route path="/terms" element={<Terms />} />
        <Route path="/privacy" element={<Privacy />} />
        <Route path="/comunidades" element={<Comunidades />} />
        <Route path="/comunidades/:communityId" element={<CommunityDetail />} />
        <Route path="/comunidades/:communityId/apuntes" element={<CommunityDetail />} />
        <Route path="/comunidades/:communityId/editar" element={<CommunityDetail />} />

        {/* Rutas protegidas - solo disponibles si está autenticado */}
        {ownerRoutes}

        {/* Catch-all: redirige rutas no encontradas a inicio */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </SocketProvider>
  );
}

function App() {
  return (
    <AuthProvider>
      <NotificationProvider>
        <AppRoutes />
      </NotificationProvider>
    </AuthProvider>
  );
}

export default App;

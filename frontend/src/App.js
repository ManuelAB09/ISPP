import { useEffect } from 'react';
import { Route, Routes } from 'react-router-dom';
import './App.css';
import { AuthProvider } from './contexts/AuthContext';
import Login from './screens/auth/Login';
import Register from './screens/auth/Register';
import CommunityDetail from './screens/comunidades/CommunityDetail';
import Comunidades from './screens/comunidades/Comunidades';
import CrearComunidad from './screens/comunidades/CrearComunidad';
import CrearEvento from './screens/evento/CrearEvento';
import DetalleEvento from './screens/evento/DetalleEvento';
import EventosMapaScreen from './screens/evento/EventosMapaScreen';
import Home from './screens/home/Home';
import Profile from './screens/myProfile/Profile';
import MisPagos from './screens/pagos/MisPagos';
import InstitutionPlansScreen from './screens/planes/InstitutionPlansScreen';
import PasarelaPago from './screens/planes/PasarelaPago';
import PlansScreen from './screens/planes/PlansScreen';
import TeacherProfile from './screens/teacherProfile/TeacherProfile';
import CrearUbicacionScreen from './screens/ubicaciones/CrearUbicacionScreen';
import VerifiedTeachers from './screens/verifiedTeachers/VerifiedTeachers';


function App() {
  let ownerRoutes = <></>

  const init = async () => {
    // TODO: Fetch user data and set it in state
  }

  useEffect(() => {
    init()
    // eslint-disable-next-line
  }, [])

  if (true) { // TODO: Check if user is logging
    ownerRoutes = (
      <>
        <Route path="/profesores" element={<VerifiedTeachers />} />
        <Route path="/profesores/nuevo" element={<TeacherProfile />} />
        <Route path="/profesores/:id" element={<TeacherProfile />} />
        <Route path="/crear-evento/new" element={<CrearEvento />} />
        <Route path="/crear-evento/:id" element={<CrearEvento />} />
        <Route path="/planes" element={<PlansScreen />} />
        <Route path="/planes/pasarela" element={<PasarelaPago />} />
        <Route path="/planes/instituciones" element={<InstitutionPlansScreen />} />
        <Route path="/pagos" element={<MisPagos />} />
        <Route path="/crear-ubicacion" element={<CrearUbicacionScreen />} />
        <Route path="/eventos/:eventId" element={<DetalleEvento />} />
        <Route path="/eventos-mapa" element={<EventosMapaScreen />} />
      </>
    )
  }

  return (
    <AuthProvider>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/comunidades" element={<Comunidades />} />
        <Route path="/comunidades/:communityId" element={<CommunityDetail />} />

        <Route path="/crear-comunidad" element={<CrearComunidad />} />
        <Route path="/crear-ubicacion" element={<CrearUbicacionScreen />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/perfil" element={<Profile />} />
        <Route path="/perfil/:userId" element={<Profile />} />
        {ownerRoutes}
      </Routes>
    </AuthProvider>
  );
}

export default App;

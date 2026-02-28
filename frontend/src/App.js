import { useEffect } from 'react';
import { Route, Routes } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import './App.css';
import Comunidades from './screens/comunidades/Comunidades';
import CommunityDetail from './screens/comunidades/CommunityDetail';
import CrearComunidad from './screens/comunidades/CrearComunidad';
import Home from './screens/home/Home';
import CreateEvent from './screens/event/CreateEvent';
import EventDetail from './screens/event/EventDetail';
import EventosMapaScreen from './screens/event/EventosMapaScreen';
import Register from './screens/auth/Register';
import Login from './screens/auth/Login';
import PlansScreen from './screens/planes/PlansScreen';
import TeacherProfile from './screens/teacherProfile/TeacherProfile';
import VerifiedTeachers from './screens/verifiedTeachers/VerifiedTeachers';
import CrearUbicacionScreen from './screens/ubicaciones/CrearUbicacionScreen';
import MyProfile from './screens/myProfile/MyProfile';

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
        <Route path="/create-event/new" element={<CreateEvent />} />
        <Route path="/create-event/:id" element={<CreateEvent />} />
        <Route path="/planes" element={<PlansScreen />} />
        <Route path="/crear-ubicacion" element={<CrearUbicacionScreen />} />
        <Route path="/eventos/:eventId" element={<EventDetail />} />
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
        <Route path="/perfil" element={<MyProfile />} />
        {ownerRoutes}
      </Routes>
    </AuthProvider>
  );
}

export default App;

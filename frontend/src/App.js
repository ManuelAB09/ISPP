import { useEffect } from 'react';
import { Route, Routes } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import './App.css';
import Comunidades from './screens/comunidades/Comunidades';
import CrearComunidad from './screens/comunidades/CrearComunidad';
import Home from './screens/home/Home';
import CreateEvent from './screens/event/CreateEvent';
import Register from './screens/auth/Register';
import Login from './screens/auth/Login';
import TeacherProfile from './screens/teacherProfile/TeacherProfile';
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
        <Route path="/profesores/:id" element={<TeacherProfile />} />
        <Route path="/create-event/new" element={<CreateEvent />} />
        <Route path="/create-event/:id" element={<CreateEvent />} />
      </>
    )
  }

  return (
    <AuthProvider>
      <div>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/comunidades/*" element={<Comunidades />} />

          <Route path="/crear-comunidad" element={<CrearComunidad />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          {ownerRoutes}
        </Routes>
      </div>
    </AuthProvider>
  );
}

export default App;

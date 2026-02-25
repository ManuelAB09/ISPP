import { useEffect } from 'react';
import { Route, Routes } from 'react-router-dom';
import './App.css';
import Home from './screens/home/Home';
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
        {/* Lista de profesores verificados — accesible para cualquier usuario */}
        <Route path="/profesores" element={<VerifiedTeachers />} />
        {/* Vista pública del perfil de un tutor — accesible para cualquier usuario */}
        {/* TODO: Añadir rutas adicionales según roles cuando estén implementados */}
        <Route path="/profesores/:id" element={<TeacherProfile />} />
      </>
    )
  }

  return (
    <div>
      <Routes>
        <Route path="/" element={<Home />} />
        {ownerRoutes}
      </Routes>
    </div>
  );
}

export default App;

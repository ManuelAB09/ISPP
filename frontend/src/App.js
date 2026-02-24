import { useEffect } from 'react';
import { Route, Routes } from 'react-router-dom';
import './App.css';
import Home from './screens/home/Home';
import Register from './screens/auth/Register';
import Login from './screens/auth/Login';

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
        
      </>
    )
  }

  return (
    <div>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        {ownerRoutes}
      </Routes>
    </div>
  );
}

export default App;

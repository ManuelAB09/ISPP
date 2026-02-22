import { useEffect } from 'react';
import { Route, Routes } from 'react-router-dom';
import './App.css';
import Home from './screens/home/Home';

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
        {ownerRoutes}
      </Routes>
    </div>
  );
}

export default App;

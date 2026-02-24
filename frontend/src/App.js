import { useEffect } from 'react';
import { Route, Routes } from 'react-router-dom';
import './App.css';

import Home from './screens/home/Home';
import CreateEvent from './screens/event/CreateEvent';

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
        <Route path="/create-event/new" element={<CreateEvent />} />
        <Route path="/create-event/:id" element={<CreateEvent />} />
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

import Header from "../../components/Header/Header"
import './Home.css'

const Home = () => {
  return (
    <>
      <Header page={'inicio'} />
      <div className="header">
        <div className="headerTitle home">
          <h1 className="title">MeerKatters</h1>
          <h2>Bienvenido a la mayor comunidad de estudio donde estudiantes y universitarios comparten sus estudios</h2>
        </div>
      </div>
      <div className="body">

      </div>
    </>
  )
}

export default Home

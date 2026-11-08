import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth-context'
import Login from './pages/Login'
import Inicio from './pages/Inicio'
import Agenda from './pages/Agenda'
import Assistente from './pages/Assistente'
import BemEstar from './pages/BemEstar'
import Perfil from './pages/Perfil'

function Private({ children }) {
  const { user } = useAuth()
  if (!user) return <Navigate to="/" replace />
  return children
}

function PublicOnly({ children }) {
  const { user } = useAuth()
  if (user) return <Navigate to="/inicio" replace />
  return children
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<PublicOnly><Login /></PublicOnly>} />
      <Route path="/inicio" element={<Private><Inicio /></Private>} />
      <Route path="/agenda" element={<Private><Agenda /></Private>} />
      <Route path="/assistente" element={<Private><Assistente /></Private>} />
      <Route path="/bem-estar" element={<Private><BemEstar /></Private>} />
      <Route path="/perfil" element={<Private><Perfil /></Private>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

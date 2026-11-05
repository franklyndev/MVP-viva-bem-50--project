import { useEffect, useState } from 'react'
import { getHealth } from './services/api'
import './App.css'

function statusMessage(health, failed) {
  if (failed) return 'Não foi possível falar com a API. Verifique se ela está rodando.'
  if (!health) return 'Verificando a conexão...'
  if (health.database === 'UP') return 'API e banco de dados conectados.'
  return 'A API está no ar, mas o banco de dados não respondeu.'
}

export default function App() {
  const [health, setHealth] = useState(null)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    getHealth().then(setHealth).catch(() => setFailed(true))
  }, [])

  return (
    <main>
      <h1>VivaBem 50+</h1>
      <p>Sua rotina de saúde organizada em um só lugar.</p>
      <p role="status" className="status">{statusMessage(health, failed)}</p>
    </main>
  )
}

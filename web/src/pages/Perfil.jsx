import { Link, useNavigate } from 'react-router-dom'
import { HeartPulse, LogOut, Type, Users } from 'lucide-react'
import { AppShell } from '../components/AppShell'
import { useAuth } from '../auth-context'

export default function Perfil() {
  const { user, signOut } = useAuth()
  const nav = useNavigate()
  const initial = user?.name?.charAt(0)?.toUpperCase() ?? '?'

  function leave() {
    signOut()
    nav('/')
  }

  return (
    <AppShell title="Perfil">
      <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
        <div className="avatar">{initial}</div>
        <div>
          <p style={{ fontSize: '1.5rem', fontWeight: 800 }}>{user?.name}</p>
          <p className="muted">{user?.age} anos{user?.city ? ` · ${user.city}` : ''}</p>
        </div>
      </div>
      <div style={{ marginTop: 24, display: 'grid', gap: 12 }}>
        <Link className="card row" to="/bem-estar"><HeartPulse color="var(--primary)" aria-hidden /> Bem-Estar e Hábitos</Link>
        <a className="card row" href="tel:192"><Users color="var(--primary)" aria-hidden /> Contatos de emergência</a>
        <div className="card row"><Type color="var(--primary)" aria-hidden /> Texto grande já está ativo</div>
        <button type="button" className="card row" style={{ color: 'var(--destructive)', width: '100%', border: '1px solid var(--border)' }} onClick={leave}>
          <LogOut aria-hidden /> Sair
        </button>
      </div>
    </AppShell>
  )
}
